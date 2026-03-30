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
 * OPTIMIZED Coverage-Analysis Extractor — DEEP AST + COMMENT EDITION
 *
 * Beyond JaCoCo gaps (code bodies, call graph, mock candidates, control flow),
 * this version additionally extracts:
 *
 *  1. Javadoc strings on every method and field
 *  2. All inline (//) and block comment (/* *\/) text attached to method elements
 *  3. A full recursive AST tree for every method body:
 *       each node carries { nodeType, text, lineStart, lineEnd, children[] }
 *  4. Field-level Javadoc inside class AST docs
 *
 * JaCoCo still provides: cyclomatic complexity, line/branch coverage, method coverage.
 *
 * Expected runtime: 20-40 minutes (recursive AST walk adds ~30-50% over base).
 */
public class MainOptimized {

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIGURATION
    // ─────────────────────────────────────────────────────────────────────────

    /** Progress log every N classes */
    private static final int BATCH_PROGRESS = 500;

    /**
     * Body text is stored verbatim unless it exceeds this character limit.
     * The recursive AST tree is NEVER truncated.
     */
    private static final int MAX_BODY_LENGTH = 50_000;

    /**
     * Maximum recursion depth for the deep AST walk.
     * Prevents pathological nesting from consuming heap on generated/synthetic code.
     * Set to Integer.MAX_VALUE to disable the guard entirely.
     */
    private static final int MAX_AST_DEPTH = 64;

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

        // ── Documentation / comments ──────────────────────────────────────
        /**
         * The raw Javadoc string attached to this method (may be null).
         * Includes the full content between /** and *\/
         */
        public String javadoc;

        /**
         * All non-Javadoc comments (inline // and block /* *\/) found anywhere
         * inside the method body, in source order.
         */
        public List<String> comments = new ArrayList<>();

        // ── Code snippets (what LLM needs) ───────────────────────────────
        public String methodBody;           // Full method body as text
        public List<String> conditions;     // All condition expressions
        public List<String> returnPoints;   // All return expressions
        public List<String> nullChecks;     // All null check expressions
        public List<String> instanceChecks; // All instanceof checks

        // ── Dependencies (for test generation) — FULL signatures, never truncated
        public List<String> mockCandidates = new ArrayList<>();  // Fields to mock
        public List<String> siblingCalls   = new ArrayList<>();  // Calls in same class
        public List<String> externalCalls  = new ArrayList<>();  // External service calls

        // ── Cross-links ────────────────────────────────────────────────────
        public String astDocId;
        public String neo4jId;
    }

    static class ClassNode {
        public String id, qualifiedName, simpleName, packageName;
        public boolean isInterface, isAbstract, isEnum;
        public List<String> fieldNames       = new ArrayList<>();
        public List<String> fieldTypes       = new ArrayList<>();
        public List<String> fieldAnnotations = new ArrayList<>();
        public String superClass;
        public List<String> superInterfaces  = new ArrayList<>();
        public List<String> annotations      = new ArrayList<>();
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

    static final Map<String, MethodNode> methods   = new LinkedHashMap<>();
    static final Map<String, ClassNode>  classes   = new LinkedHashMap<>();
    static final Map<String, PackageNode> packages = new LinkedHashMap<>();
    static final List<GraphEdge>         edges     = new ArrayList<>();
    static final Map<String, Set<String>> callers  = new LinkedHashMap<>();

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
            PackageNode p   = new PackageNode();
            p.qualifiedName = k;
            p.simpleName    = k.contains(".") ? k.substring(k.lastIndexOf('.') + 1) : k;
            return p;
        });
    }

    static GraphEdge edge(String from, String to, String type) {
        GraphEdge e = new GraphEdge();
        e.fromId = from;
        e.toId   = to;
        e.type   = type;
        return e;
    }

    static boolean isExternalClass(String fqn) {
        if (fqn == null || fqn.equals("UnresolvedType") || fqn.isEmpty()) return true;
        return fqn.startsWith("java.")             || fqn.startsWith("javax.")        ||
               fqn.startsWith("sun.")              || fqn.startsWith("com.sun.")      ||
               fqn.startsWith("org.springframework.") || fqn.startsWith("org.hibernate.") ||
               fqn.startsWith("org.apache.")       || fqn.startsWith("org.slf4j.")    ||
               fqn.startsWith("org.junit.")        || fqn.startsWith("org.mockito.");
    }

    static boolean isMockCandidate(String fieldType, String fieldName) {
        if (fieldType == null) return false;
        String ft = fieldType.toLowerCase();
        return ft.contains("service")    || ft.contains("repository") ||
               ft.contains("dao")       || ft.contains("client")     ||
               ft.contains("manager")   || ft.contains("handler")    ||
               ft.contains("provider")  || ft.contains("factory")    ||
               ft.contains("template")  || fieldName.toLowerCase().contains("mock");
    }

    static String safe(Object o) {
        try { return o == null ? null : o.toString(); }
        catch (Exception e) { return null; }
    }

    static boolean isAnonymous(CtType<?> t) {
        return t.getSimpleName().contains("$") ||
               t.getQualifiedName().contains("$") ||
               t.getSimpleName().isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMMENT EXTRACTION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the raw Javadoc text attached directly to {@code element}, or
     * {@code null} if none is present.
     *
     * <p>Spoon models Javadoc as a {@link CtComment} with kind
     * {@link CtComment.CommentType#JAVADOC}. We inspect only the comments
     * directly owned by {@code element} (not descendants) so that nested
     * anonymous-class Javadocs are not conflated with the outer method's doc.
     */
    static String extractJavadoc(CtElement element) {
        try {
            for (CtComment comment : element.getComments()) {
                if (comment.getCommentType() == CtComment.CommentType.JAVADOC) {
                    return comment.getContent();   // raw text, /** … */ stripped
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Collects every non-Javadoc comment ({@code //} inline and {@code /* *\/}
     * block) reachable anywhere inside {@code method}'s body, in source order.
     *
     * <p>The scanner visits every {@link CtElement} in the body subtree and
     * harvests its attached comments.  Duplicate comment objects that Spoon
     * may attach to multiple scopes are deduplicated by (line, content) key.
     *
     * @param method the method whose body should be scanned
     * @return ordered list of raw comment strings
     */
    static List<String> extractBodyComments(CtMethod<?> method) {
        List<String> result = new ArrayList<>();
        if (method.getBody() == null) return result;

        // Dedup key: "line:content" — Spoon may attach the same comment to
        // both the statement and its parent block.
        Set<String> seen = new LinkedHashSet<>();

        method.getBody().accept(new CtScanner() {
            @Override
            protected void enter(CtElement e) {
                try {
                    for (CtComment c : e.getComments()) {
                        if (c.getCommentType() == CtComment.CommentType.JAVADOC) continue;
                        String key = c.getPosition().getLine() + ":" + c.getContent();
                        if (seen.add(key)) {
                            result.add(c.getContent());
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEEP AST SERIALIZATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Recursively serializes a Spoon {@link CtElement} into a JSON object of the form:
     *
     * <pre>{@code
     * {
     *   "nodeType" : "CtIfImpl",          // simple class name of the Spoon node
     *   "text"     : "if (x == null) {…}",// toString() of the element (trimmed)
     *   "lineStart": 42,                  // -1 when position is invalid
     *   "lineEnd"  : 47,
     *   "comments" : ["// guard check"],  // inline/block comments on this node
     *   "children" : [ … ]               // recursively serialized direct children
     * }
     * }</pre>
     *
     * <p>Child elements are obtained via {@link CtElement#getDirectChildren()}.
     * Anonymous / synthetic children with no valid position and no own text are
     * omitted to keep the tree navigable.
     *
     * @param element the Spoon element to serialize
     * @param depth   current recursion depth — stops at {@link #MAX_AST_DEPTH}
     * @return a JSON ObjectNode, never null
     */
    static ObjectNode serializeAstNode(CtElement element, int depth) {
        ObjectNode node = mapper.createObjectNode();

        // ── Node type ──────────────────────────────────────────────────────
        node.put("nodeType", element.getClass().getSimpleName());

        // ── Source text ────────────────────────────────────────────────────
        // toString() on large bodies can be slow; we accept that cost here
        // because the deep AST is the explicit goal of this extractor.
        String text = safe(element);
        node.put("text", text != null ? text : "");

        // ── Position ───────────────────────────────────────────────────────
        try {
            if (element.getPosition().isValidPosition()) {
                node.put("lineStart", element.getPosition().getLine());
                node.put("lineEnd",   element.getPosition().getEndLine());
            } else {
                node.put("lineStart", -1);
                node.put("lineEnd",   -1);
            }
        } catch (Exception e) {
            node.put("lineStart", -1);
            node.put("lineEnd",   -1);
        }

        // ── Comments on this specific node (not descendants) ───────────────
        ArrayNode commentsNode = node.putArray("comments");
        try {
            for (CtComment c : element.getComments()) {
                commentsNode.add(c.getContent());
            }
        } catch (Exception ignored) {}

        // ── Children ───────────────────────────────────────────────────────
        ArrayNode childrenNode = node.putArray("children");

        if (depth < MAX_AST_DEPTH) {
            try {
                for (CtElement child : element.getDirectChildren()) {
                    if (child == null) continue;

                    // Skip implicit / compiler-generated nodes that carry no
                    // useful information (e.g. implicit 'this' references).
                    if (child.isImplicit()) continue;

                    ObjectNode childJson = serializeAstNode(child, depth + 1);
                    childrenNode.add(childJson);
                }
            } catch (Exception ignored) {}
        } else {
            // Depth guard hit — record a sentinel so readers know the tree was capped
            ObjectNode guard = childrenNode.addObject();
            guard.put("nodeType", "__DepthLimitReached__");
            guard.put("text", "AST depth > " + MAX_AST_DEPTH + "; subtree omitted");
            guard.put("lineStart", -1);
            guard.put("lineEnd",   -1);
            guard.putArray("comments");
            guard.putArray("children");
        }

        return node;
    }

    /**
     * Convenience wrapper: serializes the body of a method into a deep AST tree.
     * Returns {@code null} (and does NOT write a node) if the method has no body.
     *
     * @param method the method whose body should be walked
     * @return deep AST ObjectNode, or null
     */
    static ObjectNode serializeMethodBodyAst(CtMethod<?> method) {
        if (method.getBody() == null) return null;
        return serializeAstNode(method.getBody(), 0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CODE EXTRACTION (What LLM needs)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extracts every condition expression from control-flow elements inside the
     * given method body: {@code if}, ternary {@code ?:}, {@code while},
     * {@code for}, and {@code do-while}.
     *
     * @param method the method to scan
     * @return ordered list of condition expression strings
     */
    static List<String> extractConditions(CtMethod<?> method) {
        List<String> conditions = new ArrayList<>();
        if (method.getBody() == null) return conditions;

        method.getBody().accept(new CtScanner() {
            @Override
            public void visitCtIf(CtIf ifElement) {
                String cond = safe(ifElement.getCondition());
                if (cond != null && !cond.isEmpty()) conditions.add(cond);
                super.visitCtIf(ifElement);
            }

            @Override
            public <T> void visitCtConditional(CtConditional<T> conditional) {
                String cond = safe(conditional.getCondition());
                if (cond != null && !cond.isEmpty()) conditions.add(cond);
                super.visitCtConditional(conditional);
            }

            @Override
            public void visitCtWhile(CtWhile whileLoop) {
                String cond = safe(whileLoop.getLoopingExpression());
                if (cond != null && !cond.isEmpty()) conditions.add(cond);
                super.visitCtWhile(whileLoop);
            }

            @Override
            public void visitCtFor(CtFor forLoop) {
                String cond = safe(forLoop.getExpression());
                if (cond != null && !cond.isEmpty()) conditions.add(cond);
                super.visitCtFor(forLoop);
            }

            @Override
            public void visitCtDo(CtDo doLoop) {
                String cond = safe(doLoop.getLoopingExpression());
                if (cond != null && !cond.isEmpty()) conditions.add(cond);
                super.visitCtDo(doLoop);
            }
        });

        return conditions;
    }

    /**
     * Collects every {@code return} expression in the method body.
     * Void returns are represented as the string {@code "void"}.
     *
     * @param method the method to scan
     * @return ordered list of return expression strings
     */
    static List<String> extractReturns(CtMethod<?> method) {
        List<String> returns = new ArrayList<>();
        if (method.getBody() == null) return returns;

        method.getBody().accept(new CtScanner() {
            @Override
            public <T> void visitCtReturn(CtReturn<T> returnStmt) {
                String expr = safe(returnStmt.getReturnedExpression());
                returns.add(expr != null ? expr : "void");
                super.visitCtReturn(returnStmt);
            }
        });

        return returns;
    }

    /**
     * Collects every null-equality check ({@code == null} and {@code != null})
     * appearing in the method body.
     *
     * @param method the method to scan
     * @return ordered list of null-check expression strings
     */
    static List<String> extractNullChecks(CtMethod<?> method) {
        List<String> nullChecks = new ArrayList<>();
        if (method.getBody() == null) return nullChecks;

        method.getBody().accept(new CtScanner() {
            @Override
            public <T> void visitCtBinaryOperator(CtBinaryOperator<T> op) {
                String kind = safe(op.getKind());
                if ("EQ".equals(kind) || "NE".equals(kind)) {
                    String left  = safe(op.getLeftHandOperand());
                    String right = safe(op.getRightHandOperand());
                    if ("null".equals(left) || "null".equals(right)) {
                        nullChecks.add(left + " " + ("EQ".equals(kind) ? "==" : "!=") + " " + right);
                    }
                }
                super.visitCtBinaryOperator(op);
            }
        });

        return nullChecks;
    }

    /**
     * Collects every {@code instanceof} check appearing in the method body.
     *
     * @param method the method to scan
     * @return ordered list of instanceof expression strings
     */
    static List<String> extractInstanceChecks(CtMethod<?> method) {
        List<String> instanceChecks = new ArrayList<>();
        if (method.getBody() == null) return instanceChecks;

        method.getBody().accept(new CtScanner() {
            @Override
            public <T> void visitCtBinaryOperator(CtBinaryOperator<T> op) {
                if ("INSTANCEOF".equals(safe(op.getKind()))) {
                    instanceChecks.add(
                        safe(op.getLeftHandOperand()) + " instanceof " + safe(op.getRightHandOperand()));
                }
                super.visitCtBinaryOperator(op);
            }
        });

        return instanceChecks;
    }

    /**
     * Identifies fields on {@code type} that are likely to need mocking in
     * unit tests (services, repositories, DAOs, clients, etc.).
     *
     * @param type the class/interface to inspect
     * @return list of {@code "fieldName:fullyQualifiedType"} strings
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
     * Builds a fully-qualified method signature string from a call-site
     * executable reference, including parameter types and return type.
     *
     * <p>Format: {@code pkg.ClassName.methodName(ParamType1, ParamType2):ReturnType}
     *
     * @param exec the executable reference at the call site
     * @return full signature string, never null
     */
    static String getFullMethodSignature(CtExecutableReference<?> exec) {
        StringBuilder sb = new StringBuilder();

        if (exec.getDeclaringType() != null) {
            sb.append(exec.getDeclaringType().getQualifiedName());
        } else {
            sb.append("UnknownClass");
        }
        sb.append(".").append(exec.getSimpleName()).append("(");

        List<CtTypeReference<?>> paramTypes = exec.getParameters();
        for (int i = 0; i < paramTypes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(paramTypes.get(i) != null ? paramTypes.get(i).getQualifiedName() : "unknown");
        }
        sb.append(")");

        if (exec.getType() != null) {
            sb.append(":").append(exec.getType().getQualifiedName());
        }

        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON SERIALIZATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Serializes an entire {@link CtType} (class, interface, enum, annotation)
     * to a MongoDB-ready JSON document.
     *
     * <p>The document contains:
     * <ul>
     *   <li>Identity &amp; location metadata</li>
     *   <li>Fields with name, type, modifiers, and their Javadoc</li>
     *   <li>Class-level annotations</li>
     *   <li>Inheritance (superClass, superInterfaces)</li>
     *   <li>Mock candidates (for test scaffolding)</li>
     *   <li>All methods serialized via {@link #serializeMethodToJson}</li>
     * </ul>
     *
     * @param type the Spoon type element to serialize
     * @return an ObjectNode ready for insertion into {@code ast_classes}
     */
    static ObjectNode serializeClassToJson(CtType<?> type) {
        ObjectNode node = mapper.createObjectNode();

        // ── Basic identity ─────────────────────────────────────────────────
        node.put("_id",          type.getQualifiedName());
        node.put("qualifiedName", type.getQualifiedName());
        node.put("simpleName",    type.getSimpleName());
        node.put("packageName",   type.getPackage() != null
                ? type.getPackage().getQualifiedName() : "default");
        node.put("isInterface",   type instanceof CtInterface);
        node.put("isEnum",        type instanceof CtEnum);
        node.put("isAbstract",    type.isAbstract());

        // ── Class-level Javadoc ────────────────────────────────────────────
        String classJavadoc = extractJavadoc(type);
        if (classJavadoc != null) node.put("javadoc", classJavadoc);

        // ── Source location ────────────────────────────────────────────────
        try {
            if (type.getPosition() != null && type.getPosition().getFile() != null) {
                node.put("sourceFile", type.getPosition().getFile().getAbsolutePath());
                node.put("lineStart",  type.getPosition().getLine());
                node.put("lineEnd",    type.getPosition().getEndLine());
            }
        } catch (Exception ignored) {}

        // ── Fields (for mock detection + LLM context) ─────────────────────
        ArrayNode fieldsNode = node.putArray("fields");
        for (CtField<?> field : type.getFields()) {
            ObjectNode fieldNode = fieldsNode.addObject();
            fieldNode.put("name",     field.getSimpleName());
            fieldNode.put("type",     field.getType() != null
                    ? field.getType().getQualifiedName() : "unknown");
            fieldNode.put("isStatic", field.isStatic());
            fieldNode.put("isFinal",  field.isFinal());

            // Field-level Javadoc — often describes @Autowired dependencies
            String fieldJavadoc = extractJavadoc(field);
            if (fieldJavadoc != null) fieldNode.put("javadoc", fieldJavadoc);

            // Field annotations (e.g. @Autowired, @Value)
            ArrayNode fieldAnns = fieldNode.putArray("annotations");
            for (CtAnnotation<?> ann : field.getAnnotations()) {
                try { fieldAnns.add(ann.getAnnotationType().getQualifiedName()); }
                catch (Exception ignored) {}
            }
        }

        // ── Class annotations ──────────────────────────────────────────────
        ArrayNode annotations = node.putArray("annotations");
        for (CtAnnotation<?> ann : type.getAnnotations()) {
            try { annotations.add(ann.getAnnotationType().getQualifiedName()); }
            catch (Exception ignored) {}
        }

        // ── Inheritance ────────────────────────────────────────────────────
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
            try { interfaces.add(iface.getQualifiedName()); }
            catch (Exception ignored) {}
        }

        // ── Mock candidates (class-level) ──────────────────────────────────
        ArrayNode mockCandidates = node.putArray("mockCandidates");
        extractMockCandidates(type).forEach(mockCandidates::add);

        // ── Methods (deep serialization) ───────────────────────────────────
        ArrayNode methodsNode = node.putArray("methods");
        for (CtMethod<?> method : type.getMethods()) {
            methodsNode.add(serializeMethodToJson(method, type.getQualifiedName()));
        }

        return node;
    }

    /**
     * Serializes a single {@link CtMethod} to a JSON object.
     *
     * <p>Output fields:
     * <ul>
     *   <li>{@code name}, {@code signature}, {@code id} — identity</li>
     *   <li>{@code returnType}, {@code paramTypes}, {@code paramNames} — signature types</li>
     *   <li>{@code thrownTypes} — declared checked exceptions</li>
     *   <li>{@code modifiers} — visibility and other flags</li>
     *   <li>{@code lineStart}, {@code lineEnd} — for JaCoCo line mapping</li>
     *   <li>{@code javadoc} — raw Javadoc string (may be absent)</li>
     *   <li>{@code comments} — ordered list of inline/block comments in body</li>
     *   <li>{@code body} — full source text (truncated only beyond {@link #MAX_BODY_LENGTH})</li>
     *   <li>{@code ast} — deep recursive AST tree of the body (never truncated)</li>
     *   <li>{@code conditions}, {@code returnPoints}, {@code nullChecks},
     *       {@code instanceChecks} — extracted control-flow snippets</li>
     *   <li>{@code mockCandidates}, {@code siblingCalls}, {@code externalCalls}
     *       — dependency placeholders filled by call-graph pass</li>
     * </ul>
     *
     * @param method    the Spoon method element to serialize
     * @param className the fully-qualified name of the declaring class
     * @return an ObjectNode ready for embedding inside the class document
     */
    static ObjectNode serializeMethodToJson(CtMethod<?> method, String className) {
        ObjectNode node = mapper.createObjectNode();

        // ── Identity ───────────────────────────────────────────────────────
        node.put("name",      method.getSimpleName());
        node.put("signature", method.getSignature());
        node.put("id",        methodId(method));

        // ── Return type ────────────────────────────────────────────────────
        node.put("returnType", method.getType() != null
                ? method.getType().getQualifiedName() : "void");

        // ── Parameters ─────────────────────────────────────────────────────
        ArrayNode paramTypes = node.putArray("paramTypes");
        ArrayNode paramNames = node.putArray("paramNames");
        for (CtParameter<?> p : method.getParameters()) {
            paramTypes.add(p.getType() != null ? p.getType().getQualifiedName() : "unknown");
            paramNames.add(p.getSimpleName());
        }

        // ── Thrown exceptions ──────────────────────────────────────────────
        ArrayNode thrown = node.putArray("thrownTypes");
        for (CtTypeReference<?> t : method.getThrownTypes()) {
            thrown.add(t.getQualifiedName());
        }

        // ── Annotations ─────────────────────────────────────────────────────
        ArrayNode methodAnns = node.putArray("annotations");
        for (CtAnnotation<?> ann : method.getAnnotations()) {
            try { methodAnns.add(ann.getAnnotationType().getQualifiedName()); }
            catch (Exception ignored) {}
        }

        // ── Modifiers ──────────────────────────────────────────────────────
        ObjectNode modifiers = node.putObject("modifiers");
        modifiers.put("public",    method.isPublic());
        modifiers.put("private",   method.isPrivate());
        modifiers.put("protected", method.isProtected());
        modifiers.put("static",    method.isStatic());
        modifiers.put("abstract",  method.isAbstract());
        modifiers.put("final",     method.isFinal());

        // ── Position (for JaCoCo line mapping) ────────────────────────────
        if (method.getPosition().isValidPosition()) {
            node.put("lineStart", method.getPosition().getLine());
            node.put("lineEnd",   method.getPosition().getEndLine());
        }

        // ── DOCUMENTATION ──────────────────────────────────────────────────

        // Javadoc — the structured /** … */ block directly above the method.
        // Contains @param, @return, @throws descriptions authored by developers.
        // This is critical context for the LLM when generating aligned tests.
        String javadoc = extractJavadoc(method);
        if (javadoc != null) {
            node.put("javadoc", javadoc);
        }

        // Inline / block comments inside the method body — often explain
        // non-obvious branches, workarounds, or business rules that don't
        // appear in the method signature.
        ArrayNode commentsNode = node.putArray("comments");
        extractBodyComments(method).forEach(commentsNode::add);

        // ── CODE CONTENT ───────────────────────────────────────────────────

        if (method.getBody() != null) {

            // Full body text — truncated only if genuinely enormous.
            // The LLM uses this for holistic understanding of logic flow.
            String bodyText = method.getBody().toString();
            if (bodyText.length() > MAX_BODY_LENGTH) {
                node.put("body", bodyText.substring(0, MAX_BODY_LENGTH)
                        + "\n// ... body truncated (exceeds " + MAX_BODY_LENGTH + " chars) ...");
            } else {
                node.put("body", bodyText);
            }

            // ── DEEP AST TREE ──────────────────────────────────────────────
            // The recursive AST tree gives the LLM and analysis pipeline a
            // structured, typed view of every statement and expression.
            // Unlike the flat body text, the tree preserves nesting, node
            // types, and per-node comments — enabling precise path analysis
            // without re-parsing the source.  Never truncated.
            ObjectNode astTree = serializeMethodBodyAst(method);
            if (astTree != null) {
                node.set("ast", astTree);
            }

            // ── Extracted control-flow snippets ────────────────────────────

            // Conditions — all boolean expressions governing branching;
            // used to enumerate paths the LLM should exercise in tests.
            ArrayNode conditions = node.putArray("conditions");
            extractConditions(method).forEach(conditions::add);

            // Return points — all values returned; helps the LLM verify
            // that assertions cover every possible outcome.
            ArrayNode returns = node.putArray("returnPoints");
            extractReturns(method).forEach(returns::add);

            // Null checks — guards that the LLM must honour when constructing
            // both happy-path and edge-case test inputs.
            ArrayNode nullChecks = node.putArray("nullChecks");
            extractNullChecks(method).forEach(nullChecks::add);

            // Instanceof checks — type-narrowing branches that require the
            // LLM to supply polymorphic test objects.
            ArrayNode instanceChecks = node.putArray("instanceChecks");
            extractInstanceChecks(method).forEach(instanceChecks::add);
        }

        // ── DEPENDENCY PLACEHOLDERS ────────────────────────────────────────
        // Populated during the call-graph extraction pass (extractCallGraph).
        // Kept here so the document schema is stable from the first write.
        node.putArray("mockCandidates");  // Fields that need Mockito mocks
        node.putArray("siblingCalls");    // Method calls within the same class
        node.putArray("externalCalls");   // Full signatures of cross-class calls

        return node;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CALL GRAPH EXTRACTION  (fills dependency placeholders)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Walks every method invocation inside {@code method}'s body and classifies
     * each call as either a sibling call (same declaring class) or an external
     * call (different class).  Results are added to {@code methodNode}'s lists.
     *
     * <p>Also emits {@link GraphEdge} objects for Neo4j ingestion:
     * <ul>
     *   <li>{@code CALLS} — method to sibling method</li>
     *   <li>{@code CALLS_EXTERNAL} — method to external method</li>
     * </ul>
     *
     * @param method     the Spoon method whose body is scanned
     * @param methodNode the in-memory node being built
     * @param className  the FQN of the declaring class
     */
    static void extractCallGraph(CtMethod<?> method, MethodNode methodNode, String className) {
        if (method.getBody() == null) return;
        String mId = methodId(method);

        method.getBody().accept(new CtScanner() {
            @Override
            public <T> void visitCtInvocation(CtInvocation<T> inv) {
                try {
                    CtExecutableReference<?> exec = inv.getExecutable();
                    if (exec == null) { super.visitCtInvocation(inv); return; }

                    String fullSig    = getFullMethodSignature(exec);
                    String targetFqn  = exec.getDeclaringType() != null
                            ? exec.getDeclaringType().getQualifiedName() : null;
                    String calleeId   = (targetFqn != null ? targetFqn : "unknown")
                                        + "#" + exec.getSimpleName() + "("
                                        + exec.getParameters().stream()
                                              .map(p -> p != null ? p.getQualifiedName() : "?")
                                              .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b)
                                        + ")";

                    if (className.equals(targetFqn)) {
                        // Sibling call — same class
                        if (!methodNode.siblingCalls.contains(fullSig)) {
                            methodNode.siblingCalls.add(fullSig);
                        }
                        GraphEdge e = edge(mId, calleeId, "CALLS");
                        edges.add(e);
                    } else {
                        // External call
                        if (!methodNode.externalCalls.contains(fullSig)) {
                            methodNode.externalCalls.add(fullSig);
                        }
                        GraphEdge e = edge(mId, calleeId, "CALLS_EXTERNAL");
                        edges.add(e);
                    }

                    // Track reverse (callers) for Neo4j CALLED_BY edges
                    callers.computeIfAbsent(calleeId, k -> new LinkedHashSet<>()).add(mId);

                } catch (Exception ignored) {}
                super.visitCtInvocation(inv);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN PIPELINE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Entry point.
     *
     * <p>Usage: {@code java MainOptimized <sourceRoot> <outputDir>}
     *
     * <ul>
     *   <li>{@code sourceRoot} — root directory of the Java source tree to analyse</li>
     *   <li>{@code outputDir}  — directory where NDJSON output files are written</li>
     * </ul>
     *
     * <p>Output files:
     * <ul>
     *   <li>{@code ast_classes.ndjson}  — one JSON doc per class (→ MongoDB {@code ast_classes})</li>
     *   <li>{@code ast_methods.ndjson}  — one JSON doc per method (→ MongoDB {@code ast_methods})</li>
     *   <li>{@code neo4j_nodes.ndjson}  — graph node payloads (→ Neo4j)</li>
     *   <li>{@code neo4j_edges.ndjson}  — graph edge payloads (→ Neo4j)</li>
     * </ul>
     *
     * @param args CLI arguments: [sourceRoot, outputDir]
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: MainOptimized <sourceRoot> <outputDir>");
            System.exit(1);
        }

        String sourceRoot = args[0];
        String outputDir  = args[1];
        new File(outputDir).mkdirs();

        // ── 1. Build Spoon model ───────────────────────────────────────────
        System.out.println("[1/4] Building Spoon model from: " + sourceRoot);
        long t0 = System.currentTimeMillis();

        Launcher launcher = new Launcher();
        launcher.addInputResource(sourceRoot);
        launcher.getEnvironment().setAutoImports(true);
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setCommentEnabled(true);  // ← REQUIRED for comment extraction
        launcher.buildModel();

        CtModel model = launcher.getModel();
        System.out.printf("[1/4] Model built in %.1fs%n",
                (System.currentTimeMillis() - t0) / 1000.0);

        // ── 2. Extract classes, methods, and ASTs ─────────────────────────
        System.out.println("[2/4] Extracting classes, deep ASTs, and comments …");
        AtomicInteger classCount  = new AtomicInteger(0);
        AtomicInteger methodCount = new AtomicInteger(0);

        List<CtType<?>> allTypes = model.getElements(new TypeFilter<>(CtType.class));

        for (CtType<?> type : allTypes) {
            if (isAnonymous(type)) continue;

            String qn  = type.getQualifiedName();
            String pkg  = type.getPackage() != null ? type.getPackage().getQualifiedName() : "default";
            ensurePackage(pkg);

            // ── Class node (in-memory) ─────────────────────────────────────
            ClassNode cn           = new ClassNode();
            cn.id                  = qn;
            cn.qualifiedName       = qn;
            cn.simpleName          = type.getSimpleName();
            cn.packageName         = pkg;
            cn.isInterface         = type instanceof CtInterface;
            cn.isAbstract          = type.isAbstract();
            cn.isEnum              = type instanceof CtEnum;
            cn.sourceFile          = type.getPosition() != null && type.getPosition().getFile() != null
                    ? type.getPosition().getFile().getAbsolutePath() : null;
            classes.put(qn, cn);

            // ── Method nodes (in-memory) ───────────────────────────────────
            for (CtMethod<?> method : type.getMethods()) {
                String mId = methodId(method);

                MethodNode mn       = new MethodNode();
                mn.id               = mId;
                mn.simpleName       = method.getSimpleName();
                mn.className        = qn;
                mn.packageName      = pkg;
                mn.returnType       = method.getType() != null ? method.getType().getQualifiedName() : "void";
                mn.isPublic         = method.isPublic();
                mn.isPrivate        = method.isPrivate();
                mn.isProtected      = method.isProtected();
                mn.isStatic         = method.isStatic();
                mn.isAbstract       = method.isAbstract();
                mn.isFinal          = method.isFinal();
                mn.javadoc          = extractJavadoc(method);
                mn.comments         = extractBodyComments(method);
                mn.conditions       = extractConditions(method);
                mn.returnPoints     = extractReturns(method);
                mn.nullChecks       = extractNullChecks(method);
                mn.instanceChecks   = extractInstanceChecks(method);

                if (method.getPosition().isValidPosition()) {
                    mn.lineStart = method.getPosition().getLine();
                    mn.lineEnd   = method.getPosition().getEndLine();
                }

                // Call-graph extraction (fills siblingCalls, externalCalls, edges)
                extractCallGraph(method, mn, qn);

                methods.put(mId, mn);
                methodCount.incrementAndGet();
            }

            int c = classCount.incrementAndGet();
            if (c % BATCH_PROGRESS == 0) {
                System.out.printf("  … processed %,d classes, %,d methods%n", c, methodCount.get());
            }
        }

        System.out.printf("[2/4] Finished: %,d classes, %,d methods%n",
                classCount.get(), methodCount.get());

        // ── 3. Write MongoDB NDJSON files ─────────────────────────────────
        System.out.println("[3/4] Writing MongoDB NDJSON output …");

        try (java.io.PrintWriter classWriter = new java.io.PrintWriter(
                    new java.io.FileWriter(outputDir + "/ast_classes.ndjson"));
             java.io.PrintWriter methodWriter = new java.io.PrintWriter(
                    new java.io.FileWriter(outputDir + "/ast_methods.ndjson"))) {

            for (CtType<?> type : allTypes) {
                if (isAnonymous(type)) continue;
                try {
                    ObjectNode classDoc = serializeClassToJson(type);
                    classWriter.println(mapper.writeValueAsString(classDoc));

                    // Also emit a lightweight method doc per method
                    for (CtMethod<?> method : type.getMethods()) {
                        ObjectNode methodDoc = serializeMethodToJson(method, type.getQualifiedName());
                        methodDoc.put("className", type.getQualifiedName());
                        methodDoc.put("packageName", type.getPackage() != null
                                ? type.getPackage().getQualifiedName() : "default");
                        methodWriter.println(mapper.writeValueAsString(methodDoc));
                    }
                } catch (Exception e) {
                    System.err.println("  WARN: skipping " + type.getQualifiedName()
                            + " — " + e.getMessage());
                }
            }
        }

        // ── 4. Write Neo4j NDJSON files ───────────────────────────────────
        System.out.println("[4/4] Writing Neo4j NDJSON output …");

        try (java.io.PrintWriter nodeWriter = new java.io.PrintWriter(
                    new java.io.FileWriter(outputDir + "/neo4j_nodes.ndjson"));
             java.io.PrintWriter edgeWriter = new java.io.PrintWriter(
                    new java.io.FileWriter(outputDir + "/neo4j_edges.ndjson"))) {

            for (ClassNode cn : classes.values()) {
                ObjectNode n = mapper.createObjectNode();
                n.put("label", "Class");
                n.put("id",    cn.id);
                n.put("simpleName",    cn.simpleName);
                n.put("packageName",   cn.packageName);
                n.put("isInterface",   cn.isInterface);
                n.put("isAbstract",    cn.isAbstract);
                nodeWriter.println(mapper.writeValueAsString(n));
            }

            for (PackageNode pn : packages.values()) {
                ObjectNode n = mapper.createObjectNode();
                n.put("label",         "Package");
                n.put("id",            pn.qualifiedName);
                n.put("simpleName",    pn.simpleName);
                n.put("qualifiedName", pn.qualifiedName);
                nodeWriter.println(mapper.writeValueAsString(n));
            }

            for (MethodNode mn : methods.values()) {
                ObjectNode n = mapper.createObjectNode();
                n.put("label",      "Method");
                n.put("id",         mn.id);
                n.put("simpleName", mn.simpleName);
                n.put("className",  mn.className);
                n.put("lineStart",  mn.lineStart);
                n.put("lineEnd",    mn.lineEnd);
                n.put("isPublic",   mn.isPublic);
                n.put("isStatic",   mn.isStatic);
                if (mn.javadoc != null) n.put("javadoc", mn.javadoc);
                nodeWriter.println(mapper.writeValueAsString(n));
            }

            for (GraphEdge e : edges) {
                ObjectNode en = mapper.createObjectNode();
                en.put("from", e.fromId);
                en.put("to",   e.toId);
                en.put("type", e.type);
                edgeWriter.println(mapper.writeValueAsString(en));
            }
        }

        long elapsed = System.currentTimeMillis() - t0;
        System.out.printf("%nDone in %.1f minutes.%n", elapsed / 60_000.0);
        System.out.println("Output written to: " + outputDir);
    }
}
