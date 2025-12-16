package ASTNode;

import Token.*;
import Utils.TokenIterator;

import static ASTNode.LAndExpNode.parseLAndExpNode;

public class LOrExpNode {
    // LOrExp -> LAndExp | LOrExp '||' LAndExp
    private LAndExpNode lAndExpNode;
    private Token orToken;
    private LOrExpNode lOrExpNode;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();


    public LOrExpNode(LAndExpNode lAndExpNode, Token operator, LOrExpNode lOrExpNode) {
        this.lAndExpNode = lAndExpNode;
        this.orToken = operator;
        this.lOrExpNode = lOrExpNode;
    }

    public LAndExpNode getLAndExpNode() {
        return lAndExpNode;
    }

    public Token getOrToken() {
        return orToken;
    }

    public LOrExpNode getLOrExpNode() {
        return lOrExpNode;
    }

    public static LOrExpNode parseLOrExpNode() {
        LAndExpNode lAndExpNode = parseLAndExpNode();
        Token operator = null;
        LOrExpNode lOrExpNode = null;
        if (tokenIterator.getCurrentToken().getTokenType() == TokenType.OR) {
            operator = tokenIterator.match(TokenType.OR);
            lOrExpNode = parseLOrExpNode();
        }
        return new LOrExpNode(lAndExpNode, operator, lOrExpNode);
    }

    public void print() {
        lAndExpNode.print();
        System.out.println(NodeString.get(NodeType.LOrExp));
        if (orToken != null) {
            System.out.println(orToken.print());
            lOrExpNode.print();
        }
    }
}
