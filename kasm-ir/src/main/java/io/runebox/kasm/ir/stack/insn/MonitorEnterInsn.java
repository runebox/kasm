package io.runebox.kasm.ir.stack.insn;

/**
 * Pop an object value and gain a lock on it.
 * It is used to compile java synchronized blocks.
 *
 * @see MonitorExitInsn to unlock the object.
 */
public class MonitorEnterInsn extends AbstractInstruction {
    @Override
    public int getPushCount() {
        return 0;
    }

    @Override
    public int getPopCount() {
        return 1;
    }
}
