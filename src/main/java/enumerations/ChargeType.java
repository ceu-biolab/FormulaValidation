
package enumerations;

/**
 * This enumeration represents the values of the charge; positive, negative and neutral
 */
public enum ChargeType {

    /**
     * Represents a positive charge type
     */
    POSITIVE("+"),
    /**
     * Represents a negative charge type
     */
    NEGATIVE("-"),
    /**
     * Represents a neutral charge type
     */
    NEUTRAL("");

    private final String symbol;

    /**
     * The symbol representing each of the values of the enum
     * @param symbol
     */
    ChargeType(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Get a copy of the symbol
     * @return A copy of the symbol
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Get the charge type value from the symbol
     * @param symbol Representation of the different values ("+", "-" or "")
     * @return The type of enum
     */
    public static ChargeType fromSymbol(String symbol) {
        for (ChargeType chargeType : ChargeType.values()) {
            if (chargeType.getSymbol().equals(symbol)) {
                return chargeType;
            }
        }
        throw new IllegalArgumentException("No enum constant for symbol: " + symbol);
    }
}
