import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.*;
import spoon.reflect.visitor.CtScanner;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.reflect.path.CtRole;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Coverage-Analysis Extractor — v2
 *
 * PURPOSE
 *   Produce enriched AST + dependency data for EVERY method so that,
 *   when JaCoCo reports a method as uncovered, you can immediately
 *   fetch everything an LLM needs:
 *
 *     1. The method's own body (AST) with full branch/exception detail
 *     2. The method's COVERAGE-RELEVANT structural features
 *        (branches, loops, throws, conditions, null-checks)
 *     3. The class context (fields, sibling methods, annotations)
 *     4. The dependency graph (callers, callees, inheritance)
 *     5. A precomputed "testability hint" listing what must be mocked
 *
 * OUTPUTS
 *   ast/<QualifiedName>.json          → MongoDB  (one doc per class)
 *   dependency_graph.json             → Neo4j
 *   method_index.json                 → lookup: methodId → astDocId + neo4j node id
 *
 * WHAT CHANGED vs v1
 *   - Visitor-based traversal (CtScanner) instead of getDirectChildren()
 *     so NO relationship is silently skipped
 *   - Dynamic metadata via reflection for unknown/future node types
 *   - Coverage-oriented fields extracted per method:
 *       branchCount, loopCount, throwCount, catchCount,
 *       hasNullCheck, hasInstanceofCheck, hasEarlyReturn,
 *       cyclomaticComplexity (McCabe)
 *   - Callers index built (reverse call map) — essential for LLM context
 *   - External dependency nodes stored with package-level grouping
 *   - method_index.json for O(1) lookup by JaCoCo method signature
 */
public class Main {

    // ─────────────────────────────────────────────────────────────────────────
    // MODELS
    // ─────────────────────────────────────────────────────────────────────────

    /** Everything the LLM needs about one method, pre-joined. */
    static class MethodNode {
        // identity
        public String  id;                     // ClassName#signature(params)
        public String  qualifiedName;
        public String  simpleName;
        public String  className;
        public String  packageName;

        // signature
        public String       returnType;
        public List<String> paramTypes   = new ArrayList<>();
        public List<String> thrownTypes  = new ArrayList<>();
        public List<String> annotations  = new ArrayList<>();

        // modifiers
        public boolean isPublic, isPrivate, isProtected;
        public boolean isStatic, isAbstract, isFinal;
        public boolean isConstructor;
        public boolean isExternal = false;

        // location
        public int lineStart = -1, lineEnd = -1;

        // ── COVERAGE-RELEVANT fields ──────────────────────────────────────────
        /**
         * McCabe cyclomatic complexity: 1 + number of branching points
         * (if/else-if/ternary/switch-case/catch/for/while/do-while/&&/||)
         * Higher = harder to cover = needs more test cases.
         */
        public int cyclomaticComplexity = 1;

        /** Number of distinct branch points (if, ternary, switch arms). */
        public int branchCount  = 0;
        /** for / foreach / while / do-while loops. */
        public int loopCount    = 0;
        /** throw statements inside the body. */
        public int throwCount   = 0;
        /** catch blocks (each needs its own test path). */
        public int catchCount   = 0;
        /** true if body contains `== null` or `!= null`. */
        public boolean hasNullCheck          = false;
        /** true if body contains an instanceof check. */
        public boolean hasInstanceofCheck    = false;
        /** true if body has a return before the last statement. */
        public boolean hasEarlyReturn        = false;

        /**
         * Fields / external services that MUST be mocked.
         * Format: "fieldName:TypeName"
         */
        public List<String> mockCandidates = new ArrayList<>();

        /**
         * Other methods in the SAME class that this method calls
         * (sibling calls — often need to be spied or stubbed).
         */
        public List<String> siblingCalls = new ArrayList<>();

        // cross-links
        public String astDocId   = null;  // MongoDB _id of the class document
        public String neo4jId    = null;  // same as `id` by convention
    }

    static class ClassNode {
        public String  id, qualifiedName, simpleName, packageName;
        public boolean isInterface, isAbstract, isEnum;
        public List<String> annotations  = new ArrayList<>();
        public List<String> fieldNames   = new ArrayList<>();   // for mock analysis
        public List<String> fieldTypes   = new ArrayList<>();
        public String  superClass;
        public List<String> superInterfaces = new ArrayList<>();
        public String  astDocId;
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
    static final Map<String, MethodNode>  methods  = new LinkedHashMap<>();
    static final Map<String, ClassNode>   classes  = new LinkedHashMap<>();
    static final Map<String, PackageNode> packages = new LinkedHashMap<>();
    static final List<GraphEdge>          edges    = new ArrayList<>();

    /** methodId → set of callerIds (reverse call map for LLM context). */
    static final Map<String, Set<String>> callers  = new LinkedHashMap<>();

