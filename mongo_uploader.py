import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;

/**
 * MongoDB Storage Handler - Optimized for Method-Level Queries
 * Collections:
 *   - classes: Class-level metadata
 *   - methods: Individual method documents (indexed by method ID)
 *   - method_index: Quick lookup for JaCoCo integration
 */
public class MongoDBStorage implements AutoCloseable {
    
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> classesCollection;
    private final MongoCollection<Document> methodsCollection;
    private final MongoCollection<Document> methodIndexCollection;
    private final ObjectMapper mapper;
    
    public MongoDBStorage(String uri, String databaseName) {
        this.mapper = new ObjectMapper();
        
        try {
            this.mongoClient = MongoClients.create(uri);
            this.database = mongoClient.getDatabase(databaseName);
            this.classesCollection = database.getCollection("classes");
            this.methodsCollection = database.getCollection("methods");
            this.methodIndexCollection = database.getCollection("method_index");
            
            // Test connection
            database.runCommand(new Document("ping", 1));
            System.out.println("✅ Connected to MongoDB at: " + uri);
            System.out.println("📁 Database: " + databaseName);
            System.out.println("📚 Collections: classes, methods, method_index");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to MongoDB: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create indexes for efficient queries
     */
    public void createIndexes() {
        System.out.println("\n📇 Creating MongoDB indexes...");
        
        // Classes collection indexes
        try {
            classesCollection.createIndex(Indexes.ascending("qualifiedName"));
            classesCollection.createIndex(Indexes.ascending("packageName"));
            classesCollection.createIndex(Indexes.ascending("sourceFile"));
            System.out.println("  ✓ Classes collection indexes created");
        } catch (Exception e) {
            System.err.println("  ⚠️ Classes indexes error: " + e.getMessage());
        }
        
        // Methods collection indexes (MOST IMPORTANT)
        try {
            methodsCollection.createIndex(Indexes.ascending("_id"));
            methodsCollection.createIndex(Indexes.ascending("id"));
            methodsCollection.createIndex(Indexes.ascending("className"));
            methodsCollection.createIndex(Indexes.ascending("methodName"));
            methodsCollection.createIndex(Indexes.ascending("lineStart", "lineEnd"));
            methodsCollection.createIndex(Indexes.ascending("astDocId"));
            methodsCollection.createIndex(Indexes.ascending("externalCalls"));
            System.out.println("  ✓ Methods collection indexes created");
        } catch (Exception e) {
            System.err.println("  ⚠️ Methods indexes error: " + e.getMessage());
        }
        
        // Method index collection indexes
        try {
            methodIndexCollection.createIndex(Indexes.ascending("id"));
            methodIndexCollection.createIndex(Indexes.ascending("className"));
            methodIndexCollection.createIndex(Indexes.ascending("methodName"));
            methodIndexCollection.createIndex(Indexes.ascending("lineStart"));
            System.out.println("  ✓ Method index collection indexes created");
        } catch (Exception e) {
            System.err.println("  ⚠️ Method index indexes error: " + e.getMessage());
        }
        
        System.out.println("✅ MongoDB indexes ready");
    }
    
    /**
     * Upload AST JSON files from directory to MongoDB
     * @param astDirectory Path to AST JSON files
     * @param methodIndexPath Path to method_index.json
     * @param batchSize Number of files to process in batch
     * @param skipExisting Skip existing documents
     */
    public void uploadFromDirectories(String astDirectory, String methodIndexPath, 
                                       int batchSize, boolean skipExisting) {
        System.out.println("\n📁 Processing AST files from: " + astDirectory);
        
        // First, upload the method index
        uploadMethodIndex(methodIndexPath);
        
        // Then upload AST files
        uploadASTDirectory(astDirectory, batchSize, skipExisting);
    }
    
    /**
     * Upload all AST JSON files from a directory
     */
    public void uploadASTDirectory(String astDirectory, int batchSize, boolean skipExisting) {
        Path astPath = Paths.get(astDirectory);
        
        if (!Files.exists(astPath)) {
            System.err.println("❌ Directory not found: " + astDirectory);
            return;
        }
        
        try {
            // Get all JSON files
            List<Path> jsonFiles;
            try (Stream<Path> paths = Files.walk(astPath)) {
                jsonFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .toList();
            }
            
            System.out.println("\n📁 Found " + jsonFiles.size() + " AST JSON files");
            
            // Get existing documents if skipping
            Set<String> existingIds = new HashSet<>();
            if (skipExisting) {
                classesCollection.find().forEach(doc -> existingIds.add(doc.getString("_id")));
                System.out.println("  Found " + existingIds.size() + " existing class documents");
            }
            
            // Filter files to upload
            List<Path> filesToUpload = new ArrayList<>();
            for (Path file : jsonFiles) {
                String className = file.getFileName().toString().replace(".json", "").replace("_", ".");
                if (!skipExisting || !existingIds.contains(className)) {
                    filesToUpload.add(file);
                }
            }
            
            System.out.println("  Uploading " + filesToUpload.size() + " new files");
            
            if (filesToUpload.isEmpty()) {
                System.out.println("✅ All files already in MongoDB");
                return;
            }
            
            // Upload in batches
            int successCount = 0;
            int errorCount = 0;
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < filesToUpload.size(); i += batchSize) {
                int end = Math.min(i + batchSize, filesToUpload.size());
                List<Path> batch = filesToUpload.subList(i, end);
                
                List<Document> classDocs = new ArrayList<>();
                List<Document> methodDocs = new ArrayList<>();
                
                for (Path file : batch) {
                    try {
                        ObjectNode astDoc = (ObjectNode) mapper.readTree(file.toFile());
                        
                        // Store class document
                        Document classDoc = Document.parse(mapper.writeValueAsString(astDoc));
                        classDocs.add(classDoc);
                        
                        // Store individual methods
                        if (astDoc.has("methods") && astDoc.get("methods").isArray()) {
                            for (JsonNode methodNode : astDoc.get("methods")) {
                                Document methodDoc = Document.parse(mapper.writeValueAsString(methodNode));
                                methodDoc.put("astDocId", astDoc.get("_id").asText());
                                methodDocs.add(methodDoc);
                            }
                        }
                        
                        successCount++;
                        
                    } catch (Exception e) {
                        errorCount++;
                        System.err.println("  Error processing " + file.getFileName() + ": " + e.getMessage());
                    }
                }
                
                // Batch insert classes
                if (!classDocs.isEmpty()) {
                    for (Document classDoc : classDocs) {
                        String classId = classDoc.getString("_id");
                        Bson filter = Filters.eq("_id", classId);
                        classesCollection.replaceOne(filter, classDoc, new UpdateOptions().upsert(true));
                    }
                }
                
                // Batch insert methods
                if (!methodDocs.isEmpty()) {
                    List<WriteModel<Document>> writes = new ArrayList<>();
                    for (Document methodDoc : methodDocs) {
                        String methodId = methodDoc.getString("id");
                        Bson filter = Filters.eq("_id", methodId);
                        writes.add(new UpdateOneModel<>(filter, new Document("$set", methodDoc), 
                                        new UpdateOptions().upsert(true)));
                    }
                    methodsCollection.bulkWrite(writes);
                }
                
                System.out.println("  Batch " + (i / batchSize + 1) + "/" + 
                    ((filesToUpload.size() + batchSize - 1) / batchSize) + 
                    " completed (" + successCount + "/" + (successCount + errorCount) + ")");
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("\n📊 AST Upload Complete:");
            System.out.println("  ✅ Success: " + successCount);
            System.out.println("  ❌ Errors: " + errorCount);
            System.out.println("  ⏱️  Time: " + (elapsed / 1000.0) + " seconds");
            System.out.println("  📈 Rate: " + (successCount / (elapsed / 1000.0)) + " files/sec");
            
        } catch (Exception e) {
            System.err.println("❌ Failed to read directory: " + e.getMessage());
        }
    }
    
    /**
     * Upload method index to MongoDB
     */
    public void uploadMethodIndex(String methodIndexPath) {
        System.out.println("\n📇 Uploading method index from: " + methodIndexPath);
        
        try {
            File indexFile = new File(methodIndexPath);
            if (!indexFile.exists()) {
                System.err.println("❌ Method index file not found: " + methodIndexPath);
                return;
            }
            
            ObjectNode methodIndex = (ObjectNode) mapper.readTree(indexFile);
            
            // Clear existing method index
            methodIndexCollection.drop();
            System.out.println("  Cleared existing method index");
            
            // Convert to list of documents
            List<Document> docs = new ArrayList<>();
            methodIndex.fields().forEachRemaining(entry -> {
                String methodId = entry.getKey();
                JsonNode methodData = entry.getValue();
                
                try {
                    Document doc = Document.parse(mapper.writeValueAsString(methodData));
                    doc.put("_id", methodId);
                    doc.put("id", methodId);
                    docs.add(doc);
                } catch (Exception e) {
                    System.err.println("  Error parsing method " + methodId + ": " + e.getMessage());
                }
            });
            
            // Insert in batches
            int batchSize = 500;
            for (int i = 0; i < docs.size(); i += batchSize) {
                int end = Math.min(i + batchSize, docs.size());
                List<Document> batch = docs.subList(i, end);
                methodIndexCollection.insertMany(batch);
                System.out.println("  Uploaded " + (i + batch.size()) + " / " + docs.size() + " methods");
            }
            
            System.out.println("✅ Method index uploaded: " + docs.size() + " methods");
            
        } catch (Exception e) {
            System.err.println("❌ Failed to upload method index: " + e.getMessage());
        }
    }
    
    /**
     * Query: Get a single method by ID (FAST - uses index)
     */
    public Document findMethodById(String methodId) {
        return methodsCollection.find(Filters.eq("_id", methodId)).first();
    }
    
    /**
     * Query: Get all methods in a class
     */
    public List<Document> findMethodsByClass(String className) {
        List<Document> methods = new ArrayList<>();
        methodsCollection.find(Filters.eq("className", className))
            .forEach(methods::add);
        return methods;
    }
    
    /**
     * Query: Get methods by line range (for JaCoCo)
     */
    public List<Document> findMethodsByLineRange(int lineStart, int lineEnd) {
        Bson filter = Filters.and(
            Filters.lte("lineStart", lineEnd),
            Filters.gte("lineEnd", lineStart)
        );
        
        List<Document> methods = new ArrayList<>();
        methodsCollection.find(filter).forEach(methods::add);
        return methods;
    }
    
    /**
     * Get method body only (minimal data transfer)
     */
    public String getMethodBody(String methodId) {
        Document method = methodsCollection.find(Filters.eq("_id", methodId))
            .projection(new Document("body", 1).append("_id", 0))
            .first();
        return method != null ? method.getString("body") : null;
    }
    
    /**
     * Get method dependencies only (for mock generation)
     */
    public Document getMethodDependencies(String methodId) {
        return methodsCollection.find(Filters.eq("_id", methodId))
            .projection(new Document("externalCalls", 1)
                .append("siblingCalls", 1)
                .append("mockCandidates", 1)
                .append("_id", 0))
            .first();
    }
    
    /**
     * Get collection statistics
     */
    public void printStats() {
        System.out.println("\n📊 MongoDB Statistics:");
        System.out.println("  Classes: " + classesCollection.countDocuments());
        System.out.println("  Methods: " + methodsCollection.countDocuments());
        System.out.println("  Method Index: " + methodIndexCollection.countDocuments());
    }
    
    /**
     * Drop all collections
     */
    public void dropAllCollections() {
        System.out.println("\n⚠️  Dropping all collections...");
        classesCollection.drop();
        methodsCollection.drop();
        methodIndexCollection.drop();
        System.out.println("✅ Collections dropped");
    }
    
    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("\n🔌 MongoDB connection closed");
        }
    }
    
