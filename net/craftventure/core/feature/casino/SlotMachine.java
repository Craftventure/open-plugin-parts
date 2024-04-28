package net.craftventure.core.feature.casino;

import net.craftventure.bukkit.ktx.util.SoundUtils;
import net.craftventure.bukkit.ktx.util.Translation;
import net.craftventure.chat.bungee.util.CVTextColor;
import net.craftventure.core.CraftventureCore;
import net.craftventure.core.async.AsyncResultTask;
import net.craftventure.core.async.AsyncTask;
import net.craftventure.core.ktx.util.Logger;
import net.craftventure.core.map.renderer.MapManager;
import net.craftventure.core.npc.NpcEntity;
import net.craftventure.core.npc.tracker.NpcAreaTracker;
import net.craftventure.core.utils.ParticleSpawnerKt;
import net.craftventure.database.MainRepositoryProvider;
import net.craftventure.database.generated.cvdata.tables.pojos.BankAccount;
import net.craftventure.database.generated.cvdata.tables.pojos.SlotMachineItem;
import net.craftventure.database.type.BankAccountType;
import net.craftventure.database.type.SlotMachineRewardType;
import net.craftventure.database.type.TransactionType;
import net.craftventure.temporary.SlotMachineItemExtensionsKt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;


public class SlotMachine implements Listener {
    private final double angle;
    private final Location location;
    private final int input;
    private final NpcEntity item1;
    private final NpcEntity item2;
    private final NpcEntity item3;
    private final SecureRandom secureRandom = new SecureRandom();
    private int bukkitTaskId;
    private final NpcAreaTracker areaTracker;
    private Player player;
    private final List<SlotMachineItem> slotMachineItems = new ArrayList<>();
    private final String machineType;
    private boolean inUse = false;

    public SlotMachine(NpcAreaTracker areaTracker, double angle, Location location, int input, String machineType) {
        this.areaTracker = areaTracker;
        this.machineType = machineType;
        this.angle = angle;
        this.location = location;
        this.input = input;
        Bukkit.getServer().getPluginManager().registerEvents(this, CraftventureCore.getInstance());

        slotMachineItems.addAll(MainRepositoryProvider.INSTANCE.getSlotMachineItemsRepository().itemsPojo());
        slotMachineItems.removeIf(slotMachineItem -> !machineType.equalsIgnoreCase(slotMachineItem.getMachineType()));
        slotMachineItems.sort((o1, o2) -> (int) (o1.getPercentage() - o2.getPercentage()));

        for (SlotMachineItem slotMachineItem : slotMachineItems) {
            SlotMachineItemExtensionsKt.getResolvedItem(slotMachineItem);
        }

        item1 = spawnIfRequired(1.2, -0.7, 0);
        item2 = spawnIfRequired(0.5, -0.7, 0);
        item3 = spawnIfRequired(-0.2, -0.7, 0);

        for (SlotMachineItem slotMachineItem : slotMachineItems) {
            if (transform(input, slotMachineItem.getSlotMachineRewardType()) == 0) {
                throw new IllegalStateException("Missing RewardType for SlotMachine " + slotMachineItem.getSlotMachineRewardType().name());
            }
        }

//        if (CraftventureCore.isTestServer()) {
//            int start = 0;
//            for (int i = 0; i < 10_000_000; i++) {
//                start -= input;
//                SlotMachineItem randomItem = getRandomWinningItem();
//                SlotMachineItem a;
//                SlotMachineItem b;
//                SlotMachineItem c;
//                if (randomItem != null) {
////                                    Logger.console("WINNING ITEM!");
//                    a = b = c = randomItem;
//                } else {
////                                    Logger.console("No winning item");
//                    a = slotMachineItems.get((secureRandom.nextInt(slotMachineItems.size())));
//                    b = slotMachineItems.get((secureRandom.nextInt(slotMachineItems.size())));
//                    c = slotMachineItems.get((secureRandom.nextInt(slotMachineItems.size())));
//                    while (a == b && a == c) {
//                        a = slotMachineItems.get((secureRandom.nextInt(slotMachineItems.size())));
//                        b = slotMachineItems.get((secureRandom.nextInt(slotMachineItems.size())));
//                        c = slotMachineItems.get((secureRandom.nextInt(slotMachineItems.size())));
//                    }
//                }
//                if (a == b && a == c) {
//                    SlotMachineRewardType type = a.getSlotMachineRewardType();
//                    int reward = transform(input, type);
//                    start += reward;
//                }
//            }
//            Logger.debug("End result is %d for %s", false, start, machineType);
//        }

        Block block = location.clone().add(1, 0, -1).getBlock();
        if (block != null && block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            sign.line(0, Component.text("").append(Component.text(input, NamedTextColor.BLACK).decorate(TextDecoration.BOLD)).append(Component.text(" \uE006", NamedTextColor.WHITE)));
            sign.line(1, Component.text("Per spin", NamedTextColor.BLACK).decorate(TextDecoration.BOLD));
            Component line2 = Component.text("", NamedTextColor.WHITE);
            Component line3 = Component.text("", NamedTextColor.WHITE);

            SlotMachineItem item1 = slotMachineItems.get(0);
            if (item1 != null) line2 = addToComponent(line2, item1);
            SlotMachineItem item2 = slotMachineItems.get(1);
            if (item1 != null) line2 = addToComponent(line2, item2);
            SlotMachineItem item3 = slotMachineItems.get(2);
            if (item1 != null) line3 = addToComponent(line3, item3);
            SlotMachineItem item4 = slotMachineItems.get(3);
            if (item1 != null) line3 = addToComponent(line3, item4);

            sign.line(2, line2);
            sign.line(3, line3);
            sign.update(true, true);
        }
    }

