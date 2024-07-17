package exceptions;// IncorrectFormula.java

/**
 * IncorrectFormula.java: Exception to raise when a chemical formula is not correct
 * Author: Alberto Gil de la Fuente
 * License: GPL License version 3
 */
public class IncorrectFormula extends Exception {
    private Object inputValue;

    /**
     * Constructor for IncorrectFormula
     * @param inputValue The value causing the exception
     */
    public IncorrectFormula(Object inputValue) {
        super("The Formula " + inputValue + " does not correspond to a correct formula");
        this.inputValue = inputValue;
    }

    /**
     * Provides a string representation of the exception
     * @return String representation of the exception
     */
    @Override
    public String toString() {
        return "The Formula " + inputValue + " does not correspond to a correct formula";
    }

    /**
     * Provides a string representation of the exception
     * @return String representation of the exception
     */
    @Override
    public String getMessage() {
        return "The Formula " + inputValue + " does not correspond to a correct formula";
    }
}

