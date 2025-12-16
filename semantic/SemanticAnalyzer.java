package semantic;

import ASTNode.*;
import Token.Token;
import Token.TokenType;
import error.Error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SemanticAnalyzer {
    private final SymbolTable symbolTable = new SymbolTable();
    private final Error error = Error.getInstance();

    private SymbolType.BaseType currentReturnType = SymbolType.BaseType.VOID;
    private boolean hasReturnWithValue = false;
    private int loopDepth = 0;

    public void analyze(CompUnitNode compUnitNode) {
        symbolTable.enterScope(); // global scope
        declareBuiltins();
        declareGlobals(compUnitNode);
        // analyze function bodies after all functions are declared
        for (FuncDefNode funcDefNode : compUnitNode.getFuncDefNodes()) {
            analyzeFunction(funcDefNode);
        }
        analyzeMain(compUnitNode.getMainFuncDefNode());
        symbolTable.exitScope();
    }

    public List<String> dumpSymbols() {
        List<String> lines = new ArrayList<>();
        for (SymbolTable.Scope scope : symbolTable.getAllScopes()) {
            scope.getSymbols().values().forEach(sym -> {
                if ("main".equals(sym.getName()) || "getint".equals(sym.getName())) {
                    return;
                }
                lines.add(scope.getId() + " " + sym.getName() + " " + formatType(sym));
            });
        }
        return lines;
    }

    private void declareGlobals(CompUnitNode compUnitNode) {
        for (DeclNode declNode : compUnitNode.getDeclNodes()) {
            handleDecl(declNode);
        }
        for (FuncDefNode funcDefNode : compUnitNode.getFuncDefNodes()) {
            declareFunctionSymbol(funcDefNode);
        }
        declareMainSymbol(compUnitNode.getMainFuncDefNode());
    }

    private void declareBuiltins() {
        // int getint();
        Symbol getint = new Symbol("getint", SymbolKind.FUNC,
                new SymbolType(SymbolType.BaseType.INT, 0), Collections.emptyList(), false, false, null);
        symbolTable.define(getint);
    }

    private void declareFunctionSymbol(FuncDefNode funcDefNode) {
        Token ident = funcDefNode.getIdent();
        SymbolType.BaseType retBase = funcDefNode.getFuncTypeNode().getToken().getTokenType() == TokenType.VOIDTK
                ? SymbolType.BaseType.VOID : SymbolType.BaseType.INT;
        List<SymbolType> params = new ArrayList<>();
        if (funcDefNode.getFuncFParamsNode() != null) {
            for (FuncFParamNode param : funcDefNode.getFuncFParamsNode().getFuncFParamNodes()) {
                int dims = param.getLeftBrackets().size();
                params.add(new SymbolType(SymbolType.BaseType.INT, dims));
            }
        }
        Symbol funcSymbol = new Symbol(ident.getValue(), SymbolKind.FUNC,
                new SymbolType(retBase, 0), params, false, false, ident);
        if (!symbolTable.define(funcSymbol)) {
            error.addError(ident.getLine_number(), "b");
        }
    }

    private void declareMainSymbol(MainFuncDefNode mainFuncDefNode) {
        Token defToken = mainFuncDefNode.getMainToken();
        Symbol funcSymbol = new Symbol("main", SymbolKind.FUNC,
                new SymbolType(SymbolType.BaseType.INT, 0), Collections.emptyList(), false, false, defToken);
        Symbol existing = symbolTable.lookupCurrent("main");
        if (existing != null) {
            int line = defToken != null ? defToken.getLine_number() : mainFuncDefNode.getBlockNode().getRightBraceToken().getLine_number();
            error.addError(line, "b");
        } else {
            symbolTable.define(funcSymbol);
        }
    }

    private void analyzeFunction(FuncDefNode funcDefNode) {
        currentReturnType = funcDefNode.getFuncTypeNode().getToken().getTokenType() == TokenType.VOIDTK
                ? SymbolType.BaseType.VOID : SymbolType.BaseType.INT;
        hasReturnWithValue = false;
        symbolTable.enterScope(); // function scope
        if (funcDefNode.getFuncFParamsNode() != null) {
            for (FuncFParamNode param : funcDefNode.getFuncFParamsNode().getFuncFParamNodes()) {
                declareParam(param);
            }
        }
        analyzeBlock(funcDefNode.getBlockNode(), false);
        if (currentReturnType == SymbolType.BaseType.INT && !endsWithReturnValue(funcDefNode.getBlockNode())) {
            error.addError(funcDefNode.getBlockNode().getRightBraceToken().getLine_number(), "g");
        }
        symbolTable.exitScope();
    }

    private void analyzeMain(MainFuncDefNode mainFuncDefNode) {
        currentReturnType = SymbolType.BaseType.INT;
        hasReturnWithValue = false;
        symbolTable.enterScope(); // main scope
        analyzeBlock(mainFuncDefNode.getBlockNode(), false);
        if (!endsWithReturnValue(mainFuncDefNode.getBlockNode())) {
            error.addError(mainFuncDefNode.getBlockNode().getRightBraceToken().getLine_number(), "g");
        }
        symbolTable.exitScope();
    }

    private void declareParam(FuncFParamNode param) {
        Token ident = param.getIdent();
        int dims = param.getLeftBrackets().size();
        Symbol symbol = new Symbol(ident.getValue(), SymbolKind.PARAM,
                new SymbolType(SymbolType.BaseType.INT, dims), null, false, false, ident);
        if (!symbolTable.define(symbol)) {
            error.addError(ident.getLine_number(), "b");
        }
    }

    private void handleDecl(DeclNode declNode) {
        if (declNode.getConstDecl() != null) {
            handleConstDecl(declNode.getConstDecl());
        } else if (declNode.getVarDecl() != null) {
            handleVarDecl(declNode.getVarDecl());
        }
    }

    private void handleConstDecl(ConstDeclNode constDeclNode) {
        for (ConstDefNode constDefNode : constDeclNode.getConstDefNodes()) {
            Token ident = constDefNode.getIdent();
            int dims = constDefNode.getConstExpNodes().size();
            Symbol symbol = new Symbol(ident.getValue(), SymbolKind.CONST,
                    new SymbolType(SymbolType.BaseType.INT, dims), null, true, false, ident);
            if (!symbolTable.define(symbol)) {
                error.addError(ident.getLine_number(), "b");
            }
            for (ConstExpNode dim : constDefNode.getConstExpNodes()) {
                evalConstExp(dim);
            }
            evalConstInitVal(constDefNode.getConstInitValNode());
        }
    }

    private void handleVarDecl(VarDeclNode varDeclNode) {
        for (VarDefNode varDefNode : varDeclNode.getVarDefNodes()) {
            Token ident = varDefNode.getIdent();
            int dims = varDefNode.getConstExpNodes().size();
            boolean isStatic = varDeclNode.getStaticToken() != null;
            Symbol symbol = new Symbol(ident.getValue(), SymbolKind.VAR,
                    new SymbolType(SymbolType.BaseType.INT, dims), null, false, isStatic, ident);
            if (!symbolTable.define(symbol)) {
                error.addError(ident.getLine_number(), "b");
            }
            for (ConstExpNode dim : varDefNode.getConstExpNodes()) {
                evalConstExp(dim);
            }
            if (varDefNode.getInitValNode() != null) {
                evalInitVal(varDefNode.getInitValNode());
            }
        }
    }

    private void analyzeBlock(BlockNode blockNode) {
        analyzeBlock(blockNode, true);
    }

    // createScope=true for nested blocks; false to reuse current scope (function/main body)
    private void analyzeBlock(BlockNode blockNode, boolean createScope) {
        if (createScope) {
            symbolTable.enterScope();
        }
        for (BlockItemNode blockItemNode : blockNode.getBlockItemNodes()) {
            if (blockItemNode.getDeclNode() != null) {
                handleDecl(blockItemNode.getDeclNode());
            } else if (blockItemNode.getStmtNode() != null) {
                analyzeStmt(blockItemNode.getStmtNode());
            }
        }
        if (createScope) {
            symbolTable.exitScope();
        }
    }

    private void analyzeStmt(StmtNode stmtNode) {
        switch (stmtNode.getType()) {
            case LValAssignExp:
                checkAssignable(stmtNode.getLValNode());
                evalExp(stmtNode.getExpNode());
                break;
            case Exp:
                if (stmtNode.getExpNode() != null) {
                    evalExp(stmtNode.getExpNode());
                }
                break;
            case Block:
                analyzeBlock(stmtNode.getBlockNode());
                break;
            case If:
                evalCond(stmtNode.getCondNode());
                stmtNode.getStmtNodes().forEach(this::analyzeStmt);
                break;
            case For:
                loopDepth++;
                analyzeForStmt(stmtNode);
                loopDepth--;
                break;
            case Break:
            case Continue:
                if (loopDepth == 0) {
                    error.addError(stmtNode.getBreakOrContinueToken().getLine_number(), "m");
                }
                break;
            case Return:
                handleReturn(stmtNode);
                break;
            case LValAssignGetint:
                checkAssignable(stmtNode.getLValNode());
                break;
            case Printf:
                handlePrintf(stmtNode);
                break;
        }
    }

    private void handleReturn(StmtNode stmtNode) {
        if (stmtNode.getExpNode() != null) {
            if (currentReturnType == SymbolType.BaseType.VOID) {
                error.addError(stmtNode.getReturnToken().getLine_number(), "f");
            } else {
                hasReturnWithValue = true;
            }
            evalExp(stmtNode.getExpNode());
        } else {
            if (currentReturnType == SymbolType.BaseType.INT) {
                // missing return value handled at function end (g)
            }
        }
    }

    private void analyzeForStmt(StmtNode stmtNode) {
        ForStmtNode first = stmtNode.getForStmtNode1();
        ForStmtNode second = stmtNode.getForStmtNode2();
        if (first != null) {
            analyzeForStmtAssign(first);
        }
        if (stmtNode.getCondNode() != null) {
            evalCond(stmtNode.getCondNode());
        }
        if (second != null) {
            analyzeForStmtAssign(second);
        }
        stmtNode.getStmtNodes().forEach(this::analyzeStmt);
    }

    private void analyzeForStmtAssign(ForStmtNode forStmtNode) {
        for (int i = 0; i < forStmtNode.getLValNodes().size(); i++) {
            LValNode lValNode = forStmtNode.getLValNodes().get(i);
            checkAssignable(lValNode);
            evalExp(forStmtNode.getExpNodes().get(i));
        }
    }

    private void handlePrintf(StmtNode stmtNode) {
        String fmt = stmtNode.getFormatString().getValue();
        int placeholders = countPlaceholders(fmt);
        int args = stmtNode.getExpNodes().size();
        if (placeholders != args) {
            error.addError(stmtNode.getPrintfToken().getLine_number(), "l");
        }
        for (ExpNode expNode : stmtNode.getExpNodes()) {
            evalExp(expNode);
        }
    }

    private int countPlaceholders(String fmt) {
        int cnt = 0;
        for (int i = 0; i + 1 < fmt.length(); i++) {
            if (fmt.charAt(i) == '%' && fmt.charAt(i + 1) == 'd') {
                cnt++;
            }
        }
        return cnt;
    }

    private void checkAssignable(LValNode lValNode) {
        Symbol symbol = resolveSymbol(lValNode.getIdent());
        if (symbol == null) {
            return;
        }
        if (symbol.isConst()) {
            error.addError(lValNode.getIdent().getLine_number(), "h");
        }
    }

    private void evalCond(CondNode condNode) {
        evalLOrExp(condNode.getLOrExpNode());
    }

    private SymbolType evalConstExp(ConstExpNode constExpNode) {
        return evalAddExp(constExpNode.getAddExpNode());
    }

    private void evalConstInitVal(ConstInitValNode node) {
        for (ConstExpNode exp : node.getConstExpNodes()) {
            evalConstExp(exp);
        }
    }

    private void evalInitVal(InitValNode node) {
        for (ExpNode expNode : node.getExpNodes()) {
            evalExp(expNode);
        }
    }

    private SymbolType evalExp(ExpNode expNode) {
        return evalAddExp(expNode.getAddExpNode());
    }

    private SymbolType evalAddExp(AddExpNode addExpNode) {
        SymbolType left = evalMulExp(addExpNode.getMulExpNode());
        if (addExpNode.getOperator() != null) {
            // 有算术运算，结果视为标量 int
            evalAddExp(addExpNode.getAddExpNode());
            return new SymbolType(SymbolType.BaseType.INT, 0);
        }
        return left;
    }

    private SymbolType evalMulExp(MulExpNode mulExpNode) {
        SymbolType left = evalUnaryExp(mulExpNode.getUnaryExpNode());
        if (mulExpNode.getOperator() != null) {
            evalMulExp(mulExpNode.getMulExpNode());
            return new SymbolType(SymbolType.BaseType.INT, 0);
        }
        return left;
    }

    private SymbolType evalUnaryExp(UnaryExpNode unaryExpNode) {
        if (unaryExpNode.getPrimaryExpNode() != null) {
            return evalPrimaryExp(unaryExpNode.getPrimaryExpNode());
        } else if (unaryExpNode.getIdent() != null) {
            String name = unaryExpNode.getIdent().getValue();
            // 内建函数特判
            if ("getint".equals(name)) {
                if (unaryExpNode.getFuncRParamsNode() != null) {
                    for (ExpNode expNode : unaryExpNode.getFuncRParamsNode().getExpNodes()) {
                        evalExp(expNode);
                    }
                }
                return new SymbolType(SymbolType.BaseType.INT, 0);
            }

            Symbol funcSymbol = symbolTable.lookup(name);
            if (funcSymbol == null || funcSymbol.getKind() != SymbolKind.FUNC) {
                error.addError(unaryExpNode.getIdent().getLine_number(), "c");
                if (unaryExpNode.getFuncRParamsNode() != null) {
                    for (ExpNode expNode : unaryExpNode.getFuncRParamsNode().getExpNodes()) {
                        evalExp(expNode);
                    }
                }
                return new SymbolType(SymbolType.BaseType.INT, 0);
            }
            List<SymbolType> params = funcSymbol.getParams() == null ? Collections.emptyList() : funcSymbol.getParams();
            List<ExpNode> args = unaryExpNode.getFuncRParamsNode() == null
                    ? new ArrayList<>()
                    : unaryExpNode.getFuncRParamsNode().getExpNodes();
            checkCall(unaryExpNode.getIdent(), params, args);
            return funcSymbol != null ? funcSymbol.getType() : new SymbolType(SymbolType.BaseType.INT, 0);
        } else {
            evalUnaryExp(unaryExpNode.getUnaryExpNode());
            return new SymbolType(SymbolType.BaseType.INT, 0);
        }
    }

    private void checkCall(Token ident, List<SymbolType> params, List<ExpNode> args) {
        if (params.size() != args.size()) {
            error.addError(ident.getLine_number(), "d");
        } else {
            for (int i = 0; i < params.size(); i++) {
                SymbolType expected = params.get(i);
                SymbolType actual = evalExp(args.get(i));
                if (expected.getDimensions() != actual.getDimensions()) {
                    error.addError(ident.getLine_number(), "e");
                    break;
                }
            }
        }
    }

    private SymbolType evalPrimaryExp(PrimaryExpNode primaryExpNode) {
        if (primaryExpNode.getExpNode() != null) {
            return evalExp(primaryExpNode.getExpNode());
        } else if (primaryExpNode.getLValNode() != null) {
            return evalLVal(primaryExpNode.getLValNode());
        } else {
            return new SymbolType(SymbolType.BaseType.INT, 0);
        }
    }

    private SymbolType evalLVal(LValNode lValNode) {
        Symbol symbol = resolveSymbol(lValNode.getIdent());
        if (symbol == null) {
            return new SymbolType(SymbolType.BaseType.INT, 0);
        }
        int remainDim = Math.max(0, symbol.getType().getDimensions() - lValNode.getExpNodes().size());
        for (ExpNode expNode : lValNode.getExpNodes()) {
            evalExp(expNode);
        }
        return new SymbolType(SymbolType.BaseType.INT, remainDim);
    }

    private Symbol resolveSymbol(Token ident) {
        Symbol symbol = symbolTable.lookup(ident.getValue());
        if (symbol == null) {
            error.addError(ident.getLine_number(), "c");
        }
        return symbol;
    }

    private SymbolType evalLOrExp(LOrExpNode node) {
        evalLAndExp(node.getLAndExpNode());
        if (node.getOrToken() != null) {
            evalLOrExp(node.getLOrExpNode());
        }
        return new SymbolType(SymbolType.BaseType.INT, 0);
    }

    private SymbolType evalLAndExp(LAndExpNode node) {
        evalEqExp(node.getEqExpNode());
        if (node.getAndToken() != null) {
            evalLAndExp(node.getLAndExpNode());
        }
        return new SymbolType(SymbolType.BaseType.INT, 0);
    }

    private SymbolType evalEqExp(EqExpNode node) {
        evalRelExp(node.getRelExpNode());
        if (node.getOperator() != null) {
            evalEqExp(node.getEqExpNode());
        }
        return new SymbolType(SymbolType.BaseType.INT, 0);
    }

    private SymbolType evalRelExp(RelExpNode node) {
        evalAddExp(node.getAddExpNode());
        if (node.getOperator() != null) {
            evalRelExp(node.getRelExpNode());
        }
        return new SymbolType(SymbolType.BaseType.INT, 0);
    }

    // Only check the last statement in a block (as per spec for g)
    private boolean endsWithReturnValue(BlockNode blockNode) {
        List<BlockItemNode> items = blockNode.getBlockItemNodes();
        for (int i = items.size() - 1; i >= 0; i--) {
            BlockItemNode item = items.get(i);
            if (item.getStmtNode() == null) {
                continue;
            }
            return stmtEndsWithReturnValue(item.getStmtNode());
        }
        return false;
    }

    private boolean stmtEndsWithReturnValue(StmtNode stmt) {
        switch (stmt.getType()) {
            case Return:
                return stmt.getExpNode() != null;
            case Block:
                return endsWithReturnValue(stmt.getBlockNode());
            default:
                return false;
        }
    }

    private String formatType(Symbol sym) {
        SymbolType t = sym.getType();
        if (sym.getKind() == SymbolKind.FUNC) {
            return t.getBaseType() == SymbolType.BaseType.INT ? "IntFunc" : "VoidFunc";
        }
        boolean isArray = t.getDimensions() > 0;
        if (sym.isConst()) {
            return isArray ? "ConstIntArray" : "ConstInt";
        }
        if (sym.isStatic()) {
            return isArray ? "StaticIntArray" : "StaticInt";
        }
        return isArray ? "IntArray" : "Int";
    }
}
