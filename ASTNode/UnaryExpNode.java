package ASTNode;

import Token.*;
import Utils.TokenIterator;

import static ASTNode.FuncRParamsNode.parseFuncRParamsNode;
import static ASTNode.PrimaryExpNode.parsePrimaryExpNode;
import static ASTNode.UnaryOpNode.parseUnaryOpNode;

public class UnaryExpNode {
    // UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    private PrimaryExpNode primaryExpNode = null;
    private Token ident = null;
    private Token leftParentToken = null;
    private FuncRParamsNode funcRParamsNode = null;
    private Token rightParentToken = null;
    private UnaryOpNode unaryOpNode = null;
    private UnaryExpNode unaryExpNode = null;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();

    public UnaryExpNode(PrimaryExpNode primaryExpNode) {
        this.primaryExpNode = primaryExpNode;
    }

    public UnaryExpNode(Token ident, Token leftParentToken, FuncRParamsNode funcRParamsNode, Token rightParentToken) {
        this.ident = ident;
        this.leftParentToken = leftParentToken;
        this.funcRParamsNode = funcRParamsNode;
        this.rightParentToken = rightParentToken;
    }

    public UnaryExpNode(UnaryOpNode unaryOpNode, UnaryExpNode unaryExpNode) {
        this.unaryOpNode = unaryOpNode;
        this.unaryExpNode = unaryExpNode;
    }

    public PrimaryExpNode getPrimaryExpNode() {
        return primaryExpNode;
    }

    public Token getIdent() {
        return ident;
    }

    public Token getLeftParentToken() {
        return leftParentToken;
    }

    public FuncRParamsNode getFuncRParamsNode() {
        return funcRParamsNode;
    }

    public Token getRightParentToken() {
        return rightParentToken;
    }

    public UnaryOpNode getUnaryOpNode() {
        return unaryOpNode;
    }

    public UnaryExpNode getUnaryExpNode() {
        return unaryExpNode;
    }

    public static UnaryExpNode parseUnaryExpNode() {
        if (tokenIterator.getCurrentToken().getTokenType() == TokenType.IDENFR && tokenIterator.getNextNToken( 1).getTokenType() == TokenType.LPARENT) {
            Token ident = tokenIterator.match(TokenType.IDENFR);
            Token leftParentToken = tokenIterator.match(TokenType.LPARENT);
            FuncRParamsNode funcRParamsNode = null;
            if (tokenIterator.isExp()) {
                funcRParamsNode = parseFuncRParamsNode();
            }
            Token rightParentToken = tokenIterator.match(TokenType.RPARENT);
            return new UnaryExpNode(ident, leftParentToken, funcRParamsNode, rightParentToken);
        } else if (tokenIterator.getCurrentToken().getTokenType() == TokenType.PLUS || tokenIterator.getCurrentToken().getTokenType() == TokenType.MINU || tokenIterator.getCurrentToken().getTokenType() == TokenType.NOT) {
            UnaryOpNode unaryOpNode = parseUnaryOpNode();
            UnaryExpNode unaryExpNode = parseUnaryExpNode();
            return new UnaryExpNode(unaryOpNode, unaryExpNode);
        } else {
            PrimaryExpNode primaryExpNode = parsePrimaryExpNode();
            return new UnaryExpNode(primaryExpNode);
        }
    }

    public void print() {
        if (primaryExpNode != null) {
            primaryExpNode.print();
        } else if (ident != null) {
            System.out.println(ident.print());
            System.out.println(leftParentToken.print());
            if (funcRParamsNode != null) {
                funcRParamsNode.print();
            }
            System.out.println(rightParentToken.print());
        } else {
            unaryOpNode.print();
            unaryExpNode.print();
        }
        System.out.println(NodeString.get(NodeType.UnaryExp));
    }


}
