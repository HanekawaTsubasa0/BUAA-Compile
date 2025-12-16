package ASTNode;

import Token.*;
import Utils.TokenIterator;

public class BTypeNode {
    private Token token;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();
    //BType → 'int'
    public BTypeNode(Token token) {
        this.token = token;
    }

    public static BTypeNode parseBType() {
        Token bTypeToken = tokenIterator.match(TokenType.INTTK);
        return new BTypeNode(bTypeToken);
    }

    public void print() {
        System.out.println(token.print());
    }
}
