package io.runebox.kasm.ir.analysis;

import io.runebox.kasm.ir.FieldRef;
import io.runebox.kasm.ir.Path;
import io.runebox.kasm.ir.constant.PushableConstant;
import io.runebox.kasm.ir.type.ArrayType;
import io.runebox.kasm.ir.type.PrimitiveType;
import io.runebox.kasm.ir.type.RefType;
import io.runebox.kasm.ir.type.Type;
import io.runebox.kasm.ir.stack.insn.IfInsn;
import io.runebox.kasm.ir.stack.invoke.AbstractInstanceInvoke;
import io.runebox.kasm.ir.stack.invoke.Invoke;
import io.runebox.kasm.ir.util.StackInsnVisitor;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;

public class StackEmulatingInsnVisitor<I, L> extends StackInsnVisitor<I, L> {
    private Stack.Mutable<JvmType> stack;

    public StackEmulatingInsnVisitor(StackInsnVisitor<I, L> iv, Stack.Mutable<JvmType> stack) {
        super(iv);
        this.stack = stack;
    }

    public Stack.Mutable<JvmType> getStack() {
        return stack;
    }

    public void setStack(Stack.Mutable<JvmType> stack) {
        this.stack = stack;
    }

    private void push(JvmType type) {
        this.stack.push(type);
    }

    private JvmType pop() {
        return this.stack.pop();
    }

    /**
     * Require that a certain value is on top of the stack and drop it.
     *
     * @param type that must be on top of the stack
     * @throws Stack.StackUnderflowException the stack was empty
     * @throws IllegalStateException the requirement does not match
     */
    private void popRequire(JvmType type) {
        JvmType actual = pop();
        if (type != actual) {
            throw new RuntimeException("Wrong value on Stack: expeced: " + type + ", got: " + actual);
        }
    }

    @Override
    public void visitPush(PushableConstant constant) {
        push(JvmType.from(constant.getType()));

        super.visitPush(constant);
    }

    @Override
    public void visitNeg(Type type) {
        JvmType jvmType = JvmType.from(type);
        popRequire(jvmType); // value to be negated
        push(jvmType); // negated value

        super.visitNeg(type);
    }

    @Override
    public void visitAdd(Type type) {
        JvmType jvmType = JvmType.from(type);
        popRequire(jvmType); // operand 2
        popRequire(jvmType); // operand 1
        push(jvmType); //result

        super.visitAdd(type);
    }

    @Override
    public void visitSub(Type type) {
        JvmType jvmType = JvmType.from(type);
        popRequire(jvmType); // operand 2
        popRequire(jvmType); // operand 1
        push(jvmType); //result

        super.visitSub(type);
    }

    @Override
    public void visitMul(Type type) {
        JvmType jvmType = JvmType.from(type);
        popRequire(jvmType); // operand 2
        popRequire(jvmType); // operand 1
        push(jvmType); //result

        super.visitMul(type);
    }

    @Override
    public void visitDiv(Type type) {
        JvmType jvmType = JvmType.from(type);
        popRequire(jvmType); // operand 2
        popRequire(jvmType); // operand 1
        push(jvmType); //result

        super.visitDiv(type);
    }

    @Override
    public void visitMod(Type type) {
        JvmType jvmType = JvmType.from(type);
        popRequire(jvmType); // operand 2
        popRequire(jvmType); // operand 1
        push(jvmType); //result

        super.visitMod(type);
    }

    @Override
    public void visitAnd(Type type) {
        JvmType jvmType = JvmType.from(type);
        popRequire(jvmType); // operand 2
        popRequire(jvmType); // operand 1
        push(jvmType); //result

        super.visitAnd(type);
    }

    @Override
    public void visitOr(Type type) {
        JvmType jvmType = JvmType.from(type);
        popRequire(jvmType); // operand 2
        popRequire(jvmType); // operand 1
        push(jvmType); //result

        super.visitOr(type);
    }

    @Override
    public void visitXor(Type type) {
        JvmType jvmType = JvmType.from(type);
        popRequire(jvmType); // operand 2
        popRequire(jvmType); // operand 1
        push(jvmType); //result

        super.visitXor(type);
    }

