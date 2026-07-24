package zplusplus.sem_analysis;

import zplusplus.ast.*;
import zplusplus.exceptions.SemanticException;
import zplusplus.lexer.TokenType;
import zplusplus.sem_analysis.symbol.FunctionSymbol;
import zplusplus.sem_analysis.symbol.Symbol;
import zplusplus.sem_analysis.symbol.VariableSymbol;

import java.util.ArrayList;
import java.util.List;

/**
 * Semantic analyser class. Checks the semantics for all statements
 * and expressions.
 *
 * @author Zubair Abdul Matin
 */
public class Analyser {
    private List<Statement> statements;
    private Environment currentEnvironment;
    private Type currentFunctionReturnType;
    private int currentFunctionOffset = 0;
    private int loopDepth;

    /**
     * Constructor for analyser class,
     * initialises list of statements, current environment,
     * current function return type and the depth of the current loop
     * @param statements list of statements
     */
    public Analyser(List<Statement> statements) {
        this.statements = statements;
        this.currentEnvironment = new Environment(null);
        this.currentFunctionReturnType = null;
        this.loopDepth = 0;
    }

    /**
     * Loops through each statement and calls analyseStatement()
     */
    public void analyse() {
        for (Statement statement : statements) {
            analyseStatement(statement);
        }
    }

    /**
     * Checks type of statement and then calls the corresponding
     * handler function.
     *
     * @param statement current statement
     */
    private void analyseStatement(Statement statement) {
        switch (statement) {
            case VariableDeclarationStatement varDeclStatement -> analyseVarDecl(varDeclStatement);
            case AssignmentStatement assignmentStatement -> analyseAssign(assignmentStatement);
            case IfStatement ifStatement -> analyseIf(ifStatement);
            case WhileStatement whileStatement -> analyseWhile(whileStatement);
            case ForStatement forStatement -> analyseFor(forStatement);
            case ReturnStatement returnStatement -> analyseReturn(returnStatement);
            case BreakStatement breakStatement -> analyseBreak(breakStatement);
            case BlockStatement blockStatement -> analyseBlock(blockStatement);
            case FunctionDeclarationStatement functionDeclarationStatement ->
                    analyseFuncDecl(functionDeclarationStatement);
            case PrintStatement printStatement -> analysePrint(printStatement);
            case ExpressionStatement expressionStatement -> analyseExpression(expressionStatement.getExpression());
            default -> throw new SemanticException("Semantic Error: Not a valid statement", statement.getLineNumber());
        }
    }

    private void analyseVarDecl(VariableDeclarationStatement varDeclStatement) {
        Type declaredType = parseType(varDeclStatement.getTypeName(), varDeclStatement.getLineNumber());

        if (varDeclStatement.getInitializer() != null) {
            Type initType = analyseExpression(varDeclStatement.getInitializer());
            if (declaredType != initType) {
                throw new SemanticException(
                        "Semantic Error: Cannot initialize variable of type " + declaredType + " with value of type " + initType,
                        varDeclStatement.getLineNumber()
                );
            }
        }

        VariableSymbol variableSymbol = new VariableSymbol(varDeclStatement.getVarName(), declaredType);
        if (!currentEnvironment.addToTable(variableSymbol)) {
            throw new SemanticException(
                    "Semantic Error: Variable '" + varDeclStatement.getVarName() + "' is already declared in this scope",
                    varDeclStatement.getLineNumber()
            );
        }
    }

    private void analyseAssign(AssignmentStatement assignmentStatement) {
        if (assignmentStatement.getExpression() == null) {
            throw new SemanticException(
                    "Semantic Error: Not a valid assignment",
                    assignmentStatement.getLineNumber()
            );
        }

        Symbol symbol = currentEnvironment.getSymbol(assignmentStatement.getName());
        Type expressionType = analyseExpression(assignmentStatement.getExpression());

        if (symbol == null) {
            throw new SemanticException(
                    "Semantic Error: Cannot assign to undeclared variable '" + assignmentStatement.getName() + "'",
                    assignmentStatement.getLineNumber()
            );
        }

        if (symbol.getType() != expressionType) {
            throw new SemanticException(
                    "Semantic Error: Not a valid assignment",
                    assignmentStatement.getLineNumber()
            );
        }
    }

