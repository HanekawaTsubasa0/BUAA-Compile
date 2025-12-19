package backend.ir;

import java.util.ArrayList;
import java.util.List;

public abstract class IrValue {
    protected String name;
    protected IrType type;
    private final List<IrInstruction> users = new ArrayList<>();

    protected IrValue(String name, IrType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public IrType getType() {
        return type;
    }

    public void setType(IrType type) {
        this.type = type;
    }

    public void addUser(IrInstruction user) {
        users.add(user);
    }

    public void removeUser(IrInstruction user) {
        users.remove(user);
    }

    public List<IrInstruction> getUsers() {
        return users;
    }
}
