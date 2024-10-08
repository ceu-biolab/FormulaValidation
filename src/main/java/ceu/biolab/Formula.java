
package ceu.biolab;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dan2097.jnainchi.InchiStatus;
import net.sf.jniinchi.INCHI_RET;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * The ceu.biolab.Formula class represents a chemical formula and its associated porperties.
 * It provides several methods to calculate monoisotopic mass, handle adducts and manipulate chemical formulas.
 *
 * @author Blanca Pueche Granados
 * @author Alberto Gil-de-la-Fuente
 * @since 0.0
 */
public class Formula {
    private static final double ELECTRON_WEIGHT = 0.00054858;
    private static final String CC_URL = "https://www.chemcalc.org/chemcalc/mf";
    private static final int DEFAULT_PPM = 50; //Default part per million tolerance

    private Map<Element.ElementType,Integer> elements; //Map of elements and their quantities
    private String adduct;
    private int charge;
    private ChargeType chargeType; //Positive, negative of neutral
    private double monoisotopicMass;
    private double monoisotopicMassWithAdduct;
    private Map<String, Object> metadata;

    /**
     * Constructor for the ceu.biolab.Formula class.
     *
     * @param elements The map of elements and their quantities in the formula.
     * @param adduct The adduct string associated with the formula.
     * @param charge The charge of the formula.
     * @param chargeType The type of charge (positive, negative, or neutral).
     * @param metadata Additional metadata for the formula, usually a String representation of the formula used to create Formula.
     * @throws IncorrectFormula If the formula contains invalid elements or values.
     * @throws NotFoundElement If the element is not found in the periodic table.
     * @throws IncorrectAdduct If the adduct provided is invalid.
     */
    public Formula(Map<Element.ElementType, Integer> elements, String adduct, int charge, String chargeType, Map<String, Object> metadata) throws IncorrectFormula, NotFoundElement, IncorrectAdduct {
        this.metadata = metadata != null ? metadata : new HashMap<>();

        this.elements = new HashMap<>();
        for (Map.Entry<Element.ElementType, Integer> entry : elements.entrySet()) {
            if (entry.getValue() <= 0) {
                throw new IncorrectFormula(elements);
            }
            this.elements.put(entry.getKey(), this.elements.getOrDefault(entry.getKey(), 0) + entry.getValue());
        }

        this.charge = charge;
        if (chargeType.equals("") || chargeType.equals("+") || chargeType.equals("-")) {
            this.chargeType = ChargeType.fromSymbol(chargeType);
        } else {
            throw new IncorrectFormula("charge_type " + chargeType + " invalid. It should be +, - or empty");
        }

        if (adduct == null || adduct.equals("None") || adduct.equals("")) {
            this.adduct = null;
        } else {
            this.adduct = adduct;
        }

        this.monoisotopicMass = calculateMonoisotopicMass();
        this.monoisotopicMassWithAdduct = calculateMonoisotopicMassWithAdduct();
    }

    /**
     * Simplified constructor for the ceu.biolab.Formula class with no metadata.
     *
     * @param elements The map of elements and their quantities in the formula.
     * @param adduct The adduct string associated with the formula.
     * @param charge The charge of the formula.
     * @param chargeType The type of charge (positive, negative, or neutral).
     * @throws IncorrectFormula If the formula contains invalid elements or values.
     * @throws NotFoundElement If the element is not found in the periodic table.
     * @throws IncorrectAdduct If the adduct provided is invalid.
     */
    public Formula(Map<Element.ElementType, Integer> elements, String adduct, int charge, String chargeType) throws IncorrectFormula, IncorrectAdduct, NotFoundElement {
        this.elements = new HashMap<>();
        for (Map.Entry<Element.ElementType, Integer> entry : elements.entrySet()) {
            if (entry.getValue() <= 0) {
                throw new IncorrectFormula(elements);
            }
            this.elements.put(entry.getKey(), this.elements.getOrDefault(entry.getKey(), 0) + entry.getValue());
        }

        this.charge = charge;
        if (chargeType.equals("") || chargeType.equals("+") || chargeType.equals("-")) {
            this.chargeType = ChargeType.fromSymbol(chargeType);
        } else {
            throw new IncorrectFormula("charge_type " + chargeType + " invalid. It should be +, - or empty");
        }

        if (adduct == null || adduct.equals("None") || adduct.equals("")) {
            this.adduct = null;
        } else {
            this.adduct = adduct;
        }

        this.monoisotopicMass = calculateMonoisotopicMass();
        this.monoisotopicMassWithAdduct = calculateMonoisotopicMassWithAdduct();
    }