    private Component addToComponent(Component component, SlotMachineItem item) {
        String itemName = item.getItemId();
        if (itemName == null) return component;
        String emoji = itemName.equals("iron_ingot") ? "\uE05E" : itemName.equals("gold_ingot") ? "\uE05D" : itemName.equals("emerald") ? "\uE05F" : itemName.equals("diamond") ? "\uE060" : null;
        if (emoji == null) return component;
        return component
                .append(Component.text(emoji))
                .append(Component.text(" " + item.getSlotMachineRewardType().getDescription() + " ", NamedTextColor.BLACK));
    }

    public int transform(int input, SlotMachineRewardType type) {
        if (type == SlotMachineRewardType.X0_5) {
            return (int) Math.floor(input * 0.5);
        } else if (type == SlotMachineRewardType.X1) {
            return input * 1;
        } else if (type == SlotMachineRewardType.X2) {
            return input * 2;
        } else if (type == SlotMachineRewardType.X3) {
            return input * 3;
        } else if (type == SlotMachineRewardType.X4) {
            return input * 4;
        } else if (type == SlotMachineRewardType.X5) {
            return input * 5;
        } else if (type == SlotMachineRewardType.X6) {
            return input * 6;
        } else if (type == SlotMachineRewardType.X8) {
            return input * 8;
        } else if (type == SlotMachineRewardType.X10) {
            return input * 10;
        } else if (type == SlotMachineRewardType.X15) {
            return input * 15;
        } else if (type == SlotMachineRewardType.X20) {
            return input * 20;
        } else if (type == SlotMachineRewardType.X25) {
            return input * 25;
        } else if (type == SlotMachineRewardType.X50) {
            return input * 50;
        }
        return 0;
    }

    private boolean itemsValid() {
        return slotMachineItems.size() >= 2;
    }

    private NpcEntity spawnIfRequired(double xOffset, double yOffset, double zOffset) {
        NpcEntity npcEntity = new NpcEntity("slotMachine", EntityType.ARMOR_STAND, location.clone().add(xOffset, yOffset, zOffset));
        npcEntity.noGravity(true);
        npcEntity.invisible(true);
        npcEntity.helmet(new ItemStack(itemsValid() ? Material.GOLDEN_APPLE : Material.BARRIER));
        areaTracker.addEntity(npcEntity);
        return npcEntity;
    }

