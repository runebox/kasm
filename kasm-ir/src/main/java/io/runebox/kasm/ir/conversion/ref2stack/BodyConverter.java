package io.runebox.kasm.ir.conversion.ref2stack;

import io.runebox.kasm.ir.Path;
import io.runebox.kasm.ir.type.ObjectType;
import io.runebox.kasm.ir.type.Type;
import io.runebox.kasm.ir.typeannotation.LocalVariableTypeAnnotation;
import io.runebox.kasm.ir.ref.RefBody;
import io.runebox.kasm.ir.ref.RefLocal;
import io.runebox.kasm.ir.ref.Statement;
import io.runebox.kasm.ir.ref.TryCatchBlock;
import io.runebox.kasm.ir.util.RefCfgGraph;
import io.runebox.kasm.ir.stack.StackBody;
import io.runebox.kasm.ir.stack.StackLocal;
import io.runebox.kasm.ir.stack.insn.GotoInsn;
import io.runebox.kasm.ir.stack.insn.Instruction;
import io.runebox.kasm.ir.stack.insn.StoreInsn;

import java.util.*;
import java.util.stream.Collectors;

public class BodyConverter {
    private final RefBody refBody;
    private final StackBody stackBody;
    private final RefCfgGraph graph;

    private ConversionContext ctx;
    private Map<Statement, List<Instruction>> convertedStatements;

    public BodyConverter(RefBody refBody) {
        this(refBody, new StackBody());
    }

    public BodyConverter(RefBody refBody, StackBody stackBody) {
        this.refBody = refBody;
        this.stackBody = stackBody;
        this.graph = new RefCfgGraph(refBody);
    }

    public StackBody getStackBody() {
        return stackBody;
    }

    public RefBody getRefBody() {
        return refBody;
    }

    public void convert() {
        this.ctx = new ConversionContext();
        this.convertedStatements = new HashMap<>();

        convertLocals();

        convertInstructions();
        resolveInsnReferences();
        insertConvertedInstructions();

        convertTryCatchBlocks();
        convertLocalVariables();
        convertLocalVariableAnnotations();
        convertLineNumbers();
    }

    private void convertLocals() {
        Map<RefLocal, StackLocal> localMap = ctx.getLocalMap();
        List<StackLocal> locals = this.stackBody.getLocals();

        for (RefLocal refLocal : refBody.getLocals()) {
            StackLocal stackLocal = new StackLocal();
            localMap.put(refLocal, stackLocal);
            locals.add(stackLocal);
        }

        Optional<StackLocal> thisLocalOpt = refBody.getThisLocal().map(ctx::getStackLocal);
        List<StackLocal> parameterLocals = this.refBody.getArgumentLocals().stream()
                .map(ctx::getStackLocal)
                .collect(Collectors.toList());

        this.stackBody.setThisLocal(thisLocalOpt);
        this.stackBody.setParameterLocals(parameterLocals);
    }

    private void convertInstructions() {
        Deque<RefCfgGraph.Node> worklist = new ArrayDeque<>();
        Set<RefCfgGraph.Node> visited = new HashSet<>();

        worklist.add(graph.getNode(refBody.getStatements().getFirst()));
        for (TryCatchBlock tryCatchBlock : refBody.getTryCatchBlocks()) {
            worklist.add(graph.getNode(tryCatchBlock.getHandler()));
        }

        StackInsnWriter writer = new StackInsnWriter(ctx);
        RefInsnReader reader = new RefInsnReader(writer);

        RefCfgGraph.Node node;
        while ((node = worklist.poll()) != null) {
            Statement statement = node.getInstruction();

            if (!visited.add(node)) {
                continue;
            }

            reader.accept(statement);
            List<Instruction> instructions = writer.getInstructions();
            convertedStatements.put(statement, new ArrayList<>(instructions));
            instructions.clear();

            worklist.addAll(node.getSucceeding());
        }
    }

    private void resolveInsnReferences() {
        ctx.getInstructionsRefs().forEach((stmt, cells) -> {
            Instruction instruction = convertedStatements.get(stmt).get(0);
            cells.forEach(cell -> cell.set(instruction));
        });
    }

    private void insertConvertedInstructions() {
        for (Statement statement : refBody.getStatements()) {
            List<Instruction> instructions = convertedStatements.getOrDefault(statement, Collections.emptyList());
            stackBody.getInstructions().addAll(instructions);
        }
    }

    /**
     * Find the {@link Instruction} that is equivalent to a {@link Statement}.
     *
     * @param statement the {@link Statement}
     * @return the equivalent {@link Instruction}
     */
    private Instruction getInstruction(Statement statement) {
        return this.convertedStatements.get(statement).get(0);
    }

