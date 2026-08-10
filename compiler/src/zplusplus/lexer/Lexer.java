package zplusplus.lexer;

import java.util.*;

/**
 * zplusplus.lexer.Lexer breaks down the program into a list of tokens.
 *
 * @author Zubair Abdul Matin
 */
public class Lexer {

    // singleton
    private static final Lexer INSTANCE = new Lexer();

    // line number
    private int lineNumber;

    // private constructor for singleton
    private Lexer() {}

    /**
     * Gets lexer singlton
     * @return zplusplus.lexer.Lexer singlton
     */
    public static Lexer getInstance() {
        return INSTANCE;
    }

    /**
     * Cleans the inputted string file and then converts
     * it into a list of tokens
     *
     * @param input line inputted
     * @return list of tokens
     */
    public List<Token> tokenize(String input) {
        if (input.isEmpty()) {
            return new ArrayList<>();
        }

        // getting the tokens
        List<Token> tokens = new ArrayList<>();
        this.lineNumber = 1;
        int currentPointer = 0;

        // running loop to get tokens
        while (currentPointer < input.length()) {
            currentPointer = scanToken(tokens, input, currentPointer);
        }

        // adding End of File token after loop ends
        tokens.add(new Token(TokenType.EOF, "EOF", lineNumber));

        return tokens;
    }

    /**
     * Scans input and makes tokens
     *
     * @param tokens list of tokens
     * @param input the cleaned input that is being lexed
     * @param currentPointer current pointer
     * @return current pointer
     */
    private int scanToken(List<Token> tokens, String input, int currentPointer) {
        char currentChar = input.charAt(currentPointer);
        currentPointer += 1; // next character

        switch (currentChar) {
            case '(': tokens.add(new Token(TokenType.LEFT_PAREN, "(", lineNumber)); break;
            case ')': tokens.add(new Token(TokenType.RIGHT_PAREN, ")", lineNumber)); break;
            case '{': tokens.add(new Token(TokenType.LEFT_BRACE, "{", lineNumber)); break;
            case '}': tokens.add(new Token(TokenType.RIGHT_BRACE, "}", lineNumber)); break;
            case ',': tokens.add(new Token(TokenType.COMMA, ",", lineNumber)); break;
            case ';': tokens.add(new Token(TokenType.SEMICOLON, ";", lineNumber)); break;

            // Skip any spaces that were left behind
            case ' ': break;
            case '\r': break;
            case '\t': break;

            case '\n':
                lineNumber++;
                break;

            // comparison operators
            case '=':
                if (match(input, '=', currentPointer)) {
                    tokens.add(new Token(TokenType.EQUAL_EQUAL, "==", lineNumber));
                    currentPointer += 1;
                }else {
                    tokens.add(new Token(TokenType.ASSIGNMENT, "=", lineNumber));
                }
                break;

            case '>':
                if (match(input, '=', currentPointer)) {
                    tokens.add(new Token(TokenType.GREATER_OR_EQUAL, ">=", lineNumber));
                    currentPointer += 1;
                }else {
                    tokens.add(new Token(TokenType.GREATER_THAN, ">", lineNumber));
                }
                break;

            case '<':
                if (match(input, '=', currentPointer)) {
                    tokens.add(new Token(TokenType.LESS_OR_EQUAL, "<=", lineNumber));
                    currentPointer += 1;
                }else {
                    tokens.add(new Token(TokenType.LESS_THAN, "<", lineNumber));
                }
                break;

            // math operations
            case '+': tokens.add(new Token(TokenType.PLUS, "+", lineNumber)); break;
            case '-': tokens.add(new Token(TokenType.MINUS, "-", lineNumber)); break;
            case '*': tokens.add(new Token(TokenType.MULTIPLY, "*", lineNumber)); break;
            case '/':
                // checking for comments first and skipping comment lines
                if (match(input, '/', currentPointer)) {
                    // It's a comment! Skip until the end of the line
                    while (currentPointer < input.length() && input.charAt(currentPointer) != '\n') {
                        currentPointer++;
                    }
                } else {
                    tokens.add(new Token(TokenType.DIVIDE, "/", lineNumber)); //
                }
                break;
            case '%': tokens.add(new Token(TokenType.MODULO, "%", lineNumber)); break;

            // logical and bitwise operations
            case '&':
                if (match(input, '&', currentPointer)) {
                    tokens.add(new Token(TokenType.LOGICAL_AND, "&&", lineNumber));
                    currentPointer += 1;
                }else {
                    tokens.add(new Token(TokenType.BITWISE_AND, "&", lineNumber));
                }
                break;

            case '|':
                if (match(input, '|', currentPointer)) {
                    tokens.add(new Token(TokenType.LOGICAL_OR, "||", lineNumber));
                    currentPointer += 1;
                }else {
                    tokens.add(new Token(TokenType.BITWISE_OR, "|", lineNumber));
                }
                break;

            case '!':
                if (match(input, '=', currentPointer)) {
                    tokens.add(new Token(TokenType.NOT_EQUAL, "!=", lineNumber));
                    currentPointer += 1;
                }else {
                    tokens.add(new Token(TokenType.LOGICAL_NOT, "!", lineNumber));
                }
                break;

            case '~': tokens.add(new Token(TokenType.BITWISE_NOT, "~", lineNumber)); break;
            case '^': tokens.add(new Token(TokenType.BITWISE_XOR, "^", lineNumber)); break;

            // checking for string literals
            case '"':
                String stringValue = scanString(input, currentPointer);

                // checks if the string literal is ever closed
                if (currentPointer + stringValue.length() >= input.length()) {
                    tokens.add(new Token(TokenType.ERROR, "Unterminated string literal", lineNumber));
                    currentPointer = input.length(); // Jumps to end
                } else {
                    currentPointer += stringValue.length() + 1;
                    tokens.add(new Token(TokenType.STRING, stringValue, lineNumber));
                }
                break;

            // check for other opcodes
            default:
                if (Character.isDigit(currentChar)) {
                    currentPointer = scanNumber(tokens, input, currentPointer, currentChar);
                } else if (Character.isLetter(currentChar) || currentChar == '_') {
                    currentPointer = scanIdentifierOrKeyword(tokens, input, currentPointer, currentChar);
                } else {
                    tokens.add(new Token(TokenType.ERROR, String.valueOf(currentChar), lineNumber));
                }
                break;
        }

        return currentPointer;

    }

