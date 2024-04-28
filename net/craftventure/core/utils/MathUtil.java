package net.craftventure.core.utils;

import org.bukkit.util.Vector;


// Probably combined from several sources on the internet?

public class MathUtil {
    public static final float DEGTORAD = 0.017453293F;
    public static final float RADTODEG = 57.29577951F;
    public static final double RAD_180_DEG = Math.toRadians(180);


    public static double clampRadian(double input) {
        while (input > Math.PI * 2) input -= Math.PI * 2;
        while (input < 0) input += Math.PI * 2;
        return input;
    }

    public static double deltaRadian(double from, double to) {
        from = clampRadian(from);
        to = clampRadian(to);
        double difference = from - to;
        while (difference < -Math.PI) difference += (Math.PI * 2);
        while (difference > Math.PI) difference -= (Math.PI * 2);

        return difference;
    }

    public static Vector setYawPitchDegrees(Vector vector, double yaw, double pitch) {
        double xz = Math.cos(Math.toRadians(pitch));
        vector.setX(-xz * Math.sin(Math.toRadians(yaw)));
        vector.setY(-Math.sin(Math.toRadians(pitch)));
        vector.setZ(xz * Math.cos(Math.toRadians(yaw)));
        return vector;
    }

    public static Vector setYawPitchRadians(Vector vector, double yaw, double pitch) {
        double xz = Math.cos(pitch);
        vector.setX(-xz * Math.sin(yaw));
        vector.setY(-Math.sin(pitch));
        vector.setZ(xz * Math.cos(yaw));
        return vector;
    }

    public static Vector rotate(Vector vec, Vector axis, double theta) {
        double x, y, z;
        double u, v, w;
        x = vec.getX();
        y = vec.getY();
        z = vec.getZ();
        u = axis.getX();
        v = axis.getY();
        w = axis.getZ();
        double xPrime = u * (u * x + v * y + w * z) * (1d - Math.cos(theta))
                + x * Math.cos(theta)
                + (-w * y + v * z) * Math.sin(theta);
        double yPrime = v * (u * x + v * y + w * z) * (1d - Math.cos(theta))
                + y * Math.cos(theta)
                + (w * x - u * z) * Math.sin(theta);
        double zPrime = w * (u * x + v * y + w * z) * (1d - Math.cos(theta))
                + z * Math.cos(theta)
                + (-v * x + u * y) * Math.sin(theta);
        vec.setX(xPrime);
        vec.setY(yPrime);
        vec.setZ(zPrime);
        return vec;
//        return new Vector(xPrime, yPrime, zPrime);
    }

    public static double lengthSquared(double... values) {
        double rval = 0;
        for (int i = 0; i < values.length; i++) {
            double value = values[i];
            rval += value * value;
        }
        return rval;
    }

    public static double length(double... values) {
        return Math.sqrt(lengthSquared(values));
    }


    public static double getRelativeRadianAngleDifference(double angle1, double angle2) {
        return wrapRadianAngle(angle1 - angle2);
//        180 - abs(abs(a1 - a2) - 180)
//        return Math.PI - Math.abs(Math.abs(angle1 - angle2) - Math.PI);
    }

    public static double wrapRadianAngle(double angle) {
        double wrappedAngle = angle;
        while (wrappedAngle <= -Math.PI) {
            wrappedAngle += Math.PI * 2;
        }
        while (wrappedAngle > Math.PI) {
            wrappedAngle -= Math.PI * 2;
        }
        return wrappedAngle;
    }

    public static Vector rotate(float yaw, float pitch, double x, double y, double z) {
        // Conversions found by (a lot of) testing
        float angle;
        angle = yaw * DEGTORAD;
        double sinyaw = Math.sin(angle);
        double cosyaw = Math.cos(angle);

        angle = pitch * DEGTORAD;
        double sinpitch = Math.sin(angle);
        double cospitch = Math.cos(angle);

        Vector vector = new Vector();
        vector.setX((x * sinyaw) - (y * cosyaw * sinpitch) - (z * cosyaw * cospitch));
        vector.setY((y * cospitch) - (z * sinpitch));
        vector.setZ(-(x * cosyaw) - (y * sinyaw * sinpitch) - (z * sinyaw * cospitch));
        return vector;
    }
}