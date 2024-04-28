package net.craftventure.core.ride.trackedride;

import net.craftventure.core.utils.VectorUtils;
import org.bukkit.util.Vector;


public class SplineHelper {
    private static final Vector[] terms = new Vector[4];

    private static final Vector p1Cache = new Vector();
    private static final Vector p2Cache = new Vector();
    private static final Vector p3Cache = new Vector();
    private static final Vector p4Cache = new Vector();

    public static Vector getValue(double t, Vector p1, Vector p2, Vector p3, Vector p4) {
        if (t > 1.0 || t < 0.0) {
            throw new IllegalArgumentException("The value of t is out of range: " + t + " .");
        }
        double one_minus_t = 1 - t;
        Vector retValue = new Vector();
        Vector[] terms = new Vector[4];
        terms[0] = calcNewVector(one_minus_t * one_minus_t * one_minus_t, VectorUtils.copy(p1, p1Cache));
        terms[1] = calcNewVector(3 * one_minus_t * one_minus_t * t, VectorUtils.copy(p2, p2Cache));
        terms[2] = calcNewVector(3 * one_minus_t * t * t, VectorUtils.copy(p3, p3Cache));
        terms[3] = calcNewVector(t * t * t, VectorUtils.copy(p4, p4Cache));
        for (int i = 0; i < 4; i++) {
            retValue.add(terms[i]);
        }
        return retValue;
    }

    private static Vector calcNewVector(double scaler, Vector base) {
//        Vector retValue = base.clone();
//        retValue.multiply(scaler);
//        return retValue;
        return base.multiply(scaler);
    }

    public static void getValue(double t, Vector p1, Vector p2, Vector p3, Vector p4, Vector output) {
        output.setX(0);
        output.setY(0);
        output.setZ(0);
        if (t > 1.0001 || t < 0.0) {
            throw new IllegalArgumentException("The value of t is out of range: " + t + " .");
        }
        double one_minus_t = 1 - t;
        terms[0] = calcNewVector(one_minus_t * one_minus_t * one_minus_t, VectorUtils.copy(p1, p1Cache));
        terms[1] = calcNewVector(3 * one_minus_t * one_minus_t * t, VectorUtils.copy(p2, p2Cache));
        terms[2] = calcNewVector(3 * one_minus_t * t * t, VectorUtils.copy(p3, p3Cache));
        terms[3] = calcNewVector(t * t * t, VectorUtils.copy(p4, p4Cache));
        for (int i = 0; i < 4; i++) {
            output.add(terms[i]);
        }
    }
}
