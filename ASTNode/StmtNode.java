package ASTNode;

import Token.*;
import Utils.TokenIterator;

import java.util.ArrayList;

import static ASTNode.BlockNode.parseBlockNode;
import static ASTNode.CondNode.parseCondNode;
import static ASTNode.ExpNode.parseExpNode;
import static ASTNode.ForStmtNode.parseForStmtNode;
import static ASTNode.LValNode.parseLValNode;

public class StmtNode {
    // Stmt -> LVal '=' Exp ';'
    //	| [Exp] ';'
    //	| Block
    //	| 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    //  | 'for' '(' [ ForStmt ] ';' [ Cond ] ';' [ ForStmt ] ')' Stmt
    //	| 'break' ';' | 'continue' ';'
    //	| 'return' [Exp] ';'
    //	| LVal '=' 'getint' '(' ')' ';'
    //	| 'printf' '(' FormatString { ',' Exp } ')' ';'
    public enum StmtType {
        LValAssignExp, Exp, Block, If, Break, Continue, Return, LValAssignGetint, Printf,For
    }

    private StmtType type;
    private LValNode lValNode;
    private Token assignToken;
    private ExpNode expNode;
    private Token semicnToken;
    private BlockNode blockNode;
    private Token ifToken;
    private Token leftParentToken;
    private CondNode condNode;
    private Token rightParentToken;
    private ArrayList<StmtNode> stmtNodes;
    private Token elseToken;
    private Token forToken;
    private ForStmtNode forStmtNode1;
    private ForStmtNode forStmtNode2;
    private ArrayList<Token> semicnTokens;

    private Token breakOrContinueToken;
    private Token returnToken;
    private Token getintToken;
    private Token printfToken;
    private Token formatString;
    private ArrayList<Token> commas;
    private ArrayList<ExpNode> expNodes;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();

    public StmtNode(StmtType type, Token forToken, Token leftParentToken,ForStmtNode forStmtNode1,ForStmtNode forStmtNode2, ArrayList<Token> semicnTokens, CondNode condNode,ArrayList<StmtNode> stmtNodes,Token rightParentToken) {
        this.type = type;
        this.forToken = forToken;
        this.leftParentToken = leftParentToken;
        this.forStmtNode1 = forStmtNode1;
        this.forStmtNode2 = forStmtNode2;
        this.semicnTokens = semicnTokens;
        this.condNode = condNode;
        this.stmtNodes = stmtNodes;
        this.rightParentToken = rightParentToken;
    }

    public StmtNode(StmtType type, LValNode lValNode, Token assignToken, ExpNode expNode, Token semicnToken) {
        // LVal '=' Exp ';'
        this.type = type;
        this.lValNode = lValNode;
        this.assignToken = assignToken;
        this.expNode = expNode;
        this.semicnToken = semicnToken;
    }

    public StmtNode(StmtType type, ExpNode expNode, Token semicnToken) {
        // [Exp] ';'
        this.type = type;
        this.expNode = expNode;
        this.semicnToken = semicnToken;
    }

    public StmtNode(StmtType type, BlockNode blockNode) {
        // Block
        this.type = type;
        this.blockNode = blockNode;
    }

    public StmtNode(StmtType type, Token ifToken, Token leftParentToken, CondNode condNode, Token rightParentToken, ArrayList<StmtNode> stmtNodes, Token elseToken) {
        // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        this.type = type;
        this.ifToken = ifToken;
        this.leftParentToken = leftParentToken;
        this.condNode = condNode;
        this.rightParentToken = rightParentToken;
        this.stmtNodes = stmtNodes;
        this.elseToken = elseToken;
    }



    public StmtNode(StmtType type, Token breakOrContinueToken, Token semicnToken) {
        // 'break' ';'
        this.type = type;
        this.breakOrContinueToken = breakOrContinueToken;
        this.semicnToken = semicnToken;
    }

    public StmtNode(StmtType type, Token returnToken, ExpNode expNode, Token semicnToken) {
        // 'return' [Exp] ';'
        this.type = type;
        this.returnToken = returnToken;
        this.expNode = expNode;
        this.semicnToken = semicnToken;
    }

