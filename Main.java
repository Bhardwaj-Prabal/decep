import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.*;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.reflect.code.*;
import spoon.support.reflect.declaration.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import java.io.File;
import java.util.*;

/**
 * Two-pass static analyzer built on Spoon.
 *
 * PASS 1 — Full recursive AST serialization
 *   Every CtElement in Spoon's tree is walked recursively and
 *   serialized to JSON. No cherry-picking — every node type,
 *   every child, every piece of metadata is captured.
 *   Output → ast/<QualifiedName>.json  (one file per class)
 *   Loaded → MongoDB  (db: harmony_codebase, collection: ast_nodes)
 *
 * PASS 2 — Dependency / call graph extraction
 *   Walks the same model a second time to build typed nodes
 *   and edges for Neo4j.
 *   Output → dependency_graph.json
 *   Loaded → Neo4j AuraDB
 *
 * Cross-link
 *   Every :Method and :Class node in Neo4j carries astDocId
 *   = the MongoDB _id of the class AST document.
 */
public class Main {

    // ─────────────────────────────────────────────────────────────────────────
    // CALL-GRAPH MODELS  (Neo4j)
    // ─────────────────────────────────────────────────────────────────────────

    static class MethodNode {
        public String  id, qualifiedName, simpleName;
        public String  className, packageName, returnType;
        public List<String> paramTypes  = new ArrayList<>();
        public boolean isPublic, isPrivate, isProtected;
        public boolean isStatic, isAbstract, isConstructor;
        public boolean isExternal  = false;
        public int     lineNumber  = -1;
        public String  astDocId    = null;   // → MongoDB _id
    }

    static class ClassNode {
        public String  id, qualifiedName, simpleName, packageName;
        public boolean isInterface, isAbstract, isEnum;
        public String  astDocId = null;      // → MongoDB _id
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
    // Make these static to be accessible from static methods
    static final Map<String, MethodNode>  methods  = new LinkedHashMap<>();
    static final Map<String, ClassNode>   classes  = new LinkedHashMap<>();
    static final Map<String, PackageNode> packages = new LinkedHashMap<>();
    static final List<GraphEdge>          edges    = new ArrayList<>();
    static final ObjectMapper             mapper   = new ObjectMapper();

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
            p.simpleName    = k.contains(".")
                               ? k.substring(k.lastIndexOf('.') + 1) : k;
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
        return fqn.startsWith("java.")
            || fqn.startsWith("javax.")
            || fqn.startsWith("sun.")
            || fqn.startsWith("org.springframework.")
            || fqn.startsWith("org.hibernate.")
            || fqn.equals("UnresolvedType");
    }