    /**
     * Main method - Now accepts command line arguments or uses defaults
     * 
     * Usage:
     *   java MongoDBStorage [astDirectory] [methodIndexPath] [mongoUri] [databaseName]
     *   
     * Examples:
     *   java MongoDBStorage
     *   java MongoDBStorage "C:/path/to/ast/" "C:/path/to/method_index.json"
     *   java MongoDBStorage "./ast/" "./method_index.json" "mongodb://localhost:27017" "harmony_codebase"
     */
    public static void main(String[] args) {
        // Default paths (change these to match your setup)
        String defaultAstDirectory = "C:/Users/PrabalBhardwaj/Desktop/osttra-harmony3/ast/";
        String defaultMethodIndexPath = "C:/Users/PrabalBhardwaj/Desktop/osttra-harmony3/method_index.json";
        String defaultMongoUri = "mongodb://localhost:27017";
        String defaultDatabaseName = "harmony_codebase";
        
        // Parse command line arguments
        String astDirectory = args.length > 0 ? args[0] : defaultAstDirectory;
        String methodIndexPath = args.length > 1 ? args[1] : defaultMethodIndexPath;
        String mongoUri = args.length > 2 ? args[2] : defaultMongoUri;
        String databaseName = args.length > 3 ? args[3] : defaultDatabaseName;
        
        // Also support environment variables
        astDirectory = System.getenv("AST_DIR") != null ? System.getenv("AST_DIR") : astDirectory;
        methodIndexPath = System.getenv("METHOD_INDEX_PATH") != null ? 
                          System.getenv("METHOD_INDEX_PATH") : methodIndexPath;
        
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║     MongoDB Storage - Optimized for Method Queries      ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println("\n📁 Configuration:");
        System.out.println("  AST Directory: " + astDirectory);
        System.out.println("  Method Index: " + methodIndexPath);
        System.out.println("  MongoDB URI: " + mongoUri);
        System.out.println("  Database: " + databaseName);
        
        try (MongoDBStorage storage = new MongoDBStorage(mongoUri, databaseName)) {
            // Create indexes
            storage.createIndexes();
            
            // Upload data
            storage.uploadFromDirectories(astDirectory, methodIndexPath, 100, true);
            
            // Print statistics
            storage.printStats();
            
            System.out.println("\n✅ Upload complete!");
            System.out.println("\n🔍 Example queries you can run in MongoDB shell:");
            System.out.println("  // Find a method by ID");
            System.out.println("  db.methods.findOne({_id: \"com.example.MyClass#methodName\"})");
            System.out.println("  ");
            System.out.println("  // Find all methods in a class");
            System.out.println("  db.methods.find({className: \"com.example.MyClass\"})");
            System.out.println("  ");
            System.out.println("  // Find methods with external calls");
            System.out.println("  db.methods.find({externalCalls: {$exists: true, $ne: []}})");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}