    /**
     * Compares this formula to another object for equality.
     *
     * @param other The object to compare this formula to.
     * @return true if the formulas are equal, false otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof Formula) {
            Formula otherFormula = (Formula) other;
            return this.elements.equals(otherFormula.getElements()) &&
                    (this.adduct == null ? otherFormula.adduct == null : this.adduct.equals(otherFormula.adduct));
        }
        return false;
    }

    /**
     * Returns a string representation of the formula, including elements, charge, and adduct.
     *
     * @return A string representation of the formula.
     */
    @Override
    public String toString() {
        StringBuilder formulaString = new StringBuilder();

        // Append elements
        for (Map.Entry<Element.ElementType, Integer> entry : this.elements.entrySet()) {
            formulaString.append(entry.getKey()).append(entry.getValue() > 1 ? entry.getValue() : "");
        }

        // Handle charge
        String chargeStr = "";
        if (!this.chargeType.equals("")) {
            chargeStr = this.charge == 1 ? this.chargeType.getSymbol() : this.chargeType.getSymbol() + this.charge;
        }

        // Handle adduct
        String adductStr = this.adduct == null ? "" : this.adduct;

        // Return the result as formula charge adduct without quotation marks
        return String.format("%s %s %s", formulaString.toString(), chargeStr, adductStr);
    }


    /**
     * Return a string representation of the final formula plus or minus de the adduct.
     * @return A string representation of the ceu.biolab.Formula object (C12H3N3O+[M-H2O+H]+) in the format '[C12H2N3]+'
     * @throws IncorrectFormula If the formula contains invalid elements or values.
     * @throws NotFoundElement If the element is not found in the periodic table.
     * @throws IncorrectAdduct If the adduct provided is invalid.
     */
    public String getFinalFormulaWithAdduct() throws IncorrectFormula, IncorrectAdduct, NotFoundElement {
        /*
         * Returns a string representation of the final formula plus or minus the adduct.
         * e.g., "[C12H3N3O]+"
         */
        if (this.adduct == null) {
            return this.toString();  // Return the string representation of the formula
        }

        Adduct adductNew = new Adduct(this.adduct);

        // Multiply the formula by the adduct multimer
        Formula finalFormula = this.multiply(adductNew.getMultimer());

        // If the adduct charge is 0, just add or subtract the formulas
        if (adductNew.getAdductCharge() == 0) {
            Formula formulaPlus = adductNew.getFormulaPlus();
            Formula formulaMinus = adductNew.getFormulaMinus();

            finalFormula = finalFormula.add(formulaPlus).subtract(formulaMinus);
            return finalFormula.toString();
        } else {
            // Handle charge adjustments for the final formula
            int ownCharge;
            if (this.chargeType.equals("+")) {
                ownCharge = this.charge;
            } else if (this.chargeType.equals("-")) {
                ownCharge = -this.charge;
            } else {
                ownCharge = 0;
            }

            int adductCharge;
            if (adductNew.getAdductChargeType().equals("+")) {
                adductCharge = adductNew.getAdductCharge();
            } else if (adductNew.getAdductChargeType().equals("-")) {
                adductCharge = -adductNew.getAdductCharge();
            } else {
                adductCharge = 0;
            }

            int finalCharge = ownCharge + adductCharge;
            String finalChargeStr;
            if (finalCharge == 0) {
                finalChargeStr = "";
            } else if (finalCharge == 1) {
                finalChargeStr = "+";
            } else if (finalCharge > 1) {
                finalChargeStr = "+" + finalCharge;
            } else if (finalCharge == -1) {
                finalChargeStr = "-";
            } else {
                finalChargeStr = "-" + Math.abs(finalCharge);
            }

            // Apply formulaPlus and formulaMinus to the final formula
            Formula formulaPlus = adductNew.getFormulaPlus();
            Formula formulaMinus = adductNew.getFormulaMinus();

            finalFormula = finalFormula.add(formulaPlus).subtract(formulaMinus);

            // Convert finalFormula elements to string
            StringBuilder formulaString = new StringBuilder();
            for (Map.Entry<Element.ElementType, Integer> entry : finalFormula.getElements().entrySet()) {
                formulaString.append(entry.getKey().name());
                if (entry.getValue() > 1) {
                    formulaString.append(entry.getValue());
                }
            }

            // Enclose the formula in brackets and add the final charge type
            return "[" + formulaString.toString() + "]" + finalChargeStr;
        }
    }

