package exceptions;// IncorrectFormula.java

/**
 * Exception to raise when a chemical formula is not correct
 */
public class IncorrectFormula extends Exception {
    /**
     * Input value
     */
    private Object inputValue;

    /**
     * Constructor for IncorrectFormula
     * @param inputValue The value causing the exception
     */
    public IncorrectFormula(Object inputValue) {
        super("The classes.Formula " + inputValue + " does not correspond to a correct formula");
        this.inputValue = inputValue;
    }

    /**
     * Provides a string representation of the exception
     * @return String representation of the exception
     */
    @Override
    public String toString() {
        return "The classes.Formula " + inputValue + " does not correspond to a correct formula";
    }

    /**
     * Provides a string representation of the exception
     * @return String representation of the exception
     */
    @Override
    public String getMessage() {
        return "The classes.Formula " + inputValue + " does not correspond to a correct formula";
    }
}

