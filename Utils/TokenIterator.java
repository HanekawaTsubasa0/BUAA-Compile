package Utils;

import Token.*;

import error.*;
import error.Error;

import java.util.ArrayList;

public class TokenIterator {

    private ArrayList<Token> tokens;
    private int index = 0;
    private static TokenIterator instance = new TokenIterator();
    private static Error error = Error.getInstance();
    public static TokenIterator getInstance() {
        return instance;
    }
    public TokenIterator() {

    }

    public void init(ArrayList<Token> tokenArrayList) {
        tokens = tokenArrayList;
    }

    public Token getCurrentToken() {
        return tokens.get(index);
    }

    public Token getTokenByIndex(int index) {
        return tokens.get(index);
    }

    public Token getNextNToken(int n) {
        return tokens.get(index + n);
    }

    public void indexAdd(int n){
        index += n;
    }

    public Token match(TokenType tokenType){
        if(getCurrentToken().getTokenType() == tokenType){
            if(index < tokens.size()){
                index++;
            }
            return tokens.get(index-1);
        }
        else if (tokenType == TokenType.SEMICN) {
            error.addError(tokens.get(index-1).getLine_number(),"i");
            return new Token(TokenType.SEMICN,";",tokens.get(index-1).getLine_number());
        }
        else if (tokenType == TokenType.RPARENT) {
            error.addError(tokens.get(index-1).getLine_number(),"j");
            return new Token(TokenType.RPARENT,")",tokens.get(index-1).getLine_number());
        }
        else if (tokenType == TokenType.RBRACK) {
            error.addError(tokens.get(index-1).getLine_number(),"k");
            return new Token(TokenType.RBRACK,"]",tokens.get(index-1).getLine_number());
        }
        else {
            throw new RuntimeException("Invalid token type :"+index);
        }
    }

    public boolean isExp() {
        return getCurrentToken().getTokenType() == TokenType.IDENFR ||
                getCurrentToken().getTokenType() == TokenType.PLUS ||
                getCurrentToken().getTokenType() == TokenType.MINU ||
                getCurrentToken().getTokenType() == TokenType.NOT ||
                getCurrentToken().getTokenType() == TokenType.LPARENT ||
                getCurrentToken().getTokenType() == TokenType.INTCON;
    }

    public int getIndex() {
        return index;
    }

    public ArrayList<Token> getTokens() {
        return tokens;
    }

}
