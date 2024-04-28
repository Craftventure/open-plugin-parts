package net.craftventure.core.ride.trackedride;

import net.craftventure.core.ride.flatride.Flatride;
import net.craftventure.database.generated.cvdata.tables.pojos.Ride;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


public class FlatrideManager {
    private static final List<Flatride> flatrideList = new ArrayList<>(20);

    public static void addFlatride(Flatride flatride) {
        flatrideList.add(flatride);
    }

    @Nullable
    public static Flatride getFlatride(String internalName) {
        for (Flatride flatride : flatrideList) {
            Ride ride = flatride.getRide();
            if (ride != null && internalName.equalsIgnoreCase(ride.getName())) {
                return flatride;
            }
        }
        return null;
    }

    public static List<Flatride> getFlatrideList() {
        return flatrideList;
    }
}
