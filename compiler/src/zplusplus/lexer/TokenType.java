package zplusplus.lexer;

/**
 * Types of tokens that the compiler uses to break down ZLang
 * lines into. Includes all built ins, identifiers, literals,
 * operators, delimiters and other tokens
 *
 * @author Zubair Abdul Matin
 */
public enum TokenType {
    // Built ins
    IF,
    ELSE,
    WHILE,
    FOR,
    DEF,
    RETURN,
    BREAK,
    PRINT,
    TRUE,
    FALSE,

    // identifiers
    IDENTIFIER,

    // data types
    STRING_TYPE,
    INT_TYPE,
    BOOLEAN_TYPE,
    VOID_TYPE,

    // literals
    INT,
    STRING,

    // math operators
    ASSIGNMENT,
    PLUS,
    MINUS,
    MULTIPLY,
    DIVIDE,
    MODULO,

    // logical operators
    LOGICAL_AND,
    LOGICAL_OR,
    LOGICAL_NOT,

    // bitwise operators
    BITWISE_AND,
    BITWISE_OR,
    BITWISE_XOR,
    BITWISE_NOT,

    // comparison operators
    EQUAL_EQUAL,
    NOT_EQUAL,
    LESS_THAN,
    GREATER_THAN,
    LESS_OR_EQUAL,
    GREATER_OR_EQUAL,

    // delimiters / punctuation
    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACE,
    RIGHT_BRACE,
    COMMA,
    SEMICOLON,

    // non-visible tokens
    EOF,
    ERROR
}
