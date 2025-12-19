package backend.ir;

public class IrType {
    public enum Kind { INT, VOID }

    private final Kind kind;
    private final int bits; // for INT

    private IrType(Kind kind, int bits) {
        this.kind = kind;
        this.bits = bits;
    }

    public static IrType intType(int bits) {
        return new IrType(Kind.INT, bits);
    }

    public static IrType voidType() {
        return new IrType(Kind.VOID, 0);
    }

    public Kind getKind() {
        return kind;
    }

    public int getBits() {
        return bits;
    }

    public String getDesc() {
        if (kind == Kind.VOID) return "void";
        return "i" + bits;
    }
}
