package backend.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IrFunction {
    private final String name;
    private final String header;
    private final List<IrBasicBlock> blocks = new ArrayList<>();
    private final Map<String, IrValue> valueMap = new HashMap<>();
    private final Map<String, IrLabel> labelMap = new HashMap<>();
    private final Map<String, IrBasicBlock> blockMap = new HashMap<>();

    public IrFunction(String name, String header) {
        this.name = name;
        this.header = header;
    }

    public String getName() {
        return name;
    }

    public String getHeader() {
        return header;
    }

    public void addBlock(IrBasicBlock block) {
        blocks.add(block);
        labelMap.putIfAbsent(block.getLabel(), new IrLabel(block.getLabel()));
        blockMap.put(block.getLabel(), block);
    }

    public List<IrBasicBlock> getBlocks() {
        return blocks;
    }

    public IrValue getValue(String name) {
        return valueMap.get(name);
    }

    public void putValue(IrValue v) {
        valueMap.put(v.getName(), v);
    }

    public IrLabel getOrCreateLabel(String name) {
        return labelMap.computeIfAbsent(name, IrLabel::new);
    }

    public IrBasicBlock getBlock(String label) {
        return blockMap.get(label);
    }

    public String emit() {
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n");
        for (IrBasicBlock block : blocks) {
            sb.append(block.getLabel()).append(":\n");
            for (IrInstruction ins : block.getInstructions()) {
                sb.append(ins.getText()).append("\n");
            }
        }
        sb.append("}\n\n");
        return sb.toString();
    }
}
