package zplusplus.code_gen;

import zplusplus.ast.Statement;
import zplusplus.ast.VariableDeclarationStatement;
import zplusplus.exceptions.CodeGenException;
import zplusplus.sem_analysis.Environment;
import zplusplus.sem_analysis.symbol.Symbol;
import zplusplus.sem_analysis.symbol.VariableSymbol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                emitDefaultValue(stmt.getTypeName(), stmt.getVarName());
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
}
