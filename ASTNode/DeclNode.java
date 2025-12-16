package ASTNode;

import Token.TokenType;
import Utils.TokenIterator;
import org.w3c.dom.traversal.NodeIterator;

import static ASTNode.ConstDeclNode.parseConstDeclNode;
import static ASTNode.VarDeclNode.parseVarDeclNode;

public class DeclNode {
    // Decl -> ConstDecl | VarDecl
    private ConstDeclNode constDecl;
    private VarDeclNode varDecl;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();

    public DeclNode(ConstDeclNode constDecl, VarDeclNode varDecl) {
        this.constDecl = constDecl;
        this.varDecl = varDecl;
    }

    public ConstDeclNode getConstDecl() {
        return constDecl;
    }

    public VarDeclNode getVarDecl() {
        return varDecl;
    }

    public static DeclNode parseDeclNode() {
        ConstDeclNode constDeclNode = null;
        VarDeclNode varDeclNode = null;
        if(tokenIterator.getCurrentToken().getTokenType() == TokenType.CONSTTK) {
            constDeclNode = parseConstDeclNode();
        }
        else {
            varDeclNode = parseVarDeclNode();
        }
        return new DeclNode(constDeclNode, varDeclNode);
    }

    public void print() {
        if (constDecl != null) {
            constDecl.print();
        } else {
            varDecl.print();
        }
    }
}
