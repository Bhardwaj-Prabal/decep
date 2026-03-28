import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.Neo4jException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Neo4jStorage — optimized dependency graph loader.
 *
 * Key design decisions vs original:
 *
 *  1. SINGLE JSON READ — parse dependency_graph.json once, keep in memory.
 *     Original read the file separately for nodes AND edges = 2x disk I/O.
 *
 *  2. UNWIND BATCH QUERIES — send 500 nodes per transaction using UNWIND.
 *     Original ran one session.run() per node = N round-trips to Neo4j.
 *     For 10,000 methods that's 10,000 network calls vs ~20 with batching.
 *
 *  3. TYPED MATCH on edges — MATCH (a:Method {id:...}) instead of
 *     MATCH (a {id:...}). Without the label Neo4j does a full node scan
 *     for every edge. With the label it uses the unique constraint index.
 *
 *  4. SEPARATE EDGE GROUPS — edges are split by type and each type gets
 *     its own UNWIND batch. This lets Neo4j use the right indexes per type.
 *
 *  5. CALLS_EXTERNAL kept separate — stored as distinct relationship so
 *     dead-code queries don't need to filter on a property.
 *
 *  6. SINGLE SESSION PER BATCH — one session per 500-node batch, not one
 *     session per node. Reduces connection overhead dramatically.
 */
public class Neo4jStorage implements AutoCloseable {

    // ── CONFIG ────────────────────────────────────────────────────────────────
    private static final int    BATCH_SIZE = 500;
    private static final String DEFAULT_URI      = "bolt://localhost:7687";
    private static final String DEFAULT_USER     = "neo4j";
    private static final String DEFAULT_PASSWORD = "neo4j";
    private static final String DEFAULT_DB       = "neo4j";

    // ── FIELDS ────────────────────────────────────────────────────────────────
    private final Driver       driver;
    private final ObjectMapper mapper;
    private final String       database;

    // ─────────────────────────────────────────────────────────────────────────
    // CONSTRUCTOR
    // ─────────────────────────────────────────────────────────────────────────

