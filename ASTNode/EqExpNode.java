package ASTNode;

import Token.*;
import Utils.TokenIterator;

import static ASTNode.RelExpNode.parseRelExpNode;

public class EqExpNode {
    // RelExp | EqExp ('==' | '!=') RelExp
    private RelExpNode relExpNode;
    private Token operator;
    private EqExpNode eqExpNode;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();

    public EqExpNode(RelExpNode relExpNode, Token operator, EqExpNode eqExpNode) {
        this.relExpNode = relExpNode;
        this.operator = operator;
        this.eqExpNode = eqExpNode;
    }

    public RelExpNode getRelExpNode() {
        return relExpNode;
    }

    public Token getOperator() {
        return operator;
    }

    public EqExpNode getEqExpNode() {
        return eqExpNode;
    }

    public static EqExpNode parseEqExpNode() {
        RelExpNode relExpNode = parseRelExpNode();
        Token operator = null;
        EqExpNode eqExpNode = null;
        if (tokenIterator.getCurrentToken().getTokenType() == TokenType.EQL) {
            operator = tokenIterator.match(TokenType.EQL);
            eqExpNode = parseEqExpNode();
        } else if (tokenIterator.getCurrentToken().getTokenType() == TokenType.NEQ) {
            operator = tokenIterator.match(TokenType.NEQ);
            eqExpNode = parseEqExpNode();
        }
        return new EqExpNode(relExpNode, operator, eqExpNode);
    }

    public void print() {
        relExpNode.print();
        System.out.println(NodeString.get(NodeType.EqExp));
        if (operator != null) {
            System.out.println(operator.print());
            eqExpNode.print();
        }
    }
}
