import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import zplusplus.ast.*;
import zplusplus.exceptions.SemanticException;
import zplusplus.lexer.Token;
import zplusplus.lexer.TokenType;
import zplusplus.sem_analysis.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Z++ Semantic Analyser Full Test Suite")
class AnalyserTest {

    // --- Helper Methods & Dummy AST Nodes ---

    private void analyse(Statement... statements) {
        Analyser analyser = new Analyser(List.of(statements));
        analyser.analyse();
    }

    private Token token(TokenType type, String value) {
        return new Token(type, value, 1);
    }

    private static class DummyStatement extends Statement {
        public DummyStatement() {
            super(1);
        }

        @Override
        public String toString() {
            return "";
        }
    }

    private static class DummyExpression extends Expression {
        public DummyExpression() {
            super(1);
        }

        @Override
        public String toString() {
            return "";
        }
    }

    // --- 1. Variable Declarations & Assignments ---

    @Nested
    @DisplayName("1. Variable Declarations & Assignments")
    class VariableAndAssignmentTests {

        @Test
        @DisplayName("Uninitialized variable declaration should pass")
        void testUninitializedVarDecl() {
            assertDoesNotThrow(() -> analyse(
                    new VariableDeclarationStatement("int", "x", null, 1)
            ));
        }

        @Test
        @DisplayName("Initialized variable declaration with matching type should pass")
        void testInitializedVarDeclSuccess() {
            assertDoesNotThrow(() -> analyse(
                    new VariableDeclarationStatement("int", "x", new LiteralExpression(10, 1), 1),
                    new VariableDeclarationStatement("string", "s", new LiteralExpression("hello", 1), 1),
                    new VariableDeclarationStatement("bool", "b", new LiteralExpression(true, 1), 1)
            ));
        }

        @Test
        @DisplayName("Initializing variable with mismatched type throws SemanticException")
        void testVarDeclTypeMismatch() {
            VariableDeclarationStatement stmt = new VariableDeclarationStatement(
                    "int", "x", new LiteralExpression("not an int", 1), 1
            );
            assertThrows(SemanticException.class, () -> analyse(stmt));
        }

        @Test
        @DisplayName("Duplicate variable declaration in the same scope throws SemanticException")
        void testDuplicateVarDecl() {
            Statement decl1 = new VariableDeclarationStatement("int", "x", null, 1);
            Statement decl2 = new VariableDeclarationStatement("int", "x", null, 1);
            assertThrows(SemanticException.class, () -> analyse(decl1, decl2));
        }

        @Test
        @DisplayName("Declaring variable with unknown type throws SemanticException")
        void testUnknownTypeDecl() {
            Statement stmt = new VariableDeclarationStatement("float", "x", null, 1);
            assertThrows(SemanticException.class, () -> analyse(stmt));
        }

        @Test
        @DisplayName("Assignment with null right-hand side expression throws SemanticException")
        void testNullAssignmentExpression() {
            AssignmentStatement stmt = new AssignmentStatement("x", null, 1);
            assertThrows(SemanticException.class, () -> analyse(stmt));
        }

        @Test
        @DisplayName("Assigning to an undeclared variable throws SemanticException")
        void testAssignUndeclared() {
            AssignmentStatement stmt = new AssignmentStatement("x", new LiteralExpression(5, 1), 1);
            assertThrows(SemanticException.class, () -> analyse(stmt));
        }

        @Test
        @DisplayName("Assigning a value to a function name throws SemanticException")
        void testAssignToFunction() {
            FunctionDeclarationStatement func = new FunctionDeclarationStatement(
                    "int", "foo", List.of(), new BlockStatement(List.of(new ReturnStatement(new LiteralExpression(1, 1), 1)), 1), 1
            );
            AssignmentStatement assign = new AssignmentStatement("foo", new LiteralExpression(5, 1), 1);

            assertThrows(SemanticException.class, () -> analyse(func, assign));
        }

        @Test
        @DisplayName("Assignment type mismatch throws SemanticException")
        void testAssignTypeMismatch() {
            Statement decl = new VariableDeclarationStatement("int", "x", new LiteralExpression(5, 1), 1);
            AssignmentStatement assign = new AssignmentStatement("x", new LiteralExpression("str", 1), 1);

            assertThrows(SemanticException.class, () -> analyse(decl, assign));
        }

