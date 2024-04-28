package net.craftventure.core.ride.trackedride.car.effect;

import net.craftventure.core.ride.trackedride.RideCar;

public abstract class Effect {
    private final double rightOffset;
    private final double upOffset;
    private final double forwardOffset;

    public Effect(double rightOffset, double upOffset, double forwardOffset) {
        this.rightOffset = rightOffset;
        this.upOffset = upOffset;
        this.forwardOffset = forwardOffset;
    }

    public double getRightOffset() {
        return rightOffset;
    }

    public double getUpOffset() {
        return upOffset;
    }

    public double getForwardOffset() {
        return forwardOffset;
    }

    public abstract void move(double x, double y, double z, double trackYawRadian, double trackPitchRadian, double bankingDegree, RideCar rideCar);
}
