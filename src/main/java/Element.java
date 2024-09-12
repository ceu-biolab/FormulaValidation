import java.util.EnumMap;
import java.util.Map;

public class Element {

    // Enum for element types
    public enum ElementType {
        H, H2, D, He, Li, Li6, Be, B, C, C13, C14, N, N15, O, O18, F, Ne, Na, Mg, Al, Si, P, S, Cl, Cl37, Ar, K, Ca, Sc,
        Ti, V, Cr, Mn, Fe, Ni, Co, Cu, Zn, Ga, Ge, As, Se, Br, Kr, Rb, Sr, Y, Zr, Nb, Mo, Tc, Ru, Rh, Pd, Ag, Cd, In, Sn,
        Sb, Te, I, Xe, Cs, Ba, La, Ce, Pr, Nd, Pm, Sm, Eu, Gd, Tb, Dy, Ho, Er, Tm, Yb, Lu, Hf, Ta, W, Re, Os, Ir, Pt,
        Au, Hg, Tl, Pb, Bi, Po, At, Rn, Fr, Ra, Ac, Th, Pa, U, Np, Pu, Am, Cm, Bk, Cf, Es, Fm, Md, No, Lr, Rf, Db, Sg,
        Bh, Hs, Mt, Ds, Rg, Cn, Uut, Nh, Fl, Uup, Mc, Lv, Uus, Ts, Uuo
    }

    // Mapping from ElementType to their corresponding weights
    public static final Map<ElementType, Double> elementWeights = new EnumMap<>(ElementType.class);

