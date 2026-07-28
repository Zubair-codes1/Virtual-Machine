package zplusplus.code_gen;

import zplusplus.ast.*;
import zplusplus.exceptions.CodeGenException;
import zplusplus.sem_analysis.Environment;

import java.util.List;

/**
 * Class to handle code generation into my custom VM
 * assembly language (ZASM).
 *
 * @author Zubair Abdul Matin
 */
public class CodeGenerator {

    private StringBuilder assemblyString = new StringBuilder();

    /**
     * Code generator constructor
     */
    public CodeGenerator() {

    }

    public String generate(List<Statement> statements, Environment environment) {

        if (statements.isEmpty() || environment == null) {
            throw new CodeGenException(
                    "Cannot generate code for empty file",
                    0
            );
        }

        emitGlobalVariables(statements, environment);

        return assemblyString.toString();
    }

    private void emitGlobalVariables(List<Statement> statements, Environment environment) {
        for (Statement statement : statements) {
            if (statement instanceof VariableDeclarationStatement stmt && environment.isGlobal(stmt.getVarName())) {
                if (stmt.getInitializer() != null) {
                    generateExpression(stmt.getInitializer());
                }else {
                    emitDefaultValue(stmt);
                }
            }
        }
    }

    private void emitDefaultValue(VariableDeclarationStatement statement) {
        switch (statement.getTypeName().toLowerCase()) {
            case "int", "bool" -> emitInstruction("PUSH", "0");
            case "string" -> emitInstruction("PUSH_STR", "\"\"");
            default -> throw new CodeGenException(
                    "Code Generation Error: Invalid type: " + statement.getTypeName(),
                    statement.getLineNumber()
            );
        }

        emitInstruction("STORE", statement.getVarName());
    }

    private void emitInstruction(String instruction, String operand) {
        assemblyString.append("\t").append(instruction).append(" ").append(operand).append("\n");
    }

    private void generateExpression(Expression expression) {
        if (expression instanceof LiteralExpression literal) {
            generateLiteralExpression(literal);
        }else if (expression instanceof VariableExpression variable) {
            generateVariableExpression(variable);
        }else if (expression instanceof BinaryExpression binary) {
            generateBinaryExpression(binary);
        }else if (expression instanceof UnaryExpression unary) {
            generateUnaryExpression(unary);
        }else if (expression instanceof GroupingExpression grouping) {
            generateGroupingExpression(grouping);
        }else if (expression instanceof CallingExpression calling) {
            generateCallingExpression(calling);
        }
    }

    private void generateLiteralExpression(LiteralExpression literal) {}

    private void generateVariableExpression(VariableExpression variable) {}

    private void generateBinaryExpression(BinaryExpression binary) {}

    private void generateUnaryExpression(UnaryExpression unary) {}

    private void generateGroupingExpression(GroupingExpression grouping) {}

    private void generateCallingExpression(CallingExpression calling) {}


}