    public StmtNode(StmtType type, LValNode lValNode, Token assignToken, Token getintToken, Token leftParentToken, Token rightParentToken, Token semicnToken) {
        // LVal '=' 'getint' '(' ')' ';'
        this.type = type;
        this.lValNode = lValNode;
        this.assignToken = assignToken;
        this.getintToken = getintToken;
        this.leftParentToken = leftParentToken;
        this.rightParentToken = rightParentToken;
        this.semicnToken = semicnToken;
    }

    public StmtNode(StmtType type, Token printfToken, Token leftParentToken, Token formatString, ArrayList<Token> commas, ArrayList<ExpNode> expNodes, Token rightParentToken, Token semicnToken) {
        // 'printf' '(' FormatString { ',' Exp } ')' ';'
        this.type = type;
        this.printfToken = printfToken;
        this.leftParentToken = leftParentToken;
        this.formatString = formatString;
        this.commas = commas;
        this.expNodes = expNodes;
        this.rightParentToken = rightParentToken;
        this.semicnToken = semicnToken;
    }

    public StmtType getType() {
        return type;
    }

    public LValNode getLValNode() {
        return lValNode;
    }

    public Token getAssignToken() {
        return assignToken;
    }

    public ExpNode getExpNode() {
        return expNode;
    }

    public Token getSemicnToken() {
        return semicnToken;
    }

    public BlockNode getBlockNode() {
        return blockNode;
    }

    public Token getIfToken() {
        return ifToken;
    }

    public Token getLeftParentToken() {
        return leftParentToken;
    }

    public CondNode getCondNode() {
        return condNode;
    }

    public Token getRightParentToken() {
        return rightParentToken;
    }

    public ArrayList<StmtNode> getStmtNodes() {
        return stmtNodes;
    }

    public Token getElseToken() {
        return elseToken;
    }

    public ForStmtNode getForStmtNode1() {
        return forStmtNode1;
    }

    public ForStmtNode getForStmtNode2() {
        return forStmtNode2;
    }

    public Token getBreakOrContinueToken() {
        return breakOrContinueToken;
    }

    public Token getGetintToken() {
        return getintToken;
    }

    public Token getPrintfToken() {
        return printfToken;
    }

    public Token getFormatString() {
        return formatString;
    }

    public ArrayList<Token> getCommas() {
        return commas;
    }

    public ArrayList<ExpNode> getExpNodes() {
        return expNodes;
    }

    public Token getReturnToken() {
        return returnToken;
    }