        @Test
        @DisplayName("Valid variable assignment passes analysis")
        void testValidAssignment() {
            assertDoesNotThrow(() -> analyse(
                    new VariableDeclarationStatement("int", "x", new LiteralExpression(5, 1), 1),
                    new AssignmentStatement("x", new LiteralExpression(10, 1), 1)
            ));
        }
    }

    // --- 2. Control Flow (If, While, For, Break) ---

    @Nested
    @DisplayName("2. Control Flow & Loops")
    class ControlFlowTests {

        @Test
        @DisplayName("If statement with valid condition and optional else passes")
        void testValidIfElse() {
            IfStatement stmt = new IfStatement(
                    new LiteralExpression(true, 1),
                    new BlockStatement(List.of(), 1),
                    new BlockStatement(List.of(), 1),
                    1
            );
            assertDoesNotThrow(() -> analyse(stmt));
        }

        @Test
        @DisplayName("If statement with non-boolean condition throws SemanticException")
        void testInvalidIfCondition() {
            IfStatement stmt = new IfStatement(
                    new LiteralExpression(100, 1),
                    new BlockStatement(List.of(), 1),
                    null,
                    1
            );
            assertThrows(SemanticException.class, () -> analyse(stmt));
        }

        @Test
        @DisplayName("If statement with null body node throws SemanticException")
        void testNullIfBody() {
            IfStatement stmt = new IfStatement(new LiteralExpression(true, 1), null, null, 1);
            assertThrows(SemanticException.class, () -> analyse(stmt));
        }

        @Test
        @DisplayName("While statement with valid condition passes")
        void testValidWhile() {
            WhileStatement stmt = new WhileStatement(
                    new LiteralExpression(true, 1),
                    new BlockStatement(List.of(new BreakStatement(1)), 1),
                    1
            );
            assertDoesNotThrow(() -> analyse(stmt));
        }

        @Test
        @DisplayName("While statement with non-boolean condition throws SemanticException")
        void testInvalidWhileCondition() {
            WhileStatement stmt = new WhileStatement(
                    new LiteralExpression("not bool", 1),
                    new BlockStatement(List.of(), 1),
                    1
            );
            assertThrows(SemanticException.class, () -> analyse(stmt));
        }

        @Test
        @DisplayName("While statement with null body throws SemanticException")
        void testNullWhileBody() {
            WhileStatement stmt = new WhileStatement(new LiteralExpression(true, 1), null, 1);
            assertThrows(SemanticException.class, () -> analyse(stmt));
        }

        @Test
        @DisplayName("For loop with initializer, condition, increment, and body passes")
        void testValidForLoop() {
            ForStatement forLoop = new ForStatement(
                    new VariableDeclarationStatement("int", "i", new LiteralExpression(0, 1), 1),
                    new BinaryExpression(new VariableExpression("i", 1), token(TokenType.LESS_THAN, "<"), new LiteralExpression(10, 1), 1),
                    new AssignmentStatement("i", new BinaryExpression(new VariableExpression("i", 1), token(TokenType.PLUS, "+"), new LiteralExpression(1, 1), 1), 1),
                    new BlockStatement(List.of(new BreakStatement(1)), 1),
                    1
            );
            assertDoesNotThrow(() -> analyse(forLoop));
        }

        @Test
        @DisplayName("For loop with null components (except body) passes")
        void testForLoopNullComponents() {
            ForStatement forLoop = new ForStatement(null, null, null, new BlockStatement(List.of(), 1), 1);
            assertDoesNotThrow(() -> analyse(forLoop));
        }

        @Test
        @DisplayName("For loop with non-boolean condition throws SemanticException")
        void testForLoopNonBooleanCondition() {
            ForStatement forLoop = new ForStatement(
                    null,
                    new LiteralExpression(42, 1),
                    null,
                    new BlockStatement(List.of(), 1),
                    1
            );
            assertThrows(SemanticException.class, () -> analyse(forLoop));
        }

        @Test
        @DisplayName("For loop with null body throws SemanticException")
        void testForLoopNullBody() {
            ForStatement forLoop = new ForStatement(null, null, null, null, 1);
            assertThrows(SemanticException.class, () -> analyse(forLoop));
        }

        @Test
        @DisplayName("Break statement outside loop context throws SemanticException")
        void testBreakOutsideLoop() {
            BreakStatement stmt = new BreakStatement(1);
            assertThrows(SemanticException.class, () -> analyse(stmt));
        }

