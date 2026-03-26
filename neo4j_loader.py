"""
neo4j_loader.py
───────────────
Loads dependency_graph.json into Neo4j AuraDB (free cloud).
Every Method and Class node gets an astDocId property that
links to the corresponding MongoDB AST document.

Setup:
    pip install neo4j

Usage:
    python neo4j_loader.py
"""

import json, time
from neo4j import GraphDatabase

# ── CONFIG ────────────────────────────────────────────────────────────────────
NEO4J_URI      = "neo4j+s://<your-instance>.databases.neo4j.io"
NEO4J_USER     = "neo4j"
NEO4J_PASSWORD = "<your-password>"
JSON_PATH      = r"C:\Users\PrabalBhardwaj\Desktop\osttra-harmony3\dependency_graph.json"
BATCH_SIZE     = 500
# ─────────────────────────────────────────────────────────────────────────────


def load_json(path):
    print(f"Loading {path}...")
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def batch(lst, size):
    for i in range(0, len(lst), size):
        yield lst[i:i + size]


def create_constraints(session):
    print("Creating constraints...")
    for cypher in [
        "CREATE CONSTRAINT IF NOT EXISTS FOR (m:Method)         REQUIRE m.id IS UNIQUE",
        "CREATE CONSTRAINT IF NOT EXISTS FOR (c:Class)          REQUIRE c.id IS UNIQUE",
        "CREATE CONSTRAINT IF NOT EXISTS FOR (p:Package)        REQUIRE p.qualifiedName IS UNIQUE",
        "CREATE CONSTRAINT IF NOT EXISTS FOR (e:ExternalMethod) REQUIRE e.id IS UNIQUE",
    ]:
        session.run(cypher)
    print("  ✓ done")


def load_packages(session, packages):
    print(f"  Packages : {len(packages)}")
    for chunk in batch(packages, BATCH_SIZE):
        session.run("""
            UNWIND $rows AS row
            MERGE (p:Package {qualifiedName: row.qualifiedName})
            SET p.simpleName = row.simpleName
        """, rows=chunk)


def load_classes(session, classes):
    print(f"  Classes  : {len(classes)}")
    for chunk in batch(classes, BATCH_SIZE):
        session.run("""
            UNWIND $rows AS row
            MERGE (c:Class {id: row.id})
            SET c.qualifiedName = row.qualifiedName,
                c.simpleName    = row.simpleName,
                c.packageName   = row.packageName,
                c.isInterface   = row.isInterface,
                c.isAbstract    = row.isAbstract,
                c.isEnum        = row.isEnum,
                c.astDocId      = row.astDocId
        """, rows=chunk)


def load_methods(session, methods):
    internal = [m for m in methods if not m.get("isExternal", False)]
    external = [m for m in methods if     m.get("isExternal", False)]
    print(f"  Methods  : {len(internal)} internal, {len(external)} external")

    for chunk in batch(internal, BATCH_SIZE):
        session.run("""
            UNWIND $rows AS row
            MERGE (m:Method {id: row.id})
            SET m.qualifiedName = row.qualifiedName,
                m.simpleName    = row.simpleName,
                m.className     = row.className,
                m.packageName   = row.packageName,
                m.returnType    = row.returnType,
                m.paramTypes    = row.paramTypes,
                m.isPublic      = row.isPublic,
                m.isPrivate     = row.isPrivate,
                m.isProtected   = row.isProtected,
                m.isStatic      = row.isStatic,
                m.isAbstract    = row.isAbstract,
                m.isConstructor = row.isConstructor,
                m.lineNumber    = row.lineNumber,
                m.astDocId      = row.astDocId
        """, rows=chunk)

    for chunk in batch(external, BATCH_SIZE):
        session.run("""
            UNWIND $rows AS row
            MERGE (e:ExternalMethod {id: row.id})
            SET e.qualifiedName = row.qualifiedName,
                e.simpleName    = row.simpleName,
                e.className     = row.className,
                e.packageName   = row.packageName
        """, rows=chunk)


EDGE_CYPHER = {
    "BELONGS_TO": """
        UNWIND $rows AS row
        MATCH (c:Class   {id: row.fromId})
        MATCH (p:Package {qualifiedName: row.toId})
        MERGE (c)-[:BELONGS_TO]->(p)
    """,
    "EXTENDS": """
        UNWIND $rows AS row
        MATCH (a:Class {id: row.fromId})
        MERGE (b:Class {id: row.toId})
          ON CREATE SET b.qualifiedName = row.toId,
                        b.simpleName    = last(split(row.toId,'.'))
        MERGE (a)-[:EXTENDS]->(b)
    """,
    "IMPLEMENTS": """
        UNWIND $rows AS row
        MATCH (a:Class {id: row.fromId})
        MERGE (b:Class {id: row.toId})
          ON CREATE SET b.qualifiedName = row.toId,
                        b.simpleName    = last(split(row.toId,'.')),
                        b.isInterface   = true
        MERGE (a)-[:IMPLEMENTS]->(b)
    """,
    "HAS_METHOD": """
        UNWIND $rows AS row
        MATCH (c:Class  {id: row.fromId})
        MATCH (m:Method {id: row.toId})
        MERGE (c)-[:HAS_METHOD]->(m)
    """,
    "OVERRIDES": """
        UNWIND $rows AS row
        MATCH (child:Method  {id: row.fromId})
        MATCH (parent:Method {id: row.toId})
        MERGE (child)-[:OVERRIDES]->(parent)
    """,
    "CALLS": """
        UNWIND $rows AS row
        MATCH (caller:Method {id: row.fromId})
        MATCH (callee:Method {id: row.toId})
        MERGE (caller)-[r:CALLS]->(callee)
        SET r.lineNumber = row.props.lineNumber,
            r.callCount  = row.props.callCount
    """,
    "CALLS_EXTERNAL": """
        UNWIND $rows AS row
        MATCH (caller:Method         {id: row.fromId})
        MATCH (callee:ExternalMethod {id: row.toId})
        MERGE (caller)-[r:CALLS_EXTERNAL]->(callee)
        SET r.lineNumber = row.props.lineNumber,
            r.callCount  = row.props.callCount
    """,
}


def load_edges(session, edges):
    by_type = {}
    for e in edges:
        by_type.setdefault(e["type"], []).append(e)
    for etype, rows in by_type.items():
        cypher = EDGE_CYPHER.get(etype)
        if not cypher:
            print(f"  ⚠ Unknown edge type '{etype}' — skipping {len(rows)}")
            continue
        print(f"  {etype:<20} : {len(rows)}")
        for chunk in batch(rows, BATCH_SIZE):
            session.run(cypher, rows=chunk)


def main():
    t0   = time.time()
    data = load_json(JSON_PATH)

    print("\nJSON stats:")
    for k, v in data["stats"].items():
        print(f"  {k}: {v}")

    driver = GraphDatabase.driver(NEO4J_URI, auth=(NEO4J_USER, NEO4J_PASSWORD))
    with driver.session() as s:
        create_constraints(s)
        print("\nLoading nodes...")
        load_packages(s, data["nodes"]["packages"])
        load_classes(s,  data["nodes"]["classes"])
        load_methods(s,  data["nodes"]["methods"])
        print("\nLoading edges...")
        load_edges(s, data["edges"])

    driver.close()
    print(f"\n✅ Neo4j load complete in {time.time()-t0:.1f}s")


if __name__ == "__main__":
    main()

