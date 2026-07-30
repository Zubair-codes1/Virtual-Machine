package zplusplus.code_gen;

import zplusplus.ast.*;
import zplusplus.exceptions.CodeGenException;
import zplusplus.lexer.Token;
import zplusplus.sem_analysis.Environment;

import java.util.List;
import java.util.Stack;

/**
 * Class to handle code generation into my custom VM
 * assembly language (ZASM).
 *
 * @author Zubair Abdul Matin
 */
public class CodeGenerator {

    private StringBuilder assemblyString = new StringBuilder();
    private int labelCounter = 0;
    private Stack<String> breakStack = new Stack<>();

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

                emitInstruction("STORE", stmt.getVarName());
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
    }

    /**
     * emit instruction with operand
     * @param instruction instruction
     * @param operand operand
     */
    private void emitInstruction(String instruction, String operand) {
        assemblyString.append("\t").append(instruction).append(" ").append(operand).append("\n");
    }

    /**
     * Overridden emit instructino with no operand
     * @param instruction instruction
     */
    private void emitInstruction(String instruction) {
        assemblyString.append("\t").append(instruction).append("\n");
    }

    private void generateStatement(Statement statement, Environment environment) {
        if (statement instanceof VariableDeclarationStatement varDeclStmt) {
            generateVarDeclStmt(varDeclStmt, environment);
        } else if (statement instanceof ExpressionStatement expressionStmt) {
            generateExprStmt(expressionStmt, environment);
        } else if (statement instanceof AssignmentStatement assignmentStmt) {
            generateAssignStmt(assignmentStmt, environment);
        } else if (statement instanceof IfStatement ifStatement) {
            generateIfStmt(ifStatement, environment);
        } else if  (statement instanceof WhileStatement whileStatement) {
            generateWhileStmt(whileStatement, environment);
        } else if  (statement instanceof ForStatement forStatement) {
            generateForStmt(forStatement, environment);
        } else if (statement instanceof BlockStatement blockStmt) {
            generateBlockStmt(blockStmt, environment);
        } else if (statement instanceof BreakStatement breakStmt) {
            generateBreakStmt(breakStmt, environment);
        } else if  (statement instanceof ReturnStatement returnStmt) {
            generateReturnStmt(returnStmt, environment);
        } else if (statement instanceof FunctionDeclarationStatement functionStmt) {
            generateFuncDeclStmt(functionStmt, environment);
        }
    }

    private void generateVarDeclStmt(VariableDeclarationStatement varDeclstmt, Environment environment) {
        Expression initializer = varDeclstmt.getInitializer();
        if (initializer != null) {
            generateExpression(initializer, environment);
        }else {
            emitDefaultValue(varDeclstmt);
        }

        int localSlot = environment.getLocalSlot(varDeclstmt.getVarName());
        emitInstruction("STORE_LOCAL", String.valueOf(localSlot));
    }

    private void generateExprStmt(ExpressionStatement exprStmt, Environment environment) {}

    private void generateAssignStmt(AssignmentStatement assignmentStmt, Environment environment) {
        generateExpression(assignmentStmt.getExpression(), environment);

        if (environment.isGlobal(assignmentStmt.getName())) {
            emitInstruction("STORE", assignmentStmt.getName());
        }else {
            int localSlot = environment.getLocalSlot(assignmentStmt.getName());
            emitInstruction("STORE_LOCAL", String.valueOf(localSlot));
        }
    }

    private void generateIfStmt(IfStatement ifStmt, Environment environment) {
        String elseLabel = createUniqueLabel("elseLabel");
        String endLabel = createUniqueLabel("endLabel");

        generateExpression(ifStmt.getCondition(), environment);

        if (ifStmt.getElseStatement() != null) {
            emitInstruction("JIF", elseLabel);
            generateStatement(ifStmt.getIfStatement(), environment);

            emitLabel(elseLabel);
            generateStatement(ifStmt.getElseStatement(), environment);
        }

        emitLabel(endLabel);
    }

    private void generateWhileStmt(WhileStatement whileStmt, Environment environment) {
        String startLabel = createUniqueLabel("startLabel");
        String endLabel = createUniqueLabel("endLabel");

        breakStack.push(endLabel);

        emitLabel(startLabel);

        generateExpression(whileStmt.getCondition(), environment);
        emitInstruction("JIF", endLabel);

        generateStatement(whileStmt.getBody(), environment);
        emitInstruction("JUMP", startLabel);

        emitLabel(endLabel);
        breakStack.pop();
    }

    private void generateForStmt(ForStatement forStmt, Environment environment) {

        generateStatement(forStmt.getInitializer(), environment);

        String startLabel = createUniqueLabel("startLabel");
        String endLabel = createUniqueLabel("endLabel");

        breakStack.push(startLabel);

        emitLabel(startLabel);

        generateExpression(forStmt.getCondition(), environment);
        emitInstruction("JIF", endLabel);

        generateStatement(forStmt.getBody(), environment);
        generateStatement(forStmt.getIncrement(), environment);

        emitInstruction("JUMP", startLabel);
        emitLabel(endLabel);

        breakStack.pop();
    }

    private void generateBlockStmt(BlockStatement blockStmt, Environment environment) {
        for (Statement statement : blockStmt.getStatements()) {
            generateStatement(statement, environment);
        }
    }

    private void generateBreakStmt(BreakStatement breakStmt, Environment environment) {
        String currentEndLabel = breakStack.peek();
        emitInstruction("JUMP", currentEndLabel);
    }

    private void generateReturnStmt(ReturnStatement returnStmt, Environment environment) {
        if (returnStmt.getReturnValue() != null) {
            generateExpression(returnStmt.getReturnValue(), environment);
        }

        emitInstruction("RET");
    }

    private void generateFuncDeclStmt(FunctionDeclarationStatement funcDeclStmt, Environment environment) {
        emitLabel(funcDeclStmt.getName());

        for (int i = funcDeclStmt.getParameters().size() - 1; i >= 0; i--) {
            emitInstruction("STORE_LOCAL", String.valueOf(i));
        }

        generateStatement(funcDeclStmt.getBody(), environment);

        emitInstruction("RET");
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
            emitInstruction("PUSH", boolValue);
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
        Token operator = binary.getOperator();

        if (operator.tokenValue().equals("&&")) {
            generateLogicalAnd(binary, environment);
        }else if (operator.tokenValue().equals("||")) {
            generateLogicalOr(binary, environment);
        }else {
            generateExpression(binary.getLeft(), environment);
            generateExpression(binary.getRight(), environment);


            switch(operator.tokenValue()) {
                case "+" -> emitInstruction("ADD");
                case "-" -> emitInstruction("SUB");
                case "*" -> emitInstruction("MULT");
                case "/" -> emitInstruction("DIV");
                case "%" -> emitInstruction("MOD");
                case ">=" -> emitInstruction("GTE");
                case "<=" -> emitInstruction("LTE");
                case ">" -> emitInstruction("GT");
                case "<" -> emitInstruction("LT");
                case "==" -> emitInstruction("EQ");
                case "!=" -> emitInstruction("NEQ");
                case "&" -> emitInstruction("AND");
                case "|" -> emitInstruction("OR");
                case "^" -> emitInstruction("XOR");
                default -> throw new CodeGenException(
                        "Code Generator Error: Invalid binary operator " + operator.tokenValue(),
                        binary.getLineNumber()
                );
            }
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
            case "~" -> emitInstruction("NOT");
            case "!" -> {
                emitInstruction("PUSH", "0");
                emitInstruction("EQ");
            }
            case "-" -> {
                // Transform -x into (0 - x)
                emitInstruction("PUSH", "0");
                emitInstruction("SWAP");
                emitInstruction("SUB");
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
