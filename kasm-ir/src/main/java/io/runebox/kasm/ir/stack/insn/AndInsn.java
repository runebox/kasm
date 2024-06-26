package io.runebox.kasm.ir.stack.insn;

import io.runebox.kasm.ir.type.IntType;
import io.runebox.kasm.ir.type.LongType;
import io.runebox.kasm.ir.type.Type;

/**
 * Pop two int/long values, calculate the logical and ('&' operator) and push the result.
 */
public class AndInsn extends AbstractBinaryMathInsn {
    public AndInsn(Type type) {
        super(type);
    }

    @Override
    protected boolean isTypeSupported(Type type) {
        return type instanceof IntType || type instanceof LongType;
    }
}
