package backend.ir;

import java.util.ArrayList;
import java.util.List;

public class IrModule {
    private final List<String> declarations = new ArrayList<>();
    private final List<String> globalDefs = new ArrayList<>();
    private final List<String> stringDefs = new ArrayList<>();
    private final List<IrFunction> functions = new ArrayList<>();

    public void addDeclaration(String decl) {
        declarations.add(decl);
    }

    public void addGlobalDef(String def) {
        globalDefs.add(def);
    }

    public void addStringDef(String def) {
        stringDefs.add(def);
    }

    public void addFunction(IrFunction func) {
        functions.add(func);
    }

    public List<IrFunction> getFunctions() {
        return functions;
    }

    public List<String> getGlobalDefs() {
        return globalDefs;
    }

    public List<String> getStringDefs() {
        return stringDefs;
    }

    public String emit() {
        StringBuilder sb = new StringBuilder();
        for (String decl : declarations) {
            sb.append(decl).append("\n");
        }
        if (!declarations.isEmpty()) sb.append("\n");
        for (String g : globalDefs) {
            sb.append(g).append("\n");
        }
        if (!globalDefs.isEmpty()) sb.append("\n");
        for (String s : stringDefs) {
            sb.append(s).append("\n");
        }
        if (!stringDefs.isEmpty()) sb.append("\n");
        for (IrFunction f : functions) {
            sb.append(f.emit());
        }
        return sb.toString();
    }
}
