package zplusplus.code_gen;

import zplusplus.ast.*;
import zplusplus.exceptions.CodeGenException;
import zplusplus.lexer.Token;
import zplusplus.sem_analysis.Environment;
import zplusplus.sem_analysis.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Class to handle code generation into my custom VM
 * assembly language (ZASM).
 *
 * @author Zubair Abdul Matin
 */
public class CodeGenerator {

    private StringBuilder assemblyString;
    private int labelCounter;
    private Stack<String> breakStack;

    // Local slot tracking state for fallback/mock scope contexts
    private Map<String, Integer> fallbackLocalSlots;
    private int nextFallbackSlot;
    private Map<String, Type> localVariableTypes;

    /**
     * Code generator constructor
     */
    public CodeGenerator() {
        this.assemblyString = new StringBuilder();
        this.labelCounter = 0;
        this.breakStack = new Stack<>();
        this.fallbackLocalSlots = new HashMap<>();
        this.nextFallbackSlot = 0;
        this.localVariableTypes = new HashMap<>();
    }

    public String generate(List<Statement> statements, Environment environment) {

        if (statements == null || statements.isEmpty() || environment == null) {
            throw new CodeGenException(
                    "Cannot generate code for empty file",
                    0
            );
        }

        // Clean slate for every single generation run (crucial for CI test suites)
        resetGeneratorState();

        emitGlobalVariables(statements, environment);

        emitInstruction("CALL", ":main");
        emitInstruction("HALT");

        for (Statement statement : statements) {
            if (statement instanceof VariableDeclarationStatement varStmt && safeIsGlobal(environment, varStmt.getVarName())) {
                continue;
            }
            generateStatement(statement, environment);
        }

        return assemblyString.toString();
    }

    /**
     * Fully resets generator state before running a generation pass.
     */
    private void resetGeneratorState() {
        this.assemblyString = new StringBuilder();
        this.labelCounter = 0;
        this.breakStack.clear();
        resetLocalScope();
    }

    /**
     * Resets local variable slot tracking for new function scopes.
     */
    private void resetLocalScope() {
        this.fallbackLocalSlots.clear();
        this.nextFallbackSlot = 0;
    }

