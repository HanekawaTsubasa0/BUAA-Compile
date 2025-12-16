package backend;

import semantic.SymbolType;

public class LocalSymbol {
    private final int offset; // offset from $fp
    private final SymbolType type;
    private final boolean isConst;
    private final boolean isStatic;
    private final String label; // for static storage
    private final boolean isParam;

    public LocalSymbol(int offset, SymbolType type, boolean isConst) {
        this(offset, type, isConst, false, null, false);
    }

    public LocalSymbol(int offset, SymbolType type, boolean isConst, boolean isStatic, String label, boolean isParam) {
        this.offset = offset;
        this.type = type;
        this.isConst = isConst;
        this.isStatic = isStatic;
        this.label = label;
        this.isParam = isParam;
    }

    public int getOffset() {
        return offset;
    }

    public SymbolType getType() {
        return type;
    }

    public boolean isConst() {
        return isConst;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public String getLabel() {
        return label;
    }

    public boolean isParam() {
        return isParam;
    }
}
