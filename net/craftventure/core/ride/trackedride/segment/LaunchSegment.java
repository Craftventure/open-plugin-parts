package net.craftventure.core.ride.trackedride.segment;

import net.craftventure.core.ktx.util.Logger;
import net.craftventure.core.ride.trackedride.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.craftventure.core.ride.trackedride.segment.LaunchSegment.LaunchState.*;

/**
 * A basic transport segment that has a minimum speed and a brake speed. The transport speed can be set to 0 to disable the effect, so can the brake speed.
 * The brake force can be used to set the strength of the applied brake effect if used
 * <p/>
 * Can also be used as a trimbrake
 */
public class LaunchSegment extends SplinedTrackSegment {

    private LaunchState state = LaunchState.IDLE;
    private final double transportSpeed;
    private final double maxSpeed;
    private final double accelerateForce;
    private final double brakeForce;
    private final double launchTransportSpeed;
    private final double launchMaxSpeed;
    private final double launchAccelerateForce;
    private final double launchBrakeForce;
    private final int stationaryTicks;
    private final double frontCarStationaryPercentage;
    private final double failedLaunchRetryBoostSpeed = CoasterMathUtils.kmhToBpt(-25);
    private final double failedLaunchTransportSpeed = CoasterMathUtils.kmhToBpt(8);
    private final double failedLaunchBrakeForce = CoasterMathUtils.kmhToBpt(0.9);
    private final double failedLaunchAccelerateMultiplier = 1.1;

    private boolean failedLaunchBrakeActivated = false;
    private boolean executeFailedLaunchBoost = false;
    private int failedLaunchBrakeActivatedTicks = 0;

    private int currentStationaryTicks = 0;
    private OnLaunchStateChangedListener onLaunchStateChangedListener;

    public LaunchSegment(String id, TrackedRide trackedRide, double transportSpeed, double accelerateForce, double maxSpeed, double brakeForce,
                         double launchTransportSpeed, double launchAccelerateForce, double launchMaxSpeed, double launchBrakeForce,
                         int stationaryTicks, double frontCarStationaryPercentage, boolean isBlockSection) {
        this(id, id, trackedRide, transportSpeed, accelerateForce, maxSpeed, brakeForce,
                launchTransportSpeed, launchAccelerateForce, launchMaxSpeed, launchBrakeForce,
                stationaryTicks, frontCarStationaryPercentage, isBlockSection);
    }

    public LaunchSegment(String id, String displayName, TrackedRide trackedRide, double transportSpeed, double accelerateForce, double maxSpeed, double brakeForce,
                         double launchTransportSpeed, double launchAccelerateForce, double launchMaxSpeed, double launchBrakeForce,
                         int stationaryTicks, double frontCarStationaryPercentage, boolean isBlockSection) {
        super(id, displayName, trackedRide);
        this.transportSpeed = transportSpeed;
        this.maxSpeed = maxSpeed;
        this.brakeForce = brakeForce;
        this.accelerateForce = accelerateForce;

        this.launchTransportSpeed = launchTransportSpeed;
        this.launchMaxSpeed = launchMaxSpeed;
        this.launchBrakeForce = launchBrakeForce;
        this.launchAccelerateForce = launchAccelerateForce;
        this.frontCarStationaryPercentage = frontCarStationaryPercentage;

        this.isBlockSection = isBlockSection;
        this.stationaryTicks = stationaryTicks;
    }

    @Override
    protected void clearBlockReservedTrain() {
        if (state != FAILED_LAUNCH_RECOVER)
            super.clearBlockReservedTrain();
    }

    @Override
    public boolean canAdvanceToNextBlock(@NotNull RideTrain rideTrain, boolean reserveNextBlockIfRequired) {
        if (rideTrain == getBlockReservedTrain() && state == FAILED_LAUNCH_RECOVER && !reserveNextBlockIfRequired)
            return true;
        return super.canAdvanceToNextBlock(rideTrain, reserveNextBlockIfRequired);
    }

    public void setOnLaunchStateChangedListener(OnLaunchStateChangedListener onLaunchStateChangedListener) {
        this.onLaunchStateChangedListener = onLaunchStateChangedListener;
    }

    private void setState(LaunchState state) {
        if (this.state == state)
            return;
        if (state == FAILED_LAUNCH_RECOVER) {
            setDisableHaltCheck(true);
            Logger.warn("ERROR: A launch has failed at " + getTrackedRide().getName() + " @" + getId());
        } else {
            failedLaunchBrakeActivatedTicks = 0;
            failedLaunchBrakeActivated = false;
            executeFailedLaunchBoost = false;
            setDisableHaltCheck(false);
        }
//        Logger.console("LaunchState changed from " + this.state.name() + " to " + state.name() + " for " + getTrackedRide().getName() + " @" + getId());
        if (onLaunchStateChangedListener != null) {
            onLaunchStateChangedListener.onLaunchstateChanged(state, this.state);
        }
        this.state = state;
    }

