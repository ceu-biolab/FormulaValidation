package exceptions;

public class IncorrectAdduct extends Exception {
    private String inputValue;

    public IncorrectAdduct(String inputValue) {
        this.inputValue = inputValue;
    }

    @Override
    public String toString() {
        return "The adduct " + inputValue + " does not correspond to a correct adduct";
    }

    @Override
    public String getMessage() {
        return "The adduct " + inputValue + " does not correspond to a correct adduct";
    }
}

