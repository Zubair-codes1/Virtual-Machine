package zplusplus.ast;

/**
 * Input statement ast node
 *
 * @author Zubair Abdul Matin
 */
public class InputStatement extends Statement {

    // variable that the input will be stored in
    private String variable;

    /**
     * Constructor for input statement to initialise the
     * variable
     * @param variable variable that input will be set to
     * @param lineNumber line number of input statement
     */
    public InputStatement(String variable, int lineNumber) {
        super(lineNumber);

        this.variable = variable;
    }

    /**
     * variable getter
     * @return variable
     */
    public String getVariable() {
        return variable;
    }

    /**
     * Converts input statement to string for better
     * debugging and error messages
     * @return input statement string
     */
    @Override
    public String toString() {
        return "input(" + variable + ");";
    }
}
