package backend;

import ASTNode.*;
import Token.TokenType;
import semantic.SymbolType;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * LLVM IR generator (no optimizations). Maintains clean block structure.
 */
public class LlvmIRGenerator {
    /* ---------- data structures ---------- */
    private static class GlobalVar {
        String name;
        boolean isArray;
        int len;
        List<Integer> dims;
        List<Integer> inits;
        boolean isConst;

        GlobalVar(String name, boolean isArray, int len, List<Integer> dims, List<Integer> inits, boolean isConst) {
            this.name = name;
            this.isArray = isArray;
            this.len = len;
            this.dims = dims;
            this.inits = inits;
            this.isConst = isConst;
        }
    }

    private static class FuncSig {
        SymbolType.BaseType ret;
        List<List<Integer>> paramDims;
    }

    private enum Storage { LOCAL, PARAM, GLOBAL, STATIC }

    private static class VarInfo {
        Storage storage;
        boolean isArray;
        List<Integer> dims;
        String ptr;
        String label;
        boolean isParam;
        boolean isConst;
        boolean isStatic;
    }

    private static class ConstInfo {
        boolean isArray;
        List<Integer> dims;
        List<Integer> values;
    }

    private static class FuncContext {
        Deque<Map<String, VarInfo>> scopes = new ArrayDeque<>();
        Deque<String> breakLabels = new ArrayDeque<>();
        Deque<String> contLabels = new ArrayDeque<>();
        boolean terminated = false;

        void enterScope() { scopes.push(new HashMap<>()); }
        void exitScope() { scopes.pop(); }
        void defineVar(String name, VarInfo vi) { scopes.peek().put(name, vi); }
        VarInfo lookupVar(String name) {
            for (Map<String, VarInfo> scope : scopes) {
                if (scope.containsKey(name)) return scope.get(name);
            }
            return null;
        }
    }

    /* ---------- state ---------- */
    private final StringBuilder funcsSb = new StringBuilder();
    private final Map<String, String> stringLiterals = new LinkedHashMap<>();
    private final Map<String, String> strContentMap = new HashMap<>();
    private final Map<String, GlobalVar> globals = new LinkedHashMap<>();
    private final Map<String, FuncSig> funcSigs = new HashMap<>();
    private final Map<String, ConstInfo> globalConstEnv = new HashMap<>();
    private final Deque<Map<String, ConstInfo>> constEnvStack = new ArrayDeque<>();
    private final Set<String> boolRegs = new HashSet<>();
    private final Set<String> cmpRegs = new HashSet<>();
    private final Map<String, String> regType = new HashMap<>();
    private int tempId = 0;
    private int labelId = 0;
    private int strId = 0;

    /* ---------- entry ---------- */
    public String generate(CompUnitNode cu) {
        resetState();
        collectGlobals(cu);
        collectFuncSigs(cu);
        emitFunctions(cu);
        StringBuilder out = new StringBuilder();
        emitPreamble(out);
        emitGlobalDefs(out);
        emitStringConsts(out);
        out.append(funcsSb);
        return out.toString();
    }

    private void resetState() {
        funcsSb.setLength(0);
        stringLiterals.clear();
        strContentMap.clear();
        globals.clear();
        funcSigs.clear();
        globalConstEnv.clear();
        constEnvStack.clear();
        boolRegs.clear();
        cmpRegs.clear();
        regType.clear();
        tempId = 0;
        labelId = 0;
        strId = 0;
    }

    /* ---------- helpers ---------- */
    private String freshReg() { return "%t" + (tempId++); }
    private String freshLabel(String prefix) { return prefix + "_" + (labelId++); }
    private String newStaticLabel(String func, String name) { return "static_" + func + "_" + name + "_" + (labelId++); }
    private String irRet(SymbolType.BaseType t) { return t == SymbolType.BaseType.INT ? "i32" : "void"; }

    /* ---------- constant evaluation ---------- */
    private int evalConstExp(ConstExpNode node, Map<String, ConstInfo> env) {
        return evalAddConst(node.getAddExpNode(), env);
    }

    private int evalConstExpFromExp(ExpNode node, Map<String, ConstInfo> env) {
        return evalAddConst(node.getAddExpNode(), env);
    }

    private int evalAddConst(AddExpNode node, Map<String, ConstInfo> env) {
        List<Integer> values = new ArrayList<>();
        List<TokenType> ops = new ArrayList<>();
        AddExpNode cur = node;
        while (true) {
            values.add(evalMulConst(cur.getMulExpNode(), env));
            if (cur.getOperator() == null) break;
            ops.add(cur.getOperator().getTokenType());
            cur = cur.getAddExpNode();
        }
        int res = values.get(0);
        for (int i = 0; i < ops.size(); i++) {
            int rhs = values.get(i + 1);
            res = (ops.get(i) == TokenType.PLUS) ? res + rhs : res - rhs;
        }
        return res;
    }

    private int evalMulConst(MulExpNode node, Map<String, ConstInfo> env) {
        List<Integer> values = new ArrayList<>();
        List<TokenType> ops = new ArrayList<>();
        MulExpNode cur = node;
        while (true) {
            values.add(evalUnaryConst(cur.getUnaryExpNode(), env));
            if (cur.getOperator() == null) break;
            ops.add(cur.getOperator().getTokenType());
            cur = cur.getMulExpNode();
        }
        int res = values.get(0);
        for (int i = 0; i < ops.size(); i++) {
            int rhs = values.get(i + 1);
            switch (ops.get(i)) {
                case MULT: res = res * rhs; break;
                case DIV: res = rhs == 0 ? 0 : res / rhs; break;
                case MOD: res = rhs == 0 ? 0 : res % rhs; break;
                default: break;
            }
        }
        return res;
    }

    private int evalUnaryConst(UnaryExpNode node, Map<String, ConstInfo> env) {
        if (node.getPrimaryExpNode() != null) {
            return evalPrimaryConst(node.getPrimaryExpNode(), env);
        } else if (node.getIdent() != null) {
            return 0;
        } else {
            int v = evalUnaryConst(node.getUnaryExpNode(), env);
            TokenType tt = node.getUnaryOpNode().getToken().getTokenType();
            if (tt == TokenType.MINU) return -v;
            if (tt == TokenType.NOT) return v == 0 ? 1 : 0;
            return v;
        }
    }

