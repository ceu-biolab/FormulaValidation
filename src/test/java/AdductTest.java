import exceptions.IncorrectAdduct;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AdductTest {

    @Test
    public void testValidAdduct() throws Exception {
        Adduct adduct = new Adduct("[M+H]+");
        assertEquals(1, adduct.getMultimer());
        assertEquals(1, adduct.getAdductCharge());
        assertEquals("+", adduct.getAdductChargeType());
    }

    @Test
    public void testValidAdductWithMultipleCharges() throws Exception {
        Adduct adduct = new Adduct("[M+Na]2+");
        assertEquals(1, adduct.getMultimer());
        assertEquals(2, adduct.getAdductCharge());
        assertEquals("+", adduct.getAdductChargeType());
    }

    @Test
    public void testAdductWithMultimer() throws Exception {
        Adduct adduct = new Adduct("[5M+H]+");
        assertEquals(5, adduct.getMultimer());
        assertEquals(1, adduct.getAdductCharge());
    }

    @Test
    //TODO check this
    public void testIncorrectAdduct() {
        assertThrows(IncorrectAdduct.class, () -> {
            new Adduct("[M+++]");
        });
    }

    @Test
    public void testAdductMassCalculation() throws Exception {
        Adduct adduct = new Adduct("[M+H]+");
        double mass = adduct.getAdductMass();
        //H mass is 1.03 aprox
        assertTrue(mass > 1);
    }
}