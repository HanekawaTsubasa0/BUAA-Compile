package ASTNode;

import Token.*;
import Utils.TokenIterator;

import java.util.ArrayList;

import static ASTNode.ConstExpNode.parseConstExpNode;

public class ConstInitValNode {
    // ConstInitVal → ConstExp  | '{' [ ConstExp { ',' ConstExp } ] '}'
    private ArrayList<ConstExpNode> constExpNodes;
    private Token leftBraceToken;
    private ArrayList<Token> commas;
    private Token rightBraceToken;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();

    public ConstInitValNode(ArrayList<ConstExpNode> constExpNodes, Token leftBraceToken, ArrayList<Token> commas, Token rightBraceToken) {
        this.constExpNodes = constExpNodes;
        this.leftBraceToken = leftBraceToken;
        this.commas = commas;
        this.rightBraceToken = rightBraceToken;
    }

    public ArrayList<ConstExpNode> getConstExpNodes() {
        return constExpNodes;
    }

    public Token getLeftBraceToken() {
        return leftBraceToken;
    }


    public ArrayList<Token> getCommas() {
        return commas;
    }

    public Token getRightBraceToken() {
        return rightBraceToken;
    }

    public static ConstInitValNode parseConstInitValNode() {
        ArrayList<ConstExpNode> constExpNodes = new ArrayList<>();
        Token leftBraceToken = null;
        ArrayList<Token> commas = new ArrayList<Token>();
        Token rightBraceToken = null;
        if(tokenIterator.getCurrentToken().getTokenType() == TokenType.LBRACE){
            leftBraceToken = tokenIterator.match(TokenType.LBRACE);
            if(tokenIterator.getCurrentToken().getTokenType() != TokenType.RBRACE){
                constExpNodes.add(parseConstExpNode());
                while(tokenIterator.getCurrentToken().getTokenType() != TokenType.RBRACE){
                    commas.add(tokenIterator.match(TokenType.COMMA));
                    constExpNodes.add(parseConstExpNode());
                }
            }
            rightBraceToken = tokenIterator.match(TokenType.RBRACE);
        }
        else{
            constExpNodes.add(parseConstExpNode());
        }
        return new ConstInitValNode(constExpNodes, leftBraceToken,  commas, rightBraceToken);
    }

    public void print() {
        if (leftBraceToken == null) {
            constExpNodes.get(0).print();
        } else {
            System.out.println(leftBraceToken.print());
            if (!constExpNodes.isEmpty()) {
                constExpNodes.get(0).print();
                for (int i = 1; i < constExpNodes.size(); i++) {
                    System.out.println(commas.get(i - 1).print());
                    constExpNodes.get(i).print();
                }
            }
            System.out.println(rightBraceToken.print());
        }
        System.out.println(NodeString.get(NodeType.ConstInitVal));
    }
}