    private int evalPrimaryConst(PrimaryExpNode node, Map<String, ConstInfo> env) {
        if (node.getExpNode() != null) return evalConstExpFromExp(node.getExpNode(), env);
        if (node.getNumberNode() != null) return Integer.parseInt(node.getNumberNode().getStr());
        if (node.getLValNode() != null) return evalConstLVal(node.getLValNode(), env);
        return 0;
    }

    private int evalConstLVal(LValNode lValNode, Map<String, ConstInfo> env) {
        String name = lValNode.getIdent().getValue();
        ConstInfo ci = env.get(name);
        if (ci == null) ci = globalConstEnv.get(name);
        if (ci == null || ci.values == null || ci.values.isEmpty()) return 0;
        if (!ci.isArray || lValNode.getExpNodes().isEmpty()) return ci.values.get(0);
        int idx = 0;
        for (int i = 0; i < lValNode.getExpNodes().size(); i++) {
            int v = evalConstExpFromExp(lValNode.getExpNodes().get(i), env);
            int stride = 1;
            for (int j = i + 1; j < ci.dims.size(); j++) {
                Integer d = ci.dims.get(j);
                stride *= (d == null ? 1 : d);
            }
            idx += v * stride;
        }
        if (idx < 0 || idx >= ci.values.size()) return 0;
        return ci.values.get(idx);
    }

    private List<Integer> evalDims(List<ConstExpNode> nodes, Map<String, ConstInfo> env) {
        List<Integer> dims = new ArrayList<>();
        for (ConstExpNode ce : nodes) dims.add(evalConstExp(ce, env));
        return dims;
    }

    private int product(List<Integer> dims) {
        int p = 1;
        for (Integer d : dims) p *= (d == null ? 1 : d);
        return p;
    }

    private List<Integer> flattenConstInit(ConstInitValNode node, int len, Map<String, ConstInfo> env) {
        List<Integer> res = new ArrayList<>(Collections.nCopies(len, 0));
        int idx = 0;
        for (ConstExpNode ce : node.getConstExpNodes()) {
            if (idx >= len) break;
            res.set(idx++, evalConstExp(ce, env));
        }
        return res;
    }

    private List<Integer> flattenInitVal(InitValNode node, int len, Map<String, ConstInfo> env) {
        List<Integer> res = new ArrayList<>(Collections.nCopies(len, 0));
        int idx = 0;
        for (ExpNode exp : node.getExpNodes()) {
            if (idx >= len) break;
            res.set(idx++, evalConstExpFromExp(exp, env));
        }
        return res;
    }

    /* ---------- collection ---------- */
    private void collectGlobals(CompUnitNode cu) {
        for (DeclNode declNode : cu.getDeclNodes()) {
            if (declNode.getConstDecl() != null) {
                for (ConstDefNode def : declNode.getConstDecl().getConstDefNodes()) {
                    String name = def.getIdent().getValue();
                    List<Integer> dims = evalDims(def.getConstExpNodes(), globalConstEnv);
                    int len = Math.max(1, product(dims));
                    List<Integer> init = flattenConstInit(def.getConstInitValNode(), len, globalConstEnv);
                    GlobalVar gv = new GlobalVar(name, !dims.isEmpty(), len, dims, init, true);
                    globals.put(name, gv);
                    ConstInfo ci = new ConstInfo();
                    ci.isArray = !dims.isEmpty();
                    ci.dims = dims;
                    ci.values = init;
                    globalConstEnv.put(name, ci);
                }
            } else if (declNode.getVarDecl() != null) {
                for (VarDefNode def : declNode.getVarDecl().getVarDefNodes()) {
                    String name = def.getIdent().getValue();
                    List<Integer> dims = evalDims(def.getConstExpNodes(), globalConstEnv);
                    int len = Math.max(1, product(dims));
                    List<Integer> init = def.getInitValNode() == null
                            ? new ArrayList<>(Collections.nCopies(len, 0))
                            : flattenInitVal(def.getInitValNode(), len, globalConstEnv);
                    GlobalVar gv = new GlobalVar(name, !dims.isEmpty(), len, dims, init, false);
                    globals.put(name, gv);
                }
            }
        }
    }

    private void collectFuncSigs(CompUnitNode cu) {
        for (FuncDefNode func : cu.getFuncDefNodes()) {
            FuncSig sig = new FuncSig();
            sig.ret = func.getFuncTypeNode().getToken().getTokenType() == TokenType.VOIDTK
                    ? SymbolType.BaseType.VOID : SymbolType.BaseType.INT;
            sig.paramDims = new ArrayList<>();
            if (func.getFuncFParamsNode() != null) {
                for (FuncFParamNode p : func.getFuncFParamsNode().getFuncFParamNodes()) {
                    List<Integer> dims = new ArrayList<>();
                    if (!p.getLeftBrackets().isEmpty()) {
                        dims.add(null);
                        for (ConstExpNode ce : p.getConstExpNodes()) dims.add(evalConstExp(ce, globalConstEnv));
                    }
                    sig.paramDims.add(dims);
                }
            }
            funcSigs.put(func.getIdent().getValue(), sig);
        }
        FuncSig mainSig = new FuncSig();
        mainSig.ret = SymbolType.BaseType.INT;
        mainSig.paramDims = Collections.emptyList();
        funcSigs.put("main", mainSig);
    }

    /* ---------- module emission ---------- */
    private void emitPreamble(StringBuilder out) {
        out.append("; LLVM IR generated\n");
        out.append("declare i32 @getint()\n");
        out.append("declare void @putint(i32)\n");
        out.append("declare void @putch(i32)\n");
        out.append("declare void @putstr(i8*)\n\n");
    }

    private void emitGlobalDefs(StringBuilder out) {
        for (GlobalVar gv : globals.values()) {
            if (!gv.isArray) {
                out.append("@").append(gv.name)
                        .append(" = dso_local ").append(gv.isConst ? "constant" : "global")
                        .append(" i32 ").append(gv.inits.get(0)).append(", align 4\n");
            } else {
                out.append("@").append(gv.name)
                        .append(" = dso_local ").append(gv.isConst ? "constant" : "global")
                        .append(" [").append(gv.len).append(" x i32] [");
                for (int i = 0; i < gv.len; i++) {
                    if (i > 0) out.append(", ");
                    out.append("i32 ").append(gv.inits.get(i));
                }
                out.append("], align 4\n");
            }
        }
        if (!globals.isEmpty()) out.append("\n");
    }

