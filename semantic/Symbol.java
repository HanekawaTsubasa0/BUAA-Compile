package semantic;

import Token.Token;

import java.util.List;

public class Symbol {
    private final String name;
    private final SymbolKind kind;
    private final SymbolType type;
    private final List<SymbolType> params; // only for functions
    private final Token defToken; // for line info
    private final boolean isConst;
    private final boolean isStatic;

    public Symbol(String name, SymbolKind kind, SymbolType type, List<SymbolType> params, boolean isConst, boolean isStatic, Token defToken) {
        this.name = name;
        this.kind = kind;
        this.type = type;
        this.params = params;
        this.defToken = defToken;
        this.isConst = isConst;
        this.isStatic = isStatic;
    }

    public String getName() {
        return name;
    }

    public SymbolKind getKind() {
        return kind;
    }

    public SymbolType getType() {
        return type;
    }

    public List<SymbolType> getParams() {
        return params;
    }

    public boolean isConst() {
        return isConst;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public Token getDefToken() {
        return defToken;
    }
}
