package io.runebox.kasm.ir.util;

import io.runebox.kasm.ir.FieldRef;
import io.runebox.kasm.ir.Path;
import io.runebox.kasm.ir.constant.PushableConstant;
import io.runebox.kasm.ir.type.ArrayType;
import io.runebox.kasm.ir.type.PrimitiveType;
import io.runebox.kasm.ir.type.RefType;
import io.runebox.kasm.ir.type.Type;
import io.runebox.kasm.ir.stack.insn.IfInsn;
import io.runebox.kasm.ir.stack.insn.Instruction;
import io.runebox.kasm.ir.stack.invoke.Invoke;

import java.util.Map;
import java.util.Optional;

/**
 * Visitor that visits instructions in a way similar to the stack {@link Instruction instructions}.
 *
 * @param <I> A representation of a reference to an instruction
 * @param <L> A representation for a local
 */
public class StackInsnVisitor<I, L> {
    private StackInsnVisitor<I, L> iv;

    public StackInsnVisitor(StackInsnVisitor<I, L> iv) {
        this.iv = iv;
    }

    public void visitPush(PushableConstant constant) {
        if (iv != null) {
            iv.visitPush(constant);
        }
    }

    // MATH (ARITHMETIC)

    public void visitNeg(Type type) {
        if (iv != null) {
            iv.visitNeg(type);
        }
    }

    public void visitAdd(Type type) {
        if (iv != null) {
            iv.visitAdd(type);
        }
    }

    public void visitSub(Type type) {
        if (iv != null) {
            iv.visitSub(type);
        }
    }

    public void visitMul(Type type) {
        if (iv != null) {
            iv.visitMul(type);
        }
    }

    public void visitDiv(Type type) {
        if (iv != null) {
            iv.visitDiv(type);
        }
    }

    public void visitMod(Type type) {
        if (iv != null) {
            iv.visitMod(type);
        }
    }

    // MATH (LOGIC)

    public void visitAnd(Type type) {
        if (iv != null) {
            iv.visitAnd(type);
        }
    }

    public void visitOr(Type type) {
        if (iv != null) {
            iv.visitOr(type);
        }
    }

    public void visitXor(Type type) {
        if (iv != null) {
            iv.visitXor(type);
        }
    }

    public void visitShl(Type type) {
        if (iv != null) {
            iv.visitShl(type);
        }
    }

    public void visitShr(Type type) {
        if (iv != null) {
            iv.visitShr(type);
        }
    }

    public void visitUShr(Type type) {
        if (iv != null) {
            iv.visitUShr(type);
        }
    }

    public void visitCmp() {
        if (iv != null) {
            iv.visitCmp();
        }
    }

    public void visitCmpl(Type type) {
        if (iv != null) {
            iv.visitCmpl(type);
        }
    }

    public void visitCmpg(Type type) {
        if (iv != null) {
            iv.visitCmpg(type);
        }
    }

    // ARRAY RELATED INSTRUCTIONS

    public void visitNewArray(ArrayType type, int initializedDimensions) {
        if (iv != null) {
            iv.visitNewArray(type, initializedDimensions);
        }
    }


    public void visitArrayLength() {
        if (iv != null) {
            iv.visitArrayLength();
        }
    }

    public void visitArrayLoad(Type type) {
        if (iv != null) {
            iv.visitArrayLoad(type);
        }
    }

    public void visitArrayStore(Type type) {
        if (iv != null) {
            iv.visitArrayStore(type);
        }
    }

    // STACK MODIFYING INSTRUCTIONS

    public void visitSwap() {
        if (iv != null) {
            iv.visitSwap();
        }
    }

    public void visitPop() {
        if (iv != null) {
            iv.visitPop();
        }
    }

    public void visitDup() {
        if (iv != null) {
            iv.visitDup();
        }
    }

    public void visitDupX1() {
        if (iv != null) {
            iv.visitDupX1();
        }
    }

    public void visitDupX2() {
        if (iv != null) {
            iv.visitDupX2();
        }
    }

    public void visitDup2() {
        if (iv != null) {
            iv.visitDup2();
        }
    }

    public void visitDup2X1() {
        if (iv != null) {
            iv.visitDup2X1();
        }
    }

    public void visitDup2X2() {
        if (iv != null) {
            iv.visitDup2X2();
        }
    }

    // LOCAL RELATED INSTRUCTIONS

    public void visitLoad(Type type, L local) {
        if (iv != null) {
            iv.visitLoad(type, local);
        }
    }

    public void visitStore(Type type, L local) {
        if (iv != null) {
            iv.visitStore(type, local);
        }
    }

    public void visitIncrement(L local, int value) {
        if (iv != null) {
            iv.visitIncrement(local, value);
        }
    }

    // CLASS INSTANCE RELATED INSTRUCTIONS

    public void visitNew(Path type) {
        if (iv != null) {
            iv.visitNew(type);
        }
    }

    public void visitInstanceOf(RefType type) {
        if (iv != null) {
            iv.visitInstanceOf(type);
        }
    }

    // CASTS

    public void visitPrimitiveCast(PrimitiveType from, PrimitiveType to) {
        if (iv != null) {
            iv.visitPrimitiveCast(from, to);
        }
    }

    public void visitReferenceCast(RefType type) {
        if (iv != null) {
            iv.visitReferenceCast(type);
        }
    }

    // METHOD EXITING INSTRUCTIONS

    public void visitReturn(Optional<Type> type) {
        if (iv != null) {
            iv.visitReturn(type);
        }
    }

    public void visitThrow() {
        if (iv != null) {
            iv.visitThrow();
        }
    }

    // MONITOR INSTRUCTIONS

    public void visitMonitorEnter() {
        if (iv != null) {
            iv.visitMonitorEnter();
        }
    }

    public void visitMonitorExit() {
        if (iv != null) {
            iv.visitMonitorExit();
        }
    }

    // FIELD & METHOD ACCESSES

    public void visitFieldGet(FieldRef fieldRef, boolean isStatic) {
        if (iv != null) {
            iv.visitFieldGet(fieldRef, isStatic);
        }
    }

    public void visitFieldSet(FieldRef fieldRef, boolean isStatic) {
        if (iv != null) {
            iv.visitFieldSet(fieldRef, isStatic);
        }
    }

    public void visitInvokeInsn(Invoke invoke) {
        if (iv != null) {
            iv.visitInvokeInsn(invoke);
        }
    }

    // CFG

    public void visitGoto(I target) {
        if (iv != null) {
            iv.visitGoto(target);
        }
    }

    public void visitIf(IfInsn.Condition condition, I target) {
        if (iv != null) {
            iv.visitIf(condition, target);
        }
    }

    public void visitSwitch(Map<Integer, I> targetTable, I defaultTarget) {
        if (iv != null) {
            iv.visitSwitch(targetTable, defaultTarget);
        }
    }
}