    private void emitStringConsts(StringBuilder out) {
        for (String def : stringLiterals.values()) out.append(def);
        if (!stringLiterals.isEmpty()) out.append("\n");
    }

    /* ---------- functions ---------- */
    private void emitFunctions(CompUnitNode cu) {
        for (FuncDefNode func : cu.getFuncDefNodes()) emitFunction(func);
        emitMain(cu.getMainFuncDefNode());
    }

    private void emitFunction(FuncDefNode func) {
        String name = func.getIdent().getValue();
        FuncSig sig = funcSigs.get(name);
        funcsSb.append("define dso_local ").append(irRet(sig.ret)).append(" @").append(name).append("(");
        List<String> params = new ArrayList<>();
        if (func.getFuncFParamsNode() != null) {
            int idx = 0;
            for (FuncFParamNode p : func.getFuncFParamsNode().getFuncFParamNodes()) {
                List<Integer> dims = new ArrayList<>();
                if (!p.getLeftBrackets().isEmpty()) {
                    dims.add(null);
                    for (ConstExpNode ce : p.getConstExpNodes()) dims.add(evalConstExp(ce, globalConstEnv));
                    params.add("i32* %arg" + idx);
                } else {
                    params.add("i32 %arg" + idx);
                }
                idx++;
            }
        }
        funcsSb.append(String.join(", ", params)).append(") {\n");

        FuncContext ctx = new FuncContext();
        ctx.enterScope();
        constEnvStack.push(new HashMap<>());
        startBlock("entry", ctx);

        int pIdx = 0;
        if (func.getFuncFParamsNode() != null) {
            for (FuncFParamNode p : func.getFuncFParamsNode().getFuncFParamNodes()) {
                List<Integer> dims = new ArrayList<>();
                if (!p.getLeftBrackets().isEmpty()) {
                    dims.add(null);
                    for (ConstExpNode ce : p.getConstExpNodes()) dims.add(evalConstExp(ce, globalConstEnv));
                }
                if (dims.isEmpty()) {
                    String alloca = freshReg();
                    funcsSb.append("  ").append(alloca).append(" = alloca i32, align 4\n");
                    funcsSb.append("  store i32 %arg").append(pIdx).append(", i32* ").append(alloca).append(", align 4\n");
                    VarInfo vi = new VarInfo();
                    vi.storage = Storage.PARAM;
                    vi.isArray = false;
                    vi.ptr = alloca;
                    vi.dims = Collections.emptyList();
                    vi.isParam = true;
                    ctx.defineVar(p.getIdent().getValue(), vi);
                } else {
                    String alloca = freshReg();
                    funcsSb.append("  ").append(alloca).append(" = alloca i32*, align 4\n");
                    funcsSb.append("  store i32* %arg").append(pIdx).append(", i32** ").append(alloca).append(", align 4\n");
                    VarInfo vi = new VarInfo();
                    vi.storage = Storage.PARAM;
                    vi.isArray = true;
                    vi.ptr = alloca;
                    vi.dims = dims;
                    vi.isParam = true;
                    ctx.defineVar(p.getIdent().getValue(), vi);
                }
                pIdx++;
            }
        }

        emitBlock(func.getBlockNode(), ctx, name);

        if (!ctx.terminated) {
            if (sig.ret == SymbolType.BaseType.INT) funcsSb.append("  ret i32 0\n");
            else funcsSb.append("  ret void\n");
        }
        funcsSb.append("}\n\n");
        ctx.exitScope();
        constEnvStack.pop();
    }

    private void emitMain(MainFuncDefNode mainFunc) {
        funcsSb.append("define dso_local i32 @main() {\n");
        FuncContext ctx = new FuncContext();
        ctx.enterScope();
        constEnvStack.push(new HashMap<>());
        startBlock("entry", ctx);
        emitBlock(mainFunc.getBlockNode(), ctx, "main");
        if (!ctx.terminated) funcsSb.append("  ret i32 0\n");
        funcsSb.append("}\n\n");
        ctx.exitScope();
        constEnvStack.pop();
    }

    /* ---------- block & statements ---------- */
    private void startBlock(String label, FuncContext ctx) {
        funcsSb.append(label).append(":\n");
        ctx.terminated = false;
    }

    private void emitBlock(BlockNode block, FuncContext ctx, String funcName) {
        ctx.enterScope();
        constEnvStack.push(new HashMap<>());
        for (BlockItemNode item : block.getBlockItemNodes()) {
            if (ctx.terminated) break;
            if (item.getDeclNode() != null) emitDecl(item.getDeclNode(), ctx, funcName);
            else if (item.getStmtNode() != null) emitStmt(item.getStmtNode(), ctx, funcName);
        }
        constEnvStack.pop();
        ctx.exitScope();
    }

