package io.runebox.kasm.ir.conversion.stack2ref.processor;

import io.runebox.kasm.ir.util.RCell;
import io.runebox.kasm.ir.ref.Expression;
import io.runebox.kasm.ir.ref.RefBody;
import io.runebox.kasm.ir.ref.RefLocal;
import io.runebox.kasm.ir.ref.Statement;
import io.runebox.kasm.ir.ref.stmt.AssignStmt;
import io.runebox.kasm.ir.util.CommonOperations;
import io.runebox.kasm.ir.util.LocalStateAnalysis;
import io.runebox.kasm.ir.util.RefCfgGraph;

import java.util.ArrayList;
import java.util.Set;

/**
 * This postprocessor detects uses of locals that are independent from each other.
 * These locals will then get split into as many locals as possible.
 * This is necessary since one local might otherwise have different types at different locations within the code.
 *
 * Here's pseudocode for one example local that would get split:
 * <pre><code>
 *     local x;
 *
 *     // Block 1
 *     if (...) {
 *         x = 10;
 *     } else {
 *         x = 30;
 *     }
 *     System.out.println(x);
 *
 *     // Block 2
 *     if (...) {
 *         x = "a";
 *     } else {
 *         x = "b";
 *     }
 *     System.out.println(x);
 * </code></pre>
 *
 * First the local <tt>x</tt> is an <tt>int</tt>, then it is a <tt>String</tt>.
 * That makes assigning a type to <tt>x</tt> impossible.
 *
 * Since it is not possible to access values assigned to <tt>x</tt> by the first block from the second block,
 * all uses of <tt>x</tt> in the second block can be replaced against a new variable.
 */
public class LocalPartitioningPostProcessor implements PostProcessor {
    @Override
    public void process(RefBody body) {
        RefCfgGraph graph = new RefCfgGraph(body);
        LocalStateAnalysis stateAnalysis = new LocalStateAnalysis(graph);

        for (RefLocal local : new ArrayList<>(body.getLocals())) {
            LocalStateAnalysis.LocalStates localState = stateAnalysis.getLocalStates(local);
            for (Set<LocalStateAnalysis.State> group : localState.getGroups()) {
                processGroup(body, local, localState, group);
            }
        }
    }

    /**
     * Assign a new local to one group of distinct uses of a local.
     *
     * @param body the body of the method
     * @param local the locals whose states the group represents
     * @param localState a analysis of the states of the local
     * @param group a groups of states that the locals may have
     */
    private void processGroup(RefBody body, RefLocal local, LocalStateAnalysis.LocalStates localState, Set<LocalStateAnalysis.State> group) {
        RefLocal newLocal = getLocalForGroup(body, group);

        // Update the assign instructions
        for (LocalStateAnalysis.State state : group) {
            if (state instanceof LocalStateAnalysis.State.Stmt) {
                AssignStmt assignment = ((LocalStateAnalysis.State.Stmt) state).getStatement();
                assignment.setVariable(newLocal);
            }
        }

        // Update all references within the group that read from the local
        for (LocalStateAnalysis.State state : group) {
            for (Statement statement : localState.getStatement(state)) {
                for (RCell<Expression> cell : statement.getAllReadValueCells()) {
                    if (cell.get() == local) {
                        cell.w(RefLocal.class).set(newLocal);
                    }
                }
            }
        }

        // Removes the old local if it no longer used.
        CommonOperations.removeLocal(body, local);
    }

    /**
     * Get a local for a assign group.
     *
     * @param body the method containing all locals and statements
     * @param assignGroup the assign group for which a local is requested
     * @return a not yet typed local that will be used for this assignment group
     */
    private RefLocal getLocalForGroup(RefBody body, Set<LocalStateAnalysis.State> assignGroup) {
        for (LocalStateAnalysis.State assign : assignGroup) {
            if (assign instanceof LocalStateAnalysis.State.This) {
                return body.getThisLocal().get();
            } else if (assign instanceof LocalStateAnalysis.State.Parameter) {
                return  body.getArgumentLocals().get(((LocalStateAnalysis.State.Parameter) assign).getIndex());
            }
        }

        RefLocal local = new RefLocal(null);
        body.getLocals().add(local);
        return local;
    }
}
