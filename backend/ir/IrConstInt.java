package backend.ir;

public class IrConstInt extends IrValue {
    private final int value;

    public IrConstInt(int value, int bits) {
        super(Integer.toString(value), IrType.intType(bits));
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
