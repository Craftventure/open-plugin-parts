package net.craftventure.core.ride.trackedride;

import net.craftventure.bukkit.ktx.util.EntityConstants;
import net.craftventure.chat.bungee.util.CvMiniMessageKt;
import net.craftventure.core.CraftventureCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public abstract class TrackSegment {
    protected double friction = 0.9985;
    protected double gravitationalInfluence = 0.05;
    @NotNull
    private final TrackedRide trackedRide;
    protected boolean isBlockSection = false;
    protected double length = -1;
    private double stopDistance = 0;
    private double startDistance = 0; // Distance of space before a stopped car starts riding again
    @NotNull
    private BlockType blockType = BlockType.BLOCK_SECTION;
    @Nullable
    private TrackSegment nextTrackSegment;
    @Nullable
    private TrackSegment previousTrackSegment;
    private double offsetFromNextSection = 0;
    private boolean containsTrain = false;
    private ContainsListener containsListener;
    @NotNull
    private final Set<DistanceListener> distanceListeners = new HashSet<>();
    @NotNull
    private final String id;
    @NotNull
    private final Component displayName;
    private boolean disableHaltCheck = false;
    @NotNull
    private LeaveMode leaveMode = LeaveMode.LEAVE_TO_EXIT;
    @NotNull
    private EjectType ejectType = EjectType.EJECT_TO_EXIT;
    private boolean shouldAutomaticallyReroutePreviousSegment;
    private boolean isStoppingTrainFromLeaving = false;
    private boolean isHaltingTrainThisUpdate = false;
    @NotNull
    private final Set<SectionHaltListener> sectionHaltListeners = new HashSet<>();
    @NotNull
    private final Set<UpdateListener> updateListeners = new HashSet<>();
    @NotNull
    private final Set<OnSectionLeaveListener> sectionLeaveListeners = new HashSet<>();
    @NotNull
    private final Set<OnSectionEnterListener> sectionEnterListeners = new HashSet<>();
    private boolean blockSegmentAdvancing = false;
    //    private int spacingTicks = -1;
//    private int currentSpacingTicks = 0;
    @Nullable
    private RideTrain blockReservedTrain;
    @NotNull
    private TrackType trackType = TrackType.DEFAULT;

    @Nullable
    protected BankingInterceptor bankingInterceptor;
    @Nullable
    protected PositionInterceptor positionInterceptor;
    @Nullable
    protected OnExitLocationProvider onExitLocationProvider;
    @Nullable
    private String nameOnMap;
    private boolean active = true;
    @Nullable
    private ArmorStand debugTag = null;
    @Nullable
    private String debugTagString = null;
    private final Set<PlayerEnterListener> playerEnterListeners = new HashSet<>();
    private final Set<PlayerExitListener> playerExitListeners = new HashSet<>();
    private boolean forceContinuousCheck = false;
    protected List<Runnable> initializers = new LinkedList<>();

    public TrackSegment(@NotNull String id, @NotNull String displayName, @NotNull TrackedRide trackedRide) {
        this.id = id;
        this.displayName = CvMiniMessageKt.parseWithCvMessage(displayName);
        this.trackedRide = trackedRide;
    }

    public boolean isForceContinuousCheck() {
        return forceContinuousCheck;
    }

    public void setForceContinuousCheck(boolean forceContinuousCheck) {
        this.forceContinuousCheck = forceContinuousCheck;
    }

    public boolean addPlayerEnterListener(PlayerEnterListener playerEnterListener) {
        return playerEnterListeners.add(playerEnterListener);
    }

    public boolean removePlayerEnterListener(PlayerEnterListener playerEnterListener) {
        return playerEnterListeners.remove(playerEnterListener);
    }

    public boolean addPlayerExitListener(PlayerExitListener playerEnterListener) {
        return playerExitListeners.add(playerEnterListener);
    }

    public boolean removePlayerExitListener(PlayerExitListener playerEnterListener) {
        return playerExitListeners.remove(playerEnterListener);
    }

    public void setDebugTag(@Nullable String tag) {
        this.debugTagString = tag;
    }

    public boolean addUpdateListener(UpdateListener listener) {
        return updateListeners.add(listener);
    }

    public boolean removeUpdateListener(UpdateListener listener) {
        return updateListeners.remove(listener);
    }

    public String getNameOnMap() {
        if (nameOnMap != null) return nameOnMap;
        return id;
    }

    public void setNameOnMap(@Nullable String nameOnMap) {
        this.nameOnMap = nameOnMap;
    }

    public void setOnExitLocationProvider(@org.jetbrains.annotations.Nullable OnExitLocationProvider onExitLocationProvider) {
        this.onExitLocationProvider = onExitLocationProvider;
    }

    public void setFriction(double friction) {
        this.friction = friction;
    }

    public void setGravitationalInfluence(double gravitationalInfluence) {
        this.gravitationalInfluence = gravitationalInfluence;
    }

    public boolean isSectionUnreservedOrTrainFullyOnSegment() {
        TrackSegment endSegment = getSectionEndSegment();
        if (endSegment == null) return false;
        RideTrain train = endSegment.blockReservedTrain;
        if (train == null) return true;
        return train.getFrontCarTrackSegment() == endSegment && train.frontCarDistance > train.getLength();
    }

    public boolean canBeSetActive() {
        return isSectionUnreservedOrTrainFullyOnSegment();
//        return nextTrackSegment.isSectionUnreservedOrSelfReserved(this);
    }

    public boolean canBeSetInactive() {
        return isSectionUnreservedOrTrainFullyOnSegment();
//        return nextTrackSegment.isSectionUnreservedOrSelfReserved(this);
    }

    public boolean isActive() {
        return active;
    }

    public boolean setActive(boolean active, boolean force) {
        if (force || (active ? canBeSetInactive() : canBeSetActive())) {
            this.active = active;
            return true;
        }
        return false;
    }

    public BankingInterceptor getBankingInterceptor() {
        return bankingInterceptor;
    }

    public TrackSegment setBankingInterceptor(BankingInterceptor bankingInterceptor) {
        this.bankingInterceptor = bankingInterceptor;
        return this;
    }

    public PositionInterceptor getPositionInterceptor() {
        return positionInterceptor;
    }

    public TrackSegment setPositionInterceptor(PositionInterceptor positionInterceptor) {
        this.positionInterceptor = positionInterceptor;
        return this;
    }

    public TrackType getTrackType() {
        return trackType;
    }

    public TrackSegment setTrackType(TrackType trackType) {
        this.trackType = trackType;
        return this;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public boolean isBlockSegmentAdvancing() {
        return blockSegmentAdvancing;
    }

    public void setBlockSegmentAdvancing(boolean blockSegmentAdvancing) {
        this.blockSegmentAdvancing = blockSegmentAdvancing;
    }

    public void addOnSectionLeaveListener(OnSectionLeaveListener onSectionLeaveListener) {
        if (onSectionLeaveListener != null) {
            sectionLeaveListeners.add(onSectionLeaveListener);
        }
    }

    public void removeOnSectionEnterListener(OnSectionEnterListener onSectionEnterListener) {
        sectionEnterListeners.remove(onSectionEnterListener);
    }

    public void addOnSectionEnterListener(OnSectionEnterListener onSectionEnterListener) {
        if (onSectionEnterListener != null) {
            sectionEnterListeners.add(onSectionEnterListener);
        }
    }

    public void removeOnSectionLeaveListener(OnSectionLeaveListener onSectionLeaveListener) {
        sectionLeaveListeners.remove(onSectionLeaveListener);
    }

    public void onEmergencyStopActivated(boolean eStopActivated) {

    }

    public boolean shouldAutomaticallyReroutePreviousSegment() {
        return shouldAutomaticallyReroutePreviousSegment;
    }

    public void setShouldAutomaticallyReroutePreviousSegment(boolean shouldAutomaticallyReroutePreviousSegment) {
        this.shouldAutomaticallyReroutePreviousSegment = shouldAutomaticallyReroutePreviousSegment;
    }

    @NotNull
    public EjectType getEjectType() {
        return ejectType;
    }

    public void setEjectType(@NotNull EjectType ejectType) {
        this.ejectType = ejectType;
    }

    @NotNull
    public LeaveMode getLeaveMode() {
        return leaveMode;
    }

    public void setLeaveMode(@NotNull LeaveMode leaveMode) {
        this.leaveMode = leaveMode;
    }

    public Location getSeatEjectLocation(Player player) {
        return player.getLocation().clone().add(CraftventureCore.getRandom().nextBoolean() ? 0.5 : -0.5, 0, CraftventureCore.getRandom().nextBoolean() ? 0.5 : -0.5);
    }

    public Location getLeaveLocation(Player player, RideCar rideCar, LeaveType leaveType) {
        Location location = onExitLocationProvider != null ? onExitLocationProvider.onProvideExitLocation(leaveType) : null;
        if (location != null) {
            return location;
        }
//        Logger.info("Getting leave location for %s with type %s and velocity %s (lm=%s et=%s)", false,
//                player.getName(), leaveType.name(), rideCar.getAttachedTrain().getVelocity(), leaveMode, ejectType);
        double trainVelocity = rideCar.attachedTrain.getVelocity();
        if (ejectType == EjectType.EJECT_TO_SEAT && leaveType == LeaveType.EJECT) {
            return getSeatEjectLocation(player);
        }
        if (ejectType == EjectType.EJECT_TO_EXIT && leaveType == LeaveType.EJECT) {
            return getTrackedRide().getExitLocation();
        }
        if (leaveMode == LeaveMode.LEAVE_TO_EXIT && leaveType == LeaveType.LEFT) {
            return getTrackedRide().getExitLocation();
        }
        if (leaveMode == LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER && rideCar.attachedTrain.canEnter() && leaveType == LeaveType.LEFT) {
            return getSeatEjectLocation(player);
        }
        if (leaveMode == LeaveMode.LEAVE_TO_SEAT_WHEN_HALTED && leaveType == LeaveType.LEFT && trainVelocity == 0) {
            return getSeatEjectLocation(player);
        }
        if (leaveMode == LeaveMode.LEAVE_TO_SEAT) {
            return getSeatEjectLocation(player);
        }
//        else if (leaveMode == LeaveMode.STATION_EJECT_TO_EXIT) {
//            if (leaveType != LeaveType.EJECT) {
//                return getSeatEjectLocation(player);
//            }
//        }
        return getTrackedRide().getExitLocation();
    }

    public void setDisableHaltCheck(boolean disableHaltCheck) {
        this.disableHaltCheck = disableHaltCheck;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public TrackedRide getTrackedRide() {
        return trackedRide;
    }

    public TrackSegment setOffsetFromNextSection(double offsetFromNextSection) {
        this.offsetFromNextSection = offsetFromNextSection;
        return this;
    }

//    public void setSpacingTicks(int spacingTicks) {
//        if (this.spacingTicks == -1)
//            this.currentSpacingTicks = spacingTicks;
//        this.spacingTicks = spacingTicks;
//    }

    public boolean add(DistanceListener distanceListener) {
        return distanceListeners.add(distanceListener);
    }

    public boolean remove(DistanceListener distanceListener) {
        return distanceListeners.remove(distanceListener);
    }

    public boolean add(SectionHaltListener sectionHaltListener) {
        return sectionHaltListeners.add(sectionHaltListener);
    }

    public boolean remove(SectionHaltListener sectionHaltListener) {
        return sectionHaltListeners.remove(sectionHaltListener);
    }

    public void setContainsListener(ContainsListener containsListener) {
        this.containsListener = containsListener;
    }

    public TrackSegment blockSection(boolean blockSection) {
        isBlockSection = blockSection;
        return this;
    }

    public void setBlockType(BlockType blockType) {
        setBlockType(blockType, 0.0, 0.0);
    }

    public void setBlockType(BlockType blockType, double stopDistance, double startDistance) {
        this.blockType = blockType;
        this.stopDistance = stopDistance;
        this.startDistance = startDistance;
    }

    public void onPlayerEnteredCarOnSegment(RideCar rideCar, Player player) {
//        Logger.debug("Player entered " + getClass().getSimpleName() + " > " + getId());
        playerEnterListeners.forEach((listener) -> {
            listener.onPlayerEnteredCarOnSegment(rideCar, player);
        });
    }

    public void onPlayerExitedCarOnSegment(RideCar rideCar, Player player) {
//        Logger.debug("Player exited " + getClass().getSimpleName() + " > " + getId());
        playerExitListeners.forEach((listener) -> {
            listener.onPlayerExitedCarOnSegment(rideCar, player);
        });
    }

    public double transformPitch(double pitch) {
        return pitch;
    }

    public double transformYaw(double yaw) {
        return yaw;
    }

    public boolean isBlockSection() {
        return isBlockSection;
    }

//    public boolean isBlocking() {
//        if (blockType == BlockType.CONTINUOUS) {
//            for (RideTrain rideTrain : trackedRide.rideTrains) {
//                if (rideTrain.getLastCarTrackSegment() == this || rideTrain.getFrontCarTrackSegment() == this) {
//                    if (rideTrain.getFrontCarDistance() - rideTrain.getLength() < 0)
//                        return true;
//                }
//            }
//            return false;
//        } else
//            return containsTrain;
//    }

    public void clearBlockReservedTrain(RideTrain rideTrain) {
        if (blockReservedTrain == rideTrain) {
            blockReservedTrain = null;
        }
    }

    protected void clearBlockReservedTrain() {
        blockReservedTrain = null;
    }

    /**
     * Tries to set the block reserved train
     *
     * @return {@code true} if this section is reserved for this train, {@code false} otherwise
     */
    protected boolean setBlockReservedTrain(RideTrain blockReservedTrain) {
        if (blockReservedTrain == null) {
            clearBlockReservedTrain();
            return false;
        }
        if (isBlockSection && getBlockType() == BlockType.BLOCK_SECTION && getTrackedRide().isEmergencyStopActive()) {
            return false;
        }
        if (!isBlockSection())
            throw new IllegalStateException("Tried to set the block-reserved-train of a non-blocksection segment");
//        if (getTrackedRide().isEmergencyStopActive())
//            return false;
        this.blockReservedTrain = blockReservedTrain;
//        Logger.console("BlockReserved train set to " + blockReservedTrain.toString() + " for " + getId());
        return true;
    }

    @NotNull
    public BlockType getBlockType() {
        return blockType;
    }

    @Nullable
    public RideTrain getBlockReservedTrain() {
        return blockReservedTrain;
    }

    public boolean isBlocked() {
        return blockReservedTrain != null && isBlockSection;
    }

    @Nullable
    public TrackSegment getSectionEndSegment() {
        return nextTrackSegment.getSectionEndSegment(this);
    }

    @Nullable
    public TrackSegment getSectionEndSegment(TrackSegment sourceSegment) {
        // should return true if a loop is detected to prevent ride stalling
        if (sourceSegment == this)
            return this;

        if (isBlockSection()) {
            return this;
        } else {
            if (getNextTrackSegment() != null) {
                return getNextTrackSegment().getSectionEndSegment(sourceSegment);
            } else {
                return null;
            }
        }
    }

    /**
     * Checks if the next block is reserved for this train
     *
     * @return {@code true} if the next blocksection is reserved for the given RideTrain, {@code false} otherwise
     */
    public boolean isSectionReservedForTrain(TrackSegment sourceSegment, RideTrain rideTrain) {
        // should return true if a loop is detected to prevent ride stalling
        if (sourceSegment == this)
            return true;

        if (isBlockSection()) {
            return blockReservedTrain == rideTrain;
        } else {
            if (getNextTrackSegment() != null) {
                return getNextTrackSegment().isSectionReservedForTrain(sourceSegment, rideTrain);
            }
            return false;
//            return getNextTrackSegment().reserveBlockForTrain(sourceSegment, this, rideTrain);
        }
    }

    public boolean isSectionUnreserved(TrackSegment sourceSegment) {
        // should return true if a loop is detected to prevent ride stalling
        if (sourceSegment == this)
            return true;

        if (isBlockSection()) {
            return blockReservedTrain == null;
        } else {
            if (getNextTrackSegment() != null) {
                return getNextTrackSegment().isSectionUnreserved(sourceSegment);
            }
            return false;
        }
    }

//    public boolean isSectionUnreservedOrSelfReserved(TrackSegment sourceSegment) {
//        // should return true if a loop is detected to prevent ride stalling
//        if (sourceSegment == this)
//            return true;
//
//        if (isBlockSection()) {
//            return blockReservedTrain == null || blockReservedTrain == getAnyRideTrainOnSegment();
//        } else {
//            if (getNextTrackSegment() != null) {
//                return getNextTrackSegment().isSectionUnreservedOrSelfReserved(sourceSegment);
//            }
//            return false;
//        }
//    }

    /**
     * Tries to reserve the next blocksection for this train. Should usually be called on the next/previous track segment
     *
     * @return {@code true} if the next blocksection is reserved for the given RideTrain, {@code false} otherwise
     */
    public boolean reserveBlockForTrain(TrackSegment sourceSegment, TrackSegment previousSegment, RideTrain rideTrain) {
        if (!isActive()) return false;
        // should return true if a loop is detected to prevent ride stalling
        if (sourceSegment == this)
            return true;

        if (isBlockSection()) {
            if (blockReservedTrain == null) {
                if (setBlockReservedTrain(rideTrain)) {
                    if (shouldAutomaticallyReroutePreviousSegment)
                        setPreviousTrackSegment(previousSegment);
                    return true;
                }
                return false;
            }
            return blockReservedTrain == rideTrain;
        } else {
            if (getNextTrackSegment() != null && getNextTrackSegment().reserveBlockForTrain(sourceSegment, this, rideTrain)) {
                if (shouldAutomaticallyReroutePreviousSegment)
                    setPreviousTrackSegment(previousSegment);
                return true;
            }
            return false;
        }
    }

//    public boolean canAdvanceToNextBlock(@Nonnull RideTrain rideTrain) {
//        TrackSegment nextTrackSegment = getNextTrackSegment();
//        if (nextTrackSegment.getBlockType() == BlockType.CONTINUOUS)
//            return true;
//        if (nextTrackSegment.isSectionReservedForTrain(this, rideTrain))
//            return true;
//        return nextTrackSegment.reserveBlockForTrain(this, this, rideTrain);
//    }

    public boolean canLeaveSection(@NotNull RideTrain rideTrain) {
//        if (getTrackedRide().isEmergencyStopActive() && isBlockSection)
//            return false;
        return true;
    }

    public boolean canAdvanceToNextBlock(@NotNull RideTrain rideTrain, boolean reserveNextBlockIfPossible) {
        return canAdvanceToNextBlock(rideTrain, reserveNextBlockIfPossible, false);
    }

    public boolean canAdvanceToNextBlock(@NotNull RideTrain rideTrain, boolean reserveNextBlockIfPossible, boolean assumeReserved) {
        if (blockSegmentAdvancing) {
            return false;
        }
        if (!isActive()) return false;
        TrackSegment nextTrackSegment = getNextTrackSegment();
        if (nextTrackSegment == null) return false;
        if (nextTrackSegment.getBlockType() == BlockType.CONTINUOUS)
            return true;
        if (nextTrackSegment.isSectionReservedForTrain(this, rideTrain))
            return true;
        return assumeReserved || reserveNextBlockIfPossible && nextTrackSegment.reserveBlockForTrain(this, this, rideTrain);
    }

    @Nullable
    public RideTrain getAnyRideTrainOnSegment() {
        List<RideTrain> rideTrains = getTrackedRide().getRideTrains();
        for (int i = 0; i < rideTrains.size(); i++) {
            RideTrain rideTrain = rideTrains.get(i);
            if (rideTrain.getLastCarTrackSegment() == this || rideTrain.getFrontCarTrackSegment() == this) {
                return rideTrain;
            }
        }
        return null;
    }

    public boolean isContainsTrain() {
        List<RideTrain> rideTrains = getTrackedRide().getRideTrains();
        for (int i = 0; i < rideTrains.size(); i++) {
            RideTrain rideTrain = rideTrains.get(i);
//            for(RideCar rideCar : rideTrain.getCars()) {
//                if(rideCar.getTrackSegment() == this)
//                    return true;
//            }
            if (rideTrain.getLastCarTrackSegment() == this || rideTrain.getFrontCarTrackSegment() == this) {
                return true;
            }
        }
        return false;
    }

    protected boolean isContainsTrainCached() {
        return containsTrain;
    }

    public void initialize() {
        initializers.forEach((it) -> it.run());
        initializers.clear();

        if (nextTrackSegment == null)
            throw new IllegalStateException("A SplinedTrackSegment it's nextTrackSegment cannot be null for " + id);
        if (previousTrackSegment == null)
            throw new IllegalStateException("A SplinedTrackSegment it's previousTrackSegment cannot be null for " + id);
        getLength();
    }

    public void destroy() {

    }

    public abstract double getLength();

    public void onTrainLeftSection(RideTrain rideTrain) {
//        Logger.console("RideTrain left " + getId());
        if (rideTrain == blockReservedTrain) {
            clearBlockReservedTrain();
        }
//        currentSpacingTicks = 0;
        for (Iterator<OnSectionLeaveListener> it = sectionLeaveListeners.iterator(); it.hasNext(); ) {
            OnSectionLeaveListener onSectionLeaveListener = it.next();
            onSectionLeaveListener.onTrainLeftSection(this, rideTrain);
        }
    }

    public void onTrainEnteredSection(@Nullable TrackSegment previousSegment, RideTrain rideTrain) {
        for (Iterator<OnSectionEnterListener> it = sectionEnterListeners.iterator(); it.hasNext(); ) {
            OnSectionEnterListener onSectionEnterListener = it.next();
            onSectionEnterListener.onTrainEnteredSection(this, rideTrain, previousSegment);
        }
//        Logger.console("RideTrain entered " + getId());
//        if (shouldAutomaticallyReroutePreviousSegment && previousSegment.getNextTrackSegment() == this) {
//            setPreviousTrackSegment(previousSegment);
//        }
    }

    @Nullable
    public TrackSegment getNextTrackSegment() {
//        if (nextSegmentProvider != null)
//            return nextSegmentProvider.provide();
        return nextTrackSegment;
    }

    public void setNextTrackSegment(@Nullable TrackSegment nextTrackSegment) {
        this.nextTrackSegment = nextTrackSegment;
    }

    /**
     * This method sets the previous segment for the given nextTrackSegment to this segment
     *
     * @return nextTrackSegment
     */
    public TrackSegment setNextTrackSegmentRetroActive(TrackSegment nextTrackSegment) {
        this.nextTrackSegment = nextTrackSegment;
        nextTrackSegment.setPreviousTrackSegment(this);
        return nextTrackSegment;
    }

    @Nullable
    public TrackSegment getPreviousTrackSegment() {
//        if (previousSegmentProvider != null)
//            return previousSegmentProvider.provide();
        return previousTrackSegment;
    }

    public void setPreviousTrackSegment(@Nullable TrackSegment previousTrackSegment) {
        this.previousTrackSegment = previousTrackSegment;
    }

    public List<TrackSegment> getSubsegments() {
        return new ArrayList<>();
    }

    public final double getBanking(double distance) {
        return getBanking(distance, true);
    }

    public abstract double getBanking(double distance, boolean applyInterceptors);

    public abstract void getStartPosition(Vector position);

    public abstract void getEndPosition(Vector position);

    public final void getPosition(double distance, Vector position) {
        getPosition(distance, position, true);
    }

    public abstract void getPosition(double distance, Vector position, boolean applyInterceptors);

    public void onDistanceUpdated(RideCar car, double currentDistance, double previousDistance) {
        for (DistanceListener distanceListener : distanceListeners) {
            //        for (int i = 0; i < distanceListeners.size(); i++) {
//            DistanceListener distanceListener = distanceListeners.get(i);
            distanceListener.checkIfTargetHit(car, previousDistance, currentDistance);
        }
    }

    public abstract void applyForces(RideCar car, double distanceSinceLastUpdate);

    // TODO: Improve this temporary workaround for forces
    public void applySecondaryForces(RideCar car, double distanceSinceLastUpdate) {

    }

    public boolean handleContinuousCheck(RideCar car, double currentDistance, double previousDistance) {
        double trainVelocity = car.attachedTrain.getVelocity();
        if (trainVelocity != 0) {
            boolean goingForward = trainVelocity > 0;
            if (goingForward && car == car.attachedTrain.getCars().get(0)) {
                List<RideTrain> rideTrains = getTrackedRide().getRideTrains();
                for (int i = 0; i < rideTrains.size(); i++) {
                    RideTrain rideTrain = rideTrains.get(i);
                    if (rideTrain != car.attachedTrain) {
                        RideCar lastCar = rideTrain.getCars().get(rideTrain.getCars().size() - 1);
                        TrackSegment trainBack = lastCar.getTrackSegment();
                        double lastCarBackDistance = lastCar.getDistance() - lastCar.length;
                        while (lastCarBackDistance < 0) {
                            trainBack = trainBack.getPreviousTrackSegment();
                            lastCarBackDistance += trainBack.getLength();
                        }

                        if (trainBack == car.getTrackSegment() && lastCarBackDistance + car.length > car.attachedTrain.getFrontCarDistance()) {
                            //TODO: Improve startDistance vs stopDistance check
                            boolean willNextPositionRunIntoNextCar = car.attachedTrain.getVelocity() + car.getAcceleration() +
                                    currentDistance + (car.attachedTrain.getVelocity() != 0 ? getStartDistance(car) : getStopDistance(car)) >=
                                    lastCarBackDistance;
                            boolean isCurrentlyBeforeTrain = car.attachedTrain.getFrontCarDistance() < lastCarBackDistance + car.length;
                            if (willNextPositionRunIntoNextCar && isCurrentlyBeforeTrain) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public void applyForceCheck(RideCar car, double currentDistance, double previousDistance) {
        boolean haltTrain = false;
        if (isBlockSection && !disableHaltCheck) {
            if (car.attachedTrain.getVelocity() + currentDistance + offsetFromNextSection > length) {
                if (!canAdvanceToNextBlock(car.attachedTrain, true)) {
                    haltTrain = true;
                }
            }
        }
        if (forceContinuousCheck || isBlockSection && blockType == BlockType.CONTINUOUS) {
            if (handleContinuousCheck(car, currentDistance, previousDistance)) {
                haltTrain = true;
            }
        }

        if (haltTrain) {
            isHaltingTrainThisUpdate = true;

//                Logger.console("Halt for car on " + getId());
            List<RideCar> cars = car.attachedTrain.getCars();
            for (int i = 0; i < cars.size(); i++) {
                RideCar rideCar = cars.get(i);
                rideCar.setVelocity(0);
                rideCar.setAcceleration(0);
            }
        }
    }

    public double getStopDistance(RideCar car) {
        return stopDistance;
    }

    public double getStartDistance(RideCar car) {
        return startDistance;
    }

    @Nullable
    public String debugData() {
        return null;
    }

    public void update() {
        updateListeners.forEach(listener -> listener.onUpdate(this));
        if (debugTagString != null) {
            Vector position = new Vector();
            getPosition(length * 0.5, position);
            position.setY(position.getY() - EntityConstants.ArmorStandHeadOffset);
            World world = trackedRide.getArea().getWorld();
            if (debugTag == null) {
                debugTag = world.spawn(position.toLocation(world), ArmorStand.class);
                debugTag.setPersistent(false);
                debugTag.setVisible(false);
                debugTag.setCustomNameVisible(true);
            } else {
                debugTag.teleport(position.toLocation(world));
            }
            if (debugTag.getCustomName() == null || !debugTag.getCustomName().equals(debugTagString))
                debugTag.setCustomName(debugTagString);
        } else {
            if (debugTag != null) {
                debugTag.remove();
            }
        }
//        if (spacingTicks > 0) {
//            currentSpacingTicks++;
//        }
        boolean containsTrain = isContainsTrain();
        if (containsTrain != this.containsTrain) {
            this.containsTrain = containsTrain;
            if (containsListener != null) {
//                Logger.console("containsListener " + this.containsTrain);
                containsListener.onContainsTrain(this.containsTrain);
            }
        }

        if (isHaltingTrainThisUpdate) {
            if (!isStoppingTrainFromLeaving) {
                isStoppingTrainFromLeaving = true;

                for (SectionHaltListener sectionHaltListener : sectionHaltListeners) {
                    sectionHaltListener.onSectionHalted(this, containsTrain, true);
                }
            }
        } else {
            if (isStoppingTrainFromLeaving) {
                isStoppingTrainFromLeaving = false;

                for (SectionHaltListener sectionHaltListener : sectionHaltListeners) {
                    sectionHaltListener.onSectionHalted(this, containsTrain, false);
                }
            }
        }
        isHaltingTrainThisUpdate = false;
    }

    public enum BlockType {
        BLOCK_SECTION,
        CONTINUOUS
    }

    public interface ContainsListener {
        void onContainsTrain(boolean containsTrain);
    }

    public interface SectionHaltListener {
        void onSectionHalted(TrackSegment trackSegment, boolean containsTrain, boolean isStoppingTrainFromLeaving);
    }

    public static abstract class DistanceListener {
        private final double targetDistance;
        private boolean filterFirstCar;
        private boolean filterLastCar;

        public DistanceListener(double targetDistance) {
            this.targetDistance = targetDistance;
        }

        public DistanceListener(double targetDistance, boolean filterFirstCar) {
            this.targetDistance = targetDistance;
            this.filterFirstCar = filterFirstCar;
        }

        public DistanceListener(double targetDistance, boolean filterFirstCar, boolean filterLastCar) {
            this.targetDistance = targetDistance;
            this.filterFirstCar = filterFirstCar;
            this.filterLastCar = filterLastCar;
        }

        public double getTargetDistance() {
            return targetDistance;
        }

        public void checkIfTargetHit(RideCar rideCar, double oldDistance, double newDistance) {
            List<RideCar> cars = rideCar.attachedTrain.getCars();
            boolean isFirstCar = filterFirstCar && rideCar == cars.get(0) && cars.get(0).getTrackSegment() == rideCar.getTrackSegment();
            boolean isLastCar = rideCar == cars.get(cars.size() - 1) && cars.get(cars.size() - 1).getTrackSegment() == rideCar.getTrackSegment();

            if (filterFirstCar && filterLastCar && !(isFirstCar || isLastCar)) {
                return;
            } else if (filterFirstCar && !isFirstCar) {
                return;
            } else if (filterLastCar && !isLastCar) {
                return;
            }
//            if (targetDistance == 11 && (filterFirstCar || isFirstCar || filterLastCar || isLastCar))
//                Logger.console(String.format("%s %s %s %s", filterFirstCar, isFirstCar, filterLastCar, isLastCar));

//            if ((filterFirstCar && rideCar != cars.get(0)) && (filterLastCar && rideCar != cars.get(cars.size() - 1)))
//                return;

//            Logger.console(oldDistance + " vs " + newDistance);
            if (oldDistance < newDistance && rideCar.getVelocity() > 0) {
                if (targetDistance >= oldDistance && targetDistance < newDistance) {
                    onTargetHit(rideCar);
                }
            } else if (rideCar.getVelocity() < 0) {
                if (targetDistance >= newDistance && targetDistance < oldDistance) {
                    onTargetHit(rideCar);
                }
            }
        }

        public abstract void onTargetHit(@NotNull RideCar rideCar);
    }

    public interface UpdateListener {
        void onUpdate(TrackSegment segment);
    }

    public interface PositionInterceptor {
        void getPosition(@NotNull TrackSegment trackSegment, double distance, @NotNull Vector position);
    }

    public interface BankingInterceptor {
        double getBanking(@NotNull TrackSegment trackSegment, double distance, double actualBanking);
    }

    public interface OnSectionLeaveListener {
        void onTrainLeftSection(TrackSegment trackSegment, RideTrain rideTrain);
    }

    public interface OnSectionEnterListener {
        void onTrainEnteredSection(TrackSegment trackSegment, RideTrain rideTrain, @Nullable TrackSegment previousSegment);
    }

    public interface OnExitLocationProvider {
        Location onProvideExitLocation(LeaveType leaveType);
    }

    public interface PlayerEnterListener {
        void onPlayerEnteredCarOnSegment(RideCar rideCar, Player player);
    }

    public interface PlayerExitListener {
        void onPlayerExitedCarOnSegment(RideCar rideCar, Player player);
    }

    @NotNull
    public abstract TrackSegmentJson toJson();

    @NotNull
    public <T extends TrackSegmentJson> T toJson(T source) {
        source.setDisplayName(GsonComponentSerializer.gson().serialize(displayName));
        source.setId(id);
        source.setLeaveMode(leaveMode);
        source.setEjectType(ejectType);
        source.setFriction(friction);
        source.setGravitationalInfluence(gravitationalInfluence);
        source.setBlockSection(isBlockSection);
        source.setBlockType(blockType);
        source.setDisableHaltCheck(disableHaltCheck);
        source.setNameOnMap(nameOnMap);
        source.setTrackType(trackType);
        source.setActive(active);
        source.setShouldAutomaticallyReroutePreviousSegment(shouldAutomaticallyReroutePreviousSegment);
        source.setOffsetFromNextSection(offsetFromNextSection);

        return source;
    }

    @NotNull
    public <T extends TrackSegmentJson> void restore(T source) {
//        id = source.getId();
//        displayName = source.getDisplayName();
        leaveMode = source.getLeaveMode();
        ejectType = source.getEjectType();
        friction = source.getFriction();
        gravitationalInfluence = source.getGravitationalInfluence();
        isBlockSection = source.isBlockSection();
        blockType = source.getBlockType();
        disableHaltCheck = source.getDisableHaltCheck();
        nameOnMap = source.getNameOnMap();
        trackType = source.getTrackType();
        active = source.getActive();
        shouldAutomaticallyReroutePreviousSegment = source.getShouldAutomaticallyReroutePreviousSegment();
        offsetFromNextSection = source.getOffsetFromNextSection();

        if (source.getDistanceListeners() != null) {
            source.getDistanceListeners().forEach((action) -> {
                add(action.create());
            });
        }
    }

    public enum LeaveType {
        LEFT,
        EJECT,
//        OTHER
    }

    public enum EjectType {
        EJECT_TO_EXIT,
        EJECT_TO_SEAT,
        IGNORE
    }

    public enum LeaveMode {
        LEAVE_TO_EXIT,
        LEAVE_TO_SEAT_WHEN_HALTED,
        LEAVE_TO_SEAT_WHEN_CAN_ENTER,
        LEAVE_TO_SEAT,
    }

    public enum TrackType {
        DEFAULT,
        CHAIN_LIFT,
        LSM_LAUNCH,
        WHEEL_TRANSPORT,
        MAG_BRAKE,
        FRICTION_BRAKE,
    }
}