    private void emitDecl(DeclNode declNode, FuncContext ctx, String funcName) {
        if (declNode.getConstDecl() != null) {
            for (ConstDefNode def : declNode.getConstDecl().getConstDefNodes()) {
                String name = def.getIdent().getValue();
                List<Integer> dims = evalDims(def.getConstExpNodes(), mergedConstEnv());
                int len = Math.max(1, product(dims));
                List<Integer> init = flattenConstInit(def.getConstInitValNode(), len, mergedConstEnv());
                ConstInfo ci = new ConstInfo();
                ci.isArray = !dims.isEmpty();
                ci.dims = dims;
                ci.values = init;
                constEnvStack.peek().put(name, ci);
                if (dims.isEmpty()) {
                    String alloca = freshReg();
                    funcsSb.append("  ").append(alloca).append(" = alloca i32, align 4\n");
                    funcsSb.append("  store i32 ").append(init.get(0)).append(", i32* ").append(alloca).append(", align 4\n");
                    VarInfo vi = new VarInfo();
                    vi.storage = Storage.LOCAL;
                    vi.isArray = false;
                    vi.ptr = alloca;
                    vi.dims = Collections.emptyList();
                    vi.isConst = true;
                    ctx.defineVar(name, vi);
                } else {
                    String alloca = freshReg();
                    funcsSb.append("  ").append(alloca).append(" = alloca [").append(len).append(" x i32], align 4\n");
                    String base = freshReg();
                    funcsSb.append("  ").append(base).append(" = getelementptr inbounds [").append(len).append(" x i32], [")
                            .append(len).append(" x i32]* ").append(alloca).append(", i32 0, i32 0\n");
                    VarInfo vi = new VarInfo();
                    vi.storage = Storage.LOCAL;
                    vi.isArray = true;
                    vi.ptr = base;
                    vi.dims = dims;
                    vi.isConst = true;
                    ctx.defineVar(name, vi);
                    for (int i = 0; i < init.size(); i++) {
                        String elemPtr = freshReg();
                        funcsSb.append("  ").append(elemPtr).append(" = getelementptr inbounds i32, i32* ").append(base)
                                .append(", i32 ").append(i).append("\n");
                        funcsSb.append("  store i32 ").append(init.get(i)).append(", i32* ").append(elemPtr).append(", align 4\n");
                    }
                }
            }
        } else if (declNode.getVarDecl() != null) {
            boolean isStatic = declNode.getVarDecl().getStaticToken() != null;
            for (VarDefNode def : declNode.getVarDecl().getVarDefNodes()) {
                String name = def.getIdent().getValue();
                List<Integer> dims = evalDims(def.getConstExpNodes(), mergedConstEnv());
                int len = Math.max(1, product(dims));
                if (isStatic) {
                    String label = newStaticLabel(funcName, name);
                    List<Integer> init = def.getInitValNode() == null
                            ? new ArrayList<>(Collections.nCopies(len, 0))
                            : flattenInitVal(def.getInitValNode(), len, mergedConstEnv());
                    GlobalVar gv = new GlobalVar(label, !dims.isEmpty(), len, dims, init, false);
                    globals.put(label, gv);
                    VarInfo vi = new VarInfo();
                    vi.storage = Storage.STATIC;
                    vi.isArray = !dims.isEmpty();
                    vi.ptr = null;
                    vi.label = label;
                    vi.dims = dims;
                    vi.isStatic = true;
                    ctx.defineVar(name, vi);
                } else if (dims.isEmpty()) {
                    String alloca = freshReg();
                    funcsSb.append("  ").append(alloca).append(" = alloca i32, align 4\n");
                    if (def.getInitValNode() != null) {
                        String v = emitExp(def.getInitValNode().getExpNodes().get(0), ctx);
                        funcsSb.append("  store i32 ").append(v).append(", i32* ").append(alloca).append(", align 4\n");
                    }
                    VarInfo vi = new VarInfo();
                    vi.storage = Storage.LOCAL;
                    vi.isArray = false;
                    vi.ptr = alloca;
                    vi.dims = Collections.emptyList();
                    ctx.defineVar(name, vi);
                } else {
                    String alloca = freshReg();
                    funcsSb.append("  ").append(alloca).append(" = alloca [").append(len).append(" x i32], align 4\n");
                    String base = freshReg();
                    funcsSb.append("  ").append(base).append(" = getelementptr inbounds [").append(len).append(" x i32], [")
                            .append(len).append(" x i32]* ").append(alloca).append(", i32 0, i32 0\n");
                    VarInfo vi = new VarInfo();
                    vi.storage = Storage.LOCAL;
                    vi.isArray = true;
                    vi.ptr = base;
                    vi.dims = dims;
                    ctx.defineVar(name, vi);
                    if (def.getInitValNode() != null) {
                        List<ExpNode> exps = def.getInitValNode().getExpNodes();
                        for (int i = 0; i < Math.min(exps.size(), len); i++) {
                            String val = emitExp(exps.get(i), ctx);
                            String elemPtr = freshReg();
                            funcsSb.append("  ").append(elemPtr).append(" = getelementptr inbounds i32, i32* ").append(base)
                                    .append(", i32 ").append(i).append("\n");
                            funcsSb.append("  store i32 ").append(val).append(", i32* ").append(elemPtr).append(", align 4\n");
                        }
                    }
                }
            }
        }
    }

    private void emitStmt(StmtNode stmt, FuncContext ctx, String funcName) {
        switch (stmt.getType()) {
            case Block:
                emitBlock(stmt.getBlockNode(), ctx, funcName);
                break;
            case Exp:
                if (stmt.getExpNode() != null) emitExp(stmt.getExpNode(), ctx);
                break;
            case LValAssignExp: {
                String v = emitExp(stmt.getExpNode(), ctx);
                emitStoreLVal(stmt.getLValNode(), v, ctx);
                break;
            }
            case LValAssignGetint: {
                String call = freshReg();
                funcsSb.append("  ").append(call).append(" = call i32 @getint()\n");
                emitStoreLVal(stmt.getLValNode(), call, ctx);
                break;
            }
            case Printf:
                handlePrintf(stmt, ctx);
                break;
            case If:
                emitIf(stmt, ctx, funcName);
                break;
            case For:
                emitFor(stmt, ctx, funcName);
                break;
            case Break:
                if (!ctx.breakLabels.isEmpty()) {
                    funcsSb.append("  br label %").append(ctx.breakLabels.peek()).append("\n");
                    ctx.terminated = true;
                }
                break;
            case Continue:
                if (!ctx.contLabels.isEmpty()) {
                    funcsSb.append("  br label %").append(ctx.contLabels.peek()).append("\n");
                    ctx.terminated = true;
                }
                break;
            case Return:
                emitReturn(stmt, ctx, funcName);
                break;
            default:
                break;
        }
    }

    private void emitReturn(StmtNode stmt, FuncContext ctx, String funcName) {
        FuncSig sig = funcSigs.get(funcName);
        if (sig.ret == SymbolType.BaseType.VOID) {
            funcsSb.append("  ret void\n");
        } else {
            String v = stmt.getExpNode() == null ? "0" : emitExp(stmt.getExpNode(), ctx);
            funcsSb.append("  ret i32 ").append(v).append("\n");
        }
        ctx.terminated = true;
    }