    private void reward(Player player, SlotMachineItem a, SlotMachineItem b, SlotMachineItem c) {
        if (a == b && a == c) {
            SlotMachineRewardType type = a.getSlotMachineRewardType();
            int reward = transform(input, type);
            for (int i = 0; i < Math.min(reward, 5); i++)
                CasinoManager.INSTANCE.spawnCoin(location.clone().add(-1, -0.5, -0.6), new Vector(-0.05 + CraftventureCore.getRandom().nextDouble() * 0.1, -0.05 + CraftventureCore.getRandom().nextDouble() * 0.1, -0.2), 40 + CraftventureCore.getRandom().nextInt(40));
            new AsyncTask() {
                @Override
                public void doInBackground() {
                    if (reward != 0) {
                        if (MainRepositoryProvider.INSTANCE.getBankAccountRepository().delta(player.getUniqueId(), BankAccountType.VC, reward, TransactionType.CASINO_WIN)) {
                            player.sendMessage(Translation.CASINO_SLOTMACHINE_WON.getTranslation(player, reward));
                            location.getWorld().playSound(location, SoundUtils.INSTANCE.getMONEY(), 1f, 1f);
                        }
                    } else if (type != null) {
                        Logger.severe("SlotMachine won but it's reward type is unknown!", true);
                        MainRepositoryProvider.INSTANCE.getBankAccountRepository().delta(player.getUniqueId(), BankAccountType.VC, input * 1, TransactionType.CASINO_WIN);
                        return;
                    } else if (reward == 0) {
                        player.sendMessage(Translation.CASINO_SLOTMACHINE_LOST.getTranslation(player));
                    }
                    MainRepositoryProvider.INSTANCE.getCasinoLogRepository().createOrUpdate(player.getUniqueId(), SlotMachine.this.machineType, reward);
                    MapManager.getInstance().invalidateForCasinoMachine(SlotMachine.this.machineType);
                }
            }.executeNow();
        } else {
            if (CraftventureCore.getRandom().nextDouble() < 0.05) {
                ParticleSpawnerKt.spawnParticleX(location.clone().add(-1, -0.5, -0.6),
                        Particle.SOUL,
                        5,
                        0.1, 0.1, 0);
            } else {
                ParticleSpawnerKt.spawnParticleX(location.clone().add(-1, -0.5, -0.6),
                        Particle.SMOKE_NORMAL,
                        5,
                        0.1, 0.1, 0);
            }
            player.sendMessage(Translation.CASINO_SLOTMACHINE_LOST.getTranslation(player));
        }
    }

    @Nullable
    private SlotMachineItem getRandomWinningItem() {
        double winPercentage = secureRandom.nextDouble() * 100;
        double accumulativePercentage = 0;
        for (SlotMachineItem slotMachineItem : slotMachineItems) {
            double itemPercentage = slotMachineItem.getPercentage();
            if (winPercentage >= accumulativePercentage && winPercentage < accumulativePercentage + itemPercentage) {
//                Logger.console("Item: " + winPercentage + " vs " + accumulativePercentage + " > " + slotMachineItem.getPercentage() + " > " + slotMachineItem.getSlotMachineRewardType().name());
                return slotMachineItem;
            }
            accumulativePercentage += itemPercentage;
        }
//        Logger.console("Null: " + winPercentage + " vs " + accumulativePercentage);
        return null;
    }

