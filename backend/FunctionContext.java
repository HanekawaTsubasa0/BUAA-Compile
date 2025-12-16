package backend;

import semantic.SymbolType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FunctionContext {
    private static class ScopedSymbol {
        int depth;
        LocalSymbol symbol;
        ScopedSymbol(int depth, LocalSymbol symbol) {
            this.depth = depth;
            this.symbol = symbol;
        }
    }

    private final Map<String, List<ScopedSymbol>> locals = new HashMap<>();
    private final Map<String, LocalSymbol> lastDefined = new HashMap<>();
    // Locals grow down from $fp (fp points to old sp); offsets are negative.
    // We reserve 8 bytes above for saved $ra/$fp, so first local starts at fp-12.
    // Additionally, the code generator saves $s0-$s7 (32 bytes) at the top of the frame,
    // so locals must be placed below that save area to avoid overlap.
    private static final int SAVE_S_SIZE = 32;
    private int localBytes = 0; // total bytes allocated for locals/params
    private int depth = 0;
    private boolean suppressPop = false;

    public int allocBytes(int bytes) {
        // word align every allocation
        bytes = (bytes + 3) / 4 * 4;
        // place this allocation just below current locals and saved $s registers
        int offset = -(localBytes + bytes + 8 + SAVE_S_SIZE);
        localBytes += bytes;
        return offset;
    }

    public int getFrameSize() {
        // total frame = saved ra/fp (8) + locals (aligned)
        return ((localBytes + 8) + 3) / 4 * 4;
    }

    public int getLocalBytes() {
        return localBytes;
    }

    public void enterScope() {
        depth++;
    }

    public void exitScope() {
        if (suppressPop) {
            depth = Math.max(0, depth - 1);
            return;
        }
        // remove all symbols defined in the current scope depth
        Iterator<Map.Entry<String, List<ScopedSymbol>>> it = locals.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<ScopedSymbol>> e = it.next();
            List<ScopedSymbol> list = e.getValue();
            while (!list.isEmpty() && list.get(list.size() - 1).depth == depth) {
                list.remove(list.size() - 1);
            }
            if (list.isEmpty()) {
                it.remove();
            }
        }
        // rebuild lastDefined to reflect the remaining innermost bindings
        lastDefined.clear();
        for (Map.Entry<String, List<ScopedSymbol>> e : locals.entrySet()) {
            List<ScopedSymbol> list = e.getValue();
            if (!list.isEmpty()) {
                lastDefined.put(e.getKey(), list.get(list.size() - 1).symbol);
            }
        }
        depth = Math.max(0, depth - 1);
    }

    public void setSuppressPop(boolean suppressPop) {
        this.suppressPop = suppressPop;
    }

    public void addLocal(String name, LocalSymbol symbol) {
        locals.computeIfAbsent(name, k -> new ArrayList<>()).add(new ScopedSymbol(depth, symbol));
        lastDefined.put(name, symbol);
    }

    public LocalSymbol getLocal(String name) {
        List<ScopedSymbol> list = locals.get(name);
        if (list == null) return null;
        // Find the innermost symbol that is visible at the current depth.
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).depth <= depth) {
                return list.get(i).symbol;
            }
        }
        return null;
    }

    public LocalSymbol getAnyLocal(String name) {
        return lastDefined.get(name);
    }
}