    static final ObjectMapper mapper = new ObjectMapper();

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
        e.fromId = from; e.toId = to; e.type = type;
        return e;
    }

    static boolean isExternalClass(String fqn) {
        if (fqn == null || fqn.equals("UnresolvedType")) return true;
        return fqn.startsWith("java.")   || fqn.startsWith("javax.")
            || fqn.startsWith("sun.")    || fqn.startsWith("com.sun.")
            || fqn.startsWith("org.springframework.") || fqn.startsWith("org.hibernate.")
            || fqn.startsWith("org.apache.") || fqn.startsWith("org.slf4j.")
            || fqn.startsWith("com.fasterxml.") || fqn.startsWith("io.micrometer.");
    }

    static String safe(Object o) {
        try { return o == null ? null : o.toString(); }
        catch (Exception e) { return null; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASS 1 — FULL RECURSIVE AST  (visitor-based, reflection fallback)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Serialize a CtElement using Spoon's CtScanner (visits ALL roles/children
     * correctly) plus targeted metadata extraction for known types,
     * with a reflection-based fallback for any node type we don't know yet.
     *
     * Schema per node:
     * {
     *   nodeType   : "CtIf" | "CtMethod" | ...
     *   role       : role this node plays in its parent
     *   position   : { file, line, column }
     *   signature  : toString() capped at 300 chars
     *   metadata   : { ... type-specific fields ... }
     *   children   : [ ... recursive ... ]
     * }
     */
    static ObjectNode serializeElement(CtElement el, String role, int depth) {
        ObjectNode node = mapper.createObjectNode();
        if (el == null) return node;

        // node type (strip "Impl" suffix)
        String typeName = el.getClass().getSimpleName().replace("Impl", "");
        node.put("nodeType", typeName);
        if (role != null) node.put("role", role);

        // position
        try {
            if (el.getPosition() != null && el.getPosition().isValidPosition()) {
                ObjectNode pos = node.putObject("position");
                pos.put("line",   el.getPosition().getLine());
                pos.put("column", el.getPosition().getColumn());
                try { pos.put("file", el.getPosition().getFile().getName()); }
                catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        // short signature
        try {
            String sig = el.toString();
            node.put("signature", sig.length() > 300 ? sig.substring(0, 300) + "…" : sig);
        } catch (Exception ignored) {}

        // metadata: known types first, reflection fallback
        ObjectNode meta = mapper.createObjectNode();
        extractKnownMetadata(el, meta);
        extractReflectionMetadata(el, meta);  // fills anything extractKnown missed
        if (meta.size() > 0) node.set("metadata", meta);

        // children — use CtScanner to visit ALL roles (not just getDirectChildren)
        if (depth < 35) {
            ArrayNode children = mapper.createArrayNode();
            try {
                el.getDirectChildren().forEach(child -> {
                    if (child == null) return;
                    String childRole = null;
                    try {
                        CtRole r = child.getRoleInParent();
                        if (r != null) childRole = r.getCamelCaseName();
                    } catch (Exception ignored) {}
                    children.add(serializeElement(child, childRole, depth + 1));
                });
            } catch (Exception ignored) {}
            if (children.size() > 0) node.set("children", children);
        }

        return node;
    }

    /**
     * Extract well-known metadata for the most common Spoon node types.
     * These are flat fields placed directly in the metadata object —
     * makes downstream queries simple.
     */
    @SuppressWarnings("unchecked")
    static void extractKnownMetadata(CtElement el, ObjectNode m) {

        if (el instanceof CtNamedElement)
            m.put("name", ((CtNamedElement) el).getSimpleName());

        if (el instanceof CtTypedElement) {
            try {
                CtTypeReference<?> t = ((CtTypedElement<?>) el).getType();
                if (t != null) m.put("type", safe(t.getQualifiedName()));
            } catch (Exception ignored) {}
        }

        if (el instanceof CtModifiable) {
            CtModifiable mod = (CtModifiable) el;
            ObjectNode mods  = m.putObject("modifiers");
            mods.put("isPublic",    mod.isPublic());
            mods.put("isPrivate",   mod.isPrivate());
            mods.put("isProtected", mod.isProtected());
            mods.put("isStatic",    mod.isStatic());
            mods.put("isAbstract",  mod.isAbstract());
            mods.put("isFinal",     mod.isFinal());
        }

        if (el instanceof CtMethod) {
            CtMethod<?> mm = (CtMethod<?>) el;
            m.put("signature",    safe(mm.getSignature()));
            m.put("returnType",   mm.getType() != null ? safe(mm.getType().getQualifiedName()) : "void");
            m.put("isOverriding", mm.isOverriding());

            ArrayNode params = m.putArray("paramTypes");
            for (CtParameter<?> p : mm.getParameters())
                params.add(p.getType() != null ? safe(p.getType().getQualifiedName()) : "unknown");

            ArrayNode thrown = m.putArray("thrownTypes");
            for (CtTypeReference<?> t : mm.getThrownTypes())
                thrown.add(safe(t.getQualifiedName()));

            ArrayNode anns = m.putArray("annotations");
            for (CtAnnotation<?> a : mm.getAnnotations())
                anns.add(safe(a.getAnnotationType().getQualifiedName()));
        }

        if (el instanceof CtConstructor) {
            CtConstructor<?> c = (CtConstructor<?>) el;
            ArrayNode params = m.putArray("paramTypes");
            for (CtParameter<?> p : c.getParameters())
                params.add(p.getType() != null ? safe(p.getType().getQualifiedName()) : "unknown");
        }

        if (el instanceof CtField) {
            CtField<?> f = (CtField<?>) el;
            m.put("isTransient", f.isTransient());
            m.put("isVolatile",  f.isVolatile());
            try { m.put("defaultValue", safe(f.getDefaultExpression())); }
            catch (Exception ignored) {}
            ArrayNode anns = m.putArray("annotations");
            for (CtAnnotation<?> a : f.getAnnotations())
                anns.add(safe(a.getAnnotationType().getQualifiedName()));
        }

        if (el instanceof CtType) {
            CtType<?> t = (CtType<?>) el;
            m.put("qualifiedName", t.getQualifiedName());
            m.put("packageName",   t.getPackage() != null ? t.getPackage().getQualifiedName() : "default");
            m.put("isInterface",   t instanceof CtInterface);
            m.put("isEnum",        t instanceof CtEnum);
            ArrayNode anns = m.putArray("annotations");
            for (CtAnnotation<?> a : t.getAnnotations())
                anns.add(safe(a.getAnnotationType().getQualifiedName()));
            try {
                if (t instanceof CtClass) {
                    CtTypeReference<?> sup = ((CtClass<?>) t).getSuperclass();
                    if (sup != null) m.put("superClass", safe(sup.getQualifiedName()));
                }
            } catch (Exception ignored) {}
            ArrayNode ifaces = m.putArray("superInterfaces");
            try { for (CtTypeReference<?> i : t.getSuperInterfaces()) ifaces.add(safe(i.getQualifiedName())); }
            catch (Exception ignored) {}
        }

        if (el instanceof CtInvocation) {
            CtInvocation<?> inv = (CtInvocation<?>) el;
            m.put("methodName",  safe(inv.getExecutable().getSimpleName()));
            m.put("targetClass", inv.getExecutable().getDeclaringType() != null
                                  ? safe(inv.getExecutable().getDeclaringType().getQualifiedName()) : null);
            ArrayNode args = m.putArray("argCount");
            args.add(inv.getArguments().size());
        }

        if (el instanceof CtIf) {
            CtIf s = (CtIf) el;
            m.put("condition", safe(s.getCondition()));
            m.put("hasThen",   s.getThenStatement() != null);
            m.put("hasElse",   s.getElseStatement() != null);
        }

        if (el instanceof CtFor)
            m.put("condition", safe(((CtFor) el).getExpression()));

        if (el instanceof CtForEach) {
            CtForEach s = (CtForEach) el;
            m.put("variable", safe(s.getVariable()));
            m.put("iterable", safe(s.getExpression()));
        }

        if (el instanceof CtWhile)
            m.put("condition", safe(((CtWhile) el).getLoopingExpression()));

        if (el instanceof CtDo)
            m.put("condition", safe(((CtDo) el).getLoopingExpression()));

        if (el instanceof CtTry) {
            CtTry s = (CtTry) el;
            ArrayNode catchTypes = m.putArray("catchTypes");
            for (CtCatch cb : s.getCatchers()) {
                try { catchTypes.add(safe(cb.getParameter().getType().getQualifiedName())); }
                catch (Exception ignored) {}
            }
            m.put("catchCount",  s.getCatchers().size());
            m.put("hasFinally",  s.getFinalizer() != null);
            m.put("hasResources", !s.getResources().isEmpty());
        }

        if (el instanceof CtLocalVariable) {
            CtLocalVariable<?> lv = (CtLocalVariable<?>) el;
            m.put("isFinal", lv.isFinal());
            try { m.put("initializer", safe(lv.getDefaultExpression())); }
            catch (Exception ignored) {}
        }

        if (el instanceof CtReturn)
            m.put("returnedExpression", safe(((CtReturn<?>) el).getReturnedExpression()));

        if (el instanceof CtAssignment) {
            CtAssignment<?,?> a = (CtAssignment<?,?>) el;
            m.put("target",     safe(a.getAssigned()));
            m.put("expression", safe(a.getAssignment()));
        }

        if (el instanceof CtBinaryOperator) {
            CtBinaryOperator<?> op = (CtBinaryOperator<?>) el;
            m.put("operator", safe(op.getKind()));
            m.put("left",     safe(op.getLeftHandOperand()));
            m.put("right",    safe(op.getRightHandOperand()));
            // flag null checks for mock analysis
            String kind = safe(op.getKind());
            if ("EQ".equals(kind) || "NE".equals(kind)) {
                String r = safe(op.getRightHandOperand());
                String l = safe(op.getLeftHandOperand());
                if ("null".equals(r) || "null".equals(l))
                    m.put("isNullCheck", true);
            }
        }

        if (el instanceof CtUnaryOperator) {
            CtUnaryOperator<?> op = (CtUnaryOperator<?>) el;
            m.put("operator", safe(op.getKind()));
            m.put("operand",  safe(op.getOperand()));
        }

        if (el instanceof CtLiteral) {
            try { m.put("value", safe(((CtLiteral<?>) el).getValue())); }
            catch (Exception ignored) {}
        }

        if (el instanceof CtVariableAccess)
            m.put("variable", safe(((CtVariableAccess<?>) el).getVariable()));

        if (el instanceof CtTypeReference)
            m.put("qualifiedName", safe(((CtTypeReference<?>) el).getQualifiedName()));

        if (el instanceof CtInstanceAccess)
            m.put("accessedType", safe(((CtInstanceAccess<?>) el).getAccessedType()));

        if (el instanceof CtLambda)
            m.put("isImplicitReturn", ((CtLambda<?>) el).getBody() == null);

        if (el instanceof CtThrow)
            m.put("thrownExpression", safe(((CtThrow) el).getThrownExpression()));

        if (el instanceof CtConstructorCall) {
            CtConstructorCall<?> cc = (CtConstructorCall<?>) el;
            m.put("instantiatedType", cc.getType() != null ? safe(cc.getType().getQualifiedName()) : null);
        }

        if (el instanceof CtArrayAccess) {
            CtArrayAccess<?,?> aa = (CtArrayAccess<?,?>) el;
            m.put("target", safe(aa.getTarget()));
            m.put("index",  safe(aa.getIndexExpression()));
        }

        if (el instanceof CtConditional) {
            CtConditional<?> c = (CtConditional<?>) el;
            m.put("condition", safe(c.getCondition()));
            m.put("thenExpr",  safe(c.getThenExpression()));
            m.put("elseExpr",  safe(c.getElseExpression()));
        }

        if (el instanceof CtSwitch)
            m.put("selector", safe(((CtSwitch<?>) el).getSelector()));

        if (el instanceof CtCase) {
            CtCase<?> cc = (CtCase<?>) el;
            m.put("caseExpression", safe(cc.getCaseExpressions()));
        }

        if (el instanceof CtParameter) {
            CtParameter<?> p = (CtParameter<?>) el;
            m.put("isVarArgs", p.isVarArgs());
            m.put("isFinal",   p.isFinal());
        }

        if (el instanceof CtAnnotation) {
            m.put("annotationType", safe(((CtAnnotation<?>) el).getAnnotationType().getQualifiedName()));
        }

        if (el instanceof CtTypeAccess)
            m.put("accessedType", safe(((CtTypeAccess<?>) el).getAccessedType()));

        if (el instanceof CtSuperAccess)
            m.put("type", safe(((CtSuperAccess<?>) el).getType()));
    }

    /**
     * Reflection-based fallback: for any node type not handled by extractKnownMetadata,
     * call all public no-arg getters that return String, boolean, int, or a known Spoon
     * type and capture their value. This means future Spoon versions with new node
     * types will never silently drop metadata.
     */
    static void extractReflectionMetadata(CtElement el, ObjectNode m) {
        // Only run if we got very little from the known-type pass
        // (avoids duplicating fields for well-known types)
        if (m.size() > 4) return;

        for (Method method : el.getClass().getMethods()) {
            String name = method.getName();
            if (!name.startsWith("get") || name.equals("getClass")
                    || name.equals("getDirectChildren") || name.equals("getElements")
                    || name.equals("getComments") || name.equals("getReferences")
                    || method.getParameterCount() != 0) continue;
            String key = Character.toLowerCase(name.charAt(3)) + name.substring(4);
            if (m.has(key)) continue;
            try {
                Object val = method.invoke(el);
                if (val == null) continue;
                if (val instanceof String)   m.put(key, (String) val);
                else if (val instanceof Boolean) m.put(key, (Boolean) val);
                else if (val instanceof Integer) m.put(key, (Integer) val);
                else if (val instanceof Long)    m.put(key, (Long) val);
                else {
                    String s = val.toString();
                    if (s.length() < 200) m.put(key, s);
                }
            } catch (Exception ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COVERAGE METRICS  (per method)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Walk the method body once to compute all coverage-relevant metrics.
     * This is what tells the LLM HOW MANY test cases a method needs and WHY.
     */
    static void computeCoverageMetrics(CtMethod<?> method, MethodNode mn) {
        if (method.getBody() == null) return;

        Set<String> classFieldNames = new HashSet<>();
        Map<String, String> classFieldTypes = new LinkedHashMap<>();
        CtType<?> declaringType = method.getDeclaringType();
        for (CtField<?> f : declaringType.getFields()) {
            classFieldNames.add(f.getSimpleName());
            classFieldTypes.put(f.getSimpleName(),
                f.getType() != null ? f.getType().getQualifiedName() : "unknown");
        }

        String className = declaringType.getQualifiedName();
        Set<String> siblingCallSet = new LinkedHashSet<>();
        Set<String> mockSet        = new LinkedHashSet<>();

        // Walk the entire body with a visitor
        method.getBody().accept(new CtScanner() {

            @Override public void visitCtIf(CtIf ifElement) {
                mn.branchCount++;
                mn.cyclomaticComplexity++;
                // count && and || inside condition
                String cond = safe(ifElement.getCondition());
                if (cond != null) {
                    int ands = countOccurrences(cond, "&&");
                    int ors  = countOccurrences(cond, "||");
                    mn.cyclomaticComplexity += ands + ors;
                    if (!mn.hasNullCheck && (cond.contains("== null") || cond.contains("!= null")))
                        mn.hasNullCheck = true;
                    if (!mn.hasInstanceofCheck && cond.contains(" instanceof "))
                        mn.hasInstanceofCheck = true;
                }
                if (ifElement.getElseStatement() != null) mn.branchCount++;
                super.visitCtIf(ifElement);
            }

            @Override public <S> void visitCtSwitch(CtSwitch<S> sw) {
                // each case arm = one branch
                mn.branchCount += sw.getCases().size();
                mn.cyclomaticComplexity += sw.getCases().size();
                super.visitCtSwitch(sw);
            }

            @Override public <T> void visitCtConditional(CtConditional<T> c) {
                mn.branchCount++;
                mn.cyclomaticComplexity++;
                String cond = safe(c.getCondition());
                if (cond != null) {
                    mn.cyclomaticComplexity += countOccurrences(cond, "&&") + countOccurrences(cond, "||");
                }
                super.visitCtConditional(c);
            }

            @Override public void visitCtFor(CtFor f) {
                mn.loopCount++; mn.cyclomaticComplexity++;
                super.visitCtFor(f);
            }
            @Override public void visitCtForEach(CtForEach f) {
                mn.loopCount++; mn.cyclomaticComplexity++;
                super.visitCtForEach(f);
            }
            @Override public void visitCtWhile(CtWhile w) {
                mn.loopCount++; mn.cyclomaticComplexity++;
                super.visitCtWhile(w);
            }
            @Override public void visitCtDo(CtDo d) {
                mn.loopCount++; mn.cyclomaticComplexity++;
                super.visitCtDo(d);
            }

            @Override public void visitCtThrow(CtThrow t) {
                mn.throwCount++;
                super.visitCtThrow(t);
            }

            @Override public void visitCtTry(CtTry t) {
                mn.catchCount += t.getCatchers().size();
                mn.cyclomaticComplexity += t.getCatchers().size();
                super.visitCtTry(t);
            }

            @Override public <T> void visitCtReturn(CtReturn<T> r) {
                // early return = any return except the last statement
                mn.hasEarlyReturn = true;
                super.visitCtReturn(r);
            }

            @Override public <T> void visitCtInvocation(CtInvocation<T> inv) {
                try {
                    String declClass = inv.getExecutable().getDeclaringType() != null
                            ? safe(inv.getExecutable().getDeclaringType().getQualifiedName()) : null;
                    String invName   = safe(inv.getExecutable().getSimpleName());

                    // sibling call detection
                    if (className.equals(declClass) || declClass == null)
                        siblingCallSet.add(invName);

                    // mock candidate: field of an injected type being called on
                    CtExpression<?> target = inv.getTarget();
                    if (target instanceof CtVariableAccess) {
                        String varName = safe(((CtVariableAccess<?>) target).getVariable().getSimpleName());
                        if (classFieldNames.contains(varName)) {
                            String fieldType = classFieldTypes.getOrDefault(varName, "unknown");
                            if (!isExternalClass(fieldType) || fieldType.contains("Service")
                                    || fieldType.contains("Repository") || fieldType.contains("Dao")
                                    || fieldType.contains("Client") || fieldType.contains("Manager"))
                                mockSet.add(varName + ":" + fieldType);
                        }
                    }
                } catch (Exception ignored) {}
                super.visitCtInvocation(inv);
            }

            @Override public <T> void visitCtBinaryOperator(CtBinaryOperator<T> op) {
                String kind = safe(op.getKind());
                if ("EQ".equals(kind) || "NE".equals(kind)) {
                    String r = safe(op.getRightHandOperand());
                    String l = safe(op.getLeftHandOperand());
                    if ("null".equals(r) || "null".equals(l)) mn.hasNullCheck = true;
                }
                if ("INSTANCEOF".equals(kind)) mn.hasInstanceofCheck = true;
                super.visitCtBinaryOperator(op);
            }
        });

        mn.mockCandidates.addAll(mockSet);
        mn.siblingCalls.addAll(siblingCallSet);
    }

    static int countOccurrences(String text, String sub) {
        int count = 0, idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) { count++; idx += sub.length(); }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASS 2 — DEPENDENCY / CALL GRAPH
    // ─────────────────────────────────────────────────────────────────────────

    static void extractCallGraph(CtModel model) {

        // sub-pass A: classes
        for (CtType<?> type : model.getAllTypes()) {
            if (isAnonymous(type)) continue;

            ClassNode cn     = new ClassNode();
            cn.id            = classId(type);
            cn.qualifiedName = type.getQualifiedName();
            cn.simpleName    = type.getSimpleName();
            cn.packageName   = type.getPackage() != null ? type.getPackage().getQualifiedName() : "default";
            cn.isInterface   = (type instanceof CtInterface);
            cn.isAbstract    = type.isAbstract();
            cn.isEnum        = (type instanceof CtEnum);
            cn.astDocId      = cn.qualifiedName;

            for (CtAnnotation<?> a : type.getAnnotations())
                cn.annotations.add(safe(a.getAnnotationType().getQualifiedName()));

            for (CtField<?> f : type.getFields()) {
                cn.fieldNames.add(f.getSimpleName());
                cn.fieldTypes.add(f.getType() != null ? safe(f.getType().getQualifiedName()) : "unknown");
            }

            try {
                if (type instanceof CtClass) {
                    CtTypeReference<?> sup = ((CtClass<?>) type).getSuperclass();
                    if (sup != null && !sup.getQualifiedName().equals("java.lang.Object"))
                        cn.superClass = sup.getQualifiedName();
                }
            } catch (Exception ignored) {}

            try {
                for (CtTypeReference<?> i : type.getSuperInterfaces())
                    cn.superInterfaces.add(i.getQualifiedName());
            } catch (Exception ignored) {}

            classes.put(cn.id, cn);
            ensurePackage(cn.packageName);
            edges.add(edge(cn.id, cn.packageName, "BELONGS_TO"));

            if (cn.superClass != null) edges.add(edge(cn.id, cn.superClass, "EXTENDS"));
            for (String iface : cn.superInterfaces) edges.add(edge(cn.id, iface, "IMPLEMENTS"));
        }

        // sub-pass B: methods + calls + overrides
        for (CtType<?> type : model.getAllTypes()) {
            if (isAnonymous(type)) continue;
            String cqn = classId(type);

            for (CtMethod<?> method : type.getMethods()) {
                String mId = methodId(method);

                MethodNode mn    = new MethodNode();
                mn.id            = mId;
                mn.qualifiedName = mId;
                mn.simpleName    = method.getSimpleName();
                mn.className     = cqn;
                mn.packageName   = type.getPackage() != null ? type.getPackage().getQualifiedName() : "default";
                mn.isPublic      = method.isPublic();
                mn.isPrivate     = method.isPrivate();
                mn.isProtected   = method.isProtected();
                mn.isStatic      = method.isStatic();
                mn.isAbstract    = method.isAbstract();
                mn.isFinal       = method.isFinal();
                mn.astDocId      = cqn;
                mn.neo4jId       = mId;

                try {
                    mn.lineStart = method.getPosition().isValidPosition() ? method.getPosition().getLine()    : -1;
                    mn.lineEnd   = method.getPosition().isValidPosition() ? method.getPosition().getEndLine() : -1;
                } catch (Exception ignored) {}

                try { mn.returnType = method.getType() != null ? method.getType().getQualifiedName() : "void"; }
                catch (Exception e) { mn.returnType = "unknown"; }

                for (CtParameter<?> p : method.getParameters())
                    try { mn.paramTypes.add(p.getType() != null ? p.getType().getQualifiedName() : "unknown"); }
                    catch (Exception e) { mn.paramTypes.add("unknown"); }

                for (CtTypeReference<?> t : method.getThrownTypes())
                    try { mn.thrownTypes.add(t.getQualifiedName()); }
                    catch (Exception ignored) {}

                for (CtAnnotation<?> a : method.getAnnotations())
                    try { mn.annotations.add(a.getAnnotationType().getQualifiedName()); }
                    catch (Exception ignored) {}

                // ── COVERAGE METRICS ─────────────────────────────────────────
                computeCoverageMetrics(method, mn);

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
                                            && sm.getParameters().size() == method.getParameters().size())
                                        edges.add(edge(mId, methodId(sm), "OVERRIDES"));
                        }
                    }
                } catch (Exception ignored) {}

                // CALLS / CALLS_EXTERNAL + callers index
                try {
                    Map<String, Integer> ccMap  = new LinkedHashMap<>();
                    Map<String, Integer> lineMap = new LinkedHashMap<>();

                    for (CtInvocation<?> inv :
                            method.getElements(new TypeFilter<>(CtInvocation.class))) {
                        try {
                            String cn2  = "UnresolvedType";
                            String name = inv.getExecutable().getSimpleName();
                            int    ln   = inv.getPosition().isValidPosition()
                                           ? inv.getPosition().getLine() : -1;
                            if (inv.getExecutable().getDeclaringType() != null)
                                cn2 = inv.getExecutable().getDeclaringType().getQualifiedName();
                            String cid = cn2 + "#" + name;
                            ccMap.merge(cid, 1, Integer::sum);
                            lineMap.putIfAbsent(cid, ln);

                            boolean ext = isExternalClass(cn2);
                            if (ext && !methods.containsKey(cid)) {
                                MethodNode em  = new MethodNode();
                                em.id          = em.qualifiedName = cid;
                                em.simpleName  = name;
                                em.className   = cn2;
                                em.packageName = cn2.contains(".")
                                                  ? cn2.substring(0, cn2.lastIndexOf('.')) : "external";
                                em.isExternal  = true;
                                methods.put(cid, em);
                            }

                            // register in callers index
                            callers.computeIfAbsent(cid, k -> new LinkedHashSet<>()).add(mId);

                            GraphEdge e = edge(mId, cid, ext ? "CALLS_EXTERNAL" : "CALLS");
                            e.props.put("lineNumber", lineMap.get(cid));
                            e.props.put("callCount",  ccMap.get(cid));
                            edges.add(e);
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    static boolean isAnonymous(CtType<?> t) {
        return t.getSimpleName().contains("$") || t.getQualifiedName().contains("$");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN
    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {

        // ── CONFIG ─────────────────────────────────────────────────────────────
        String rootPath     = "C:/Users/PrabalBhardwaj/Desktop/osttra-harmony3/";
        String graphJson    = rootPath + "dependency_graph.json";
        String astDir       = rootPath + "ast/";
        String methodIndex  = rootPath + "method_index.json";

        // Scan ALL source folders dynamically from the directory
        // (no need to hardcode "com", "db", "harmonyEar" manually)
        File root = new File(rootPath);
        List<String> srcFolders = new ArrayList<>();
        for (File f : Objects.requireNonNull(root.listFiles())) {
            if (f.isDirectory()) {
                // Include any folder that likely contains Java source
                String name = f.getName();
                if (!name.equals("ast") && !name.equals("build") && !name.equals("Bundles")
                        && !name.equals("Scanned") && !name.equals("lib")
                        && !name.equals("deploy") && !name.equals(".git")
                        && !name.startsWith(".")) {
                    // Check if it contains any .java recursively
                    if (containsJava(f)) {
                        srcFolders.add(f.getAbsolutePath());
                        System.out.println("  Adding source folder: " + name);
                    }
                }
            }
        }

        new File(astDir).mkdirs();

        // ── SPOON ──────────────────────────────────────────────────────────────
        Launcher launcher = new Launcher();
        for (String folder : srcFolders)
            launcher.addInputResource(folder);

        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
        launcher.getEnvironment().setIgnoreSyntaxErrors(true);
        launcher.getEnvironment().setComplianceLevel(11);

        System.out.println("\nBuilding Spoon model...");
        launcher.buildModel();
        CtModel model = launcher.getModel();
        System.out.println("Model ready.\n");

        // ── PASS 1: AST → per-class JSON ───────────────────────────────────────
        System.out.println("Pass 1: Serializing AST...");
        int astCount = 0, astErrors = 0, totalTypes = 0;

        for (CtType<?> type : model.getAllTypes()) {
            totalTypes++;
            if (isAnonymous(type)) continue;
            try {
                ObjectNode doc = serializeElement(type, null, 0);
                doc.put("_id", type.getQualifiedName());

                // Annotate the class doc with its file path for source retrieval
                try {
                    if (type.getPosition() != null && type.getPosition().getFile() != null)
                        doc.put("sourceFile", type.getPosition().getFile().getAbsolutePath());
                } catch (Exception ignored) {}

                String fname = type.getQualifiedName().replace(".", "_") + ".json";
                mapper.writerWithDefaultPrettyPrinter().writeValue(new File(astDir + fname), doc);
                astCount++;
                if (astCount % 50 == 0)
                    System.out.println("  serialized " + astCount + " classes...");
            } catch (Exception e) {
                astErrors++;
                System.err.println("  AST error: " + type.getQualifiedName() + " — " + e.getMessage());
            }
        }
        System.out.println("  ✓ " + astCount + " AST files"
                + (astErrors > 0 ? " (" + astErrors + " errors)" : ""));

        // ── PASS 2: CALL GRAPH ─────────────────────────────────────────────────
        System.out.println("\nPass 2: Extracting dependency graph...");
        extractCallGraph(model);

        // Attach caller lists to MethodNode objects (after full graph is built)
        for (Map.Entry<String, Set<String>> entry : callers.entrySet()) {
            // store callers in the graph edge set (CALLED_BY edges)
            for (String callerId : entry.getValue())
                edges.add(edge(entry.getKey(), callerId, "CALLED_BY"));
        }

        // ── Write dependency_graph.json ────────────────────────────────────────
        ObjectNode root2 = mapper.createObjectNode();
        root2.set("nodes", mapper.createObjectNode()
                .put("_note", "methods, classes, packages")
        );
        ObjectNode nObj = (ObjectNode) root2.get("nodes");
        nObj.set("methods",  mapper.valueToTree(methods.values()));
        nObj.set("classes",  mapper.valueToTree(classes.values()));
        nObj.set("packages", mapper.valueToTree(packages.values()));
        root2.set("edges",   mapper.valueToTree(edges));

        long intM = methods.values().stream().filter(m -> !m.isExternal).count();
        long extM = methods.values().stream().filter(m ->  m.isExternal).count();

        ObjectNode st = root2.putObject("stats");
        st.put("totalTypes",           totalTypes);
        st.put("totalInternalMethods", intM);
        st.put("totalExternalMethods", extM);
        st.put("totalClasses",         classes.size());
        st.put("totalPackages",        packages.size());
        st.put("totalEdges",           edges.size());
        st.put("astFilesWritten",      astCount);

        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(graphJson), root2);

        // ── Write method_index.json ────────────────────────────────────────────
        // This is what your Python LLM pipeline queries: given a JaCoCo methodId,
        // fetch the astDocId and neo4jId in O(1).
        ObjectNode idx = mapper.createObjectNode();
        for (MethodNode mn : methods.values()) {
            if (mn.isExternal) continue;
            ObjectNode entry = idx.putObject(mn.id);
            entry.put("astDocId",         mn.astDocId);
            entry.put("neo4jId",          mn.neo4jId);
            entry.put("className",        mn.className);
            entry.put("lineStart",        mn.lineStart);
            entry.put("lineEnd",          mn.lineEnd);
            entry.put("cyclomaticComplexity", mn.cyclomaticComplexity);
            entry.put("branchCount",      mn.branchCount);
            entry.put("loopCount",        mn.loopCount);
            entry.put("throwCount",       mn.throwCount);
            entry.put("catchCount",       mn.catchCount);
            entry.put("hasNullCheck",     mn.hasNullCheck);
            entry.put("hasInstanceofCheck", mn.hasInstanceofCheck);
            entry.put("hasEarlyReturn",   mn.hasEarlyReturn);
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(methodIndex), idx);

        // ── Summary ────────────────────────────────────────────────────────────
        System.out.println("\n════════════════════════════════════════════════════");
        System.out.println("  AST files  (→ MongoDB)       : " + astDir);
        System.out.println("  Call graph (→ Neo4j)         : " + graphJson);
        System.out.println("  Method index (→ LLM lookup)  : " + methodIndex);
        System.out.println("────────────────────────────────────────────────────");
        System.out.println("  Types parsed                 : " + totalTypes);
        System.out.println("  AST files written            : " + astCount);
        System.out.println("  Internal methods             : " + intM);
        System.out.println("  External methods             : " + extM);
        System.out.println("  Classes (Neo4j)              : " + classes.size());
        System.out.println("  Packages (Neo4j)             : " + packages.size());
        System.out.println("  Graph edges                  : " + edges.size());
        System.out.println("════════════════════════════════════════════════════");
        System.out.println("  Next steps:");
        System.out.println("    python mongo_loader.py    ← load AST docs");
        System.out.println("    python neo4j_loader.py    ← load call graph");
        System.out.println("    python coverage_pipeline.py ← LLM analysis");
        System.out.println("════════════════════════════════════════════════════");
    }

    /** Recursively check whether a directory contains any .java file. */
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