    /**
     * Checks if the next value in the input matches an expected value.
     * Returns true if they do match and false otherwise.
     * @param input line input
     * @param expected expected character
     * @param currentPointer current pointer
     * @return true if they match, false otherwise
     */
    private boolean match(String input, char expected, int currentPointer) {
        if (currentPointer >= input.length()) {
            return false;
        }
        return input.charAt(currentPointer) == expected;
    }

    /**
     * Scans a string starting from the first element after "
     * and loops through the input until another " is found or
     * the input is exhausted. Returns the full string within the two
     * quotation marks.
     * @param input input line
     * @param currentPointer current pointer
     * @return the string of numbers within the quotation mark
     */
    private String scanString(String input, int currentPointer) {
        StringBuilder result = new StringBuilder();
        while (currentPointer < input.length()) {
            if (match(input, '"', currentPointer)) {
                break;
            }

            result.append(input.charAt(currentPointer));
            currentPointer += 1;
        }

        return result.toString();
    }

    /**
     * Scans a number until a space is found or the next
     * character is no longer a digit. Whilst doing so it collects
     * the numbers into a full string and passes that as the value for the token
     * and uses the length of the string to adjust the current pointer
     *
     * @param tokens list of tokens
     * @param input input
     * @param currentPointer current pointer
     * @param currentChar the current character (also needs to be included in the token)
     *
     * @return current pointer
     */
    private int scanNumber(List<Token> tokens, String input, int currentPointer, char currentChar) {
        StringBuilder numberString =  new StringBuilder();
        numberString.append(currentChar);
        while (currentPointer < input.length()) {
            if (Character.isDigit(input.charAt(currentPointer))) {
                numberString.append(input.charAt(currentPointer));
                currentPointer += 1;
            }else {
                break;
            }

        }

        String number = numberString.toString();
        tokens.add(new Token(TokenType.INT, number, lineNumber));

        return currentPointer;
    }

    /**
     * Scans a string of characters to check whether it is
     * a built-in keyword or if it is an identifier and returns
     * the length of that keyword or identifier
     *
     * @param tokens list of tokens
     * @param input input line
     * @param currentPointer current pointer
     * @param currentChar current char (needs to be included in the token as well)
     *
     * @return length of the keyword or identifier that has been tokenized
     */
    private int scanIdentifierOrKeyword(List<Token> tokens, String input, int currentPointer, char currentChar) {
        StringBuilder result = new StringBuilder();
        result.append(currentChar);
        while (currentPointer < input.length()) {
            if (Character.isLetterOrDigit(input.charAt(currentPointer)) ||  input.charAt(currentPointer) == '_') {
                result.append(input.charAt(currentPointer));
                currentPointer += 1;
            }else {
                break;
            }
        }

        String stringResult = result.toString();
        if (!stringResult.isEmpty()) {
            switch (stringResult) {
                case "if": tokens.add(new Token(TokenType.IF, stringResult, lineNumber)); break;
                case "else": tokens.add(new Token(TokenType.ELSE, stringResult, lineNumber)); break;
                case "while": tokens.add(new Token(TokenType.WHILE, stringResult, lineNumber)); break;
                case "for": tokens.add(new Token(TokenType.FOR, stringResult, lineNumber)); break;
                case "def": tokens.add(new Token(TokenType.DEF, stringResult, lineNumber)); break;
                case "return": tokens.add(new Token(TokenType.RETURN, stringResult, lineNumber)); break;
                case "break": tokens.add(new Token(TokenType.BREAK, stringResult, lineNumber)); break;
                case "print": tokens.add(new Token(TokenType.PRINT, stringResult, lineNumber)); break;
                case "input": tokens.add(new Token(TokenType.INPUT, stringResult, lineNumber)); break;
                case "int": tokens.add(new Token(TokenType.INT_TYPE, stringResult, lineNumber)); break;
                case "string": tokens.add(new Token(TokenType.STRING_TYPE, stringResult, lineNumber)); break;
                case "bool": tokens.add(new Token(TokenType.BOOLEAN_TYPE, stringResult, lineNumber)); break;
                case "void": tokens.add(new Token(TokenType.VOID_TYPE, stringResult, lineNumber)); break;
                case "true": tokens.add(new Token(TokenType.TRUE, stringResult, lineNumber)); break;
                case "false": tokens.add(new Token(TokenType.FALSE, stringResult, lineNumber)); break;
                default: tokens.add(new Token(TokenType.IDENTIFIER, stringResult, lineNumber));
            }
        }

        return currentPointer;

    }

}
