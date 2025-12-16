package ASTNode;

import Utils.TokenIterator;

import static ASTNode.AddExpNode.parseAddExpNode;

public class ExpNode {
    // Exp -> AddExp

    private AddExpNode addExpNode;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();

    public ExpNode(AddExpNode addExpNode) {
        this.addExpNode = addExpNode;
    }

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }

    public static ExpNode parseExpNode() {
        return new ExpNode(parseAddExpNode());
    }

    public void print() {
        addExpNode.print();
        System.out.println(NodeString.get(NodeType.Exp));
    }


}