    static {
        elementWeights.put(ElementType.H, 1.0078250321);
        elementWeights.put(ElementType.H2, 2.01410177811);
        elementWeights.put(ElementType.D, 2.01410177811);
        elementWeights.put(ElementType.He, 4.0026032542);
        elementWeights.put(ElementType.Li, 7.016004558);
        elementWeights.put(ElementType.Li6, 6.0151223);
        elementWeights.put(ElementType.Be, 9.012182);
        elementWeights.put(ElementType.B, 11.009305);
        elementWeights.put(ElementType.C, 12.0);
        elementWeights.put(ElementType.C13, 13.00335484);
        elementWeights.put(ElementType.C14, 14.00307401);
        elementWeights.put(ElementType.N, 14.003074);
        elementWeights.put(ElementType.N15, 15.00010897);
        elementWeights.put(ElementType.O, 15.994915);
        elementWeights.put(ElementType.O18, 17.9991604);
        elementWeights.put(ElementType.F, 18.998403163);
        elementWeights.put(ElementType.Ne, 19.99244);
        elementWeights.put(ElementType.Na, 22.98976928);
        elementWeights.put(ElementType.Mg, 23.98504);
        elementWeights.put(ElementType.Al, 26.9815385);
        elementWeights.put(ElementType.Si, 27.97693);
        elementWeights.put(ElementType.P, 30.973761632);
        elementWeights.put(ElementType.S, 31.9720710015);
        elementWeights.put(ElementType.Cl, 34.96885);
        elementWeights.put(ElementType.Cl37, 36.9659026);
        elementWeights.put(ElementType.Ar, 39.962383);
        elementWeights.put(ElementType.K, 38.96371);
        elementWeights.put(ElementType.Ca, 39.96259);
        elementWeights.put(ElementType.Sc, 44.95591);
        elementWeights.put(ElementType.Ti, 47.94794);
        elementWeights.put(ElementType.V, 50.94396);
        elementWeights.put(ElementType.Cr, 51.94051);
        elementWeights.put(ElementType.Mn, 54.93804);
        elementWeights.put(ElementType.Fe, 55.93494);
        elementWeights.put(ElementType.Co, 58.93319);
        elementWeights.put(ElementType.Ni, 57.93534);
        elementWeights.put(ElementType.Cu, 62.92960);
        elementWeights.put(ElementType.Zn, 63.92914);
        elementWeights.put(ElementType.Ga, 68.92557);
        elementWeights.put(ElementType.Ge, 73.92118);
        elementWeights.put(ElementType.As, 74.92159);
        elementWeights.put(ElementType.Se, 79.91652);
        elementWeights.put(ElementType.Br, 78.91834);
        elementWeights.put(ElementType.Kr, 83.91150);
        elementWeights.put(ElementType.Rb, 84.91179);
        elementWeights.put(ElementType.Sr, 87.90561);
        elementWeights.put(ElementType.Y, 88.90584);
        elementWeights.put(ElementType.Zr, 89.90470);
        elementWeights.put(ElementType.Nb, 92.90637);
        elementWeights.put(ElementType.Mo, 97.90540);
        elementWeights.put(ElementType.Tc, 98.0);
        elementWeights.put(ElementType.Ru, 101.90434);
        elementWeights.put(ElementType.Rh, 102.90550);
        elementWeights.put(ElementType.Pd, 105.90348);
        elementWeights.put(ElementType.Ag, 106.90509);
        elementWeights.put(ElementType.Cd, 113.90337);
        elementWeights.put(ElementType.In, 114.90388);
        elementWeights.put(ElementType.Sn, 119.90220);
        elementWeights.put(ElementType.Sb, 120.90381);
        elementWeights.put(ElementType.Te, 129.90622);
        elementWeights.put(ElementType.I, 126.90447);
        elementWeights.put(ElementType.Xe, 131.90416);
        elementWeights.put(ElementType.Cs, 132.90545);
        elementWeights.put(ElementType.Ba, 137.90525);
        elementWeights.put(ElementType.La, 138.90636);
        elementWeights.put(ElementType.Ce, 139.90544);
        elementWeights.put(ElementType.Pr, 140.90766);
        elementWeights.put(ElementType.Nd, 141.90773);
        elementWeights.put(ElementType.Pm, 145.0);
        elementWeights.put(ElementType.Sm, 151.91974);
        elementWeights.put(ElementType.Eu, 152.92124);
        elementWeights.put(ElementType.Gd, 157.92411);
        elementWeights.put(ElementType.Tb, 158.92535);
        elementWeights.put(ElementType.Dy, 163.92918);
        elementWeights.put(ElementType.Ho, 164.93033);
        elementWeights.put(ElementType.Er, 165.93030);
        elementWeights.put(ElementType.Tm, 168.93422);
        elementWeights.put(ElementType.Yb, 173.93887);
        elementWeights.put(ElementType.Lu, 174.94078);
        elementWeights.put(ElementType.Hf, 179.94656);
        elementWeights.put(ElementType.Ta, 180.94800);
        elementWeights.put(ElementType.W, 183.95093);
        elementWeights.put(ElementType.Re, 186.95575);
        elementWeights.put(ElementType.Os, 191.96148);
        elementWeights.put(ElementType.Ir, 192.96292);
        elementWeights.put(ElementType.Pt, 194.96479);
        elementWeights.put(ElementType.Au, 196.96657);
        elementWeights.put(ElementType.Hg, 201.97064);
        elementWeights.put(ElementType.Tl, 204.97443);
        elementWeights.put(ElementType.Pb, 207.97665);
        elementWeights.put(ElementType.Bi, 208.98040);
        elementWeights.put(ElementType.Po, 209.98287);
        elementWeights.put(ElementType.At, 210.0);
        elementWeights.put(ElementType.Rn, 222.0);
        elementWeights.put(ElementType.Fr, 223.0);
        elementWeights.put(ElementType.Ra, 226.0);
        elementWeights.put(ElementType.Ac, 227.0);
        elementWeights.put(ElementType.Th, 232.03806);
        elementWeights.put(ElementType.Pa, 231.03588);
        elementWeights.put(ElementType.U, 238.05079);
        elementWeights.put(ElementType.Np, 237.0);
        elementWeights.put(ElementType.Pu, 244.0);
        elementWeights.put(ElementType.Am, 243.0);
        elementWeights.put(ElementType.Cm, 247.0);
        elementWeights.put(ElementType.Bk, 247.0);
        elementWeights.put(ElementType.Cf, 251.0);
        elementWeights.put(ElementType.Es, 252.0);
        elementWeights.put(ElementType.Fm, 257.0);
        elementWeights.put(ElementType.Md, 258.0);
        elementWeights.put(ElementType.No, 259.0);
        elementWeights.put(ElementType.Lr, 266.0);
        elementWeights.put(ElementType.Rf, 267.0);
        elementWeights.put(ElementType.Db, 268.0);
        elementWeights.put(ElementType.Sg, 269.0);
        elementWeights.put(ElementType.Bh, 270.0);
        elementWeights.put(ElementType.Hs, 269.0);
        elementWeights.put(ElementType.Mt, 278.0);
        elementWeights.put(ElementType.Ds, 281.0);
        elementWeights.put(ElementType.Rg, 282.0);
        elementWeights.put(ElementType.Cn, 285.0);
        elementWeights.put(ElementType.Uut, 286.0);
        elementWeights.put(ElementType.Fl, 289.0);
        elementWeights.put(ElementType.Uup, 289.0);
        elementWeights.put(ElementType.Lv, 293.0);
        elementWeights.put(ElementType.Uus, 294.0);
        elementWeights.put(ElementType.Uuo, 294.0);
    }
}
