package net.craftventure.core.ride.trackedride.segment;

import net.craftventure.core.ride.trackedride.RideCar;
import net.craftventure.core.ride.trackedride.RideTrain;
import net.craftventure.core.ride.trackedride.TrackedRide;
import net.craftventure.core.utils.SimpleInterpolator;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;

/**
 * A basic blockbrake segment
 */
public class VerticalAutoLift extends SplinedTrackSegment {

    public interface LiftListener {
        void onUpdate(@Nonnull VerticalAutoLift segment, double offset);

        void onGoingUp(@Nonnull VerticalAutoLift segment, @Nonnull RideTrain rideTrain);

        void onGoingDown(@Nonnull VerticalAutoLift segment);

        void onStateChanged(@Nonnull VerticalAutoLift segment, @Nonnull LiftState newState);
    }

    private double offset = 0;
    private double startYOffset = 0;
    private double endYOffset = 0;
    private LiftState liftState = LiftState.IDLE;

    private final double transportSpeed;
    private final double maxSpeed;
    private final double brakeForce;
    private final double accelerateForce;
    private double transportSpeedLeaving;
    private double maxSpeedLeaving;
    private double brakeForceLeaving;
    private double accelerateForceLeaving;

    private final SimpleInterpolator simpleInterpolator;
    private long liftDuration = 20 * 8;
    private long waitBottomEnterDuration = 20 * 1;
    private long waitBottomExitDuration = 20 * 1;
    private long waitTopEnterDuration = 20 * 1;
    private long waitTopExitDuration = 20 * 1;
    private LiftListener liftListener;
    private long currentLiftingTicks = 0;
    private long currentWaitTopEnterDuration = 0;
    private long currentWaitTopExitDuration = 0;
    private long currentWaitBottomEnterDuration = 0;
    private long currentWaitBottomExitDuration = 0;

    private double triggerDistanceFromEnd = -1;

    public VerticalAutoLift(String id, TrackedRide trackedRide, double transportSpeed, double maxSpeed, double brakeForce, double accelerateForce, SimpleInterpolator simpleInterpolator) {
        this(id, id, trackedRide, transportSpeed, maxSpeed, brakeForce, accelerateForce, simpleInterpolator);
    }

    public VerticalAutoLift(String id, String displayName, TrackedRide trackedRide, double transportSpeed, double maxSpeed, double brakeForce, double accelerateForce, SimpleInterpolator simpleInterpolator) {
        super(id, displayName, trackedRide);
        this.transportSpeed = transportSpeed;
        this.maxSpeed = maxSpeed;
        this.brakeForce = brakeForce;
        this.accelerateForce = accelerateForce;
        this.transportSpeedLeaving = this.transportSpeed;
        this.maxSpeedLeaving = this.maxSpeed;
        this.brakeForceLeaving = this.brakeForce;
        this.accelerateForceLeaving = this.accelerateForce;
        assert simpleInterpolator != null;
        this.simpleInterpolator = simpleInterpolator;
    }

    public double getOffset() {
        return offset;
    }

    public double getStartYOffset() {
        return startYOffset;
    }

    public double getEndYOffset() {
        return endYOffset;
    }

    public void setLeavingSpeeds(double transportSpeed, double maxSpeed, double brakeForce, double accelerateForce) {
        this.transportSpeedLeaving = transportSpeed;
        this.maxSpeedLeaving = maxSpeed;
        this.brakeForceLeaving = brakeForce;
        this.accelerateForceLeaving = accelerateForce;
    }

    public void setTriggerDistanceFromEnd(double triggerDistanceFromEnd) {
        this.triggerDistanceFromEnd = triggerDistanceFromEnd;
    }

    public void setLiftDuration(long liftDuration) {
        this.liftDuration = liftDuration;
    }

    public void setWaitBottomEnterDuration(long waitBottomEnterDuration) {
        this.waitBottomEnterDuration = waitBottomEnterDuration;
    }

    public void setWaitBottomExitDuration(long waitBottomExitDuration) {
        this.waitBottomExitDuration = waitBottomExitDuration;
    }

    public void setWaitTopEnterDuration(long waitTopEnterDuration) {
        this.waitTopEnterDuration = waitTopEnterDuration;
    }

    public void setWaitTopExitDuration(long waitTopExitDuration) {
        this.waitTopExitDuration = waitTopExitDuration;
    }

    public void setLiftListener(LiftListener liftListener) {
        this.liftListener = liftListener;
    }

    @Override
    public void initialize() {
        super.initialize();
        Vector positionOther = new Vector(0, 0, 0);
        Vector positionSelf = new Vector(0, 0, 0);

        getPreviousTrackSegment().getPosition(getPreviousTrackSegment().getLength() - 0.001, positionOther);
        getPosition(0.001, positionSelf);
        startYOffset = positionOther.getY() - positionSelf.getY();

        getNextTrackSegment().getPosition(0.001, positionOther);
        getPosition(getLength() - 0.001, positionSelf);
        endYOffset = positionOther.getY() - positionSelf.getY();

//        Logger.console("startYOffset = " + startYOffset);
//        Logger.console("endYOffset = " + endYOffset);
    }

    public void setState(LiftState state) {
        if (this.liftState != state) {
            this.liftState = state;
            if (liftListener != null)
                liftListener.onStateChanged(this, state);
        }
    }

    @Override
    public boolean isBlockSection() {
        return true;
    }

    @Override
    protected boolean setBlockReservedTrain(RideTrain blockReservedTrain) {
        if (liftState != LiftState.IDLE)
            return false;
        return super.setBlockReservedTrain(blockReservedTrain);
    }

