"""
mongo_loader.py
───────────────
Loads all AST JSON files from the ast/ directory into MongoDB Atlas (free).

Setup:
    pip install pymongo

Get a free cluster at mongodb.com/atlas → grab your connection string.

Usage:
    python mongo_loader.py
"""

import json, time, os
from pathlib import Path
from pymongo import MongoClient, UpdateOne
from pymongo.errors import BulkWriteError

# ── CONFIG ────────────────────────────────────────────────────────────────────
MONGO_URI   = "mongodb+srv://<user>:<password>@<cluster>.mongodb.net/?retryWrites=true&w=majority"
DB_NAME     = "harmony_codebase"
COLLECTION  = "ast_nodes"          # one document per class
AST_DIR     = r"C:\Users\PrabalBhardwaj\Desktop\osttra-harmony3\ast"
BATCH_SIZE  = 200
# ─────────────────────────────────────────────────────────────────────────────


def load_ast_files(ast_dir: str) -> list[dict]:
    docs = []
    path = Path(ast_dir)
    files = list(path.glob("*.json"))
    print(f"Found {len(files)} AST files in {ast_dir}")
    for f in files:
        try:
            with open(f, "r", encoding="utf-8") as fh:
                docs.append(json.load(fh))
        except Exception as e:
            print(f"  ⚠ Could not read {f.name}: {e}")
    return docs


def batch(lst, size):
    for i in range(0, len(lst), size):
        yield lst[i:i + size]


def upsert_batch(collection, docs: list[dict]):
    """Upsert on _id so re-runs are idempotent."""
    ops = [
        UpdateOne({"_id": doc["_id"]}, {"$set": doc}, upsert=True)
        for doc in docs
    ]
    try:
        result = collection.bulk_write(ops, ordered=False)
        return result.upserted_count + result.modified_count
    except BulkWriteError as bwe:
        print(f"  ⚠ Bulk write error: {bwe.details}")
        return 0


def create_indexes(collection):
    """Indexes that speed up the most common AST queries."""
    print("Creating indexes...")
    collection.create_index("qualifiedName",           background=True)
    collection.create_index("packageName",             background=True)
    collection.create_index("methods.callGraphId",     background=True)
    collection.create_index("methods.simpleName",      background=True)
    collection.create_index("fields.name",             background=True)
    print("  ✓ Indexes ready")


def print_sample_queries():
    print("""
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
SAMPLE MongoDB QUERIES (run in Atlas UI or mongosh)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

// 1. Find AST for a specific class
db.ast_nodes.findOne({ qualifiedName: "com.example.MyClass" })

// 2. All classes that have a field named 'connection'
db.ast_nodes.find({ "fields.name": "connection" }, { qualifiedName:1 })

// 3. Methods containing try-catch blocks
db.ast_nodes.find(
  { "methods.tryCatchBlocks.0": { $exists: true } },
  { qualifiedName:1, "methods.simpleName":1 }
)

// 4. All methods with more than 5 local variables (complexity signal)
db.ast_nodes.aggregate([
  { $unwind: "$methods" },
  { $addFields: { localVarCount: { $size: "$methods.localVariables" } } },
  { $match: { localVarCount: { $gt: 5 } } },
  { $project: { qualifiedName:1, "methods.simpleName":1, localVarCount:1 } },
  { $sort: { localVarCount: -1 } }
])

// 5. Find all methods that catch a specific exception type
db.ast_nodes.find(
  { "methods.tryCatchBlocks.catchTypes": "java.sql.SQLException" },
  { qualifiedName:1 }
)

// 6. Methods with no return statement (void or always throws)
db.ast_nodes.find(
  {
    "methods": {
      $elemMatch: {
        returnType: { $ne: "void" },
        "returnStatements": { $size: 0 },
        hasBody: true
      }
    }
  }
)

// 7. Classes with more than 10 fields (god-class smell)
db.ast_nodes.aggregate([
  { $addFields: { fieldCount: { $size: "$fields" } } },
  { $match:     { fieldCount: { $gt: 10 } } },
  { $project:   { qualifiedName:1, fieldCount:1 } },
  { $sort:      { fieldCount: -1 } }
])

// 8. Full-text search on invocation expressions (find SQL usage)
db.ast_nodes.find(
  { "methods.invocations.expression": { $regex: "prepareStatement", $options:"i" } },
  { qualifiedName:1 }
)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
""")


def main():
    t0   = time.time()
    docs = load_ast_files(AST_DIR)
    if not docs:
        print("No AST files found. Run Main.java first.")
        return

    print(f"Connecting to MongoDB...")
    client     = MongoClient(MONGO_URI)
    db         = client[DB_NAME]
    collection = db[COLLECTION]

    create_indexes(collection)

    total = 0
    print(f"\nUploading {len(docs)} class AST documents in batches of {BATCH_SIZE}...")
    for i, chunk in enumerate(batch(docs, BATCH_SIZE)):
        count = upsert_batch(collection, chunk)
        total += count
        print(f"  Batch {i+1}: {count} upserted")

    client.close()
    print(f"\n✅ Done — {total} documents upserted in {time.time()-t0:.1f}s")
    print_sample_queries()


if __name__ == "__main__":
    main()
