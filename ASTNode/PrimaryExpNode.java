package ASTNode;

import Token.*;
import Utils.TokenIterator;

import static ASTNode.ExpNode.parseExpNode;
import static ASTNode.LValNode.parseLValNode;
import static ASTNode.NumberNode.parseNumberNode;

public class PrimaryExpNode {
    // PrimaryExp -> '(' Exp ')' | LVal | Number

    private Token leftParentToken = null;
    private ExpNode expNode = null;
    private Token rightParentToken = null;
    private LValNode lValNode = null;
    private NumberNode numberNode = null;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();


    public PrimaryExpNode(Token leftParentToken, ExpNode expNode, Token rightParentToken) {
        this.leftParentToken = leftParentToken;
        this.expNode = expNode;
        this.rightParentToken = rightParentToken;
    }

    public PrimaryExpNode(LValNode lValNode) {
        this.lValNode = lValNode;
    }

    public PrimaryExpNode(NumberNode numberNode) {
        this.numberNode = numberNode;
    }

    public Token getLeftParentToken() {
        return leftParentToken;
    }

    public ExpNode getExpNode() {
        return expNode;
    }

    public Token getRightParentToken() {
        return rightParentToken;
    }

    public LValNode getLValNode() {
        return lValNode;
    }

    public NumberNode getNumberNode() {
        return numberNode;
    }

    public static PrimaryExpNode parsePrimaryExpNode() {
        if (tokenIterator.getCurrentToken().getTokenType() == TokenType.LPARENT) {
            Token leftParentToken = tokenIterator.match(TokenType.LPARENT);
            ExpNode expNode = parseExpNode();
            Token rightParentToken = tokenIterator.match(TokenType.RPARENT);
            return new PrimaryExpNode(leftParentToken, expNode, rightParentToken);
        } else if (tokenIterator.getCurrentToken().getTokenType() == TokenType.INTCON) {

            NumberNode numberNode = parseNumberNode();
            return new PrimaryExpNode(numberNode);
        } else {
            LValNode lValNode = parseLValNode();
            return new PrimaryExpNode(lValNode);
        }
    }

    public void print() {
        if (expNode != null) {
            System.out.println(leftParentToken.print());
            expNode.print();
            System.out.println(rightParentToken.print());
        } else if (lValNode != null) {
            lValNode.print();
        } else {
            numberNode.print();
        }
        System.out.println(NodeString.get(NodeType.PrimaryExp));
    }


}
