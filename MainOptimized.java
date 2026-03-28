import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.*;
import spoon.reflect.visitor.CtScanner;
import spoon.reflect.visitor.filter.TypeFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OPTIMIZED Coverage-Analysis Extractor
 * 
 * Focuses ONLY on what JaCoCo doesn't provide:
 * - Full method code (AST as JSON)
 * - Method dependencies (call graph)
 * - Mock candidates (fields that need mocking)
 * - Control flow structure (conditions, loops, returns)
 * - Complete external calls with full signatures
 * 
 * JaCoCo already provides:
 * - Cyclomatic complexity
 * - Line/branch coverage
 * - Method coverage
 * 
 * Expected runtime: 15-30 minutes
 */
public class MainOptimized {

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIGURATION
    // ─────────────────────────────────────────────────────────────────────────
    
    private static final int BATCH_PROGRESS = 500;  // Progress report every 500 classes
    private static final int MAX_BODY_LENGTH = 50000;  // Only truncate if body exceeds this
    
    // ─────────────────────────────────────────────────────────────────────────
    // MODELS
    // ─────────────────────────────────────────────────────────────────────────
    
    static class MethodNode {
        public String id, simpleName, className, packageName;
        public String returnType;
        public List<String> paramTypes = new ArrayList<>();
        public List<String> paramNames = new ArrayList<>();
        public List<String> thrownTypes = new ArrayList<>();
        public List<String> annotations = new ArrayList<>();
        
        // Modifiers (from Spoon)
        public boolean isPublic, isPrivate, isProtected;
        public boolean isStatic, isAbstract, isFinal;
        public boolean isConstructor;
        public boolean isExternal = false;
        
        // Location (for JaCoCo line mapping)
        public int lineStart = -1, lineEnd = -1;
        
        // Code snippets (what LLM needs)
        public String methodBody;           // Full method body as text
        public List<String> conditions;     // All condition expressions
        public List<String> returnPoints;   // All return expressions
        public List<String> nullChecks;     // All null check expressions
        public List<String> instanceChecks; // All instanceof checks
        
        // Dependencies (for test generation) - FULL signatures, never truncated
        public List<String> mockCandidates = new ArrayList<>();  // Fields to mock
        public List<String> siblingCalls = new ArrayList<>();    // Calls to other methods in same class
        public List<String> externalCalls = new ArrayList<>();   // Calls to external services (FULL signatures)
        
        // Cross-links
        public String astDocId;
        public String neo4jId;
    }
    
    static class ClassNode {
        public String id, qualifiedName, simpleName, packageName;
        public boolean isInterface, isAbstract, isEnum;
        public List<String> fieldNames = new ArrayList<>();
        public List<String> fieldTypes = new ArrayList<>();
        public List<String> fieldAnnotations = new ArrayList<>();
        public String superClass;
        public List<String> superInterfaces = new ArrayList<>();
        public List<String> annotations = new ArrayList<>();
        public String astDocId;
        public String sourceFile;
        public int lineStart, lineEnd;
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
        .configure(SerializationFeature.INDENT_OUTPUT, false);
    
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
        if (fqn == null || fqn.equals("UnresolvedType") || fqn.isEmpty()) return true;
        return fqn.startsWith("java.") || fqn.startsWith("javax.") ||
               fqn.startsWith("sun.") || fqn.startsWith("com.sun.") ||
               fqn.startsWith("org.springframework.") || fqn.startsWith("org.hibernate.") ||
               fqn.startsWith("org.apache.") || fqn.startsWith("org.slf4j.") ||
               fqn.startsWith("org.junit.") || fqn.startsWith("org.mockito.");
    }
    
