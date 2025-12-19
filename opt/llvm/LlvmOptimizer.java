package opt.llvm;

import backend.ir.IrBasicBlock;
import backend.ir.IrConstInt;
import backend.ir.IrFunction;
import backend.ir.IrInstruction;
import backend.ir.IrModule;
import backend.ir.IrRegister;
import backend.ir.IrValue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * LLVM IR 结构化优化：常量折叠 + 未使用结果的指令删除。
 * 仅作用于无副作用、SSA 结果型指令，保证正确性。
 */
public class LlvmOptimizer {
    public IrModule optimize(IrModule module) {
        constantFold(module);
        deadStoreEliminate(module);
        deadResultEliminate(module);
        return module;
    }

    // 兼容旧接口，纯文本路径直接透传
    public String optimize(String ir) {
        return ir;
    }

    private void constantFold(IrModule module) {
        for (IrFunction fn : module.getFunctions()) {
            for (IrBasicBlock bb : fn.getBlocks()) {
                Iterator<IrInstruction> it = bb.getInstructions().iterator();
                while (it.hasNext()) {
                    IrInstruction ins = it.next();
                    if (tryFold(ins)) {
                        ins.detachOperands();
                        it.remove();
                    }
                }
            }
        }
    }

    private boolean tryFold(IrInstruction ins) {
        IrInstruction.Opcode op = ins.getOpcode();
        IrRegister def = ins.getResult();
        if (def == null) return false;
        if (!FOLDABLE.contains(op)) return false;
        if (ins.getOperandCount() < 2) return false;
        IrValue v1 = ins.getOperand(0);
        IrValue v2 = ins.getOperand(1);
        if (!(v1 instanceof IrConstInt) || !(v2 instanceof IrConstInt)) return false;
        IrConstInt c1 = (IrConstInt) v1;
        IrConstInt c2 = (IrConstInt) v2;
        int bits = c1.getType() != null ? c1.getType().getBits() : 32;
        Integer folded = compute(op, c1.getValue(), c2.getValue());
        if (folded == null) return false;
        IrConstInt foldedConst = new IrConstInt(folded, bits);
        replaceAllUses(def, foldedConst);
        return true;
    }

    private Integer compute(IrInstruction.Opcode op, int a, int b) {
        switch (op) {
            case ADD: return a + b;
            case SUB: return a - b;
            case MUL: return a * b;
            case SDIV: return b == 0 ? null : a / b;
            case SREM: return b == 0 ? null : a % b;
            case XOR: return a ^ b;
            default: return null;
        }
    }

    private void replaceAllUses(IrRegister reg, IrValue replacement) {
        List<IrInstruction> users = new ArrayList<>(reg.getUsers());
        for (IrInstruction user : users) {
            for (int i = 0; i < user.getOperandCount(); i++) {
                if (user.getOperand(i) == reg) {
                    user.replaceOperand(i, replacement);
                    user.setText(user.getText().replace(reg.getName(), replacement.getName()));
                }
            }
        }
    }

    private void deadResultEliminate(IrModule module) {
        for (IrFunction fn : module.getFunctions()) {
            for (IrBasicBlock bb : fn.getBlocks()) {
                Iterator<IrInstruction> it = bb.getInstructions().iterator();
                while (it.hasNext()) {
                    IrInstruction ins = it.next();
                    IrRegister def = ins.getResult();
                    if (def == null) continue;
                    if (!REMOVABLE.contains(ins.getOpcode())) continue;
                    if (!def.getUsers().isEmpty()) continue;
                    ins.detachOperands();
                    it.remove();
                }
            }
        }
    }

    // Remove stores (and their allocas) whose pointer is never read/escaped
    private void deadStoreEliminate(IrModule module) {
        for (IrFunction fn : module.getFunctions()) {
            Set<IrInstruction> toRemove = new HashSet<>();
            // identify allocas
            List<IrInstruction> allocas = new ArrayList<>();
            for (IrBasicBlock bb : fn.getBlocks()) {
                for (IrInstruction ins : bb.getInstructions()) {
                    if (ins.getOpcode() == IrInstruction.Opcode.ALLOCA && ins.getResult() != null) {
                        allocas.add(ins);
                    }
                }
            }
            for (IrInstruction alloca : allocas) {
                IrRegister ptr = alloca.getResult();
                if (ptr == null) continue;
                boolean safe = true;
                for (IrInstruction user : new ArrayList<>(ptr.getUsers())) {
                    if (user.getOpcode() != IrInstruction.Opcode.STORE) {
                        safe = false;
                        break;
                    }
                    if (user.getOperandCount() < 2 || user.getOperand(1) != ptr) {
                        safe = false;
                        break;
                    }
                }
                if (safe) {
                    // remove all store users
                    for (IrInstruction user : new ArrayList<>(ptr.getUsers())) {
                        user.detachOperands();
                        toRemove.add(user);
                    }
                    // remove alloca itself
                    alloca.detachOperands();
                    toRemove.add(alloca);
                }
            }
            // sweep removal
            for (IrBasicBlock bb : fn.getBlocks()) {
                Iterator<IrInstruction> it = bb.getInstructions().iterator();
                while (it.hasNext()) {
                    IrInstruction ins = it.next();
                    if (toRemove.contains(ins)) {
                        it.remove();
                    }
                }
            }
        }
    }

    private static final EnumSet<IrInstruction.Opcode> FOLDABLE = EnumSet.of(
            IrInstruction.Opcode.ADD,
            IrInstruction.Opcode.SUB,
            IrInstruction.Opcode.MUL,
            IrInstruction.Opcode.SDIV,
            IrInstruction.Opcode.SREM,
            IrInstruction.Opcode.XOR
    );

    private static final EnumSet<IrInstruction.Opcode> REMOVABLE = EnumSet.of(
            IrInstruction.Opcode.ALLOCA,
            IrInstruction.Opcode.LOAD,
            IrInstruction.Opcode.GEP,
            IrInstruction.Opcode.ADD,
            IrInstruction.Opcode.SUB,
            IrInstruction.Opcode.MUL,
            IrInstruction.Opcode.SDIV,
            IrInstruction.Opcode.SREM,
            IrInstruction.Opcode.ICMP,
            IrInstruction.Opcode.ZEXT,
            IrInstruction.Opcode.XOR,
            IrInstruction.Opcode.PHI
    );
}