    public boolean use(Player player) {
        if (CraftventureCore.getInstance().isShuttingDown()) {
            player.sendMessage(Translation.SHUTDOWN_PREPARING.getTranslation(player));
            return true;
        }
        if (this.player == null && !inUse) {
            if (!itemsValid()) {
                player.sendMessage(Translation.CASINO_SLOTMACHINE_BROKEN.getTranslation(player));
                return true;
            }

            if (WheelOfFortune.wheelOfFortune.get(player) != null) {
                player.sendMessage(Component.text("You can't use the slotmachines while taking part in the wheel of fortune", CVTextColor.getServerError()));
                return true;
            }

            inUse = true;
            this.player = player;
            new AsyncResultTask() {
                boolean paid;
                boolean failed = false;

                @Override
                public void doInBackground() {
                    BankAccount bankAccount = MainRepositoryProvider.INSTANCE.getBankAccountRepository().getOrCreate(player.getUniqueId(), BankAccountType.VC);
                    if (!(bankAccount != null && bankAccount.getBalance() >= input)) {
                        player.sendMessage(Translation.CASINO_SLOTMACHINE_NOT_ENOUGH_VENTURECOINS.getTranslation(player, input - bankAccount.getBalance(),
                                BankAccountType.VC.getPluralName(),
                                BankAccountType.VC.getAbbreviation()));
                        failed = true;
                        return;
                    }

                    paid = MainRepositoryProvider.INSTANCE.getBankAccountRepository().delta(player.getUniqueId(), BankAccountType.VC, -input, TransactionType.CASINO_SPEND);
                    MainRepositoryProvider.INSTANCE.getAchievementProgressRepository().reward(player.getUniqueId(), "casino_spend");
                }

                @Override
                public void onPostExecute() {
                    if (failed) {
                        SlotMachine.this.player = null;
                        inUse = false;
                        return;
                    }

                    if (!paid) {
                        player.sendMessage(Translation.CASINO_SLOTMACHINE_FAILED_TO_PAY_MACHINE.getTranslation(player, input));
                        SlotMachine.this.player = null;
                        inUse = false;
                        return;
                    }
                    player.sendMessage(Translation.CASINO_SLOTMACHINE_SPENT.getTranslation(player, input));
                    location.getWorld().playSound(location, SoundUtils.INSTANCE.getMONEY(), 1f, 1f);
                    bukkitTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), new Runnable() {
                        int tick = 0;
                        SlotMachineItem a;
                        SlotMachineItem b;
                        SlotMachineItem c;

                        @Override
                        public void run() {
                            tick++;
                            if (tick > 20 * 3) {
                                SlotMachineItem randomItem = getRandomWinningItem();
                                if (randomItem != null) {
//                                    Logger.console("WINNING ITEM!");
                                    a = b = c = randomItem;
                                } else {
//                                    Logger.console("No winning item");
                                    a = slotMachineItems.get((secureRandom.nextInt(slotMachineItems.size())));
                                    b = slotMachineItems.get((secureRandom.nextInt(slotMachineItems.size())));
                                    c = slotMachineItems.get((secureRandom.nextInt(slotMachineItems.size())));
                                    while (a == b && a == c) {
                                        a = slotMachineItems.get((secureRandom.nextInt(slotMachineItems.size())));
                                        b = slotMachineItems.get((secureRandom.nextInt(slotMachineItems.size())));
                                        c = slotMachineItems.get((secureRandom.nextInt(slotMachineItems.size())));
                                    }
                                }
                                item1.helmet(SlotMachineItemExtensionsKt.getResolvedItem(a));
                                item2.helmet(SlotMachineItemExtensionsKt.getResolvedItem(b));
                                item3.helmet(SlotMachineItemExtensionsKt.getResolvedItem(c));

                                Bukkit.getScheduler().cancelTask(bukkitTaskId);
                                reward(SlotMachine.this.player, a, b, c);
                                SlotMachine.this.player = null;
                                Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), () -> {
//                                    item1.helmet(new ItemStack(Material.GOLDEN_APPLE));
//                                    item2.helmet(new ItemStack(Material.GOLDEN_APPLE));
//                                    item3.helmet(new ItemStack(Material.GOLDEN_APPLE));
                                    inUse = false;
                                }, 20 * 2);
                            } else if (tick % Math.floor(tick / 10f) == 0 || tick < 10) {
                                a = slotMachineItems.get((secureRandom.nextInt(slotMachineItems.size())));
                                b = slotMachineItems.get((secureRandom.nextInt(slotMachineItems.size())));
                                c = slotMachineItems.get((secureRandom.nextInt(slotMachineItems.size())));
                                item1.helmet(SlotMachineItemExtensionsKt.getResolvedItem(a));
                                item2.helmet(SlotMachineItemExtensionsKt.getResolvedItem(b));
                                item3.helmet(SlotMachineItemExtensionsKt.getResolvedItem(c));
                            }
                        }
                    }, 1L, 1L);
                }
            }.executeNow();

            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block != null) {
            Location location = block.getLocation();
            if (this.location.getBlockX() == location.getBlockX() &&
                    this.location.getBlockY() == location.getBlockY() &&
                    this.location.getBlockZ() - 1 == location.getBlockZ()) {
                event.setCancelled(true);
                if (!use(event.getPlayer())) {
                    if (this.player != null) {
                        if (this.player != event.getPlayer()) {
                            event.getPlayer().sendMessage(Translation.CASINO_SLOTMACHINE_MACHINE_IN_USE.getTranslation(event.getPlayer(), this.player.getName()));
                        }
                    } else {
                        event.getPlayer().sendMessage(Translation.CASINO_SLOTMACHINE_WAIT_UNTIL_RESET.getTranslation(event.getPlayer()));
                    }
                }
            }
        }
    }
}
