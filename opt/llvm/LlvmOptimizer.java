package opt.llvm;

import backend.ir.IrBasicBlock;
import backend.ir.IrConstInt;
import backend.ir.IrFunction;
import backend.ir.IrInstruction;
import backend.ir.IrModule;
import backend.ir.IrRegister;
import backend.ir.IrValue;
import backend.ir.IrGlobalRef;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LLVM IR 结构化优化：常量折叠、代数化简、局部 CSE、分支简化、死代码清理等。
 * 保持正确性为先，仅作用于无副作用指令。
 */
public class LlvmOptimizer {
    public IrModule optimize(IrModule module) {
        constantFold(module);
        algebraicSimplify(module);
        localCse(module);
        branchSimplify(module);
        trimAfterTerminator(module);
        mergeStraightLineBlocks(module);
        forwardLoadFromStore(module);
        killOverwrittenStores(module);
        deadStoreEliminate(module);
        deadResultEliminate(module);
        removeUnreachableBlocks(module);
        rebuildValueUsers(module);
        simplifyFixpoint(module, 2);
        rebuildValueUsers(module);
        return module;
    }

    // 兼容旧接口
    public String optimize(String ir) {
        return ir;
    }

    /* ---------- use-list rebuild ---------- */
    private void rebuildValueUsers(IrModule module) {
        for (IrFunction fn : module.getFunctions()) {
            for (IrValue v : new ArrayList<>(fnValues(fn))) {
                if (v != null) {
                    v.getUsers().clear();
                }
            }
            for (IrBasicBlock bb : fn.getBlocks()) {
                for (IrInstruction ins : bb.getInstructions()) {
                    for (int i = 0; i < ins.getOperandCount(); i++) {
                        IrValue op = ins.getOperand(i);
                        if (op != null) op.addUser(ins);
                    }
                }
            }
        }
    }

    private List<IrValue> fnValues(IrFunction fn) {
        List<IrValue> vals = new ArrayList<>();
        for (IrBasicBlock bb : fn.getBlocks()) {
            for (IrInstruction ins : bb.getInstructions()) {
                if (ins.getResult() != null) {
                    vals.add(ins.getResult());
                }
                for (int i = 0; i < ins.getOperandCount(); i++) {
                    IrValue op = ins.getOperand(i);
                    if (op != null) vals.add(op);
                }
            }
        }
        return vals;
    }

    /* ---------- algebraic simplification ---------- */
    private void algebraicSimplify(IrModule module) {
        for (IrFunction fn : module.getFunctions()) {
            for (IrBasicBlock bb : fn.getBlocks()) {
                Iterator<IrInstruction> it = bb.getInstructions().iterator();
                while (it.hasNext()) {
                    IrInstruction ins = it.next();
                    if (trySimplify(ins)) {
                        ins.detachOperands();
                        it.remove();
                    }
                }
            }
        }
    }

    private boolean trySimplify(IrInstruction ins) {
        IrInstruction.Opcode op = ins.getOpcode();
        IrRegister def = ins.getResult();
        if (def == null) return false;
        if (!ALGEBRA_OPS.contains(op)) return false;
        if (ins.getOperandCount() < 2) return false;
        IrValue a = ins.getOperand(0);
        IrValue b = ins.getOperand(1);

        java.util.function.Function<IrValue, Boolean> repl = v -> { replaceAllUses(def, v); return true; };

        switch (op) {
            case ICMP: {
                String pred = icmpPred(ins);
                if (pred != null && a == b) {
                    int bits = def.getType() != null ? def.getType().getBits() : 1;
                    int val = ("eq".equals(pred) || "sle".equals(pred) || "sge".equals(pred)) ? 1 : 0;
                    return repl.apply(new IrConstInt(val, bits));
                }
                break;
            }
            case ADD:
                if (isZero(b)) return repl.apply(a);
                if (isZero(a)) return repl.apply(b);
                break;
            case SUB:
                if (isZero(b)) return repl.apply(a);
                if (a == b) return repl.apply(new IrConstInt(0, 32));
                break;
            case MUL:
                if (isOne(a)) return repl.apply(b);
                if (isOne(b)) return repl.apply(a);
                if (isZero(a) || isZero(b)) return repl.apply(new IrConstInt(0, 32));
                break;
            case SDIV:
                if (isOne(b)) return repl.apply(a);
                break;
            case SREM:
                if (isOne(b)) return repl.apply(new IrConstInt(0, 32));
                break;
            case XOR:
                if (isZero(a)) return repl.apply(b);
                if (isZero(b)) return repl.apply(a);
                if (a == b) return repl.apply(new IrConstInt(0, 32));
                break;
            default:
                break;
        }
        return false;
    }

