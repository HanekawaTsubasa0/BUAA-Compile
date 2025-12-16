package Token;

import Token.TokenType;

import java.util.HashMap;
import java.util.Map;

public class KeywordTable {
    public static final Map<String, TokenType> keywords = new HashMap<>();

    static {
        keywords.put("const", TokenType.CONSTTK);
        keywords.put("int", TokenType.INTTK);
        keywords.put("static", TokenType.STATICTK);
        keywords.put("break", TokenType.BREAKTK);
        keywords.put("continue", TokenType.CONTINUETK);
        keywords.put("if", TokenType.IFTK);
        keywords.put("else", TokenType.ELSETK);
        keywords.put("for", TokenType.FORTK);
        keywords.put("return", TokenType.RETURNTK);
        keywords.put("void", TokenType.VOIDTK);
        keywords.put("main", TokenType.MAINTK);
        keywords.put("printf", TokenType.PRINTFTK);
    }

    public static TokenType getKeywordType(String word) {
        return keywords.getOrDefault(word, TokenType.IDENFR);
    }
}
