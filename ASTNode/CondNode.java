package ASTNode;

import Utils.TokenIterator;

import static ASTNode.LOrExpNode.parseLOrExpNode;

public class CondNode {
    // Cond -> LOrExp

    private LOrExpNode lOrExpNode;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();


    public CondNode(LOrExpNode lOrExpNode) {
        this.lOrExpNode = lOrExpNode;
    }

    public LOrExpNode getLOrExpNode() {
        return lOrExpNode;
    }

    public static CondNode parseCondNode() {
        return new CondNode(parseLOrExpNode());
    }

    public void print() {
        lOrExpNode.print();
        System.out.println(NodeString.get(NodeType.Cond));
    }
}
