package ASTNode;

import Token.*;
import Utils.TokenIterator;

import java.util.ArrayList;

import static ASTNode.ExpNode.parseExpNode;

public class InitValNode {
    // InitVal → Exp | '{' [ Exp { ',' Exp } ] '}'
    private ArrayList<ExpNode> expNodes;
    private Token leftBraceToken;
    private ArrayList<Token> commas;
    private Token rightBraceToken;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();

    public InitValNode(ArrayList<ExpNode> expNodes, Token leftBraceToken,  ArrayList<Token> commas, Token rightBraceToken) {
        this.expNodes = expNodes;
        this.leftBraceToken = leftBraceToken;
        this.commas = commas;
        this.rightBraceToken = rightBraceToken;
    }

    public ArrayList<ExpNode> getExpNodes() {
        return expNodes;
    }


    public static InitValNode parseInitValNode() {
        ArrayList<ExpNode> expNodes = new ArrayList<>();
        Token leftBraceToken = null;
        ArrayList<Token> commas = new ArrayList<>();
        Token rightBraceToken = null;
        if (tokenIterator.getCurrentToken().getTokenType() == TokenType.LBRACE) {
            leftBraceToken = tokenIterator.match(TokenType.LBRACE);
            if (tokenIterator.getCurrentToken().getTokenType() != TokenType.RBRACE) {
                expNodes.add(parseExpNode());
                while (tokenIterator.getCurrentToken().getTokenType() != TokenType.RBRACE) {
                    commas.add(tokenIterator.match(TokenType.COMMA));
                    expNodes.add(parseExpNode());
                }
            }
            rightBraceToken = tokenIterator.match(TokenType.RBRACE);
        } else {
            expNodes.add(parseExpNode());
        }
        return new InitValNode(expNodes, leftBraceToken,  commas, rightBraceToken);
    }

    public void print() {
        if (leftBraceToken == null) {
            expNodes.get(0).print();
        } else {
            System.out.println(leftBraceToken.print());
            if (!expNodes.isEmpty()) {
                for (int i = 0; i < expNodes.size(); i++) {
                    expNodes.get(i).print();
                    if (i != expNodes.size() - 1) {
                        System.out.println(commas.get(i).print());
                    }
                }
            }
            System.out.println(rightBraceToken.print());
        }
        System.out.println(NodeString.get(NodeType.InitVal));
    }
}
