package ASTNode;

import Token.TokenType;
import Utils.TokenIterator;

import java.util.ArrayList;


import static ASTNode.DeclNode.parseDeclNode;
import static ASTNode.FuncDefNode.parseFuncDefNode;
import static ASTNode.MainFuncDefNode.parseMainFuncDefNode;

public class CompUnitNode {
    // CompUnit -> {Decl} {FuncDef} MainFuncDef

    private ArrayList<DeclNode> declNodes;
    private ArrayList<FuncDefNode> funcDefNodes;
    private MainFuncDefNode mainFuncDefNode;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();

    public CompUnitNode(ArrayList<DeclNode> declNodes, ArrayList<FuncDefNode> funcDefNodes, MainFuncDefNode mainFuncDefNode) {
        this.declNodes = declNodes;
        this.funcDefNodes = funcDefNodes;
        this.mainFuncDefNode = mainFuncDefNode;
    }

    public ArrayList<DeclNode> getDeclNodes() {
        return declNodes;
    }

    public ArrayList<FuncDefNode> getFuncDefNodes() {
        return funcDefNodes;
    }

    public MainFuncDefNode getMainFuncDefNode() {
        return mainFuncDefNode;
    }

    public static CompUnitNode parseCompUnitNode() {
        ArrayList<DeclNode> declNodes = new ArrayList<>();
        ArrayList<FuncDefNode> funcDefNodes = new ArrayList<>();
        MainFuncDefNode mainFuncDefNode;
        while(tokenIterator.getNextNToken(1).getTokenType() != TokenType.MAINTK && tokenIterator.getNextNToken(2).getTokenType() != TokenType.LPARENT) {
            DeclNode declNode = parseDeclNode();
            declNodes.add(declNode);
        }
        while(tokenIterator.getNextNToken(1).getTokenType() != TokenType.MAINTK) {
            FuncDefNode funcDefNode = parseFuncDefNode();
            funcDefNodes.add(funcDefNode);
        }
        mainFuncDefNode = parseMainFuncDefNode();
        return new CompUnitNode(declNodes, funcDefNodes, mainFuncDefNode);
    }

    public void print() {
        for (DeclNode declNode : declNodes) {
            declNode.print();
        }
        for (FuncDefNode funcDefNode : funcDefNodes) {
            funcDefNode.print();
        }
        mainFuncDefNode.print();
        System.out.println(NodeString.get(NodeType.CompUnit));
    }
}