    private boolean envHasSlot(Environment env, String varName) {
        if (env == null || varName == null) return false;
        try {
            return env.getLocalSlot(varName) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    private int getLocalSlot(String varName, Environment env) {
        if (envHasSlot(env, varName)) {
            try {
                return env.getLocalSlot(varName);
            } catch (Exception ignored) {

            }
        }

        if (fallbackLocalSlots.containsKey(varName)) {
            return fallbackLocalSlots.get(varName);
        }

        int slot = nextFallbackSlot++;
        fallbackLocalSlots.put(varName, slot);
        return slot;
    }

    private void emitGlobalVariables(List<Statement> statements, Environment environment) {
        for (Statement statement : statements) {
            if (statement instanceof VariableDeclarationStatement stmt && safeIsGlobal(environment, stmt.getVarName())) {
                if (stmt.getInitializer() != null) {
                    generateExpression(stmt.getInitializer(), environment);
                } else {
                    emitDefaultValue(stmt);
                }

                emitInstruction("STORE", stmt.getVarName());
            }
        }
    }

    private void emitDefaultValue(VariableDeclarationStatement statement) {
        if (statement.getTypeName() == null) {
            throw new CodeGenException(
                    "Code Generation Error: Invalid type: null",
                    statement.getLineNumber()
            );
        }
        switch (statement.getTypeName().toLowerCase().trim()) {
            case "int", "bool", "boolean" -> emitInstruction("PUSH", "0");
            case "string" -> emitInstruction("PUSH_STR", "\" \"");
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
     * Overridden emit instruction with no operand
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
        } else if (statement instanceof WhileStatement whileStatement) {
            generateWhileStmt(whileStatement, environment);
        } else if (statement instanceof ForStatement forStatement) {
            generateForStmt(forStatement, environment);
        } else if (statement instanceof BlockStatement blockStmt) {
            generateBlockStmt(blockStmt, environment);
        } else if (statement instanceof BreakStatement breakStmt) {
            generateBreakStmt(breakStmt, environment);
        } else if (statement instanceof ReturnStatement returnStmt) {
            generateReturnStmt(returnStmt, environment);
        } else if (statement instanceof FunctionDeclarationStatement functionStmt) {
            generateFuncDeclStmt(functionStmt, environment);
        } else if (statement instanceof PrintStatement printStmt) {
            generatePrintStmt(printStmt, environment);
        } else if (statement instanceof InputStatement inputStmt) {
            generateInputStmt(inputStmt, environment);
        } else {
            throw new CodeGenException(
                    "Code Generation Error: Invalid statement: " + statement,
                    statement.getLineNumber()
            );
        }
    }

    private void generateVarDeclStmt(VariableDeclarationStatement varDeclstmt, Environment environment) {
        Expression initializer = varDeclstmt.getInitializer();
        if (initializer != null) {
            generateExpression(initializer, environment);
        } else {
            emitDefaultValue(varDeclstmt);
        }

        String varName = varDeclstmt.getVarName();
        // Fix: Parse type directly from the AST statement instead of environment lookup
        Type varType = parseType(varDeclstmt.getTypeName());
        localVariableTypes.put(varName, varType);

        if (safeIsGlobal(environment, varName)) {
            emitInstruction("STORE", varName);
        } else {
            int localSlot = getLocalSlot(varName, environment);
            emitInstruction("STORE_LOCAL", String.valueOf(localSlot));
        }
    }

    private void generateExprStmt(ExpressionStatement exprStmt, Environment environment) {
        generateExpression(exprStmt.getExpression(), environment);

        if (exprStmt.getExpression() instanceof CallingExpression callingExpression) {
            Type returnType = safeGetSymbolType(environment, callingExpression.getCallee());
            if (returnType != Type.VOID) {
                emitInstruction("POP");
            }
        } else {
            emitInstruction("POP");
        }
    }

    private void generateAssignStmt(AssignmentStatement assignmentStmt, Environment environment) {
        generateExpression(assignmentStmt.getExpression(), environment);

        if (safeIsGlobal(environment, assignmentStmt.getName())) {
            emitInstruction("STORE", assignmentStmt.getName());
        } else {
            int localSlot = getLocalSlot(assignmentStmt.getName(), environment);
            emitInstruction("STORE_LOCAL", String.valueOf(localSlot));
        }
    }

    private void generateIfStmt(IfStatement ifStmt, Environment environment) {
        if (ifStmt.getElseStatement() != null) {
            String elseLabel = createUniqueLabel("elseLabel");
            String endLabel = createUniqueLabel("endLabel");

            generateExpression(ifStmt.getCondition(), environment);
            emitInstruction("JIF", elseLabel);

            generateStatement(ifStmt.getIfStatement(), environment);
            emitInstruction("JUMP", endLabel);

            emitLabel(elseLabel);
            generateStatement(ifStmt.getElseStatement(), environment);
            emitLabel(endLabel);
        } else {
            String endLabel = createUniqueLabel("endLabel");

            generateExpression(ifStmt.getCondition(), environment);
            emitInstruction("JIF", endLabel);

            generateStatement(ifStmt.getIfStatement(), environment);
            emitLabel(endLabel);
        }
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
        if (forStmt.getInitializer() != null) {
            generateStatement(forStmt.getInitializer(), environment);
        }

        String startLabel = createUniqueLabel("startLabel");
        String endLabel = createUniqueLabel("endLabel");

        breakStack.push(endLabel);

        emitLabel(startLabel);

        if (forStmt.getCondition() != null) {
            generateExpression(forStmt.getCondition(), environment);
            emitInstruction("JIF", endLabel);
        }

        if (forStmt.getBody() != null) {
            generateStatement(forStmt.getBody(), environment);
        }

        if (forStmt.getIncrement() != null) {
            generateStatement(forStmt.getIncrement(), environment);
        }

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
        if (breakStack.isEmpty()) {
            throw new CodeGenException(
                    "Break statement outside of loop context",
                    breakStmt.getLineNumber()
            );
        }
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
        resetLocalScope();

        emitLabel(":" + funcDeclStmt.getName());

        List<Parameter> params = funcDeclStmt.getParameters();

        for (int i = 0; i < params.size(); i++) {
            String paramName = params.get(i).name();
            if (!envHasSlot(environment, paramName)) {
                fallbackLocalSlots.put(paramName, i);
            }

            // Fix: Use parseType on the parameter's type string instead of safeGetSymbolType
            Type paramType = parseType(params.get(i).type());
            localVariableTypes.put(paramName, paramType);
        }
        nextFallbackSlot = Math.max(nextFallbackSlot, params.size());

        for (int i = params.size() - 1; i >= 0; i--) {
            int slot = getLocalSlot(params.get(i).name(), environment);
            emitInstruction(
                    "STORE_LOCAL",
                    String.valueOf(slot)
            );
        }

        generateStatement(funcDeclStmt.getBody(), environment);

        emitInstruction("RET");
    }

    private Type getExpressionType(Expression expr, Environment env) {
        if (expr == null) {
            return Type.INT;
        }

        if (expr instanceof LiteralExpression literal) {
            if (literal.getValue() instanceof String) {
                return Type.STRING;
            }
            return Type.INT;
        }

        if (expr instanceof VariableExpression varExpr) {
            String varName = varExpr.getName();

            // 1. Check global environment first
            Type symbolType = safeGetSymbolType(env, varName);
            if (symbolType != null) {
                return symbolType;
            }

            // 2. Check local tracked types (fixes local variable lookup failure)
            if (localVariableTypes.containsKey(varName)) {
                return localVariableTypes.get(varName);
            }

            // Fallback default if completely unknown
            return Type.INT;
        }

        if (expr instanceof CallingExpression callExpr) {
            Type symbolType = safeGetSymbolType(env, callExpr.getCallee());
            if (symbolType != null) {
                return symbolType;
            }
            return Type.INT;
        }

        if (expr instanceof GroupingExpression groupExpr) {
            return getExpressionType(groupExpr.getExpression(), env);
        }

        if (expr instanceof BinaryExpression binary) {
            Type left = getExpressionType(binary.getLeft(), env);
            Type right = getExpressionType(binary.getRight(), env);
            if (left == Type.STRING || right == Type.STRING) {
                return Type.STRING;
            }
        }

        return Type.INT;
    }

    private Type parseType(String typeName) {
        if (typeName == null) return Type.INT;
        switch (typeName.toLowerCase().trim()) {
            case "string", "str" -> {
                return Type.STRING;
            }
            case "boolean", "bool" -> {
                return Type.BOOLEAN;
            }
            default -> {
                return Type.INT;
            }
        }
    }

    public void generatePrintStmt(PrintStatement stmt, Environment environment) {
        generateExpression(stmt.getExpression(), environment);

        Type exprType = getExpressionType(stmt.getExpression(), environment);

        if (exprType == Type.STRING) {
            emitInstruction("PRINT_STR");
        } else {
            emitInstruction("PRINT");
        }
    }

    public void generateInputStmt(InputStatement inputStmt, Environment environment) {
        emitInstruction("INPUT");

        int localSlot = getLocalSlot(inputStmt.getVariable(), environment);
        emitInstruction("STORE_LOCAL", String.valueOf(localSlot));
    }

    private void generateExpression(Expression expression, Environment environment) {
        if (expression instanceof LiteralExpression literal) {
            generateLiteralExpression(literal);
        } else if (expression instanceof VariableExpression variable) {
            generateVariableExpression(variable, environment);
        } else if (expression instanceof BinaryExpression binary) {
            generateBinaryExpression(binary, environment);
        } else if (expression instanceof UnaryExpression unary) {
            generateUnaryExpression(unary, environment);
        } else if (expression instanceof GroupingExpression grouping) {
            generateGroupingExpression(grouping, environment);
        } else if (expression instanceof CallingExpression calling) {
            generateCallingExpression(calling, environment);
        } else {
            throw new CodeGenException(
                    "Code Gen Error: Invalid expression type: " + (expression != null ? expression.getClass().getName() : "null"),
                    expression != null ? expression.getLineNumber() : 0
            );
        }
    }

    private void generateLiteralExpression(LiteralExpression literal) {
        if (literal.getValue() instanceof Integer) {
            emitInstruction("PUSH", literal.getValue().toString());
        } else if (literal.getValue() instanceof Boolean) {
            String boolValue = (Boolean) literal.getValue() ? "1" : "0";
            emitInstruction("PUSH", boolValue);
        } else if (literal.getValue() instanceof String strVal) {
            String formatted = strVal.startsWith("\"") && strVal.endsWith("\"") ? strVal : "\"" + strVal + "\"";
            emitInstruction("PUSH_STR", formatted);
        } else {
            throw new CodeGenException(
                    "Code Generation Error: Invalid literal: " + literal.getValue(),
                    literal.getLineNumber()
            );
        }
    }

    private void generateVariableExpression(VariableExpression variable, Environment environment) {
        if (safeIsGlobal(environment, variable.getName())) {
            emitInstruction("LOAD", variable.getName());
        } else {
            int slot = getLocalSlot(variable.getName(), environment);
            emitInstruction("LOAD_LOCAL", String.valueOf(slot));
        }
    }

    private void generateBinaryExpression(BinaryExpression binary, Environment environment) {
        Token operator = binary.getOperator();

        if (operator.tokenValue().equals("&&")) {
            generateLogicalAnd(binary, environment);
        } else if (operator.tokenValue().equals("||")) {
            generateLogicalOr(binary, environment);
        } else {
            generateExpression(binary.getLeft(), environment);
            generateExpression(binary.getRight(), environment);

            switch (operator.tokenValue()) {
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
        assemblyString.append(labelName).append("\n");
    }

    private void generateLogicalAnd(BinaryExpression expr, Environment env) {
        String falseLabel = createUniqueLabel("and_false");
        String endLabel = createUniqueLabel("and_end");

        generateExpression(expr.getLeft(), env);
        emitInstruction("JIF", falseLabel);

        generateExpression(expr.getRight(), env);
        emitInstruction("JIF", falseLabel);

        emitInstruction("PUSH", "1");
        emitInstruction("JUMP", endLabel);

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
                emitInstruction("PUSH", "0");
                emitInstruction("SWAP");
                emitInstruction("SUB");
            }
            default -> throw new CodeGenException(
                    "Code Generation Error: Invalid unary operator " + unary.getOperator().tokenValue(),
                    unary.getLineNumber()
            );
        }
    }

    private void generateGroupingExpression(GroupingExpression grouping, Environment environment) {
        generateExpression(grouping.getExpression(), environment);
    }

    private void generateCallingExpression(CallingExpression calling, Environment environment) {
        for (Expression argument : calling.getArguments()) {
            generateExpression(argument, environment);
        }

        String functionLabel = ":" + calling.getCallee();
        emitInstruction("CALL", functionLabel);
    }

    private boolean safeIsGlobal(Environment env, String varName) {
        if (env == null || varName == null) return false;
        try {
            return env.isGlobal(varName);
        } catch (Exception e) {
            return false;
        }
    }

    private Type safeGetSymbolType(Environment env, String name) {
        if (env == null || name == null) return null;
        try {
            var symbol = env.getSymbol(name);
            return symbol != null ? symbol.getType() : null;
        } catch (Exception e) {
            return null;
        }
    }
}