import enumerations.ChargeType;
import exceptions.IncorrectAdduct;
import exceptions.IncorrectFormula;
import exceptions.NotFoundElement;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

public class Formula {
    private static final double ELECTRON_WEIGHT = 0.00054858;
    private static final String CC_URL = "https://www.chemcalc.org/chemcalc/mf";
    private static final int DEFAULT_PPM = 50;

    private Map<Element.ElementType,Integer> elements;
    private String adduct;
    private int charge;
    private ChargeType chargeType;
    private double monoisotopicMass;
    private double monoisotopicMassWithAdduct;
    private Map<String, Object> metadata;

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

    public Formula() {
        this.elements = new HashMap<>();
        this.adduct = null;
        this.charge = 0;
        this.chargeType = ChargeType.fromSymbol("");
        this.monoisotopicMass = 0.0;
        this.monoisotopicMassWithAdduct = 0.0;
        this.metadata = new HashMap<>();
    }

    public boolean equals(Object other) {
        if (other instanceof Formula) {
            Formula otherFormula = (Formula) other;
            return this.elements.equals(otherFormula.getElements()) &&
                    (this.adduct == null ? otherFormula.adduct == null : this.adduct.equals(otherFormula.adduct));
        }
        return false;
    }

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

    public int hashCode() {
        return this.elements.hashCode();
    }

    public Formula add(Formula other) throws IncorrectFormula, IncorrectAdduct, NotFoundElement {
        Map<Element.ElementType, Integer> newElements = new HashMap<>(this.elements);
        for (Map.Entry<Element.ElementType, Integer> entry : other.elements.entrySet()) {
            newElements.put(entry.getKey(), newElements.getOrDefault(entry.getKey(), 0) + entry.getValue());
        }
        int newCharge = this.chargeType.equals("-") ? -this.charge : this.charge;
        newCharge = other.chargeType.equals("-") ? newCharge - other.charge : newCharge + other.charge;
        String newChargeType = newCharge == 0 ? "" : (newCharge > 0 ? "+" : "-");
        //TODO keep og adduct
        return new Formula(newElements, null, Math.abs(newCharge), newChargeType);
    }

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
        return new Formula(newElements, null, Math.abs(newCharge), newChargeType);
    }

    public Formula multiply(int numToMultiply) throws IncorrectFormula, IncorrectAdduct, NotFoundElement {
        Map<Element.ElementType, Integer> newElements = new HashMap<>(this.elements);
        for (Map.Entry<Element.ElementType, Integer> entry : newElements.entrySet()) {
            newElements.put(entry.getKey(), entry.getValue() * numToMultiply);
        }
        return new Formula(newElements, this.adduct, this.charge, this.chargeType.getSymbol());
    }

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
                throw new NotFoundElement("Element " + elementSymbol + " not found");
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

                // Use the Hill notation formula to create the Formula object
                return formulaFromStringHill(mfHill, adduct, metadata);
            } catch (IOException e) {
                throw new IncorrectFormula("Error connecting to ChemCalc API: " + e.getMessage());
            }
        } else {
            // If noApi is true, return null as no API call is made
            return null;
        }
    }

    public boolean checkMonoisotopicMass(double externalMass, double massToleranceInPpm) {
        double absValueDelta = ppmToAbsolute(getMonoisotopicMass(), massToleranceInPpm);
        return Math.abs(getMonoisotopicMass() - externalMass) <= absValueDelta;
    }

    public boolean checkMonoisotopicMassWithAdduct(double externalMass, double massToleranceInPpm) {
        double absValueDelta = ppmToAbsolute(getMonoisotopicMassWithAdduct(), massToleranceInPpm);
        return Math.abs(getMonoisotopicMassWithAdduct() - externalMass) <= absValueDelta;
    }

    public double ppmDifferenceWithExpMass(double referenceMonoisotopicMass) {
        return absoluteToPpm(getMonoisotopicMassWithAdduct(), referenceMonoisotopicMass);
    }

    public static double absoluteToPpm(double referenceMonoisotopicMass, double massToCompare) {
        return Math.abs((referenceMonoisotopicMass - massToCompare) / referenceMonoisotopicMass) * 1_000_000.0;
    }

    public static double ppmToAbsolute(double referenceMonoisotopicMass, double ppm) {
        return (referenceMonoisotopicMass / 1_000_000.0) * ppm;
    }

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

    public static Formula formulaFromInChI(String inchi) throws IncorrectFormula, NotFoundElement {
        try {
            // Use CDK's InChI generator factory to parse the InChI string
            InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
            InChIToStructure inchiToStructure = factory.getInChIToStructure(inchi, SilentChemObjectBuilder.getInstance());

            // Check if the InChI conversion was successful
            if (inchiToStructure.getReturnStatus() != INCHI_RET.OKAY) {
                throw new IncorrectFormula("Error: Could not parse InChI string.");
            }

            // Get the molecule from the InChI string
            IAtomContainer molecule = inchiToStructure.getAtomContainer();

            // Get the molecular formula from the molecule
            IMolecularFormula molecularFormula = MolecularFormulaManipulator.getMolecularFormula(molecule);
            String formulaStr = MolecularFormulaManipulator.getString(molecularFormula);

            // Calculate the charge from the molecule
            //int totalCharge = molecule.getCharge();

            // Use your existing formulaFromStringHill to construct the Formula object
            return formulaFromStringHill(formulaStr, null, null);
        } catch (IncorrectAdduct e) {
            throw new RuntimeException(e);
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<Element.ElementType, Integer> getElements() {
        return elements;
    }

    public String getAdduct() {
        return adduct;
    }

    public int getCharge() {
        return charge;
    }

    public ChargeType getChargeType() {
        return chargeType;
    }

    public double getMonoisotopicMass() {
        return monoisotopicMass;
    }

    public double getMonoisotopicMassWithAdduct() {
        return monoisotopicMassWithAdduct;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