        @Test
        @DisplayName("Break statement inside nested loops maintains correct loop depth")
        void testNestedLoopsBreak() {
            // for (;;) { while (true) { break; } break; }
            WhileStatement inner = new WhileStatement(
                    new LiteralExpression(true, 1),
                    new BlockStatement(List.of(new BreakStatement(1)), 1),
                    1
            );
            ForStatement outer = new ForStatement(
                    null, null, null,
                    new BlockStatement(List.of(inner, new BreakStatement(1)), 1),
                    1
            );
            assertDoesNotThrow(() -> analyse(outer));
        }
    }

    // --- 3. Functions & Returns ---

    @Nested
    @DisplayName("3. Functions & Return Statements")
    class FunctionTests {

        @Test
        @DisplayName("Return statement outside function scope throws SemanticException")
        void testReturnOutsideFunction() {
            ReturnStatement stmt = new ReturnStatement(new LiteralExpression(10, 1), 1);
            assertThrows(SemanticException.class, () -> analyse(stmt));
        }

        @Test
        @DisplayName("Void function with empty return statement passes")
        void testVoidFunctionEmptyReturn() {
            FunctionDeclarationStatement func = new FunctionDeclarationStatement(
                    "void", "log", List.of(),
                    new BlockStatement(List.of(new ReturnStatement(null, 1)), 1),
                    1
            );
            assertDoesNotThrow(() -> analyse(func));
        }

        @Test
        @DisplayName("Void function returning a value throws SemanticException")
        void testVoidFunctionReturningValue() {
            FunctionDeclarationStatement func = new FunctionDeclarationStatement(
                    "void", "log", List.of(),
                    new BlockStatement(List.of(new ReturnStatement(new LiteralExpression(10, 1), 1)), 1),
                    1
            );
            assertThrows(SemanticException.class, () -> analyse(func));
        }

        @Test
        @DisplayName("Non-void function with empty return statement throws SemanticException")
        void testNonVoidFunctionEmptyReturn() {
            FunctionDeclarationStatement func = new FunctionDeclarationStatement(
                    "int", "getVal", List.of(),
                    new BlockStatement(List.of(new ReturnStatement(null, 1)), 1),
                    1
            );
            assertThrows(SemanticException.class, () -> analyse(func));
        }

        @Test
        @DisplayName("Non-void function returning mismatched type throws SemanticException")
        void testNonVoidFunctionTypeMismatch() {
            FunctionDeclarationStatement func = new FunctionDeclarationStatement(
                    "int", "getVal", List.of(),
                    new BlockStatement(List.of(new ReturnStatement(new LiteralExpression("str", 1), 1)), 1),
                    1
            );
            assertThrows(SemanticException.class, () -> analyse(func));
        }

        @Test
        @DisplayName("Duplicate function declarations throw SemanticException")
        void testDuplicateFunctionDeclaration() {
            FunctionDeclarationStatement f1 = new FunctionDeclarationStatement("void", "run", List.of(), new BlockStatement(List.of(), 1), 1);
            FunctionDeclarationStatement f2 = new FunctionDeclarationStatement("void", "run", List.of(), new BlockStatement(List.of(), 1), 1);
            assertThrows(SemanticException.class, () -> analyse(f1, f2));
        }

        @Test
        @DisplayName("Duplicate parameter names in function declaration throw SemanticException")
        void testDuplicateParameterNames() {
            FunctionDeclarationStatement func = new FunctionDeclarationStatement(
                    "void", "compute",
                    List.of(new Parameter("int", "a"), new Parameter("string", "a")),
                    new BlockStatement(List.of(), 1),
                    1
            );
            assertThrows(SemanticException.class, () -> analyse(func));
        }

        @Test
        @DisplayName("Redeclaring a parameter inside the function body throws SemanticException")
        void testParameterRedeclarationInBody() {
            FunctionDeclarationStatement func = new FunctionDeclarationStatement(
                    "void", "compute",
                    List.of(new Parameter("int", "a")),
                    new BlockStatement(List.of(
                            new VariableDeclarationStatement("int", "a", new LiteralExpression(5, 1), 1)
                    ), 1),
                    1
            );
            assertThrows(SemanticException.class, () -> analyse(func));
        }