    private void emitIf(StmtNode stmt, FuncContext ctx, String funcName) {
        String thenLabel = freshLabel("if_then");
        String elseLabel = stmt.getElseToken() != null ? freshLabel("if_else") : null;
        String endLabel = freshLabel("if_end");
        String cond = emitCondValue(stmt.getCondNode(), ctx);
        funcsSb.append("  br i1 ").append(cond).append(", label %").append(thenLabel).append(", label %")
                .append(elseLabel != null ? elseLabel : endLabel).append("\n");
        ctx.terminated = true;

        startBlock(thenLabel, ctx);
        emitStmt(stmt.getStmtNodes().get(0), ctx, funcName);
        if (!ctx.terminated) {
            funcsSb.append("  br label %").append(endLabel).append("\n");
            ctx.terminated = true;
        }

        if (elseLabel != null) {
            startBlock(elseLabel, ctx);
            emitStmt(stmt.getStmtNodes().get(1), ctx, funcName);
            if (!ctx.terminated) {
                funcsSb.append("  br label %").append(endLabel).append("\n");
                ctx.terminated = true;
            }
        }
        startBlock(endLabel, ctx);
    }

    private void emitFor(StmtNode stmt, FuncContext ctx, String funcName) {
        if (stmt.getForStmtNode1() != null) emitForAssign(stmt.getForStmtNode1(), ctx);
        String loopLabel = freshLabel("for_cond");
        String bodyLabel = freshLabel("for_body");
        String contLabel = freshLabel("for_cont");
        String endLabel = freshLabel("for_end");
        funcsSb.append("  br label %").append(loopLabel).append("\n");
        ctx.terminated = true;

        startBlock(loopLabel, ctx);
        String cond = stmt.getCondNode() == null ? "1" : emitCondValue(stmt.getCondNode(), ctx);
        funcsSb.append("  br i1 ").append(cond).append(", label %").append(bodyLabel)
                .append(", label %").append(endLabel).append("\n");
        ctx.terminated = true;

        startBlock(bodyLabel, ctx);
        ctx.breakLabels.push(endLabel);
        ctx.contLabels.push(contLabel);
        emitStmt(stmt.getStmtNodes().get(0), ctx, funcName);
        ctx.breakLabels.pop();
        ctx.contLabels.pop();
        if (!ctx.terminated) {
            funcsSb.append("  br label %").append(contLabel).append("\n");
            ctx.terminated = true;
        }

        startBlock(contLabel, ctx);
        if (stmt.getForStmtNode2() != null) emitForAssign(stmt.getForStmtNode2(), ctx);
        funcsSb.append("  br label %").append(loopLabel).append("\n");
        ctx.terminated = true;

        startBlock(endLabel, ctx);
    }

    private void emitForAssign(ForStmtNode forStmt, FuncContext ctx) {
        for (int i = 0; i < forStmt.getLValNodes().size(); i++) {
            String v = emitExp(forStmt.getExpNodes().get(i), ctx);
            emitStoreLVal(forStmt.getLValNodes().get(i), v, ctx);
        }
    }

    /* ---------- expressions ---------- */
    private String emitExp(ExpNode expNode, FuncContext ctx) { return emitAddExp(expNode.getAddExpNode(), ctx); }

    private String emitAddExp(AddExpNode node, FuncContext ctx) {
        List<MulExpNode> terms = new ArrayList<>();
        List<TokenType> ops = new ArrayList<>();
        AddExpNode cur = node;
        while (true) {
            terms.add(cur.getMulExpNode());
            if (cur.getOperator() == null) break;
            ops.add(cur.getOperator().getTokenType());
            cur = cur.getAddExpNode();
        }
        String res = emitMulExp(terms.get(0), ctx);
        for (int i = 0; i < ops.size(); i++) {
            String rhs = emitMulExp(terms.get(i + 1), ctx);
            String tmp = freshReg();
            if (ops.get(i) == TokenType.PLUS) funcsSb.append("  ").append(tmp).append(" = add i32 ").append(res).append(", ").append(rhs).append("\n");
            else funcsSb.append("  ").append(tmp).append(" = sub i32 ").append(res).append(", ").append(rhs).append("\n");
            res = tmp;
        }
        return res;
    }

    private String emitMulExp(MulExpNode node, FuncContext ctx) {
        List<UnaryExpNode> factors = new ArrayList<>();
        List<TokenType> ops = new ArrayList<>();
        MulExpNode cur = node;
        while (true) {
            factors.add(cur.getUnaryExpNode());
            if (cur.getOperator() == null) break;
            ops.add(cur.getOperator().getTokenType());
            cur = cur.getMulExpNode();
        }
        String res = emitUnaryExp(factors.get(0), ctx);
        for (int i = 0; i < ops.size(); i++) {
            String rhs = emitUnaryExp(factors.get(i + 1), ctx);
            String tmp = freshReg();
            switch (ops.get(i)) {
                case MULT: funcsSb.append("  ").append(tmp).append(" = mul i32 ").append(res).append(", ").append(rhs).append("\n"); break;
                case DIV: funcsSb.append("  ").append(tmp).append(" = sdiv i32 ").append(res).append(", ").append(rhs).append("\n"); break;
                case MOD: funcsSb.append("  ").append(tmp).append(" = srem i32 ").append(res).append(", ").append(rhs).append("\n"); break;
                default: break;
            }
            res = tmp;
        }
        return res;
    }

    private String emitUnaryExp(UnaryExpNode node, FuncContext ctx) {
        if (node.getPrimaryExpNode() != null) return emitPrimaryExp(node.getPrimaryExpNode(), ctx);
        if (node.getIdent() != null) return emitCall(node, ctx);
        String inner = emitUnaryExp(node.getUnaryExpNode(), ctx);
        TokenType tt = node.getUnaryOpNode().getToken().getTokenType();
        if (tt == TokenType.MINU) {
            String res = freshReg();
            funcsSb.append("  ").append(res).append(" = sub i32 0, ").append(inner).append("\n");
            return res;
        } else if (tt == TokenType.NOT) {
            String b = toBool(inner);
            String inv = freshReg();
            funcsSb.append("  ").append(inv).append(" = xor i1 ").append(b).append(", true\n");
            boolRegs.add(inv);
            cmpRegs.add(inv);
            regType.put(inv, "i1");
            String res = freshReg();
            funcsSb.append("  ").append(res).append(" = zext i1 ").append(inv).append(" to i32\n");
            return res;
        }
        return inner;
    }

