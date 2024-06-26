package io.runebox.kasm.ir.stack.insn;

import io.runebox.kasm.ir.type.ArrayType;

/**
 * Pop int(s) from the stack and initialize an array with these size(s).
 *
 * This instruction is capable of initializing multidimensional instructions.
 * It replaces the opcodes "NEWARRAY", "ANEWARRAY" and "MULTIANEWARRAY".
 */
public class NewArrayInsn extends AbstractInstruction {
    /**
     * Type of array that will be created.
     */
    private ArrayType type;

    /**
     * Amount of dimensions that will be initialized.
     *
     * examples:
     * - <tt>1</tt> for <pre><code>new int[7]</code></pre>
     * - <tt>1</tt> for <pre><code>new int[7][][]</code></pre>
     * - <tt>2</tt> for <pre><code>new int[7][8][]</code></pre>
     * - <tt>3</tt> for <pre><code>new int[7][8][9]</code></pre>
     */
    private int initializedDimensions;

    /**
     * Initialize a one dimensional array or a multidimensional
     * array where all values are null.
     *
     * @param type of array to be initialized.
     */
    public NewArrayInsn(ArrayType type) {
        this(type, 1);
    }

    public NewArrayInsn(ArrayType type, int initializedDimensions) {
        this.type = type;
        setInitializedDimensions(initializedDimensions);
    }

    public ArrayType getType() {
        return type;
    }

    public void setType(ArrayType type) {
        this.type = type;
    }

    public int getInitializedDimensions() {
        return initializedDimensions;
    }

    public void setInitializedDimensions(int initializedDimensions) {
        if (initializedDimensions < 1) {
            throw new IllegalArgumentException("initializedDimensions < 1");
        }

        this.initializedDimensions = initializedDimensions;
    }

    @Override
    public int getPushCount() {
        return 1;
    }

    @Override
    public int getPopCount() {
        return initializedDimensions;
    }
}