    @Override
    public int hashCode() {
        return this.elements.hashCode();
    }

    /**
     * Addition of two Formulas
     * @param other another formula to add the elements with the current one and keeps the adduct of the current formula
     * @return ceu.biolab.Formula resulting from the addition of the chemical elements of both formulas
     * @throws IncorrectFormula If the formula contains invalid elements or values
     * @throws NotFoundElement If the element is not found in the periodic table
     * @throws IncorrectAdduct If the adduct provided is invalid
     */
    public Formula add(Formula other) throws IncorrectFormula, IncorrectAdduct, NotFoundElement {
        Map<Element.ElementType, Integer> newElements = new HashMap<>(this.elements);
        for (Map.Entry<Element.ElementType, Integer> entry : other.elements.entrySet()) {
            newElements.put(entry.getKey(), newElements.getOrDefault(entry.getKey(), 0) + entry.getValue());
        }
        int newCharge = this.chargeType.equals("-") ? -this.charge : this.charge;
        newCharge = other.chargeType.equals("-") ? newCharge - other.charge : newCharge + other.charge;
        String newChargeType = newCharge == 0 ? "" : (newCharge > 0 ? "+" : "-");
        return new Formula(newElements, this.adduct, Math.abs(newCharge), newChargeType);
    }

    /**
     * Subtraction of two Formulas
     * @param other another formula to subtract the elements from the current one and keeps the adduct of the current formula
     * @return ceu.biolab.Formula resulting from the subtraction of the chemical elements
     * @throws IncorrectFormula If the formula contains invalid elements or values, such as negative elements
     * @throws NotFoundElement If the element is not found in the periodic table.
     * @throws IncorrectAdduct If the adduct provided is invalid.
     */
    public Formula subtract(Formula other) throws IncorrectFormula, IncorrectAdduct, NotFoundElement {
        Map<Element.ElementType, Integer> newElements = new HashMap<>(this.elements);
        for (Map.Entry<Element.ElementType, Integer> entry : other.elements.entrySet()) {
            newElements.put(entry.getKey(), newElements.getOrDefault(entry.getKey(), 0) - entry.getValue());
        }
        for (Map.Entry<Element.ElementType, Integer> entry : newElements.entrySet()) {
            if (entry.getValue() < 0) {
                throw new IncorrectFormula("The subtraction of these two formulas contains a negative number of " + entry.getKey());
            }
        }
        int newCharge = this.chargeType.equals("-") ? -this.charge : this.charge;
        newCharge = other.chargeType.equals("-") ? newCharge + other.charge : newCharge - other.charge;
        String newChargeType = newCharge == 0 ? "" : (newCharge > 0 ? "+" : "-");
        return new Formula(newElements, this.adduct, Math.abs(newCharge), newChargeType);
    }

    /**
     * Multiplies a Formula by a number
     * @param numToMultiply number to multiply the formula by
     * @return ceu.biolab.Formula resulting from the multiplication operation with the number provided
     * @throws IncorrectFormula If the formula contains invalid elements or values
     * @throws NotFoundElement If the element is not found in the periodic table
     * @throws IncorrectAdduct If the adduct provided is invalid
     */
    public Formula multiply(int numToMultiply) throws IncorrectFormula, IncorrectAdduct, NotFoundElement {
        Map<Element.ElementType, Integer> newElements = new HashMap<>(this.elements);
        for (Map.Entry<Element.ElementType, Integer> entry : newElements.entrySet()) {
            newElements.put(entry.getKey(), entry.getValue() * numToMultiply);
        }
        return new Formula(newElements, this.adduct, this.charge, this.chargeType.getSymbol());
    }

