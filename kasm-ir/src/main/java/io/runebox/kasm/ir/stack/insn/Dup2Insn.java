package io.runebox.kasm.ir.stack.insn;

/**
 * Pop two values from the stack and push them twice.
 *
 * Example:
 * - before: value1, value2, ...
 * - after: value1, value2, value1, value2, ...
 */
public class Dup2Insn extends AbstractInstruction {
    @Override
    public int getPushCount() {
        return 4;
    }

    @Override
    public int getPopCount() {
        return 2;
    }
}
