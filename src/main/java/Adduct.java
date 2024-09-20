import exceptions.IncorrectAdduct;
import exceptions.IncorrectFormula;
import exceptions.NotFoundElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;

public class Adduct {
    private static final double ELECTRON_WEIGHT = 0.00054858;

    private int multimer;
    private Formula formulaPlus;
    private Formula formulaMinus;
    private double adductMass;
    private int charge;
    private String chargeType;
    private String originalFormula;

    public Adduct(String adduct) throws IncorrectAdduct, NotFoundElement, IncorrectFormula {
        /*
          adduct (String): A string like '[M+CH3CN+H]+', '[M-3H2O+2H]2+' or '[5M+Ca]2+'
        */
        String pattern = "\\[(\\d*)M([\\+-].*?)\\](\\d*)([\\+-])?";
        String patternNativelyCharged = "\\[(\\d*)M([\\+-].*?)?\\](\\d*)([\\+-])?";
        Pattern regex = Pattern.compile(pattern);
        Matcher match = regex.matcher(adduct);

        if (match.matches()) {
            this.multimer = match.group(1).isEmpty() ? 1 : Integer.parseInt(match.group(1));
            this.originalFormula = match.group(2).trim();

            // Parse the formula to add and subtract elements
            this.formulaPlus = new Formula();  // Placeholder for the actual formula
            this.formulaMinus = new Formula();  // Placeholder for the actual formula
            this.adductMass = calculateAdductMass(this.originalFormula);

            if (match.group(4) != null) {
                this.charge = match.group(3).isEmpty() ? 1 : Integer.parseInt(match.group(3));
                this.chargeType = match.group(4);
            } else {
                this.charge = 0;
                this.chargeType = "";
            }
        } else {
            throw new IncorrectAdduct(adduct);
        }
    }
    private void calculateAdductFormulaToAddAndSubtract(String adductFormula) throws IncorrectAdduct, NotFoundElement, IncorrectFormula {
        /*
         * Args:
         * adductFormula (String): String representing the formula within an adduct in the form +HCOOH-H, +Ca, +H, +CH3COOH-H, etc.
         * Returns:
         * - The formula with the elements of the adduct to add (formulaPlus)
         * - The formula with the elements of the adduct to subtract (formulaMinus)
         * - The adduct mass.
         * Raises:
         * - IncorrectAdduct: if the adduct does not follow the format specified.
         */

        this.adductMass = 0.0;
        // Regex to match symbols (+/-), optional numbers, and formulas
        String pattern = "([\\+-])(\\d*)([A-Za-z0-9]+)";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(adductFormula);

        // Local dictionary to store the final number of elements to add or subtract
        Map<Element.ElementType, Integer> finalAdductElements = new HashMap<>();

        while (matcher.find()) {
            String symbol = matcher.group(1);
            String numberStr = matcher.group(2);
            String subformulaStr = matcher.group(3);

            int numberSubformulas = (numberStr == null || numberStr.isEmpty()) ? 1 : Integer.parseInt(numberStr);

            // Convert the subformula string to a Formula object
            Formula subformula = Formula.formulaFromStringHill(subformulaStr, null, null);
            Map<Element.ElementType, Integer> subformulaElements = subformula.getElements();

            // Update finalAdductElements depending on the symbol (+ or -)
            if (symbol.equals("+")) {
                for (Map.Entry<Element.ElementType, Integer> entry : subformulaElements.entrySet()) {
                    finalAdductElements.put(entry.getKey(), finalAdductElements.getOrDefault(entry.getKey(), 0) + entry.getValue() * numberSubformulas);
                }
            } else if (symbol.equals("-")) {
                for (Map.Entry<Element.ElementType, Integer> entry : subformulaElements.entrySet()) {
                    finalAdductElements.put(entry.getKey(), finalAdductElements.getOrDefault(entry.getKey(), 0) - entry.getValue() * numberSubformulas);
                }
            } else {
                throw new IncorrectAdduct(adductFormula);
            }
        }

        // Separate the elements to add and subtract
        Map<Element.ElementType, Integer> elementsToAdd = new HashMap<>();
        Map<Element.ElementType, Integer> elementsToSubtract = new HashMap<>();

        for (Map.Entry<Element.ElementType, Integer> entry : finalAdductElements.entrySet()) {
            if (entry.getValue() > 0) {
                elementsToAdd.put(entry.getKey(), entry.getValue());
            } else if (entry.getValue() < 0) {
                elementsToSubtract.put(entry.getKey(), -entry.getValue());
            }
        }

        // Create Formula objects for the elements to add and subtract
        this.formulaPlus = new Formula(elementsToAdd, null, 0, null);
        this.formulaMinus = new Formula(elementsToSubtract, null, 0, null);

        // Calculate the adduct mass
        this.adductMass = this.formulaPlus.getMonoisotopicMass() - this.formulaMinus.getMonoisotopicMass();
    }


    // Method to calculate the adduct mass based on the formula
    private double calculateAdductMass(String adductFormula) throws IncorrectAdduct, NotFoundElement, IncorrectFormula {
        String pattern = "([\\+-])(\\d*)([A-Za-z0-9]+)";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(adductFormula);

        while (matcher.find()) {
            String symbol = matcher.group(1);
            int count = matcher.group(2).isEmpty() ? 1 : Integer.parseInt(matcher.group(2));
            String element = matcher.group(3);
            // Adjust mass based on element and symbol (+/-)
            if (symbol.equals("+")) {
                // Add mass of elements
                Formula elementFormula = Formula.formulaFromStringHill(element, null, null);
                formulaPlus = formulaPlus.add(elementFormula.multiply(count));
            } else if (symbol.equals("-")) {
                // Subtract mass of elements
                Formula elementFormula = Formula.formulaFromStringHill(element, null, null);
                formulaMinus = formulaMinus.add(elementFormula.multiply(count));
            } else {
                throw new IncorrectAdduct(adductFormula);
            }
        }

        // Calculate the overall adduct mass difference
        return formulaPlus.getMonoisotopicMass() - formulaMinus.getMonoisotopicMass();
    }

    public int getMultimer() {
        return multimer;
    }

    public String getFormulaStr() {
        StringBuilder formulaStr = new StringBuilder();
        if (formulaPlus != null) {
            formulaStr.append("+").append(formulaPlus.toString());
        }
        if (formulaMinus != null) {
            formulaStr.append("-").append(formulaMinus.toString());
        }
        return formulaStr.toString();
    }

    public Formula getFormulaPlus() {
        return formulaPlus;
    }

    public Formula getFormulaMinus() {
        return formulaMinus;
    }

    public double getAdductMass() {
        return adductMass;
    }

    public int getAdductCharge() {
        return charge;
    }

    public String getAdductChargeType() {
        return chargeType;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Adduct) {
            Adduct otherAdduct = (Adduct) other;
            return this.multimer == otherAdduct.multimer &&
                    this.formulaPlus.equals(otherAdduct.formulaPlus) &&
                    this.formulaMinus.equals(otherAdduct.formulaMinus) &&
                    this.charge == otherAdduct.charge &&
                    this.chargeType.equals(otherAdduct.chargeType);
        }
        return false;
    }

    @Override
    public String toString() {
        String multimerStr = (multimer == 1) ? "" : String.valueOf(multimer);
        String chargeStr = (charge == 1) ? "" : String.valueOf(charge);
        return String.format("[%sM%s]%s%s", multimerStr, getFormulaStr(), chargeStr, chargeType);
    }

    @Override
    public int hashCode() {
        return multimer + formulaPlus.hashCode() + formulaMinus.hashCode() + charge + chargeType.hashCode();
    }
}
