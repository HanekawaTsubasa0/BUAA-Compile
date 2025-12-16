package frontend;

import ASTNode.CompUnitNode;
import Token.Token;
import Utils.TokenIterator;

import java.util.ArrayList;

public class Parser {
    private ArrayList<Token> tokens;
    private int index = 0;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();
    private CompUnitNode compUnitNode;

    public Parser(ArrayList<Token> tokens) {
        this.tokens = tokens;
        tokenIterator.init(tokens);
        parse();
    }

    public void parse() {
        compUnitNode = CompUnitNode.parseCompUnitNode();
    }

    public void print() {
        compUnitNode.print();
    }

    public CompUnitNode getCompUnitNode() {
        return compUnitNode;
    }



}
