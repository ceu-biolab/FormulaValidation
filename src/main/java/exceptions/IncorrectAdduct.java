
package exceptions;

/**
 * Raised when the adduct format is not valid. An adduct is represented by '[M+CH3CN+H]+', '[M-3H2O+2H]2+' or '[5M+Ca]2+'
 */
public class IncorrectAdduct extends Exception {
    /**
     * Input value
     */
    private String inputValue;

    /**
     * Constructor for IncorrectAdduct
     * @param inputValue The value causing the exception
     */
    public IncorrectAdduct(String inputValue) {
        this.inputValue = inputValue;
    }

    /**
     * Provides a string representation of the exception
     * @return String representation of the exception
     */
    @Override
    public String toString() {
        return "The adduct " + inputValue + " does not correspond to a correct adduct";
    }

    /**
     * Provides a string representation of the exception
     * @return String representation of the exception
     */
    @Override
    public String getMessage() {
        return "The adduct " + inputValue + " does not correspond to a correct adduct";
    }
}

