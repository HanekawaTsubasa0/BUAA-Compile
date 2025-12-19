package backend.ir;

import java.util.ArrayList;
import java.util.List;

public class IrBasicBlock {
    private final String label;
    private final List<IrInstruction> instructions = new ArrayList<>();
    private final List<IrBasicBlock> preds = new ArrayList<>();
    private final List<IrBasicBlock> succs = new ArrayList<>();

    public IrBasicBlock(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void addInstruction(IrInstruction insn) {
        instructions.add(insn);
        insn.setParent(this);
    }

    public List<IrInstruction> getInstructions() {
        return instructions;
    }

    public List<IrBasicBlock> getPreds() {
        return preds;
    }

    public List<IrBasicBlock> getSuccs() {
        return succs;
    }

    public void addPred(IrBasicBlock b) {
        preds.add(b);
    }

    public void addSucc(IrBasicBlock b) {
        succs.add(b);
    }
}