    @Override
    public void visitShl(Type type) {
        JvmType jvmType = JvmType.from(type);
        popRequire(JvmType.INT); // operand 2
        popRequire(jvmType); // operand 1
        push(jvmType); //result

        super.visitShl(type);
    }

    @Override
    public void visitShr(Type type) {
        JvmType jvmType = JvmType.from(type);
        popRequire(JvmType.INT); // operand 2
        popRequire(jvmType); // operand 1
        push(jvmType); //result

        super.visitShr(type);
    }

    @Override
    public void visitUShr(Type type) {
        JvmType jvmType = JvmType.from(type);
        popRequire(JvmType.INT); // operand 2
        popRequire(jvmType); // operand 1
        push(jvmType); //result

        super.visitUShr(type);
    }

    @Override
    public void visitCmp() {
        popRequire(JvmType.LONG); // operand 2
        popRequire(JvmType.LONG); // operand 1
        push(JvmType.INT); //result

        super.visitCmp();
    }

    @Override
    public void visitCmpl(Type type) {
        JvmType jvmType = JvmType.from(type);
        popRequire(jvmType); // operand 2
        popRequire(jvmType); // operand 1
        push(JvmType.INT); //result

        super.visitCmpl(type);
    }

    @Override
    public void visitCmpg(Type type) {
        JvmType jvmType = JvmType.from(type);
        popRequire(jvmType); // operand 2
        popRequire(jvmType); // operand 1
        push(JvmType.INT); //result

        super.visitCmpg(type);
    }

    @Override
    public void visitNewArray(ArrayType type, int initializedDimensions) {
        for (int i = 0; i < initializedDimensions; i++) {
            popRequire(JvmType.INT); // size of a dimension
        }
        push(JvmType.REFERENCE); // the created array

        super.visitNewArray(type, initializedDimensions);
    }

    @Override
    public void visitArrayLength() {
        popRequire(JvmType.REFERENCE); // an array
        push(JvmType.INT); // length of array

        super.visitArrayLength();
    }

    @Override
    public void visitArrayLoad(Type type) {
        popRequire(JvmType.INT); // index
        popRequire(JvmType.REFERENCE); // the array
        push(JvmType.from(type)); // an element from the array

        super.visitArrayLoad(type);
    }

    @Override
    public void visitArrayStore(Type type) {
        popRequire(JvmType.from(type)); // element to be stored
        popRequire(JvmType.INT); // index
        popRequire(JvmType.REFERENCE); // the array

        super.visitArrayStore(type);
    }

    @Override
    public void visitSwap() {
        JvmType value1 = pop();
        JvmType value2 = pop();
        push(value1);
        push(value2);

        super.visitSwap();
    }

    @Override
    public void visitPop() {
        pop();

        super.visitPop();
    }

    @Override
    public void visitDup() {
        JvmType value = pop();
        push(value);
        push(value);

        super.visitDup();
    }

    @Override
    public void visitDupX1() {
        JvmType value1 = pop();
        JvmType value2 = pop();

        push(value1);

        push(value2);
        push(value1);

        super.visitDupX1();
    }

    @Override
    public void visitDupX2() {
        JvmType value1 = pop();
        JvmType value2 = pop();
        JvmType value3 = pop();

        push(value1);

        push(value3);
        push(value2);
        push(value1);

        super.visitDupX2();
    }

    @Override
    public void visitDup2() {
        JvmType value1 = pop();
        JvmType value2 = pop();

        push(value2);
        push(value1);

        push(value2);
        push(value1);

        super.visitDup2();
    }

    @Override
    public void visitDup2X1() {
        JvmType value1 = pop();
        JvmType value2 = pop();
        JvmType value3 = pop();

        push(value2);
        push(value1);

        push(value3);
        push(value2);
        push(value1);

        super.visitDup2X1();
    }

    @Override
    public void visitDup2X2() {
        JvmType value1 = pop();
        JvmType value2 = pop();
        JvmType value3 = pop();
        JvmType value4 = pop();

        push(value2);
        push(value1);

        push(value4);
        push(value3);
        push(value2);
        push(value1);

        super.visitDup2X2();
    }

