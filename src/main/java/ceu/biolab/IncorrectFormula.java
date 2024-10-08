package ceu.biolab;// IncorrectFormula.java

/**
 *  Exception to raise when a chemical formula is not correct
 *
 * @author Blanca Pueche Granados
 * @author Alberto Gil-de-la-Fuente
 * @since 0.0
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
        super("The ceu.biolab.Formula " + inputValue + " does not correspond to a correct formula");
        this.inputValue = inputValue;
    }

    /**
     * Provides a string representation of the exception
     * @return String representation of the exception
     */
    @Override
    public String toString() {
        return "The ceu.biolab.Formula " + inputValue + " does not correspond to a correct formula";
    }

    /**
     * Provides a string representation of the exception
     * @return String representation of the exception
     */
    @Override
    public String getMessage() {
        return "The ceu.biolab.Formula " + inputValue + " does not correspond to a correct formula";
    }
}