    @Override
    public void applyForces(RideCar car, double distanceSinceLastUpdate) {
        super.applyForces(car, distanceSinceLastUpdate);

        if (state == LaunchState.STATIONARY) {
            car.setVelocity(0);
            car.setAcceleration(0);
        } else if (state == LaunchState.ENTERING) {
            if (transportSpeed != 0 && car.getVelocity() + car.getAcceleration() < transportSpeed) {
                car.setAcceleration(Math.min(accelerateForce, Math.max(transportSpeed - car.getVelocity(), 0)));
            }
            if (maxSpeed != 0 && car.getVelocity() + car.getAcceleration() > maxSpeed) {
                if (car.getVelocity() > maxSpeed) {
                    car.setAcceleration(-Math.min(brakeForce, car.getVelocity() - maxSpeed));
                } else {
                    car.setAcceleration(Math.min(brakeForce, maxSpeed - car.getVelocity()));
                }
            }

            if (car.attachedTrain.getFrontCarDistance() > getLength() * frontCarStationaryPercentage) {
                if (stationaryTicks == 0) {
                    if (isBlockSection() && !canAdvanceToNextBlock(getBlockReservedTrain(), true)) {
                        setState(FAILED_LAUNCH_RECOVER);
                    } else {
                        setState(LAUNCHING);
                    }
                } else {
                    car.setVelocity(0);
                    car.setAcceleration(0);
                    setState(LaunchState.STATIONARY);
                    currentStationaryTicks = stationaryTicks;
                }
            }
        } else if (state == LaunchState.LAUNCHING) {
            if (launchTransportSpeed != 0 && car.getVelocity() < launchTransportSpeed) {
                car.setAcceleration(launchAccelerateForce * failedLaunchAccelerateMultiplier);
            }
            if (launchMaxSpeed != 0 && car.getVelocity() > launchMaxSpeed) {
                car.setAcceleration(-launchBrakeForce);
            }
        } else if (state == FAILED_LAUNCH_RECOVER) {
            if (executeFailedLaunchBoost) {
                RideTrain rideTrain = car.attachedTrain;
                if (rideTrain.getVelocity() == 0) {
                    List<RideCar> cars = rideTrain.getCars();
                    for (int i = 0; i < cars.size(); i++) {
                        RideCar rideCar = cars.get(i);
                        rideCar.setVelocity(failedLaunchRetryBoostSpeed);
                    }
                }
                if (rideTrain.getVelocity() > 0) {
                    setState(LAUNCHING);
                }
            } else {
                double targetSpeed = failedLaunchTransportSpeed;
                if (targetSpeed != 0 && car.getVelocity() + car.getAcceleration() < targetSpeed) {
                    car.setAcceleration(Math.min(accelerateForce, Math.max(targetSpeed - car.getVelocity(), 0)));
                }

                if (failedLaunchTransportSpeed != 0 && car.getVelocity() + car.getAcceleration() > failedLaunchTransportSpeed) {
                    if (car.getVelocity() > failedLaunchTransportSpeed) {
                        car.setAcceleration(-Math.min(failedLaunchBrakeForce, car.getVelocity() - failedLaunchTransportSpeed));
                    } else {
                        car.setAcceleration(Math.min(failedLaunchBrakeForce, failedLaunchTransportSpeed - car.getVelocity()));
                    }
                }

                RideTrain rideTrain = car.attachedTrain;
                double trainLength = rideTrain.getLength();
                boolean shouldGoForward = rideTrain.getFrontCarDistance() < trainLength;

                if (car.getVelocity() < failedLaunchTransportSpeed && car.getVelocity() > -failedLaunchTransportSpeed) {
                    if (car.attachedTrain.getFrontCarTrackSegment() == getNextTrackSegment())
                        shouldGoForward = false;
                    if (shouldGoForward) {
                        if (car.getVelocity() < failedLaunchTransportSpeed) {
                            car.setAcceleration(Math.min(failedLaunchBrakeForce, car.getVelocity() - failedLaunchTransportSpeed));
                        }
                    } else {
                        if (car.getVelocity() > -failedLaunchTransportSpeed) {
                            car.setAcceleration(-Math.min(failedLaunchBrakeForce, failedLaunchTransportSpeed - car.getVelocity()));
                        }
                    }
                }
                if (!failedLaunchBrakeActivated) {
                    if (rideTrain.getFrontCarDistance() < trainLength && rideTrain.getFrontCarDistance() + rideTrain.getVelocity() + rideTrain.getAcceleration() > trainLength) {
                        if (car.getVelocity() < failedLaunchTransportSpeed) {
                            failedLaunchBrakeActivated = true;
                        }
                    } else if (rideTrain.getFrontCarDistance() > trainLength && rideTrain.getFrontCarDistance() + rideTrain.getVelocity() + rideTrain.getAcceleration() < trainLength) {
                        if (car.getVelocity() > -failedLaunchTransportSpeed) {
                            failedLaunchBrakeActivated = true;
                        }
                    }
                    if (failedLaunchBrakeActivated) {
                        Logger.warn("ERROR: A train got stalled at " + getTrackedRide().getName() + " @" + getId());
                    }
                }
                if (failedLaunchBrakeActivated) {
                    car.setVelocity(0);
                    car.setAcceleration(0);
                }
            }
        }
    }

