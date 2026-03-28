import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.*;
import spoon.reflect.visitor.filter.TypeFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * OPTIMIZED Coverage-Analysis Extractor
 * 
 * Performance optimizations:
 * 1. No reflection fallback (major speedup)
 * 2. Reduced depth limit from 35 to 15
 * 3. Skip inner/anonymous classes early
 * 4. Parallel processing for AST serialization
 * 5. Minimal metadata extraction (only what LLM needs)
 * 6. Direct MongoDB batch inserts (no intermediate files)
 * 
 * Expected runtime: 20-40 minutes (down from 6+ hours)
 */
public class OptimizedMain {

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIGURATION
    // ─────────────────────────────────────────────────────────────────────────
    
    private static final boolean INCLUDE_POSITIONS = true;      // Keep for line mapping
    private static final boolean INCLUDE_SIGNATURES = true;     // Keep for method lookup
    private static final boolean INCLUDE_METRICS = true;        // Keep for complexity
    private static final boolean USE_REFLECTION = false;        // DISABLED (major speedup)
    private static final int MAX_DEPTH = 15;                    // Reduced from 35
    private static final int BATCH_SIZE = 100;                  // For MongoDB batch insert
    private static final boolean WRITE_FILES = false;           // Set to false if using MongoDB
    private static final boolean USE_MONGODB = true;            // Direct MongoDB insert
    
    // ─────────────────────────────────────────────────────────────────────────
    // MODELS (Simplified for LLM)
    // ─────────────────────────────────────────────────────────────────────────
    
    static class MethodNode {
        public String id, simpleName, className, packageName;
        public String returnType;
        public List<String> paramTypes = new ArrayList<>();
        public List<String> thrownTypes = new ArrayList<>();
        public List<String> annotations = new ArrayList<>();
        public boolean isPublic, isPrivate, isProtected;
        public boolean isStatic, isAbstract;
        public int lineStart = -1, lineEnd = -1;
        
        // Coverage metrics
        public int cyclomaticComplexity = 1;
        public int branchCount = 0;
        public int loopCount = 0;
        public int throwCount = 0;
        public int catchCount = 0;
        public boolean hasNullCheck = false;
        public boolean hasInstanceofCheck = false;
        
        // Code snippets (what LLM really needs)
        public String methodBody;           // Full method body as text
        public List<String> conditions;     // All conditions in method
        public List<String> returnPoints;   // All return statements
        
        // Dependencies (will be filled from Neo4j later)
        public List<String> mockCandidates = new ArrayList<>();
        public List<String> siblingCalls = new ArrayList<>();
        
        public String astDocId;
        public String neo4jId;
    }
    
    static class ClassNode {
        public String id, qualifiedName, simpleName, packageName;
        public boolean isInterface, isAbstract, isEnum;
        public List<String> fieldNames = new ArrayList<>();
        public List<String> fieldTypes = new ArrayList<>();
        public String superClass;
        public List<String> superInterfaces = new ArrayList<>();
        public String astDocId;
    }
    
    static class PackageNode {
        public String qualifiedName, simpleName;
    }
    
    static class GraphEdge {
        public String fromId, toId, type;
        public Map<String, Object> props = new LinkedHashMap<>();
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // GLOBAL STORES
    // ─────────────────────────────────────────────────────────────────────────
    
    static final Map<String, MethodNode> methods = new LinkedHashMap<>();
    static final Map<String, ClassNode> classes = new LinkedHashMap<>();
    static final Map<String, PackageNode> packages = new LinkedHashMap<>();
    static final List<GraphEdge> edges = new ArrayList<>();
    static final Map<String, Set<String>> callers = new LinkedHashMap<>();
    
    static final ObjectMapper mapper = new ObjectMapper()
        .configure(SerializationFeature.INDENT_OUTPUT, false)  // No pretty print during serialization
        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    
    // ─────────────────────────────────────────────────────────────────────────
    // MONGODB CONNECTION (if using)
    // ─────────────────────────────────────────────────────────────────────────
    
    static com.mongodb.client.MongoClient mongoClient;
    static com.mongodb.client.MongoCollection<org.bson.Document> astCollection;
    
    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    
    static String methodId(CtMethod<?> m) {
        return m.getDeclaringType().getQualifiedName() + "#" + m.getSignature();
    }
    