    static String safe(Object o) {
        try { 
            return o == null ? null : o.toString(); 
        }
        catch (Exception e) { 
            return null; 
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASS 1 — FULL RECURSIVE AST SERIALIZATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Recursively serialize any Spoon CtElement into a JSON ObjectNode.
     *
     * For every node we capture:
     *   - nodeType   : the concrete Spoon class name (e.g. CtIfImpl, CtMethodImpl)
     *   - role       : this node's role in its parent (e.g. BODY, CONDITION, THEN)
     *   - position   : file, line, column (when available)
     *   - signature  : short string representation of the element
     *   - metadata   : type-specific fields (modifiers, name, type ref, value, etc.)
     *   - children   : recursive array of child CtElements
     *
     * Nothing is cherry-picked. If Spoon parsed it, it ends up in the JSON.
     */
    static ObjectNode serializeElement(spoon.reflect.declaration.CtElement el,
                                       String role,
                                       int depth) {
        ObjectNode node = mapper.createObjectNode();
        if (el == null) return node;

        // ── node identity ─────────────────────────────────────────────────────
        String typeName = el.getClass().getSimpleName()
                            .replace("Impl", "");   // CtIfImpl → CtIf
        node.put("nodeType", typeName);
        if (role != null) node.put("role", role);

        // ── source position ───────────────────────────────────────────────────
        try {
            if (el.getPosition() != null && el.getPosition().isValidPosition()) {
                ObjectNode pos = node.putObject("position");
                pos.put("line",   el.getPosition().getLine());
                pos.put("column", el.getPosition().getColumn());
                try {
                    pos.put("file", el.getPosition().getFile().getName());
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        // ── short string signature ─────────────────────────────────────────────
        try {
            String sig = el.toString();
            // cap length to avoid embedding entire method bodies as strings
            node.put("signature", sig.length() > 200
                                   ? sig.substring(0, 200) + "…" : sig);
        } catch (Exception ignored) {}

        // ── type-specific metadata ─────────────────────────────────────────────
        extractMetadata(el, node);

        // ── children — recurse (guard against infinite loops with depth cap) ───
        if (depth < 30) {
            ArrayNode children = mapper.createArrayNode();
            try {
                for (spoon.reflect.declaration.CtElement child
                        : el.getDirectChildren()) {
                    if (child == null) continue;
                    // determine the child's role in the parent
                    String childRole = null;
                    try {
                        childRole = child.getRoleInParent() != null
                                     ? child.getRoleInParent().toString() : null;
                    } catch (Exception ignored) {}
                    children.add(serializeElement(child, childRole, depth + 1));
                }
            } catch (Exception ignored) {}
            if (children.size() > 0) node.set("children", children);
        }

        return node;
    }

    /**
     * Extract type-specific metadata fields for well-known CtElement subtypes.
     * These are stored flat on the node alongside the recursive children,
     * making common queries simpler (e.g. just read node.name instead of
     * hunting through children for a NAME child).
     */
    @SuppressWarnings("unchecked")
    static void extractMetadata(spoon.reflect.declaration.CtElement el,
                                 ObjectNode node) {
        // Named elements
        if (el instanceof CtNamedElement) {
            node.put("name", ((CtNamedElement) el).getSimpleName());
        }

        // Typed elements (fields, variables, parameters, methods)
        if (el instanceof CtTypedElement) {
            try {
                CtTypedElement<?> typed = (CtTypedElement<?>) el;
                if (typed.getType() != null) {
                    node.put("type", safe(typed.getType().getQualifiedName()));
                }
            } catch (Exception ignored) {}
        }

        // Modifiers
        if (el instanceof CtModifiable) {
            CtModifiable mod = (CtModifiable) el;
            ObjectNode mods  = node.putObject("modifiers");
            mods.put("isPublic",    mod.isPublic());
            mods.put("isPrivate",   mod.isPrivate());
            mods.put("isProtected", mod.isProtected());
            mods.put("isStatic",    mod.isStatic());
            mods.put("isAbstract",  mod.isAbstract());
            mods.put("isFinal",     mod.isFinal());
        }

        // Method
        if (el instanceof CtMethod) {
            CtMethod<?> m = (CtMethod<?>) el;
            node.put("signature",    safe(m.getSignature()));
            if (m.getType() != null) {
                node.put("returnType",   safe(m.getType().getQualifiedName()));
            }
            node.put("isOverriding", m.isOverriding());
            ArrayNode params = node.putArray("paramTypes");
            for (CtParameter<?> p : m.getParameters()) {
                if (p.getType() != null) {
                    params.add(safe(p.getType().getQualifiedName()));
                }
            }
            ArrayNode thrown = node.putArray("thrownTypes");
            for (CtTypeReference<?> t : m.getThrownTypes()) {
                thrown.add(safe(t.getQualifiedName()));
            }
            // annotations as flat list
            ArrayNode anns = node.putArray("annotations");
            for (CtAnnotation<?> a : m.getAnnotations())
                anns.add(safe(a.getAnnotationType().getQualifiedName()));
        }

        // Constructor
        if (el instanceof CtConstructor) {
            CtConstructor<?> c = (CtConstructor<?>) el;
            ArrayNode params = node.putArray("paramTypes");
            for (CtParameter<?> p : c.getParameters()) {
                if (p.getType() != null) {
                    params.add(safe(p.getType().getQualifiedName()));
                }
            }
        }

        // Field
        if (el instanceof CtField) {
            CtField<?> f = (CtField<?>) el;
            node.put("isTransient", f.isTransient());
            node.put("isVolatile",  f.isVolatile());
            try { node.put("defaultValue", safe(f.getDefaultExpression())); }
            catch (Exception ignored) {}
            ArrayNode anns = node.putArray("annotations");
            for (CtAnnotation<?> a : f.getAnnotations())
                anns.add(safe(a.getAnnotationType().getQualifiedName()));
        }

        // Class / Interface
        if (el instanceof CtType) {
            CtType<?> t = (CtType<?>) el;
            node.put("qualifiedName", t.getQualifiedName());
            node.put("packageName",   t.getPackage() != null
                                       ? t.getPackage().getQualifiedName() : "default");
            node.put("isInterface",   t instanceof CtInterface);
            node.put("isEnum",        t instanceof CtEnum);
            ArrayNode anns = node.putArray("annotations");
            for (CtAnnotation<?> a : t.getAnnotations())
                anns.add(safe(a.getAnnotationType().getQualifiedName()));
            // superclass / interfaces flat for quick lookup
            try {
                if (t instanceof CtClass) {
                    CtTypeReference<?> sup = ((CtClass<?>) t).getSuperclass();
                    if (sup != null) node.put("superClass", safe(sup.getQualifiedName()));
                }
            } catch (Exception ignored) {}
            ArrayNode ifaces = node.putArray("superInterfaces");
            try {
                for (CtTypeReference<?> i : t.getSuperInterfaces())
                    ifaces.add(safe(i.getQualifiedName()));
            } catch (Exception ignored) {}
        }

        // Method invocation
        if (el instanceof CtInvocation) {
            CtInvocation<?> inv = (CtInvocation<?>) el;
            node.put("methodName",  safe(inv.getExecutable().getSimpleName()));
            node.put("targetClass", inv.getExecutable().getDeclaringType() != null
                                     ? safe(inv.getExecutable().getDeclaringType().getQualifiedName()) : null);
        }

        // If statement
        if (el instanceof CtIf) {
            CtIf s = (CtIf) el;
            node.put("condition", safe(s.getCondition()));
            node.put("hasThen",   s.getThenStatement() != null);
            node.put("hasElse",   s.getElseStatement() != null);
        }

        // For loop
        if (el instanceof CtFor) {
            node.put("condition", safe(((CtFor) el).getExpression()));
        }

        // ForEach loop
        if (el instanceof CtForEach) {
            CtForEach s = (CtForEach) el;
            node.put("variable", safe(s.getVariable()));
            node.put("iterable", safe(s.getExpression()));
        }

        // While loop
        if (el instanceof CtWhile) {
            node.put("condition", safe(((CtWhile) el).getLoopingExpression()));
        }

        // Try-catch
        if (el instanceof CtTry) {
            CtTry s = (CtTry) el;
            ArrayNode catchTypes = node.putArray("catchTypes");
            for (CtCatch cb : s.getCatchers()) {
                try { 
                    catchTypes.add(safe(cb.getParameter().getType().getQualifiedName())); 
                }
                catch (Exception ignored) {}
            }
            node.put("hasFinally", s.getFinalizer() != null);
        }

        // Local variable
        if (el instanceof CtLocalVariable) {
            CtLocalVariable<?> lv = (CtLocalVariable<?>) el;
            node.put("isFinal", lv.isFinal());
            try { node.put("initializer", safe(lv.getDefaultExpression())); }
            catch (Exception ignored) {}
        }

        // Return
        if (el instanceof CtReturn) {
            node.put("returnedExpression",
                     safe(((CtReturn<?>) el).getReturnedExpression()));
        }

        // Assignment
        if (el instanceof CtAssignment) {
            CtAssignment<?,?> a = (CtAssignment<?,?>) el;
            node.put("target",     safe(a.getAssigned()));
            node.put("expression", safe(a.getAssignment()));
        }

        // Binary operator
        if (el instanceof CtBinaryOperator) {
            CtBinaryOperator<?> op = (CtBinaryOperator<?>) el;
            node.put("operator", safe(op.getKind()));
            node.put("left",     safe(op.getLeftHandOperand()));
            node.put("right",    safe(op.getRightHandOperand()));
        }

        // Unary operator
        if (el instanceof CtUnaryOperator) {
            CtUnaryOperator<?> op = (CtUnaryOperator<?>) el;
            node.put("operator",  safe(op.getKind()));
            node.put("operand",   safe(op.getOperand()));
        }

        // Literal
        if (el instanceof CtLiteral) {
            try { node.put("value", safe(((CtLiteral<?>) el).getValue())); }
            catch (Exception ignored) {}
        }

        // Variable read / write
        if (el instanceof CtVariableAccess) {
            node.put("variable", safe(((CtVariableAccess<?>) el).getVariable()));
        }

        // Type reference
        if (el instanceof CtTypeReference) {
            node.put("qualifiedName", safe(((CtTypeReference<?>) el).getQualifiedName()));
        }

        // Lambda
        if (el instanceof CtLambda) {
            node.put("isImplicitReturn", ((CtLambda<?>) el).getBody() == null);
        }

        // Throw
        if (el instanceof CtThrow) {
            node.put("thrownExpression", safe(((CtThrow) el).getThrownExpression()));
        }

        // New object
        if (el instanceof CtConstructorCall) {
            CtConstructorCall<?> cc = (CtConstructorCall<?>) el;
            node.put("instantiatedType",
                     safe(cc.getType() != null ? cc.getType().getQualifiedName() : null));
        }

        // Array access
        if (el instanceof CtArrayAccess) {
            CtArrayAccess<?,?> aa = (CtArrayAccess<?,?>) el;
            node.put("target", safe(aa.getTarget()));
            node.put("index",  safe(aa.getIndexExpression()));
        }

        // Conditional (ternary)
        if (el instanceof CtConditional) {
            CtConditional<?> c = (CtConditional<?>) el;
            node.put("condition",  safe(c.getCondition()));
            node.put("thenExpr",   safe(c.getThenExpression()));
            node.put("elseExpr",   safe(c.getElseExpression()));
        }

        // Switch
        if (el instanceof CtSwitch) {
            node.put("selector", safe(((CtSwitch<?>) el).getSelector()));
        }

        // Parameter
        if (el instanceof CtParameter) {
            CtParameter<?> p = (CtParameter<?>) el;
            node.put("isVarArgs", p.isVarArgs());
            node.put("isFinal",   p.isFinal());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASS 2 — DEPENDENCY / CALL GRAPH EXTRACTION  (Neo4j)
    // ─────────────────────────────────────────────────────────────────────────

    static void extractCallGraph(CtModel model) {

        // ── sub-pass A: classes + inheritance ────────────────────────────────
        for (CtType<?> type : model.getAllTypes()) {
            if (type.getSimpleName().contains("$") ||
                type.getQualifiedName().contains("$")) continue;

            ClassNode cn     = new ClassNode();
            cn.id            = classId(type);
            cn.qualifiedName = type.getQualifiedName();
            cn.simpleName    = type.getSimpleName();
            cn.packageName   = type.getPackage() != null
                                ? type.getPackage().getQualifiedName() : "default";
            cn.isInterface   = (type instanceof CtInterface);
            cn.isAbstract    = type.isAbstract();
            cn.isEnum        = (type instanceof CtEnum);
            cn.astDocId      = cn.qualifiedName;   // MongoDB _id
            classes.put(cn.id, cn);

            ensurePackage(cn.packageName);
            edges.add(edge(cn.id, cn.packageName, "BELONGS_TO"));

            try {
                if (type instanceof CtClass) {
                    CtTypeReference<?> sup = ((CtClass<?>) type).getSuperclass();
                    if (sup != null &&
                        !sup.getQualifiedName().equals("java.lang.Object"))
                        edges.add(edge(cn.id, sup.getQualifiedName(), "EXTENDS"));
                }
            } catch (Exception ignored) {}

            try {
                for (CtTypeReference<?> iface : type.getSuperInterfaces())
                    edges.add(edge(cn.id, iface.getQualifiedName(), "IMPLEMENTS"));
            } catch (Exception ignored) {}
        }

        // ── sub-pass B: methods + calls + overrides ───────────────────────────
        for (CtType<?> type : model.getAllTypes()) {
            if (type.getSimpleName().contains("$") ||
                type.getQualifiedName().contains("$")) continue;

            String cqn = classId(type);

            for (CtMethod<?> method : type.getMethods()) {
                String mId = methodId(method);

                MethodNode mn    = new MethodNode();
                mn.id            = mId;
                mn.qualifiedName = mId;
                mn.simpleName    = method.getSimpleName();
                mn.className     = cqn;
                mn.packageName   = type.getPackage() != null
                                    ? type.getPackage().getQualifiedName() : "default";
                mn.isPublic      = method.isPublic();
                mn.isPrivate     = method.isPrivate();
                mn.isProtected   = method.isProtected();
                mn.isStatic      = method.isStatic();
                mn.isAbstract    = method.isAbstract();
                mn.lineNumber    = method.getPosition().isValidPosition()
                                    ? method.getPosition().getLine() : -1;
                mn.astDocId      = cqn;
                try   { 
                    if (method.getType() != null) {
                        mn.returnType = method.getType().getQualifiedName(); 
                    } else {
                        mn.returnType = "void";
                    }
                }
                catch (Exception e) { mn.returnType = "unknown"; }
                for (CtParameter<?> p : method.getParameters()) {
                    try   { 
                        if (p.getType() != null) {
                            mn.paramTypes.add(p.getType().getQualifiedName()); 
                        } else {
                            mn.paramTypes.add("unknown");
                        }
                    }
                    catch (Exception e) { mn.paramTypes.add("unknown"); }
                }
                methods.put(mId, mn);
                edges.add(edge(cqn, mId, "HAS_METHOD"));

                // OVERRIDES
                try {
                    if (type instanceof CtClass) {
                        CtTypeReference<?> sr = ((CtClass<?>) type).getSuperclass();
                        if (sr != null) {
                            CtType<?> st = sr.getTypeDeclaration();
                            if (st != null)
                                for (CtMethod<?> sm : st.getMethods())
                                    if (sm.getSimpleName().equals(method.getSimpleName())
                                     && sm.getParameters().size()
                                        == method.getParameters().size())
                                        edges.add(edge(mId, methodId(sm), "OVERRIDES"));
                        }
                    }
                } catch (Exception ignored) {}

                // CALLS / CALLS_EXTERNAL
                try {
                    Map<String, Integer> ccMap   = new LinkedHashMap<>();
                    Map<String, Integer> lineMap  = new LinkedHashMap<>();

                    for (CtInvocation<?> inv :
                            method.getElements(new TypeFilter<>(CtInvocation.class))) {
                        try {
                            String cn2  = "UnresolvedType";
                            String name = inv.getExecutable().getSimpleName();
                            int    ln   = inv.getPosition().isValidPosition()
                                           ? inv.getPosition().getLine() : -1;
                            if (inv.getExecutable().getDeclaringType() != null)
                                cn2 = inv.getExecutable()
                                         .getDeclaringType().getQualifiedName();
                            String cid = cn2 + "#" + name;
                            ccMap.merge(cid, 1, Integer::sum);
                            lineMap.putIfAbsent(cid, ln);

                            boolean ext = isExternalClass(cn2);
                            if (ext && !methods.containsKey(cid)) {
                                MethodNode em   = new MethodNode();
                                em.id           = em.qualifiedName = cid;
                                em.simpleName   = name;
                                em.className    = cn2;
                                em.packageName  = cn2.contains(".")
                                    ? cn2.substring(0, cn2.lastIndexOf('.'))
                                    : "external";
                                em.isExternal   = true;
                                methods.put(cid, em);
                            }
                            GraphEdge e = edge(mId, cid,
                                               ext ? "CALLS_EXTERNAL" : "CALLS");
                            e.props.put("lineNumber", lineMap.get(cid));
                            e.props.put("callCount",  ccMap.get(cid));
                            edges.add(e);
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN
    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {

        // ── CONFIG ─────────────────────────────────────────────────────────────
        String rootPath     = "C:/Users/PrabalBhardwaj/Desktop/osttra-harmony3/";
        String graphJson    = rootPath + "dependency_graph.json";
        String astDir       = rootPath + "ast/";
        String[] srcFolders = {"com", "db", "harmonyEar"};

        new File(astDir).mkdirs();

        // ── SPOON ──────────────────────────────────────────────────────────────
        Launcher launcher = new Launcher();
        for (String folder : srcFolders) {
            File dir = new File(rootPath + folder);
            if (dir.exists()) launcher.addInputResource(dir.getAbsolutePath());
        }
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
        launcher.getEnvironment().setIgnoreSyntaxErrors(true);
        launcher.getEnvironment().setComplianceLevel(11);

        System.out.println("Building Spoon model...");
        launcher.buildModel();
        CtModel model = launcher.getModel();
        System.out.println("Model ready.\n");

        // ── PASS 1: FULL RECURSIVE AST → per-class JSON files ─────────────────
        System.out.println("Pass 1: Serializing full AST...");
        int astCount    = 0;
        int astErrors   = 0;
        int totalTypes  = 0;

        for (CtType<?> type : model.getAllTypes()) {
            totalTypes++;
            if (type.getSimpleName().contains("$") ||
                type.getQualifiedName().contains("$")) continue;

            try {
                // root AST document: the CtType itself, recursed fully
                ObjectNode doc = serializeElement(type, null, 0);

                // add MongoDB _id at top level for easy lookup
                doc.put("_id", type.getQualifiedName());

                String fname = type.getQualifiedName()
                                   .replace(".", "_") + ".json";
                mapper.writerWithDefaultPrettyPrinter()
                      .writeValue(new File(astDir + fname), doc);
                astCount++;

                if (astCount % 50 == 0)
                    System.out.println("  serialized " + astCount + " classes...");

            } catch (Exception e) {
                astErrors++;
                System.err.println("  AST error: " + type.getQualifiedName()
                                   + " — " + e.getMessage());
            }
        }
        System.out.println("  ✓ " + astCount + " AST files written"
                           + (astErrors > 0 ? " (" + astErrors + " errors)" : ""));

        // ── PASS 2: CALL GRAPH → dependency_graph.json ─────────────────────────
        System.out.println("\nPass 2: Extracting dependency graph...");
        extractCallGraph(model);

        ObjectNode root  = mapper.createObjectNode();
        ObjectNode nObj  = root.putObject("nodes");
        nObj.set("methods",  mapper.valueToTree(methods.values()));
        nObj.set("classes",  mapper.valueToTree(classes.values()));
        nObj.set("packages", mapper.valueToTree(packages.values()));
        root.set("edges", mapper.valueToTree(edges));

        long intM = methods.values().stream().filter(m -> !m.isExternal).count();
        long extM = methods.values().stream().filter(m ->  m.isExternal).count();

        ObjectNode st = root.putObject("stats");
        st.put("totalTypes",            totalTypes);
        st.put("totalInternalMethods",  intM);
        st.put("totalExternalMethods",  extM);
        st.put("totalClasses",          classes.size());
        st.put("totalPackages",         packages.size());
        st.put("totalEdges",            edges.size());
        st.put("astFilesWritten",       astCount);

        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(new File(graphJson), root);

        System.out.println("  ✓ dependency_graph.json written");
        System.out.println("\n════════════════════════════════════════════════");
        System.out.println("  AST files (MongoDB)  → " + astDir);
        System.out.println("  Call graph (Neo4j)   → " + graphJson);
        System.out.println("────────────────────────────────────────────────");
        System.out.println("  Types parsed         : " + totalTypes);
        System.out.println("  AST files written    : " + astCount);
        System.out.println("  Internal methods     : " + intM);
        System.out.println("  External methods     : " + extM);
        System.out.println("  Classes (Neo4j)      : " + classes.size());
        System.out.println("  Packages (Neo4j)     : " + packages.size());
        System.out.println("  Graph edges          : " + edges.size());
        System.out.println("════════════════════════════════════════════════");
        System.out.println("  Next:");
        System.out.println("    python neo4j_loader.py   ← load call graph");
        System.out.println("    python mongo_loader.py   ← load AST files");
        System.out.println("════════════════════════════════════════════════");
    }
}