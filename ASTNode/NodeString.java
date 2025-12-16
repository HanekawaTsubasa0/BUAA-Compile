package ASTNode;

import java.util.EnumMap;
import java.util.Map;

public class NodeString {
    private static final Map<NodeType, String> NODE_STRINGS = new EnumMap<>(NodeType.class);

    static {
        NODE_STRINGS.put(NodeType.CompUnit, "<CompUnit>");
        NODE_STRINGS.put(NodeType.Decl, "<Decl>");
        NODE_STRINGS.put(NodeType.ConstDecl, "<ConstDecl>");
        NODE_STRINGS.put(NodeType.BType, "<BType>");
        NODE_STRINGS.put(NodeType.ConstDef, "<ConstDef>");
        NODE_STRINGS.put(NodeType.ConstInitVal, "<ConstInitVal>");
        NODE_STRINGS.put(NodeType.VarDecl, "<VarDecl>");
        NODE_STRINGS.put(NodeType.VarDef, "<VarDef>");
        NODE_STRINGS.put(NodeType.InitVal, "<InitVal>");
        NODE_STRINGS.put(NodeType.FuncDef, "<FuncDef>");
        NODE_STRINGS.put(NodeType.MainFuncDef, "<MainFuncDef>");
        NODE_STRINGS.put(NodeType.FuncType, "<FuncType>");
        NODE_STRINGS.put(NodeType.FuncFParams, "<FuncFParams>");
        NODE_STRINGS.put(NodeType.FuncFParam, "<FuncFParam>");
        NODE_STRINGS.put(NodeType.Block, "<Block>");
        NODE_STRINGS.put(NodeType.BlockItem, "<BlockItem>");
        NODE_STRINGS.put(NodeType.Stmt, "<Stmt>");
        NODE_STRINGS.put(NodeType.ForStmt, "<ForStmt>");
        NODE_STRINGS.put(NodeType.Exp, "<Exp>");
        NODE_STRINGS.put(NodeType.Cond, "<Cond>");
        NODE_STRINGS.put(NodeType.LVal, "<LVal>");
        NODE_STRINGS.put(NodeType.PrimaryExp, "<PrimaryExp>");
        NODE_STRINGS.put(NodeType.Number, "<Number>");
        NODE_STRINGS.put(NodeType.UnaryExp, "<UnaryExp>");
        NODE_STRINGS.put(NodeType.UnaryOp, "<UnaryOp>");
        NODE_STRINGS.put(NodeType.FuncRParams, "<FuncRParams>");
        NODE_STRINGS.put(NodeType.MulExp, "<MulExp>");
        NODE_STRINGS.put(NodeType.AddExp, "<AddExp>");
        NODE_STRINGS.put(NodeType.RelExp, "<RelExp>");
        NODE_STRINGS.put(NodeType.EqExp, "<EqExp>");
        NODE_STRINGS.put(NodeType.LAndExp, "<LAndExp>");
        NODE_STRINGS.put(NodeType.LOrExp, "<LOrExp>");
        NODE_STRINGS.put(NodeType.ConstExp, "<ConstExp>");
    }

    // 根据 NodeType 获取字符串
    public static String get(NodeType type) {
        return NODE_STRINGS.get(type);
    }
}