    @Override
    public void visitLoad(Type type, L local) {
        push(JvmType.from(type)); // value from local

        super.visitLoad(type, local);
    }

    @Override
    public void visitStore(Type type, L local) {
        popRequire(JvmType.from(type)); // value to be stored in local

        super.visitStore(type, local);
    }

    @Override
    public void visitIncrement(L local, int value) {
        // no stack change

        super.visitIncrement(local, value);
    }

    @Override
    public void visitNew(Path type) {
        push(JvmType.REFERENCE); // not yet initialized object

        super.visitNew(type);
    }

    @Override
    public void visitInstanceOf(RefType type) {
        popRequire(JvmType.REFERENCE); // object to be checked
        push(JvmType.INT); // result (0 or 1)

        super.visitInstanceOf(type);
    }

    @Override
    public void visitPrimitiveCast(PrimitiveType from, PrimitiveType to) {
        popRequire(JvmType.from(from)); // value to be casted
        push(JvmType.from(to)); // casted value

        super.visitPrimitiveCast(from, to);
    }

    @Override
    public void visitReferenceCast(RefType type) {
        popRequire(JvmType.REFERENCE); // value to be casted
        push(JvmType.REFERENCE); // the casted value

        super.visitReferenceCast(type);
    }

    @Override
    public void visitReturn(Optional<Type> type) {
        type.map(JvmType::from).ifPresent(this::popRequire); // value to be returned

        super.visitReturn(type);
    }

    @Override
    public void visitThrow() {
        popRequire(JvmType.REFERENCE); // exception to be thrown

        // Clear the stack and push the exception.
        // This prepares the stack for a possible jump to an exception handler.
        this.stack.clear();
        push(JvmType.REFERENCE);

        super.visitThrow();
    }

    @Override
    public void visitMonitorEnter() {
        popRequire(JvmType.REFERENCE); // value to gain lock on

        super.visitMonitorEnter();
    }

    @Override
    public void visitMonitorExit() {
        popRequire(JvmType.REFERENCE); // value to release lock from

        super.visitMonitorExit();
    }

    @Override
    public void visitFieldGet(FieldRef fieldRef, boolean isStatic) {
        if (!isStatic) {
            popRequire(JvmType.REFERENCE); // instance of class containing the field
        }
        push(JvmType.from(fieldRef.getType())); // value of the field

        super.visitFieldGet(fieldRef, isStatic);
    }

    @Override
    public void visitFieldSet(FieldRef fieldRef, boolean isStatic) {
        popRequire(JvmType.from(fieldRef.getType())); // value to store in field
        if (!isStatic) {
            popRequire(JvmType.REFERENCE); // instance of class containing the field
        }

        super.visitFieldSet(fieldRef, isStatic);
    }

    @Override
    public void visitInvokeInsn(Invoke invoke) {
        // pop arguments in reverse order
        List<Type> arguments = invoke.getDescriptor().getParameterTypes();
        ListIterator<Type> iter = arguments.listIterator(arguments.size());
        while (iter.hasPrevious()) {
            popRequire(JvmType.from(iter.previous()));
        }

        if (invoke instanceof AbstractInstanceInvoke) {
            popRequire(JvmType.REFERENCE); // instance of class containing the method
        }

        invoke.getDescriptor().getReturnType()
                .map(JvmType::from).ifPresent(this::push);

        super.visitInvokeInsn(invoke);
    }

    @Override
    public void visitGoto(I target) {
        // no stack changes

        super.visitGoto(target);
    }

    @Override
    public void visitIf(IfInsn.Condition condition, I target) {
        JvmType type = condition instanceof IfInsn.IntCondition ? JvmType.INT : JvmType.REFERENCE;
        if (condition.getCompareValue() instanceof IfInsn.StackValue) {
            popRequire(type); // other value from stack to compare against
        }
        popRequire(type); // value to be compared

        super.visitIf(condition, target);
    }

    @Override
    public void visitSwitch(Map<Integer, I> targetTable, I defaultTarget) {
        popRequire(JvmType.INT); // value to be compared against the table

        super.visitSwitch(targetTable, defaultTarget);
    }
}
