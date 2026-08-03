import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import zplusplus.ast.*;
import zplusplus.code_gen.*;
import zplusplus.exceptions.CodeGenException;
import zplusplus.lexer.Token;
import zplusplus.lexer.TokenType;
import zplusplus.sem_analysis.Analyser;
import zplusplus.sem_analysis.Environment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Z++ Code Generator Test Suite")
class CodeGeneratorTest {

    private CodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new CodeGenerator();
    }

    /**
     * Helper to run Semantic Analysis and Code Generation on directly constructed ASTs.
     * Direct AST construction avoids String vs Integer literal parsing ambiguities.
     */
    private String generateCode(List<Statement> statements) {
        Analyser analyser = new Analyser(statements);
        Environment env = analyser.analyse();
        return generator.generate(statements, env);
    }

    private Token token(TokenType type, String value) {
        return new Token(type, value, 1);
    }

    // =========================================================================
    // 1. Code Generator Exception Guards (Direct CodeGen Invocations)
    // =========================================================================

    @Test
    @DisplayName("Throws CodeGenException on empty statement list or null environment")
    void testEmptyOrNullInput() {
        Environment env = new Environment(null);

        assertThrows(CodeGenException.class, () -> generator.generate(List.of(), env));
        assertThrows(CodeGenException.class, () -> generator.generate(List.of(new BreakStatement(1)), null));
    }

    @Test
    @DisplayName("Throws CodeGenException when variable declaration initializer default is invalid type")
    void testInvalidDefaultVariableType() {
        // Bypasses Analyser (which throws SemanticException) to test generator fallback directly
        VariableDeclarationStatement badVar = new VariableDeclarationStatement("float", "x", null, 1);
        Environment env = new Environment(null);

        assertThrows(CodeGenException.class, () -> generator.generate(List.of(badVar), env));
    }

    @Test
    @DisplayName("Throws CodeGenException when invalid literal value type is processed")
    void testInvalidLiteralExpressionType() {
        // Bypasses Analyser to test generator literal type checking directly
        LiteralExpression badLiteral = new LiteralExpression(3.14f, 1);
        PrintStatement printStmt = new PrintStatement(badLiteral, 1);
        BlockStatement body = new BlockStatement(List.of(printStmt), 1);
        FunctionDeclarationStatement mainFunc = new FunctionDeclarationStatement("void", "main", List.of(), body, 1);

        Environment env = new Environment(null);

        assertThrows(CodeGenException.class, () -> generator.generate(List.of(mainFunc), env));
    }

    @Test
    @DisplayName("Throws CodeGenException when invalid binary operator token is processed")
    void testInvalidBinaryOperator() {
        BinaryExpression badBinExp = new BinaryExpression(
                new LiteralExpression(1, 1),
                token(TokenType.ERROR, "**"),
                new LiteralExpression(2, 1),
                1
        );
        ExpressionStatement exprStmt = new ExpressionStatement(badBinExp, 1);
        BlockStatement body = new BlockStatement(List.of(exprStmt), 1);
        FunctionDeclarationStatement mainFunc = new FunctionDeclarationStatement("void", "main", List.of(), body, 1);

        Environment env = new Environment(null);

        assertThrows(CodeGenException.class, () -> generator.generate(List.of(mainFunc), env));
    }

    // =========================================================================
    // 2. Global Variables & Main Bootstrapping
    // =========================================================================

    @Test
    @DisplayName("Emits global variable storage and main entry call/halt sequence")
    void testGlobalVariablesAndMainBootstrap() {
        List<Statement> statements = List.of(
                new VariableDeclarationStatement("int", "gInt", new LiteralExpression(100, 1), 1),
                new VariableDeclarationStatement("bool", "gBool", new LiteralExpression(true, 1), 1),
                new VariableDeclarationStatement("string", "gStr", new LiteralExpression("hello", 1), 1),
                new FunctionDeclarationStatement(
                        "void", "main", List.of(),
                        new BlockStatement(List.of(new ReturnStatement(null, 1)), 1), 1
                )
        );

        String zasm = generateCode(statements);

        assertTrue(zasm.contains("PUSH 100"));
        assertTrue(zasm.contains("STORE gInt"));
        assertTrue(zasm.contains("PUSH 1"));
        assertTrue(zasm.contains("STORE gBool"));
        assertTrue(zasm.contains("PUSH_STR \"hello\""));
        assertTrue(zasm.contains("STORE gStr"));

        assertTrue(zasm.contains("CALL :main"));
        assertTrue(zasm.contains("HALT"));
        assertTrue(zasm.contains(":main"));
        assertTrue(zasm.contains("RET"));
    }

    // =========================================================================
    // 3. Local Variables & Variable Assignments
    // =========================================================================

    @Test
    @DisplayName("Generates correct local variable slot stores and assignment targets")
    void testLocalVariablesAndAssignments() {
        List<Statement> statements = List.of(
                new VariableDeclarationStatement("int", "gVar", new LiteralExpression(0, 1), 1),
                new FunctionDeclarationStatement(
                        "void", "main", List.of(),
                        new BlockStatement(List.of(
                                new VariableDeclarationStatement("int", "a", new LiteralExpression(5, 1), 1),
                                new VariableDeclarationStatement("string", "b", new LiteralExpression("init", 1), 1),
                                new AssignmentStatement("a", new LiteralExpression(10, 1), 1),
                                new AssignmentStatement("gVar", new LiteralExpression(20, 1), 1),
                                new ReturnStatement(null, 1)
                        ), 1), 1
                )
        );

        String zasm = generateCode(statements);

        assertTrue(zasm.contains("PUSH 5"));
        assertTrue(zasm.contains("STORE_LOCAL 0"));
        assertTrue(zasm.contains("PUSH 10"));
        assertTrue(zasm.contains("STORE_LOCAL 0"));
        assertTrue(zasm.contains("PUSH 20"));
        assertTrue(zasm.contains("STORE gVar"));
    }

    // =========================================================================
    // 4. Expression Lowering: Arithmetic, Relational & Bitwise
    // =========================================================================

    @Test
    @DisplayName("Emits correct ZASM opcodes for all math, bitwise, and comparison operators")
    void testBinaryOperators() {
        List<Statement> mainBody = List.of(
                new VariableDeclarationStatement("int", "a", new BinaryExpression(new LiteralExpression(10, 1), token(TokenType.PLUS, "+"), new LiteralExpression(20, 1), 1), 1),
                new VariableDeclarationStatement("int", "b", new BinaryExpression(new LiteralExpression(20, 1), token(TokenType.MINUS, "-"), new LiteralExpression(5, 1), 1), 1),
                new VariableDeclarationStatement("int", "c", new BinaryExpression(new LiteralExpression(5, 1), token(TokenType.MULTIPLY, "*"), new LiteralExpression(4, 1), 1), 1),
                new VariableDeclarationStatement("int", "d", new BinaryExpression(new LiteralExpression(20, 1), token(TokenType.DIVIDE, "/"), new LiteralExpression(4, 1), 1), 1),
                new VariableDeclarationStatement("int", "e", new BinaryExpression(new LiteralExpression(10, 1), token(TokenType.MODULO, "%"), new LiteralExpression(3, 1), 1), 1),
                new VariableDeclarationStatement("bool", "f", new BinaryExpression(new LiteralExpression(10, 1), token(TokenType.GREATER_OR_EQUAL, ">="), new LiteralExpression(5, 1), 1), 1),
                new VariableDeclarationStatement("bool", "g", new BinaryExpression(new LiteralExpression(5, 1), token(TokenType.LESS_OR_EQUAL, "<="), new LiteralExpression(10, 1), 1), 1),
                new VariableDeclarationStatement("bool", "h", new BinaryExpression(new LiteralExpression(10, 1), token(TokenType.GREATER_THAN, ">"), new LiteralExpression(5, 1), 1), 1),
                new VariableDeclarationStatement("bool", "i", new BinaryExpression(new LiteralExpression(5, 1), token(TokenType.LESS_THAN, "<"), new LiteralExpression(10, 1), 1), 1),
                new VariableDeclarationStatement("bool", "j", new BinaryExpression(new LiteralExpression(10, 1), token(TokenType.EQUAL_EQUAL, "=="), new LiteralExpression(10, 1), 1), 1),
                new VariableDeclarationStatement("bool", "k", new BinaryExpression(new LiteralExpression(10, 1), token(TokenType.NOT_EQUAL, "!="), new LiteralExpression(5, 1), 1), 1),
                new VariableDeclarationStatement("int", "l", new BinaryExpression(new LiteralExpression(6, 1), token(TokenType.BITWISE_AND, "&"), new LiteralExpression(3, 1), 1), 1),
                new VariableDeclarationStatement("int", "m", new BinaryExpression(new LiteralExpression(6, 1), token(TokenType.BITWISE_OR, "|"), new LiteralExpression(3, 1), 1), 1),
                new VariableDeclarationStatement("int", "n", new BinaryExpression(new LiteralExpression(6, 1), token(TokenType.BITWISE_XOR, "^"), new LiteralExpression(3, 1), 1), 1),
                new ReturnStatement(null, 1)
        );

        List<Statement> statements = List.of(
                new FunctionDeclarationStatement("void", "main", List.of(), new BlockStatement(mainBody, 1), 1)
        );

        String zasm = generateCode(statements);

        assertTrue(zasm.contains("ADD"));
        assertTrue(zasm.contains("SUB"));
        assertTrue(zasm.contains("MULT"));
        assertTrue(zasm.contains("DIV"));
        assertTrue(zasm.contains("MOD"));
        assertTrue(zasm.contains("GTE"));
        assertTrue(zasm.contains("LTE"));
        assertTrue(zasm.contains("GT"));
        assertTrue(zasm.contains("LT"));
        assertTrue(zasm.contains("EQ"));
        assertTrue(zasm.contains("NEQ"));
        assertTrue(zasm.contains("AND"));
        assertTrue(zasm.contains("OR"));
        assertTrue(zasm.contains("XOR"));
    }

    @Test
    @DisplayName("Emits branching labels for short-circuit logical AND and OR")
    void testLogicalShortCircuitOperators() {
        List<Statement> mainBody = List.of(
                new VariableDeclarationStatement("bool", "a", new BinaryExpression(new LiteralExpression(true, 1), token(TokenType.LOGICAL_AND, "&&"), new LiteralExpression(false, 1), 1), 1),
                new VariableDeclarationStatement("bool", "b", new BinaryExpression(new LiteralExpression(false, 1), token(TokenType.LOGICAL_OR, "||"), new LiteralExpression(true, 1), 1), 1),
                new ReturnStatement(null, 1)
        );

        List<Statement> statements = List.of(
                new FunctionDeclarationStatement("void", "main", List.of(), new BlockStatement(mainBody, 1), 1)
        );

        String zasm = generateCode(statements);

        assertTrue(zasm.contains("JIF :and_false_0"));
        assertTrue(zasm.contains(":and_false_0"));
        assertTrue(zasm.contains(":and_end_1"));

        assertTrue(zasm.contains("JIT :or_false_2"));
        assertTrue(zasm.contains(":or_false_2"));
        assertTrue(zasm.contains(":or_end_3"));
    }

    @Test
    @DisplayName("Emits instructions for unary minus, logical NOT, and bitwise NOT")
    void testUnaryOperators() {
        List<Statement> mainBody = List.of(
                new VariableDeclarationStatement("int", "x", new UnaryExpression(token(TokenType.MINUS, "-"), new LiteralExpression(5, 1), 1), 1),
                new VariableDeclarationStatement("bool", "y", new UnaryExpression(token(TokenType.LOGICAL_NOT, "!"), new LiteralExpression(true, 1), 1), 1),
                new VariableDeclarationStatement("int", "z", new UnaryExpression(token(TokenType.BITWISE_NOT, "~"), new LiteralExpression(10, 1), 1), 1),
                new ReturnStatement(null, 1)
        );

        List<Statement> statements = List.of(
                new FunctionDeclarationStatement("void", "main", List.of(), new BlockStatement(mainBody, 1), 1)
        );

        String zasm = generateCode(statements);

        assertTrue(zasm.contains("PUSH 0\n\tSWAP\n\tSUB"));
        assertTrue(zasm.contains("PUSH 0\n\tEQ"));
        assertTrue(zasm.contains("NOT"));
    }

    // =========================================================================
    // 5. Control Flow: If-Else, Loops & Break
    // =========================================================================

    @Test
    @DisplayName("Emits conditional jumps for If and If-Else blocks")
    void testIfElseControlFlow() {
        Statement print1 = new PrintStatement(new LiteralExpression("1", 1), 1);
        Statement print2 = new PrintStatement(new LiteralExpression("2", 1), 1);
        Statement print3 = new PrintStatement(new LiteralExpression("3", 1), 1);

        IfStatement ifElse = new IfStatement(
                new LiteralExpression(true, 1),
                new BlockStatement(List.of(print1), 1),
                new BlockStatement(List.of(print2), 1),
                1
        );

        IfStatement simpleIf = new IfStatement(
                new LiteralExpression(false, 1),
                new BlockStatement(List.of(print3), 1),
                null,
                1
        );

        List<Statement> statements = List.of(
                new FunctionDeclarationStatement("void", "main", List.of(), new BlockStatement(List.of(ifElse, simpleIf, new ReturnStatement(null, 1)), 1), 1)
        );

        String zasm = generateCode(statements);

        assertTrue(zasm.contains("JIF :elseLabel_0"));
        assertTrue(zasm.contains("JUMP :endLabel_1"));
        assertTrue(zasm.contains(":elseLabel_0"));
        assertTrue(zasm.contains(":endLabel_1"));
        assertTrue(zasm.contains("JIF :endLabel_2"));
        assertTrue(zasm.contains(":endLabel_2"));
    }

    @Test
    @DisplayName("Emits loop labels and jumps for While, For, and Break statements")
    void testLoopsAndBreak() {
        WhileStatement whileLoop = new WhileStatement(
                new LiteralExpression(true, 1),
                new BlockStatement(List.of(new BreakStatement(1)), 1),
                1
        );

        ForStatement forLoop = new ForStatement(
                new VariableDeclarationStatement("int", "i", new LiteralExpression(0, 1), 1),
                new BinaryExpression(new VariableExpression("i", 1), token(TokenType.LESS_THAN, "<"), new LiteralExpression(10, 1), 1),
                new AssignmentStatement("i", new BinaryExpression(new VariableExpression("i", 1), token(TokenType.PLUS, "+"), new LiteralExpression(1, 1), 1), 1),
                new BlockStatement(List.of(new BreakStatement(1)), 1),
                1
        );

        List<Statement> statements = List.of(
                new FunctionDeclarationStatement("void", "main", List.of(), new BlockStatement(List.of(whileLoop, forLoop, new ReturnStatement(null, 1)), 1), 1)
        );

        String zasm = generateCode(statements);

        assertTrue(zasm.contains(":startLabel_0"));
        assertTrue(zasm.contains("JIF :endLabel_1"));
        assertTrue(zasm.contains("JUMP :endLabel_1"));

        assertTrue(zasm.contains(":startLabel_2"));
        assertTrue(zasm.contains("JIF :endLabel_3"));
        assertTrue(zasm.contains("JUMP :endLabel_3"));
    }

    // =========================================================================
    // 6. Function Calls, Parameter Offsets & Expression Statements
    // =========================================================================

    @Test
    @DisplayName("Pops function parameters in reverse order and handles return values")
    void testFunctionsAndCalls() {
        FunctionDeclarationStatement addFunc = new FunctionDeclarationStatement(
                "int", "add",
                List.of(new Parameter("int", "a"), new Parameter("int", "b")),
                new BlockStatement(List.of(
                        new ReturnStatement(new BinaryExpression(new VariableExpression("a", 1), token(TokenType.PLUS, "+"), new VariableExpression("b", 1), 1), 1)
                ), 1), 1
        );

        FunctionDeclarationStatement doNothingFunc = new FunctionDeclarationStatement(
                "void", "doNothing", List.of(),
                new BlockStatement(List.of(new ReturnStatement(null, 1)), 1), 1
        );

        List<Statement> mainBody = List.of(
                new VariableDeclarationStatement("int", "res", new CallingExpression("add", List.of(new LiteralExpression(5, 1), new LiteralExpression(10, 1)), 1), 1),
                new ExpressionStatement(new CallingExpression("add", List.of(new LiteralExpression(1, 1), new LiteralExpression(2, 1)), 1), 1),
                new ExpressionStatement(new CallingExpression("doNothing", List.of(), 1), 1),
                new ReturnStatement(null, 1)
        );

        FunctionDeclarationStatement mainFunc = new FunctionDeclarationStatement("void", "main", List.of(), new BlockStatement(mainBody, 1), 1);

        String zasm = generateCode(List.of(addFunc, doNothingFunc, mainFunc));

        assertTrue(zasm.contains(":add"));
        assertTrue(zasm.contains("STORE_LOCAL 1"));
        assertTrue(zasm.contains("STORE_LOCAL 0"));

        assertTrue(zasm.contains("CALL :add"));
        assertTrue(zasm.contains("CALL :doNothing"));
        assertTrue(zasm.contains("POP"));
    }

    // =========================================================================
    // 7. Print Statement Type Lowering (Valid STRING expressions only)
    // =========================================================================

    @Test
    @DisplayName("Emits PRINT_STR for string literals, string variables, and string-returning functions")
    void testPrintTypeLowering() {
        FunctionDeclarationStatement getGreetingFunc = new FunctionDeclarationStatement(
                "string", "getGreeting", List.of(),
                new BlockStatement(List.of(new ReturnStatement(new LiteralExpression("hello", 1), 1)), 1), 1
        );

        List<Statement> mainBody = List.of(
                new PrintStatement(new LiteralExpression("world", 1), 1),
                new VariableDeclarationStatement("string", "msg", new LiteralExpression("foo", 1), 1),
                new PrintStatement(new VariableExpression("msg", 1), 1),
                new PrintStatement(new CallingExpression("getGreeting", List.of(), 1), 1),
                new ReturnStatement(null, 1)
        );

        FunctionDeclarationStatement mainFunc = new FunctionDeclarationStatement("void", "main", List.of(), new BlockStatement(mainBody, 1), 1);

        String zasm = generateCode(List.of(getGreetingFunc, mainFunc));

        assertTrue(zasm.contains("PUSH_STR \"world\""));
        assertTrue(zasm.contains("PRINT_STR"));
    }
}