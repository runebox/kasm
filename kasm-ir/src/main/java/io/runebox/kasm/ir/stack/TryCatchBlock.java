package io.runebox.kasm.ir.stack;

import io.runebox.kasm.ir.Path;
import io.runebox.kasm.ir.typeannotation.ExceptionTypeAnnotation;
import io.runebox.kasm.ir.util.RWCell;
import io.runebox.kasm.ir.stack.insn.Instruction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a handler for exceptions that occur within a certain range of instructions.
 */
public class TryCatchBlock {
    /**
     * First instruction in the range of instructions protected by this block
     */
    private Instruction first;

    /**
     * Last instruction in the range of instructions protected by this block.
     */
    private Instruction last;

    /**
     * The jvm branches to this location if an exception was caught.
     */
    private Instruction handler;

    /**
     * Kind of exceptions that get caught by this block or empty
     * to catch any exception (see <tt>finally</tt> blocks).
     */
    private Optional<Path> exceptionType;

    /**
     * Type annotations of the exception type.
     */
    private List<ExceptionTypeAnnotation> typeAnnotations = new ArrayList<>();

    public TryCatchBlock(Instruction first, Instruction last, Instruction handler, Optional<Path> exceptionType) {
        this.first = first;
        this.last = last;
        this.handler = handler;
        this.exceptionType = exceptionType;
    }

    public Instruction getFirst() {
        return first;
    }

    public void setFirst(Instruction first) {
        this.first = first;
    }

    public RWCell<Instruction> getFirstCell() {
        return RWCell.of(this::getFirst, this::setFirst, Instruction.class);
    }

    public Instruction getLast() {
        return last;
    }

    public void setLast(Instruction last) {
        this.last = last;
    }

    public RWCell<Instruction> getLastCell() {
        return RWCell.of(this::getLast, this::setLast, Instruction.class);
    }

    public Instruction getHandler() {
        return handler;
    }

    public void setHandler(Instruction handler) {
        this.handler = handler;
    }

    public RWCell<Instruction> getHandlerCell() {
        return RWCell.of(this::getHandler, this::setHandler, Instruction.class);
    }

    public Optional<Path> getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(Optional<Path> exceptionType) {
        this.exceptionType = exceptionType;
    }

    public List<ExceptionTypeAnnotation> getTypeAnnotations() {
        return typeAnnotations;
    }

    public void setTypeAnnotations(List<ExceptionTypeAnnotation> typeAnnotations) {
        this.typeAnnotations = typeAnnotations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TryCatchBlock that = (TryCatchBlock) o;
        return Objects.equals(first, that.first) &&
                Objects.equals(last, that.last) &&
                Objects.equals(handler, that.handler) &&
                Objects.equals(exceptionType, that.exceptionType) &&
                Objects.equals(typeAnnotations, that.typeAnnotations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, last, handler, exceptionType, typeAnnotations);
    }
}
