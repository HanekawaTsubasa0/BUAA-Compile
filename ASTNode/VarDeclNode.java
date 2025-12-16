package ASTNode;

import Token.*;
import Utils.TokenIterator;

import java.util.ArrayList;

import static ASTNode.BTypeNode.parseBType;
import static ASTNode.VarDefNode.parseVarDefNode;

public class VarDeclNode {
    // VarDecl -> BType VarDef { ',' VarDef } ';'
    private Token staticToken;
    private Token varToken;
    private DeclNode declNode;
    private StmtNode stmtNode;
    private BTypeNode bTypeNode;
    private ArrayList<VarDefNode> varDefNodes;
    private ArrayList<Token> commas;
    private Token semicn;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();

    public VarDeclNode(Token staticToken,BTypeNode bTypeNode, ArrayList<VarDefNode> varDefNodes, ArrayList<Token> commas, Token semicn) {
        this.staticToken = staticToken;
        this.bTypeNode = bTypeNode;
        this.varDefNodes = varDefNodes;
        this.commas = commas;
        this.semicn = semicn;
    }

    public ArrayList<VarDefNode> getVarDefNodes() {
        return varDefNodes;
    }

    public Token getStaticToken() {
        return staticToken;
    }

    public static VarDeclNode parseVarDeclNode() {
        Token staticToken = null;
        if (tokenIterator.getCurrentToken().getTokenType() == TokenType.STATICTK) {
            staticToken = tokenIterator.match(TokenType.STATICTK);
        }
        BTypeNode bTypeNode = parseBType();
        ArrayList<VarDefNode> varDefNodes = new ArrayList<>();
        ArrayList<Token> commas = new ArrayList<>();
        Token semicnToken;
        varDefNodes.add(parseVarDefNode());
        while (tokenIterator.getCurrentToken().getTokenType() == TokenType.COMMA) {
            commas.add(tokenIterator.match(TokenType.COMMA));
            varDefNodes.add(parseVarDefNode());
        }
        semicnToken = tokenIterator.match(TokenType.SEMICN);
        return new VarDeclNode(staticToken,bTypeNode, varDefNodes, commas, semicnToken);
    }

    public void print() {
        if(staticToken != null) {
            System.out.println(staticToken.print());
        }
        bTypeNode.print();
        varDefNodes.get(0).print();
        for (int i = 1; i < varDefNodes.size(); i++) {
            System.out.println(commas.get(i - 1).print());
            varDefNodes.get(i).print();
        }
        System.out.println(semicn.print());
        System.out.println(NodeString.get(NodeType.VarDecl));
    }
}