    /**
     * Static method to create a ceu.biolab.Formula object from a chemical formula string in Hill notation.
     * @param formulaStr A string representing a molecular formula in Hill notation. Example: 'C4H5N6Na'. Other example 'C4H5N6Na+'
     * @param adduct A string representing an adduct in the form '[M+C2H2O-H]-', '[M-3H2O+2H]2+' or '[5M+Ca]2+' where the charge is specified at the end
     * @param metadata Optional argument to include a dict of metadata, defaults to None.
     * @return A new instance of the ceu.biolab.Formula class with the elements specified in the string
     * @throws IncorrectFormula If the number of appearances is &lt;=0 or if the formula contains elements that are not valid chemical elements
     * @throws NotFoundElement If the element is not found in the periodic table
     * @throws IncorrectAdduct If the adduct provided is invalid
     */
    public static Formula formulaFromStringHill(String formulaStr, String adduct, Map<String, Object> metadata) throws IncorrectFormula, NotFoundElement, IncorrectAdduct {
        if (!formulaStr.matches("^[\\[?a-zA-Z0-9\\]?]+(\\(?[+-]?\\d*\\)?)?$")) {
            throw new IncorrectFormula(formulaStr);
        }

        Pattern pattern = Pattern.compile("(\\[\\d+\\])?([A-Z][a-z]*)(\\d*)");
        Matcher matcher = pattern.matcher(formulaStr);
        Map<Element.ElementType, Integer> elements = new HashMap<>();

        while (matcher.find()) {
            String elementSymbol = matcher.group(2);  // Extract the element symbol as a String
            int appearances = matcher.group(3).isEmpty() ? 1 : Integer.parseInt(matcher.group(3));

            // Convert the element symbol (String) to an ElementType
            Element.ElementType elementType;
            try {
                elementType = Element.ElementType.valueOf(elementSymbol);  // Converts the string to an ElementType
            } catch (IllegalArgumentException e) {
                throw new NotFoundElement("ceu.biolab.Element " + elementSymbol + " not found");
            }

            // Put the ElementType in the map with its appearances
            elements.put(elementType, elements.getOrDefault(elementType, 0) + appearances);
        }

        // Adjusted pattern to handle charges
        Pattern chargePattern = Pattern.compile("\\(?([-+])(\\d*)\\)?$");
        Matcher chargeMatcher = chargePattern.matcher(formulaStr);
        int charge = 0;
        String chargeType = "";

        if (chargeMatcher.find()) {
            // Capture the charge type ('+' or '-')
            chargeType = chargeMatcher.group(1);
            // Capture the numeric part of the charge, if present
            String chargeValue = chargeMatcher.group(2);
            // If the numeric part is empty, set the charge to 1; otherwise, convert to an integer
            charge = chargeValue.isEmpty() ? 1 : Integer.parseInt(chargeValue);
        } else {
            // If no charge is found, set charge to 0 and chargeType to empty string
            charge = 0;
            chargeType = "";
        }


        return new Formula(elements, adduct, charge, chargeType, metadata);
    }

    /**
     * Calculates the monoisotopic mass of the formula
     * @return double value of the monoisotopic mass
     * @throws IncorrectFormula If the formula contains invalid elements or values.
     */
    private double calculateMonoisotopicMass() throws IncorrectFormula {
        double monoisotopicMass = 0.0;

        // Iterate over elements and their counts (appearances)
        for (Map.Entry<Element.ElementType, Integer> entry : elements.entrySet()) {
            Element.ElementType element = entry.getKey();
            int appearances = entry.getValue();
            monoisotopicMass += Element.elementWeights.get(element) * appearances;
        }

        // Adjust for charge type
        double electronsWeight = 0.0;
        switch (chargeType) {
            case POSITIVE:
                electronsWeight = -ELECTRON_WEIGHT * charge;
                break;
            case NEGATIVE:
                electronsWeight = ELECTRON_WEIGHT * charge;
                break;
            case NEUTRAL:
                electronsWeight = 0.0;
                break;
            default:
                throw new IncorrectFormula("The formula contains a wrong charge type");
        }

        monoisotopicMass += electronsWeight;

        // If charge is zero, divide by 1, otherwise divide by charge
        int adductChargeToDivide = (charge != 0) ? charge : 1;
        monoisotopicMass /= adductChargeToDivide;

        return monoisotopicMass;
    }

