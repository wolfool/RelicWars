package com.wolfool.relicwars.command;

import com.wolfool.relicwars.RelicWars;
import com.wolfool.relicwars.relic.RelicDefinition;
import com.wolfool.relicwars.relic.RelicItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RelicCommand implements CommandExecutor, TabCompleter {

    private final RelicWars plugin;
    private final Map<UUID, BukkitTask> transferTasks = new HashMap<>();

    public RelicCommand(RelicWars plugin) {
        this.plugin = plugin;
    }

    private void sendHelpMessage(CommandSender sender, String command, String description) {
        if (sender instanceof Player player) {
            player.sendMessage(Component.text("§e" + command + " - " + description)
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand(command.split(" ")[0] + " " + command.split(" ")[1] + " "))
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("§a클릭하여 명령어 입력"))));
        } else {
            sender.sendMessage("§e" + command + " - " + description);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6§l========== [ RelicWars 명령어 ] ==========");
            if (sender.hasPermission("relicwars.admin")) {
                sendHelpMessage(sender, "/relic give <유저> <0~30>", "유물 지급");
                sendHelpMessage(sender, "/relic take <유저>", "유물 압수");
                sendHelpMessage(sender, "/relic revive <유저>", "다운된 유저 부활");
                sendHelpMessage(sender, "/relic resetcd <유저>", "유물 쿨타임 초기화");
                sendHelpMessage(sender, "/relic announce <메시지>", "전체 공지");
                sendHelpMessage(sender, "/relic checkowner <유물번호|all>", "유물 소유자 확인 (관리자용)");
                sendHelpMessage(sender, "/relic spawnsealed <유물번호>", "테스트용: 유물을 봉인된 상태로 바닥에 소환");
                sendHelpMessage(sender, "/relic resetstate <유물번호|all>", "유물을 강제로 DB에서 초기화 (미스폰 상태)");
                sendHelpMessage(sender, "/relic altar <start|stop|status>", "엔딩 제단 이벤트 관리");
            }
            sendHelpMessage(sender, "/relic transfer <팀원>", "유물 양도 (5초 대기 필요)");
            sender.sendMessage("§6§l=========================================");
            return true;
        }

        if (args[0].equalsIgnoreCase("give") && sender.hasPermission("relicwars.admin")) {
            if (args.length < 3) return false;
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§c대상을 찾을 수 없습니다.");
                return true;
            }

            // "all" 처리: 모든 유물 일괄 지급
            if (args[2].equalsIgnoreCase("all")) {
                int given = 0;
                for (int i = 1; i <= 30; i++) {
                    RelicDefinition def = RelicDefinition.getByNumber(i);
                    if (def == null) continue;
                    String currentState = plugin.getDatabaseManager().getRelicState(i);
                    if (!currentState.equals("unspawned")) continue;
                    ItemStack relic = com.wolfool.relicwars.relic.RelicItemUtil.createRelicItem(def);
                    java.util.Map<Integer, ItemStack> overflow = target.getInventory().addItem(relic);
                    overflow.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
                    plugin.getDatabaseManager().updateRelicState(i, "held", target.getUniqueId().toString(), target.getLocation());
                    given++;
                }
                sender.sendMessage("§a[RelicWars] " + target.getName() + "님에게 유물 " + given + "개를 일괄 지급했습니다.");
                return true;
            }

            try {
                int number = Integer.parseInt(args[2]);
                RelicDefinition def = RelicDefinition.getByNumber(number);
                if (def == null) {
                    sender.sendMessage("§c존재하지 않는 유물 번호입니다. (1~30 또는 all)");
                    return true;
                }

                String currentState = plugin.getDatabaseManager().getRelicState(number);
                if (!currentState.equals("unspawned")) {
                    sender.sendMessage("§c[RelicWars] 해당 번호의 유물은 이미 세상에 존재합니다! (상태: " + currentState + ")");
                    return true;
                }

                ItemStack relic = com.wolfool.relicwars.relic.RelicItemUtil.createRelicItem(def);
                java.util.Map<Integer, ItemStack> overflow = target.getInventory().addItem(relic);
                overflow.values().forEach(i -> target.getWorld().dropItemNaturally(target.getLocation(), i));
                plugin.getDatabaseManager().updateRelicState(number, "held", target.getUniqueId().toString(), target.getLocation());
                sender.sendMessage("§a[RelicWars] " + target.getName() + "님에게 " + def.getName() + " 유물을 지급했습니다.");
            } catch (NumberFormatException e) {
                sender.sendMessage("§c숫자 또는 'all'을 입력하세요.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("take") && sender.hasPermission("relicwars.admin")) {
            if (args.length < 2) return false;
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§c대상을 찾을 수 없습니다.");
                return true;
            }
            
            int taken = 0;
            ItemStack[] contents = target.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(contents[i])) {
                    int num = com.wolfool.relicwars.relic.RelicItemUtil.getRelicNumber(contents[i]);
                    plugin.getDatabaseManager().updateRelicState(num, "unspawned", null, null);
                    contents[i] = null;
                    taken++;
                }
            }
            target.getInventory().setContents(contents);
            sender.sendMessage("§a[RelicWars] " + target.getName() + "님에게서 " + taken + "개의 유물을 압수했습니다.");
            return true;
        }

        if (args[0].equalsIgnoreCase("revive") && sender.hasPermission("relicwars.admin")) {
            if (args.length < 2) return false;
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§c대상을 찾을 수 없습니다.");
                return true;
            }

            if (!plugin.getCombatManager().isDowned(target)) {
                sender.sendMessage("§c" + target.getName() + "님은 다운된 상태가 아닙니다.");
                return true;
            }

            plugin.getCombatManager().revivePlayer(target);
            sender.sendMessage("§a[RelicWars] " + target.getName() + "님을 강제로 부활시켰습니다.");
            target.sendMessage("§a[RelicWars] 관리자의 권능으로 부활했습니다!");
            return true;
        }

        if (args[0].equalsIgnoreCase("resetcd") && sender.hasPermission("relicwars.admin")) {
            if (args.length < 2) return false;
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§c대상을 찾을 수 없습니다.");
                return true;
            }

            int resetCount = 0;
            ItemStack[] contents = target.getInventory().getContents();
            for (ItemStack item : contents) {
                if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(item) && com.wolfool.relicwars.relic.RelicItemUtil.isOnCooldown(item)) {
                    com.wolfool.relicwars.relic.RelicItemUtil.resetCooldown(item);
                    resetCount++;
                }
            }
            sender.sendMessage("§a[RelicWars] " + target.getName() + "님의 유물 " + resetCount + "개의 쿨타임을 초기화했습니다.");
            return true;
        }

        if ((args[0].equalsIgnoreCase("announce") || args[0].equalsIgnoreCase("broadcast")) && sender.hasPermission("relicwars.admin")) {
            if (args.length < 2) {
                sender.sendMessage("§c사용법: /relic announce <메시지>");
                return true;
            }
            String msg = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            Bukkit.broadcast(Component.text("§b[소문] §f" + msg));
            return true;
        }

        if (args[0].equalsIgnoreCase("checkowner") && sender.hasPermission("relicwars.admin")) {
            if (args.length < 2) {
                sender.sendMessage("§c사용법: /relic checkowner <유물번호|all>");
                return true;
            }

            // === all: 모든 유물 상태 일괄 출력 ===
            if (args[1].equalsIgnoreCase("all")) {
                sender.sendMessage("§6§l========== 유물 소유 현황 ==========");
                for (int i = 0; i <= 30; i++) {
                    RelicDefinition def = RelicDefinition.getByNumber(i);
                    if (def == null) continue;
                    String state = plugin.getDatabaseManager().getRelicState(i);
                    String ownerUuid = plugin.getDatabaseManager().getRelicOwner(i);

                    String stateStr;
                    if ("unspawned".equals(state)) {
                        stateStr = "§8[미출현]";
                    } else if ("sealed".equals(state)) {
                        stateStr = "§c[봉인 중]";
                    } else if (ownerUuid != null) {
                        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(ownerUuid));
                        stateStr = "§a[소유] §f" + (op.getName() != null ? op.getName() : ownerUuid);
                    } else {
                        stateStr = "§e[" + state + "]";
                    }
                    sender.sendMessage(def.getTierColor() + String.format("#%03d ", i) + def.getName() + " §7→ " + stateStr);
                }
                sender.sendMessage("§6§l====================================");
                return true;
            }

            try {
                int targetNum = Integer.parseInt(args[1]);
                RelicDefinition def = RelicDefinition.getByNumber(targetNum);
                if (def == null) {
                    sender.sendMessage("§c[RelicWars] 존재하지 않는 유물 번호입니다. (0~30)");
                    return true;
                }
                String ownerUuid = plugin.getDatabaseManager().getRelicOwner(targetNum);
                if (ownerUuid == null) {
                    sender.sendMessage("§a[관리자 정보] " + def.getName() + " 유물은 아직 누구의 소유도 아닙니다.");
                } else {
                    org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(UUID.fromString(ownerUuid));
                    sender.sendMessage("§a[관리자 정보] " + def.getName() + " 유물의 소유자: " + offlineTarget.getName());
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§c[RelicWars] 유물 번호는 숫자여야 합니다.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("spawnsealed") && sender.hasPermission("relicwars.admin")) {
            if (!(sender instanceof Player player)) return true;
            if (args.length < 2) {
                sender.sendMessage("§c사용법: /relic spawnsealed <유물번호>");
                return true;
            }
            try {
                int number = Integer.parseInt(args[1]);
                RelicDefinition def = RelicDefinition.getByNumber(number);
                if (def == null) {
                    sender.sendMessage("§c존재하지 않는 유물 번호입니다.");
                    return true;
                }
                
                String currentState = plugin.getDatabaseManager().getRelicState(number);
                if (!currentState.equals("unspawned")) {
                    sender.sendMessage("§c[RelicWars] 해당 번호의 유물은 이미 세상에 존재합니다! (상태: " + currentState + ")");
                    return true;
                }

                ItemStack relic = com.wolfool.relicwars.relic.RelicItemUtil.createRelicItem(def);
                plugin.getSealedRelicManager().spawnSealedRelic(player.getLocation(), relic, 5); // 5초 테스트 봉인
                sender.sendMessage("§a[RelicWars] " + def.getName() + " 유물을 바닥에 봉인된 상태로 소환했습니다.");
            } catch (NumberFormatException e) {
                sender.sendMessage("§c숫자를 입력하세요.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("resetstate") && sender.hasPermission("relicwars.admin")) {
            if (args.length < 2) {
                sender.sendMessage("§c사용법: /relic resetstate <유물번호|all>");
                return true;
            }

            // "all" 처리: 모든 유물 초기화
            if (args[1].equalsIgnoreCase("all")) {
                int count = 0;
                for (int i = 0; i <= 30; i++) {
                    RelicDefinition def = RelicDefinition.getByNumber(i);
                    if (def != null) {
                        plugin.getDatabaseManager().updateRelicState(i, "unspawned", null, null);
                        count++;
                    }
                }
                sender.sendMessage("§a[RelicWars] 총 " + count + "개 유물의 DB 상태를 모두 초기화(unspawned)했습니다.");
                return true;
            }

            try {
                int number = Integer.parseInt(args[1]);
                RelicDefinition def = RelicDefinition.getByNumber(number);
                if (def == null) {
                    sender.sendMessage("§c존재하지 않는 유물 번호입니다.");
                    return true;
                }
                
                plugin.getDatabaseManager().updateRelicState(number, "unspawned", null, null);
                sender.sendMessage("§a[RelicWars] " + number + "번 유물의 DB 상태를 초기화(unspawned)했습니다. 이제 다시 스폰/기믹 달성이 가능합니다.");
            } catch (NumberFormatException e) {
                sender.sendMessage("§c숫자 또는 'all'을 입력하세요.");
            }
            return true;
        }

        // ======================== 엔딩 제단 관리 ========================
        if (args[0].equalsIgnoreCase("altar") && sender.hasPermission("relicwars.admin")) {
            if (args.length < 2) {
                sender.sendMessage("§c사용법: /relic altar <start|stop|status>");
                return true;
            }
            var endingManager = plugin.getEndingManager();
            switch (args[1].toLowerCase()) {
                case "start" -> {
                    if (endingManager.isCaptureActive()) {
                        sender.sendMessage("§c[엔딩] 이미 점령전이 진행 중입니다.");
                    } else {
                        endingManager.forceStart();
                        sender.sendMessage("§a[엔딩] 엔딩 제단 점령전을 강제 시작합니다.");
                    }
                }
                case "stop" -> {
                    if (!endingManager.isCaptureActive() && !endingManager.isEndingTriggered()) {
                        sender.sendMessage("§c[엔딩] 현재 진행 중인 점령전이 없습니다.");
                    } else {
                        endingManager.resetEnding();
                        Bukkit.broadcast(Component.text("§c[엔딩] 관리자에 의해 엔딩 제단 이벤트가 중단되었습니다."));
                        sender.sendMessage("§a[엔딩] 엔딩 제단 점령전을 중단하고 초기화했습니다.");
                    }
                }
                case "status" -> {
                    sender.sendMessage("§e======== [엔딩 제단 상태] ========");
                    sender.sendMessage("§7점령전 활성: " + (endingManager.isCaptureActive() ? "§a예" : "§c아니오"));
                    sender.sendMessage("§7엔딩 트리거: " + (endingManager.isEndingTriggered() ? "§a예" : "§c아니오"));
                    if (endingManager.isCaptureActive()) {
                        int percent = Math.round(endingManager.getCaptureProgress() * 100);
                        sender.sendMessage("§7점령 게이지: §e" + percent + "%");
                        org.bukkit.Location loc = endingManager.getAltarLocation();
                        if (loc != null) {
                            sender.sendMessage("§7제단 좌표: §f" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
                        }
                    }
                    sender.sendMessage("§e================================");
                }
                default -> sender.sendMessage("§c사용법: /relic altar <start|stop|status>");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("transfer")) {
            if (!(sender instanceof Player player)) return true;
            if (args.length < 2) {
                player.sendMessage("§c[RelicWars] /relic transfer <팀원>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                player.sendMessage("§c[RelicWars] 대상을 찾을 수 없습니다.");
                return true;
            }

            if (!plugin.getTeamManager().isSameTeam(player, target)) {
                player.sendMessage("§c[RelicWars] 같은 팀원에게만 양도할 수 있습니다.");
                return true;
            }

            startTransferTask(player, target);
            return true;
        }

        return false;
    }

    private void startTransferTask(Player sender, Player target) {
        if (transferTasks.containsKey(sender.getUniqueId())) {
            sender.sendMessage("§c[RelicWars] 이미 양도를 진행 중입니다.");
            return;
        }

        sender.sendMessage("§a[RelicWars] 양도를 시작합니다! (5초간 제자리에 가만히 서 계세요.)");
        Location startLoc = sender.getLocation().clone();

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!sender.isOnline() || !target.isOnline() || sender.getLocation().distanceSquared(startLoc) > 1.0) {
                    sender.sendMessage("§c[RelicWars] 양도 취소: 움직였거나 접속이 끊겼습니다.");
                    transferTasks.remove(sender.getUniqueId()).cancel();
                    return;
                }

                ticks += 5;
                if (ticks % 20 == 0) {
                    sender.sendActionBar(net.kyori.adventure.text.Component.text("§e양도 중... " + (ticks/20) + " / 5초"));
                }

                if (ticks >= 100) { // 5초
                    transferTasks.remove(sender.getUniqueId()).cancel();
                    executeTransfer(sender, target);
                }
            }
        }, 0L, 5L);

        transferTasks.put(sender.getUniqueId(), task);
    }

    private void executeTransfer(Player sender, Player target) {
        // 손에 들고 있는 유물만 넘기기 (기획상 전체 넘기기보다 손에 든 것만 넘기는 게 더 직관적)
        ItemStack hand = sender.getInventory().getItemInMainHand();
        if (!com.wolfool.relicwars.relic.RelicItemUtil.isRelic(hand)) {
            sender.sendMessage("§c[RelicWars] 손에 유물을 들고 있어야 합니다.");
            return;
        }

        sender.getInventory().setItemInMainHand(null);
        java.util.Map<Integer, ItemStack> overflow = target.getInventory().addItem(hand);
        overflow.values().forEach(i -> target.getWorld().dropItemNaturally(target.getLocation(), i));
        
        int relicNum = com.wolfool.relicwars.relic.RelicItemUtil.getRelicNumber(hand);
        plugin.getDatabaseManager().updateRelicState(relicNum, "held", target.getUniqueId().toString(), target.getLocation());

        sender.sendMessage("§a[RelicWars] 유물을 " + target.getName() + "님에게 성공적으로 넘겼습니다!");
        target.sendMessage("§a[RelicWars] " + sender.getName() + "님으로부터 유물을 받았습니다!");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("relicwars.admin")) {
                completions.add("give");
                completions.add("take");
                completions.add("revive");
                completions.add("resetcd");
                completions.add("announce");
                completions.add("checkowner");
                completions.add("spawnsealed");
                completions.add("resetstate");
                completions.add("altar");
            }
            completions.add("transfer");
        } else if (args.length == 2) {
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("relicwars.admin")) {
            completions.add("all");
            for (int i = 1; i <= 30; i++) {
                completions.add(String.valueOf(i));
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("altar") && sender.hasPermission("relicwars.admin")) {
            completions.add("start");
            completions.add("stop");
            completions.add("status");
        }
        return completions;
    }
}
