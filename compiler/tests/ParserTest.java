import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import zplusplus.ast.*;
import zplusplus.exceptions.SyntaxException;
import zplusplus.parser.Parser;

public class ParserTest {

    private Parser parser;

    @BeforeEach
    public void setUp() {
        parser = new Parser();
    }

    @Test
    public void testVariableDeclarationAndAssignment() {
        String input = "int score = 100;";
        List<Statement> statements = parser.parse(input);

        assertNotNull(statements);
        assertEquals(1, statements.size());
        assertInstanceOf(VariableDeclarationStatement.class, statements.getFirst());

        VariableDeclarationStatement varDecl = (VariableDeclarationStatement) statements.getFirst();
        assertEquals("int", varDecl.getTypeName()); // Assuming getter names
        assertEquals("score", varDecl.getVarName());
        assertNotNull(varDecl.getInitializer());
    }

    @Test
    public void testFunctionDeclarationAndCall() {
        String input = """
            def int doubleMe(int x) {
                return x * 2;
            }
            doubleMe(5);
            """;
        List<Statement> statements = parser.parse(input);

        assertNotNull(statements);
        assertEquals(2, statements.size());

        // Verify Declaration
        assertInstanceOf(FunctionDeclarationStatement.class, statements.getFirst());
        FunctionDeclarationStatement funcDecl = (FunctionDeclarationStatement) statements.getFirst();
        assertEquals("doubleMe", funcDecl.getName());
        assertEquals("int", funcDecl.getReturnType());
        assertEquals(1, funcDecl.getParameters().size());

        // Verify Standalone Call (ExpressionStatement wrapping a CallingExpression)
        assertInstanceOf(ExpressionStatement.class, statements.get(1));
        ExpressionStatement exprStmt = (ExpressionStatement) statements.get(1);
        assertInstanceOf(CallingExpression.class, exprStmt.getExpression());
    }

    @Test
    public void testForLoopWithInlineAssignment() {
        String input = """
            for (int i = 0; i < 10; i = i + 1) {
                print(i);
            }
            """;
        List<Statement> statements = parser.parse(input);

        assertNotNull(statements);
        assertEquals(1, statements.size());
        assertInstanceOf(ForStatement.class, statements.getFirst());

        ForStatement forStmt = (ForStatement) statements.getFirst();
        // Verify your inline assignment fix works flawlessly
        assertInstanceOf(AssignmentStatement.class, forStmt.getIncrement());
    }

    @Test
    public void testIfElseConditionals() {
        String input = """
            if (isValid) {
                break;
            } else {
                return false;
            }
            """;
        List<Statement> statements = parser.parse(input);

        assertNotNull(statements);
        assertEquals(1, statements.size());
        assertInstanceOf(IfStatement.class, statements.getFirst());

        IfStatement ifStmt = (IfStatement) statements.getFirst();
        assertNotNull(ifStmt.getIfStatement());
        assertNotNull(ifStmt.getIfStatement());
    }

    @Test
    public void testSyntaxErrorMissingSemicolonThrowsException() {
        String invalidInput = "int x = 42"; // Missing ';'

        SyntaxException exception = assertThrows(SyntaxException.class, () -> {
            parser.parse(invalidInput);
        });

        assertTrue(exception.getMessage().contains("Expected ';'"));
    }

    @Test
    public void testSyntaxErrorUnbalancedParenthesesThrowsException() {
        String invalidInput = "if (x == 5 { print(x); }"; // Missing ')'

        SyntaxException exception = assertThrows(SyntaxException.class, () -> {
            parser.parse(invalidInput);
        });

        assertTrue(exception.getMessage().contains("Expected ')'"));
    }

    @Test
    public void testOperatorPrecedenceLadder() {
        // Enforces: factor (*) > term (+) > comparison (>) > logical and (&&)
        String input = "bool result = 5 + 3 * 2 > 10 && !false;";
        List<Statement> statements = parser.parse(input);

        assertNotNull(statements);
        assertEquals(1, statements.size());
        assertInstanceOf(VariableDeclarationStatement.class, statements.getFirst());

        VariableDeclarationStatement decl = (VariableDeclarationStatement) statements.getFirst();
        Expression initExpr = decl.getInitializer();

        // Root of the RHS should be the lowest precedence operator: LOGICAL_AND
        assertInstanceOf(BinaryExpression.class, initExpr);
        BinaryExpression andExpr = (BinaryExpression) initExpr;
        assertEquals("&&", andExpr.getOperator().tokenValue());

        // Right side of AND should be the unary NOT expression (!false)
        assertInstanceOf(UnaryExpression.class, andExpr.getRight());
        UnaryExpression notExpr = (UnaryExpression) andExpr.getRight();
        assertEquals("!", notExpr.getOperator().tokenValue());
    }