    /**
     * Calculates the monoisotopic mass of the formula taking into account the adduct
     * @return double value of the monoisotopic mass
     * @throws IncorrectFormula If the formula contains invalid elements or values
     * @throws NotFoundElement If the element is not found in the periodic table
     * @throws IncorrectAdduct If the adduct provided is invalid
     */
    private double calculateMonoisotopicMassWithAdduct() throws IncorrectFormula, IncorrectAdduct, NotFoundElement {
        double monoisotopicMassWithAdduct = this.getMonoisotopicMass();
        if (this.adduct == null) {
            return monoisotopicMassWithAdduct;
        }

        Adduct adductNew = new Adduct(this.adduct);

        // Calculate partial elements considering the adduct multiplier
        Map<Element.ElementType, Integer> partialElements = new EnumMap<>(Element.ElementType.class);
        for (Map.Entry<Element.ElementType, Integer> entry : this.elements.entrySet()) {
            Element.ElementType element = entry.getKey();
            int count = entry.getValue();
            partialElements.put(element, count * adductNew.getMultimer());
        }

        // Calculate partial charge
        int partialCharge = this.chargeType.equals("+") ? this.charge : -this.charge;
        ChargeType adductChargeType = adductNew.getAdductChargeType();
        int adductCharge = adductNew.getAdductCharge();
        int finalCharge;

        switch (adductChargeType) {
            case POSITIVE:
                finalCharge = partialCharge + adductCharge;
                break;
            case NEGATIVE:
                finalCharge = partialCharge - adductCharge;
                break;
            case NEUTRAL:
                finalCharge = partialCharge;
                break;
            default:
                throw new IncorrectFormula("The formula contains a wrong adduct");
        }

        // Update partial elements with adduct formula
        Formula formulaPlus = adductNew.getFormulaPlus();
        Formula formulaMinus = adductNew.getFormulaMinus();

        for (Map.Entry<Element.ElementType, Integer> entry : formulaPlus.getElements().entrySet()) {
            Element.ElementType element = entry.getKey();
            int appearances = entry.getValue();
            partialElements.merge(element, appearances, Integer::sum);
        }

        for (Map.Entry<Element.ElementType, Integer> entry : formulaMinus.getElements().entrySet()) {
            Element.ElementType element = entry.getKey();
            int appearances = entry.getValue();
            partialElements.merge(element, -appearances, Integer::sum);
            if (partialElements.get(element) < 0) {
                throw new IncorrectFormula("The formula contains a wrong adduct because the element " + element + " is negative " + partialElements.get(element));
            }
        }

        // Calculate monoisotopic mass with adduct
        monoisotopicMassWithAdduct = 0;
        for (Map.Entry<Element.ElementType, Integer> entry : partialElements.entrySet()) {
            Element.ElementType element = entry.getKey();
            int appearances = entry.getValue();
            monoisotopicMassWithAdduct += Element.elementWeights.get(element) * appearances;
        }

        // Adjust for charge
        double electronsWeight = (finalCharge != 0) ? -ELECTRON_WEIGHT * finalCharge : 0;
        monoisotopicMassWithAdduct += electronsWeight;
        int adductChargeToDivide = (finalCharge != 0) ? finalCharge : 1;
        monoisotopicMassWithAdduct /= Math.abs(adductChargeToDivide);

        return monoisotopicMassWithAdduct;
    }

    /**
     * Static method to create a ceu.biolab.Formula object from a chemical formula string.
     * @param formulaStr A string representing a molecular formula. Example: 'C4H5N6Na'
     * @param adduct A string representing an adduct in the form '[M+C2H2O-H]-', '[M-3H2O+2H]2+' or '[5M+Ca]2+' where the charge is specified at the end
     * @param noApi Disables api calls for formula resolution
     * @param metadata Optional argument to include a dict of metadata, defaults to None
     * @return A new instance of the ceu.biolab.Formula class with the elements specified in the string
     * @throws IncorrectFormula If the number of appearances is &lt;=0 or if the formula contains elements that are not valid chemical elements
     * @throws NotFoundElement If the element is not found in the periodic table
     * @throws IncorrectAdduct If the adduct provided is invalid
     */
    public static Formula formulaFromString(String formulaStr, String adduct, boolean noApi, Map<String, Object> metadata) throws IncorrectFormula, NotFoundElement, IncorrectAdduct {
        try {
            // Attempt to process the formula directly using formulaFromStringHill
            return formulaFromStringHill(formulaStr, adduct, metadata);
        } catch (Exception e) {
            // Print the error and continue
            System.out.println(e.getMessage());
        }

        if (!noApi) {
            // If noApi is false, attempt to resolve the formula via ChemCalc API
            try {
                String url = CC_URL + "?mf=" + formulaStr + "&isotopomers=jcamp,xy";
                HttpClient client = new HttpClient();
                GetMethod method = new GetMethod(url);

                int statusCode = client.executeMethod(method);
                if (statusCode != 200) {
                    throw new IncorrectFormula("The formula " + formulaStr + " was not parseable to a correct formula");
                }

                // Parse JSON response
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode data = objectMapper.readTree(method.getResponseBodyAsString());

                // Extract the molecular formula in Hill notation
                String mfHill = data.get("mf").asText();

                // Use the Hill notation formula to create the ceu.biolab.Formula object
                return formulaFromStringHill(mfHill, adduct, metadata);
            } catch (IOException e) {
                throw new IncorrectFormula("Error connecting to ChemCalc API: " + e.getMessage());
            }
        } else {
            // If noApi is true, return null as no API call is made
            return null;
        }
    }