        @Test
        @DisplayName("Recursive function call inside body passes analysis")
        void testRecursiveFunctionCall() {
            // def int fact(int n) { return fact(n); }
            FunctionDeclarationStatement func = new FunctionDeclarationStatement(
                    "int", "fact",
                    List.of(new Parameter("int", "n")),
                    new BlockStatement(List.of(
                            new ReturnStatement(
                                    new CallingExpression("fact", List.of(new VariableExpression("n", 1)), 1),
                                    1
                            )
                    ), 1),
                    1
            );
            assertDoesNotThrow(() -> analyse(func));
        }

        @Test
        @DisplayName("Calling non-function symbol throws SemanticException")
        void testCallNonFunction() {
            VariableDeclarationStatement var = new VariableDeclarationStatement("int", "notAFunc", new LiteralExpression(5, 1), 1);
            ExpressionStatement call = new ExpressionStatement(new CallingExpression("notAFunc", List.of(), 1), 1);
            assertThrows(SemanticException.class, () -> analyse(var, call));
        }

        @Test
        @DisplayName("Function call argument count mismatch throws SemanticException")
        void testCallArgumentCountMismatch() {
            FunctionDeclarationStatement func = new FunctionDeclarationStatement(
                    "void", "add", List.of(new Parameter("int", "a"), new Parameter("int", "b")),
                    new BlockStatement(List.of(), 1), 1
            );
            ExpressionStatement call = new ExpressionStatement(
                    new CallingExpression("add", List.of(new LiteralExpression(1, 1)), 1), 1
            );
            assertThrows(SemanticException.class, () -> analyse(func, call));
        }

        @Test
        @DisplayName("Function call argument type mismatch throws SemanticException")
        void testCallArgumentTypeMismatch() {
            FunctionDeclarationStatement func = new FunctionDeclarationStatement(
                    "void", "add", List.of(new Parameter("int", "a")),
                    new BlockStatement(List.of(), 1), 1
            );
            ExpressionStatement call = new ExpressionStatement(
                    new CallingExpression("add", List.of(new LiteralExpression("string arg", 1)), 1), 1
            );
            assertThrows(SemanticException.class, () -> analyse(func, call));
        }
    }

    // --- 4. Binary Expressions ---

    @Nested
    @DisplayName("4. Binary Expressions")
    class BinaryExpressionTests {

        @Test
        @DisplayName("Logical AND/OR require BOOLEAN operands")
        void testLogicalOperators() {
            // Valid boolean logic
            VariableDeclarationStatement valid = new VariableDeclarationStatement(
                    "bool", "res",
                    new BinaryExpression(
                            new LiteralExpression(true, 1),
                            token(TokenType.LOGICAL_AND, "&&"),
                            new LiteralExpression(false, 1),
                            1
                    ), 1
            );
            assertDoesNotThrow(() -> analyse(valid));

            // Invalid non-boolean logic
            VariableDeclarationStatement invalid = new VariableDeclarationStatement(
                    "bool", "res2",
                    new BinaryExpression(
                            new LiteralExpression(10, 1),
                            token(TokenType.LOGICAL_OR, "||"),
                            new LiteralExpression(false, 1),
                            1
                    ), 1
            );
            assertThrows(SemanticException.class, () -> analyse(invalid));
        }

        @ParameterizedTest
        @ValueSource(strings = {"==", "!=", "<", ">", "<=", ">="})
        @DisplayName("Comparison operators return BOOLEAN and check matching types")
        void testComparisonOperators(String op) {
            TokenType type = switch (op) {
                case "==" -> TokenType.EQUAL_EQUAL;
                case "!=" -> TokenType.NOT_EQUAL;
                case "<" -> TokenType.LESS_THAN;
                case ">" -> TokenType.GREATER_THAN;
                case "<=" -> TokenType.LESS_OR_EQUAL;
                case ">=" -> TokenType.GREATER_OR_EQUAL;
                default -> TokenType.ERROR;
            };

            // Valid comparison (matching types)
            VariableDeclarationStatement valid = new VariableDeclarationStatement(
                    "bool", "b",
                    new BinaryExpression(new LiteralExpression(5, 1), token(type, op), new LiteralExpression(10, 1), 1),
                    1
            );
            assertDoesNotThrow(() -> analyse(valid));

            // Invalid comparison (mismatched types)
            VariableDeclarationStatement invalid = new VariableDeclarationStatement(
                    "bool", "b2",
                    new BinaryExpression(new LiteralExpression(5, 1), token(type, op), new LiteralExpression("str", 1), 1),
                    1
            );
            assertThrows(SemanticException.class, () -> analyse(invalid));
        }

