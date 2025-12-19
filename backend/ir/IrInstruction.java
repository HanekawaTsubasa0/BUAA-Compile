package backend.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IrInstruction {
    public enum Opcode {
        ALLOCA, LOAD, STORE, ADD, SUB, MUL, SDIV, SREM, ICMP, ZEXT, XOR, BR, CALL, RET, GEP, PHI
    }

    private final Opcode opcode;
    private final IrRegister result;
    private final List<IrValue> operands;
    private String text;
    private IrBasicBlock parent;

    public IrInstruction(Opcode opcode, IrRegister result, List<IrValue> operands, String text) {
        this.opcode = opcode;
        this.result = result;
        this.operands = new ArrayList<>(operands);
        this.text = text;
        for (IrValue op : operands) {
            if (op != null) op.addUser(this);
        }
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public IrRegister getResult() {
        return result;
    }

    public List<IrValue> getOperands() {
        return Collections.unmodifiableList(operands);
    }

    public IrValue getOperand(int idx) {
        return operands.get(idx);
    }

    public int getOperandCount() {
        return operands.size();
    }

    public void replaceOperand(int idx, IrValue newVal) {
        IrValue old = operands.get(idx);
        if (old != null) old.removeUser(this);
        operands.set(idx, newVal);
        if (newVal != null) newVal.addUser(this);
    }

    public void detachOperands() {
        for (IrValue op : operands) {
            if (op != null) op.removeUser(this);
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setParent(IrBasicBlock parent) {
        this.parent = parent;
    }

    public IrBasicBlock getParent() {
        return parent;
    }
}