    /**
     * Check if the monoisotopic mass of the formula is within a specified mass tolerance of an external mass.
     * @param externalMass The external monoisotopic mass to compare with the formula's mass
     * @param massToleranceInPpm The mass tolerance in parts per million (ppm) for the comparison
     * @return True if the external mass is within the specified tolerance of the formula's mass, otherwise False
     */
    public boolean checkMonoisotopicMass(double externalMass, double massToleranceInPpm) {
        double absValueDelta = ppmToAbsolute(getMonoisotopicMass(), massToleranceInPpm);
        return Math.abs(getMonoisotopicMass() - externalMass) <= absValueDelta;
    }

    /**
     * Check if the monoisotopic mass of the formula, considering the adduct, is within a specified mass tolerance of an external mass.
     * @param externalMass The external monoisotopic mass to compare with the formula's mass with the adduct
     * @param massToleranceInPpm The mass tolerance in parts per million (ppm) for the comparison
     * @return True if the external mass is within the specified tolerance of the formula's mass with the adduct, otherwise False
     */
    public boolean checkMonoisotopicMassWithAdduct(double externalMass, double massToleranceInPpm) {
        double absValueDelta = ppmToAbsolute(getMonoisotopicMassWithAdduct(), massToleranceInPpm);
        return Math.abs(getMonoisotopicMassWithAdduct() - externalMass) <= absValueDelta;
    }

    /**
     *
     * @param referenceMonoisotopicMass Monoisotopic mass of reference
     * @return The ppms between the monoisotopic mass of the formula taking into account the adduct and the experimental mass detected
     */
    public double ppmDifferenceWithExpMass(double referenceMonoisotopicMass) {
        return absoluteToPpm(getMonoisotopicMassWithAdduct(), referenceMonoisotopicMass);
    }

    /**
     *
     * @param referenceMonoisotopicMass Monoisotopic mass of reference
     * @param massToCompare Mass to compare
     * @return The ppms between the reference_monoisotopic_mass mass and mass_to_compare
     */
    public static double absoluteToPpm(double referenceMonoisotopicMass, double massToCompare) {
        return Math.abs((referenceMonoisotopicMass - massToCompare) / referenceMonoisotopicMass) * 1_000_000.0;
    }

    /**
     *
     * @param referenceMonoisotopicMass Monoisotopic mass of reference
     * @param ppm ppm of the reference monoisotopic mass
     * @return The absolute value of the ppm calculated
     */
    public static double ppmToAbsolute(double referenceMonoisotopicMass, double ppm) {
        return (referenceMonoisotopicMass / 1_000_000.0) * ppm;
    }