    public Neo4jStorage(String uri, String username,
                        String password, String database) {
        this.mapper   = new ObjectMapper();
        this.database = database;

        Config config = Config.builder()
            .withMaxConnectionPoolSize(10)
            .withConnectionAcquisitionTimeout(30, TimeUnit.SECONDS)
            .withMaxTransactionRetryTime(15, TimeUnit.SECONDS)
            .build();

        this.driver = GraphDatabase.driver(
            uri, AuthTokens.basic(username, password), config);

        // verify connection
        try (Session s = driver.session(sessionConfig())) {
            s.run("RETURN 1").consume();
        }
        System.out.println("Connected to Neo4j at: " + uri);
        System.out.println("Database            : " + database);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Full pipeline: read JSON once, load nodes, load edges, verify.
     */
    public void loadGraph(String graphJsonPath) throws Exception {
        long t0 = System.currentTimeMillis();

        System.out.println("\n Reading: " + graphJsonPath);
        JsonNode root = mapper.readTree(new File(graphJsonPath)); // ← single read

        JsonNode nodesObj = root.get("nodes");
        JsonNode edgesArr = root.get("edges");

        if (nodesObj == null || edgesArr == null)
            throw new IllegalArgumentException(
                "JSON must have 'nodes' and 'edges' keys");

        // ── 1. constraints + indexes (idempotent) ─────────────────────────────
        createConstraintsAndIndexes();

        // ── 2. nodes ─────────────────────────────────────────────────────────
        System.out.println("\nLoading nodes...");
        loadPackages(nodesObj.get("packages"));
        loadClasses(nodesObj.get("classes"));
        loadMethods(nodesObj.get("methods"));

        // ── 3. edges ─────────────────────────────────────────────────────────
        System.out.println("\nLoading edges...");
        loadEdges(edgesArr);

        long elapsed = System.currentTimeMillis() - t0;
        System.out.printf("%n Done in %.1f s%n", elapsed / 1000.0);

        verifyData();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSTRAINTS + INDEXES
    // ─────────────────────────────────────────────────────────────────────────

    public void createConstraintsAndIndexes() {
        System.out.println("\nCreating constraints and indexes...");
        try (Session s = driver.session(sessionConfig())) {

            String[] ddl = {
                // unique constraints — these also act as indexes
                "CREATE CONSTRAINT IF NOT EXISTS FOR (m:Method)         REQUIRE m.id IS UNIQUE",
                "CREATE CONSTRAINT IF NOT EXISTS FOR (c:Class)          REQUIRE c.id IS UNIQUE",
                "CREATE CONSTRAINT IF NOT EXISTS FOR (p:Package)        REQUIRE p.qualifiedName IS UNIQUE",
                "CREATE CONSTRAINT IF NOT EXISTS FOR (e:ExternalMethod) REQUIRE e.id IS UNIQUE",

                // additional indexes for common query patterns
                "CREATE INDEX IF NOT EXISTS FOR (m:Method) ON (m.simpleName)",
                "CREATE INDEX IF NOT EXISTS FOR (m:Method) ON (m.className)",
                "CREATE INDEX IF NOT EXISTS FOR (m:Method) ON (m.astDocId)",
                "CREATE INDEX IF NOT EXISTS FOR (m:Method) ON (m.lineStart)",
                "CREATE INDEX IF NOT EXISTS FOR (c:Class)  ON (c.packageName)",
                "CREATE INDEX IF NOT EXISTS FOR (c:Class)  ON (c.astDocId)",
            };

            for (String stmt : ddl) {
                try   { s.run(stmt).consume(); }
                catch (Neo4jException e) { /* already exists — ignore */ }
            }
        }
        System.out.println("  Constraints and indexes ready");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NODE LOADERS — all use UNWIND for batching
    // ─────────────────────────────────────────────────────────────────────────

    private void loadPackages(JsonNode packages) {
        if (packages == null || !packages.isArray()) return;

        /*
         * UNWIND sends the entire batch to Neo4j in one round-trip.
         * Neo4j processes each row server-side — no Java loop per node.
         */
        String cypher = """
            UNWIND $rows AS row
            MERGE (p:Package {qualifiedName: row.qualifiedName})
            SET   p.simpleName = row.simpleName
            """;

        List<Map<String, Object>> rows = new ArrayList<>();
        for (JsonNode n : packages) {
            Map<String, Object> row = new HashMap<>();
            row.put("qualifiedName", text(n, "qualifiedName"));
            row.put("simpleName",    text(n, "simpleName"));
            rows.add(row);
        }

        runBatched("Packages", cypher, rows);
    }

    private void loadClasses(JsonNode classes) {
        if (classes == null || !classes.isArray()) return;

        String cypher = """
            UNWIND $rows AS row
            MERGE (c:Class {id: row.id})
            SET   c.qualifiedName   = row.qualifiedName,
                  c.simpleName      = row.simpleName,
                  c.packageName     = row.packageName,
                  c.isInterface     = row.isInterface,
                  c.isAbstract      = row.isAbstract,
                  c.isEnum          = row.isEnum,
                  c.astDocId        = row.astDocId,
                  c.superClass      = row.superClass,
                  c.superInterfaces = row.superInterfaces,
                  c.annotations     = row.annotations
            """;

        List<Map<String, Object>> rows = new ArrayList<>();
        for (JsonNode n : classes) {
            Map<String, Object> row = new HashMap<>();
            row.put("id",             text(n, "id"));
            row.put("qualifiedName",  text(n, "qualifiedName"));
            row.put("simpleName",     text(n, "simpleName"));
            row.put("packageName",    text(n, "packageName"));
            row.put("isInterface",    bool(n, "isInterface"));
            row.put("isAbstract",     bool(n, "isAbstract"));
            row.put("isEnum",         bool(n, "isEnum"));
            row.put("astDocId",       text(n, "astDocId"));
            row.put("superClass",     text(n, "superClass"));
            row.put("superInterfaces", strList(n, "superInterfaces"));
            row.put("annotations",    strList(n, "annotations"));
            rows.add(row);
        }

        runBatched("Classes", cypher, rows);
    }

    private void loadMethods(JsonNode methods) {
        if (methods == null || !methods.isArray()) return;

        // ── internal methods ──────────────────────────────────────────────────
        String internalCypher = """
            UNWIND $rows AS row
            MERGE (m:Method {id: row.id})
            SET   m.simpleName   = row.simpleName,
                  m.className    = row.className,
                  m.packageName  = row.packageName,
                  m.returnType   = row.returnType,
                  m.paramTypes   = row.paramTypes,
                  m.isPublic     = row.isPublic,
                  m.isPrivate    = row.isPrivate,
                  m.isProtected  = row.isProtected,
                  m.isStatic     = row.isStatic,
                  m.isAbstract   = row.isAbstract,
                  m.isConstructor= row.isConstructor,
                  m.lineNumber   = row.lineNumber,
                  m.astDocId     = row.astDocId
            """;

        // ── external methods — lighter node, separate label ───────────────────
        String externalCypher = """
            UNWIND $rows AS row
            MERGE (e:ExternalMethod {id: row.id})
            SET   e.simpleName  = row.simpleName,
                  e.className   = row.className,
                  e.packageName = row.packageName
            """;

        List<Map<String, Object>> internalRows = new ArrayList<>();
        List<Map<String, Object>> externalRows = new ArrayList<>();

        for (JsonNode n : methods) {
            boolean isExternal = bool(n, "isExternal");

            if (isExternal) {
                Map<String, Object> row = new HashMap<>();
                row.put("id",          text(n, "id"));
                row.put("simpleName",  text(n, "simpleName"));
                row.put("className",   text(n, "className"));
                row.put("packageName", text(n, "packageName"));
                externalRows.add(row);
            } else {
                Map<String, Object> row = new HashMap<>();
                row.put("id",           text(n, "id"));
                row.put("simpleName",   text(n, "simpleName"));
                row.put("className",    text(n, "className"));
                row.put("packageName",  text(n, "packageName"));
                row.put("returnType",   text(n, "returnType"));
                row.put("paramTypes",   strList(n, "paramTypes"));
                row.put("isPublic",     bool(n, "isPublic"));
                row.put("isPrivate",    bool(n, "isPrivate"));
                row.put("isProtected",  bool(n, "isProtected"));
                row.put("isStatic",     bool(n, "isStatic"));
                row.put("isAbstract",   bool(n, "isAbstract"));
                row.put("isConstructor",bool(n, "isConstructor"));
                row.put("lineNumber",   intVal(n, "lineNumber"));
                row.put("astDocId",     text(n, "astDocId"));
                internalRows.add(row);
            }
        }

        runBatched("Methods (internal)", internalCypher, internalRows);
        runBatched("Methods (external)", externalCypher, externalRows);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EDGE LOADER
    // ─────────────────────────────────────────────────────────────────────────

    private void loadEdges(JsonNode edgesArr) {
        if (edgesArr == null || !edgesArr.isArray()) return;

        /*
         * Group edges by type so each UNWIND batch uses a single query
         * with typed MATCH clauses — Neo4j can use label indexes for each.
         *
         * Typed MATCH (m:Method {id: ...}) is O(1) via unique constraint.
         * Untyped MATCH ({id: ...}) is O(n) — full node scan every time.
         */
        Map<String, List<Map<String, Object>>> byType = new LinkedHashMap<>();
        for (JsonNode e : edgesArr) {
            String type = text(e, "type");
            byType.computeIfAbsent(type, k -> new ArrayList<>())
                  .add(edgeRow(e));
        }

        // Cypher per edge type — each uses the most specific labels
        Map<String, String> edgeCypher = new LinkedHashMap<>();

        edgeCypher.put("BELONGS_TO", """
            UNWIND $rows AS row
            MATCH (c:Class   {id: row.fromId})
            MATCH (p:Package {qualifiedName: row.toId})
            MERGE (c)-[:BELONGS_TO]->(p)
            """);

        edgeCypher.put("EXTENDS", """
            UNWIND $rows AS row
            MATCH (a:Class {id: row.fromId})
            MERGE (b:Class {id: row.toId})
              ON CREATE SET b.qualifiedName = row.toId,
                            b.simpleName    = last(split(row.toId,'.'))
            MERGE (a)-[:EXTENDS]->(b)
            """);

        edgeCypher.put("IMPLEMENTS", """
            UNWIND $rows AS row
            MATCH (a:Class {id: row.fromId})
            MERGE (b:Class {id: row.toId})
              ON CREATE SET b.qualifiedName = row.toId,
                            b.simpleName    = last(split(row.toId,'.')),
                            b.isInterface   = true
            MERGE (a)-[:IMPLEMENTS]->(b)
            """);

        edgeCypher.put("HAS_METHOD", """
            UNWIND $rows AS row
            MATCH (c:Class  {id: row.fromId})
            MATCH (m:Method {id: row.toId})
            MERGE (c)-[:HAS_METHOD]->(m)
            """);

        edgeCypher.put("OVERRIDES", """
            UNWIND $rows AS row
            MATCH (child:Method  {id: row.fromId})
            MATCH (parent:Method {id: row.toId})
            MERGE (child)-[:OVERRIDES]->(parent)
            """);

        /*
         * CALLS — internal method to internal method.
         * lineNumber and callCount stored on the relationship.
         * MERGE alone can't SET props, so we use MERGE + SET pattern.
         */
        edgeCypher.put("CALLS", """
            UNWIND $rows AS row
            MATCH (caller:Method {id: row.fromId})
            MATCH (callee:Method {id: row.toId})
            MERGE (caller)-[r:CALLS]->(callee)
            SET r.lineNumber = row.lineNumber,
                r.callCount  = row.callCount
            """);

        /*
         * CALLS_EXTERNAL — kept as a separate relationship type.
         * Reason: dead code queries like
         *   MATCH (m:Method) WHERE NOT ()-[:CALLS]->(m)
         * would otherwise also count external methods as "callers",
         * making the query wrong. Separate type = clean separation.
         */
        edgeCypher.put("CALLS_EXTERNAL", """
            UNWIND $rows AS row
            MATCH (caller:Method         {id: row.fromId})
            MATCH (callee:ExternalMethod {id: row.toId})
            MERGE (caller)-[r:CALLS_EXTERNAL]->(callee)
            SET r.lineNumber = row.lineNumber,
                r.callCount  = row.callCount
            """);

        for (Map.Entry<String, List<Map<String, Object>>> entry : byType.entrySet()) {
            String type   = entry.getKey();
            String cypher = edgeCypher.get(type);
            if (cypher == null) {
                System.out.printf("  Skipping unknown edge type '%s' (%d edges)%n",
                                  type, entry.getValue().size());
                continue;
            }
            runBatched(type, cypher, entry.getValue());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BATCH RUNNER — the core optimization
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Splits rows into chunks of BATCH_SIZE and executes each chunk
     * in a single transaction using UNWIND.
     *
     * Example: 8,000 Method nodes → 16 transactions of 500 rows each.
     * Each transaction = 1 network round-trip to Neo4j.
     * Without batching: 8,000 round-trips.
     */
    private void runBatched(String label, String cypher,
                        List<Map<String, Object>> rows) {
    if (rows.isEmpty()) return;

    int total   = rows.size();
    int batches = (int) Math.ceil((double) total / BATCH_SIZE);
    System.out.printf("  %-30s %,d items in %d batches%n",
                      label, total, batches);

    try (Session session = driver.session()) {
        for (int i = 0; i < total; i += BATCH_SIZE) {
            List<Map<String, Object>> chunk =
                rows.subList(i, Math.min(i + BATCH_SIZE, total));

            // 6.x: use explicit transaction
            try (Transaction tx = session.beginTransaction()) {
                tx.run(cypher, Map.of("rows", chunk));
                tx.commit();
            }
        }
    }
    System.out.printf("    Done: %,d%n", total);
}

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFY
    // ─────────────────────────────────────────────────────────────────────────

    public void verifyData() {
        System.out.println("\nVerification:");
        try (Session s = driver.session(sessionConfig())) {
            printCount(s, "Method nodes",         "MATCH (m:Method)         RETURN count(m) AS n");
            printCount(s, "ExternalMethod nodes",  "MATCH (e:ExternalMethod) RETURN count(e) AS n");
            printCount(s, "Class nodes",           "MATCH (c:Class)          RETURN count(c) AS n");
            printCount(s, "Package nodes",         "MATCH (p:Package)        RETURN count(p) AS n");
            printCount(s, "CALLS edges",           "MATCH ()-[:CALLS]->()          RETURN count(*) AS n");
            printCount(s, "CALLS_EXTERNAL edges",  "MATCH ()-[:CALLS_EXTERNAL]->() RETURN count(*) AS n");
            printCount(s, "HAS_METHOD edges",      "MATCH ()-[:HAS_METHOD]->()     RETURN count(*) AS n");
            printCount(s, "EXTENDS edges",         "MATCH ()-[:EXTENDS]->()        RETURN count(*) AS n");
            printCount(s, "IMPLEMENTS edges",      "MATCH ()-[:IMPLEMENTS]->()     RETURN count(*) AS n");
            printCount(s, "OVERRIDES edges",       "MATCH ()-[:OVERRIDES]->()      RETURN count(*) AS n");
        }
    }

    private void printCount(Session s, String label, String cypher) {
        long n = s.run(cypher).single().get("n").asLong();
        System.out.printf("  %-30s %,d%n", label, n);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUERY HELPERS (called from Main.java or FastAPI backend)
    // ─────────────────────────────────────────────────────────────────────────

    public List<Map<String, Object>> getCallers(String methodId) {
    try (Session s = driver.session();
         Transaction tx = s.beginTransaction()) {
        return tx.run("""
            MATCH (caller:Method)-[:CALLS]->(m:Method {id: $id})
            RETURN caller.id AS id,
                   caller.simpleName AS name,
                   caller.className  AS className
            LIMIT 20
            """, Map.of("id", methodId))
            .list(r -> r.asMap());
    }
}

public List<Map<String, Object>> getCallees(String methodId) {
    try (Session s = driver.session();
         Transaction tx = s.beginTransaction()) {
        return tx.run("""
            MATCH (m:Method {id: $id})-[:CALLS]->(callee:Method)
            RETURN callee.id AS id,
                   callee.simpleName AS name,
                   callee.className  AS className
            LIMIT 20
            """, Map.of("id", methodId))
            .list(r -> r.asMap());
    }
}

public int getCallerCount(String methodId) {
    try (Session s = driver.session();
         Transaction tx = s.beginTransaction()) {
        return tx.run("""
            MATCH ()-[:CALLS]->(m:Method {id: $id})
            RETURN count(*) AS n
            """, Map.of("id", methodId))
            .single().get("n").asInt();
    }
}

public List<Map<String, Object>> getDeadMethods() {
    try (Session s = driver.session();
         Transaction tx = s.beginTransaction()) {
        return tx.run("""
            MATCH (m:Method)
            WHERE NOT ()-[:CALLS]->(m)
              AND NOT m.isAbstract
              AND NOT m.simpleName IN
                  ['main','init','destroy','equals',
                   'hashCode','toString','compareTo',
                   'doGet','doPost','run']
            RETURN m.id         AS id,
                   m.className  AS className,
                   m.simpleName AS method,
                   m.lineNumber AS line
            ORDER BY className, method
            """)
            .list(r -> r.asMap());
    }
}

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITIES
    // ─────────────────────────────────────────────────────────────────────────

    private SessionConfig sessionConfig() {
        return SessionConfig.builder().withDatabase(database).build();
    }

    private static String text(JsonNode n, String key) {
        return n.has(key) && !n.get(key).isNull() ? n.get(key).asText() : "";
    }

    private static boolean bool(JsonNode n, String key) {
        return n.has(key) && n.get(key).asBoolean();
    }

    private static int intVal(JsonNode n, String key) {
        return n.has(key) ? n.get(key).asInt(-1) : -1;
    }

    private static List<String> strList(JsonNode n, String key) {
        List<String> list = new ArrayList<>();
        if (n.has(key) && n.get(key).isArray())
            n.get(key).forEach(e -> list.add(e.asText()));
        return list;
    }

    /** Build a flat map for an edge row including props. */
    private static Map<String, Object> edgeRow(JsonNode e) {
        Map<String, Object> row = new HashMap<>();
        row.put("fromId", text(e, "fromId"));
        row.put("toId",   text(e, "toId"));
        JsonNode props = e.get("props");
        row.put("lineNumber", props != null && props.has("lineNumber")
                               ? props.get("lineNumber").asInt() : -1);
        row.put("callCount",  props != null && props.has("callCount")
                               ? props.get("callCount").asInt() : 0);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLOSE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void close() {
        if (driver != null) driver.close();
        System.out.println("Neo4j connection closed");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN
    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {

        // ── defaults ──────────────────────────────────────────────────────────
        String graphJson = "C:/Users/PrabalBhardwaj/Desktop/osttra-harmony3/dependency_graph.json";
        String neo4jUri  = DEFAULT_URI;
        String username  = DEFAULT_USER;
        String password  = DEFAULT_PASSWORD;
        String database  = DEFAULT_DB;

        // ── CLI args override defaults ─────────────────────────────────────────
        if (args.length > 0) graphJson = args[0];
        if (args.length > 1) neo4jUri  = args[1];
        if (args.length > 2) username  = args[2];
        if (args.length > 3) password  = args[3];
        if (args.length > 4) database  = args[4];

        // ── env vars override CLI (useful for Docker / CI) ────────────────────
        graphJson = envOr("DEPENDENCY_GRAPH_PATH", graphJson);
        neo4jUri  = envOr("NEO4J_URI",             neo4jUri);
        username  = envOr("NEO4J_USER",             username);
        password  = envOr("NEO4J_PASSWORD",         password);
        database  = envOr("NEO4J_DATABASE",         database);

        System.out.println("Graph JSON : " + graphJson);
        System.out.println("Neo4j URI  : " + neo4jUri);
        System.out.println("Database   : " + database);

        try (Neo4jStorage storage = new Neo4jStorage(
                neo4jUri, username, password, database)) {

            storage.loadGraph(graphJson);

        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String envOr(String envKey, String fallback) {
        String val = System.getenv(envKey);
        return (val != null && !val.isBlank()) ? val : fallback;
    }
}