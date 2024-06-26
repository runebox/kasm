package io.runebox.kasm.ir.stack.insn;

import io.runebox.kasm.ir.type.Type;

/**
 * Code shared between instruction that can only operate on certain types.
 */
public abstract class AbstractTypeInsn extends AbstractInstruction {
    /**
     * Types that this instruction may operate on.
     */
    private Type type;

    public AbstractTypeInsn(Type type) {
        setType(type);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        if (!isTypeSupported(type)) {
            throw new IllegalArgumentException(type + " is not supported by this instruction");
        }

        this.type = type;
    }

    protected abstract boolean isTypeSupported(Type type);
}
