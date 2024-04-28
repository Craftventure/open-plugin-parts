package net.craftventure.core.feature.casino;

import net.craftventure.bukkit.ktx.MaterialConfig;
import net.craftventure.bukkit.ktx.manager.MessageBarManager;
import net.craftventure.bukkit.ktx.util.ChatUtils;
import net.craftventure.bukkit.ktx.util.SoundUtils;
import net.craftventure.bukkit.ktx.util.Translation;
import net.craftventure.core.CraftventureCore;
import net.craftventure.core.async.AsyncTask;
import net.craftventure.core.ktx.util.Logger;
import net.craftventure.core.ktx.util.TimeUtils;
import net.craftventure.core.map.renderer.MapManager;
import net.craftventure.core.npc.NpcEntity;
import net.craftventure.core.npc.tracker.NpcAreaTracker;
import net.craftventure.core.utils.LocationUtil;
import net.craftventure.database.MainRepositoryProvider;
import net.craftventure.database.generated.cvdata.tables.pojos.BankAccount;
import net.craftventure.database.type.BankAccountType;
import net.craftventure.database.type.TransactionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import penner.easing.Quad;

import javax.annotation.Nullable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// TODO: Either unrig this or ban Aron so that it doesnt leak that this machine is 1000% rigged and not just random

public class WheelOfFortune implements Listener {
    private static final int MAX_INPUT_AMOUNT = 2;
    private final Location centerLocation;

    private int bukkitTaskId = -1;
    private int countdownTaskId = -1;

    private boolean isCountingDown = false;
    private boolean isPlaying = false;
    private final Wheel wheel;

    private final List<InputSign> inputSigns = new ArrayList<>(4 * 3);

    private final List<FortunePlayer> fortunePlayers = new ArrayList<>();

    public static WheelOfFortune wheelOfFortune;

    public WheelOfFortune(NpcAreaTracker areaTracker) {
        wheelOfFortune = this;
        this.centerLocation = new Location(Bukkit.getWorld("world"), 139.5, 43.5 - 1.45, -807.5, 90, 0);

        Bukkit.getServer().getPluginManager().registerEvents(this, CraftventureCore.getInstance());
        wheel = new Wheel(centerLocation);
        areaTracker.addEntity(wheel.npcEntity);
        areaTracker.addEntity(wheel.backside);
        onValuesUpdated();

        inputSigns.add(new InputSign(new Location(Bukkit.getWorld("world"), 130.0, 40, -806), InputColor.YELLOW, 10));
        inputSigns.add(new InputSign(new Location(Bukkit.getWorld("world"), 130.0, 40, -807), InputColor.GREEN, 10));
        inputSigns.add(new InputSign(new Location(Bukkit.getWorld("world"), 130.0, 40, -809), InputColor.RED, 10));
        inputSigns.add(new InputSign(new Location(Bukkit.getWorld("world"), 130.0, 40, -810), InputColor.PURPLE, 10));

        inputSigns.add(new InputSign(new Location(Bukkit.getWorld("world"), 130.0, 41, -806), InputColor.YELLOW, 100));
        inputSigns.add(new InputSign(new Location(Bukkit.getWorld("world"), 130.0, 41, -807), InputColor.GREEN, 100));
        inputSigns.add(new InputSign(new Location(Bukkit.getWorld("world"), 130.0, 41, -809), InputColor.RED, 100));
        inputSigns.add(new InputSign(new Location(Bukkit.getWorld("world"), 130.0, 41, -810), InputColor.PURPLE, 100));

        inputSigns.add(new InputSign(new Location(Bukkit.getWorld("world"), 130.0, 42, -806), InputColor.YELLOW, 1000));
        inputSigns.add(new InputSign(new Location(Bukkit.getWorld("world"), 130.0, 42, -807), InputColor.GREEN, 1000));
        inputSigns.add(new InputSign(new Location(Bukkit.getWorld("world"), 130.0, 42, -809), InputColor.RED, 1000));
        inputSigns.add(new InputSign(new Location(Bukkit.getWorld("world"), 130.0, 42, -810), InputColor.PURPLE, 1000));

        inputSigns.add(new InputSign(new Location(Bukkit.getWorld("world"), 130.0, 41, -808), null, 0));

        for (InputSign inputSign : inputSigns) {
            updateSign(inputSign);
        }
    }