    @Override
    public void applyForceCheck(RideCar car, double currentDistance, double previousDistance) {
        super.applyForceCheck(car, currentDistance, previousDistance);
        if (state == STATIONARY) {
            List<RideCar> cars = car.attachedTrain.getCars();
            for (int i = 0; i < cars.size(); i++) {
                RideCar rideCar = cars.get(i);
                rideCar.setVelocity(0);
                rideCar.setAcceleration(0);
            }
        }
    }

    @Override
    public void update() {
        super.update();
        boolean containsTrain = isContainsTrainCached();
        if (containsTrain && state == LaunchState.IDLE) {
            setState(LaunchState.ENTERING);
        }
        if (!containsTrain && state != LaunchState.IDLE & state != FAILED_LAUNCH_RECOVER) {
            setState(LaunchState.IDLE);
        }
        if (state == LaunchState.STATIONARY /*&& currentStationaryTicks > 0*/) {
            currentStationaryTicks--;
            if (currentStationaryTicks <= 0 && (!isBlockSection() || canAdvanceToNextBlock(getBlockReservedTrain(), true))) {
                setState(LaunchState.LAUNCHING);
            }
        }
        if (failedLaunchBrakeActivated)
            failedLaunchBrakeActivatedTicks++;
        if (state == FAILED_LAUNCH_RECOVER && getBlockReservedTrain() != null) {
            RideTrain rideTrain = getBlockReservedTrain();
            if (rideTrain != null) {
                if (rideTrain.getAcceleration() == 0 && rideTrain.getVelocity() == 0) {
                    if (canAdvanceToNextBlock(rideTrain, true) && failedLaunchBrakeActivatedTicks > 20 * 4) {
                        executeFailedLaunchBoost = true;
                        failedLaunchBrakeActivated = false;
                        failedLaunchBrakeActivatedTicks = 0;
                    }
                }
                if (executeFailedLaunchBoost) {
                    if (rideTrain.getVelocity() > 0) {
                        setState(LAUNCHING);
                    }
                }
            }
        }
    }

    @Override
    public LaunchSegmentJson toJson() {
        return toJson(new LaunchSegmentJson());
    }

    @NotNull
    @Override
    public <T extends TrackSegmentJson> T toJson(T source1) {
        LaunchSegmentJson source = (LaunchSegmentJson) source1;
        source.setTransportSpeed(transportSpeed);
        source.setMaxSpeed(maxSpeed);
        source.setAccelerateForce(accelerateForce);
        source.setBrakeForce(brakeForce);
        source.setLaunchTransportSpeed(launchTransportSpeed);
        source.setLaunchMaxSpeed(launchMaxSpeed);
        source.setLaunchAccelerateForce(launchAccelerateForce);
        source.setLaunchBrakeForce(launchBrakeForce);
        source.setStationaryTicks(stationaryTicks);
        source.setFrontCarStationaryPercentage(frontCarStationaryPercentage);
        return super.toJson(source1);
    }

    @NotNull
    @Override
    public <T extends TrackSegmentJson> void restore(T source1) {
        LaunchSegmentJson source = (LaunchSegmentJson) source1;
        super.restore(source1);
    }

    public enum LaunchState {
        IDLE, ENTERING, STATIONARY, LAUNCHING, FAILED_LAUNCH_RECOVER
    }

    public interface OnLaunchStateChangedListener {
        void onLaunchstateChanged(LaunchState newState, LaunchState oldState);
    }
}