    public static StmtNode parseStmtNode() {
        if (tokenIterator.getCurrentToken().getTokenType() == TokenType.LBRACE) {
            // Block
            BlockNode blockNode = parseBlockNode();
            return new StmtNode(StmtNode.StmtType.Block, blockNode);
        }
        else if (tokenIterator.getCurrentToken().getTokenType() == TokenType.FORTK) {
            //'for' '(' [ ForStmt ] ';' [ Cond ] ';' [ ForStmt ] ')' Stmt
            Token forToken = tokenIterator.match(TokenType.FORTK);
            Token leftParentToken = tokenIterator.match(TokenType.LPARENT);
            ForStmtNode forStmtNode1 = null;
            ForStmtNode forStmtNode2 = null;
            ArrayList<Token> semicnTokens = new ArrayList<>();
            ArrayList<StmtNode> stmtNodes = new ArrayList<>();
            CondNode condNode = null;
            if(tokenIterator.getCurrentToken().getTokenType() == TokenType.IDENFR) {
                forStmtNode1 = parseForStmtNode();

            }
            semicnTokens.add(tokenIterator.match(TokenType.SEMICN));
            if(tokenIterator.isExp()) {
                condNode = parseCondNode();
            }
            semicnTokens.add(tokenIterator.match(TokenType.SEMICN));
            if(tokenIterator.getCurrentToken().getTokenType() == TokenType.IDENFR){
                forStmtNode2 = parseForStmtNode();

            }
            Token rightParentToken = tokenIterator.match(TokenType.RPARENT);
            stmtNodes.add(parseStmtNode());
            return new StmtNode(StmtNode.StmtType.For,forToken,leftParentToken,forStmtNode1,forStmtNode2,semicnTokens,condNode,stmtNodes,rightParentToken);
        }

        else if (tokenIterator.getCurrentToken().getTokenType() == TokenType.PRINTFTK) {
            // 'printf' '(' FormatString { ',' Exp } ')' ';'
            Token printfToken = tokenIterator.match(TokenType.PRINTFTK);
            Token leftParentToken = tokenIterator.match(TokenType.LPARENT);
            Token formatString = tokenIterator.match(TokenType.STRCON);
            ArrayList<Token> commas = new ArrayList<>();
            ArrayList<ExpNode> expNodes = new ArrayList<>();
            while (tokenIterator.getCurrentToken().getTokenType() == TokenType.COMMA) {
                commas.add(tokenIterator.match(TokenType.COMMA));
                expNodes.add(parseExpNode());
            }
            Token rightParentToken = tokenIterator.match(TokenType.RPARENT);
            Token semicnToken = tokenIterator.match(TokenType.SEMICN);
            return new StmtNode(StmtNode.StmtType.Printf, printfToken, leftParentToken, formatString, commas, expNodes, rightParentToken, semicnToken);
        } else if (tokenIterator.getCurrentToken().getTokenType() == TokenType.IFTK) {
            // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            Token ifToken = tokenIterator.match(TokenType.IFTK);
            Token leftParentToken = tokenIterator.match(TokenType.LPARENT);
            CondNode condNode = parseCondNode();
            Token rightParentToken = tokenIterator.match(TokenType.RPARENT);
            ArrayList<StmtNode> stmtNodes = new ArrayList<>();
            stmtNodes.add(parseStmtNode());
            Token elseToken = null;
            if (tokenIterator.getCurrentToken().getTokenType() == TokenType.ELSETK) {
                elseToken = tokenIterator.match(TokenType.ELSETK);
                stmtNodes.add(parseStmtNode());
            }
            return new StmtNode(StmtNode.StmtType.If, ifToken, leftParentToken, condNode, rightParentToken, stmtNodes, elseToken);
        }  else if (tokenIterator.getCurrentToken().getTokenType() == TokenType.BREAKTK) {
            // 'break' ';'
            Token breakToken = tokenIterator.match(TokenType.BREAKTK);
            Token semicnToken = tokenIterator.match(TokenType.SEMICN);
            return new StmtNode(StmtNode.StmtType.Break, breakToken, semicnToken);
        } else if (tokenIterator.getCurrentToken().getTokenType() == TokenType.CONTINUETK) {
            // 'continue' ';'
            Token continueToken = tokenIterator.match(TokenType.CONTINUETK);
            Token semicnToken = tokenIterator.match(TokenType.SEMICN);
            return new StmtNode(StmtNode.StmtType.Continue, continueToken, semicnToken);
        } else if (tokenIterator.getCurrentToken().getTokenType() == TokenType.RETURNTK) {
            // 'return' [Exp] ';'
            Token returnToken = tokenIterator.match(TokenType.RETURNTK);
            ExpNode expNode = null;
            if (isExp()) {
                expNode = parseExpNode();
            }
            Token semicnToken = tokenIterator.match(TokenType.SEMICN);
            return new StmtNode(StmtNode.StmtType.Return, returnToken, expNode, semicnToken);
        }
        else {
            int assign = tokenIterator.getIndex();
            for (int i = tokenIterator.getIndex(); i < tokenIterator.getTokens().size() && tokenIterator.getTokens().get(i).getLine_number() == tokenIterator.getCurrentToken().getLine_number(); i++) {
                if (tokenIterator.getTokens().get(i).getTokenType() == TokenType.ASSIGN) {
                    assign = i;
                }
            }
            if (assign > tokenIterator.getIndex()) {
                // LVal '=' (Exp | getint())
                LValNode lValNode = parseLValNode();
                Token assignToken = tokenIterator.match(TokenType.ASSIGN);
                if (tokenIterator.getCurrentToken().getTokenType() == TokenType.IDENFR
                        && "getint".equals(tokenIterator.getCurrentToken().getValue())
                        && tokenIterator.getNextNToken(1).getTokenType() == TokenType.LPARENT) {
                    Token getintToken = tokenIterator.match(TokenType.IDENFR);
                    Token leftParent = tokenIterator.match(TokenType.LPARENT);
                    Token rightParent = tokenIterator.match(TokenType.RPARENT);
                    Token semicnToken = tokenIterator.match(TokenType.SEMICN);
                    return new StmtNode(StmtNode.StmtType.LValAssignGetint, lValNode, assignToken, getintToken, leftParent, rightParent, semicnToken);
                } else {
                    ExpNode expNode = parseExpNode();
                    Token semicnToken = tokenIterator.match(TokenType.SEMICN);
                    return new StmtNode(StmtNode.StmtType.LValAssignExp, lValNode, assignToken, expNode, semicnToken);
                }

            } else {
                // [Exp] ';'
                ExpNode expNode = null;
                if (isExp()) {
                    expNode = parseExpNode();
                }
                Token semicnToken = tokenIterator.match(TokenType.SEMICN);
                return new StmtNode(StmtNode.StmtType.Exp, expNode, semicnToken);
            }
        }
    }

