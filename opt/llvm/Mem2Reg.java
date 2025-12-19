package opt.llvm;

import backend.ir.IrBasicBlock;
import backend.ir.IrFunction;
import backend.ir.IrInstruction;
import backend.ir.IrModule;
import backend.ir.IrRegister;
import backend.ir.IrValue;

import java.util.*;

/**
 * 极简 mem2reg：将局部标量的 alloca + {load/store} 提升为 SSA 寄存器。
 * 仅处理函数内局部标量，跳过数组和可能逃逸的指针。
 * 不生成 phi（仅在单前驱链路下安全重命名），用于保守优化。
 */
public class Mem2Reg {
    public IrModule run(IrModule module) {
        for (IrFunction fn : module.getFunctions()) {
            promoteFunction(fn);
        }
        return module;
    }

    private void promoteFunction(IrFunction fn) {
        // 收集可提升的 alloca
        List<IrInstruction> allocas = new ArrayList<>();
        for (IrBasicBlock bb : fn.getBlocks()) {
            for (IrInstruction ins : bb.getInstructions()) {
                if (ins.getOpcode() == IrInstruction.Opcode.ALLOCA && ins.getResult() != null) {
                    allocas.add(ins);
                }
            }
        }
        // 仅处理“仅在单一基本块内，且不被取址”的简单模式
        Map<IrRegister, List<IrInstruction>> uses = new HashMap<>();
        for (IrInstruction alloca : allocas) {
            IrRegister ptr = alloca.getResult();
            boolean safe = true;
            List<IrInstruction> userList = new ArrayList<>(ptr.getUsers());
            for (IrInstruction user : userList) {
                switch (user.getOpcode()) {
                    case STORE:
                        // store val, ptr
                        if (user.getOperandCount() < 2 || user.getOperand(1) != ptr) { safe = false; }
                        break;
                    case LOAD:
                        // load from ptr
                        if (user.getOperandCount() < 1 || user.getOperand(0) != ptr) { safe = false; }
                        break;
                    default:
                        safe = false;
                }
                if (!safe) break;
            }
            if (safe) {
                uses.put(ptr, userList);
            }
        }
        if (uses.isEmpty()) return;

        // 重命名：对每个安全的 alloca，用最近的 store 值替换后续 load
        for (IrBasicBlock bb : fn.getBlocks()) {
            List<IrInstruction> insns = bb.getInstructions();
            Map<IrRegister, IrValue> curVal = new HashMap<>();
            Iterator<IrInstruction> it = insns.iterator();
            while (it.hasNext()) {
                IrInstruction ins = it.next();
                // store val, ptr
                if (ins.getOpcode() == IrInstruction.Opcode.STORE) {
                    IrValue ptr = ins.getOperandCount() >= 2 ? ins.getOperand(1) : null;
                    if (ptr instanceof IrRegister && uses.containsKey(ptr)) {
                        IrValue val = ins.getOperand(0);
                        curVal.put((IrRegister) ptr, val);
                        ins.detachOperands();
                        it.remove();
                        continue;
                    }
                }
                // load ptr
                if (ins.getOpcode() == IrInstruction.Opcode.LOAD) {
                    IrValue ptr = ins.getOperandCount() >= 1 ? ins.getOperand(0) : null;
                    if (ptr instanceof IrRegister && uses.containsKey(ptr)) {
                        IrValue val = curVal.get(ptr);
                        if (val != null && ins.getResult() != null) {
                            replaceAllUses(ins.getResult(), val);
                            ins.detachOperands();
                            it.remove();
                            continue;
                        }
                    }
                }
                // alloca 删除
                if (ins.getOpcode() == IrInstruction.Opcode.ALLOCA && uses.containsKey(ins.getResult())) {
                    ins.detachOperands();
                    it.remove();
                }
            }
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
}
