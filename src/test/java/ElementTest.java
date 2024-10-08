import ceu.biolab.Element;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ElementTest {

    @Test
    public void testElementWeightHydrogen() {
        // Verify Hydrogen weight
        assertEquals(1.0078250321, Element.elementWeights.get(Element.ElementType.H), 1e-9);
    }

    @Test
    public void testElementWeightCarbon() {
        // Verify Carbon weight
        assertEquals(12.0, Element.elementWeights.get(Element.ElementType.C), 1e-9);
    }

    @Test
    public void testElementWeightOxygen() {
        // Verify Oxygen weight
        assertEquals(15.994915, Element.elementWeights.get(Element.ElementType.O), 1e-9);
    }
}
