package ASTNode;

import Token.*;
import Utils.TokenIterator;

import java.util.ArrayList;

import static ASTNode.BTypeNode.parseBType;
import static ASTNode.ConstDefNode.parseConstDefNode;

public class ConstDeclNode {
    // ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
    private Token constToken;
    private BTypeNode bTypeNode;
    private ArrayList<ConstDefNode> constDefNodes;
    private ArrayList<Token> commas;
    private Token semicnToken;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();

    public ConstDeclNode(Token constToken, BTypeNode bTypeNode, ArrayList<ConstDefNode> constDefNodes, ArrayList<Token> commas, Token semicnToken) {
        this.constToken = constToken;
        this.bTypeNode = bTypeNode;
        this.constDefNodes = constDefNodes;
        this.commas = commas;
        this.semicnToken = semicnToken;
    }

    public Token getConstToken() {
        return constToken;
    }

    public BTypeNode getbTypeNode() {
        return bTypeNode;
    }

    public ArrayList<ConstDefNode> getConstDefNodes() {
        return constDefNodes;
    }

    public ArrayList<Token> getCommas() {
        return commas;
    }

    public Token getSemicnToken() {
        return semicnToken;
    }

    public static ConstDeclNode parseConstDeclNode() {
        Token constToken = tokenIterator.match(TokenType.CONSTTK);
        BTypeNode bTypeNode = parseBType();
        ArrayList<ConstDefNode> constDefNodes = new ArrayList<>();
        ArrayList<Token> commas = new ArrayList<>();
        Token semicnToken ;
        constDefNodes.add(parseConstDefNode());
        while(tokenIterator.getCurrentToken().getTokenType() == TokenType.COMMA) {
            commas.add(tokenIterator.match(TokenType.COMMA));
            constDefNodes.add(parseConstDefNode());
        }
        semicnToken = tokenIterator.match(TokenType.SEMICN);
        return new ConstDeclNode(constToken, bTypeNode, constDefNodes, commas, semicnToken);
    }

    public void print() {
        System.out.println(constToken.print());
        bTypeNode.print();
        constDefNodes.get(0).print();
        for (int i = 1; i < constDefNodes.size(); i++) {
            System.out.println(commas.get(i - 1).print());
            constDefNodes.get(i).print();
        }
        System.out.println(semicnToken.print());
        System.out.println(NodeString.get(NodeType.ConstDecl));
    }
}
