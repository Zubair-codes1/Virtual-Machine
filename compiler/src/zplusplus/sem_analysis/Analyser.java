package zplusplus.sem_analysis;

import zplusplus.ast.*;
import zplusplus.exceptions.SemanticException;
import zplusplus.sem_analysis.symbol.Symbol;
import zplusplus.sem_analysis.symbol.VariableSymbol;

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
        if (varDeclStatement.getInitializer() != null) {
            Type type = null;
            switch (varDeclStatement.getTypeName()){
                case "int" -> type = Type.INT;
                case "bool" -> type = Type.BOOLEAN;
                case "string" -> type = Type.STRING;
                default -> throw new SemanticException("Semantic Error: Not a valid type", varDeclStatement.getLineNumber());
            }
            VariableSymbol variableSymbol = new VariableSymbol(varDeclStatement.getVarName(), type);

            currentEnvironment.addToTable(variableSymbol);
        }else {
            throw new SemanticException("Semantic Error: Not a valid variable", varDeclStatement.getLineNumber());
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

        Statement initializer = forStatement.getInitializer();
        if (initializer != null) {
            analyseStatement(initializer);
        }

        Expression condition = forStatement.getCondition();
        if (condition != null) {
            if (analyseExpression(condition) != Type.BOOLEAN) {
                throw new SemanticException(
                        "Semantic Error: For loop condition must be of boolean type",
                        forStatement.getLineNumber()
                );
            }
        }

        Statement increment = forStatement.getIncrement();
        if (increment != null) {
            analyseStatement(increment);
        }

        loopDepth++;

        Statement body = forStatement.getBody();
        if (body == null) {
            throw new SemanticException("Semantic Error: No AST node found for body", forStatement.getLineNumber());
        }

        analyseStatement(body);
        loopDepth--;

        currentEnvironment = currentEnvironment.getParentEnvironment();

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
        if (loopDepth <= 1) {
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

    private void analyseFuncDecl(FunctionDeclarationStatement functionDeclarationStatement) {
        return;
    }

    private void analysePrint(PrintStatement printStatement) {
        return;
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
        return null;
    }

    private Type analyseUnaryExpr(UnaryExpression unaryExpression) {
        return null;
    }

    private Type analyseCallExpr(CallingExpression callingExpression) {
        return null;
    }

    private Type analyseGroupExpr(GroupingExpression groupingExpression) {
        return null;
    }
}