    private boolean isZero(IrValue v) {
        return v instanceof IrConstInt && ((IrConstInt) v).getValue() == 0;
    }

    private boolean isOne(IrValue v) {
        return v instanceof IrConstInt && ((IrConstInt) v).getValue() == 1;
    }

    /* ---------- constant folding ---------- */
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

        if (op == IrInstruction.Opcode.ZEXT && ins.getOperandCount() == 1) {
            IrValue v = ins.getOperand(0);
            if (!(v instanceof IrConstInt)) return false;
            int bits = def.getType() != null ? def.getType().getBits() : 32;
            IrConstInt foldedConst = new IrConstInt(((IrConstInt) v).getValue(), bits);
            replaceAllUses(def, foldedConst);
            return true;
        }

        if (ins.getOperandCount() < 2) return false;
        IrValue v1 = ins.getOperand(0);
        IrValue v2 = ins.getOperand(1);
        if (!(v1 instanceof IrConstInt) || !(v2 instanceof IrConstInt)) return false;
        IrConstInt c1 = (IrConstInt) v1;
        IrConstInt c2 = (IrConstInt) v2;
        int bits = c1.getType() != null ? c1.getType().getBits() : 32;
        Integer folded = (op == IrInstruction.Opcode.ICMP)
                ? computeIcmp(ins, c1.getValue(), c2.getValue())
                : compute(op, c1.getValue(), c2.getValue());
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

    private Integer computeIcmp(IrInstruction ins, int a, int b) {
        String pred = icmpPred(ins);
        if (pred == null) return null;
        switch (pred) {
            case "eq": return a == b ? 1 : 0;
            case "ne": return a != b ? 1 : 0;
            case "slt": return a < b ? 1 : 0;
            case "sgt": return a > b ? 1 : 0;
            case "sle": return a <= b ? 1 : 0;
            case "sge": return a >= b ? 1 : 0;
            default: return null;
        }
    }