    private void analyseIf(IfStatement ifStatement) {
        Expression condition = ifStatement.getCondition();
        if (analyseExpression(condition) != Type.BOOLEAN) {
            throw new SemanticException(
                    "Semantic Error: Invalid if condition, must be a boolean condition",
                    ifStatement.getLineNumber()
            );
        }

        // considers when there is no body expression at all, not an empty body
        if (ifStatement.getIfStatement() == null) {
            throw new SemanticException("Semantic Error: No AST node found for if body", ifStatement.getLineNumber());
        }

        analyseStatement(ifStatement.getIfStatement());

        if (ifStatement.getElseStatement() != null) {
            analyseStatement(ifStatement.getElseStatement());
        }
    }

    private void analyseWhile(WhileStatement whileStatement) {
        Expression condition = whileStatement.getCondition();
        if  (analyseExpression(condition) != Type.BOOLEAN) {
            throw new SemanticException(
                    "Semantic Error: Invalid while condition at",
                    whileStatement.getLineNumber()
            );
        }

        loopDepth++;

        if (whileStatement.getBody() == null) {
            throw new SemanticException("Semantic Error: No AST node found for while body", whileStatement.getLineNumber());
        }

        analyseStatement(whileStatement.getBody());

        loopDepth--;
    }

    private void analyseFor(ForStatement forStatement) {
        currentEnvironment = new Environment(currentEnvironment);
        loopDepth++;

        try {
            if (forStatement.getInitializer() != null) {
                analyseStatement(forStatement.getInitializer());
            }

            if (forStatement.getCondition() != null) {
                if (analyseExpression(forStatement.getCondition()) != Type.BOOLEAN) {
                    throw new SemanticException(
                            "Semantic Error: For loop condition must be of boolean type",
                            forStatement.getLineNumber()
                    );
                }
            }

            if (forStatement.getIncrement() != null) {
                analyseStatement(forStatement.getIncrement());
            }

            if (forStatement.getBody() == null) {
                throw new SemanticException("Semantic Error: No AST node found for body", forStatement.getLineNumber());
            }

            analyseStatement(forStatement.getBody());
        } finally {
            loopDepth--;
            currentEnvironment = currentEnvironment.getParentEnvironment();
        }
    }

    private void analyseReturn(ReturnStatement returnStatement) {

        if (currentFunctionReturnType == null) {
            throw new SemanticException(
                    "Semantic Error: Not a valid function",
                    returnStatement.getLineNumber()
            );
        }

        if (returnStatement.getReturnValue() == null) {
            if (currentFunctionReturnType != Type.VOID) {
                throw new SemanticException(
                        "Semantic Error: Non-void function must return a value of type " + currentFunctionReturnType,
                        returnStatement.getLineNumber()
                );
            }
            return;
        }

        if (currentFunctionReturnType == Type.VOID) {
            throw new SemanticException(
                    "Semantic Error: Function return type is void but return value is non-void",
                    returnStatement.getLineNumber()
            );
        }

        if (currentFunctionReturnType != analyseExpression(returnStatement.getReturnValue())) {
            throw new SemanticException(
                    "Semantic Error: Invalid return type at. Expected: " + currentFunctionReturnType,
                    returnStatement.getLineNumber()
            );
        }
    }

    private void analyseBreak(BreakStatement breakStatement) {
        if (loopDepth <= 0) {
            throw new SemanticException(
                    "Semantic Error: Invalid break statement",
                    breakStatement.getLineNumber()
            );
        }
    }

    private void analyseBlock(BlockStatement blockStatement) {
        // making a new environment and setting it as the current, making the previous one the parent
        currentEnvironment = new Environment(currentEnvironment);

        for (Statement statement : blockStatement.getStatements()) {
            analyseStatement(statement);
        }

        // popping off the old environment
        currentEnvironment = currentEnvironment.getParentEnvironment();
    }