    private void convertTryCatchBlocks() {
        // In the ref intermediation caught exceptions a stored in locals while they are put onto
        // the stack in the stack intermediation. Therefore a StoreInsn that pops the caught exception
        // from the stack and stores it in the corresponding local has to be inserted for each handler.

        refBody.getTryCatchBlocks().stream()
                .collect(Collectors.groupingBy(TryCatchBlock::getHandler))
                .forEach((handler, blocksForHandler) -> {
            Instruction handlerInsn = getInstruction(handler);
            Map<RefLocal, List<TryCatchBlock>> handlersByLocal = blocksForHandler.stream()
                    .collect(Collectors.groupingBy(TryCatchBlock::getExceptionLocal));

            RefCfgGraph.Node handlerNode = graph.getNode(handler);
            if (handlersByLocal.size() == 1 && handlerNode.getSucceeding().isEmpty()) {
                // Since each try/catch blocks pointing at this handler instructions has the same local,
                // we will insert the StoreInsn directly before the actual handler instruction.

                Map.Entry<RefLocal, List<TryCatchBlock>> onlyEntry = handlersByLocal.entrySet().iterator().next();
                RefLocal local = onlyEntry.getKey();
                List<TryCatchBlock> blocks = onlyEntry.getValue();

                StoreInsn assignInsn = new StoreInsn(ObjectType.OBJECT, ctx.getStackLocal(local));
                stackBody.getInstructions().insertBefore(handlerInsn, assignInsn);

                convertTryCatchBlocks(blocks, assignInsn);
            } else {
                // There are multiple try/catch blocks pointing at this handler instruction each
                // having different locals. We will append the corresponding StoreInsn at the end of
                // the method followed by a goto to the actual handler instruction.

                handlersByLocal.forEach((local, blocksForLocal) -> {
                    StoreInsn assignInsn = new StoreInsn(ObjectType.OBJECT, ctx.getStackLocal(local));
                    GotoInsn gotoInsn = new GotoInsn(handlerInsn);
                    stackBody.getInstructions().addAll(List.of(assignInsn, gotoInsn));

                    convertTryCatchBlocks(blocksForLocal, assignInsn);
                });
            }
        });
    }

    private void convertTryCatchBlocks(List<TryCatchBlock> blocks, StoreInsn handlerInsn) {
        for (TryCatchBlock refBlock : blocks) {
            Instruction firstInsn = getInstruction(refBlock.getFirst());
            Instruction lastInsn = getInstruction(refBlock.getLast());
            Optional<Path> exception = refBlock.getException();

            stackBody.getTryCatchBlocks().add(new io.runebox.kasm.ir.stack.TryCatchBlock(firstInsn, lastInsn, handlerInsn, exception));
        }
    }

    private void convertLocalVariables() {
        for (RefBody.LocalVariable refVar : refBody.getLocalVariables()) {
            String name = refVar.getName();
            Type type = refVar.getType();
            Optional<String> signature = refVar.getSignature();
            Instruction start = getInstruction(refVar.getStart());
            Instruction end = getInstruction(refVar.getEnd());
            StackLocal local = ctx.getStackLocal(refVar.getLocal());

            stackBody.getLocalVariables().add(new StackBody.LocalVariable(name, type, signature, start, end, local));
        }
    }

    private void convertLocalVariableAnnotations() {
        for (RefBody.LocalVariableAnnotation refVarAnno : refBody.getLocalVariableAnnotations()) {
            LocalVariableTypeAnnotation annotation = refVarAnno.getAnnotation();
            List<StackBody.LocalVariableAnnotation.Location> stackLocations = refVarAnno.getLocations().stream()
                    .map(location -> {
                        Instruction start = getInstruction(location.getStart());
                        Instruction end = getInstruction(location.getEnd());
                        StackLocal local = ctx.getStackLocal(location.getLocal());
                        return new StackBody.LocalVariableAnnotation.Location(start, end, local);
                    }).collect(Collectors.toList());

            stackBody.getLocalVariableAnnotations().add(new StackBody.LocalVariableAnnotation(annotation, stackLocations));
        }
    }

    private void convertLineNumbers() {
        for (RefBody.LineNumber lineNumber : refBody.getLineNumbers()) {
            int line = lineNumber.getLine();
            Instruction instruction = getInstruction(lineNumber.getStatement());

            stackBody.getLineNumbers().add(new StackBody.LineNumber(line, instruction));
        }
    }
}
