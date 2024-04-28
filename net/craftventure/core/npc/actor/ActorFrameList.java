package net.craftventure.core.npc.actor;

import net.craftventure.core.npc.NpcEntity;
import net.craftventure.core.npc.actor.action.ActionDoubleSetting;
import net.craftventure.core.utils.InterpolationUtils;

import java.util.ArrayList;
import java.util.Collections;


public class ActorFrameList<T extends ActorFrame> extends ArrayList<T> {
    private int currentIndex = 0;
    private ActionInterpolator<T> actionInterpolator;

    public ActorFrameList<T> setInterpolator(ActionInterpolator<T> actionInterpolator) {
        this.actionInterpolator = actionInterpolator;
        return this;
    }

    public void resetIndex() {
        currentIndex = 0;
    }

    public void apply(long timeTo, NpcEntity npcEntity) {
        if (isEmpty())
            return;

        for (int i = currentIndex; i < size(); i++) {
            ActorFrame actorFrame = get(i);
            if (actorFrame.getTime() < timeTo) {
                if (actionInterpolator == null) {
                    actorFrame.getAction().executeAction(npcEntity);
                }
                currentIndex = i;
            } else {
                break;
            }
        }
        if (actionInterpolator != null) {
            if (size() >= 2) {
                int nextIndex = currentIndex + 1 < size() ? currentIndex + 1 : currentIndex;

                if (nextIndex != currentIndex) {
                    T a = get(currentIndex);
                    T b = get(nextIndex);
                    if (a.getTime() < timeTo) {
                        double t = (float) (timeTo - a.getTime()) / (float) (b.getTime() - a.getTime());
//                        Logger.console("T " + t + " for " + timeTo + " > " + a.getTime() + " > " + b.getTime());
                        actionInterpolator.interpolate(a, b, t, npcEntity);
                    } else {
                        a.getAction().executeAction(npcEntity);
//                        Logger.console("A executed (" + a.getTime() + " < " + timeTo + ")");
                    }
                } else {
                    get(currentIndex).getAction().executeAction(npcEntity);
                }
            } else if (!isEmpty()) {
                get(0).getAction().executeAction(npcEntity);
            }
        }
    }

    public void sortByFrametime() {
        Collections.sort(this, (o1, o2) -> (int) (o1.getTime() - o2.getTime()));
    }

    public interface ActionInterpolator<T> {
        void interpolate(T from, T to, double t, NpcEntity npcEntity);
    }

    public static class MultiDoubleSettingInterpolator {
        private final ActorFrameList<ActorFrame<ActionDoubleSetting>>[] framesHolders;
        public final double[] values;
        private MultiDoubleSettingInterpolatorApplyer applyer;

        public MultiDoubleSettingInterpolator(ActorFrameList<ActorFrame<ActionDoubleSetting>>... framesHolders) {
            this.framesHolders = framesHolders;
            this.values = new double[this.framesHolders.length];
        }

        public void setApplyer(MultiDoubleSettingInterpolatorApplyer applyer) {
            this.applyer = applyer;
        }

        public int frames() {
            return framesHolders.length;
        }

        public void apply(long timeTo, NpcEntity npcEntity) {
            for (int i = 0; i < framesHolders.length; i++) {
                values[i] = 0;

                ActorFrameList<ActorFrame<ActionDoubleSetting>> actorFrames = framesHolders[i];
                if (actorFrames.isEmpty())
                    return;

                //TODO: Get this working again Edit: I think it does already?
                for (int j = actorFrames.currentIndex; j < actorFrames.size(); j++) {
                    ActorFrame actorFrame = actorFrames.get(j);
                    if (actorFrame.getTime() < timeTo) {
//                        if (actorFrames.actionInterpolator == null) {
//                            actorFrame.getAction().executeAction(npcEntity);
//                        }
                        actorFrames.currentIndex = j;
                    } else {
                        break;
                    }
                }
                if (actorFrames.size() >= 2) {
                    int nextIndex = actorFrames.currentIndex + 1 < actorFrames.size() ? actorFrames.currentIndex + 1 : actorFrames.currentIndex;

                    if (nextIndex != actorFrames.currentIndex) {
                        ActorFrame<ActionDoubleSetting> a = actorFrames.get(actorFrames.currentIndex);
                        ActorFrame<ActionDoubleSetting> b = actorFrames.get(nextIndex);
                        if (a.getTime() < timeTo) {
                            double t = (float) (timeTo - a.getTime()) / (float) (b.getTime() - a.getTime());
//                        Logger.console("T " + t + " for " + timeTo + " > " + a.getTime() + " > " + b.getTime());
                            values[i] = InterpolationUtils.linearInterpolate(a.getAction().getValue(), b.getAction().getValue(), t);
                        } else {
                            values[i] = a.getAction().getValue();
//                            a.getAction().executeAction(npcEntity);
//                        Logger.console("A executed (" + a.getTime() + " < " + timeTo + ")");
                        }
                    } else {
                        values[i] = actorFrames.get(actorFrames.currentIndex).getAction().getValue();
                    }
                } else if (!actorFrames.isEmpty()) {
                    values[i] = actorFrames.get(0).getAction().getValue();
                }
            }
            if (applyer != null) {
                applyer.apply(this);
            }
        }
    }

    public interface MultiDoubleSettingInterpolatorApplyer {
        void apply(MultiDoubleSettingInterpolator multiDoubleSettingInterpolator);
    }
}
