package backend;

import ASTNode.*;
import Token.TokenType;
import semantic.SymbolType;

import java.util.*;

/**
 * 简单 AST -> MIPS 生成器（覆盖实验常用特性，未做优化）。
 */
public class CodeGenerator {
    private final StringBuilder data = new StringBuilder();
    private final StringBuilder text = new StringBuilder();

    private final Map<String, GlobalVar> globals = new LinkedHashMap<>();
    private final Map<String, String> stringLiterals = new LinkedHashMap<>();
    private int labelId = 0;
    private int staticId = 0;
    private static final int SAVE_S_SIZE = 32; // save $s0-$s7 in prologue

    private RegAllocator regAllocator = new RegAllocator();

    private static class GlobalVar {
        String name;
        boolean isArray;
        List<Integer> initValues;

        GlobalVar(String name, boolean isArray, List<Integer> initValues) {
            this.name = name;
            this.isArray = isArray;
            this.initValues = initValues;
        }
    }

    public String generate(CompUnitNode compUnitNode) {
        collectGlobals(compUnitNode);
        emitTextSection(compUnitNode); // 文本段可能收集字符串常量
        emitDataSection(); // 最后生成数据段，包含全局变量和字符串
        return data.toString() + text.toString();
    }

    /* ---------- 收集与常量求值 ---------- */
    private void collectGlobals(CompUnitNode compUnitNode) {
        for (DeclNode declNode : compUnitNode.getDeclNodes()) {
            if (declNode.getConstDecl() != null) {
                for (ConstDefNode def : declNode.getConstDecl().getConstDefNodes()) {
                    String name = def.getIdent().getValue();
                    int dims = def.getConstExpNodes().size();
                    if (dims == 0) {
                        int v = evalConstExp(def.getConstInitValNode().getConstExpNodes().get(0));
                        globals.put(name, new GlobalVar(name, false, Collections.singletonList(v)));
                    } else {
                        int len = evalConstExp(def.getConstExpNodes().get(0));
                        List<Integer> init = flattenConstInit(def.getConstInitValNode(), len);
                        globals.put(name, new GlobalVar(name, true, init));
                    }
                }
            } else if (declNode.getVarDecl() != null) {
                for (VarDefNode def : declNode.getVarDecl().getVarDefNodes()) {
                    String name = def.getIdent().getValue();
                    int dims = def.getConstExpNodes().size();
                    if (dims == 0) {
                        int v = def.getInitValNode() == null ? 0 : evalInitFirst(def.getInitValNode());
                        globals.put(name, new GlobalVar(name, false, Collections.singletonList(v)));
                    } else {
                        int len = evalConstExp(def.getConstExpNodes().get(0));
                        List<Integer> init = def.getInitValNode() == null
                                ? Collections.nCopies(len, 0)
                                : flattenInit(def.getInitValNode(), len);
                        globals.put(name, new GlobalVar(name, true, init));
                    }
                }
            }
        }
    }

    private List<Integer> flattenConstInit(ConstInitValNode node, int len) {
        List<Integer> res = new ArrayList<>(Collections.nCopies(len, 0));
        int idx = 0;
        for (ConstExpNode expNode : node.getConstExpNodes()) {
            if (idx >= len) break;
            res.set(idx++, evalConstExp(expNode));
        }
        return res;
    }

    private List<Integer> flattenInit(InitValNode node, int len) {
        List<Integer> res = new ArrayList<>(Collections.nCopies(len, 0));
        int idx = 0;
        for (ExpNode expNode : node.getExpNodes()) {
            if (idx >= len) break;
            res.set(idx++, evalConstExpFromExp(expNode));
        }
        return res;
    }

    private List<Integer> collectStaticInit(VarDefNode def, int len) {
        // 尝试按常量方式求值，否则默认 0
        if (def.getInitValNode() == null) {
            return Collections.nCopies(len, 0);
        }
        List<Integer> res = new ArrayList<>(Collections.nCopies(len, 0));
        int idx = 0;
        for (ExpNode expNode : def.getInitValNode().getExpNodes()) {
            if (idx >= len) break;
            res.set(idx++, evalConstExpFromExp(expNode));
        }
        return res;
    }

    private int evalConstExp(ConstExpNode node) {
        return evalAdd(node.getAddExpNode());
    }

    private int evalConstExpFromExp(ExpNode node) {
        return evalAdd(node.getAddExpNode());
    }

