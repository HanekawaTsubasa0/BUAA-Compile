package ASTNode;

import Token.*;
import Utils.TokenIterator;

import static ASTNode.EqExpNode.parseEqExpNode;

public class LAndExpNode {
    // LAndExp -> EqExp | LAndExp '&&' EqExp
    private EqExpNode eqExpNode;
    private Token andToken;
    private LAndExpNode lAndExpNode;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();


    public LAndExpNode(EqExpNode eqExpNode, Token operator, LAndExpNode lAndExpNode) {
        this.eqExpNode = eqExpNode;
        this.andToken = operator;
        this.lAndExpNode = lAndExpNode;
    }

    public EqExpNode getEqExpNode() {
        return eqExpNode;
    }

    public Token getAndToken() {
        return andToken;
    }

    public LAndExpNode getLAndExpNode() {
        return lAndExpNode;
    }

    public static LAndExpNode parseLAndExpNode() {
        EqExpNode eqExpNode = parseEqExpNode();
        Token operator = null;
        LAndExpNode lAndExpNode = null;
        if (tokenIterator.getCurrentToken().getTokenType() == TokenType.AND) {
            operator = tokenIterator.match(TokenType.AND);
            lAndExpNode = parseLAndExpNode();
        }
        return new LAndExpNode(eqExpNode, operator, lAndExpNode);
    }

    public void print() {
        eqExpNode.print();
        System.out.println(NodeString.get(NodeType.LAndExp));
        if (andToken != null) {
            System.out.println(andToken.print());
            lAndExpNode.print();
        }
    }
}