    /**
     * Static method to create a ceu.biolab.Formula object from a SMILES (Simplified Molecular Input Line Entry System) string.
     * @param smiles A string representing a molecular structure in SMILES notation. Example: CCCCCCC[C@@H](C/C=C/CCC(=O)NC/C(=C/Cl)/[C@@]12[C@@H](O1)[C@H](CCC2=O)O)OC
     * @return A new instance of the ceu.biolab.Formula class according to the molecular structure
     * @throws IncorrectFormula If the SMILES string does not represent a valid molecular structure
     */
    public static Formula formulaFromSMILES(String smiles) throws IncorrectFormula {
        try {
            // Create a SMILES parser
            SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());

            // Parse the SMILES string to get a molecule
            IAtomContainer molecule = smilesParser.parseSmiles(smiles);

            // Get the molecular formula
            String molecularFormula = MolecularFormulaManipulator.getString(
                    MolecularFormulaManipulator.getMolecularFormula(molecule)
            );

            // Calculate the total charge
            int totalCharge = 0;
            for (IAtom atom : molecule.atoms()) {
                Integer charge = atom.getFormalCharge();
                if (charge != null) {
                    totalCharge += charge;
                }
            }

            return formulaFromStringHill(molecularFormula, null, null);

        } catch (InvalidSmilesException | NotFoundElement | IncorrectAdduct e) {
            throw new IncorrectFormula("Invalid SMILES string: " + smiles);
        }
    }

    /**
     * Static method to create a ceu.biolab.Formula object from an InChI (International Chemical Identifier) string.
     * @param inchi A string representing a molecular structure in InChI notation. Example: InChI=1S/C45H73N5O10S3/c1-14-17-24(6)34(52)26(8)37-25(7)30(58-13)18-31-46-29(19-61-31)39-49-45(12,21-62-39)43-50-44(11,20-63-43)42(57)48-32(22(4)15-2)35(53)27(9)40(55)59-36(23(5)16-3)38(54)47-33(28(10)51)41(56)60-37/h19,22-28,30,32-37,51-53H,14-18,20-21H2,1-13H3,(H,47,54)(H,48,57)/t22-,23-,24+,25-,26-,27+,28+,30-,32-,33-,34-,35-,36-,37-,44+,45+/m0/s1
     * @return A new instance of the ceu.biolab.Formula class according to the molecular structure
     * @throws IncorrectFormula If the inchi string does not represent a valid molecular structure
     * @throws NotFoundElement If the element is not found in the periodic table
     */
    public static Formula formulaFromInChI(String inchi) throws IncorrectFormula, NotFoundElement {
        try {
            // Use CDK's InChI generator factory to parse the InChI string
            InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
            InChIToStructure inchiToStructure = factory.getInChIToStructure(inchi, SilentChemObjectBuilder.getInstance());

            // Check if the InChI conversion was successful
            if (inchiToStructure.getStatus() == InchiStatus.ERROR) {
                throw new IncorrectFormula("Error: Could not parse InChI string.");
            }
            else if (inchiToStructure.getStatus() != InchiStatus.WARNING) {
                // TODO LOG SOMEHOW TO SEE WHAT HAPPENS. STILL TO THINK ABOUT IT
                System.out.println("Warning generating the InChI");
            }

            // Get the molecule from the InChI string
            IAtomContainer molecule = inchiToStructure.getAtomContainer();

            // Get the molecular formula from the molecule
            IMolecularFormula molecularFormula = MolecularFormulaManipulator.getMolecularFormula(molecule);
            String formulaStr = MolecularFormulaManipulator.getString(molecularFormula);

            // Calculate the charge from the molecule
            //int totalCharge = molecule.getCharge();

            // Use your existing formulaFromStringHill to construct the ceu.biolab.Formula object
            return formulaFromStringHill(formulaStr, null, null);
        } catch (IncorrectAdduct e) {
            throw new RuntimeException(e);
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the map of chemical elements and their counts in the formula.
     * @return A map containing chemical elements as keys and their respective counts as values
     */
    public Map<Element.ElementType, Integer> getElements() {
        return elements;
    }

    /**
     * Get the adduct of the ceu.biolab.Formula
     * @return The adduct in String form
     */
    public String getAdduct() {
        return adduct;
    }

    /**
     * Get the charge
     * @return The charge
     */
    public int getCharge() {
        return charge;
    }

    /**
     * Get the ChargeType
     * @return The ChargeType
     */
    public ChargeType getChargeType() {
        return chargeType;
    }

    /**
     * Get the monoitopic mass
     * @return The monoisotopic mass
     */
    public double getMonoisotopicMass() {
        return monoisotopicMass;
    }

    /**
     * Get the monoisotopic mass taking into account the adduct
     * @return The monoisotopic mass taking into account the adduct
     */
    public double getMonoisotopicMassWithAdduct() {
        return monoisotopicMassWithAdduct;
    }

    /**
     * Get coppy the additional metadata info
     * @return A copy of the additional metadata info
     */
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
}
