package zplusplus.code_gen;

import zplusplus.ast.Expression;
import zplusplus.ast.Statement;
import zplusplus.ast.VariableDeclarationStatement;
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
                    emitDefaultValue(stmt.getTypeName(), stmt.getVarName());
                }
            }
        }
    }

    private void emitDefaultValue(String typeName, String variableName) {
        switch (typeName.toLowerCase()) {
            case "int", "bool" -> emitInstruction("PUSH", "0");
            case "string" -> emitInstruction("PUSH_STR", "\"\"");
        }

        emitInstruction("STORE", variableName);
    }

    private void emitInstruction(String instruction, String operand) {
        assemblyString.append("\t").append(instruction).append(" ").append(operand).append("\n");
    }

    private void generateExpression(Expression expression) {

    }
}