    @Test
    public void testParenthesesGroupingPrecedence() {
        // Parentheses force addition to happen before multiplication
        String input = "int result = (5 + 3) * 2;";
        List<Statement> statements = parser.parse(input);

        assertNotNull(statements);
        VariableDeclarationStatement decl = (VariableDeclarationStatement) statements.getFirst();

        // Root of the RHS should MULTIPLY, with the grouped expression nested inside
        assertInstanceOf(BinaryExpression.class, decl.getInitializer());
        BinaryExpression multiplyExpr = (BinaryExpression) decl.getInitializer();
        assertEquals("*", multiplyExpr.getOperator().tokenValue());

        // Left branch should be the addition binary expression extracted from parentheses
        assertInstanceOf(BinaryExpression.class, multiplyExpr.getLeft());
        BinaryExpression addExpr = (BinaryExpression) multiplyExpr.getLeft();
        assertEquals("+", addExpr.getOperator().tokenValue());
    }

    // =========================================================================
    // ADVANCED CONTROL FLOW & BLOCK STATEMENTS
    // =========================================================================

    @Test
    public void testWhileLoopStatement() {
        String input = """
            while (count < 10) {
                count = count + 1;
            }
            """;
        List<Statement> statements = parser.parse(input);

        assertNotNull(statements);
        assertEquals(1, statements.size());
        assertInstanceOf(WhileStatement.class, statements.getFirst());

        WhileStatement whileStmt = (WhileStatement) statements.getFirst();
        assertInstanceOf(BinaryExpression.class, whileStmt.getCondition());
        assertInstanceOf(BlockStatement.class, whileStmt.getBody());
    }

    @Test
    public void testNestedBlockStatements() {
        String input = """
            {
                int x = 1;
                {
                    int y = 2;
                }
            }
            """;
        List<Statement> statements = parser.parse(input);

        assertNotNull(statements);
        assertEquals(1, statements.size());
        assertInstanceOf(BlockStatement.class, statements.getFirst());

        BlockStatement outerBlock = (BlockStatement) statements.getFirst();
        assertEquals(2, outerBlock.getStatements().size());

        // Second statement inside outer block should be the nested inner BlockStatement
        assertInstanceOf(BlockStatement.class, outerBlock.getStatements().get(1));
    }

    @Test
    public void testPrintStatementWithComplexExpression() {
        String input = "print(\"Value: \" + (x % 2));";
        List<Statement> statements = parser.parse(input);

        assertNotNull(statements);
        assertEquals(1, statements.size());
        assertInstanceOf(PrintStatement.class, statements.getFirst());

        PrintStatement printStmt = (PrintStatement) statements.getFirst();
        assertInstanceOf(BinaryExpression.class, printStmt.getExpression());
    }

    // =========================================================================
    // FUNCTION SIGNATURES & ARRAYS / NESTED CALLS
    // =========================================================================

    @Test
    public void testMultiParameterFunctionDeclaration() {
        String input = """
            def bool validate(int age, string name, bool active) {
                return true;
            }
            """;
        List<Statement> statements = parser.parse(input);

        assertNotNull(statements);
        FunctionDeclarationStatement func = (FunctionDeclarationStatement) statements.getFirst();
        assertEquals("validate", func.getName());
        assertEquals(3, func.getParameters().size());

        assertEquals("int", func.getParameters().get(0).type());
        assertEquals("age", func.getParameters().get(0).name());
        assertEquals("string", func.getParameters().get(1).type());
        assertEquals("bool", func.getParameters().get(2).type());
    }

    @Test
    public void testNestedFunctionCallInExpressions() {
        // Tests the fixed semicolon logic: ensures nested calls do not prematurely stop parsing
        String input = "int total = getBase() + calculateFactor(5, x);";
        List<Statement> statements = parser.parse(input);

        assertNotNull(statements);
        VariableDeclarationStatement decl = (VariableDeclarationStatement) statements.getFirst();

        assertInstanceOf(BinaryExpression.class, decl.getInitializer());
        BinaryExpression addExpr = (BinaryExpression) decl.getInitializer();

        // Both sides of the operator must be separate Function Call Expressions
        assertInstanceOf(CallingExpression.class, addExpr.getLeft());
        assertInstanceOf(CallingExpression.class, addExpr.getRight());

        CallingExpression rightCall = (CallingExpression) addExpr.getRight();
        assertEquals("calculateFactor", rightCall.getCallee());
        assertEquals(2, rightCall.getArguments().size());
    }

    @Test
    public void testValidVoidReturnType() {
        String input = "def void test() { return; }"; // 'void' is not a supported token type

        List<Statement> statements = parser.parse(input);
        assertNotNull(statements);
        assertEquals(1, statements.size());

        FunctionDeclarationStatement func = (FunctionDeclarationStatement) statements.getFirst();
        assertEquals("void", func.getReturnType());
    }

    // =========================================================================
    // DEFENSIVE ERROR & BOUNDARY COGNIZANCE TESTS
    // =========================================================================

    @Test
    public void testSyntaxErrorTrailingCommaInFunctionCall() {
        String input = "compute(1, 2, );"; // Structural invalid trailing comma

        SyntaxException exception = assertThrows(SyntaxException.class, () -> {
            parser.parse(input);
        });
        assertTrue(exception.getMessage().contains("Trailing comma in argument list"));
    }

    @Test
    public void testSyntaxErrorTrailingCommaInFunctionDeclaration() {
        String input = "def int compute(int a, ) { return a; }";

        SyntaxException exception = assertThrows(SyntaxException.class, () -> {
            parser.parse(input);
        });
        assertTrue(exception.getMessage().contains("Trailing comma in parameter list"));
    }
}