package io.runebox.kasm.ir.stack.insn;

import io.runebox.kasm.ir.type.Type;

import java.util.Optional;

/**
 * Return from a method (java's 'return' keyword).
 *
 * If the method is not of 'void' type, a value will
 * be popped from the stack and returned.
 */
public class ReturnInsn extends AbstractInstruction {
    /**
     * Type that is returned by the method or empty for 'void' methods.
     */
    private Optional<Type> type;

    /**
     * Initialize a return instruction that returns no value.
     */
    public ReturnInsn() {
        this.type = Optional.empty();
    }

    /**
     * Initialize a return instruction that returns a value.
     *
     * @param type of returned value
     */
    public ReturnInsn(Type type) {
        this.type = Optional.of(type);
    }

    public ReturnInsn(Optional<Type> type) {
        this.type = type;
    }

    public Optional<Type> getType() {
        return type;
    }

    public void setType(Optional<Type> type) {
        this.type = type;
    }

    @Override
    public int getPushCount() {
        return 0;
    }

    @Override
    public int getPopCount() {
        return type.isPresent() ? 1 : 0;
    }

    @Override
    public boolean continuesExecution() {
        return false;
    }
}