    private String icmpPred(IrInstruction ins) {
        String txt = ins.getText();
        if (txt == null) return null;
        int idx = txt.indexOf("icmp ");
        if (idx < 0) return null;
        int start = idx + 5;
        int end = txt.indexOf(' ', start);
        if (end < 0) return null;
        return txt.substring(start, end).trim();
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

    /* ---------- simple in-block store->load forwarding for stack slots ---------- */
    private void forwardLoadFromStore(IrModule module) {
        for (IrFunction fn : module.getFunctions()) {
            Set<IrRegister> promotable = new HashSet<>();
            for (IrBasicBlock bb : fn.getBlocks()) {
                for (IrInstruction ins : bb.getInstructions()) {
                    if (ins.getOpcode() == IrInstruction.Opcode.ALLOCA && ins.getResult() != null) {
                        IrRegister ptr = ins.getResult();
                        if (isPromotableAlloca(ptr)) {
                            promotable.add(ptr);
                        }
                    }
                }
            }
            if (promotable.isEmpty()) continue;

            for (IrBasicBlock bb : fn.getBlocks()) {
                Map<IrRegister, IrValue> curVal = new HashMap<>();
                Map<IrRegister, IrRegister> lastLoad = new HashMap<>();
                Iterator<IrInstruction> it = bb.getInstructions().iterator();
                while (it.hasNext()) {
                    IrInstruction ins = it.next();
                    if (ins.getOpcode() == IrInstruction.Opcode.STORE) {
                        IrValue ptr = ins.getOperandCount() >= 2 ? ins.getOperand(1) : null;
                        if (ptr instanceof IrRegister && promotable.contains(ptr)) {
                            IrValue val = ins.getOperand(0);
                            curVal.put((IrRegister) ptr, val);
                            lastLoad.remove(ptr);
                        }
                    } else if (ins.getOpcode() == IrInstruction.Opcode.LOAD) {
                        IrValue ptr = ins.getOperandCount() >= 1 ? ins.getOperand(0) : null;
                        if (ptr instanceof IrRegister && promotable.contains(ptr)) {
                            IrValue known = curVal.get(ptr);
                            if (known != null && ins.getResult() != null) {
                                replaceAllUses(ins.getResult(), known);
                                ins.detachOperands();
                                it.remove();
                                continue;
                            }
                            IrRegister cached = lastLoad.get(ptr);
                            if (cached != null && ins.getResult() != null) {
                                replaceAllUses(ins.getResult(), cached);
                                ins.detachOperands();
                                it.remove();
                                continue;
                            }
                            if (ins.getResult() instanceof IrRegister) {
                                lastLoad.put((IrRegister) ptr, (IrRegister) ins.getResult());
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isPromotableAlloca(IrRegister ptr) {
        for (IrInstruction user : new ArrayList<>(ptr.getUsers())) {
            switch (user.getOpcode()) {
                case STORE:
                    if (user.getOperandCount() < 2 || user.getOperand(1) != ptr) return false;
                    break;
                case LOAD:
                    if (user.getOperandCount() < 1 || user.getOperand(0) != ptr) return false;
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    /* ---------- eliminate stores overwritten before any load (per basic block) ---------- */
    private void killOverwrittenStores(IrModule module) {
        for (IrFunction fn : module.getFunctions()) {
            Set<IrRegister> promotable = new HashSet<>();
            for (IrBasicBlock bb : fn.getBlocks()) {
                for (IrInstruction ins : bb.getInstructions()) {
                    if (ins.getOpcode() == IrInstruction.Opcode.ALLOCA && ins.getResult() != null) {
                        IrRegister ptr = ins.getResult();
                        if (isPromotableAlloca(ptr)) {
                            promotable.add(ptr);
                        }
                    }
                }
            }
            if (promotable.isEmpty()) continue;

            for (IrBasicBlock bb : fn.getBlocks()) {
                Map<IrRegister, IrInstruction> lastStore = new HashMap<>();
                Set<IrInstruction> toRemove = new HashSet<>();
                for (IrInstruction ins : bb.getInstructions()) {
                    if (ins.getOpcode() == IrInstruction.Opcode.STORE) {
                        IrValue ptr = ins.getOperandCount() >= 2 ? ins.getOperand(1) : null;
                        if (ptr instanceof IrRegister && promotable.contains(ptr)) {
                            IrInstruction prev = lastStore.get(ptr);
                            if (prev != null) {
                                toRemove.add(prev);
                            }
                            lastStore.put((IrRegister) ptr, ins);
                        }
                    } else if (ins.getOpcode() == IrInstruction.Opcode.LOAD) {
                        IrValue ptr = ins.getOperandCount() >= 1 ? ins.getOperand(0) : null;
                        if (ptr instanceof IrRegister && promotable.contains(ptr)) {
                            lastStore.remove(ptr);
                        }
                    } else if (ins.getOpcode() == IrInstruction.Opcode.CALL) {
                        lastStore.clear();
                    }
                }
                if (!toRemove.isEmpty()) {
                    Iterator<IrInstruction> it = bb.getInstructions().iterator();
                    while (it.hasNext()) {
                        IrInstruction ins = it.next();
                        if (toRemove.contains(ins)) {
                            ins.detachOperands();
                            it.remove();
                        }
                    }
                }
            }
        }
    }

    /* ---------- branch simplification ---------- */
    private void branchSimplify(IrModule module) {
        for (IrFunction fn : module.getFunctions()) {
            for (IrBasicBlock bb : fn.getBlocks()) {
                List<IrInstruction> insns = bb.getInstructions();
                if (insns.isEmpty()) continue;
                IrInstruction last = insns.get(insns.size() - 1);
                if (last.getOpcode() != IrInstruction.Opcode.BR) continue;
                if (last.getOperandCount() == 3) {
                    IrValue cond = last.getOperand(0);
                    IrValue trueT = last.getOperand(1);
                    IrValue falseT = last.getOperand(2);
                    String tName = trueT instanceof backend.ir.IrLabel ? ((backend.ir.IrLabel) trueT).getName() : trueT.getName();
                    String fName = falseT instanceof backend.ir.IrLabel ? ((backend.ir.IrLabel) falseT).getName() : falseT.getName();
                    if (tName.equals(fName)) {
                        List<IrValue> newOps = new ArrayList<>();
                        newOps.add(trueT);
                        last.detachOperands();
                        insns.remove(insns.size() - 1);
                        IrInstruction simplified = new IrInstruction(IrInstruction.Opcode.BR, null, newOps,
                                "  br label %" + tName);
                        bb.addInstruction(simplified);
                        continue;
                    }
                    if (cond instanceof IrConstInt) {
                        boolean takeTrue = ((IrConstInt) cond).getValue() != 0;
                        IrValue target = takeTrue ? trueT : falseT;
                        List<IrValue> newOps = new ArrayList<>();
                        newOps.add(target);
                        String targetName = target instanceof backend.ir.IrLabel
                                ? ((backend.ir.IrLabel) target).getName()
                                : target.getName();
                        last.detachOperands();
                        insns.remove(insns.size() - 1);
                        IrInstruction simplified = new IrInstruction(IrInstruction.Opcode.BR, null, newOps,
                                "  br label %" + targetName);
                        bb.addInstruction(simplified);
                    }
                }
            }
        }
    }

    /* ---------- trim after terminator ---------- */
    private void trimAfterTerminator(IrModule module) {
        for (IrFunction fn : module.getFunctions()) {
            for (IrBasicBlock bb : fn.getBlocks()) {
                boolean terminated = false;
                Iterator<IrInstruction> it = bb.getInstructions().iterator();
                while (it.hasNext()) {
                    IrInstruction ins = it.next();
                    if (terminated) {
                        ins.detachOperands();
                        it.remove();
                        continue;
                    }
                    if (ins.getOpcode() == IrInstruction.Opcode.BR || ins.getOpcode() == IrInstruction.Opcode.RET) {
                        terminated = true;
                    }
                }
            }
        }
    }

    /* ---------- merge straight-line blocks ---------- */
    private void mergeStraightLineBlocks(IrModule module) {
        for (IrFunction fn : module.getFunctions()) {
            boolean changed = true;
            while (changed) {
                changed = false;
                List<IrBasicBlock> blocks = fn.getBlocks();
                Map<String, IrBasicBlock> labelMap = new HashMap<>();
                for (IrBasicBlock b : blocks) {
                    labelMap.put(b.getLabel(), b);
                }
                Map<IrBasicBlock, Integer> predCount = computePredCount(blocks, labelMap);
                for (int i = 1; i < blocks.size(); i++) {
                    IrBasicBlock b = blocks.get(i);
                    if (hasPhi(b)) continue;
                    if (predCount.getOrDefault(b, 0) != 1) continue;
                    IrBasicBlock pred = findUniquePred(blocks, b.getLabel());
                    if (pred == null || pred == b) continue;
                    List<IrInstruction> pins = pred.getInstructions();
                    if (pins.isEmpty()) continue;
                    IrInstruction term = pins.get(pins.size() - 1);
                    if (term.getOpcode() != IrInstruction.Opcode.BR || term.getOperandCount() != 1) continue;
                    pins.remove(pins.size() - 1);
                    for (IrInstruction ins : new ArrayList<>(b.getInstructions())) {
                        pred.addInstruction(ins);
                    }
                    b.getInstructions().clear();
                    blocks.remove(i);
                    changed = true;
                    break;
                }
            }
        }
    }

    private Map<IrBasicBlock, Integer> computePredCount(List<IrBasicBlock> blocks, Map<String, IrBasicBlock> labelMap) {
        Map<IrBasicBlock, Integer> predCount = new HashMap<>();
        for (IrBasicBlock bb : blocks) {
            if (bb.getInstructions().isEmpty()) continue;
            IrInstruction last = bb.getInstructions().get(bb.getInstructions().size() - 1);
            if (last.getOpcode() != IrInstruction.Opcode.BR) continue;
            List<IrValue> ops = last.getOperands();
            if (ops.size() == 1) {
                IrBasicBlock succ = labelMap.get(ops.get(0).getName());
                if (succ != null) predCount.merge(succ, 1, Integer::sum);
            } else if (ops.size() == 3) {
                IrBasicBlock succT = labelMap.get(ops.get(1).getName());
                IrBasicBlock succF = labelMap.get(ops.get(2).getName());
                if (succT != null) predCount.merge(succT, 1, Integer::sum);
                if (succF != null) predCount.merge(succF, 1, Integer::sum);
            }
        }
        return predCount;
    }

    private IrBasicBlock findUniquePred(List<IrBasicBlock> blocks, String targetLabel) {
        IrBasicBlock pred = null;
        for (IrBasicBlock bb : blocks) {
            if (bb.getInstructions().isEmpty()) continue;
            IrInstruction last = bb.getInstructions().get(bb.getInstructions().size() - 1);
            if (last.getOpcode() != IrInstruction.Opcode.BR) continue;
            List<IrValue> ops = last.getOperands();
            if (ops.size() == 1) {
                if (targetLabel.equals(ops.get(0).getName())) {
                    if (pred != null) return null;
                    pred = bb;
                }
            } else if (ops.size() == 3) {
                if (targetLabel.equals(ops.get(1).getName()) || targetLabel.equals(ops.get(2).getName())) {
                    if (pred != null) return null;
                    pred = bb;
                }
            }
        }
        return pred;
    }

    private boolean hasPhi(IrBasicBlock bb) {
        for (IrInstruction ins : bb.getInstructions()) {
            if (ins.getOpcode() == IrInstruction.Opcode.PHI) return true;
        }
        return false;
    }

    /* ---------- bounded fixpoint sweep ---------- */
    private void simplifyFixpoint(IrModule module, int maxIter) {
        for (int it = 0; it < maxIter; it++) {
            int before = countInstr(module);
            constantFold(module);
            algebraicSimplify(module);
            localCse(module);
            branchSimplify(module);
            trimAfterTerminator(module);
            mergeStraightLineBlocks(module);
            deadResultEliminate(module);
            removeUnreachableBlocks(module);
            int after = countInstr(module);
            if (after >= 0 && after == before) break;
        }
    }

    private int countInstr(IrModule module) {
        int cnt = 0;
        for (IrFunction fn : module.getFunctions()) {
            for (IrBasicBlock bb : fn.getBlocks()) {
                cnt += bb.getInstructions().size();
            }
        }
        return cnt;
    }

    /* ---------- local CSE/LVN ---------- */
    private void localCse(IrModule module) {
        for (IrFunction fn : module.getFunctions()) {
            for (IrBasicBlock bb : fn.getBlocks()) {
                Map<String, IrValue> table = new HashMap<>();
                Iterator<IrInstruction> it = bb.getInstructions().iterator();
                while (it.hasNext()) {
                    IrInstruction ins = it.next();
                    IrRegister def = ins.getResult();
                    if (def == null) continue;
                    if (!CSE_OPS.contains(ins.getOpcode())) continue;
                    String key = buildKey(ins);
                    if (key == null) continue;
                    IrValue existed = table.get(key);
                    if (existed != null) {
                        replaceAllUses(def, existed);
                        ins.detachOperands();
                        it.remove();
                    } else {
                        table.put(key, def);
                    }
                }
            }
        }
    }

    private String buildKey(IrInstruction ins) {
        StringBuilder sb = new StringBuilder();
        sb.append(ins.getOpcode().name());
        if (ins.getOpcode() == IrInstruction.Opcode.ICMP) {
            String pred = icmpPred(ins);
            if (pred == null) return null;
            sb.append(':').append(pred);
        }
        List<String> ops = new ArrayList<>();
        for (int i = 0; i < ins.getOperandCount(); i++) {
            IrValue v = ins.getOperand(i);
            if (v == null) return null;
            ops.add(opKey(v));
        }
        if (COMMUTATIVE.contains(ins.getOpcode())) {
            ops.sort(String::compareTo);
        }
        for (String op : ops) {
            sb.append('|').append(op);
        }
        return sb.toString();
    }

    private String opKey(IrValue v) {
        if (v instanceof IrConstInt) {
            IrConstInt ci = (IrConstInt) v;
            return "#" + ci.getValue() + ":" + (ci.getType() != null ? ci.getType().getBits() : 32);
        }
        if (v instanceof IrGlobalRef) {
            return "G" + v.getName();
        }
        return v.getName();
    }

    /* ---------- dead result elimination ---------- */
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

    /* ---------- dead store elimination ---------- */
    private void deadStoreEliminate(IrModule module) {
        for (IrFunction fn : module.getFunctions()) {
            Set<IrInstruction> toRemove = new HashSet<>();
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
                    for (IrInstruction user : new ArrayList<>(ptr.getUsers())) {
                        user.detachOperands();
                        toRemove.add(user);
                    }
                    alloca.detachOperands();
                    toRemove.add(alloca);
                }
            }
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

    /* ---------- unreachable block removal ---------- */
    private void removeUnreachableBlocks(IrModule module) {
        for (IrFunction fn : module.getFunctions()) {
            List<IrBasicBlock> blocks = fn.getBlocks();
            if (blocks.isEmpty()) continue;
            Map<String, IrBasicBlock> labelMap = new HashMap<>();
            for (IrBasicBlock b : blocks) {
                labelMap.put(b.getLabel(), b);
            }
            Set<IrBasicBlock> reachable = new HashSet<>();
            dfsReach(blocks.get(0), reachable, labelMap);
            if (reachable.size() == blocks.size()) continue;
            Iterator<IrBasicBlock> bit = blocks.iterator();
            while (bit.hasNext()) {
                IrBasicBlock b = bit.next();
                if (!reachable.contains(b)) {
                    for (IrInstruction ins : b.getInstructions()) {
                        ins.detachOperands();
                    }
                    bit.remove();
                }
            }
        }
    }

    private void dfsReach(IrBasicBlock block, Set<IrBasicBlock> vis, Map<String, IrBasicBlock> labelMap) {
        if (block == null || vis.contains(block)) return;
        vis.add(block);
        List<IrInstruction> insns = block.getInstructions();
        if (insns.isEmpty()) return;
        IrInstruction last = insns.get(insns.size() - 1);
        if (last.getOpcode() != IrInstruction.Opcode.BR) return;
        List<IrValue> ops = last.getOperands();
        if (ops.isEmpty()) return;
        if (ops.size() == 1) {
            IrBasicBlock succ = labelMap.get(ops.get(0).getName());
            dfsReach(succ, vis, labelMap);
        } else if (ops.size() == 3) {
            IrBasicBlock succT = labelMap.get(ops.get(1).getName());
            IrBasicBlock succF = labelMap.get(ops.get(2).getName());
            dfsReach(succT, vis, labelMap);
            dfsReach(succF, vis, labelMap);
        }
    }

    private static final EnumSet<IrInstruction.Opcode> FOLDABLE = EnumSet.of(
            IrInstruction.Opcode.ADD,
            IrInstruction.Opcode.SUB,
            IrInstruction.Opcode.MUL,
            IrInstruction.Opcode.SDIV,
            IrInstruction.Opcode.SREM,
            IrInstruction.Opcode.XOR,
            IrInstruction.Opcode.ZEXT,
            IrInstruction.Opcode.ICMP
    );

    private static final EnumSet<IrInstruction.Opcode> ALGEBRA_OPS = EnumSet.of(
            IrInstruction.Opcode.ICMP,
            IrInstruction.Opcode.ADD,
            IrInstruction.Opcode.SUB,
            IrInstruction.Opcode.MUL,
            IrInstruction.Opcode.SDIV,
            IrInstruction.Opcode.SREM,
            IrInstruction.Opcode.XOR
    );

    private static final EnumSet<IrInstruction.Opcode> CSE_OPS = EnumSet.of(
            IrInstruction.Opcode.ADD,
            IrInstruction.Opcode.SUB,
            IrInstruction.Opcode.MUL,
            IrInstruction.Opcode.SDIV,
            IrInstruction.Opcode.SREM,
            IrInstruction.Opcode.XOR,
            IrInstruction.Opcode.ZEXT,
            IrInstruction.Opcode.ICMP
    );

    private static final EnumSet<IrInstruction.Opcode> COMMUTATIVE = EnumSet.of(
            IrInstruction.Opcode.ADD,
            IrInstruction.Opcode.MUL,
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
