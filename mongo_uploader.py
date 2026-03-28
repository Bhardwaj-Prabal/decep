import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class MongoAtlasUploader {
    
    private final MongoClient client;
    private final MongoCollection<Document> collection;
    private final ObjectMapper mapper = new ObjectMapper();
    
    public MongoAtlasUploader(String uri, String database, String collection) {
        this.client = MongoClients.create(uri);
        MongoDatabase db = client.getDatabase(database);
        this.collection = db.getCollection(collection);
        System.out.println("✅ Connected to MongoDB Atlas");
    }
    
    public void uploadAll(String astDir, int batchSize) throws Exception {
        Path astPath = Paths.get(astDir);
        
        try (Stream<Path> paths = Files.walk(astPath)) {
            List<Path> jsonFiles = paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .toList();
            
            System.out.println("📁 Found " + jsonFiles.size() + " AST JSON files");
            
            List<Document> batch = new ArrayList<>();
            int count = 0;
            
            for (Path file : jsonFiles) {
                Document doc = Document.parse(Files.readString(file));
                
                // Ensure _id is set
                if (!doc.containsKey("_id") && doc.containsKey("qualifiedName")) {
                    doc.put("_id", doc.getString("qualifiedName"));
                }
                
                // Add metadata
                doc.put("_uploaded_at", System.currentTimeMillis());
                doc.put("_source_file", file.toString());
                
                batch.add(doc);
                
                if (batch.size() >= batchSize) {
                    collection.insertMany(batch);
                    count += batch.size();
                    System.out.println("  Uploaded " + count + " documents...");
                    batch.clear();
                }
            }
            
            // Upload remaining
            if (!batch.isEmpty()) {
                collection.insertMany(batch);
                count += batch.size();
                System.out.println("  Uploaded " + count + " documents...");
            }
            
            System.out.println("✅ Upload complete! Total: " + count + " documents");
            
            // Verify
            long total = collection.countDocuments();
            System.out.println("📊 Total documents in collection: " + total);
        }
    }
    
    public void close() {
        if (client != null) {
            client.close();
            System.out.println("🔌 Connection closed");
        }
    }
    
    public static void main(String[] args) throws Exception {
        String uri = System.getenv("MONGODB_URI");
        if (uri == null) {
            System.err.println("Please set MONGODB_URI environment variable");
            System.exit(1);
        }
        
        String astDir = "C:/Users/PrabalBhardwaj/Desktop/osttra-harmony3/ast/";
        
        MongoAtlasUploader uploader = new MongoAtlasUploader(uri, "harmony_codebase", "ast_nodes");
        
        try {
            uploader.uploadAll(astDir, 100);
        } finally {
            uploader.close();
        }
    }
}