    private void analyseFuncDecl(FunctionDeclarationStatement stmt) {
        Type returnType = parseType(stmt.getReturnType(), stmt.getLineNumber());

        List<VariableSymbol> paramTypes = new ArrayList<>();
        for (Parameter param : stmt.getParameters()) {
            paramTypes.add(new VariableSymbol(param.name(), parseType(param.type(), stmt.getLineNumber())));
        }

        // increments the function offset counter
        int offset = currentFunctionOffset++;

        // creates function symbol
        FunctionSymbol funcSymbol = new FunctionSymbol(
                stmt.getName(),
                returnType,
                paramTypes,
                offset
        );

        // checks the current environment if the function already is declared
        if (!currentEnvironment.addToTable(funcSymbol)) {
            throw new SemanticException(
                    "Semantic Error: Function '" + stmt.getName() + "' is already declared",
                    stmt.getLineNumber()
            );
        }

        Type previousReturnType = currentFunctionReturnType;
        currentFunctionReturnType = returnType;

        currentEnvironment = new Environment(currentEnvironment);

        try {
            // Adds parameters as VariableSymbols inside the function's scope
            List<Parameter> params = stmt.getParameters();
            for (int i = 0; i < params.size(); i++) {
                Parameter param = params.get(i);
                Type pType = paramTypes.get(i).getType();

                VariableSymbol paramSymbol = new VariableSymbol(param.name(), pType);

                if (!currentEnvironment.addToTable(paramSymbol)) {
                    throw new SemanticException(
                            "Semantic Error: Duplicate parameter name '" + param.name() + "'",
                            stmt.getLineNumber()
                    );
                }
            }

            analyseStatement(stmt.getBody());

        } finally { // makes sure that environment reverts back to original
            currentEnvironment = currentEnvironment.getParentEnvironment();
            currentFunctionReturnType = previousReturnType;
        }
    }

    private void analysePrint(PrintStatement printStatement) {
        if (analyseExpression(printStatement.getExpression()) != Type.STRING) {
            throw new SemanticException(
                    "Semantic Error: Invalid print statement, must be a string expression",
                    printStatement.getLineNumber()
            );
        }
    }

    private Type analyseExpression(Expression expression) {
        if (expression instanceof LiteralExpression literalExpression) {
            if (literalExpression.getValue() instanceof Integer) {
                return Type.INT;
            }else if (literalExpression.getValue() instanceof String) {
                return Type.STRING;
            }else if (literalExpression.getValue() instanceof Boolean) {
                return Type.BOOLEAN;
            }else {
                return Type.ERROR;
            }
        }else if (expression instanceof VariableExpression variableExpression) {
            return analyseVarExpression(variableExpression);
        }else if (expression instanceof BinaryExpression binaryExpression) {
            return analyseBinExpr(binaryExpression);
        }else if (expression instanceof UnaryExpression unaryExpression) {
            return analyseUnaryExpr(unaryExpression);
        }else if (expression instanceof GroupingExpression groupingExpression) {
            return analyseGroupExpr(groupingExpression);
        }else if (expression instanceof CallingExpression callingExpression) {
            return analyseCallExpr(callingExpression);
        }

        return Type.ERROR;
    }

    private Type analyseVarExpression(VariableExpression variableExpression) {
        Symbol symbol = currentEnvironment.getSymbol(variableExpression.getName());
        return symbol != null ? symbol.getType() : Type.ERROR;
    }

