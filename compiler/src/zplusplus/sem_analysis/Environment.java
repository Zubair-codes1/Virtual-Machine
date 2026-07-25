package zplusplus.sem_analysis;

import java.util.HashMap;
import java.util.Map;

import zplusplus.exceptions.SemanticException;
import zplusplus.sem_analysis.symbol.Symbol;

/**
 * Environment/Scope node class to hold information for each scope level
 * Holds its own symbols and the environment of its parent scope
 *
 * @author Zubair Abdul Matin
 */
public class Environment {
    private Environment parentEnvironment;
    private Map<String, Symbol> table = new HashMap<>();

    /**
     * Constructor for Environment/Scope/Symbol table
     * @param parentEnvironment parent passed in
     */
    public Environment(Environment parentEnvironment) {
        this.parentEnvironment = parentEnvironment;
    }

    /**
     * Adds a symbol to the table along with its name as
     * an identifier.
     * @param symbol symbol instance
     */
    public boolean addToTable(Symbol symbol) {
        if (table.containsKey(symbol.getName())) {
            return false;
        }
        table.put(symbol.getName(), symbol);
        return true;
    }

    /**
     * Gets a symbol from the symbol table, if not present then
     * recursively checks parent environments until found. If not then
     * returns null
     * @param name name of symbol/varibale/function
     * @return symbol
     */
    public Symbol getSymbol(String name) {
        if (table.containsKey(name)) {
            return table.get(name);
        }else if (parentEnvironment != null) {
            return parentEnvironment.getSymbol(name);
        }
        return null;
    }

    /**
     * Getter method for parent environment/scope
     * @return parent environment
     */
    public Environment getParentEnvironment() {
        return parentEnvironment;
    }

    public Map<String, Symbol> getTable() {
        return table;
    }

}
