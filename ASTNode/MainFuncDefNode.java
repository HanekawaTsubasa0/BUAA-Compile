package ASTNode;

import Token.*;
import Utils.TokenIterator;

import static ASTNode.BlockNode.parseBlockNode;

public class MainFuncDefNode {
    // MainFuncDef -> 'int' 'main' '(' ')' Block

    private Token intToken;
    private Token mainToken;
    private Token leftParentToken;
    private Token rightParentToken;
    private BlockNode blockNode;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();

    public MainFuncDefNode(Token intToken, Token mainToken, Token leftParentToken, Token rightParentToken, BlockNode blockNode) {
        this.intToken = intToken;
        this.mainToken = mainToken;
        this.leftParentToken = leftParentToken;
        this.rightParentToken = rightParentToken;
        this.blockNode = blockNode;
    }

    public BlockNode getBlockNode() {
        return blockNode;
    }

    public Token getMainToken() {
        return mainToken;
    }

    public static MainFuncDefNode parseMainFuncDefNode() {
        Token intToken = tokenIterator.match(TokenType.INTTK);
        Token mainToken = tokenIterator.match(TokenType.MAINTK);
        Token leftParentToken = tokenIterator.match(TokenType.LPARENT);
        Token rightParentToken = tokenIterator.match(TokenType.RPARENT);
        BlockNode blockNode = parseBlockNode();
        return new MainFuncDefNode(intToken, mainToken, leftParentToken, rightParentToken, blockNode);

    }

    public void print() {
        System.out.println(intToken.print());
        System.out.println(mainToken.print());
        System.out.println(leftParentToken.print());
        System.out.println(rightParentToken.print());
        blockNode.print();
        System.out.println(NodeString.get(NodeType.MainFuncDef));
    }
}
