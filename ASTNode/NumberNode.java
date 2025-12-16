package ASTNode;

import Token.*;
import Utils.TokenIterator;

public class NumberNode {
    // Number -> IntConst

    Token token;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();

    public NumberNode(Token token) {
        this.token = token;
    }

    public Token getToken() {
        return token;
    }

    public static NumberNode parseNumberNode() {
        return new NumberNode(tokenIterator.match(TokenType.INTCON));
    }

    public void print() {
        System.out.println(token.print());
        System.out.println(NodeString.get(NodeType.Number));
    }

    public String getStr() {
        return token.getValue();
    }
}
