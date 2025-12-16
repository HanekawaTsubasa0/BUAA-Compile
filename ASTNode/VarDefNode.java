package ASTNode;

import Token.*;
import Utils.TokenIterator;

import java.util.ArrayList;

import static ASTNode.ConstExpNode.parseConstExpNode;
import static ASTNode.InitValNode.parseInitValNode;

public class VarDefNode {
    // VarDef -> Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
    private Token ident;
    private ArrayList<Token> leftBrackets;
    private ArrayList<ConstExpNode> constExpNodes;
    private ArrayList<Token> rightBrackets;
    private Token assign;
    private InitValNode initValNode;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();

    public VarDefNode(Token ident, ArrayList<Token> leftBrackets, ArrayList<ConstExpNode> constExpNodes, ArrayList<Token> rightBrackets, Token assign, InitValNode initValNode) {
        this.ident = ident;
        this.leftBrackets = leftBrackets;
        this.constExpNodes = constExpNodes;
        this.rightBrackets = rightBrackets;
        this.assign = assign;
        this.initValNode = initValNode;
    }

    public Token getIdent() {
        return ident;
    }

    public ArrayList<ConstExpNode> getConstExpNodes() {
        return constExpNodes;
    }

    public InitValNode getInitValNode() {
        return initValNode;
    }

    public static VarDefNode parseVarDefNode(){
        Token ident = tokenIterator.match(TokenType.IDENFR);
        ArrayList<Token> leftBrackets = new ArrayList<>();
        ArrayList<ConstExpNode> constExpNodes = new ArrayList<>();
        ArrayList<Token> rightBrackets = new ArrayList<>();
        Token equalToken = null;
        InitValNode initValNode = null;
        while (tokenIterator.getCurrentToken().getTokenType() == TokenType.LBRACK) {
            leftBrackets.add(tokenIterator.match(TokenType.LBRACK));
            constExpNodes.add(parseConstExpNode());
            rightBrackets.add(tokenIterator.match(TokenType.RBRACK));
        }
        if (tokenIterator.getCurrentToken().getTokenType() == TokenType.ASSIGN) {
            equalToken = tokenIterator.match(TokenType.ASSIGN);
            initValNode = parseInitValNode();
        }
        return new VarDefNode(ident, leftBrackets, constExpNodes, rightBrackets, equalToken, initValNode);
    }

    public void print() {
        System.out.println(ident.print());
        for (int i = 0; i < leftBrackets.size(); i++) {
            System.out.println(leftBrackets.get(i).print());
            constExpNodes.get(i).print();
            System.out.println(rightBrackets.get(i).print());
        }
        if (initValNode != null) {
            System.out.println(assign.print());
            initValNode.print();
        }
        System.out.println(NodeString.get(NodeType.VarDef));
    }
}
