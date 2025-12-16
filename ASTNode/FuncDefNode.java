package ASTNode;

import Token.*;
import Utils.TokenIterator;

import static ASTNode.BlockNode.parseBlockNode;
import static ASTNode.FuncFParamsNode.parseFuncFParamsNode;
import static ASTNode.FuncTypeNode.parseFuncTypeNode;

public class FuncDefNode {
    // FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block

    private FuncTypeNode funcTypeNode;
    private Token ident;
    private Token leftParentToken;
    private FuncFParamsNode funcFParamsNode;
    private Token rightParentToken;
    private BlockNode blockNode;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();


    public FuncDefNode(FuncTypeNode funcTypeNode, Token ident, Token leftParentToken, FuncFParamsNode funcFParamsNode, Token rightParentToken, BlockNode blockNode) {
        this.funcTypeNode = funcTypeNode;
        this.ident = ident;
        this.leftParentToken = leftParentToken;
        this.funcFParamsNode = funcFParamsNode;
        this.rightParentToken = rightParentToken;
        this.blockNode = blockNode;
    }

    public FuncTypeNode getFuncTypeNode() {
        return funcTypeNode;
    }

    public Token getIdent() {
        return ident;
    }

    public FuncFParamsNode getFuncFParamsNode() {
        return funcFParamsNode;
    }

    public BlockNode getBlockNode() {
        return blockNode;
    }

    public static FuncDefNode parseFuncDefNode() {
        FuncTypeNode funcTypeNode = parseFuncTypeNode();
        Token ident = tokenIterator.match(TokenType.IDENFR);
        Token leftParentToken = tokenIterator.match(TokenType.LPARENT);
        FuncFParamsNode funcParamsNode = null;
        if (tokenIterator.getCurrentToken().getTokenType() == TokenType.INTTK) {
            funcParamsNode = parseFuncFParamsNode();
        }
        Token rightParentToken = tokenIterator.match(TokenType.RPARENT);
        BlockNode blockNode = parseBlockNode();
        return new FuncDefNode(funcTypeNode, ident, leftParentToken, funcParamsNode, rightParentToken, blockNode);
    }

    public void print() {
        funcTypeNode.print();
        System.out.println(ident.print());
        System.out.println(leftParentToken.print());
        if (funcFParamsNode != null) {
            funcFParamsNode.print();
        }
        System.out.println(rightParentToken.print());
        blockNode.print();
        System.out.println(NodeString.get(NodeType.FuncDef));
    }
}
