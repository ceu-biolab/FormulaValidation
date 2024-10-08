import ceu.biolab.Adduct;
import ceu.biolab.ChargeType;
import ceu.biolab.IncorrectAdduct;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AdductTest {

    @Test
    public void testValidAdduct() throws Exception {
        Adduct adduct = new Adduct("[M+H]+");
        assertEquals(1, adduct.getMultimer());
        assertEquals(1, adduct.getAdductCharge());
        assertEquals(ChargeType.POSITIVE, adduct.getAdductChargeType());
    }

    @Test
    public void testValidAdductWithMultipleCharges() throws Exception {
        Adduct adduct = new Adduct("[M+Na]2+");
        assertEquals(1, adduct.getMultimer());
        assertEquals(2, adduct.getAdductCharge());
        assertEquals(ChargeType.POSITIVE, adduct.getAdductChargeType());
    }

    @Test
    public void testAdductWithMultimer() throws Exception {
        Adduct adduct = new Adduct("[5M+H]+");
        assertEquals(5, adduct.getMultimer());
        assertEquals(1, adduct.getAdductCharge());
    }

    @Test
    public void testIncorrectAdduct() {
        assertThrows(IncorrectAdduct.class, () -> {
            new Adduct("[3]");
        });
    }

    @Test
    public void testAdductMassCalculation() throws Exception {
        Adduct adduct = new Adduct("[M+H-H2O]+");
        double mass = adduct.getAdductMass();
        System.out.println(mass);
        System.out.println(adduct.getFormulaPlus());
        System.out.println(adduct.getFormulaMinus());
        //H mass is 1.01 aprox
        assertTrue(mass < -17 && mass > -17.01);
    }
}