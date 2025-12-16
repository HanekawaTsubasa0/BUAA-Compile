package ASTNode;

import Token.*;
import Utils.TokenIterator;

import static ASTNode.UnaryExpNode.parseUnaryExpNode;

public class MulExpNode {
    // MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp

    private UnaryExpNode unaryExpNode;
    private Token operator;
    private MulExpNode mulExpNode;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();

    public MulExpNode(UnaryExpNode unaryExpNode, Token operator, MulExpNode mulExpNode) {
        this.unaryExpNode = unaryExpNode;
        this.operator = operator;
        this.mulExpNode = mulExpNode;
    }

    public UnaryExpNode getUnaryExpNode() {
        return unaryExpNode;
    }

    public Token getOperator() {
        return operator;
    }

    public MulExpNode getMulExpNode() {
        return mulExpNode;
    }

    public static MulExpNode parseMulExpNode() {
        UnaryExpNode unaryExpNode = parseUnaryExpNode();
        Token operator = null;
        MulExpNode mulExpNode = null;
        if (tokenIterator.getCurrentToken().getTokenType() == TokenType.MULT) {
            operator = tokenIterator.match(TokenType.MULT);
            mulExpNode = parseMulExpNode();
        } else if (tokenIterator.getCurrentToken().getTokenType() == TokenType.DIV) {
            operator = tokenIterator.match(TokenType.DIV);
            mulExpNode = parseMulExpNode();
        } else if (tokenIterator.getCurrentToken().getTokenType() == TokenType.MOD) {
            operator = tokenIterator.match(TokenType.MOD);
            mulExpNode = parseMulExpNode();
        }
        return new MulExpNode(unaryExpNode, operator, mulExpNode);
    }

    public void print() {
        unaryExpNode.print();
        System.out.println(NodeString.get(NodeType.MulExp));
        if (operator != null) {
            System.out.println(operator.print());
            mulExpNode.print();
        }
    }


}
