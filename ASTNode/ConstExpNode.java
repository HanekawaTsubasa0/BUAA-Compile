package ASTNode;

import Utils.TokenIterator;

import static ASTNode.AddExpNode.parseAddExpNode;

public class ConstExpNode {
    // ConstExp -> AddExp

    private AddExpNode addExpNode;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();

    public ConstExpNode(AddExpNode addExpNode) {
        this.addExpNode = addExpNode;
    }

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }

    public static ConstExpNode parseConstExpNode() {
        return new ConstExpNode(parseAddExpNode());
    }

    public void print() {
        addExpNode.print();
        System.out.println(NodeString.get(NodeType.ConstExp));
    }
}
