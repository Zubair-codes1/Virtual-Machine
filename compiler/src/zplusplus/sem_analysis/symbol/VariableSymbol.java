package zplusplus.sem_analysis.symbol;

import zplusplus.sem_analysis.Type;

/**
 * Variable symbol class
 *
 * @author Zubair Abdul Matin
 */
public class VariableSymbol extends Symbol {

    private int slot;

    /**
     * Variable Symbol constructor
     * @param name variable name
     * @param type variable type
     */
    public VariableSymbol(String name, Type type, int slot) {
        super(name, type);

        this.slot = slot;
    }

    /**
     * Displays variable as a string
     * @return variable as a string
     */
    @Override
    public String toString() {
        return getType().toString().toLowerCase() + " " + getName();
    }
}