    private Type analyseBinExpr(BinaryExpression binaryExpression) {

        Type left = analyseExpression(binaryExpression.getLeft());
        Type right = analyseExpression(binaryExpression.getRight());

        if (left == Type.ERROR || right == Type.ERROR) return Type.ERROR;

        TokenType op = binaryExpression.getOperator().type();

        // handling logical operators
        if (op == TokenType.LOGICAL_AND || op == TokenType.LOGICAL_OR) {
            if (left != Type.BOOLEAN || right != Type.BOOLEAN) {
                throw new SemanticException(
                        "Semantic Error: Logical operations require boolean operands",
                        binaryExpression.getLineNumber()
                );
            }
            return Type.BOOLEAN;
        }

        // handling comparison operators
        if (op == TokenType.EQUAL_EQUAL || op == TokenType.NOT_EQUAL ||
                op == TokenType.LESS_THAN || op == TokenType.GREATER_THAN ||
                op == TokenType.LESS_OR_EQUAL || op == TokenType.GREATER_OR_EQUAL) {
            if (left != right) {
                throw new SemanticException(
                        "Semantic Error: Cannot compare mismatched types " + left + " and " + right,
                        binaryExpression.getLineNumber()
                );
            }
            return Type.BOOLEAN;
        }

        // handling bitwise and arithmetic operators
        if (op == TokenType.BITWISE_AND || op == TokenType.BITWISE_OR || op == TokenType.BITWISE_XOR ||
                op == TokenType.PLUS || op == TokenType.MINUS ||
                op == TokenType.MULTIPLY || op == TokenType.DIVIDE || op == TokenType.MODULO) {

            if (left != Type.INT || right != Type.INT) {
                throw new SemanticException(
                        "Semantic Error: Bitwise and arithmetic operations require integer operands",
                        binaryExpression.getLineNumber()
                );
            }
            return Type.INT;
        }

        return Type.ERROR;
    }

    private Type analyseUnaryExpr(UnaryExpression unaryExpression) {
        Type operandType = analyseExpression(unaryExpression.getRightExpression());

        if (operandType == Type.ERROR) return Type.ERROR;

        TokenType op = unaryExpression.getOperator().type();

        // Logical NOT (!)
        if (op == TokenType.LOGICAL_NOT) {
            if (operandType != Type.BOOLEAN) {
                throw new SemanticException(
                        "Semantic Error: Logical NOT '!' requires a boolean operand",
                        unaryExpression.getLineNumber()
                );
            }
            return Type.BOOLEAN;
        }

        // Unary MINUS (-)
        if (op == TokenType.MINUS) {
            if (operandType != Type.INT) {
                throw new SemanticException(
                        "Semantic Error: Unary minus '-' requires an integer operand",
                        unaryExpression.getLineNumber()
                );
            }
            return Type.INT;
        }

        // Bitwise NOT (~)
        if (op == TokenType.BITWISE_NOT) {
            if (operandType != Type.INT) {
                throw new SemanticException(
                        "Semantic Error: Bitwise NOT '~' requires an integer operand",
                        unaryExpression.getLineNumber()
                );
            }
            return Type.INT;
        }

        return Type.ERROR;
    }


    private Type analyseCallExpr(CallingExpression callingExpression) {
        Symbol symbol = currentEnvironment.getSymbol(callingExpression.getCallee());

        if (!(symbol instanceof FunctionSymbol functionSymbol)) {
            throw new SemanticException(
                    "Semantic Error: Symbol '" + callingExpression.getCallee() + "' is not a declared function",
                    callingExpression.getLineNumber()
            );
        }

        if (functionSymbol.getParameters().size() != callingExpression.getArguments().size()) {
            throw new SemanticException(
                    "Semantic Error: Function argument count mismatch for '" + callingExpression.getCallee() + "'",
                    callingExpression.getLineNumber()
            );
        }

        for (int i = 0; i < functionSymbol.getParameters().size(); i++) {
            Type argType = analyseExpression(callingExpression.getArguments().get(i));
            Type paramType = functionSymbol.getParameters().get(i).getType();
            if (argType != paramType) {
                throw new SemanticException(
                        "Semantic Error: Type mismatch for argument " + i + " in function call",
                        callingExpression.getLineNumber()
                );
            }
        }

        return functionSymbol.getType();
    }

    private Type analyseGroupExpr(GroupingExpression groupingExpression) {
        return analyseExpression(groupingExpression.getExpression());
    }

    private Type parseType(String typeName, int lineNumber) {
        if (typeName == null) {
            return Type.VOID;
        }

        return switch (typeName.trim().toLowerCase()) {
            case "int" -> Type.INT;
            case "string" -> Type.STRING;
            case "bool" -> Type.BOOLEAN;
            case "void" -> Type.VOID;
            default -> throw new SemanticException(
                    "Semantic Error: Unknown or unsupported type '" + typeName + "'",
                    lineNumber
            );
        };
    }
}
