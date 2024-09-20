import exceptions.IncorrectAdduct;
import exceptions.IncorrectFormula;
import exceptions.NotFoundElement;
import org.junit.jupiter.api.Test;

import java.text.Normalizer;

import static org.junit.jupiter.api.Assertions.*;
public class FormulaTest {

    @Test
    public void testAddFormulas() throws IncorrectFormula, IncorrectAdduct, NotFoundElement {
        Formula formula1 = Formula.formulaFromStringHill("H2O", "None", null);
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
        Formula formula = Formula.formulaFromStringHill("H2O", null, null);
        double mass = formula.getMonoisotopicMass();

        // Peso esperado: 1.007825 * 2 + 15.994915 (H2O)
        assertEquals(18.010565, mass, 1e-6);
    }
}
