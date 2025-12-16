package ASTNode;

import Token.*;
import Utils.TokenIterator;

import java.util.ArrayList;

import static ASTNode.BTypeNode.parseBType;
import static ASTNode.ConstExpNode.parseConstExpNode;

public class FuncFParamNode {
    // FuncFParam -> BType Ident [ '[' ']' { '[' ConstExp ']' }]

    private BTypeNode bTypeNode;
    private Token ident;
    private ArrayList<Token> leftBrackets;
    private ArrayList<Token> rightBrackets;
    private ArrayList<ConstExpNode> constExpNodes;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();

    public FuncFParamNode(BTypeNode bTypeNode, Token ident, ArrayList<Token> leftBrackets, ArrayList<Token> rightBrackets, ArrayList<ConstExpNode> constExpNodes) {
        this.bTypeNode = bTypeNode;
        this.ident = ident;
        this.leftBrackets = leftBrackets;
        this.rightBrackets = rightBrackets;
        this.constExpNodes = constExpNodes;
    }

    public BTypeNode getbTypeNode() {
        return bTypeNode;
    }

    public Token getIdent() {
        return ident;
    }

    public ArrayList<Token> getLeftBrackets() {
        return leftBrackets;
    }

    public ArrayList<Token> getRightBrackets() {
        return rightBrackets;
    }

    public ArrayList<ConstExpNode> getConstExpNodes() {
        return constExpNodes;
    }

    public static FuncFParamNode parseFuncFParamNode() {
        BTypeNode bTypeNode = parseBType();
        Token ident = tokenIterator.match(TokenType.IDENFR);
        ArrayList<Token> leftBrackets = new ArrayList<>();
        ArrayList<Token> rightBrackets = new ArrayList<>();
        ArrayList<ConstExpNode> constExpNodes = new ArrayList<>();
        if (tokenIterator.getCurrentToken().getTokenType() == TokenType.LBRACK) {
            leftBrackets.add(tokenIterator.match(TokenType.LBRACK));
            rightBrackets.add(tokenIterator.match(TokenType.RBRACK));
            while (tokenIterator.getCurrentToken().getTokenType() == TokenType.LBRACK) {
                leftBrackets.add(tokenIterator.match(TokenType.LBRACK));
                constExpNodes.add(parseConstExpNode());
                rightBrackets.add(tokenIterator.match(TokenType.RBRACK));
            }
        }
        return new FuncFParamNode(bTypeNode, ident, leftBrackets, rightBrackets, constExpNodes);
    }

    public void print() {
        bTypeNode.print();
        System.out.println(ident.print());
        if (!leftBrackets.isEmpty()) {
            System.out.println(leftBrackets.get(0).print());
            System.out.println(rightBrackets.get(0).print());
            for (int i = 1; i < leftBrackets.size(); i++) {
                System.out.println(leftBrackets.get(i).print());
                constExpNodes.get(i - 1).print();
                System.out.println(rightBrackets.get(i).print());
            }
        }
        System.out.println(NodeString.get(NodeType.FuncFParam));
    }
}
