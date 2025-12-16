package ASTNode;

import Token.*;
import Utils.TokenIterator;

import static ASTNode.AddExpNode.parseAddExpNode;

public class RelExpNode {
    // RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    private AddExpNode addExpNode;
    private Token operator;
    private RelExpNode relExpNode;
    private static TokenIterator tokenIterator = TokenIterator.getInstance();


    public RelExpNode(AddExpNode addExpNode, Token operator, RelExpNode relExpNode) {
        this.addExpNode = addExpNode;
        this.operator = operator;
        this.relExpNode = relExpNode;
    }

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }

    public Token getOperator() {
        return operator;
    }

    public RelExpNode getRelExpNode() {
        return relExpNode;
    }

    public static RelExpNode parseRelExpNode() {
        AddExpNode addExpNode = parseAddExpNode();
        Token operator = null;
        RelExpNode relExpNode = null;
        if (tokenIterator.getCurrentToken().getTokenType() == TokenType.LSS) {
            operator = tokenIterator.match(TokenType.LSS);
            relExpNode = parseRelExpNode();
        } else if (tokenIterator.getCurrentToken().getTokenType() == TokenType.GRE) {
            operator = tokenIterator.match(TokenType.GRE);
            relExpNode = parseRelExpNode();
        } else if (tokenIterator.getCurrentToken().getTokenType() == TokenType.LEQ) {
            operator = tokenIterator.match(TokenType.LEQ);
            relExpNode = parseRelExpNode();
        } else if (tokenIterator.getCurrentToken().getTokenType() == TokenType.GEQ) {
            operator = tokenIterator.match(TokenType.GEQ);
            relExpNode = parseRelExpNode();
        }
        return new RelExpNode(addExpNode, operator, relExpNode);
    }

    public void print() {
        addExpNode.print();
        System.out.println(NodeString.get(NodeType.RelExp));
        if (operator != null) {
            System.out.println(operator.print());
            relExpNode.print();
        }
    }
}
