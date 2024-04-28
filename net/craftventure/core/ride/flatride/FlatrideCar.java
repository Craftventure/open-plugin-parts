package net.craftventure.core.ride.flatride;

import net.craftventure.core.utils.EntityUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public abstract class FlatrideCar<T extends Entity> {
    protected final T[] entities;

    protected FlatrideCar(T[] entities) {
        this.entities = entities;
    }

    public abstract void teleport(double x, double y, double z, float yaw, float pitch, boolean forceTeleport);

    public T[] getEntities() {
        return entities;
    }

    public boolean isExitAllowed(Player player, Entity dismounted) {
        return true;
    }

    public int passengerCount() {
        int count = 0;
        if (entities != null) {
            for (T entity : entities) {
                if (entity != null && entity.getPassenger() != null)
                    count++;
            }
        }
        return count;
    }

    public void eject() {
        if (entities != null) {
            for (T entity : entities) {
                eject(entity);
            }
        }
    }

    protected void eject(T entity) {
        if (entity != null)
            entity.eject();
    }

    @Nullable
    public Entity getEntity(Entity target) {
        if (entities != null) {
            for (T entity : entities) {
                if (entity != null && entity.getEntityId() == target.getEntityId()) {
                    return entity;
                }
            }
        }
        return null;
    }

    public boolean setPassenger(Entity seat, Player player) {
        if (entities != null) {
            for (T entity : entities) {
                if (entity != null && entity.getEntityId() == seat.getEntityId()) {
                    return !EntityUtils.INSTANCE.hasPlayerPassengers(entity) && entity.addPassenger(player);
                }
            }
        }
        return false;
    }

    public boolean isPassenger(Entity passenger) {
        if (entities != null) {
            for (T entity : entities) {
                if (entity != null && entity.getPassenger() != null && entity.getPassenger().getEntityId() == passenger.getEntityId()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isPassengerOrSeat(Entity passenger, @Nullable Entity seat) {
        if (entities != null) {
            for (T entity : entities) {
                if (entity != null && ((entity.getPassenger() != null && entity.getPassenger().getEntityId() == passenger.getEntityId()) ||
                        (passenger instanceof Player && ((Player) passenger).getSpectatorTarget() != null && ((Player) passenger).getSpectatorTarget().getEntityId() == entity.getEntityId()))) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isPartOf(Entity part) {
        if (entities != null) {
            for (T entity : entities) {
                if (entity != null && entity.getEntityId() == part.getEntityId()) {
                    return true;
                }
            }
        }
        return false;
    }
}
