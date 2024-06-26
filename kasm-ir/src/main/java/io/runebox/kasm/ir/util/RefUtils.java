package io.runebox.kasm.ir.util;

import io.runebox.kasm.ir.ref.Expression;
import io.runebox.kasm.ir.ref.RefBody;
import io.runebox.kasm.ir.ref.Statement;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RefUtils {
    /**
     * Get all references to any statement within one method.
     *
     * @param body the body of the method
     * @return all cell references
     */
    public static Stream<RWCell<Statement>> getStatementRefs(RefBody body) {
        Stream<RWCell<Statement>> tryCatchRefs = body.getTryCatchBlocks().stream()
                .flatMap(block -> Stream.of(block.getFirstCell(), block.getLastCell(), block.getHandlerCell()));

        Stream<RWCell<Statement>> lineRefs = body.getLineNumbers().stream()
                .map(RefBody.LineNumber::getStatementCell);

        return Stream.of(tryCatchRefs, lineRefs).reduce(Stream.empty(), Stream::concat);
    }

    /**
     * Get all references to one certain statement within one method.
     *
     * @param refBody body of the method
     * @param statement references to this statement are requested
     * @return all references of the statements
     */
    public static List<RWCell<Statement>> getStatementRefs(RefBody refBody, Statement statement) {
        return getStatementRefs(refBody)
                .filter(cell -> cell.get() == statement)
                .collect(Collectors.toList());
    }

    /**
     * Get a map that zips statement of a method with all cells currently pointing at them.
     *
     * @param refBody body of the method
     * @return statements zip with their cells
     */
    public static Map<Statement, Set<RWCell<Statement>>> getStatementRefMap(RefBody refBody) {
        return getStatementRefs(refBody).collect(Collectors.groupingBy(RWCell::get, Collectors.toSet()));
    }

    /**
     * Get all cells to any expression within one method.
     *
     * @param refBody body of the method
     * @return all expression cells
     */
    public static Stream<RCell<Expression>> getExpressionCells(RefBody refBody) {
        Stream<RCell<Expression>> stmtRefs = refBody.getStatements().stream()
                .flatMap(stmt -> stmt.getAllReadValueCells().stream());

        Stream<RCell<Expression>> tryCatchRefs = refBody.getTryCatchBlocks().stream()
                .map(block -> block.getExceptionLocalCell().r(Expression.class));

        return Stream.concat(stmtRefs, tryCatchRefs);
    }

    /**
     * Get a map that zips expressions of a method with all cells that currently point a them.
     *
     * @param refBody body of the method
     * @return expressions zipped with their cells
     */
    public static Map<Expression, Set<RCell<Expression>>> getExpressionCellMap(RefBody refBody) {
        return getExpressionCells(refBody).collect(Collectors.groupingBy(RCell::get, Collectors.toSet()));
    }
}
