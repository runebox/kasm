package io.runebox.kasm.ir.conversion.stack2ref;

import io.runebox.kasm.ir.FieldRef;
import io.runebox.kasm.ir.MethodDescriptor;
import io.runebox.kasm.ir.Path;
import io.runebox.kasm.ir.constant.IntConstant;
import io.runebox.kasm.ir.constant.NullConstant;
import io.runebox.kasm.ir.constant.PushableConstant;
import io.runebox.kasm.ir.type.ArrayType;
import io.runebox.kasm.ir.type.PrimitiveType;
import io.runebox.kasm.ir.type.RefType;
import io.runebox.kasm.ir.type.Type;
import io.runebox.kasm.ir.util.RWCell;
import io.runebox.kasm.ir.ref.Expression;
import io.runebox.kasm.ir.ref.RefLocal;
import io.runebox.kasm.ir.ref.Statement;
import io.runebox.kasm.ir.ref.condition.*;
import io.runebox.kasm.ir.ref.expr.*;
import io.runebox.kasm.ir.ref.invoke.AbstractInstanceInvoke;
import io.runebox.kasm.ir.ref.invoke.AbstractInvoke;
import io.runebox.kasm.ir.ref.invoke.InvokeStatic;
import io.runebox.kasm.ir.ref.stmt.*;
import io.runebox.kasm.ir.stack.StackLocal;
import io.runebox.kasm.ir.stack.insn.IfInsn;
import io.runebox.kasm.ir.stack.insn.Instruction;
import io.runebox.kasm.ir.stack.invoke.Invoke;
import io.runebox.kasm.ir.stack.invoke.SpecialInvoke;
import io.runebox.kasm.ir.util.StackInsnVisitor;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RefInsnWriter extends StackInsnVisitor<Instruction, StackLocal> {
    private final BodyConverter converter;

    /**
     * Instance of the current visited instruction
     */
    private Instruction instruction;

    public RefInsnWriter(BodyConverter converter) {
        super(null);
        this.converter = converter;
    }

    public void setInstruction(Instruction instruction) {
        this.instruction = instruction;
    }

    /**
     * Convert an instruction or else merge the popped values.
     *
     * @param pops values popped from the stack
     * @param convertInstruction lambda that converts the instruction and returns the pushed value
     */
    private void convertOrElseMerge(List<StackValue> pops, Supplier<Optional<StackValue>> convertInstruction) {
        StackDelta delta = converter.getStackDeltaMap().get(instruction);

        if (delta == null) {
            Optional<StackValue> push = convertInstruction.get();
            delta = new StackDelta(pops, push);
            converter.getStackDeltaMap().put(instruction, delta);
        } else {
            delta.merge(converter, pops);
        }

        for (StackValue pop : pops) {
            List<RWCell<Expression>> references = pop.getReferences();

            // if a value is referenced multiple times, move it to a local.
            if (references.size() > 1) {
                Expression value = pop.getValue();
                if (value instanceof ConstantExpr) {
                    // ConstantExpr can just be copied
                    PushableConstant constant = ((ConstantExpr) value).getConstant();

                    references.forEach(cell -> cell.set(new ConstantExpr(constant)));
                } else {
                    pop.storeInLocal(converter, converter.newLocal());
                }
            }
        }

        delta.getPush().ifPresent(converter::push);
    }

    @Override
    public void visitPush(PushableConstant constant) {
        convertOrElseMerge(List.of(), () ->
                Optional.of(new StackValue(instruction, new ConstantExpr(constant))));

        super.visitPush(constant);
    }

    @Override
    public void visitNeg(Type type) {
        StackValue value = converter.pop();
        convertOrElseMerge(List.of(value), () -> {
            NegExpr expr = new NegExpr(value.getValue());
            value.addReference(expr.getValueCell());

            return Optional.of(new StackValue(instruction, expr));
        });

        super.visitNeg(type);
    }

    private void convertBinaryMathExpr(BiFunction<Expression, Expression, AbstractBinaryExpr> newMathExpression) {
        StackValue value2 = converter.pop();
        StackValue value1 = converter.pop();

        convertOrElseMerge(List.of(value2, value1), () -> {
            AbstractBinaryExpr expr = newMathExpression.apply(value1.getValue(), value2.getValue());
            value1.addReference(expr.getValue1Cell());
            value2.addReference(expr.getValue2Cell());

            return Optional.of(new StackValue(instruction, expr));
        });
    }

    @Override
    public void visitAdd(Type type) {
        convertBinaryMathExpr(AddExpr::new);
        super.visitAdd(type);
    }

    @Override
    public void visitSub(Type type) {
        convertBinaryMathExpr(SubExpr::new);
        super.visitSub(type);
    }

    @Override
    public void visitMul(Type type) {
        convertBinaryMathExpr(MulExpr::new);
        super.visitMul(type);
    }

    @Override
    public void visitDiv(Type type) {
        convertBinaryMathExpr(DivExpr::new);
        super.visitDiv(type);
    }

    @Override
    public void visitMod(Type type) {
        convertBinaryMathExpr(ModExpr::new);
        super.visitMod(type);
    }

    @Override
    public void visitAnd(Type type) {
        convertBinaryMathExpr(AndExpr::new);
        super.visitAnd(type);
    }

    @Override
    public void visitOr(Type type) {
        convertBinaryMathExpr(OrExpr::new);
        super.visitOr(type);
    }

    @Override
    public void visitXor(Type type) {
        convertBinaryMathExpr(XorExpr::new);
        super.visitXor(type);
    }

    @Override
    public void visitShl(Type type) {
        convertBinaryMathExpr(ShlExpr::new);
        super.visitShl(type);
    }

    @Override
    public void visitShr(Type type) {
        convertBinaryMathExpr(ShrExpr::new);
        super.visitShr(type);
    }

    @Override
    public void visitUShr(Type type) {
        convertBinaryMathExpr(UShrExpr::new);
        super.visitUShr(type);
    }

    @Override
    public void visitCmp() {
        convertBinaryMathExpr(CmpExpr::new);
        super.visitCmp();
    }

    @Override
    public void visitCmpl(Type type) {
        convertBinaryMathExpr(CmplExpr::new);
        super.visitCmpl(type);
    }

    @Override
    public void visitCmpg(Type type) {
        convertBinaryMathExpr(CmpgExpr::new);
        super.visitCmpg(type);
    }

    @Override
    public void visitNewArray(ArrayType type, int initializedDimensions) {
        List<StackValue> dimensions = new ArrayList<>(initializedDimensions);
        for (int i = 0; i < initializedDimensions; i++) {
            dimensions.add(converter.pop());
        }

        Collections.reverse(dimensions);

        convertOrElseMerge(dimensions, () -> {
            List<Expression> dimensionSizes = dimensions.stream()
                    .map(StackValue::getValue)
                    .collect(Collectors.toList());

            NewArrayExpr expr = new NewArrayExpr(type, dimensionSizes);

            Iterator<StackValue> valueIter = dimensions.iterator();
            Iterator<RWCell<Expression>> cellIter = expr.getDimensionSizeCells().iterator();
            while(valueIter.hasNext()) {
                valueIter.next().addReference(cellIter.next());
            }

            return Optional.of(new StackValue(instruction, expr));
        });
        super.visitNewArray(type, initializedDimensions);
    }

    @Override
    public void visitArrayLength() {
        StackValue array = converter.pop();

        convertOrElseMerge(List.of(array), () -> {
            ArrayLengthExpr expr = new ArrayLengthExpr(array.getValue());

            array.addReference(expr.getArrayCell());

            return Optional.of(new StackValue(instruction, expr));
        });

        super.visitArrayLength();
    }

    @Override
    public void visitArrayLoad(Type type) {
        StackValue index = converter.pop();
        StackValue array = converter.pop();

        convertOrElseMerge(List.of(index, array), () -> {
            ArrayBoxExpr expr = new ArrayBoxExpr(array.getValue(), index.getValue());

            array.addReference(expr.getArrayCell());
            index.addReference(expr.getIndexCell());

            return Optional.of(new StackValue(instruction, expr));
        });

        super.visitArrayLoad(type);
    }

    @Override
    public void visitArrayStore(Type type) {
        StackValue value = converter.pop();
        StackValue index = converter.pop();
        StackValue array = converter.pop();

        convertOrElseMerge(List.of(value, index, array), () -> {
            ArrayBoxExpr expr = new ArrayBoxExpr(array.getValue(), index.getValue());
            AssignStmt statement = new AssignStmt(expr, value.getValue());

            array.addReference(expr.getArrayCell());
            index.addReference(expr.getIndexCell());
            value.addReference(statement.getValueCell());

            converter.addStatement(instruction, statement);
            return Optional.empty();
        });

        super.visitArrayStore(type);
    }

    @Override
    public void visitSwap() {
        StackValue value1 = converter.pop();
        StackValue value2 = converter.pop();
        converter.push(value1);
        converter.push(value2);

        super.visitSwap();
    }

    @Override
    public void visitPop() {
        converter.pop();

        super.visitPop();
    }

    @Override
    public void visitDup() {
        StackValue value = converter.pop();
        converter.push(value);
        converter.push(value);

        super.visitDup();
    }

    @Override
    public void visitDupX1() {
        StackValue value1 = converter.pop();
        StackValue value2 = converter.pop();

        converter.push(value1);

        converter.push(value2);
        converter.push(value1);

        super.visitDupX1();
    }

    @Override
    public void visitDupX2() {
        StackValue value1 = converter.pop();
        StackValue value2 = converter.pop();
        StackValue value3 = converter.pop();

        converter.push(value1);

        converter.push(value3);
        converter.push(value2);
        converter.push(value1);

        super.visitDupX2();
    }

    @Override
    public void visitDup2() {
        StackValue value1 = converter.pop();
        StackValue value2 = converter.pop();

        converter.push(value2);
        converter.push(value1);

        converter.push(value2);
        converter.push(value1);

        super.visitDup2();
    }

    @Override
    public void visitDup2X1() {
        StackValue value1 = converter.pop();
        StackValue value2 = converter.pop();
        StackValue value3 = converter.pop();

        converter.push(value2);
        converter.push(value1);

        converter.push(value3);
        converter.push(value2);
        converter.push(value1);

        super.visitDup2X1();
    }

    @Override
    public void visitDup2X2() {
        StackValue value1 = converter.pop();
        StackValue value2 = converter.pop();
        StackValue value3 = converter.pop();
        StackValue value4 = converter.pop();

        converter.push(value2);
        converter.push(value1);

        converter.push(value4);
        converter.push(value3);
        converter.push(value2);
        converter.push(value1);

        super.visitDup2X2();
    }

    @Override
    public void visitLoad(Type type, StackLocal stackLocal) {
        convertOrElseMerge(List.of(), () -> {
            RefLocal refLocal = converter.getLocal(stackLocal);
            return Optional.of(new StackValue(instruction, refLocal));
        });

        super.visitLoad(type, stackLocal);
    }

    @Override
    public void visitStore(Type type, StackLocal stackLocal) {
        StackValue value = converter.pop();

        convertOrElseMerge(List.of(value), () -> {
            RefLocal refLocal = converter.getLocal(stackLocal);
            AssignStmt statement = new AssignStmt(refLocal, value.getValue());

            value.addReference(statement.getValueCell());

            converter.addStatement(instruction, statement);
            return Optional.empty();
        });

        super.visitStore(type, stackLocal);
    }

    @Override
    public void visitIncrement(StackLocal stackLocal, int value) {
        convertOrElseMerge(List.of(), () -> {
            RefLocal refLocal = converter.getLocal(stackLocal);
            AssignStmt statement = new AssignStmt(refLocal, new AddExpr(refLocal, new ConstantExpr(new IntConstant(1))));

            converter.addStatement(instruction, statement);
            return Optional.empty();
        });

        super.visitIncrement(stackLocal, value);
    }

    @Override
    public void visitNew(Path type) {
        convertOrElseMerge(List.of(), () -> {
            NewExpr expr = new NewExpr(type);

            return Optional.of(new StackValue(instruction, expr));
        });

        super.visitNew(type);
    }

    @Override
    public void visitInstanceOf(RefType type) {
        StackValue value = converter.pop();

        convertOrElseMerge(List.of(value), () -> {
            InstanceOfExpr expr = new InstanceOfExpr(type, value.getValue());

            value.addReference(expr.getValueCell());

            return Optional.of(new StackValue(instruction, expr));
        });

        super.visitInstanceOf(type);
    }

    private void visitCast(Type type) {
        StackValue value = converter.pop();

        convertOrElseMerge(List.of(value), () -> {
            CastExpr expr = new CastExpr(type, value.getValue());

            value.addReference(expr.getValueCell());

            return Optional.of(new StackValue(instruction, expr));
        });
    }

    @Override
    public void visitPrimitiveCast(PrimitiveType from, PrimitiveType to) {
        visitCast(to);

        super.visitPrimitiveCast(from, to);
    }

    @Override
    public void visitReferenceCast(RefType type) {
        visitCast(type);

        super.visitReferenceCast(type);
    }

    @Override
    public void visitReturn(Optional<Type> type) {
        Optional<StackValue> valueOpt = type.map(x -> converter.pop());

        convertOrElseMerge(valueOpt.map(List::of).orElseGet(List::of), () -> {
            ReturnStmt stmt = new ReturnStmt(valueOpt.map(StackValue::getValue));

            valueOpt.ifPresent(value -> value.addReference(stmt.getValueCell().get()));

            converter.addStatement(instruction, stmt);
            return Optional.empty();
        });

        super.visitReturn(type);
    }

    @Override
    public void visitThrow() {
        StackValue exception = converter.pop();

        convertOrElseMerge(List.of(exception), () -> {
            ThrowStmt stmt = new ThrowStmt(exception.getValue());

            exception.addReference(stmt.getValueCell());

            converter.addStatement(instruction, stmt);
            return Optional.empty();
        });

        super.visitThrow();
    }

    @Override
    public void visitMonitorEnter() {
        StackValue value = converter.pop();

        convertOrElseMerge(List.of(value), () -> {
            MonitorEnterStmt stmt = new MonitorEnterStmt(value.getValue());

            value.addReference(stmt.getValueCell());

            converter.addStatement(instruction, stmt);
            return Optional.empty();
        });

        super.visitMonitorEnter();
    }

    @Override
    public void visitMonitorExit() {
        StackValue value = converter.pop();

        convertOrElseMerge(List.of(value), () -> {
            MonitorExitStmt stmt = new MonitorExitStmt(value.getValue());

            value.addReference(stmt.getValueCell());

            converter.addStatement(instruction, stmt);
            return Optional.empty();
        });

        super.visitMonitorExit();
    }

    @Override
    public void visitFieldGet(FieldRef fieldRef, boolean isStatic) {
        if (isStatic) {
            convertOrElseMerge(List.of(), () -> {
                StaticFieldExpr expr = new StaticFieldExpr(fieldRef);
                return Optional.of(new StackValue(instruction, expr));
            });
        } else {
            StackValue instance = converter.pop();
            convertOrElseMerge(List.of(instance), () -> {
                InstanceFieldExpr expr = new InstanceFieldExpr(fieldRef, instance.getValue());

                instance.addReference(expr.getInstanceCell());

                return Optional.of(new StackValue(instruction, expr));
            });
        }

        super.visitFieldGet(fieldRef, isStatic);
    }

    @Override
    public void visitFieldSet(FieldRef fieldRef, boolean isStatic) {
        StackValue value = converter.pop();

        if (isStatic) {
            convertOrElseMerge(List.of(value), () -> {
                StaticFieldExpr expr = new StaticFieldExpr(fieldRef);
                AssignStmt stmt = new AssignStmt(expr, value.getValue());

                value.addReference(stmt.getValueCell());

                converter.addStatement(instruction, stmt);
                return Optional.empty();
            });
        } else {
            StackValue instance = converter.pop();

            convertOrElseMerge(List.of(value, instance), () -> {
                InstanceFieldExpr expr = new InstanceFieldExpr(fieldRef, instance.getValue());
                AssignStmt stmt = new AssignStmt(expr, value.getValue());

                instance.addReference(expr.getInstanceCell());
                value.addReference(stmt.getValueCell());

                converter.addStatement(instruction, stmt);
                return Optional.empty();
            });
        }

        super.visitFieldSet(fieldRef, isStatic);
    }

    @Override
    public void visitInvokeInsn(Invoke stackInvoke) {
        MethodDescriptor descriptor = stackInvoke.getDescriptor();

        List<StackValue> argumentValues = descriptor.getParameterTypes().stream()
                .map(x -> converter.pop())
                .collect(Collectors.toList());
        Collections.reverse(argumentValues);

        Optional<StackValue> instanceOpt = stackInvoke instanceof io.runebox.kasm.ir.stack.invoke.AbstractInstanceInvoke ?
                Optional.of(converter.pop()) : Optional.empty();

        List<StackValue> allPops = new ArrayList<>(argumentValues);
        instanceOpt.ifPresent(allPops::add);
        convertOrElseMerge(allPops, () -> {
            AbstractInvoke refInvoke = convertAbstractInvoke(stackInvoke, argumentValues, instanceOpt);

            if (refInvoke instanceof io.runebox.kasm.ir.ref.invoke.AbstractInstanceInvoke) {
                RWCell<Expression> instanceCell = ((AbstractInstanceInvoke) refInvoke).getInstanceCell();
                instanceOpt.get().addReference(instanceCell);
            }

            Iterator<StackValue> argIter = argumentValues.iterator();
            Iterator<RWCell<Expression>> cellIter = refInvoke.getArgumentCells().iterator();
            while(argIter.hasNext()) {
                argIter.next().addReference(cellIter.next());
            }

            if (descriptor.getReturnType().isPresent()) {
                RefLocal local = converter.newLocal();
                AssignStmt stmt = new AssignStmt(local, new InvokeExpr(refInvoke));
                converter.addStatement(instruction, stmt);
                return Optional.of(new StackValue(instruction, local));
            } else {
                InvokeStmt stmt = new InvokeStmt(refInvoke);
                converter.addStatement(instruction, stmt);
                return Optional.empty();
            }
        });

        super.visitInvokeInsn(stackInvoke);
    }

    private AbstractInvoke convertAbstractInvoke(Invoke stackInvoke, List<StackValue> argumentValues, Optional<StackValue> instanceOpt) {
        List<Expression> arguments = argumentValues.stream()
                .map(StackValue::getValue)
                .collect(Collectors.toList());

        if (stackInvoke instanceof io.runebox.kasm.ir.stack.invoke.AbstractInstanceInvoke) {
            if (stackInvoke instanceof io.runebox.kasm.ir.stack.invoke.InterfaceInvoke) {
                io.runebox.kasm.ir.stack.invoke.InterfaceInvoke interfaceInvoke = (io.runebox.kasm.ir.stack.invoke.InterfaceInvoke) stackInvoke;
                return new io.runebox.kasm.ir.ref.invoke.InvokeInterface(interfaceInvoke.getMethod(), instanceOpt.get().getValue(), arguments);
            } else if (stackInvoke instanceof io.runebox.kasm.ir.stack.invoke.VirtualInvoke) {
                io.runebox.kasm.ir.stack.invoke.VirtualInvoke virtualInvoke = (io.runebox.kasm.ir.stack.invoke.VirtualInvoke) stackInvoke;
                return new io.runebox.kasm.ir.ref.invoke.InvokeVirtual(virtualInvoke.getMethod(), instanceOpt.get().getValue(), arguments);
            } else if (stackInvoke instanceof SpecialInvoke) {
                SpecialInvoke specialInvoke = (SpecialInvoke) stackInvoke;
                return new io.runebox.kasm.ir.ref.invoke.InvokeSpecial(specialInvoke.getMethod(), instanceOpt.get().getValue(), arguments, specialInvoke.isInterface());
            } else {
                throw new AssertionError();
            }
        } else if (stackInvoke instanceof io.runebox.kasm.ir.stack.invoke.StaticInvoke) {
            io.runebox.kasm.ir.stack.invoke.StaticInvoke staticInvoke = (io.runebox.kasm.ir.stack.invoke.StaticInvoke) stackInvoke;
            return new InvokeStatic(staticInvoke.getMethod(), arguments, staticInvoke.isInterface());
        } else if (stackInvoke instanceof io.runebox.kasm.ir.stack.invoke.DynamicInvoke) {
            io.runebox.kasm.ir.stack.invoke.DynamicInvoke dynamicInvoke = (io.runebox.kasm.ir.stack.invoke.DynamicInvoke) stackInvoke;
            return new io.runebox.kasm.ir.ref.invoke.InvokeDynamic(
                    dynamicInvoke.getName(), dynamicInvoke.getDescriptor(), dynamicInvoke.getBootstrapMethod(),
                    dynamicInvoke.getBootstrapArguments(), arguments);
        } else {
            throw new AssertionError();
        }
    }

    @Override
    public void visitGoto(Instruction target) {
        convertOrElseMerge(List.of(), () -> {
            GotoStmt stmt = new GotoStmt(null);

            converter.registerInsnReference(target, stmt.getTargetCell());

            converter.addStatement(instruction, stmt);
            return Optional.empty();
        });

        super.visitGoto(target);
    }

    @Override
    public void visitIf(IfInsn.Condition stackCondition, Instruction target) {
        Optional<StackValue> value2Opt = stackCondition.getCompareValue() instanceof IfInsn.StackValue ?
                Optional.of(converter.pop()) : Optional.empty();

        StackValue value1 = converter.pop();

        List<StackValue> allPops = new ArrayList<>();
        allPops.add(value1);
        value2Opt.ifPresent(allPops::add);

        convertOrElseMerge(allPops, () -> {
            Expression value2 = convertIfCompareValue(stackCondition, value2Opt);
            Condition condition = convertCondition(stackCondition.getComparison(), value1.getValue(), value2);

            value1.addReference(condition.getValue1Cell());
            value2Opt.ifPresent(value -> value.addReference(condition.getValue2Cell()));

            IfStmt stmt = new IfStmt(condition, null);
            converter.registerInsnReference(target, stmt.getTargetCell());
            converter.addStatement(instruction, stmt);
            return Optional.empty();
        });

        super.visitIf(stackCondition, target);
    }

    private Expression convertIfCompareValue(IfInsn.Condition stackCondition, Optional<StackValue> value2Opt) {
        IfInsn.CompareValue compareValue = stackCondition.getCompareValue();
        if (compareValue instanceof IfInsn.StackValue) {
            return value2Opt.get().getValue();
        } else if (compareValue instanceof IfInsn.NullValue) {
            return new ConstantExpr(NullConstant.getInstance());
        } else if (compareValue instanceof IfInsn.ZeroValue) {
            return new ConstantExpr(new IntConstant(0));
        } else {
            throw new AssertionError();
        }
    }

    private Condition convertCondition(IfInsn.Comparison comparison, Expression value1, Expression value2)  {
        if (comparison instanceof IfInsn.EQ) {
            return new Equal(value1, value2);
        } else if (comparison instanceof IfInsn.NE) {
            return new NonEqual(value1, value2);
        } else if (comparison instanceof IfInsn.LE) {
            return new LessEqual(value1, value2);
        } else if (comparison instanceof IfInsn.LT) {
            return new LessThan(value1, value2);
        } else if (comparison instanceof IfInsn.GE) {
            return new GreaterEqual(value1, value2);
        } else if (comparison instanceof IfInsn.GT) {
            return new GreaterThan(value1, value2);
        } else {
            throw new AssertionError();
        }
    }

    @Override
    public void visitSwitch(Map<Integer, Instruction> targetTable, Instruction defaultTarget) {
        StackValue value = converter.pop();

        convertOrElseMerge(List.of(value), () -> {
            LinkedHashMap<Integer, Statement> refBranchTable = new LinkedHashMap<>();
            for (Integer key : targetTable.keySet()) {
                refBranchTable.put(key, null);
            }

            SwitchStmt stmt = new SwitchStmt(value.getValue(), refBranchTable, null);

            converter.registerInsnReference(defaultTarget, stmt.getDefaultTargetCell());
            for (Integer key : targetTable.keySet()) {
                converter.registerInsnReference(targetTable.get(key), RWCell.ofMap(key, refBranchTable, Statement.class));
            }

            converter.addStatement(instruction, stmt);
            return Optional.empty();
        });

        super.visitSwitch(targetTable, defaultTarget);
    }
}