    @Override
    public void update() {
        super.update();
        boolean containsTrain = isContainsTrainCached();
        if (containsTrain && liftState == LiftState.IDLE) {
            setState(LiftState.ENTERING);
            currentLiftingTicks = 0;
            currentWaitTopEnterDuration = 0;
            currentWaitTopExitDuration = 0;
            currentWaitBottomEnterDuration = 0;
            currentWaitBottomExitDuration = 0;
        } else if (!containsTrain && liftState == LiftState.LEAVING) {
            if (liftListener != null)
                liftListener.onGoingDown(this);
            setState(LiftState.DOWNING);
            currentLiftingTicks = 0;
        }
        if (liftState == LiftState.LIFTING) {
            if (currentWaitBottomExitDuration < waitBottomExitDuration) {
                currentWaitBottomExitDuration++;
            } else {
                currentLiftingTicks++;
                offset = simpleInterpolator.interpolate(Math.min(currentLiftingTicks, liftDuration), (float) startYOffset, (float) endYOffset, liftDuration);
                if (currentLiftingTicks > liftDuration) {
                    offset = endYOffset;
                    if (currentWaitTopEnterDuration < waitTopEnterDuration) {
                        currentWaitTopEnterDuration++;
                    } else if (canAdvanceToNextBlock(getBlockReservedTrain(), true)) {
                        currentLiftingTicks = 0;
                        setState(LiftState.LEAVING);
                    }
                }
            }
        }
        if (liftState == LiftState.DOWNING) {
            if (getBlockReservedTrain() == null) {
                if (currentWaitTopExitDuration < waitTopExitDuration) {
                    currentWaitTopExitDuration++;
                } else {
                    currentLiftingTicks++;
                    offset = simpleInterpolator.interpolate(Math.max(0, liftDuration - currentLiftingTicks), (float) startYOffset, (float) endYOffset, liftDuration);
                    if (currentLiftingTicks > liftDuration) {
                        offset = startYOffset;
                        if (currentWaitBottomEnterDuration < waitBottomEnterDuration) {
                            currentWaitBottomEnterDuration++;
                        } else {
                            currentLiftingTicks = 0;
                            setState(LiftState.IDLE);
                        }
                    }
                }
            }
        }

        if (liftListener != null) {
            liftListener.onUpdate(this, offset);
        }
//        Logger.info("Lift offset = " + offset);
    }

    @Override
    public void applyForces(RideCar car, double distanceSinceLastUpdate) {
        super.applyForces(car, distanceSinceLastUpdate);
        if (liftState == LiftState.ENTERING) {
            if (maxSpeed != 0 && car.getVelocity() + car.getAcceleration() < maxSpeed) {
                car.setAcceleration(Math.min(accelerateForce, Math.max(maxSpeed - car.getVelocity(), 0)));
            }

            if (maxSpeed != 0 && car.getVelocity() + car.getAcceleration() > maxSpeed) {
                if (car.getVelocity() > maxSpeed) {
                    car.setAcceleration(-Math.min(brakeForce, car.getVelocity() - maxSpeed));
                } else {
                    car.setAcceleration(Math.min(brakeForce, maxSpeed - car.getVelocity()));
                }
            }
        } else if (liftState == LiftState.LEAVING) {
            if (maxSpeedLeaving != 0 && car.getVelocity() + car.getAcceleration() < maxSpeedLeaving) {
                car.setAcceleration(Math.min(accelerateForceLeaving, Math.max(maxSpeedLeaving - car.getVelocity(), 0)));
            }

            if (maxSpeedLeaving != 0 && car.getVelocity() + car.getAcceleration() > maxSpeedLeaving) {
                if (car.getVelocity() > maxSpeedLeaving) {
                    car.setAcceleration(-Math.min(brakeForceLeaving, car.getVelocity() - maxSpeedLeaving));
                } else {
                    car.setAcceleration(Math.min(brakeForceLeaving, maxSpeedLeaving - car.getVelocity()));
                }
            }
        }
        if (liftState == LiftState.ENTERING) {
            double triggerDistance = triggerDistanceFromEnd > 0 ? getLength() - triggerDistanceFromEnd : getLength() - ((getLength() - car.attachedTrain.getLength()) / 2.0);
//            Logger.console(triggerDistance + " vs " + getLength() + " vs " + car.getAttachedTrain().getFrontCarDistance());
            if (car.attachedTrain.getFrontCarDistance() > triggerDistance) {
//                Logger.console("Commencing lift");
                car.setVelocity(0);
                car.setAcceleration(0);
                if (liftListener != null)
                    liftListener.onGoingUp(this, car.attachedTrain);
                setState(LiftState.LIFTING);
            }
        } else if (liftState == LiftState.LIFTING) {
            car.setVelocity(0);
            car.setAcceleration(0);
        }
    }

    @Override
    public void applyForceCheck(RideCar car, double currentDistance, double previousDistance) {
        super.applyForceCheck(car, currentDistance, previousDistance);
        double triggerDistance = triggerDistanceFromEnd > 0 ? getLength() - triggerDistanceFromEnd : length * 0.5;
        if (currentDistance > triggerDistance && liftState == LiftState.ENTERING) {
            if (liftListener != null)
                liftListener.onGoingUp(this, car.attachedTrain);
            setState(LiftState.LIFTING);
            car.setVelocity(0);
            car.setAcceleration(0);
        }
    }

    @Override
    public void getPosition(double distance, Vector position, boolean applyInterceptors) {
        super.getPosition(distance, position, applyInterceptors);
        position.setY(position.getY() + offset);
    }

    public enum LiftState {
        IDLE, ENTERING, LIFTING, LEAVING, DOWNING
    }
}
