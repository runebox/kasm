package io.runebox.kasm.ir.stack.insn;

/**
 * Pop an object value and try to remove one lock.
 * It is used at the end of java's synchronized blocks.
 *
 * @see MonitorEnterInsn gain a lock on an object
 */
public class MonitorExitInsn extends AbstractInstruction {
    @Override
    public int getPushCount() {
        return 0;
    }

    @Override
    public int getPopCount() {
        return 1;
    }
}
