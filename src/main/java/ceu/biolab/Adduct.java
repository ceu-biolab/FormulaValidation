package ceu.biolab;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;

/**
 * The class represents a chemical adduct, including its formula, charge, and mass.
 * It can parse adducts from string representations (e.g., '[M+CH3CN+H]+', '[M-3H2O+2H]2+', '[5M+Ca]2+'), and calculates the adduct mass and charge properties.
 *
 * @author Blanca Pueche Granados
 * @author Alberto Gil-de-la-Fuente
 * @since 0.0
 */
public class Adduct {
    private static final double ELECTRON_WEIGHT = 0.00054858;

    private int multimer;
    private Formula formulaPlus;
    private Formula formulaMinus;
    private double adductMass;
    private int charge;
    private ChargeType chargeType;
    private String originalFormula;

    /**
     * Constructor for the ceu.biolab.Adduct class.
     * @param adduct String representation
     * @throws IncorrectFormula If the formula contains invalid elements or values
     * @throws NotFoundElement If the element is not found in the periodic table
     * @throws IncorrectAdduct If the adduct provided is invalid
     */
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
            calculateAdductFormulaToAddAndSubtract(this.originalFormula);
            //this.adductMass = calculateAdductMass(adduct);

            if (match.group(4) != null) {
                this.charge = match.group(3).isEmpty() ? 1 : Integer.parseInt(match.group(3));
                this.chargeType = ChargeType.fromSymbol(match.group(4));
            } else {
                this.charge = 0;
                this.chargeType = ChargeType.fromSymbol("");
            }
        } else {
            throw new IncorrectAdduct(adduct);
        }
    }

    /**
     *
     * @param adductFormula String representing the formula within an adduct in the form +HCOOH-H, +Ca, +H, +CH3COOH-H, etc.
     * @throws IncorrectFormula If the formula contains invalid elements or values
     * @throws NotFoundElement If the element is not found in the periodic table
     * @throws IncorrectAdduct If the adduct provided is invalid
     */
    private void calculateAdductFormulaToAddAndSubtract(String adductFormula) throws IncorrectAdduct, NotFoundElement, IncorrectFormula {

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

            // Convert the subformula string to a ceu.biolab.Formula object
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

        // Create ceu.biolab.Formula objects for the elements to add and subtract
        this.formulaPlus = new Formula(elementsToAdd, null, 0, "");
        this.formulaMinus = new Formula(elementsToSubtract, null, 0, "");

        // Calculate the adduct mass
        this.adductMass = this.formulaPlus.getMonoisotopicMass() - this.formulaMinus.getMonoisotopicMass();
    }


    /**
     * Calculate ceu.biolab.Adduct with the ceu.biolab.Formula in String form
     * @param adductFormula
     * @return Double value for the ceu.biolab.Adduct
     * @throws IncorrectFormula If the formula contains invalid elements or values
     * @throws NotFoundElement If the element is not found in the periodic table
     * @throws IncorrectAdduct If the adduct provided is invalid
     */
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

    /**
     * Get a copy of the multimer value
     * @return A copy of the multimer value
     */
    public int getMultimer() {
        return multimer;
    }

    /**
     * Get a copy of the String representation of a ceu.biolab.Formula
     * @return A copy of the String representation of a ceu.biolab.Formula
     */
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

    /**
     * Get a Formula object representing the elements to add in the adduct.
     * Example: with the adduct [M+H-H2O]+, formulaPlus would be 0   (since H-H2O=-OH)
     * @return The FormulaPlus
     */
    public Formula getFormulaPlus() {
        return formulaPlus;
    }

    /**
     * Get a Formula object representing the elements to subtract in the adduct
     * Example: with the adduct [M+H-H2O]+, formulaMinus would be OH (since H-H2O=-OH)
     * @return The FormulaMinus
     */
    public Formula getFormulaMinus() {
        return formulaMinus;
    }

    /**
     * Get the mass
     * @return The mass
     */
    public double getAdductMass() {
        return adductMass;
    }

    /**
     * Get the charge
     * @return The charge
     */
    public int getAdductCharge() {
        return charge;
    }

    /**
     * Get the charge type
     * @return The charge type
     */
    public ChargeType getAdductChargeType() {
        return chargeType;
    }

    /**
     * Compares this adduct to another object for equality.
     *
     * @param other The object to compare this adduct to.
     * @return true if the adducts are equal, false otherwise.
     */
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

    /**
     * Returns a string representation of the adduct, including multimer, charge, and charge type.
     *
     * @return A string representation of the adduct.
     */
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
