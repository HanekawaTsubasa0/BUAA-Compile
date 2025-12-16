package semantic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class SymbolTable {
    public static class Scope {
        private final int id;
        private final LinkedHashMap<String, Symbol> symbols = new LinkedHashMap<>();

        public Scope(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public LinkedHashMap<String, Symbol> getSymbols() {
            return symbols;
        }
    }

    private final List<Scope> scopeStack = new ArrayList<>();
    private final List<Scope> allScopes = new ArrayList<>();
    private int nextScopeId = 1;

    public void enterScope() {
        Scope scope = new Scope(nextScopeId++);
        scopeStack.add(scope);
        allScopes.add(scope);
    }

    public void exitScope() {
        if (!scopeStack.isEmpty()) {
            scopeStack.remove(scopeStack.size() - 1);
        }
    }

    public boolean define(Symbol symbol) {
        Scope current = scopeStack.get(scopeStack.size() - 1);
        if (current.getSymbols().containsKey(symbol.getName())) {
            return false;
        }
        current.getSymbols().put(symbol.getName(), symbol);
        return true;
    }

    public Symbol lookupCurrent(String name) {
        if (scopeStack.isEmpty()) return null;
        return scopeStack.get(scopeStack.size() - 1).getSymbols().get(name);
    }

    public Symbol lookup(String name) {
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Scope scope = scopeStack.get(i);
            if (scope.getSymbols().containsKey(name)) {
                return scope.getSymbols().get(name);
            }
        }
        return null;
    }

    public List<Scope> getAllScopes() {
        return allScopes;
    }

    public int depth() {
        return scopeStack.size();
    }
}
