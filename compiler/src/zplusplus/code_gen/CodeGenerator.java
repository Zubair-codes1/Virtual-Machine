package zplusplus.code_gen;

import zplusplus.ast.Statement;
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

        emitGlobalVariables(environment);

        return assemblyString.toString();
    }

    private void emitGlobalVariables(Environment environment) {
        HashMap<String, Symbol> symbolTable = (HashMap<String, Symbol>) environment.getTable();

        for (Map.Entry<String, Symbol> entry : symbolTable.entrySet()) {
            if (environment.isGlobal(entry.getKey()) && entry.getValue() instanceof VariableSymbol) {
                emitDefaultValue(entry.getValue().getType().toString());
            }
        }
    }

    private void emitDefaultValue(String typeName) {
        switch (typeName.toLowerCase()) {
            case "int", "bool" -> emitInstruction("PUSH", "0");
            case "string" -> emitInstruction("PUSH_STR", "\"\"");
        }
    }

    private void emitInstruction(String instruction, String operand) {
        assemblyString.append(instruction).append(" ").append(operand);
    }
}