    static String classId(CtType<?> t) {
        return t.getQualifiedName();
    }
    
    static void ensurePackage(String pkg) {
        packages.computeIfAbsent(pkg, k -> {
            PackageNode p = new PackageNode();
            p.qualifiedName = k;
            p.simpleName = k.contains(".") ? k.substring(k.lastIndexOf('.') + 1) : k;
            return p;
        });
    }
    
    static GraphEdge edge(String from, String to, String type) {
        GraphEdge e = new GraphEdge();
        e.fromId = from;
        e.toId = to;
        e.type = type;
        return e;
    }
    
    static boolean isExternalClass(String fqn) {
        if (fqn == null || fqn.equals("UnresolvedType")) return true;
        return fqn.startsWith("java.") || fqn.startsWith("javax.") ||
               fqn.startsWith("sun.") || fqn.startsWith("com.sun.") ||
               fqn.startsWith("org.springframework.") || fqn.startsWith("org.hibernate.") ||
               fqn.startsWith("org.apache.") || fqn.startsWith("org.slf4j.");
    }
    
    static String safe(Object o) {
        try {
            return o == null ? null : o.toString();
        } catch (Exception e) {
            return null;
        }
    }
    
    static boolean isAnonymous(CtType<?> t) {
        return t.getSimpleName().contains("$") || 
               t.getQualifiedName().contains("$") ||
               t.isAnonymous() ||
               t.isLocalType();
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // OPTIMIZED SERIALIZER - Only what LLM needs
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Optimized serialization - focuses on code snippets and essential metadata
     * No reflection, no deep recursion, no unnecessary fields
     */
    static org.bson.Document serializeClassOptimized(CtType<?> type) {
        org.bson.Document doc = new org.bson.Document();
        
        // Basic identity
        doc.put("_id", type.getQualifiedName());
        doc.put("nodeType", type.getClass().getSimpleName().replace("Impl", ""));
        doc.put("qualifiedName", type.getQualifiedName());
        doc.put("simpleName", type.getSimpleName());
        doc.put("packageName", type.getPackage() != null ? 
                type.getPackage().getQualifiedName() : "default");
        doc.put("isInterface", type instanceof CtInterface);
        doc.put("isEnum", type instanceof CtEnum);
        doc.put("isAbstract", type.isAbstract());
        
        // Source file location (for retrieving original source)
        try {
            if (type.getPosition() != null && type.getPosition().getFile() != null) {
                doc.put("sourceFile", type.getPosition().getFile().getAbsolutePath());
                doc.put("lineStart", type.getPosition().getLine());
                doc.put("lineEnd", type.getPosition().getEndLine());
            }
        } catch (Exception ignored) {}
        
        // Fields (for mock candidate detection)
        List<org.bson.Document> fields = new ArrayList<>();
        for (CtField<?> field : type.getFields()) {
            org.bson.Document fieldDoc = new org.bson.Document();
            fieldDoc.put("name", field.getSimpleName());
            fieldDoc.put("type", field.getType() != null ? 
                    field.getType().getQualifiedName() : "unknown");
            fieldDoc.put("modifiers", getModifiers(field));
            fields.add(fieldDoc);
        }
        doc.put("fields", fields);
        
        // Annotations
        List<String> annotations = new ArrayList<>();
        for (CtAnnotation<?> ann : type.getAnnotations()) {
            try {
                annotations.add(ann.getAnnotationType().getQualifiedName());
            } catch (Exception ignored) {}
        }
        doc.put("annotations", annotations);
        
        // Inheritance
        try {
            if (type instanceof CtClass) {
                CtTypeReference<?> sup = ((CtClass<?>) type).getSuperclass();
                if (sup != null && !sup.getQualifiedName().equals("java.lang.Object")) {
                    doc.put("superClass", sup.getQualifiedName());
                }
            }
        } catch (Exception ignored) {}
        
        List<String> interfaces = new ArrayList<>();
        for (CtTypeReference<?> iface : type.getSuperInterfaces()) {
            try {
                interfaces.add(iface.getQualifiedName());
            } catch (Exception ignored) {}
        }
        doc.put("superInterfaces", interfaces);
        
        // METHODS - This is the most important part for LLM
        List<org.bson.Document> methodDocs = new ArrayList<>();
        for (CtMethod<?> method : type.getMethods()) {
            methodDocs.add(serializeMethodOptimized(method, type.getQualifiedName()));
        }
        doc.put("methods", methodDocs);
        
        return doc;
    }
    
    /**
     * Optimized method serialization - captures code as text, not full AST
     */
    static org.bson.Document serializeMethodOptimized(CtMethod<?> method, String className) {
        org.bson.Document doc = new org.bson.Document();
        
        // Identity
        doc.put("name", method.getSimpleName());
        doc.put("signature", method.getSignature());
        doc.put("id", methodId(method));
        
        // Return type
        doc.put("returnType", method.getType() != null ? 
                method.getType().getQualifiedName() : "void");
        
        // Parameters
        List<String> paramTypes = new ArrayList<>();
        List<String> paramNames = new ArrayList<>();
        for (CtParameter<?> p : method.getParameters()) {
            paramTypes.add(p.getType() != null ? p.getType().getQualifiedName() : "unknown");
            paramNames.add(p.getSimpleName());
        }
        doc.put("paramTypes", paramTypes);
        doc.put("paramNames", paramNames);
        
        // Thrown exceptions
        List<String> thrown = new ArrayList<>();
        for (CtTypeReference<?> t : method.getThrownTypes()) {
            thrown.add(t.getQualifiedName());
        }
        doc.put("thrownTypes", thrown);
        
        // Modifiers
        doc.put("modifiers", getModifiers(method));
        
        // Position (for JaCoCo line mapping)
        if (method.getPosition().isValidPosition()) {
            doc.put("lineStart", method.getPosition().getLine());
            doc.put("lineEnd", method.getPosition().getEndLine());
        }
        
        // ─── THE CODE SNIPPETS (what LLM actually needs) ───
        
        // Full method body as text
        if (method.getBody() != null) {
            String bodyText = method.getBody().toString();
            doc.put("body", bodyText);
            
            // Extract all conditions for path analysis
            List<String> conditions = extractConditions(method);
            doc.put("conditions", conditions);
            
            // Extract all return points
            List<String> returns = extractReturns(method);
            doc.put("returnPoints", returns);
            
            // Simple metrics from text analysis
            doc.put("lineCount", bodyText.split("\n").length);
            doc.put("statementCount", method.getBody().getStatements().size());
        }
        
        // Coverage metrics (will be enriched from JaCoCo later)
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("cyclomaticComplexity", computeComplexityFromCode(method));
        metrics.put("branchCount", countBranches(method));
        metrics.put("loopCount", countLoops(method));
        metrics.put("hasNullCheck", hasNullCheck(method));
        metrics.put("hasInstanceofCheck", hasInstanceofCheck(method));
        doc.put("metrics", metrics);
        
        // Dependencies (to be filled from Neo4j)
        doc.put("mockCandidates", new ArrayList<String>());
        doc.put("siblingCalls", new ArrayList<String>());
        
        return doc;
    }
    
    /**
     * Get modifiers as a map
     */
    static Map<String, Boolean> getModifiers(CtModifiable mod) {
        Map<String, Boolean> mods = new LinkedHashMap<>();
        mods.put("public", mod.isPublic());
        mods.put("private", mod.isPrivate());
        mods.put("protected", mod.isProtected());
        mods.put("static", mod.isStatic());
        mods.put("abstract", mod.isAbstract());
        mods.put("final", mod.isFinal());
        return mods;
    }
    
    /**
     * Extract all condition expressions from method
     */
    static List<String> extractConditions(CtMethod<?> method) {
        List<String> conditions = new ArrayList<>();
        if (method.getBody() == null) return conditions;
        
        method.getBody().accept(new CtScanner() {
            @Override
            public void visitCtIf(CtIf ifElement) {
                conditions.add(safe(ifElement.getCondition()));
                super.visitCtIf(ifElement);
            }
            
            @Override
            public <T> void visitCtConditional(CtConditional<T> conditional) {
                conditions.add(safe(conditional.getCondition()));
                super.visitCtConditional(conditional);
            }
            
            @Override
            public void visitCtWhile(CtWhile whileLoop) {
                conditions.add(safe(whileLoop.getLoopingExpression()));
                super.visitCtWhile(whileLoop);
            }
            
            @Override
            public void visitCtFor(CtFor forLoop) {
                conditions.add(safe(forLoop.getExpression()));
                super.visitCtFor(forLoop);
            }
        });
        
        return conditions;
    }
    
    /**
     * Extract all return statements
     */
    static List<String> extractReturns(CtMethod<?> method) {
        List<String> returns = new ArrayList<>();
        if (method.getBody() == null) return returns;
        
        method.getBody().accept(new CtScanner() {
            @Override
            public <T> void visitCtReturn(CtReturn<T> returnStmt) {
                returns.add(safe(returnStmt.getReturnedExpression()));
                super.visitCtReturn(returnStmt);
            }
        });
        
        return returns;
    }
    
    /**
     * Compute cyclomatic complexity from code text (fast approximation)
     */
    static int computeComplexityFromCode(CtMethod<?> method) {
        if (method.getBody() == null) return 1;
        String body = method.getBody().toString();
        
        int complexity = 1;
        complexity += countOccurrences(body, "if");
        complexity += countOccurrences(body, "?");
        complexity += countOccurrences(body, "for");
        complexity += countOccurrences(body, "while");
        complexity += countOccurrences(body, "catch");
        complexity += countOccurrences(body, "&&");
        complexity += countOccurrences(body, "||");
        complexity += countOccurrences(body, "case ");
        
        return Math.min(complexity, 50); // Cap at 50
    }
    
    static int countOccurrences(String text, String sub) {
        int count = 0, idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
    
    static int countBranches(CtMethod<?> method) {
        if (method.getBody() == null) return 0;
        String body = method.getBody().toString();
        return countOccurrences(body, "if") + countOccurrences(body, "?");
    }
    
    static int countLoops(CtMethod<?> method) {
        if (method.getBody() == null) return 0;
        String body = method.getBody().toString();
        return countOccurrences(body, "for") + countOccurrences(body, "while");
    }
    
    static boolean hasNullCheck(CtMethod<?> method) {
        if (method.getBody() == null) return false;
        String body = method.getBody().toString();
        return body.contains("== null") || body.contains("!= null");
    }
    
    static boolean hasInstanceofCheck(CtMethod<?> method) {
        if (method.getBody() == null) return false;
        return method.getBody().toString().contains("instanceof");
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // DEPENDENCY GRAPH EXTRACTION (Optimized)
    // ─────────────────────────────────────────────────────────────────────────
    
    static void extractCallGraph(CtModel model) {
        System.out.println("  Building class nodes...");
        
        // Classes
        for (CtType<?> type : model.getAllTypes()) {
            if (isAnonymous(type)) continue;
            
            ClassNode cn = new ClassNode();
            cn.id = classId(type);
            cn.qualifiedName = type.getQualifiedName();
            cn.simpleName = type.getSimpleName();
            cn.packageName = type.getPackage() != null ? 
                    type.getPackage().getQualifiedName() : "default";
            cn.isInterface = (type instanceof CtInterface);
            cn.isAbstract = type.isAbstract();
            cn.isEnum = (type instanceof CtEnum);
            cn.astDocId = cn.qualifiedName;
            
            for (CtField<?> f : type.getFields()) {
                cn.fieldNames.add(f.getSimpleName());
                cn.fieldTypes.add(f.getType() != null ? 
                        f.getType().getQualifiedName() : "unknown");
            }
            
            try {
                if (type instanceof CtClass) {
                    CtTypeReference<?> sup = ((CtClass<?>) type).getSuperclass();
                    if (sup != null && !sup.getQualifiedName().equals("java.lang.Object")) {
                        cn.superClass = sup.getQualifiedName();
                    }
                }
            } catch (Exception ignored) {}
            
            for (CtTypeReference<?> i : type.getSuperInterfaces()) {
                cn.superInterfaces.add(i.getQualifiedName());
            }
            
            classes.put(cn.id, cn);
            ensurePackage(cn.packageName);
            edges.add(edge(cn.id, cn.packageName, "BELONGS_TO"));
            
            if (cn.superClass != null) {
                edges.add(edge(cn.id, cn.superClass, "EXTENDS"));
            }
            for (String iface : cn.superInterfaces) {
                edges.add(edge(cn.id, iface, "IMPLEMENTS"));
            }
        }
        
        System.out.println("  Building method nodes and call graph...");
        
        // Methods and calls
        int methodCount = 0;
        for (CtType<?> type : model.getAllTypes()) {
            if (isAnonymous(type)) continue;
            String cqn = classId(type);
            
            for (CtMethod<?> method : type.getMethods()) {
                String mId = methodId(method);
                methodCount++;
                
                if (methodCount % 1000 == 0) {
                    System.out.println("    Processed " + methodCount + " methods...");
                }
                
                MethodNode mn = new MethodNode();
                mn.id = mId;
                mn.simpleName = method.getSimpleName();
                mn.className = cqn;
                mn.packageName = type.getPackage() != null ? 
                        type.getPackage().getQualifiedName() : "default";
                mn.isPublic = method.isPublic();
                mn.isPrivate = method.isPrivate();
                mn.isProtected = method.isProtected();
                mn.isStatic = method.isStatic();
                mn.isAbstract = method.isAbstract();
                mn.astDocId = cqn;
                mn.neo4jId = mId;
                
                try {
                    mn.lineStart = method.getPosition().isValidPosition() ? 
                            method.getPosition().getLine() : -1;
                    mn.lineEnd = method.getPosition().isValidPosition() ? 
                            method.getPosition().getEndLine() : -1;
                } catch (Exception ignored) {}
                
                mn.returnType = method.getType() != null ? 
                        method.getType().getQualifiedName() : "void";
                
                for (CtParameter<?> p : method.getParameters()) {
                    mn.paramTypes.add(p.getType() != null ? 
                            p.getType().getQualifiedName() : "unknown");
                }
                
                // Code snippets
                if (method.getBody() != null) {
                    mn.methodBody = method.getBody().toString();
                    mn.conditions = extractConditions(method);
                    mn.returnPoints = extractReturns(method);
                }
                
                // Metrics
                mn.cyclomaticComplexity = computeComplexityFromCode(method);
                mn.branchCount = countBranches(method);
                mn.loopCount = countLoops(method);
                mn.hasNullCheck = hasNullCheck(method);
                mn.hasInstanceofCheck = hasInstanceofCheck(method);
                
                methods.put(mId, mn);
                edges.add(edge(cqn, mId, "HAS_METHOD"));
                
                // Extract calls (simplified)
                try {
                    for (CtInvocation<?> inv : method.getElements(
                            new TypeFilter<>(CtInvocation.class))) {
                        try {
                            String targetClass = "UnresolvedType";
                            String methodName = inv.getExecutable().getSimpleName();
                            
                            if (inv.getExecutable().getDeclaringType() != null) {
                                targetClass = inv.getExecutable()
                                        .getDeclaringType().getQualifiedName();
                            }
                            
                            String targetId = targetClass + "#" + methodName;
                            boolean ext = isExternalClass(targetClass);
                            
                            if (ext && !methods.containsKey(targetId)) {
                                MethodNode em = new MethodNode();
                                em.id = targetId;
                                em.simpleName = methodName;
                                em.className = targetClass;
                                em.isExternal = true;
                                methods.put(targetId, em);
                            }
                            
                            edges.add(edge(mId, targetId, ext ? "CALLS_EXTERNAL" : "CALLS"));
                            callers.computeIfAbsent(targetId, k -> new HashSet<>()).add(mId);
                            
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }
        }
        
        System.out.println("  Total methods: " + methodCount);
        
        // Add CALLED_BY edges
        for (Map.Entry<String, Set<String>> entry : callers.entrySet()) {
            for (String callerId : entry.getValue()) {
                edges.add(edge(entry.getKey(), callerId, "CALLED_BY"));
            }
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // MAIN
    // ─────────────────────────────────────────────────────────────────────────
    
    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
        
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║     OPTIMIZED Coverage Analyzer - Harmony Codebase       ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
        
        // ── CONFIG ─────────────────────────────────────────────────────────────────
        String rootPath = "C:/Users/PrabalBhardwaj/Desktop/osttra-harmony3/";
        String graphJson = rootPath + "dependency_graph.json";
        String methodIndexPath = rootPath + "method_index.json";
        
        // Auto-discover source folders
        File root = new File(rootPath);
        List<String> srcFolders = new ArrayList<>();
        for (File f : Objects.requireNonNull(root.listFiles())) {
            if (f.isDirectory() && !f.getName().startsWith(".") && 
                !f.getName().equals("ast") && !f.getName().equals("build") &&
                !f.getName().equals("target") && !f.getName().equals("lib")) {
                if (containsJava(f)) {
                    srcFolders.add(f.getAbsolutePath());
                    System.out.println("  📁 Source folder: " + f.getName());
                }
            }
        }
        
        // ── SPOON MODEL ──────────────────────────────────────────────────────────
        System.out.println("\n🔧 Building Spoon model...");
        long modelStart = System.currentTimeMillis();
        
        Launcher launcher = new Launcher();
        for (String folder : srcFolders) {
            launcher.addInputResource(folder);
        }
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
        launcher.getEnvironment().setIgnoreSyntaxErrors(true);
        launcher.getEnvironment().setComplianceLevel(11);
        launcher.getEnvironment().setAutoImports(false);  // Disable auto-imports for speed
        
        launcher.buildModel();
        CtModel model = launcher.getModel();
        
        System.out.println("  ✅ Model built in: " + (System.currentTimeMillis() - modelStart) + "ms");
        
        // ── OPTION 1: Direct MongoDB Insert (Recommended) ────────────────────────
        if (USE_MONGODB) {
            System.out.println("\n📦 Connecting to MongoDB...");
            mongoClient = com.mongodb.client.MongoClients.create("mongodb://localhost:27017");
            com.mongodb.client.MongoDatabase db = mongoClient.getDatabase("harmony_codebase");
            astCollection = db.getCollection("ast_nodes");
            
            // Drop existing collection for fresh start
            astCollection.drop();
            System.out.println("  ✅ Connected, ready to insert");
        }
        
        // ── PASS 1: AST Serialization (Parallel) ─────────────────────────────────
        System.out.println("\n📄 Pass 1: Serializing AST (parallel)...");
        
        AtomicInteger astCount = new AtomicInteger(0);
        AtomicInteger astErrors = new AtomicInteger(0);
        List<CtType<?>> types = model.getAllTypes().stream()
                .filter(t -> !isAnonymous(t))
                .collect(Collectors.toList());
        
        System.out.println("  Total types to process: " + types.size());
        
        // For batch MongoDB insert
        List<org.bson.Document> batch = new ArrayList<>();
        
        // Process in parallel
        types.parallelStream().forEach(type -> {
            try {
                org.bson.Document doc = serializeClassOptimized(type);
                
                if (USE_MONGODB) {
                    synchronized (batch) {
                        batch.add(doc);
                        if (batch.size() >= BATCH_SIZE) {
                            astCollection.insertMany(new ArrayList<>(batch));
                            batch.clear();
                        }
                    }
                } else if (WRITE_FILES) {
                    String fname = type.getQualifiedName().replace(".", "_") + ".json";
                    synchronized (mapper) {
                        mapper.writerWithDefaultPrettyPrinter()
                              .writeValue(new File(rootPath + "ast/" + fname), doc);
                    }
                }
                
                int count = astCount.incrementAndGet();
                if (count % 500 == 0) {
                    System.out.println("    Processed " + count + " / " + types.size() + " classes");
                }
                
            } catch (Exception e) {
                astErrors.incrementAndGet();
                if (astErrors.get() <= 10) {
                    System.err.println("    Error: " + type.getQualifiedName() + " - " + e.getMessage());
                }
            }
        });
        
        // Insert remaining batch
        if (USE_MONGODB && !batch.isEmpty()) {
            astCollection.insertMany(batch);
        }
        
        System.out.println("  ✅ AST serialized: " + astCount.get() + " classes" +
                (astErrors.get() > 0 ? " (" + astErrors.get() + " errors)" : ""));
        
        // ── PASS 2: Call Graph ───────────────────────────────────────────────────
        System.out.println("\n🔗 Pass 2: Extracting dependency graph...");
        long graphStart = System.currentTimeMillis();
        
        extractCallGraph(model);
        
        System.out.println("  ✅ Graph extracted in: " + (System.currentTimeMillis() - graphStart) + "ms");
        
        // ── Write dependency_graph.json ─────────────────────────────────────────
        System.out.println("\n💾 Writing dependency graph...");
        
        ObjectNode rootNode = mapper.createObjectNode();
        ObjectNode nodesObj = rootNode.putObject("nodes");
        
        // Convert to JSON-friendly format
        nodesObj.set("methods", mapper.valueToTree(methods.values().stream()
                .map(m -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", m.id);
                    map.put("simpleName", m.simpleName);
                    map.put("className", m.className);
                    map.put("packageName", m.packageName);
                    map.put("returnType", m.returnType);
                    map.put("isPublic", m.isPublic);
                    map.put("isStatic", m.isStatic);
                    map.put("cyclomaticComplexity", m.cyclomaticComplexity);
                    map.put("lineStart", m.lineStart);
                    map.put("lineEnd", m.lineEnd);
                    map.put("astDocId", m.astDocId);
                    map.put("isExternal", m.isExternal);
                    return map;
                }).collect(Collectors.toList())));
        
        nodesObj.set("classes", mapper.valueToTree(classes.values()));
        nodesObj.set("packages", mapper.valueToTree(packages.values()));
        rootNode.set("edges", mapper.valueToTree(edges));
        
        // Stats
        ObjectNode stats = rootNode.putObject("stats");
        stats.put("totalClasses", classes.size());
        stats.put("totalMethods", methods.values().stream().filter(m -> !m.isExternal).count());
        stats.put("totalExternalMethods", methods.values().stream().filter(m -> m.isExternal).count());
        stats.put("totalPackages", packages.size());
        stats.put("totalEdges", edges.size());
        stats.put("astFilesWritten", astCount.get());
        stats.put("processingTimeMs", System.currentTimeMillis() - startTime);
        
        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(new File(graphJson), rootNode);
        
        // ── Write method_index.json (for fast LLM lookup) ───────────────────────
        System.out.println("📇 Writing method index...");
        
        ObjectNode methodIndex = mapper.createObjectNode();
        for (MethodNode mn : methods.values()) {
            if (mn.isExternal) continue;
            ObjectNode entry = methodIndex.putObject(mn.id);
            entry.put("astDocId", mn.astDocId);
            entry.put("className", mn.className);
            entry.put("methodName", mn.simpleName);
            entry.put("lineStart", mn.lineStart);
            entry.put("lineEnd", mn.lineEnd);
            entry.put("cyclomaticComplexity", mn.cyclomaticComplexity);
            entry.put("branchCount", mn.branchCount);
            entry.put("loopCount", mn.loopCount);
            entry.put("hasNullCheck", mn.hasNullCheck);
            entry.put("hasInstanceofCheck", mn.hasInstanceofCheck);
            
            // Store code snippets for quick LLM access
            if (mn.methodBody != null) {
                entry.put("body", mn.methodBody.length() > 5000 ? 
                        mn.methodBody.substring(0, 5000) + "..." : mn.methodBody);
            }
            if (mn.conditions != null) {
                entry.set("conditions", mapper.valueToTree(mn.conditions));
            }
        }
        
        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(new File(methodIndexPath), methodIndex);
        
        // ── Summary ─────────────────────────────────────────────────────────────
        long totalTime = System.currentTimeMillis() - startTime;
        
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                    PROCESSING COMPLETE                    ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ Total time:        " + String.format("%.2f", totalTime / 1000.0) + " seconds");
        System.out.println("║ Classes processed: " + astCount.get());
        System.out.println("║ Methods indexed:   " + methods.values().stream().filter(m -> !m.isExternal).count());
        System.out.println("║ Graph edges:       " + edges.size());
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ Outputs:                                                  ║");
        System.out.println("║   - dependency_graph.json                                 ║");
        System.out.println("║   - method_index.json                                     ║");
        if (USE_MONGODB) {
            System.out.println("║   - MongoDB: harmony_codebase.ast_nodes                ║");
        }
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        
        // Cleanup
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
    
    static boolean containsJava(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return false;
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(".java")) return true;
            if (f.isDirectory() && containsJava(f)) return true;
        }
        return false;
    }
}