    private int evalAdd(AddExpNode node) {
        // flatten right-recursive AddExp to left-associative evaluation
        List<Integer> values = new ArrayList<>();
        List<TokenType> ops = new ArrayList<>();
        AddExpNode cur = node;
        while (true) {
            values.add(evalMul(cur.getMulExpNode()));
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

    private int evalMul(MulExpNode node) {
        List<Integer> values = new ArrayList<>();
        List<TokenType> ops = new ArrayList<>();
        MulExpNode cur = node;
        while (true) {
            values.add(evalUnary(cur.getUnaryExpNode()));
            if (cur.getOperator() == null) break;
            ops.add(cur.getOperator().getTokenType());
            cur = cur.getMulExpNode();
        }
        int res = values.get(0);
        for (int i = 0; i < ops.size(); i++) {
            int rhs = values.get(i + 1);
            switch (ops.get(i)) {
                case MULT:
                    res = res * rhs;
                    break;
                case DIV:
                    res = rhs == 0 ? 0 : res / rhs;
                    break;
                case MOD:
                    res = rhs == 0 ? 0 : res % rhs;
                    break;
                default:
                    break;
            }
        }
        return res;
    }

    private int evalUnary(UnaryExpNode node) {
        if (node.getPrimaryExpNode() != null) {
            return evalPrimary(node.getPrimaryExpNode());
        } else if (node.getIdent() != null) {
            // const 中不会出现函数调用，这里简化
            return 0;
        } else {
            int v = evalUnary(node.getUnaryExpNode());
            TokenType tt = node.getUnaryOpNode().getToken().getTokenType();
            if (tt == TokenType.MINU) return -v;
            if (tt == TokenType.NOT) return v == 0 ? 1 : 0;
            return v;
        }
    }

    private int evalPrimary(PrimaryExpNode node) {
        if (node.getExpNode() != null) {
            return evalConstExpFromExp(node.getExpNode());
        } else if (node.getNumberNode() != null) {
            return Integer.parseInt(node.getNumberNode().getStr());
        } else if (node.getLValNode() != null) {
            return evalConstLVal(node.getLValNode());
        } else {
            // 未解析 const LVal，简化为 0
            return 0;
        }
    }

    private int evalConstLVal(LValNode lValNode) {
        String name = lValNode.getIdent().getValue();
        GlobalVar gv = globals.get(name);
        if (gv != null) {
            int idx = 0;
            if (!lValNode.getExpNodes().isEmpty()) {
                idx = evalConstExpFromExp(lValNode.getExpNodes().get(0));
            }
            if (idx >= 0 && idx < gv.initValues.size()) {
                return gv.initValues.get(idx);
            }
        }
        return 0;
    }

    private int evalInitFirst(InitValNode initValNode) {
        return evalConstExpFromExp(initValNode.getExpNodes().get(0));
    }

    /* ---------- 片段输出 ---------- */
    private void emitDataSection() {
        StringBuilder sb = new StringBuilder();
        sb.append(".data\n");
        for (GlobalVar gv : globals.values()) {
            sb.append(gv.name).append(":\n");
            sb.append("  .word ");
            for (int i = 0; i < gv.initValues.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(gv.initValues.get(i));
            }
            sb.append("\n");
        }
        for (Map.Entry<String, String> e : stringLiterals.entrySet()) {
            sb.append(e.getKey()).append(": .asciiz ").append(e.getValue()).append("\n");
        }
        data.insert(0, sb.toString());
    }

    private void emitTextSection(CompUnitNode compUnitNode) {
        text.append(".text\n");
        // 让 main 作为文本段的第一个标签，装载后直接从 main 开始执行
        emitMain(compUnitNode.getMainFuncDefNode());
        for (FuncDefNode func : compUnitNode.getFuncDefNodes()) {
            emitFunction(func);
        }
    }

    /* ---------- 函数与块 ---------- */
    private void preScanBlock(BlockNode blockNode, FunctionContext ctx, String funcName) {
        ctx.enterScope();
        for (BlockItemNode item : blockNode.getBlockItemNodes()) {
            if (item.getDeclNode() != null) {
                DeclNode decl = item.getDeclNode();
                if (decl.getConstDecl() != null) {
                    for (ConstDefNode def : decl.getConstDecl().getConstDefNodes()) {
                        int dims = def.getConstExpNodes().size();
                        int size = dims == 0 ? 4 : evalConstExp(def.getConstExpNodes().get(0)) * 4;
                        int offset = ctx.allocBytes(size);
                        ctx.addLocal(def.getIdent().getValue(), new LocalSymbol(offset, new SymbolType(SymbolType.BaseType.INT, dims), true, false, null, false));
                    }
                } else if (decl.getVarDecl() != null) {
                    for (VarDefNode def : decl.getVarDecl().getVarDefNodes()) {
                        int dims = def.getConstExpNodes().size();
                        if (decl.getVarDecl().getStaticToken() != null) {
                            String label = newStaticLabel(funcName, def.getIdent().getValue());
                            int len = dims == 0 ? 1 : evalConstExp(def.getConstExpNodes().get(0));
                            List<Integer> init = collectStaticInit(def, len);
                            globals.put(label, new GlobalVar(label, dims > 0, init));
                            ctx.addLocal(def.getIdent().getValue(), new LocalSymbol(0, new SymbolType(SymbolType.BaseType.INT, dims), false, true, label, false));
                        } else {
                            int size = dims == 0 ? 4 : evalConstExp(def.getConstExpNodes().get(0)) * 4;
                            int offset = ctx.allocBytes(size);
                            ctx.addLocal(def.getIdent().getValue(), new LocalSymbol(offset, new SymbolType(SymbolType.BaseType.INT, dims), false, false, null, false));
                        }
                    }
                }
            } else if (item.getStmtNode() != null && item.getStmtNode().getType() == StmtNode.StmtType.Block) {
                preScanBlock(item.getStmtNode().getBlockNode(), ctx, funcName);
            } else if (item.getStmtNode() != null) {
                preScanStmt(item.getStmtNode(), ctx, funcName);
            }
        }
        ctx.exitScope();
    }

    private void preScanStmt(StmtNode stmtNode, FunctionContext ctx, String funcName) {
        switch (stmtNode.getType()) {
            case Block:
                preScanBlock(stmtNode.getBlockNode(), ctx, funcName);
                break;
            case If:
                preScanStmt(stmtNode.getStmtNodes().get(0), ctx, funcName);
                if (stmtNode.getStmtNodes().size() > 1) {
                    preScanStmt(stmtNode.getStmtNodes().get(1), ctx, funcName);
                }
                break;
            case For:
                preScanStmt(stmtNode.getStmtNodes().get(0), ctx, funcName);
                break;
            default:
                break;
        }
    }

    private void emitFunction(FuncDefNode func) {
        text.append(func.getIdent().getValue()).append(":\n");
        FunctionContext ctx = new FunctionContext();
        ctx.enterScope();
        // params
        int paramIdx = 0;
        if (func.getFuncFParamsNode() != null) {
            for (FuncFParamNode p : func.getFuncFParamsNode().getFuncFParamNodes()) {
                int dims = p.getLeftBrackets().size();
                int offset = ctx.allocBytes(4); // store pointer or value
                ctx.addLocal(p.getIdent().getValue(), new LocalSymbol(offset, new SymbolType(SymbolType.BaseType.INT, dims), false, false, null, true));
                paramIdx++;
            }
        }
        ctx.setSuppressPop(true);
        preScanBlock(func.getBlockNode(), ctx, func.getIdent().getValue());
        ctx.setSuppressPop(false);
        int frameSize = align(ctx.getFrameSize() + SAVE_S_SIZE);
        String exitLabel = newLabel("exit_" + func.getIdent().getValue());
        // prologue
        text.append("  addiu $sp, $sp, -").append(frameSize).append("\n");
        text.append("  sw $ra, ").append(frameSize - 4).append("($sp)\n");
        text.append("  sw $fp, ").append(frameSize - 8).append("($sp)\n");
        text.append("  addiu $fp, $sp, ").append(frameSize).append("\n");
        saveSRegisters(frameSize);
        // store params
        paramIdx = 0;
        if (func.getFuncFParamsNode() != null) {
            for (FuncFParamNode p : func.getFuncFParamsNode().getFuncFParamNodes()) {
                LocalSymbol ls = ctx.getLocal(p.getIdent().getValue());
                if (paramIdx < 4) {
                    text.append("  sw $a").append(paramIdx).append(", ").append(ls.getOffset()).append("($fp)\n");
                } else {
                    int off = (paramIdx - 4) * 4;
                    text.append("  lw $t0, ").append(off).append("($fp)\n");
                    text.append("  sw $t0, ").append(ls.getOffset()).append("($fp)\n");
                }
                paramIdx++;
            }
        }
        emitBlock(func.getBlockNode(), ctx, new ArrayDeque<>(), new ArrayDeque<>(), exitLabel, func.getIdent().getValue());
        ctx.exitScope();
        text.append(exitLabel).append(":\n");
        emitEpilogue(frameSize);
    }

    private void emitMain(MainFuncDefNode mainFunc) {
        text.append("main:\n");
        FunctionContext ctx = new FunctionContext();
        ctx.enterScope();
        ctx.setSuppressPop(true);
        preScanBlock(mainFunc.getBlockNode(), ctx, "main");
        ctx.setSuppressPop(false);
        int frameSize = align(ctx.getFrameSize() + SAVE_S_SIZE);
        String exitLabel = newLabel("exit_main");
        text.append("  addiu $sp, $sp, -").append(frameSize).append("\n");
        text.append("  sw $ra, ").append(frameSize - 4).append("($sp)\n");
        text.append("  sw $fp, ").append(frameSize - 8).append("($sp)\n");
        text.append("  addiu $fp, $sp, ").append(frameSize).append("\n");
        saveSRegisters(frameSize);
        emitBlock(mainFunc.getBlockNode(), ctx, new ArrayDeque<>(), new ArrayDeque<>(), exitLabel, "main");
        ctx.exitScope();
        text.append(exitLabel).append(":\n");
        text.append("  lw $ra, ").append(frameSize - 4).append("($sp)\n");
        text.append("  lw $fp, ").append(frameSize - 8).append("($sp)\n");
        text.append("  addiu $sp, $sp, ").append(frameSize).append("\n");
        text.append("  li $v0, 10\n");
        text.append("  syscall\n");
    }

    private void emitEpilogue(int frameSize) {
        restoreSRegisters(frameSize);
        text.append("  lw $ra, ").append(frameSize - 4).append("($sp)\n");
        text.append("  lw $fp, ").append(frameSize - 8).append("($sp)\n");
        text.append("  addiu $sp, $sp, ").append(frameSize).append("\n");
        text.append("  jr $ra\n");
    }

    private void emitBlock(BlockNode blockNode, FunctionContext ctx, Deque<String> breakLabels, Deque<String> continueLabels, String exitLabel, String funcName) {
        ctx.enterScope();
        for (BlockItemNode item : blockNode.getBlockItemNodes()) {
            if (item.getDeclNode() != null) {
                emitDecl(item.getDeclNode(), ctx, funcName);
            } else if (item.getStmtNode() != null) {
                emitStmt(item.getStmtNode(), ctx, breakLabels, continueLabels, exitLabel, funcName);
            }
        }
        ctx.exitScope();
    }

    private void emitDecl(DeclNode declNode, FunctionContext ctx, String funcName) {
        if (declNode.getConstDecl() != null) {
            for (ConstDefNode def : declNode.getConstDecl().getConstDefNodes()) {
                LocalSymbol ls = ctx.getLocal(def.getIdent().getValue());
                if (ls == null) {
                    ls = ctx.getAnyLocal(def.getIdent().getValue());
                }
                if (ls == null) {
                    // 未预扫描到的声明，跳过以避免空指针
                    continue;
                }
                if (ls != null && ls.isStatic()) {
                    // 静态 const 已在数据段初始化，无需再次赋值
                    continue;
                }
                if (ls.getType().getDimensions() == 0) {
                    int val = evalConstExp(def.getConstInitValNode().getConstExpNodes().get(0));
                    text.append("  li $t0, ").append(val).append("\n");
                    text.append("  sw $t0, ").append(ls.getOffset()).append("($fp)\n");
                } else {
                    int len = evalConstExp(def.getConstExpNodes().get(0));
                    List<Integer> init = flattenConstInit(def.getConstInitValNode(), len);
                    for (int i = 0; i < init.size(); i++) {
                        text.append("  li $t0, ").append(init.get(i)).append("\n");
                        text.append("  sw $t0, ").append(ls.getOffset() + i * 4).append("($fp)\n");
                    }
                }
            }
        } else if (declNode.getVarDecl() != null) {
            for (VarDefNode def : declNode.getVarDecl().getVarDefNodes()) {
                LocalSymbol ls = ctx.getLocal(def.getIdent().getValue());
                if (ls == null) {
                    ls = ctx.getAnyLocal(def.getIdent().getValue());
                }
                if (ls == null) {
                    continue;
                }
                if (ls.isStatic()) {
                    // static locals are placed in .data; no per-call initialization needed
                    continue;
                }
                if (ls.getType().getDimensions() == 0) {
                    if (def.getInitValNode() != null) {
                        String r = emitExp(def.getInitValNode().getExpNodes().get(0), ctx);
                        text.append("  sw ").append(r).append(", ").append(ls.getOffset()).append("($fp)\n");
                        regAllocator.release(r);
                    }
                } else {
                    if (def.getInitValNode() != null) {
                        List<ExpNode> inits = def.getInitValNode().getExpNodes();
                        for (int i = 0; i < inits.size(); i++) {
                            String r = emitExp(inits.get(i), ctx);
                            text.append("  sw ").append(r).append(", ").append(ls.getOffset() + i * 4).append("($fp)\n");
                            regAllocator.release(r);
                        }
                    }
                }
            }
        }
    }

    /* ---------- 语句 ---------- */
    private void emitStmt(StmtNode stmtNode, FunctionContext ctx, Deque<String> breakLabels, Deque<String> continueLabels, String exitLabel, String funcName) {
        switch (stmtNode.getType()) {
            case LValAssignExp: {
                String val = emitExp(stmtNode.getExpNode(), ctx);
                emitStoreLVal(stmtNode.getLValNode(), val, ctx);
                regAllocator.release(val);
                break;
            }
            case LValAssignGetint: {
                String addr = emitLValAddress(stmtNode.getLValNode(), ctx);
                text.append("  li $v0, 5\n");
                text.append("  syscall\n");
                text.append("  sw $v0, 0(").append(addr).append(")\n");
                regAllocator.release(addr);
                break;
            }
            case Exp:
                if (stmtNode.getExpNode() != null) {
                    String r = emitExp(stmtNode.getExpNode(), ctx);
                    regAllocator.release(r);
                }
                break;
            case Block:
                emitBlock(stmtNode.getBlockNode(), ctx, breakLabels, continueLabels, exitLabel, funcName);
                break;
            case If: {
                String elseLabel = newLabel("else");
                String endLabel = newLabel("endif");
                emitCondBranch(stmtNode.getCondNode(), ctx, null, elseLabel);
                emitStmt(stmtNode.getStmtNodes().get(0), ctx, breakLabels, continueLabels, exitLabel, funcName);
                text.append("  j ").append(endLabel).append("\n");
                text.append(elseLabel).append(":\n");
                if (stmtNode.getStmtNodes().size() > 1) {
                    emitStmt(stmtNode.getStmtNodes().get(1), ctx, breakLabels, continueLabels, exitLabel, funcName);
                }
                text.append(endLabel).append(":\n");
                break;
            }
            case For: {
                String loopLabel = newLabel("for");
                String endLabel = newLabel("endfor");
                String contLabel = newLabel("cont");
                if (stmtNode.getForStmtNode1() != null) {
                    emitForAssign(stmtNode.getForStmtNode1(), ctx);
                }
                text.append(loopLabel).append(":\n");
                if (stmtNode.getCondNode() != null) {
                    emitCondBranch(stmtNode.getCondNode(), ctx, null, endLabel);
                }
                breakLabels.push(endLabel);
                continueLabels.push(contLabel);
                emitStmt(stmtNode.getStmtNodes().get(0), ctx, breakLabels, continueLabels, exitLabel, funcName);
                text.append(contLabel).append(":\n");
                if (stmtNode.getForStmtNode2() != null) {
                    emitForAssign(stmtNode.getForStmtNode2(), ctx);
                }
                text.append("  j ").append(loopLabel).append("\n");
                text.append(endLabel).append(":\n");
                breakLabels.pop();
                continueLabels.pop();
                break;
            }
            case Break:
                text.append("  j ").append(breakLabels.peek()).append("\n");
                break;
            case Continue:
                text.append("  j ").append(continueLabels.peek()).append("\n");
                break;
            case Return:
                if (stmtNode.getExpNode() != null) {
                    String r = emitExp(stmtNode.getExpNode(), ctx);
                    text.append("  move $v0, ").append(r).append("\n");
                    regAllocator.release(r);
                }
                if (exitLabel != null) {
                    text.append("  j ").append(exitLabel).append("\n");
                } else {
                    text.append("  jr $ra\n");
                }
                break;
            case Printf:
                handlePrintf(stmtNode, ctx);
                break;
            default:
                break;
        }
    }

    private void emitForAssign(ForStmtNode forStmtNode, FunctionContext ctx) {
        for (int i = 0; i < forStmtNode.getLValNodes().size(); i++) {
            String r = emitExp(forStmtNode.getExpNodes().get(i), ctx);
            emitStoreLVal(forStmtNode.getLValNodes().get(i), r, ctx);
            regAllocator.release(r);
        }
    }

    /* ---------- 条件 ---------- */
    private void emitCondBranch(CondNode condNode, FunctionContext ctx, String trueLabel, String falseLabel) {
        String r = emitCondValue(condNode, ctx);
        branchOnBool(r, trueLabel, falseLabel);
        regAllocator.release(r);
    }

    private String emitCondValue(CondNode condNode, FunctionContext ctx) {
        return emitLOrValue(condNode.getLOrExpNode(), ctx);
    }

    private String emitLOrValue(LOrExpNode node, FunctionContext ctx) {
        if (node.getOrToken() == null) {
            return emitLAndValue(node.getLAndExpNode(), ctx);
        }
        String res = regAllocator.alloc();
        String end = newLabel("lor_end");
        String left = emitLAndValue(node.getLAndExpNode(), ctx);
        text.append("  move ").append(res).append(", ").append(left).append("\n");
        text.append("  bne ").append(res).append(", $zero, ").append(end).append("\n");
        regAllocator.release(left);
        String right = emitLOrValue(node.getLOrExpNode(), ctx);
        text.append("  move ").append(res).append(", ").append(right).append("\n");
        regAllocator.release(right);
        text.append(end).append(":\n");
        return res;
    }

    private String emitLAndValue(LAndExpNode node, FunctionContext ctx) {
        if (node.getAndToken() == null) {
            return emitEqValue(node.getEqExpNode(), ctx);
        }
        String res = regAllocator.alloc();
        String end = newLabel("land_end");
        String left = emitEqValue(node.getEqExpNode(), ctx);
        text.append("  move ").append(res).append(", ").append(left).append("\n");
        text.append("  beq ").append(res).append(", $zero, ").append(end).append("\n");
        regAllocator.release(left);
        String right = emitLAndValue(node.getLAndExpNode(), ctx);
        text.append("  move ").append(res).append(", ").append(right).append("\n");
        regAllocator.release(right);
        text.append(end).append(":\n");
        return res;
    }

    private String emitEqValue(EqExpNode node, FunctionContext ctx) {
        // Flatten to left-to-right evaluation to avoid right-recursion ordering issues
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
            if (ops.get(i) == TokenType.EQL) {
                text.append("  xor ").append(res).append(", ").append(res).append(", ").append(rhs).append("\n");
                text.append("  sltu ").append(res).append(", $zero, ").append(res).append("\n");
                text.append("  xori ").append(res).append(", ").append(res).append(", 1\n");
            } else { // NEQ
                text.append("  xor ").append(res).append(", ").append(res).append(", ").append(rhs).append("\n");
                text.append("  sltu ").append(res).append(", $zero, ").append(res).append("\n");
            }
            regAllocator.release(rhs);
        }
        return res;
    }

    private String emitRelValue(RelExpNode node, FunctionContext ctx) {
        // Flatten to left-to-right evaluation
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
            switch (ops.get(i)) {
                case LSS:
                    text.append("  slt ").append(res).append(", ").append(res).append(", ").append(rhs).append("\n");
                    break;
                case GRE:
                    text.append("  slt ").append(res).append(", ").append(rhs).append(", ").append(res).append("\n");
                    break;
                case LEQ: {
                    // res = (res < rhs) || (res == rhs)
                    String lt = regAllocator.alloc();
                    text.append("  slt ").append(lt).append(", ").append(res).append(", ").append(rhs).append("\n");
                    String eq = regAllocator.alloc();
                    text.append("  xor ").append(eq).append(", ").append(res).append(", ").append(rhs).append("\n");
                    text.append("  sltu ").append(eq).append(", $zero, ").append(eq).append("\n");
                    text.append("  xori ").append(eq).append(", ").append(eq).append(", 1\n");
                    text.append("  or ").append(res).append(", ").append(lt).append(", ").append(eq).append("\n");
                    regAllocator.release(lt);
                    regAllocator.release(eq);
                    break;
                }
                case GEQ: {
                    // res = (res > rhs) || (res == rhs)
                    String gt = regAllocator.alloc();
                    text.append("  slt ").append(gt).append(", ").append(rhs).append(", ").append(res).append("\n");
                    String eq = regAllocator.alloc();
                    text.append("  xor ").append(eq).append(", ").append(res).append(", ").append(rhs).append("\n");
                    text.append("  sltu ").append(eq).append(", $zero, ").append(eq).append("\n");
                    text.append("  xori ").append(eq).append(", ").append(eq).append(", 1\n");
                    text.append("  or ").append(res).append(", ").append(gt).append(", ").append(eq).append("\n");
                    regAllocator.release(gt);
                    regAllocator.release(eq);
                    break;
                }
                default:
                    break;
            }
            regAllocator.release(rhs);
        }
        return res;
    }

