package frontend;

import java.util.ArrayList;

import Token.Token;
import Token.TokenType;
import Token.KeywordTable;
import error.Error;

public class Lexer {
    private String input;
    private int len;
    private int index = 0;
    private ArrayList<Token> tokens =  new ArrayList<>();
    private int line_number = 1;
    private static Error error = Error.getInstance();

    public Lexer(String input) {
        this.input = input;
        this.index = 0;
        len = input.length();
        parseInput();
    }

    public void output() {
        for (Token token : tokens) {
            System.out.println(token.print());
        }
    }

    public ArrayList<Token> getTokens() {
        return tokens;
    }

    private void parseInput() {

        for (; index < len; index++) {
            char ch = input.charAt(index);
            if (Character.isLetter(ch) || ch == '_') {
                parseIdentifier();
            } else if (Character.isDigit(ch)) {
                parseDigitConst();
            } else if (ch == '/') {
                parseComment();
            } else if (ch == '\"') {
                index++;
                parseStrConst();
            } else if (ch == '\n') {
                line_number++;
            } else if (ch == '\t' || ch == '\r' || ch == ' ') {

            } else {

                Token token = parseSymbol();
                if (token != null) {
                    tokens.add(token);
                }
            }
        }
    }

    private void parseIdentifier() {
        StringBuilder identifier = new StringBuilder();
        while (index < len ) {
            char ch = input.charAt(index);
            if (!(Character.isLetterOrDigit(ch)||ch == '_')) {
                index--;
                break;
            }
            identifier.append(ch);
            index++;
        }
        Token token = new Token(KeywordTable.getKeywordType(identifier.toString()), identifier.toString(), line_number);
        tokens.add(token);
    }

    private void parseDigitConst() {
        StringBuilder builder = new StringBuilder();
        while (index < len &&  Character.isDigit(input.charAt(index))) {
            char ch = input.charAt(index);
            builder.append(ch);
            index++;
        }
        Token token = new Token(TokenType.INTCON, builder.toString(), line_number);
        tokens.add(token);
        index--;
    }

    private void parseComment() {

        index++;
        if (input.charAt(index) == '/') {
            while (index < len && input.charAt(index) != '\n') {
                index++;
            }
            line_number++;
        } else if (input.charAt(index) == '*') {
            while (true) {
                if (input.charAt(index) == '\n') {
                    line_number++;
                }
                index++;
                if(index < len && input.charAt(index) == '*'){
                    if(input.charAt(index+1) == '/'){
                        break;
                    }
                }
            }
            index++;
        } else {
            Token token = new Token(TokenType.DIV, "/", line_number);
            tokens.add(token);
            index--;
        }

    }

    private void parseStrConst() {
        StringBuilder ans = new StringBuilder();
        while(index < len&&input.charAt(index) != '\"') {
            ans.append(input.charAt(index));
            index++;
        }
        Token token = new Token(TokenType.STRCON, "\""+ans.toString()+"\"",line_number);
        tokens.add(token);
    }

    private void advance(int count) {
        index += count;
    }

    private char peek(int offset) {
        int pos = index + offset;
        return (pos < input.length()) ? input.charAt(pos) : '\0';
    }

    public Token parseSymbol() {
        char ch = peek(0);

        switch (ch) {
            // === 逻辑运算符 ===
            case '!':
                if (peek(1) == '=') {
                    advance(1);
                    return new Token(TokenType.NEQ, "!=", line_number);
                } else {
                    return new Token(TokenType.NOT, "!", line_number);
                }

            case '&':
                if (peek(1) == '&') {
                    advance(1);
                    return new Token(TokenType.AND, "&&", line_number);
                } else {
                    error.addError(line_number, "a"); // 非法单个 &
                    return new Token(TokenType.AND, "&&", line_number);
                }

            case '|':
                if (peek(1) == '|') {
                    advance(1);
                    return new Token(TokenType.OR, "||", line_number);
                } else {
                    error.addError(line_number, "a"); // 非法单个 |
                    return new Token(TokenType.OR, "||", line_number);
                }

                // === 赋值、比较 ===
            case '=':
                if (peek(1) == '=') {
                    advance(1);
                    return new Token(TokenType.EQL, "==", line_number);
                } else {
                    return new Token(TokenType.ASSIGN, "=", line_number);
                }

            case '<':
                if (peek(1) == '=') {
                    advance(1);
                    return new Token(TokenType.LEQ, "<=", line_number);
                } else {
                    return new Token(TokenType.LSS, "<", line_number);
                }

            case '>':
                if (peek(1) == '=') {
                    advance(1);
                    return new Token(TokenType.GEQ, ">=", line_number);
                } else {
                    return new Token(TokenType.GRE, ">", line_number);
                }

                // === 算术运算符 ===
            case '+':
                return new Token(TokenType.PLUS, "+", line_number);
            case '-':
                return new Token(TokenType.MINU, "-", line_number);
            case '*':
                return new Token(TokenType.MULT, "*", line_number);
//            case '/':
//                return new Token(TokenType.DIV, "/", line_number);
            case '%':
                return new Token(TokenType.MOD, "%", line_number);

            // === 分隔符 ===
            case ';':
                return new Token(TokenType.SEMICN, ";", line_number);
            case ',':
                return new Token(TokenType.COMMA, ",", line_number);
            case '(':
                return new Token(TokenType.LPARENT, "(", line_number);
            case ')':
                return new Token(TokenType.RPARENT, ")", line_number);
            case '[':
                return new Token(TokenType.LBRACK, "[", line_number);
            case ']':
                return new Token(TokenType.RBRACK, "]", line_number);
            case '{':
                return new Token(TokenType.LBRACE, "{", line_number);
            case '}':
                return new Token(TokenType.RBRACE, "}", line_number);

            default:
                return null; // 非符号，由上层判断
        }
    }


}