    private String emitPrimaryExp(PrimaryExpNode node, FuncContext ctx) {
        if (node.getExpNode() != null) return emitExp(node.getExpNode(), ctx);
        if (node.getLValNode() != null) {
            String addr = emitLValAddress(node.getLValNode(), ctx);
            String res = freshReg();
            funcsSb.append("  ").append(res).append(" = load i32, i32* ").append(addr).append(", align 4\n");
            return res;
        }
        int val = Integer.parseInt(node.getNumberNode().getStr());
        String res = freshReg();
        funcsSb.append("  ").append(res).append(" = add i32 0, ").append(val).append("\n");
        return res;
    }

    private String emitCall(UnaryExpNode node, FuncContext ctx) {
        String funcName = node.getIdent().getValue();
        List<ExpNode> args = node.getFuncRParamsNode() == null ? Collections.emptyList() : node.getFuncRParamsNode().getExpNodes();
        List<String> argVals = new ArrayList<>();
        List<String> argTypes = new ArrayList<>();
        for (ExpNode arg : args) {
            String arrPtr = tryGetArrayPointer(arg, ctx);
            if (arrPtr != null) {
                argVals.add(arrPtr);
                argTypes.add("i32*");
            } else {
                String v = emitExp(arg, ctx);
                argVals.add(v);
                argTypes.add("i32");
            }
        }
        FuncSig sig = funcSigs.get(funcName);
        String retType = sig == null ? "i32" : irRet(sig.ret);
        String res = null;
        if (!"void".equals(retType)) {
            res = freshReg();
            funcsSb.append("  ").append(res).append(" = call ").append(retType).append(" @").append(funcName).append("(");
        } else {
            funcsSb.append("  call void @").append(funcName).append("(");
        }
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < argVals.size(); i++) parts.add(argTypes.get(i) + " " + argVals.get(i));
        funcsSb.append(String.join(", ", parts)).append(")\n");
        if (res == null) {
            String zero = freshReg();
            funcsSb.append("  ").append(zero).append(" = add i32 0, 0\n");
            return zero;
        }
        return res;
    }

    /* ---------- conditions (short-circuit) ---------- */
    private String emitCondValue(CondNode condNode, FuncContext ctx) { return emitLOrValue(condNode.getLOrExpNode(), ctx); }

    private String emitLOrValue(LOrExpNode node, FuncContext ctx) {
        if (node.getOrToken() == null) return emitLAndValue(node.getLAndExpNode(), ctx);
        String resPtr = freshReg();
        funcsSb.append("  ").append(resPtr).append(" = alloca i1\n");
        String lhs = emitLAndValue(node.getLAndExpNode(), ctx);
        funcsSb.append("  store i1 ").append(lhs).append(", i1* ").append(resPtr).append("\n");
        String rhsLabel = freshLabel("lor_rhs");
        String endLabel = freshLabel("lor_end");
        funcsSb.append("  br i1 ").append(lhs).append(", label %").append(endLabel).append(", label %").append(rhsLabel).append("\n");
        ctx.terminated = true;

        startBlock(rhsLabel, ctx);
        String rhs = emitLOrValue(node.getLOrExpNode(), ctx);
        funcsSb.append("  store i1 ").append(rhs).append(", i1* ").append(resPtr).append("\n");
        funcsSb.append("  br label %").append(endLabel).append("\n");
        ctx.terminated = true;

        startBlock(endLabel, ctx);
        String res = freshReg();
        funcsSb.append("  ").append(res).append(" = load i1, i1* ").append(resPtr).append("\n");
        boolRegs.add(res);
        return res;
    }

    private String emitLAndValue(LAndExpNode node, FuncContext ctx) {
        if (node.getAndToken() == null) return emitEqValue(node.getEqExpNode(), ctx);
        String resPtr = freshReg();
        funcsSb.append("  ").append(resPtr).append(" = alloca i1\n");
        String lhs = emitEqValue(node.getEqExpNode(), ctx);
        funcsSb.append("  store i1 ").append(lhs).append(", i1* ").append(resPtr).append("\n");
        String rhsLabel = freshLabel("land_rhs");
        String endLabel = freshLabel("land_end");
        funcsSb.append("  br i1 ").append(lhs).append(", label %").append(rhsLabel).append(", label %").append(endLabel).append("\n");
        ctx.terminated = true;

        startBlock(rhsLabel, ctx);
        String rhs = emitLAndValue(node.getLAndExpNode(), ctx);
        funcsSb.append("  store i1 ").append(rhs).append(", i1* ").append(resPtr).append("\n");
        funcsSb.append("  br label %").append(endLabel).append("\n");
        ctx.terminated = true;

        startBlock(endLabel, ctx);
        String res = freshReg();
        funcsSb.append("  ").append(res).append(" = load i1, i1* ").append(resPtr).append("\n");
        boolRegs.add(res);
        return res;
    }

    private String emitEqValue(EqExpNode node, FuncContext ctx) {
        List<RelExpNode> terms = new ArrayList<>();
        List<TokenType> ops = new ArrayList<>();
        EqExpNode cur = node;
        while (true) {
            terms.add(cur.getRelExpNode());
            if (cur.getOperator() == null) break;
            ops.add(cur.getOperator().getTokenType());
            cur = cur.getEqExpNode();
        }
        String res = emitRelValue(terms.get(0), ctx);
        for (int i = 0; i < ops.size(); i++) {
            String rhs = emitRelValue(terms.get(i + 1), ctx);
            String lType = typeOf(res);
            String rType = typeOf(rhs);
            String lhsVal = res;
            String rhsVal = rhs;
            if ("i1".equals(lType) && !"i1".equals(rType)) {
                lhsVal = ensureI32(res);
                lType = "i32";
            } else if (!"i1".equals(lType) && "i1".equals(rType)) {
                rhsVal = ensureI32(rhs);
                rType = "i32";
            } else if ("i1".equals(lType) && "i1".equals(rType)) {
                // compare as i1 directly
            } else {
                lhsVal = ensureI32(res);
                rhsVal = ensureI32(rhs);
                lType = "i32";
            }
            String cmp = freshReg();
            String cmpTy = "i32".equals(lType) ? "i32" : "i1";
            if (ops.get(i) == TokenType.EQL) funcsSb.append("  ").append(cmp).append(" = icmp eq ").append(cmpTy).append(" ").append(lhsVal).append(", ").append(rhsVal).append("\n");
            else funcsSb.append("  ").append(cmp).append(" = icmp ne ").append(cmpTy).append(" ").append(lhsVal).append(", ").append(rhsVal).append("\n");
            boolRegs.add(cmp);
            cmpRegs.add(cmp);
            regType.put(cmp, "i1");
            res = cmp;
        }
        return toBool(res);
    }

    private String emitRelValue(RelExpNode node, FuncContext ctx) {
        List<AddExpNode> terms = new ArrayList<>();
        List<TokenType> ops = new ArrayList<>();
        RelExpNode cur = node;
        while (true) {
            terms.add(cur.getAddExpNode());
            if (cur.getOperator() == null) break;
            ops.add(cur.getOperator().getTokenType());
            cur = cur.getRelExpNode();
        }
        String res = emitAddExp(terms.get(0), ctx);
        for (int i = 0; i < ops.size(); i++) {
            String rhs = emitAddExp(terms.get(i + 1), ctx);
            String cmp = freshReg();
            switch (ops.get(i)) {
                case LSS: funcsSb.append("  ").append(cmp).append(" = icmp slt i32 ").append(res).append(", ").append(rhs).append("\n"); break;
                case GRE: funcsSb.append("  ").append(cmp).append(" = icmp sgt i32 ").append(res).append(", ").append(rhs).append("\n"); break;
                case LEQ: funcsSb.append("  ").append(cmp).append(" = icmp sle i32 ").append(res).append(", ").append(rhs).append("\n"); break;
                case GEQ: funcsSb.append("  ").append(cmp).append(" = icmp sge i32 ").append(res).append(", ").append(rhs).append("\n"); break;
                default: break;
            }
            boolRegs.add(cmp);
            cmpRegs.add(cmp);
            regType.put(cmp, "i1");
            res = cmp;
        }
        if (boolRegs.contains(res)) {
            String z = freshReg();
            funcsSb.append("  ").append(z).append(" = zext i1 ").append(res).append(" to i32\n");
            return z;
        }
        return res;
    }

    private String toBool(String v) {
        if (boolRegs.contains(v)) return v;
        String res = freshReg();
        funcsSb.append("  ").append(res).append(" = icmp ne i32 ").append(v).append(", 0\n");
        boolRegs.add(res);
        cmpRegs.add(res);
        regType.put(res, "i1");
        return res;
    }

    // Cast bool i1 to i32 when needed.
    private String ensureI32(String v) {
        if (!boolRegs.contains(v) && !cmpRegs.contains(v) && !"i1".equals(regType.get(v))) return v;
        String z = freshReg();
        funcsSb.append("  ").append(z).append(" = zext i1 ").append(v).append(" to i32\n");
        return z;
    }

    private boolean isBool(String v) {
        return boolRegs.contains(v) || cmpRegs.contains(v) || "i1".equals(regType.get(v));
    }

    private String typeOf(String v) {
        String t = regType.get(v);
        if (t != null) return t;
        return "i32";
    }

    /* ---------- LVal ---------- */
    private void emitStoreLVal(LValNode lValNode, String value, FuncContext ctx) {
        String addr = emitLValAddress(lValNode, ctx);
        funcsSb.append("  store i32 ").append(value).append(", i32* ").append(addr).append(", align 4\n");
    }

    private String emitLValAddress(LValNode lValNode, FuncContext ctx) {
        String name = lValNode.getIdent().getValue();
        VarInfo vi = ctx.lookupVar(name);
        if (vi != null) {
            if (vi.storage == Storage.STATIC) {
                // static locals lowered to globals with label
                if (!vi.isArray || lValNode.getExpNodes().isEmpty()) {
                    String base = freshReg();
                    if (vi.isArray) {
                        funcsSb.append("  ").append(base).append(" = getelementptr inbounds [").append(vi.dims.get(0)).append(" x i32], [")
                                .append(vi.dims.get(0)).append(" x i32]* @").append(vi.label).append(", i32 0, i32 0\n");
                    } else {
                        funcsSb.append("  ").append(base).append(" = getelementptr inbounds i32, i32* @").append(vi.label).append(", i32 0\n");
                    }
                    return base;
                } else {
                    String base = freshReg();
                    funcsSb.append("  ").append(base).append(" = getelementptr inbounds [").append(vi.dims.get(0)).append(" x i32], [")
                            .append(vi.dims.get(0)).append(" x i32]* @").append(vi.label).append(", i32 0, i32 0\n");
                    String offset = computeOffset(lValNode.getExpNodes(), vi.dims, ctx);
                    String addr = freshReg();
                    funcsSb.append("  ").append(addr).append(" = getelementptr inbounds i32, i32* ").append(base).append(", i32 ").append(offset).append("\n");
                    return addr;
                }
            }
            if (!vi.isArray || lValNode.getExpNodes().isEmpty()) {
                if (vi.storage == Storage.PARAM && vi.isArray) {
                    String base = freshReg();
                    funcsSb.append("  ").append(base).append(" = load i32*, i32** ").append(vi.ptr).append(", align 4\n");
                    return base;
                }
                return vi.ptr;
            }
            String base;
            if (vi.storage == Storage.PARAM && vi.isArray) {
                base = freshReg();
                funcsSb.append("  ").append(base).append(" = load i32*, i32** ").append(vi.ptr).append(", align 4\n");
            } else {
                base = vi.ptr;
            }
            String offset = computeOffset(lValNode.getExpNodes(), vi.dims, ctx);
            String addr = freshReg();
            funcsSb.append("  ").append(addr).append(" = getelementptr inbounds i32, i32* ").append(base).append(", i32 ").append(offset).append("\n");
            return addr;
        }
        GlobalVar gv = globals.get(name);
        if (gv != null) {
            if (!gv.isArray || lValNode.getExpNodes().isEmpty()) {
                String base = freshReg();
                if (gv.isArray) {
                    funcsSb.append("  ").append(base).append(" = getelementptr inbounds [").append(gv.len).append(" x i32], [")
                            .append(gv.len).append(" x i32]* @").append(gv.name).append(", i32 0, i32 0\n");
                } else {
                    funcsSb.append("  ").append(base).append(" = getelementptr inbounds i32, i32* @").append(gv.name).append(", i32 0\n");
                }
                return base;
            } else {
                String base = freshReg();
                funcsSb.append("  ").append(base).append(" = getelementptr inbounds [").append(gv.len).append(" x i32], [")
                        .append(gv.len).append(" x i32]* @").append(gv.name).append(", i32 0, i32 0\n");
                String offset = computeOffset(lValNode.getExpNodes(), gv.dims, ctx);
                String addr = freshReg();
                funcsSb.append("  ").append(addr).append(" = getelementptr inbounds i32, i32* ").append(base).append(", i32 ").append(offset).append("\n");
                return addr;
            }
        }
        String dummy = freshReg();
        funcsSb.append("  ").append(dummy).append(" = alloca i32\n");
        return dummy;
    }

    private String computeOffset(List<ExpNode> idxNodes, List<Integer> dims, FuncContext ctx) {
        String offset = null;
        for (int i = 0; i < idxNodes.size(); i++) {
            String idx = emitExp(idxNodes.get(i), ctx);
            int stride = 1;
            for (int j = i + 1; j < dims.size(); j++) {
                Integer d = dims.get(j);
                stride *= (d == null || d == 0) ? 1 : d;
            }
            String term;
            if (stride != 1) {
                term = freshReg();
                funcsSb.append("  ").append(term).append(" = mul i32 ").append(idx).append(", ").append(stride).append("\n");
            } else term = idx;
            if (offset == null) offset = term;
            else {
                String sum = freshReg();
                funcsSb.append("  ").append(sum).append(" = add i32 ").append(offset).append(", ").append(term).append("\n");
                offset = sum;
            }
        }
        if (offset == null) offset = "0";
        return offset;
    }

    /* ---------- printf ---------- */
    private void handlePrintf(StmtNode stmt, FuncContext ctx) {
        String raw = stmt.getFormatString().getValue();
        String fmt = raw.substring(1, raw.length() - 1);
        List<String> segments = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < fmt.length(); i++) {
            char ch = fmt.charAt(i);
            if (ch == '\\' && i + 1 < fmt.length() && fmt.charAt(i + 1) == 'n') {
                cur.append('\n'); i++;
            } else if (ch == '%' && i + 1 < fmt.length() && fmt.charAt(i + 1) == 'd') {
                segments.add(cur.toString());
                segments.add("%d");
                cur.setLength(0);
                i++;
            } else {
                cur.append(ch);
            }
        }
        if (cur.length() > 0) segments.add(cur.toString());

        // 先求值所有参数，保证副作用在任何输出前发生
        List<String> argVals = new ArrayList<>();
        for (ExpNode exp : stmt.getExpNodes()) {
            argVals.add(emitExp(exp, ctx));
        }

        int argIdx = 0;
        for (String seg : segments) {
            if ("%d".equals(seg)) {
                if (argIdx < argVals.size()) {
                    String v = argVals.get(argIdx++);
                    funcsSb.append("  call void @putint(i32 ").append(v).append(")\n");
                }
            } else if (!seg.isEmpty()) {
                String label = newStringConst(seg);
                int len = getStringLen(seg);
                String ptr = freshReg();
                funcsSb.append("  ").append(ptr).append(" = getelementptr inbounds [").append(len).append(" x i8], [")
                        .append(len).append(" x i8]* ").append(label).append(", i32 0, i32 0\n");
                funcsSb.append("  call void @putstr(i8* ").append(ptr).append(")\n");
            }
        }
    }

    private String newStringConst(String content) {
        if (strContentMap.containsKey(content)) {
            return strContentMap.get(content);
        }
        String label = ".str_" + (strId++);
        int len = getStringLen(content);
        String def = "@" + label + " = private unnamed_addr constant [" + len + " x i8] c\"" + escapeString(content) + "\", align 1\n";
        stringLiterals.put(label, def);
        strContentMap.put(content, "@" + label);
        return "@" + label;
    }

    private int getStringLen(String s) { return s.getBytes(StandardCharsets.UTF_8).length + 1; }

    private String escapeString(String s) {
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '\\': sb.append("\\5C"); break;
                case '\"': sb.append("\\22"); break;
                case '\n': sb.append("\\0A"); break;
                case '\t': sb.append("\\09"); break;
                default:
                    if (ch >= 32 && ch < 127) sb.append(ch);
                    else sb.append(String.format("\\%02X", (int) ch));
            }
        }
        sb.append("\\00");
        return sb.toString();
    }

    /* ---------- utils ---------- */
    private Map<String, ConstInfo> mergedConstEnv() {
        Map<String, ConstInfo> res = new HashMap<>(globalConstEnv);
        for (Map<String, ConstInfo> m : constEnvStack) res.putAll(m);
        return res;
    }

    private String tryGetArrayPointer(ExpNode expNode, FuncContext ctx) {
        AddExpNode add = expNode.getAddExpNode();
        if (add.getOperator() != null) return null;
        MulExpNode mul = add.getMulExpNode();
        if (mul.getOperator() != null) return null;
        UnaryExpNode unary = mul.getUnaryExpNode();
        if (unary.getPrimaryExpNode() == null || unary.getPrimaryExpNode().getLValNode() == null) return null;
        LValNode lVal = unary.getPrimaryExpNode().getLValNode();
        if (!lVal.getExpNodes().isEmpty()) return null;
        VarInfo vi = ctx.lookupVar(lVal.getIdent().getValue());
        if (vi == null) {
            GlobalVar gv = globals.get(lVal.getIdent().getValue());
            if (gv != null && gv.isArray) {
                String base = freshReg();
                funcsSb.append("  ").append(base).append(" = getelementptr inbounds [").append(gv.len).append(" x i32], [")
                        .append(gv.len).append(" x i32]* @").append(gv.name).append(", i32 0, i32 0\n");
                return base;
            }
            return null;
        }
        if (!vi.isArray) return null;
        if (vi.storage == Storage.PARAM && vi.isArray) {
            String base = freshReg();
            funcsSb.append("  ").append(base).append(" = load i32*, i32** ").append(vi.ptr).append(", align 4\n");
            return base;
        }
        return vi.ptr;
    }
}
