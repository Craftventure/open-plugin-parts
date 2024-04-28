package net.craftventure.core.feature.casino;

import net.craftventure.bukkit.ktx.MaterialConfig;
import net.craftventure.bukkit.ktx.manager.MessageBarManager;
import net.craftventure.bukkit.ktx.util.ChatUtils;
import net.craftventure.bukkit.ktx.util.SoundUtils;
import net.craftventure.bukkit.ktx.util.Translation;
import net.craftventure.chat.bungee.util.CVTextColor;
import net.craftventure.core.CraftventureCore;
import net.craftventure.core.async.AsyncTask;
import net.craftventure.core.ktx.util.Logger;
import net.craftventure.core.ktx.util.TimeUtils;
import net.craftventure.core.npc.NpcEntity;
import net.craftventure.core.npc.tracker.NpcAreaTracker;
import net.craftventure.core.utils.LocationUtil;
import net.craftventure.database.MainRepositoryProvider;
import net.craftventure.database.generated.cvdata.tables.pojos.BankAccount;
import net.craftventure.database.type.BankAccountType;
import net.craftventure.database.type.TransactionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
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


public class WinterWheelOfFortune implements Listener {
    private static final BankAccountType accountType = BankAccountType.WINTER_TICKETS;
    private static final int MAX_INPUT_AMOUNT = 1;
    private final Location centerLocation;

    private int bukkitTaskId = -1;
    private int countdownTaskId = -1;

    private boolean isCountingDown = false;
    private boolean isPlaying = false;
    private final Wheel wheel;

    private final List<InputSign> inputSigns = new ArrayList<>(4 * 3);

    private final List<FortunePlayer> fortunePlayers = new ArrayList<>();

    public WinterWheelOfFortune(NpcAreaTracker areaTracker) {
        this.centerLocation = new Location(Bukkit.getWorld("world"), 41.15, 41 - 1.45, -605, -90, 0);
        Bukkit.getServer().getPluginManager().registerEvents(this, CraftventureCore.getInstance());
        wheel = new Wheel(centerLocation);
        areaTracker.addEntity(wheel.npcEntity);
        onValuesUpdated();

        inputSigns.add(new InputSign(new Location(Bukkit.getWorld("world"), 45.5, 39, -600.5), InputColor.SNOW, 1));
        inputSigns.add(new InputSign(new Location(Bukkit.getWorld("world"), 43.5, 39, -601.5), InputColor.CYAN, 1));
        inputSigns.add(new InputSign(new Location(Bukkit.getWorld("world"), 43.5, 39, -608.5), InputColor.BLUE, 1));
        inputSigns.add(new InputSign(new Location(Bukkit.getWorld("world"), 45.5, 39, -609.5), InputColor.ICE, 1));

        for (InputSign inputSign : inputSigns) {
            updateSign(inputSign);
        }
    }

    private void updateSign(InputSign inputSign) {
        if (inputSign.getLocation().getBlock().getState() instanceof Sign) {
            inputSign.getLocation().getBlock().setType(Material.SPRUCE_WALL_SIGN);
//            inputSign.getLocation().getBlock().setData((byte) 5);
        }
        if (inputSign.getLocation().getBlock().getState() instanceof Sign) {
            Sign sign = (Sign) inputSign.getLocation().getBlock().getState();
            sign.setLine(0, "");
            sign.line(1, inputSign.getColor().displayName());
            sign.setLine(2, "Right click to bet");
            sign.setLine(3, "");
            sign.update(true);
        }
    }