    static boolean isMockCandidate(String fieldType, String fieldName) {
        if (fieldType == null) return false;
        String ft = fieldType.toLowerCase();
        return ft.contains("service") || ft.contains("repository") ||
               ft.contains("dao") || ft.contains("client") ||
               ft.contains("manager") || ft.contains("handler") ||
               ft.contains("provider") || ft.contains("factory") ||
               ft.contains("template") || fieldName.toLowerCase().contains("mock");
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
               t.getSimpleName().isEmpty();
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // CODE EXTRACTION (What LLM needs)
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Extract all condition expressions from method
     */
    static List<String> extractConditions(CtMethod<?> method) {
        List<String> conditions = new ArrayList<>();
        if (method.getBody() == null) return conditions;
        
        method.getBody().accept(new CtScanner() {
            @Override
            public void visitCtIf(CtIf ifElement) {
                String cond = safe(ifElement.getCondition());
                if (cond != null && !cond.isEmpty()) {
                    conditions.add(cond);
                }
                super.visitCtIf(ifElement);
            }
            
            @Override
            public <T> void visitCtConditional(CtConditional<T> conditional) {
                String cond = safe(conditional.getCondition());
                if (cond != null && !cond.isEmpty()) {
                    conditions.add(cond);
                }
                super.visitCtConditional(conditional);
            }
            
            @Override
            public void visitCtWhile(CtWhile whileLoop) {
                String cond = safe(whileLoop.getLoopingExpression());
                if (cond != null && !cond.isEmpty()) {
                    conditions.add(cond);
                }
                super.visitCtWhile(whileLoop);
            }
            
            @Override
            public void visitCtFor(CtFor forLoop) {
                String cond = safe(forLoop.getExpression());
                if (cond != null && !cond.isEmpty()) {
                    conditions.add(cond);
                }
                super.visitCtFor(forLoop);
            }
            
            @Override
            public void visitCtDo(CtDo doLoop) {
                String cond = safe(doLoop.getLoopingExpression());
                if (cond != null && !cond.isEmpty()) {
                    conditions.add(cond);
                }
                super.visitCtDo(doLoop);
            }
        });
        
        return conditions;
    }
    
    /**
     * Extract all return expressions
     */
    static List<String> extractReturns(CtMethod<?> method) {
        List<String> returns = new ArrayList<>();
        if (method.getBody() == null) return returns;
        
        method.getBody().accept(new CtScanner() {
            @Override
            public <T> void visitCtReturn(CtReturn<T> returnStmt) {
                String expr = safe(returnStmt.getReturnedExpression());
                if (expr != null) {
                    returns.add(expr);
                } else {
                    returns.add("void");
                }
                super.visitCtReturn(returnStmt);
            }
        });
        
        return returns;
    }
    
    /**
     * Extract all null check expressions
     */
    static List<String> extractNullChecks(CtMethod<?> method) {
        List<String> nullChecks = new ArrayList<>();
        if (method.getBody() == null) return nullChecks;
        
        method.getBody().accept(new CtScanner() {
            @Override
            public <T> void visitCtBinaryOperator(CtBinaryOperator<T> op) {
                String kind = safe(op.getKind());
                if ("EQ".equals(kind) || "NE".equals(kind)) {
                    String left = safe(op.getLeftHandOperand());
                    String right = safe(op.getRightHandOperand());
                    if ("null".equals(left) || "null".equals(right)) {
                        nullChecks.add(left + " " + (kind.equals("EQ") ? "==" : "!=") + " " + right);
                    }
                }
                super.visitCtBinaryOperator(op);
            }
        });
        
        return nullChecks;
    }
    
    /**
     * Extract all instanceof checks
     */
    static List<String> extractInstanceChecks(CtMethod<?> method) {
        List<String> instanceChecks = new ArrayList<>();
        if (method.getBody() == null) return instanceChecks;
        
        method.getBody().accept(new CtScanner() {
            @Override
            public <T> void visitCtBinaryOperator(CtBinaryOperator<T> op) {
                String kind = safe(op.getKind());
                if ("INSTANCEOF".equals(kind)) {
                    String left = safe(op.getLeftHandOperand());
                    String right = safe(op.getRightHandOperand());
                    instanceChecks.add(left + " instanceof " + right);
                }
                super.visitCtBinaryOperator(op);
            }
        });
        
        return instanceChecks;
    }
    
    /**
     * Extract mock candidates from fields
     */
    static List<String> extractMockCandidates(CtType<?> type) {
        List<String> candidates = new ArrayList<>();
        for (CtField<?> field : type.getFields()) {
            String fieldType = field.getType() != null ? field.getType().getQualifiedName() : null;
            String fieldName = field.getSimpleName();
            
            if (isMockCandidate(fieldType, fieldName)) {
                candidates.add(fieldName + ":" + fieldType);
            }
        }
        return candidates;
    }
    
    /**
     * Get full method signature including parameter types and return type
     */
    static String getFullMethodSignature(CtExecutableReference<?> exec) {
        StringBuilder fullSig = new StringBuilder();
        
        // Class name
        if (exec.getDeclaringType() != null) {
            fullSig.append(exec.getDeclaringType().getQualifiedName());
        } else {
            fullSig.append("UnknownClass");
        }
        fullSig.append(".");
        
        // Method name
        fullSig.append(exec.getSimpleName());
        fullSig.append("(");
        
        // Parameter types
        List<CtTypeReference<?>> paramTypes = exec.getParameters();
        for (int i = 0; i < paramTypes.size(); i++) {
            if (i > 0) fullSig.append(", ");
            fullSig.append(paramTypes.get(i) != null ? 
                paramTypes.get(i).getQualifiedName() : "unknown");
        }
        fullSig.append(")");
        
        // Return type
        if (exec.getType() != null) {
            fullSig.append(":").append(exec.getType().getQualifiedName());
        }
        
        return fullSig.toString();
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // JSON SERIALIZATION
    // ─────────────────────────────────────────────────────────────────────────
    
    static ObjectNode serializeClassToJson(CtType<?> type) {
        ObjectNode node = mapper.createObjectNode();
        
        // Basic identity
        node.put("_id", type.getQualifiedName());
        node.put("qualifiedName", type.getQualifiedName());
        node.put("simpleName", type.getSimpleName());
        node.put("packageName", type.getPackage() != null ? 
                type.getPackage().getQualifiedName() : "default");
        node.put("isInterface", type instanceof CtInterface);
        node.put("isEnum", type instanceof CtEnum);
        node.put("isAbstract", type.isAbstract());
        
        // Source location
        try {
            if (type.getPosition() != null && type.getPosition().getFile() != null) {
                node.put("sourceFile", type.getPosition().getFile().getAbsolutePath());
                node.put("lineStart", type.getPosition().getLine());
                node.put("lineEnd", type.getPosition().getEndLine());
            }
        } catch (Exception ignored) {}
        
        // Fields (for mock detection)
        ArrayNode fields = node.putArray("fields");
        for (CtField<?> field : type.getFields()) {
            ObjectNode fieldNode = fields.addObject();
            fieldNode.put("name", field.getSimpleName());
            fieldNode.put("type", field.getType() != null ? 
                    field.getType().getQualifiedName() : "unknown");
            fieldNode.put("isStatic", field.isStatic());
            fieldNode.put("isFinal", field.isFinal());
        }
        
        // Annotations
        ArrayNode annotations = node.putArray("annotations");
        for (CtAnnotation<?> ann : type.getAnnotations()) {
            try {
                annotations.add(ann.getAnnotationType().getQualifiedName());
            } catch (Exception ignored) {}
        }
        
        // Inheritance
        try {
            if (type instanceof CtClass) {
                CtTypeReference<?> sup = ((CtClass<?>) type).getSuperclass();
                if (sup != null && !sup.getQualifiedName().equals("java.lang.Object")) {
                    node.put("superClass", sup.getQualifiedName());
                }
            }
        } catch (Exception ignored) {}
        
        ArrayNode interfaces = node.putArray("superInterfaces");
        for (CtTypeReference<?> iface : type.getSuperInterfaces()) {
            try {
                interfaces.add(iface.getQualifiedName());
            } catch (Exception ignored) {}
        }
        
        // Mock candidates (for test generation)
        ArrayNode mockCandidates = node.putArray("mockCandidates");
        for (String candidate : extractMockCandidates(type)) {
            mockCandidates.add(candidate);
        }
        
        // Methods
        ArrayNode methodsNode = node.putArray("methods");
        for (CtMethod<?> method : type.getMethods()) {
            methodsNode.add(serializeMethodToJson(method, type.getQualifiedName()));
        }
        
        return node;
    }
    
    static ObjectNode serializeMethodToJson(CtMethod<?> method, String className) {
        ObjectNode node = mapper.createObjectNode();
        
        // Identity
        node.put("name", method.getSimpleName());
        node.put("signature", method.getSignature());
        node.put("id", methodId(method));
        
        // Return type
        node.put("returnType", method.getType() != null ? 
                method.getType().getQualifiedName() : "void");
        
        // Parameters
        ArrayNode paramTypes = node.putArray("paramTypes");
        ArrayNode paramNames = node.putArray("paramNames");
        for (CtParameter<?> p : method.getParameters()) {
            paramTypes.add(p.getType() != null ? p.getType().getQualifiedName() : "unknown");
            paramNames.add(p.getSimpleName());
        }
        
        // Thrown exceptions
        ArrayNode thrown = node.putArray("thrownTypes");
        for (CtTypeReference<?> t : method.getThrownTypes()) {
            thrown.add(t.getQualifiedName());
        }
        
        // Modifiers
        ObjectNode modifiers = node.putObject("modifiers");
        modifiers.put("public", method.isPublic());
        modifiers.put("private", method.isPrivate());
        modifiers.put("protected", method.isProtected());
        modifiers.put("static", method.isStatic());
        modifiers.put("abstract", method.isAbstract());
        modifiers.put("final", method.isFinal());
        
        // Position (for JaCoCo line mapping)
        if (method.getPosition().isValidPosition()) {
            node.put("lineStart", method.getPosition().getLine());
            node.put("lineEnd", method.getPosition().getEndLine());
        }
        
        // ─── CODE SNIPPETS (What LLM needs for test generation) ───
        
        if (method.getBody() != null) {
            // Full method body - only truncate if ENORMOUS
            String bodyText = method.getBody().toString();
            if (bodyText.length() > MAX_BODY_LENGTH) {
                node.put("body", bodyText.substring(0, MAX_BODY_LENGTH) + 
                        "\n// ... body truncated (exceeds " + MAX_BODY_LENGTH + " chars) ...");
            } else {
                node.put("body", bodyText);
            }
            
            // Conditions (for path analysis)
            ArrayNode conditions = node.putArray("conditions");
            extractConditions(method).forEach(conditions::add);
            
            // Return points
            ArrayNode returns = node.putArray("returnPoints");
            extractReturns(method).forEach(returns::add);
            
            // Null checks
            ArrayNode nullChecks = node.putArray("nullChecks");
            extractNullChecks(method).forEach(nullChecks::add);
            
            // Instanceof checks
            ArrayNode instanceChecks = node.putArray("instanceChecks");
            extractInstanceChecks(method).forEach(instanceChecks::add);
        }
        
        // ─── DEPENDENCIES (NEVER TRUNCATE!) ───
        // These will be filled later in extractCallGraph
        node.putArray("mockCandidates");
        node.putArray("siblingCalls");
        node.putArray("externalCalls");
        
        return node;
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // DEPENDENCY GRAPH EXTRACTION (with FULL external call signatures)
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
            
            try {
                if (type.getPosition() != null && type.getPosition().getFile() != null) {
                    cn.sourceFile = type.getPosition().getFile().getAbsolutePath();
                    cn.lineStart = type.getPosition().getLine();
                    cn.lineEnd = type.getPosition().getEndLine();
                }
            } catch (Exception ignored) {}
            
            for (CtField<?> f : type.getFields()) {
                cn.fieldNames.add(f.getSimpleName());
                cn.fieldTypes.add(f.getType() != null ? 
                        f.getType().getQualifiedName() : "unknown");
            }
            
            for (CtAnnotation<?> a : type.getAnnotations()) {
                try {
                    cn.annotations.add(a.getAnnotationType().getQualifiedName());
                } catch (Exception ignored) {}
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
            List<String> mockCandidates = extractMockCandidates(type);
            
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
                mn.isFinal = method.isFinal();
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
                    mn.paramNames.add(p.getSimpleName());
                }
                
                for (CtTypeReference<?> t : method.getThrownTypes()) {
                    mn.thrownTypes.add(t.getQualifiedName());
                }
                
                for (CtAnnotation<?> a : method.getAnnotations()) {
                    try {
                        mn.annotations.add(a.getAnnotationType().getQualifiedName());
                    } catch (Exception ignored) {}
                }
                
                // Code snippets
                if (method.getBody() != null) {
                    mn.methodBody = method.getBody().toString();
                    mn.conditions = extractConditions(method);
                    mn.returnPoints = extractReturns(method);
                    mn.nullChecks = extractNullChecks(method);
                    mn.instanceChecks = extractInstanceChecks(method);
                }
                
                // Mock candidates from class fields
                mn.mockCandidates.addAll(mockCandidates);
                
                methods.put(mId, mn);
                edges.add(edge(cqn, mId, "HAS_METHOD"));
                
                // Extract calls with FULL signatures
                Set<String> siblingSet = new HashSet<>();
                Set<String> externalSet = new HashSet<>();
                
                try {
                    for (CtInvocation<?> inv : method.getElements(
                            new TypeFilter<>(CtInvocation.class))) {
                        try {
                            String targetClass = "UnresolvedType";
                            CtExecutableReference<?> exec = inv.getExecutable();
                            
                            if (exec.getDeclaringType() != null) {
                                targetClass = exec.getDeclaringType().getQualifiedName();
                            }
                            
                            String methodName = exec.getSimpleName();
                            boolean ext = isExternalClass(targetClass);
                            
                            // Get FULL signature with parameters and return type
                            String fullSignature = getFullMethodSignature(exec);
                            
                            // Check if it's a sibling call (same class)
                            if (targetClass.equals(cqn)) {
                                siblingSet.add(methodName + "()");
                            }
                            
                            if (ext) {
                                // Store FULL signature for external calls
                                externalSet.add(fullSignature);
                            }
                            
                            String targetId = targetClass + "#" + methodName;
                            
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
                
                mn.siblingCalls.addAll(siblingSet);
                mn.externalCalls.addAll(externalSet);
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
        System.out.println("║     (JaCoCo provides complexity, we provide context)     ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
        
        // ── CONFIG ─────────────────────────────────────────────────────────────────
        String rootPath = "C:/Users/PrabalBhardwaj/Desktop/osttra-harmony3/";
        String astDir = rootPath + "ast/";
        String graphJson = rootPath + "dependency_graph.json";
        String methodIndexPath = rootPath + "method_index.json";
        
        new File(astDir).mkdirs();
        
        // Auto-discover source folders
        File root = new File(rootPath);
        List<String> srcFolders = new ArrayList<>();
        for (File f : Objects.requireNonNull(root.listFiles())) {
            if (f.isDirectory() && !f.getName().startsWith(".") && 
                !f.getName().equals("ast") && !f.getName().equals("build") &&
                !f.getName().equals("target") && !f.getName().equals("lib") &&
                !f.getName().equals("Bundles") && !f.getName().equals("Scanned") &&
                !f.getName().equals("deploy")) {
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
        launcher.getEnvironment().setAutoImports(false);
        
        launcher.buildModel();
        CtModel model = launcher.getModel();
        
        System.out.println("  ✅ Model built in: " + (System.currentTimeMillis() - modelStart) + "ms");
        
        // ── PASS 1: AST Serialization ────────────────────────────────────────────
        System.out.println("\n📄 Pass 1: Serializing AST to JSON files...");
        
        AtomicInteger astCount = new AtomicInteger(0);
        AtomicInteger astErrors = new AtomicInteger(0);
        
        List<CtType<?>> types = model.getAllTypes().stream()
                .filter(t -> !isAnonymous(t))
                .collect(java.util.stream.Collectors.toList());
        
        System.out.println("  Total types to process: " + types.size());
        
        // Process sequentially to avoid file write conflicts
        for (CtType<?> type : types) {
            try {
                ObjectNode doc = serializeClassToJson(type);
                
                String fname = type.getQualifiedName().replace(".", "_") + ".json";
                mapper.writerWithDefaultPrettyPrinter()
                      .writeValue(new File(astDir + fname), doc);
                
                int count = astCount.incrementAndGet();
                if (count % BATCH_PROGRESS == 0) {
                    System.out.println("    Processed " + count + " / " + types.size() + " classes");
                }
                
            } catch (Exception e) {
                astErrors.incrementAndGet();
                if (astErrors.get() <= 10) {
                    System.err.println("    Error: " + type.getQualifiedName() + " - " + e.getMessage());
                }
            }
        }
        
        System.out.println("  ✅ AST files written: " + astCount.get() + 
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
        List<Map<String, Object>> methodList = new ArrayList<>();
        for (MethodNode mn : methods.values()) {
            if (mn.isExternal) continue;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", mn.id);
            map.put("simpleName", mn.simpleName);
            map.put("className", mn.className);
            map.put("packageName", mn.packageName);
            map.put("returnType", mn.returnType);
            map.put("paramTypes", mn.paramTypes);
            map.put("paramNames", mn.paramNames);
            map.put("isPublic", mn.isPublic);
            map.put("isStatic", mn.isStatic);
            map.put("lineStart", mn.lineStart);
            map.put("lineEnd", mn.lineEnd);
            map.put("astDocId", mn.astDocId);
            map.put("mockCandidates", mn.mockCandidates);
            map.put("siblingCalls", mn.siblingCalls);
            map.put("externalCalls", mn.externalCalls);
            map.put("conditions", mn.conditions);
            map.put("returnPoints", mn.returnPoints);
            map.put("nullChecks", mn.nullChecks);
            map.put("instanceChecks", mn.instanceChecks);
            methodList.add(map);
        }
        
        nodesObj.set("methods", mapper.valueToTree(methodList));
        nodesObj.set("classes", mapper.valueToTree(classes.values()));
        nodesObj.set("packages", mapper.valueToTree(packages.values()));
        rootNode.set("edges", mapper.valueToTree(edges));
        
        // Stats
        ObjectNode stats = rootNode.putObject("stats");
        stats.put("totalClasses", classes.size());
        stats.put("totalMethods", methodList.size());
        stats.put("totalPackages", packages.size());
        stats.put("totalEdges", edges.size());
        stats.put("astFilesWritten", astCount.get());
        stats.put("processingTimeSeconds", (System.currentTimeMillis() - startTime) / 1000);
        
        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(new File(graphJson), rootNode);
        
        // ── Write method_index.json (with FULL signatures) ───────────────────────
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
            entry.put("returnType", mn.returnType);
            entry.put("paramTypes", mapper.valueToTree(mn.paramTypes));
            entry.put("paramNames", mapper.valueToTree(mn.paramNames));
            entry.put("mockCandidates", mapper.valueToTree(mn.mockCandidates));
            entry.put("siblingCalls", mapper.valueToTree(mn.siblingCalls));
            
            // FULL external calls with signatures (NEVER truncated)
            entry.put("externalCalls", mapper.valueToTree(mn.externalCalls));
            
            // Code snippets for quick LLM access
            if (mn.methodBody != null) {
                // Only truncate if ENORMOUS (prevents OOM but keeps useful context)
                String body = mn.methodBody;
                if (body.length() > MAX_BODY_LENGTH) {
                    body = body.substring(0, MAX_BODY_LENGTH) + 
                           "\n// ... body truncated (exceeds " + MAX_BODY_LENGTH + " chars) ...";
                }
                entry.put("body", body);
            }
            entry.put("conditions", mapper.valueToTree(mn.conditions));
            entry.put("returnPoints", mapper.valueToTree(mn.returnPoints));
            entry.put("nullChecks", mapper.valueToTree(mn.nullChecks));
            entry.put("instanceChecks", mapper.valueToTree(mn.instanceChecks));
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
        System.out.println("║ Methods indexed:   " + methodList.size());
        System.out.println("║ Graph edges:       " + edges.size());
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ Outputs:                                                  ║");
        System.out.println("║   - ast/*.json (class AST files)                         ║");
        System.out.println("║   - dependency_graph.json (Neo4j)                        ║");
        System.out.println("║   - method_index.json (LLM lookup)                       ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ External calls now have FULL signatures:                 ║");
        System.out.println("║   Example: java.sql.Connection.commit():void             ║");
        System.out.println("║            java.lang.reflect.Method.invoke():Object      ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ Next steps:                                              ║");
        System.out.println("║   1. Parse JaCoCo XML to get uncovered methods           ║");
        System.out.println("║   2. Look up method in method_index.json                 ║");
        System.out.println("║   3. Send to LLM: code + dependencies + mock candidates  ║");
        System.out.println("║   4. Generate targeted tests with precise mocks         ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
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