    private void branchOnBool(String reg, String trueLabel, String falseLabel) {
        if (trueLabel != null) {
            text.append("  bne ").append(reg).append(", $zero, ").append(trueLabel).append("\n");
        }
        if (falseLabel != null) {
            text.append("  beq ").append(reg).append(", $zero, ").append(falseLabel).append("\n");
        }
    }

    /* ---------- 表达式 ---------- */
    private String emitExp(ExpNode expNode, FunctionContext ctx) {
        return emitAddExp(expNode.getAddExpNode(), ctx);
    }

    private String emitAddExp(AddExpNode node, FunctionContext ctx) {
        // flatten right-recursive AddExp to left-associative codegen
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
            if (ops.get(i) == TokenType.PLUS) {
                text.append("  addu ").append(res).append(", ").append(res).append(", ").append(rhs).append("\n");
            } else {
                text.append("  subu ").append(res).append(", ").append(res).append(", ").append(rhs).append("\n");
            }
            regAllocator.release(rhs);
        }
        return res;
    }

    private String emitMulExp(MulExpNode node, FunctionContext ctx) {
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
            switch (ops.get(i)) {
                case MULT:
                    text.append("  mul ").append(res).append(", ").append(res).append(", ").append(rhs).append("\n");
                    break;
                case DIV:
                    text.append("  div ").append(res).append(", ").append(rhs).append("\n");
                    text.append("  mflo ").append(res).append("\n");
                    break;
                case MOD:
                    text.append("  div ").append(res).append(", ").append(rhs).append("\n");
                    text.append("  mfhi ").append(res).append("\n");
                    break;
                default:
                    break;
            }
            regAllocator.release(rhs);
        }
        return res;
    }

    private String emitUnaryExp(UnaryExpNode node, FunctionContext ctx) {
        if (node.getPrimaryExpNode() != null) {
            return emitPrimaryExp(node.getPrimaryExpNode(), ctx);
        } else if (node.getIdent() != null) {
            List<ExpNode> args = node.getFuncRParamsNode() == null ? Collections.emptyList() : node.getFuncRParamsNode().getExpNodes();
            String funcName = node.getIdent().getValue();
            // builtin getint -> syscall 5
            if ("getint".equals(funcName)) {
                text.append("  li $v0, 5\n");
                text.append("  syscall\n");
                String res = regAllocator.alloc();
                text.append("  move ").append(res).append(", $v0\n");
                return res;
            }
            // Evaluate all arguments first to avoid clobbering earlier args by nested calls.
            List<String> argRegs = new ArrayList<>();
            for (ExpNode arg : args) {
                String r = emitArgValue(arg, ctx);
                argRegs.add(r);
            }
            // push extra args (>=4) on caller stack, right-to-left so arg4 is at fp in callee
            int extra = Math.max(0, argRegs.size() - 4);
            for (int i = argRegs.size() - 1; i >= 4; i--) {
                text.append("  addiu $sp, $sp, -4\n");
                text.append("  sw ").append(argRegs.get(i)).append(", 0($sp)\n");
            }
            for (int i = 0; i < argRegs.size() && i < 4; i++) {
                text.append("  move $a").append(i).append(", ").append(argRegs.get(i)).append("\n");
            }
            for (String r : argRegs) {
                regAllocator.release(r);
            }
            text.append("  jal ").append(funcName).append("\n");
            if (extra > 0) {
                text.append("  addiu $sp, $sp, ").append(extra * 4).append("\n");
            }
            String res = regAllocator.alloc();
            text.append("  move ").append(res).append(", $v0\n");
            return res;
        } else {
            String inner = emitUnaryExp(node.getUnaryExpNode(), ctx);
            TokenType tt = node.getUnaryOpNode().getToken().getTokenType();
            if (tt == TokenType.MINU) {
                text.append("  subu ").append(inner).append(", $zero, ").append(inner).append("\n");
            } else if (tt == TokenType.NOT) {
                text.append("  sltu ").append(inner).append(", $zero, ").append(inner).append("\n");
                text.append("  xori ").append(inner).append(", ").append(inner).append(", 1\n");
            }
            return inner;
        }
    }

    private String emitPrimaryExp(PrimaryExpNode node, FunctionContext ctx) {
        if (node.getExpNode() != null) {
            return emitExp(node.getExpNode(), ctx);
        } else if (node.getLValNode() != null) {
            String addr = emitLValAddress(node.getLValNode(), ctx);
            String res = regAllocator.alloc();
            text.append("  lw ").append(res).append(", 0(").append(addr).append(")\n");
            regAllocator.release(addr);
            return res;
        } else {
            int val = Integer.parseInt(node.getNumberNode().getStr());
            String r = regAllocator.alloc();
            text.append("  li ").append(r).append(", ").append(val).append("\n");
            return r;
        }
    }

    /**
     * 实参求值：标量传值，数组传地址
     */
    private String emitArgValue(ExpNode expNode, FunctionContext ctx) {
        if (expNode.getAddExpNode().getMulExpNode().getUnaryExpNode().getPrimaryExpNode() != null) {
            PrimaryExpNode p = expNode.getAddExpNode().getMulExpNode().getUnaryExpNode().getPrimaryExpNode();
            if (p.getLValNode() != null && p.getLValNode().getExpNodes().isEmpty()) {
                // 检查符号维度，数组传地址，标量传值
                String name = p.getLValNode().getIdent().getValue();
                LocalSymbol local = ctx.getLocal(name);
                if (local != null && local.getType().getDimensions() > 0) {
                    return emitLValAddress(p.getLValNode(), ctx);
                }
                GlobalVar gv = globals.get(name);
                if (gv != null && gv.isArray) {
                    return emitLValAddress(p.getLValNode(), ctx);
                }
            }
        }
        return emitExp(expNode, ctx);
    }

    /* ---------- LVal ---------- */
    private void emitStoreLVal(LValNode lValNode, String valueReg, FunctionContext ctx) {
        String addr = emitLValAddress(lValNode, ctx);
        text.append("  sw ").append(valueReg).append(", 0(").append(addr).append(")\n");
        regAllocator.release(addr);
    }

    private String emitLValAddress(LValNode lValNode, FunctionContext ctx) {
        String name = lValNode.getIdent().getValue();
        LocalSymbol local = ctx.getLocal(name);
        if (local != null) {
            String base = regAllocator.alloc();
            if (local.isStatic()) {
                text.append("  la ").append(base).append(", ").append(local.getLabel()).append("\n");
            } else {
                text.append("  addiu ").append(base).append(", $fp, ").append(local.getOffset()).append("\n");
            }
            if (lValNode.getExpNodes().isEmpty()) {
                return base;
            } else {
                if (local.getType().getDimensions() > 0) {
                    // array param stored as pointer on stack: load base first
                    String tmp = regAllocator.alloc();
                    if (local.isStatic()) {
                        text.append("  move ").append(tmp).append(", ").append(base).append("\n");
                    } else if (local.isParam()) {
                        text.append("  lw ").append(tmp).append(", 0(").append(base).append(")\n");
                    } else {
                        text.append("  move ").append(tmp).append(", ").append(base).append("\n");
                    }
                    regAllocator.release(base);
                    base = tmp;
                }
                String idx = emitExp(lValNode.getExpNodes().get(0), ctx);
                text.append("  sll ").append(idx).append(", ").append(idx).append(", 2\n");
                text.append("  addu ").append(base).append(", ").append(base).append(", ").append(idx).append("\n");
                regAllocator.release(idx);
                return base;
            }
        }
        GlobalVar gv = globals.get(name);
        if (gv != null) {
            String base = regAllocator.alloc();
            text.append("  la ").append(base).append(", ").append(name).append("\n");
            if (lValNode.getExpNodes().isEmpty()) {
                return base;
            } else {
                String idx = emitExp(lValNode.getExpNodes().get(0), ctx);
                text.append("  sll ").append(idx).append(", ").append(idx).append(", 2\n");
                text.append("  addu ").append(base).append(", ").append(base).append(", ").append(idx).append("\n");
                regAllocator.release(idx);
                return base;
            }
        }
        throw new RuntimeException("Undefined symbol: " + name);
    }

    /* ---------- printf ---------- */
    private void handlePrintf(StmtNode stmtNode, FunctionContext ctx) {
        // 先计算所有实参（保证副作用先于任何输出发生），再按格式串顺序打印。
        String fmtRaw = stmtNode.getFormatString().getValue(); // includes quotes
        String fmt = fmtRaw.substring(1, fmtRaw.length() - 1); // strip quotes
        List<String> argRegs = new ArrayList<>();
        for (ExpNode exp : stmtNode.getExpNodes()) {
            argRegs.add(emitExp(exp, ctx));
        }
        int argIdx = 0;
        StringBuilder literal = new StringBuilder();
        for (int i = 0; i < fmt.length(); i++) {
            char ch = fmt.charAt(i);
            if (ch == '\\' && i + 1 < fmt.length() && fmt.charAt(i + 1) == 'n') {
                literal.append('\n');
                i++;
            } else if (ch == '%' && i + 1 < fmt.length() && fmt.charAt(i + 1) == 'd') {
                // 先输出累积的文字，再处理占位符
                if (literal.length() > 0) {
                    String label = newLabel("str");
                    stringLiterals.put(label, toAsciizLiteral(literal.toString()));
                    text.append("  la $a0, ").append(label).append("\n");
                    text.append("  li $v0, 4\n");
                    text.append("  syscall\n");
                    literal.setLength(0);
                }
                // 输出对应实参
                if (argIdx < argRegs.size()) {
                    String r = argRegs.get(argIdx);
                    text.append("  move $a0, ").append(r).append("\n");
                    text.append("  li $v0, 1\n");
                    text.append("  syscall\n");
                    argIdx++;
                }
                i++; // 跳过 'd'
            } else {
                literal.append(ch);
            }
        }
        // 尾部文字
        if (literal.length() > 0) {
            String label = newLabel("str");
            stringLiterals.put(label, toAsciizLiteral(literal.toString()));
            text.append("  la $a0, ").append(label).append("\n");
            text.append("  li $v0, 4\n");
            text.append("  syscall\n");
        }
        // 释放保存的寄存器
        for (String r : argRegs) {
            regAllocator.release(r);
        }
    }

    private String toAsciizLiteral(String s) {
        String escaped = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }

    private String newLabel(String prefix) {
        return prefix + "_" + (labelId++);
    }

    private String newStaticLabel(String funcName, String varName) {
        return "static_" + funcName + "_" + varName + "_" + (staticId++);
    }

    private int align(int size) {
        return (size + 3) / 4 * 4;
    }

    // Save/restore callee-saved $s0-$s7 in the current frame.
    private void saveSRegisters(int frameSize) {
        for (int i = 0; i <= 7; i++) {
            text.append("  sw $s").append(i).append(", ").append(frameSize - 12 - i * 4).append("($sp)\n");
        }
    }

    private void restoreSRegisters(int frameSize) {
        for (int i = 0; i <= 7; i++) {
            text.append("  lw $s").append(i).append(", ").append(frameSize - 12 - i * 4).append("($sp)\n");
        }
    }
}
