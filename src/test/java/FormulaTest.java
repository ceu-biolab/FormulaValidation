import exceptions.IncorrectAdduct;
import exceptions.IncorrectFormula;
import exceptions.NotFoundElement;
import org.junit.jupiter.api.Test;

import java.text.Normalizer;

import static org.junit.jupiter.api.Assertions.*;
public class FormulaTest {

    @Test
    public void testAddFormulas() throws IncorrectFormula, IncorrectAdduct, NotFoundElement {
        Formula formula1 = Formula.formulaFromStringHill("H2O-1", "None", null);
        Formula formula2 = Formula.formulaFromStringHill("CH4", "None", null);
        Formula result = formula1.add(formula2);

        System.out.println("formula1: "+formula1);
        System.out.println("with adduct: "+formula1.getFinalFormulaWithAdduct());
        System.out.println();
        System.out.println("formula2: "+formula2);
        System.out.println("with adduct: "+formula2.getFinalFormulaWithAdduct());
        System.out.println();
        System.out.println("result:"+result); //results from add operation
        System.out.println("adduct of result: "+result.getAdduct());
        System.out.println("with adduct: "+result.getFinalFormulaWithAdduct());
        System.out.println();

        Formula expected = Formula.formulaFromStringHill("CH6O", "None", null);
        System.out.println("expected:"+expected);
        System.out.println("adduct of expected: "+expected.getAdduct());
        System.out.println("with adduct: "+expected.getFinalFormulaWithAdduct());
        assertEquals(expected, result);
    }

    @Test
    public void testParse() throws IncorrectAdduct, NotFoundElement, IncorrectFormula {
        Formula formula1 = Formula.formulaFromStringHill("H20", "[M+H]+", null);

        Formula expected = Formula.formulaFromString("H20", "[M+H]+", Boolean.FALSE, null);
        assertEquals(expected, formula1);
    }

    @Test
    public void testMultiplyFormula() throws Exception {
        Formula formula = Formula.formulaFromStringHill("H2O", "[M+H]+", null);
        Formula result = formula.multiply(2);

        assertEquals(Formula.formulaFromStringHill("H4O2", "[M+H]+", null), result);
    }

    @Test
    public void testMonoisotopicMass() throws Exception {
        Formula formula = Formula.formulaFromStringHill("H2O", "[M+H]+", null);
        double mass = formula.getMonoisotopicMassWithAdduct();

        System.out.println(mass);
        assertEquals(19.0178415163, mass, 1e-6);
    }

    @Test
    public void testFormulaFromSMILES() throws Exception {
        String smiles = "O";
        Formula result = Formula.formulaFromSMILES(smiles);
        Formula expected = Formula.formulaFromStringHill("H2O", null, null);

        assertEquals(expected, result);
    }

    @Test
    public void testFormulaFromInChi() throws Exception {
        String inchi = "InChI=1S/H2O/h1H2";
        Formula result = Formula.formulaFromInChI(inchi);
        Formula expected = Formula.formulaFromStringHill("H2O", null, null);

        assertEquals(expected, result);
    }

    @Test
    public void testCheckMonoisotopicMass() throws IncorrectFormula, IncorrectAdduct, NotFoundElement {
        Formula formula = Formula.formulaFromStringHill("H2O", "None", null);
        double externalMass = 18.010565;  // Expected monoisotopic mass for H2O
        double massToleranceInPpm = 10.0;  // 10 ppm tolerance

        assertTrue(formula.checkMonoisotopicMass(externalMass, massToleranceInPpm),
                "Expected the monoisotopic mass to be within tolerance.");

        externalMass = 19.0;  // Outside the tolerance
        assertFalse(formula.checkMonoisotopicMass(externalMass, massToleranceInPpm),
                "Expected the monoisotopic mass to be outside the tolerance.");
    }

    @Test
    public void testCheckMonoisotopicMassWithAdduct() throws IncorrectFormula, IncorrectAdduct, NotFoundElement {
        Formula formula = Formula.formulaFromStringHill("H2O", "[M+H]+", null);
        double externalMass = 19.01776;  // Expected monoisotopic mass with adduct [M+H]+ for H2O
        double massToleranceInPpm = 10.0;  // 10 ppm tolerance


        assertTrue(formula.checkMonoisotopicMassWithAdduct(externalMass, massToleranceInPpm),
                "Expected the monoisotopic mass with adduct to be within tolerance.");

        externalMass = 20.0;  // Outside the tolerance
        assertFalse(formula.checkMonoisotopicMassWithAdduct(externalMass, massToleranceInPpm),
                "Expected the monoisotopic mass with adduct to be outside the tolerance.");
    }


    @Test
    public void testPpmDifferenceWithExpMass() throws IncorrectFormula, IncorrectAdduct, NotFoundElement {
        Formula formula = Formula.formulaFromStringHill("H2O", null, null);
        double referenceMass = 18.0110;  // Expected reference monoisotopic mass for H2O

        double expectedPpmDifference = 24;  // PPM difference calculation
        assertEquals(expectedPpmDifference, Math.round(formula.ppmDifferenceWithExpMass(referenceMass)), 1e-4,
                "PPM difference calculation failed.");
    }

    @Test
    public void testAbsoluteToPpm() {
        double referenceMass = 18.01056;  // Monoisotopic mass of H2O
        double massToCompare = 18.0110;

        double expectedPpm = 24;  // Expected PPM difference
        assertEquals(expectedPpm, Math.round(Formula.absoluteToPpm(referenceMass, massToCompare)), 1e-4,
                "Absolute to PPM conversion failed.");
    }

    @Test
    public void testPpmToAbsolute() {
        double referenceMass = 18.010565;  // Monoisotopic mass of H2O
        double ppm = 50.0;

        double expectedAbsValue = 0.0009005;  // Expected absolute value for 50 ppm
        assertEquals(expectedAbsValue, Formula.ppmToAbsolute(referenceMass, ppm), 1e-6,
                "PPM to absolute conversion failed.");
    }
}