    private void updateSign(InputSign inputSign) {
        if (inputSign.getLocation().getBlock().getState() instanceof Sign) {
            BlockData blockData = Material.SPRUCE_WALL_SIGN.createBlockData();
            if (blockData instanceof Directional) {
                Directional directional = (Directional) blockData;
                directional.setFacing(BlockFace.EAST);
            }
            inputSign.getLocation().getBlock().setBlockData(blockData);
//            inputSign.getLocation().getBlock().setData((byte) 5);
        }
        if (inputSign.getLocation().getBlock().getState() instanceof Sign) {
            Sign sign = (Sign) inputSign.getLocation().getBlock().getState();
            sign.line(0, Component.empty());
            sign.line(1, inputSign.getColor().displayName());
            if (inputSign.getAmount() == 0)
                sign.line(2, Component.text("Reset this color", NamedTextColor.DARK_RED));
            else
                sign.line(2, Component.text(inputSign.getAmount() + "VC", NamedTextColor.DARK_GREEN));
            sign.line(3, Component.empty());
//            sign.setLine(3, "§4Right click -" + inputSign.getAmount() + "VC");
            sign.update(true);
        }
    }

    public void play() {
        if (!isPlaying) {
            cancelCountdown();
            isPlaying = true;
            wheel.newRandoms();
            bukkitTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), () -> {
                if (!wheel.update()) {
                    Bukkit.getScheduler().cancelTask(bukkitTaskId);
                    int space = (int) Math.floor(wheel.targetAngle % 360 / 90d);
//                            Logger.console("Space " + space);
                    final List<FortunePlayer> fortunePlayers = new ArrayList<>(WheelOfFortune.this.fortunePlayers);
                    new AsyncTask() {
                        final List<Player> players = new ArrayList<>();

                        @Override
                        public void doInBackground() {
                            try {
                                for (FortunePlayer fortunePlayer : fortunePlayers) {
                                    fortunePlayer.player.sendMessage(Translation.CASINO_WOF_PROCESSED.getTranslation(fortunePlayer.player));
                                }
                                for (FortunePlayer fortunePlayer : fortunePlayers) {
                                    fortunePlayer.player.playSound(fortunePlayer.player.getLocation(), SoundUtils.INSTANCE.getMONEY(), 10f, 1f);


                                    int input = -(fortunePlayer.purple + fortunePlayer.yellow + fortunePlayer.red + fortunePlayer.green);
                                    int win = 0;

                                    if (space == 0 && fortunePlayer.purple > 0) {
                                        win += (fortunePlayer.purple * 4);
                                    } else if (space == 1 && fortunePlayer.yellow > 0) {
                                        win += (fortunePlayer.yellow * 4);
                                    } else if (space == 2 && fortunePlayer.green > 0) {
                                        win += (fortunePlayer.green * 4);
                                    } else if (space == 3 && fortunePlayer.red > 0) {
                                        win += (fortunePlayer.red * 4);
                                    }

                                    MainRepositoryProvider.INSTANCE.getCasinoLogRepository().createOrUpdate(fortunePlayer.player.getUniqueId(), "wof", win);
                                    if (win != 0)
                                        fortunePlayer.player.sendMessage(Translation.CASINO_WOF_WON.getTranslation(fortunePlayer.player, win));
                                    else
                                        fortunePlayer.player.sendMessage(Translation.CASINO_WOF_LOST.getTranslation(fortunePlayer.player));


                                    MessageBarManager.remove(fortunePlayer.player, ChatUtils.INSTANCE.getID_CASINO());
                                    MainRepositoryProvider.INSTANCE.getBankAccountRepository().delta(fortunePlayer.player.getUniqueId(), BankAccountType.VC, win, TransactionType.CASINO_WIN);
                                    MainRepositoryProvider.INSTANCE.getBankAccountRepository().delta(fortunePlayer.player.getUniqueId(), BankAccountType.VC, input, TransactionType.CASINO_SPEND);

//                                        Logger.console("%s won %s (spent %s)", fortunePlayer.player.getName(), win, input);

                                    players.add(fortunePlayer.player);
                                }
                                for (FortunePlayer fortunePlayer : fortunePlayers) {
                                    if (fortunePlayer.player != null)
                                        MainRepositoryProvider.INSTANCE.getAchievementProgressRepository().reward(fortunePlayer.player.getUniqueId(), "casino_spend");
                                    fortunePlayer.player = null;
                                }
                            } catch (Exception e) {
                                Logger.capture(e);
                            }
                            MapManager.getInstance().invalidateForCasinoMachine("wof");
                        }
                    }.executeNow();
                    isPlaying = false;
                    WheelOfFortune.this.fortunePlayers.clear();
                }
            }, 1L, 1L);
        }
    }

    private void cancelCountdown() {
        if (isCountingDown) {
            Bukkit.getScheduler().cancelTask(countdownTaskId);
            isCountingDown = false;
        }
    }

    private void triggerCountdown() {
        if (!isCountingDown) {
            isCountingDown = true;
            countdownTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), new Runnable() {
                int secondsLeft = 30;

                @Override
                public void run() {
                    if (secondsLeft <= 0) {
                        play();
                        return;
                    }
                    if (secondsLeft < 5 || secondsLeft % 5 == 0) {
                        for (FortunePlayer fortunePlayer : fortunePlayers) {
                            fortunePlayer.player.sendMessage(Translation.CASINO_WOF_SPINNING.getTranslation(fortunePlayer.player, secondsLeft));
                        }
                    }
                    secondsLeft--;
                }
            }, 0L, 20L);
        }
    }

    @Nullable
    public FortunePlayer get(Player player) {
        for (FortunePlayer fortunePlayer : fortunePlayers) {
            if (fortunePlayer.player == player) {
                return fortunePlayer;
            }
        }
        return null;
    }

    public FortunePlayer getOrCreate(Player player) {
        FortunePlayer fortunePlayer = get(player);
        if (fortunePlayer == null) {
            fortunePlayer = new FortunePlayer(player);
            fortunePlayers.add(fortunePlayer);
            player.sendMessage(Translation.CASINO_WOF_JOIN.getTranslation(player));
            triggerCountdown();
        }
        return fortunePlayer;
    }

    public void remove(Player player) {
        for (Iterator<FortunePlayer> fortunePlayerIterator = fortunePlayers.iterator(); fortunePlayerIterator.hasNext(); ) {
            FortunePlayer fortunePlayer = fortunePlayerIterator.next();
            if (fortunePlayer.player == player) {
                fortunePlayerIterator.remove();
                if (fortunePlayers.size() == 0) {
                    cancelCountdown();
                }
                onValuesUpdated();
                return;
            }
        }
    }

    public void onValuesUpdated() {
//        int yellow = 0;
//        int green = 0;
//        int red = 0;
//        int purple = 0;
//
//        for (FortunePlayer fortunePlayer : fortunePlayers) {
//            yellow += fortunePlayer.yellow;
//            green += fortunePlayer.green;
//            red += fortunePlayer.red;
//            purple += fortunePlayer.purple;
//        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block != null) {
            for (int i = 0; i < inputSigns.size(); i++) {
                InputSign inputSign = inputSigns.get(i);
                if (LocationUtil.INSTANCE.equals(inputSign.getLocation(), block) ||
                        LocationUtil.INSTANCE.equals(inputSign.getBlockLocation(), block)) {
                    event.setCancelled(true);
                }
            }
        }
        if (isPlaying) {
            return;
        }

        if (block != null && (block.getState() instanceof Sign || block.getType().data.isAssignableFrom(Switch.class)) && (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {

            for (int i = 0; i < inputSigns.size(); i++) {
                InputSign inputSign = inputSigns.get(i);
                if (LocationUtil.INSTANCE.equals(inputSign.getLocation(), block)) {
                    boolean isInput = inputSign.getAmount() > 0;
                    FortunePlayer fortunePlayer = get(event.getPlayer());

                    if (CraftventureCore.getInstance().isShuttingDown() && fortunePlayer == null) {
                        event.getPlayer().sendMessage(Translation.SHUTDOWN_PREPARING.getTranslation(event.getPlayer()));
                        return;
                    }
                    if (isInput)
                        fortunePlayer = getOrCreate(event.getPlayer());

                    if (fortunePlayer != null) {
                        if (!fortunePlayer.canIncreaseWith(inputSign.getAmount()) && isInput) {
                            fortunePlayer.player.sendMessage(Translation.CASINO_WOF_NOT_ENOUGH_VENTURECOINS.getTranslation(fortunePlayer.player,
                                    BankAccountType.VC.getPluralName(),
                                    BankAccountType.VC.getPluralName()));
                            if (fortunePlayer.total() == 0)
                                remove(event.getPlayer());
                            return;
                        }
                    }

                    if (inputSign.getColor() == InputColor.YELLOW) {
                        if (fortunePlayer != null)
                            if (fortunePlayer.canSetYellow())
                                if (isInput) fortunePlayer.yellow += inputSign.getAmount();
                                else fortunePlayer.yellow = 0;
                            else
                                fortunePlayer.player.sendMessage(Translation.CASINO_WOF_MAX_INPUT.getTranslation(fortunePlayer.player, MAX_INPUT_AMOUNT));
                    } else if (inputSign.getColor() == InputColor.GREEN) {
                        if (fortunePlayer != null)
                            if (fortunePlayer.canSetGreen())
                                if (isInput) fortunePlayer.green += inputSign.getAmount();
                                else fortunePlayer.green = 0;
                            else
                                fortunePlayer.player.sendMessage(Translation.CASINO_WOF_MAX_INPUT.getTranslation(fortunePlayer.player, MAX_INPUT_AMOUNT));
                    } else if (inputSign.getColor() == InputColor.RED) {
                        if (fortunePlayer != null)
                            if (fortunePlayer.canSetRed())
                                if (isInput) fortunePlayer.red += inputSign.getAmount();
                                else fortunePlayer.red = 0;
                            else
                                fortunePlayer.player.sendMessage(Translation.CASINO_WOF_MAX_INPUT.getTranslation(fortunePlayer.player, MAX_INPUT_AMOUNT));
                    } else if (inputSign.getColor() == InputColor.PURPLE) {
                        if (fortunePlayer != null)
                            if (fortunePlayer.canSetPurple())
                                if (isInput) fortunePlayer.purple += inputSign.getAmount();
                                else fortunePlayer.purple = 0;
                            else
                                fortunePlayer.player.sendMessage(Translation.CASINO_WOF_MAX_INPUT.getTranslation(fortunePlayer.player, MAX_INPUT_AMOUNT));
                    } else if (inputSign.color == null) {
                        if (fortunePlayer != null) {
                            fortunePlayer.yellow = 0;
                            fortunePlayer.green = 0;
                            fortunePlayer.purple = 0;
                            fortunePlayer.red = 0;
                        }
                    }

                    event.setCancelled(true);
                    if (fortunePlayer != null)
                        fortunePlayer.onValuesUpdated();

                    return;
                }
            }
        }
    }

//    @EventHandler
//    public void onPlayerQuit(PlayerQuitEvent event) {
//        remove(event.getPlayer());
//    }

    private class FortunePlayer {
        private Player player;
        private int yellow;
        private int green;
        private int red;
        private int purple;

        public FortunePlayer(Player player) {
            this.player = player;
        }

        public int total() {
            return yellow + green + red + purple;
        }

        public boolean canSetYellow() {
            int colorsSet = 0;
//            if (yellow > 0) colorsSet++;
            if (green > 0) colorsSet++;
            if (red > 0) colorsSet++;
            if (purple > 0) colorsSet++;
            return colorsSet < MAX_INPUT_AMOUNT;
        }

        public boolean canSetGreen() {
            int colorsSet = 0;
            if (yellow > 0) colorsSet++;
//            if (green > 0) colorsSet++;
            if (red > 0) colorsSet++;
            if (purple > 0) colorsSet++;
            return colorsSet < MAX_INPUT_AMOUNT;
        }

        public boolean canSetRed() {
            int colorsSet = 0;
            if (yellow > 0) colorsSet++;
            if (green > 0) colorsSet++;
//            if (red > 0) colorsSet++;
            if (purple > 0) colorsSet++;
            return colorsSet < MAX_INPUT_AMOUNT;
        }

        public boolean canSetPurple() {
            int colorsSet = 0;
            if (yellow > 0) colorsSet++;
            if (green > 0) colorsSet++;
            if (red > 0) colorsSet++;
//            if (purple > 0) colorsSet++;
            return colorsSet < MAX_INPUT_AMOUNT;
        }

        public boolean canIncreaseWith(int amount) {
            BankAccount bankAccount = MainRepositoryProvider.INSTANCE.getBankAccountRepository().getOrCreate(player.getUniqueId(), BankAccountType.VC);
            if (bankAccount != null) {
                return bankAccount.getBalance() >= yellow + green + red + purple + amount;
            }
            return false;
        }

        public void onValuesUpdated() {
            yellow = Math.max(0, yellow);
            green = Math.max(0, green);
            red = Math.max(0, red);
            purple = Math.max(0, purple);

            if (yellow == 0 && green == 0 && red == 0 && purple == 0) {
                remove(player);
                MessageBarManager.remove(player, ChatUtils.INSTANCE.getID_CASINO());
                player.sendMessage(Translation.CASINO_WOF_LEAVE.getTranslation(player));
            } else {
                MessageBarManager.display(player,
                        Translation.CASINO_WOF_INPUT.getTranslation(player, yellow, green, red, purple),
                        MessageBarManager.Type.CASINO,
                        TimeUtils.secondsFromNow(40.0),
                        ChatUtils.INSTANCE.getID_CASINO()
                );
            }

            player.playSound(player.getLocation(), SoundUtils.INSTANCE.getR3_CLICK1(), 10f, 1f);
            WheelOfFortune.this.onValuesUpdated();
        }
    }

    private class Wheel {
        private final SecureRandom secureRandom = new SecureRandom();
        private double targetAngle;
        private int spinDuration;
        private final NpcEntity npcEntity;
        private final NpcEntity backside;
        private double startAngle;
        private int currentTick;
        private int lastSpot = 0;

        public Wheel(Location centerLocation) {
            npcEntity = new NpcEntity("wof", EntityType.ARMOR_STAND, centerLocation);
            npcEntity.invisible(true);
//        npcEntity.marker(true);
            npcEntity.helmet(MaterialConfig.INSTANCE.getCASINO_ARROW());
            npcEntity.head(0, 0, 0);
            for (int i = 0; i < 10; i++)
                secureRandom.nextDouble();

            backside = new NpcEntity("wof", EntityType.ARMOR_STAND, centerLocation);
            backside.invisible(true);
//        npcEntity.marker(true);
            backside.helmet(MaterialConfig.INSTANCE.getCASINO_WOF());
            backside.head(0, 0, 0);
        }

        void newRandoms() {
            targetAngle = 0;
            while (targetAngle % 90 < 2 || targetAngle % 90 > 88) {
                targetAngle = (secureRandom.nextDouble() * 360) + ((secureRandom.nextInt(2) + 3) * 360);
            }
            spinDuration = secureRandom.nextInt(20 * 5) + (5 * 20);
            currentTick = 0;
            startAngle = startAngle % 360;
        }

        boolean update() {
            currentTick++;
            double percentage = Quad.easeOut(currentTick, (float) startAngle, (float) targetAngle, spinDuration);
            npcEntity.head(0, 0, (float) (percentage));
            if (currentTick > spinDuration) {
                return false;
            }
            int newSpot = (int) Math.floor((percentage % 360) / 90.0);
//            Logger.info(percentage + " > " + newSpot);
            if (newSpot != lastSpot) {
                playTickSound();
                lastSpot = newSpot;
            }
            return true;
        }

        private void playTickSound() {
            centerLocation.getWorld().playSound(centerLocation, Sound.BLOCK_DISPENSER_FAIL, 1f, 1f);
        }
    }

    public enum InputColor {
        PURPLE(Component.text("Purple", NamedTextColor.BLUE, net.kyori.adventure.text.format.TextDecoration.BOLD)),
        YELLOW(Component.text("Yellow", NamedTextColor.GOLD, net.kyori.adventure.text.format.TextDecoration.BOLD)),
        GREEN(Component.text("Green", NamedTextColor.GREEN, net.kyori.adventure.text.format.TextDecoration.BOLD)),
        RED(Component.text("Red", NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD));

        private final Component displayName;

        InputColor(Component displayName) {
            this.displayName = displayName;
        }

        public Component displayName() {
            return displayName;
        }
    }

    public static class InputSign {
        private final Location location;
        private final Location blockLocation;
        @Nullable
        private final InputColor color;
        private final int amount;

        public InputSign(Location location, @Nullable InputColor color, int amount) {
            this.location = location;
            this.color = color;
            this.amount = amount;
            this.blockLocation = this.location.clone().add(-1, 0, 0);
        }

        public Location getLocation() {
            return location;
        }

        public Location getBlockLocation() {
            return blockLocation;
        }

        public @Nullable InputColor getColor() {
            return color;
        }

        public int getAmount() {
            return amount;
        }
    }
}
