package zplusplus.code_gen;

import zplusplus.ast.*;
import zplusplus.exceptions.CodeGenException;
import zplusplus.lexer.Token;
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
    private int labelCounter = 0;

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
                    generateExpression(stmt.getInitializer(), environment);
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

    private void generateExpression(Expression expression, Environment environment) {
        if (expression instanceof LiteralExpression literal) {
            generateLiteralExpression(literal);
        }else if (expression instanceof VariableExpression variable) {
            generateVariableExpression(variable, environment);
        }else if (expression instanceof BinaryExpression binary) {
            generateBinaryExpression(binary, environment);
        }else if (expression instanceof UnaryExpression unary) {
            generateUnaryExpression(unary, environment);
        }else if (expression instanceof GroupingExpression grouping) {
            generateGroupingExpression(grouping, environment);
        }else if (expression instanceof CallingExpression calling) {
            generateCallingExpression(calling, environment);
        }
    }

    private void generateLiteralExpression(LiteralExpression literal) {
        if (literal.getValue() instanceof Integer) {
            emitInstruction("PUSH", literal.getValue().toString());
        }else if (literal.getValue() instanceof Boolean) {
            String boolValue = (Boolean) literal.getValue() ? "1" : "0";
            emitInstruction("PUSH", literal.getValue().toString());
        }else if (literal.getValue() instanceof String) {
            emitInstruction("PUSH_STR", literal.getValue().toString());
        }else {
            throw new CodeGenException(
                    "Code Generation Error: Invalid literal: " + literal.getValue(),
                    literal.getLineNumber()
            );
        }
    }

    private void generateVariableExpression(VariableExpression variable, Environment environment) {
        if (environment.isGlobal(variable.getName())) {
            emitInstruction("LOAD",  variable.getName());
        }else {
            int slot = environment.getLocalSlot(variable.getName());
            emitInstruction("LOAD_LOCAL", String.valueOf(slot));
        }
    }

    private void generateBinaryExpression(BinaryExpression binary, Environment environment) {
        generateExpression(binary.getLeft(), environment);
        generateExpression(binary.getRight(), environment);

        Token operator = binary.getOperator();
        switch(operator.tokenValue()) {
            case "+" -> emitInstruction("ADD" ,"");
            case "-" -> emitInstruction("SUB" ,"");
            case "*" -> emitInstruction("MULT" ,"");
            case "/" -> emitInstruction("DIV" ,"");
            case "%" -> emitInstruction("MOD" ,"");
            case ">=" -> emitInstruction("GTE" ,"");
            case "<=" -> emitInstruction("LTE" ,"");
            case ">" -> emitInstruction("GT" ,"");
            case "<" -> emitInstruction("LT" ,"");
            case "==" -> emitInstruction("EQ" ,"");
            case "!=" -> emitInstruction("NEQ" ,"");
            case "&&" -> generateLogicalAnd(binary, environment);
            case "||" -> generateLogicalOr(binary, environment);
            case "&" -> emitInstruction("AND" ,"");
            case "|" -> emitInstruction("OR" ,"");
            case "^" -> emitInstruction("XOR" ,"");
            default -> throw new CodeGenException(
                    "Code Generator Error: Invalid binary operator " + operator.tokenValue(),
                    binary.getLineNumber()
            );
        }
    }

    private String createUniqueLabel(String prefix) {
        return ":" + prefix + "_" + (labelCounter++);
    }

    private void emitLabel(String labelName) {
        assemblyString.append(labelName).append(":\n");
    }

    private void generateLogicalAnd(BinaryExpression expr, Environment env) {
        String falseLabel = createUniqueLabel("and_false");
        String endLabel = createUniqueLabel("and_end");

        // Evaluate Left
        generateExpression(expr.getLeft(), env);
        emitInstruction("JIF", falseLabel);

        // Evaluate Right (only reached if left was true/non-zero)
        generateExpression(expr.getRight(), env);
        emitInstruction("JIF", falseLabel);

        // if both true
        emitInstruction("PUSH", "1");
        emitInstruction("JUMP", endLabel);

        // False block
        emitLabel(falseLabel);
        emitInstruction("PUSH", "0");

        emitLabel(endLabel);
    }

    private void generateLogicalOr(BinaryExpression binary, Environment environment) {
        String trueLabel = createUniqueLabel("or_false");
        String endLabel = createUniqueLabel("or_end");

        generateExpression(binary.getLeft(), environment);
        emitInstruction("JIT", trueLabel);

        generateExpression(binary.getRight(), environment);
        emitInstruction("JIT", trueLabel);

        emitInstruction("PUSH", "0");
        emitInstruction("JUMP", endLabel);

        emitLabel(trueLabel);
        emitInstruction("PUSH", "1");

        emitLabel(endLabel);
    }

    private void generateUnaryExpression(UnaryExpression unary, Environment environment) {
        generateExpression(unary.getRightExpression(), environment);

        switch (unary.getOperator().tokenValue()) {
            case "~" -> emitInstruction("NOT", "");
            case "!" -> {
                emitInstruction("PUSH", "0");
                emitInstruction("NOT", "");
            }
            case "-" -> {
                // Transform -x into (0 - x)
                emitInstruction("PUSH", "0");
                emitInstruction("SWAP", "");
                emitInstruction("SUB", "");
            }
        }
    }

    private void generateGroupingExpression(GroupingExpression grouping, Environment environment) {
        generateExpression(grouping.getExpression(), environment);
    }

    private void generateCallingExpression(CallingExpression calling, Environment environment) {

        for (Expression argument:  calling.getArguments()) {
            generateExpression(argument, environment);
        }

        String functionLabel = ":" + calling.getCallee();
        emitInstruction("CALL", functionLabel);
    }

}
