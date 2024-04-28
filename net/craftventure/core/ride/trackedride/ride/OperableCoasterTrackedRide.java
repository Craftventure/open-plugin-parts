package net.craftventure.core.ride.trackedride.ride;

import net.craftventure.bukkit.ktx.area.Area;
import net.craftventure.bukkit.ktx.area.AreaTracker;
import net.craftventure.bukkit.ktx.util.PermissionChecker;
import net.craftventure.chat.bungee.util.CVChatColor;
import net.craftventure.chat.bungee.util.CVTextColor;
import net.craftventure.core.CraftventureCore;
import net.craftventure.core.ride.operator.OperableRide;
import net.craftventure.core.ride.operator.OperatorAreaTracker;
import net.craftventure.core.ride.operator.controls.ControlColors;
import net.craftventure.core.ride.operator.controls.OperatorButton;
import net.craftventure.core.ride.operator.controls.OperatorControl;
import net.craftventure.core.ride.trackedride.TrackSegment;
import net.craftventure.core.ride.trackedride.segment.OperableDependentSegment;
import net.craftventure.core.ride.trackedride.segment.OperableTrackSegment;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


public class OperableCoasterTrackedRide extends CoasterTrackedRide implements OperableRide, OperatorControl.ControlListener {
    private final int maxOperators = 1;
    private Player operator;
    private Area operatorArea;
    private final OperatorButton emergencyButton;
    private final OperatorButton broadcastButton;
    private final OperatorAreaTracker operatorAreaTracker;

    public OperableCoasterTrackedRide(String name, Area area, Location exitLocation, String achievementName, String rideName) {
        super(name, area, exitLocation, achievementName, rideName);

        broadcastButton = new OperatorButton("broadcast", OperatorButton.Type.DEFAULT, ControlColors.getNEUTRAL(), ControlColors.getNEUTRAL_DARK(), ControlColors.getNEUTRAL());
        broadcastButton
                .setFlashing(true)
                .setName(CVTextColor.getMENU_DEFAULT_TITLE(), "Broadcast operated")
                .setDescription(CVTextColor.getMENU_DEFAULT_LORE(), "Can be used every few minutes to let the server know you're operating this ride")
                .setSort(1)
                .setGroup(null)
                .setControlListener(this);

        emergencyButton = new OperatorButton("emergency", OperatorButton.Type.E_STOP);
        emergencyButton
                .setName(CVTextColor.getMENU_DEFAULT_TITLE(), "E-stop (Crew)")
                .setDescription(CVTextColor.getMENU_DEFAULT_LORE(), "Used by crewmembers to enable/disable an emergency stop")
                .setSort(2)
                .setGroup(null)
                .setControlListener(this);

        operatorAreaTracker = new OperatorAreaTracker(this, area);
    }

    @NotNull
    @Override
    public AreaTracker getOperatorAreaTracker() {
        return operatorAreaTracker;
    }

    public void setOperatorArea(Area operatorArea) {
        this.operatorArea = operatorArea;
        operatorAreaTracker.updateArea(operatorArea);
    }

    @Override
    public boolean isBeingOperated() {
        return operator != null;
    }

    @Nullable
    @Override
    public Player getOperatorForSlot(int slot) {
        if (slot == 0)
            return operator;
        return null;
    }

    @Override
    public int getOperatorSlot(Player player) {
        return player == operator ? 0 : -1;
    }

    @Override
    public boolean setOperator(@Nullable Player player, int slot) {
        if (slot == 0 && operator == null) {
            operator = player;
            if (operator != null)
                operator.sendMessage(CVChatColor.INSTANCE.getServerNotice() + "You are now operating " + getRide().getDisplayName());
//            cancelAutoStart();

            for (TrackSegment trackSegment : trackSegments) {
                if (trackSegment instanceof OperableTrackSegment) {
                    OperableTrackSegment operableTrackSegment = (OperableTrackSegment) trackSegment;
                    operableTrackSegment.onOperatorsChanged();
                } else if (trackSegment instanceof OperableDependentSegment) {
                    ((OperableDependentSegment) trackSegment).onOperatorsChanged();
                }
            }

            return true;
        }
        return false;
    }

    @Override
    public int getTotalOperatorSpots() {
        return 1;
    }

    @Override
    public void cancelOperating(int slot) {
        if (slot == 0) {
            if (operator != null) {
                operator.sendMessage(CVChatColor.INSTANCE.getServerNotice() + "You are no longer operating " + getRide().getDisplayName());
                operator = null;

                for (TrackSegment trackSegment : trackSegments) {
                    if (trackSegment instanceof OperableTrackSegment) {
                        OperableTrackSegment operableTrackSegment = (OperableTrackSegment) trackSegment;
                        operableTrackSegment.onOperatorsChanged();
                    } else if (trackSegment instanceof OperableDependentSegment) {
                        ((OperableDependentSegment) trackSegment).onOperatorsChanged();
                    }
                }
//                scheduleAutoStart();
//                tryOpenGatesIfPossible(true);
            }
        }
    }

    @Override
    public List<OperatorControl> provideControls() {
        List<OperatorControl> controls = new ArrayList<>();

        controls.add(broadcastButton);

        for (TrackSegment trackSegment : trackSegments) {
            if (trackSegment instanceof OperableTrackSegment) {
                OperableTrackSegment operableTrackSegment = (OperableTrackSegment) trackSegment;
                controls.addAll(operableTrackSegment.provideControls());
            }
        }

        controls.add(emergencyButton);

        return controls;
    }

    @Override
    public boolean isInOperateableArea(@NotNull Location location) {
        return operatorArea.isInArea(location);
    }

    @Override
    public void updateWhileOperated() {

    }

    @Override
    public String getId() {
        return getName();
    }

    @Override
    public void onClick(@NotNull OperableRide operableRide, @NotNull Player player, @NotNull OperatorControl operatorControl, Integer operatorSlot) {
        if (!operatorControl.isEnabled()) {
            return;
        }
        if (operatorControl == emergencyButton) {
            if (PermissionChecker.INSTANCE.isCrew(player)) {
                activateEmergencyStop(!isEmergencyStopActive());
            } else {
                player.sendMessage(CVChatColor.INSTANCE.getServerError() + "E-Stop usage is only accessible to crew");
            }
        } else if (operatorControl == broadcastButton) {
            CraftventureCore.getOperatorManager().broadcastRideOperated(this);
        }
    }

    @Override
    protected void onEmergencyStopChanged(boolean emergencyStopActive) {
        super.onEmergencyStopChanged(emergencyStopActive);
//        emergencyButton.setControlColor(emergencyStopActive ? ControlColor.GREEN : ControlColor.RED);
        emergencyButton.setType(emergencyStopActive ? OperatorButton.Type.E_STOP_ACTIVATED : OperatorButton.Type.E_STOP);
    }

//    @Override
//    public void onClick(OperateableRide operateableRide, Player player, OperatorControl operatorControl, int operatorSlot) {
//        if (operatorControl.isEnabled()) {
//            if (operatorControl == buttonDispatch) {
//                tryOperatorStart();
//            } else if (operatorControl == buttonGates) {
//                tryOpenGatesIfPossible(!buttonGates.isOn());
//            }
//        }
//    }
}
