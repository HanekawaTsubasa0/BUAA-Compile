package ASTNode;

import Token.*;
import Utils.TokenIterator;

import java.util.ArrayList;

import static ASTNode.FuncFParamNode.parseFuncFParamNode;

public class FuncFParamsNode {
    // FuncFParams -> FuncFParam { ',' FuncFParam }

    private ArrayList<FuncFParamNode> funcFParamNodes;
    private ArrayList<Token> commas;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();

    public FuncFParamsNode(ArrayList<FuncFParamNode> funcFParamNodes, ArrayList<Token> commas) {
        this.funcFParamNodes = funcFParamNodes;
        this.commas = commas;
    }

    public ArrayList<FuncFParamNode> getFuncFParamNodes() {
        return funcFParamNodes;
    }

    public static FuncFParamsNode parseFuncFParamsNode() {
        ArrayList<FuncFParamNode> funcFParamNodes = new ArrayList<>();
        ArrayList<Token> commas = new ArrayList<>();
        funcFParamNodes.add(parseFuncFParamNode());
        while (tokenIterator.getCurrentToken().getTokenType() == TokenType.COMMA) {
            commas.add(tokenIterator.match(TokenType.COMMA));
            funcFParamNodes.add(parseFuncFParamNode());
        }
        return new FuncFParamsNode(funcFParamNodes, commas);
    }

    public void print() {
        funcFParamNodes.get(0).print();
        for (int i = 1; i < funcFParamNodes.size(); i++) {
            System.out.println(commas.get(i - 1).print());
            funcFParamNodes.get(i).print();
        }
        System.out.println(NodeString.get(NodeType.FuncFParams));
    }
}
