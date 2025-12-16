package ASTNode;

import Token.*;
import Utils.TokenIterator;

import java.util.ArrayList;

import static ASTNode.ExpNode.parseExpNode;
import static ASTNode.LValNode.parseLValNode;

public class ForStmtNode {
    //ForStmt → LVal '=' Exp { ',' LVal '=' Exp }
    private ArrayList<Token> commas;
    private ArrayList<LValNode> lValNodes;
    private ArrayList<Token> assigns;
    private ArrayList<ExpNode> expNodes;
    private static TokenIterator tokenIterator= TokenIterator.getInstance();

    ForStmtNode(ArrayList<Token> commas,ArrayList<LValNode> lValNodes,ArrayList<Token> assigns,ArrayList<ExpNode> expNodes) {
        this.commas = commas;
        this.lValNodes = lValNodes;
        this.assigns = assigns;
        this.expNodes = expNodes;
    }

    public ArrayList<Token> getCommas() {
        return commas;
    }
    public ArrayList<LValNode> getLValNodes() {
        return lValNodes;
    }
    public ArrayList<Token> getAssigns() {
        return assigns;
    }
    public ArrayList<ExpNode> getExpNodes() {
        return expNodes;
    }

    public static ForStmtNode parseForStmtNode() {

        ArrayList<LValNode> lValNodes = new ArrayList<>();
        ArrayList<Token> assigns = new ArrayList<>();
        ArrayList<ExpNode> expNodes = new ArrayList<>();
        ArrayList<Token> commas = new ArrayList<>();
        LValNode lValNode = parseLValNode();
        lValNodes.add(lValNode);
        Token assign = tokenIterator.match(TokenType.ASSIGN);
        assigns.add(assign);
        ExpNode expNode = parseExpNode();
        expNodes.add(expNode);
        while(tokenIterator.getCurrentToken().getTokenType() == TokenType.COMMA) {
            Token comma = tokenIterator.match(TokenType.COMMA);
            commas.add(comma);
            LValNode lValNode1 = parseLValNode();
            lValNodes.add(lValNode1);
            Token assign1 = tokenIterator.match(TokenType.ASSIGN);
            assigns.add(assign1);
            ExpNode expNode1 = parseExpNode();
            expNodes.add(expNode1);
        }
        return new ForStmtNode(commas, lValNodes, assigns, expNodes);
    }

    public void print() {
        lValNodes.get(0).print();
        System.out.println(assigns.get(0).print());
        expNodes.get(0).print();
        for(int i = 0;i<commas.size();i++) {
            System.out.println(commas.get(i).print());
            lValNodes.get(i+1).print();
            System.out.println(assigns.get(i).print());
            expNodes.get(i+1).print();
        }
        System.out.println(NodeString.get(NodeType.ForStmt));
    }



}