        @Test
        @DisplayName("String concatenation with '+' produces Type.STRING")
        void testStringConcatenation() {
            VariableDeclarationStatement stmt = new VariableDeclarationStatement(
                    "string", "s",
                    new BinaryExpression(new LiteralExpression("foo", 1), token(TokenType.PLUS, "+"), new LiteralExpression("bar", 1), 1),
                    1
            );
            assertDoesNotThrow(() -> analyse(stmt));
        }

        @ParameterizedTest
        @ValueSource(strings = {"+", "-", "*", "/", "%", "&", "|", "^"})
        @DisplayName("Arithmetic and bitwise binary operations require INT operands")
        void testArithmeticAndBitwiseOperators(String op) {
            TokenType type = switch (op) {
                case "+" -> TokenType.PLUS;
                case "-" -> TokenType.MINUS;
                case "*" -> TokenType.MULTIPLY;
                case "/" -> TokenType.DIVIDE;
                case "%" -> TokenType.MODULO;
                case "&" -> TokenType.BITWISE_AND;
                case "|" -> TokenType.BITWISE_OR;
                case "^" -> TokenType.BITWISE_XOR;
                default -> TokenType.ERROR;
            };

            // Valid int operation
            VariableDeclarationStatement valid = new VariableDeclarationStatement(
                    "int", "res",
                    new BinaryExpression(new LiteralExpression(20, 1), token(type, op), new LiteralExpression(5, 1), 1),
                    1
            );
            assertDoesNotThrow(() -> analyse(valid));

            // Invalid non-int operation
            VariableDeclarationStatement invalid = new VariableDeclarationStatement(
                    "int", "res2",
                    new BinaryExpression(new LiteralExpression(true, 1), token(type, op), new LiteralExpression(5, 1), 1),
                    1
            );
            assertThrows(SemanticException.class, () -> analyse(invalid));
        }

        @Test
        @DisplayName("Binary expression with unsupported operator returns Type.ERROR")
        void testUnsupportedBinaryOperator() {
            // Assignment token passed inside binary expression returns Type.ERROR, making initializer evaluate to Type.ERROR
            VariableDeclarationStatement stmt = new VariableDeclarationStatement(
                    "int", "res",
                    new BinaryExpression(new LiteralExpression(5, 1), token(TokenType.ASSIGNMENT, "="), new LiteralExpression(5, 1), 1),
                    1
            );
            assertThrows(SemanticException.class, () -> analyse(stmt));
        }

        @Test
        @DisplayName("Binary expression propagates operand Type.ERROR gracefully")
        void testBinaryExpressionErrorPropagation() {
            // Left operand uses undeclared variable (Type.ERROR)
            VariableDeclarationStatement stmt = new VariableDeclarationStatement(
                    "int", "res",
                    new BinaryExpression(new VariableExpression("undeclared", 1), token(TokenType.PLUS, "+"), new LiteralExpression(5, 1), 1),
                    1
            );
            assertThrows(SemanticException.class, () -> analyse(stmt));
        }
    }

    // --- 5. Unary Expressions & Grouping ---

    @Nested
    @DisplayName("5. Unary Expressions & Grouping")
    class UnaryAndGroupingTests {

        @Test
        @DisplayName("Logical NOT (!) requires BOOLEAN operand")
        void testLogicalNot() {
            assertDoesNotThrow(() -> analyse(
                    new VariableDeclarationStatement("bool", "b", new UnaryExpression(token(TokenType.LOGICAL_NOT, "!"), new LiteralExpression(true, 1), 1), 1)
            ));

            assertThrows(SemanticException.class, () -> analyse(
                    new VariableDeclarationStatement("bool", "b", new UnaryExpression(token(TokenType.LOGICAL_NOT, "!"), new LiteralExpression(10, 1), 1), 1)
            ));
        }

        @Test
        @DisplayName("Unary MINUS (-) requires INT operand")
        void testUnaryMinus() {
            assertDoesNotThrow(() -> analyse(
                    new VariableDeclarationStatement("int", "x", new UnaryExpression(token(TokenType.MINUS, "-"), new LiteralExpression(10, 1), 1), 1)
            ));

            assertThrows(SemanticException.class, () -> analyse(
                    new VariableDeclarationStatement("int", "x", new UnaryExpression(token(TokenType.MINUS, "-"), new LiteralExpression("str", 1), 1), 1)
            ));
        }

