package ASTNode;

import Token.*;
import Utils.TokenIterator;

public class UnaryOpNode {
    // UnaryOp -> '+' | '−' | '!'

    Token token;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();


    public UnaryOpNode(Token token) {
        this.token = token;
    }

    public Token getToken() {
        return token;
    }
    

    public static UnaryOpNode parseUnaryOpNode() {
        Token token;
        if (tokenIterator.getCurrentToken().getTokenType() == TokenType.PLUS) {
            token = tokenIterator.match(TokenType.PLUS);
        } else if (tokenIterator.getCurrentToken().getTokenType() == TokenType.MINU) {
            token = tokenIterator.match(TokenType.MINU);
        } else {
            token = tokenIterator.match(TokenType.NOT);
        }
        return new UnaryOpNode(token);
    }
    public void print() {
        System.out.println(token.print());
        System.out.println(NodeString.get(NodeType.UnaryOp));
    }

    public String getStr() {
        return token.getValue();
    }
}
