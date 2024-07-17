package exceptions;// NotFoundElement.java

/**
 * NotFoundElement.java: Exception to raise when a chemical element is not present
 * Author: Alberto Gil de la Fuente
 * License: GPL License version 3
 */
public class NotFoundElement extends Exception {
    private Object inputValue;

    /**
     * Constructor for NotFoundElement
     * @param inputValue The value causing the exception
     */
    public NotFoundElement(Object inputValue) {
        super("The element " + inputValue + " does not correspond to any element");
        this.inputValue = inputValue;
    }

    /**
     * Provides a string representation of the exception
     * @return String representation of the exception
     */
    @Override
    public String toString() {
        return "The element " + inputValue + " does not correspond to any element";
    }

    /**
     * Provides a string representation of the exception
     * @return String representation of the exception
     */
    @Override
    public String getMessage() {
        return "The element " + inputValue + " does not correspond to any element";
    }
}

