package semantic;

public class SymbolType {
    public enum BaseType {
        INT,
        VOID
    }

    private final BaseType baseType;
    private final int dimensions; // 0 for scalar, >=1 for array

    public SymbolType(BaseType baseType, int dimensions) {
        this.baseType = baseType;
        this.dimensions = dimensions;
    }

    public BaseType getBaseType() {
        return baseType;
    }

    public int getDimensions() {
        return dimensions;
    }

    public boolean isArray() {
        return dimensions > 0;
    }
}