    private void play() {
        if (!isPlaying) {
            cancelCountdown();
            isPlaying = true;
            wheel.newRandoms();
            bukkitTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), () -> {
                if (!wheel.update()) {
                    Bukkit.getScheduler().cancelTask(bukkitTaskId);
                    int space = (int) Math.floor(wheel.targetAngle % 360 / 90d);
//                            Logger.console("Space " + space);
//                    Logger.console("%s has won (%s)", space, space == 0 ? "purple" : space == 1 ? "yellow" : space == 2 ? "green" : "red");
//                    Logger.console("%s has won (%s)", space, space == 0 ? "blue" : space == 1 ? "ice" : space == 2 ? "snow" : "cyan");
                    final List<FortunePlayer> fortunePlayers = new ArrayList<>(WinterWheelOfFortune.this.fortunePlayers);
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


                                    int input = -(fortunePlayer.blue + fortunePlayer.ice + fortunePlayer.cyan + fortunePlayer.snow);
                                    int win = 0;

                                    if (space == 0 && fortunePlayer.blue > 0) {
                                        win = 50;
                                    } else if (space == 1 && fortunePlayer.ice > 0) {
                                        win = 50;
                                    } else if (space == 2 && fortunePlayer.snow > 0) {
                                        win = 50;
                                    } else if (space == 3 && fortunePlayer.cyan > 0) {
                                        win = 50;
                                    }

                                    MainRepositoryProvider.INSTANCE.getCasinoLogRepository().createOrUpdate(fortunePlayer.player.getUniqueId(), "wof", win);
                                    if (win != 0)
                                        fortunePlayer.player.sendMessage(Component.text(String.format("§eYou won %1$dWC!", win), CVTextColor.getServerNotice()));
                                    else
                                        fortunePlayer.player.sendMessage(Component.text("You won nothing, sorry", CVTextColor.getServerNotice()));


                                    MessageBarManager.remove(fortunePlayer.player, ChatUtils.INSTANCE.getID_CASINO());
                                    MainRepositoryProvider.INSTANCE.getBankAccountRepository().delta(fortunePlayer.player.getUniqueId(), BankAccountType.WINTERCOIN, win, TransactionType.CASINO_WIN);
                                    MainRepositoryProvider.INSTANCE.getBankAccountRepository().delta(fortunePlayer.player.getUniqueId(), BankAccountType.WINTER_TICKETS, input, TransactionType.CASINO_SPEND);

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
                        }
                    }.executeNow();
                    isPlaying = false;
                    WinterWheelOfFortune.this.fortunePlayers.clear();
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
                int secondsLeft = 5;

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

        if (block != null && block.getState() instanceof Sign && (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {

            for (int i = 0; i < inputSigns.size(); i++) {
                InputSign inputSign = inputSigns.get(i);
                if (LocationUtil.INSTANCE.equals(inputSign.getLocation(), block)) {
                    boolean isInput = true;//event.getAction() == Action.LEFT_CLICK_BLOCK;
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
                                    accountType.getPluralName(),
                                    accountType.getPluralName()));
                            if (fortunePlayer.total() == 0)
                                remove(event.getPlayer());
                            return;
                        }

                        if (isInput && fortunePlayer.total() >= 1) {
                            fortunePlayer.player.sendMessage(Component.text("You can only use 1 ticket at a time!", CVTextColor.getServerNotice()));
                            return;
                        }

                        if (!isInput) {
                            fortunePlayer.ice = 0;
                            fortunePlayer.snow = 0;
                            fortunePlayer.cyan = 0;
                            fortunePlayer.blue = 0;
                            fortunePlayer.onValuesUpdated();
                            remove(event.getPlayer());
                            return;
                        }
                    }

                    if (inputSign.getColor() == InputColor.ICE) {
                        if (fortunePlayer != null)
                            if (fortunePlayer.canSetYellow())
                                if (isInput) fortunePlayer.ice += inputSign.getAmount();
                                else fortunePlayer.ice = 0;
                            else
                                fortunePlayer.player.sendMessage(Translation.CASINO_WOF_MAX_INPUT.getTranslation(fortunePlayer.player, MAX_INPUT_AMOUNT));
                    } else if (inputSign.getColor() == InputColor.SNOW) {
                        if (fortunePlayer != null)
                            if (fortunePlayer.canSetGreen())
                                if (isInput) fortunePlayer.snow += inputSign.getAmount();
                                else fortunePlayer.snow = 0;
                            else
                                fortunePlayer.player.sendMessage(Translation.CASINO_WOF_MAX_INPUT.getTranslation(fortunePlayer.player, MAX_INPUT_AMOUNT));
                    } else if (inputSign.getColor() == InputColor.CYAN) {
                        if (fortunePlayer != null)
                            if (fortunePlayer.canSetRed())
                                if (isInput) fortunePlayer.cyan += inputSign.getAmount();
                                else fortunePlayer.cyan = 0;
                            else
                                fortunePlayer.player.sendMessage(Translation.CASINO_WOF_MAX_INPUT.getTranslation(fortunePlayer.player, MAX_INPUT_AMOUNT));
                    } else if (inputSign.getColor() == InputColor.BLUE) {
                        if (fortunePlayer != null)
                            if (fortunePlayer.canSetPurple())
                                if (isInput) fortunePlayer.blue += inputSign.getAmount();
                                else fortunePlayer.blue = 0;
                            else
                                fortunePlayer.player.sendMessage(Translation.CASINO_WOF_MAX_INPUT.getTranslation(fortunePlayer.player, MAX_INPUT_AMOUNT));
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
        private int ice;
        private int snow;
        private int cyan;
        private int blue;

        public FortunePlayer(Player player) {
            this.player = player;
        }

        public int total() {
            return ice + snow + cyan + blue;
        }

        public boolean canSetYellow() {
            int colorsSet = 0;
//            if (yellow > 0) colorsSet++;
            if (snow > 0) colorsSet++;
            if (cyan > 0) colorsSet++;
            if (blue > 0) colorsSet++;
            return colorsSet < MAX_INPUT_AMOUNT;
        }

        public boolean canSetGreen() {
            int colorsSet = 0;
            if (ice > 0) colorsSet++;
//            if (green > 0) colorsSet++;
            if (cyan > 0) colorsSet++;
            if (blue > 0) colorsSet++;
            return colorsSet < MAX_INPUT_AMOUNT;
        }

        public boolean canSetRed() {
            int colorsSet = 0;
            if (ice > 0) colorsSet++;
            if (snow > 0) colorsSet++;
//            if (red > 0) colorsSet++;
            if (blue > 0) colorsSet++;
            return colorsSet < MAX_INPUT_AMOUNT;
        }

        public boolean canSetPurple() {
            int colorsSet = 0;
            if (ice > 0) colorsSet++;
            if (snow > 0) colorsSet++;
            if (cyan > 0) colorsSet++;
//            if (purple > 0) colorsSet++;
            return colorsSet < MAX_INPUT_AMOUNT;
        }

        public boolean canIncreaseWith(int amount) {
            BankAccount bankAccount = MainRepositoryProvider.INSTANCE.getBankAccountRepository().getOrCreate(player.getUniqueId(), accountType);
            if (bankAccount != null) {
                return bankAccount.getBalance() >= ice + snow + cyan + blue + amount;
            }
            return false;
        }

        public void onValuesUpdated() {
            ice = Math.max(0, ice);
            snow = Math.max(0, snow);
            cyan = Math.max(0, cyan);
            blue = Math.max(0, blue);

            if (ice == 0 && snow == 0 && cyan == 0 && blue == 0) {
                remove(player);
                MessageBarManager.remove(player, ChatUtils.INSTANCE.getID_CASINO());
                player.sendMessage(Translation.CASINO_WOF_LEAVE.getTranslation(player));
            } else {
                //Translation.CASINO_WOF_INPUT_WINTER.getTranslation(player, ice, snow, cyan, blue)
                MessageBarManager.display(player,
                        Component.text("Betting on " +
                                (ice > 0 ? "Ice" :
                                        snow > 0 ? "Snow" :
                                                cyan > 0 ? "Cyan" : "Blue"), CVTextColor.getServerNotice()),
                        MessageBarManager.Type.CASINO,
                        TimeUtils.secondsFromNow(10.0),
                        ChatUtils.INSTANCE.getID_CASINO()
                );
            }

            player.playSound(player.getLocation(), SoundUtils.INSTANCE.getR3_CLICK1(), 10f, 1f);
            WinterWheelOfFortune.this.onValuesUpdated();
        }
    }

    private class Wheel {
        private final SecureRandom secureRandom = new SecureRandom();
        private double targetAngle;
        private int spinDuration;
        private final NpcEntity npcEntity;
        private double startAngle;
        private int currentTick;

        public Wheel(Location centerLocation) {
            npcEntity = new NpcEntity("winterWof", EntityType.ARMOR_STAND, centerLocation);
            npcEntity.invisible(true);
//        npcEntity.marker(true);
            npcEntity.helmet(MaterialConfig.INSTANCE.getCASINO_ARROW());
            npcEntity.head(0, 0, 0);
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
//            Logger.console("percentage " + percentage);
            npcEntity.head(0, 0, (float) (percentage));
            return currentTick <= spinDuration;
        }
    }

    private enum InputColor {
        ICE(Component.text("Ice", NamedTextColor.DARK_AQUA, TextDecoration.BOLD)),
        SNOW(Component.text("Snow", NamedTextColor.WHITE, TextDecoration.BOLD)),
        CYAN(Component.text("Cyan", NamedTextColor.AQUA, TextDecoration.BOLD)),
        BLUE(Component.text("Blue", NamedTextColor.DARK_BLUE, TextDecoration.BOLD));

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
        private final InputColor color;
        private final int amount;

        public InputSign(Location location, InputColor color, int amount) {
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

        public InputColor getColor() {
            return color;
        }

        public int getAmount() {
            return amount;
        }
    }
}
