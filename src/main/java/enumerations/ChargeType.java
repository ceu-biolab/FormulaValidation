package enumerations;

public enum ChargeType {
    POSITIVE("+"),
    NEGATIVE("-"),
    NEUTRAL("");

    private final String symbol;

    ChargeType(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public static ChargeType fromSymbol(String symbol) {
        for (ChargeType chargeType : ChargeType.values()) {
            if (chargeType.getSymbol().equals(symbol)) {
                return chargeType;
            }
        }
        throw new IllegalArgumentException("No enum constant for symbol: " + symbol);
    }
}
