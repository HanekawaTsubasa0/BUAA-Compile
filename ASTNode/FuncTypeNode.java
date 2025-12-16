package ASTNode;

import Token.*;
import Utils.TokenIterator;

public class FuncTypeNode {
    // FuncType -> 'void' | 'int'

    private Token token;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();


    public FuncTypeNode(Token token) {
        this.token = token;
    }

    public Token getToken() {
        return token;
    }

    public void print() {
        System.out.println(token.print());
        System.out.println(NodeString.get(NodeType.FuncType));
    }

    public static FuncTypeNode parseFuncTypeNode() {
        if(tokenIterator.getCurrentToken().getTokenType() == TokenType.VOIDTK){
           Token voidToken = tokenIterator.match(TokenType.VOIDTK);
           return new FuncTypeNode(voidToken);
        }
        else {
            Token intToken = tokenIterator.match(TokenType.INTTK);
            return new FuncTypeNode(intToken);
        }
    }


}
