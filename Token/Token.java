package Token;
import Token.TokenType;

public class Token {
    private TokenType tokenType;
    private String value;
    private int line_number;

    public Token(TokenType tokenType, String value, int line_number) {
        this.tokenType = tokenType;
        this.value = value;
        this.line_number = line_number;
    }
    public Token(TokenType tokenType, String value) {
        this.tokenType = tokenType;
        this.value = value;
    }

    public String print() {
        return tokenType.toString() + " " + value;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }
    public void setValue(String value) {
        this.value = value;
    }
    public void setLine_number(int line_number) {
        this.line_number = line_number;
    }
    public TokenType getTokenType() {
        return tokenType;
    }
    public String getValue() {
        return value;
    }
    public int getLine_number() {
        return line_number;
    }

}