        @Test
        @DisplayName("Bitwise NOT (~) requires INT operand")
        void testBitwiseNot() {
            assertDoesNotThrow(() -> analyse(
                    new VariableDeclarationStatement("int", "x", new UnaryExpression(token(TokenType.BITWISE_NOT, "~"), new LiteralExpression(10, 1), 1), 1)
            ));

            assertThrows(SemanticException.class, () -> analyse(
                    new VariableDeclarationStatement("int", "x", new UnaryExpression(token(TokenType.BITWISE_NOT, "~"), new LiteralExpression(true, 1), 1), 1)
            ));
        }

        @Test
        @DisplayName("Unary expression with unsupported operator returns Type.ERROR")
        void testUnsupportedUnaryOperator() {
            assertThrows(SemanticException.class, () -> analyse(
                    new VariableDeclarationStatement("int", "x", new UnaryExpression(token(TokenType.PLUS, "+"), new LiteralExpression(10, 1), 1), 1)
            ));
        }

        @Test
        @DisplayName("GroupingExpression passes inner expression type")
        void testGroupingExpression() {
            GroupingExpression grouped = new GroupingExpression(new LiteralExpression(42, 1), 1);
            assertDoesNotThrow(() -> analyse(
                    new VariableDeclarationStatement("int", "x", grouped, 1)
            ));
        }
    }

    // --- 6. Print Statements & Blocks ---

    @Nested
    @DisplayName("6. Print & Block Statements")
    class PrintAndBlockTests {

        @Test
        @DisplayName("Print statement with string expression passes")
        void testValidPrint() {
            assertDoesNotThrow(() -> analyse(
                    new PrintStatement(new LiteralExpression("hello", 1), 1)
            ));
        }

        @Test
        @DisplayName("Print statement with non-string expression throws SemanticException")
        void testInvalidPrint() {
            assertThrows(SemanticException.class, () -> analyse(
                    new PrintStatement(new LiteralExpression(123, 1), 1)
            ));
        }

        @Test
        @DisplayName("Block statement creates isolated scope")
        void testBlockStatementScopeIsolation() {
            // { int inner = 1; } inner = 2; (Should fail outside block)
            BlockStatement block = new BlockStatement(List.of(
                    new VariableDeclarationStatement("int", "inner", new LiteralExpression(1, 1), 1)
            ), 1);
            AssignmentStatement assignOuter = new AssignmentStatement("inner", new LiteralExpression(2, 1), 1);

            assertThrows(SemanticException.class, () -> analyse(block, assignOuter));
        }
    }

    // --- 7. Edge Cases & Fallthrough Coverage ---

    @Nested
    @DisplayName("7. Edge Cases & Fallthrough Coverage")
    class FallthroughAndEdgeCaseTests {

        @Test
        @DisplayName("Unknown AST Statement type throws SemanticException in default branch")
        void testUnknownStatementType() {
            assertThrows(SemanticException.class, () -> analyse(new DummyStatement()));
        }

        @Test
        @DisplayName("Unknown AST Expression type evaluates to Type.ERROR")
        void testUnknownExpressionType() {
            VariableDeclarationStatement stmt = new VariableDeclarationStatement("int", "x", new DummyExpression(), 1);
            assertThrows(SemanticException.class, () -> analyse(stmt));
        }

        @Test
        @DisplayName("Unknown literal value type evaluates to Type.ERROR")
        void testUnknownLiteralValueType() {
            VariableDeclarationStatement stmt = new VariableDeclarationStatement("int", "x", new LiteralExpression(3.14159, 1), 1);
            assertThrows(SemanticException.class, () -> analyse(stmt));
        }

        @Test
        @DisplayName("Type parsing handles case insensitivity and whitespace")
        void testTypeParsingVariations() {
            assertDoesNotThrow(() -> analyse(
                    new VariableDeclarationStatement(" INT ", "a", new LiteralExpression(1, 1), 1),
                    new VariableDeclarationStatement("sTrInG", "b", new LiteralExpression("s", 1), 1),
                    new VariableDeclarationStatement("Bool", "c", new LiteralExpression(true, 1), 1)
            ));
        }
    }
}