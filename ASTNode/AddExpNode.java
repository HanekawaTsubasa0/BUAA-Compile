package ASTNode;

import Token.*;
import Utils.TokenIterator;

import static ASTNode.MulExpNode.parseMulExpNode;

public class AddExpNode {
    // AddExp -> MulExp | AddExp ('+' | '−') MulExp
    private MulExpNode mulExpNode;
    private Token operator;
    private AddExpNode addExpNode;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();

    public AddExpNode(MulExpNode mulExpNode, Token operator, AddExpNode addExpNode) {
        this.mulExpNode = mulExpNode;
        this.operator = operator;
        this.addExpNode = addExpNode;
    }

    public MulExpNode getMulExpNode() {
        return mulExpNode;
    }

    public Token getOperator() {
        return operator;
    }

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }

    public static AddExpNode parseAddExpNode() {
        MulExpNode mulExpNode = parseMulExpNode();
        Token operator = null;
        AddExpNode addExpNode = null;
        if (tokenIterator.getCurrentToken().getTokenType() == TokenType.PLUS) {
            operator = tokenIterator.match(TokenType.PLUS);
            addExpNode = parseAddExpNode();
        } else if (tokenIterator.getCurrentToken().getTokenType() == TokenType.MINU) {
            operator = tokenIterator.match(TokenType.MINU);
            addExpNode = parseAddExpNode();
        }
        return new AddExpNode(mulExpNode, operator, addExpNode);
    }

    public void print() {
        mulExpNode.print();
        System.out.println(NodeString.get(NodeType.AddExp));
        if (operator != null) {
            System.out.println(operator.print());
            addExpNode.print();
        }
    }


}
