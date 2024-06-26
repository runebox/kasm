package io.runebox.kasm.ir.ref.expr;

import io.runebox.kasm.ir.type.IntType;
import io.runebox.kasm.ir.type.Type;
import io.runebox.kasm.ir.ref.Expression;

/**
 * Compare two float or double values.
 *
 * If both numbers are equal it evaluates to 0.
 * Otherwise it evaluates to either 1 or -1
 * depending on which number is greater.
 *
 * {@link CmplExpr} and {@link CmpgExpr} differ in the way they handle NaN.
 */
public class CmpgExpr extends AbstractBinaryExpr {
    public CmpgExpr(Expression value1, Expression value2) {
        super(value1, value2);
    }

    @Override
    public Type getType() {
        return IntType.getInstance();
    }
}