    private static boolean isExp() {
        return tokenIterator.getCurrentToken().getTokenType() == TokenType.IDENFR ||
                tokenIterator.getCurrentToken().getTokenType() == TokenType.PLUS ||
                tokenIterator.getCurrentToken().getTokenType() == TokenType.MINU ||
                tokenIterator.getCurrentToken().getTokenType() == TokenType.NOT ||
                tokenIterator.getCurrentToken().getTokenType() == TokenType.LPARENT ||
                tokenIterator.getCurrentToken().getTokenType() == TokenType.INTCON;
    }
    
    public void print() {
        switch (type) {
            case LValAssignExp:
                // LVal '=' Exp ';'
                lValNode.print();
                System.out.println(assignToken.print());
                expNode.print();
                System.out.println(semicnToken.print());
                break;
            case Exp:
                // [Exp] ';'
                if (expNode != null) expNode.print();
                System.out.println(semicnToken.print());
                break;
            case For:
                //'for' '(' [ ForStmt ] ';' [ Cond ] ';' [ ForStmt ] ')' Stmt
                System.out.println(forToken.print());
                System.out.println(leftParentToken.print());
                if(forStmtNode1 != null) forStmtNode1.print();
                System.out.println(semicnTokens.get(0).print());
                if(condNode != null) condNode.print();
                System.out.println(semicnTokens.get(1).print());
                if(forStmtNode2 != null) forStmtNode2.print();
                System.out.println(rightParentToken.print());
                stmtNodes.get(0).print();
                break;

            case Block:
                // Block
                blockNode.print();
                break;
            case If:
                // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
                System.out.println(ifToken.print());
                System.out.println(leftParentToken.print());
                condNode.print();
                System.out.println(rightParentToken.print());
                stmtNodes.get(0).print();
                if (elseToken != null) {
                    System.out.println(elseToken.print());
                    stmtNodes.get(1).print();
                }
                break;
            case Break:
                // 'break' ';'
            case Continue:
                // 'continue' ';'
                System.out.println(breakOrContinueToken.print());
                System.out.println(semicnToken.print());
                break;
            case Return:
                // 'return' [Exp] ';'
                System.out.println(returnToken.print());
                if (expNode != null) {
                    expNode.print();
                }
                System.out.println(semicnToken.print());
                break;
            case LValAssignGetint:
                // LVal '=' 'getint' '(' ')' ';'
                lValNode.print();
                System.out.println(assignToken.print());
                System.out.println(getintToken.print());
                System.out.println(leftParentToken.print());
                System.out.println(rightParentToken.print());
                System.out.println(semicnToken.print());
                break;
            case Printf:
                // 'printf' '(' FormatString { ',' Exp } ')' ';'
                System.out.println(printfToken.print());
                System.out.println(leftParentToken.print());
                System.out.println(formatString.print());
                for (int i = 0; i < commas.size(); i++) {
                    System.out.println(commas.get(i).print());
                    expNodes.get(i).print();
                }
                System.out.println(rightParentToken.print());
                System.out.println(semicnToken.print());
                break;
        }
        System.out.println(NodeString.get(NodeType.Stmt));
    }
}
