package ASTNode;

import Token.*;
import Utils.TokenIterator;

import java.util.ArrayList;

import static ASTNode.ExpNode.parseExpNode;

public class FuncRParamsNode {
    // FuncRParams -> Exp { ',' Exp }

    private ArrayList<ExpNode> expNodes;
    private ArrayList<Token> commas;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();


    public FuncRParamsNode(ArrayList<ExpNode> expNodes, ArrayList<Token> commas) {
        this.expNodes = expNodes;
        this.commas = commas;
    }

    public ArrayList<ExpNode> getExpNodes() {
        return expNodes;
    }

    public ArrayList<Token> getCommas() {
        return commas;
    }

    public void print() {
        expNodes.get(0).print();
        for (int i = 1; i < expNodes.size(); i++) {
            System.out.println(commas.get(i - 1).print());
            expNodes.get(i).print();
        }
        System.out.println(NodeString.get(NodeType.FuncRParams));
    }

    public static FuncRParamsNode parseFuncRParamsNode() {
        ArrayList<ExpNode> expNodes = new ArrayList<>();
        ArrayList<Token> commas = new ArrayList<>();
        expNodes.add(parseExpNode());
        while (tokenIterator.getCurrentToken().getTokenType() == TokenType.COMMA) {
            commas.add(tokenIterator.match(TokenType.COMMA));
            expNodes.add(parseExpNode());
        }
        return new FuncRParamsNode(expNodes, commas);
    }


}
