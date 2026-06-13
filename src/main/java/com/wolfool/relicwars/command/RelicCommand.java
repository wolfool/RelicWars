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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender.hasPermission("relicwars.admin")) {
                sender.sendMessage("§e/relic give <유저> <1~30> - 유물 지급");
                sender.sendMessage("§e/relic take <유저> - 유물 압수");
                sender.sendMessage("§e/relic revive <유저> - 다운된 유저 부활");
                sender.sendMessage("§e/relic resetcd <유저> - 유물 쿨타임 초기화");
            }
            sender.sendMessage("§e/relic transfer <팀원> - 유물 양도 (5초 대기 필요)");
            sender.sendMessage("§e/relic scan <유물번호> - 유물 소유자 확인 (#020 소유자 전용)");
            return true;
        }

        if (args[0].equalsIgnoreCase("give") && sender.hasPermission("relicwars.admin")) {
            if (args.length < 3) return false;
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§c대상을 찾을 수 없습니다.");
                return true;
            }

            try {
                int number = Integer.parseInt(args[2]);
                RelicDefinition def = RelicDefinition.getByNumber(number);
                if (def == null) {
                    sender.sendMessage("§c존재하지 않는 유물 번호입니다. (1~30)");
                    return true;
                }

                ItemStack relic = RelicItemUtil.createRelicItem(def);
                target.getInventory().addItem(relic);
                plugin.getDatabaseManager().updateRelicState(number, "held", target.getUniqueId().toString(), target.getLocation());
                sender.sendMessage("§a[RelicWars] " + target.getName() + "님에게 " + def.getName() + " 유물을 지급했습니다.");
            } catch (NumberFormatException e) {
                sender.sendMessage("§c숫자를 입력하세요.");
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
                if (RelicItemUtil.isRelic(contents[i])) {
                    int num = RelicItemUtil.getRelicNumber(contents[i]);
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
                if (RelicItemUtil.isRelic(item) && RelicItemUtil.isOnCooldown(item)) {
                    RelicItemUtil.resetCooldown(item);
                    resetCount++;
                }
            }
            sender.sendMessage("§a[RelicWars] " + target.getName() + "님의 유물 " + resetCount + "개의 쿨타임을 초기화했습니다.");
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

        if (args[0].equalsIgnoreCase("scan")) {
            if (!(sender instanceof Player player)) return true;
            if (args.length < 2) {
                player.sendMessage("§c[RelicWars] 사용법: /relic scan <유물번호>");
                return true;
            }

            boolean hasLantern = false;
            for (ItemStack item : player.getInventory().getContents()) {
                if (RelicItemUtil.isRelic(item) && RelicItemUtil.getRelicNumber(item) == 20) {
                    hasLantern = true;
                    break;
                }
            }

            if (!hasLantern && !player.hasPermission("relicwars.admin")) {
                player.sendMessage("§c[RelicWars] '#020 소문의 등불' 유물 소유자만 사용할 수 있습니다.");
                return true;
            }

            try {
                int targetNum = Integer.parseInt(args[1]);
                RelicDefinition def = RelicDefinition.getByNumber(targetNum);
                if (def == null) {
                    player.sendMessage("§c[RelicWars] 존재하지 않는 유물 번호입니다. (1~30)");
                    return true;
                }
                
                String ownerUuid = plugin.getDatabaseManager().getRelicOwner(targetNum);
                if (ownerUuid == null) {
                    player.sendMessage("§7[소문의 등불] §e" + def.getName() + " §7유물은 현재 소유자가 없거나 맵 어딘가에 있습니다.");
                } else {
                    Player target = Bukkit.getPlayer(UUID.fromString(ownerUuid));
                    if (target != null && target.isOnline()) {
                        player.sendMessage("§7[소문의 등불] §e" + def.getName() + " §7유물의 소유자: §c" + target.getName());
                    } else {
                        // 오프라인이거나 다른 월드인 경우 이름 가져오기
                        org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(UUID.fromString(ownerUuid));
                        player.sendMessage("§7[소문의 등불] §e" + def.getName() + " §7유물의 소유자: §c" + (offlineTarget.getName() != null ? offlineTarget.getName() : "알 수 없음") + " §7(오프라인)");
                    }
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§c[RelicWars] 유물 번호는 숫자여야 합니다.");
            }
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
        if (!RelicItemUtil.isRelic(hand)) {
            sender.sendMessage("§c[RelicWars] 손에 유물을 들고 있어야 합니다.");
            return;
        }

        sender.getInventory().setItemInMainHand(null);
        target.getInventory().addItem(hand);
        
        int relicNum = RelicItemUtil.getRelicNumber(hand);
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
            }
            completions.add("transfer");
            completions.add("scan");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("scan")) {
                for (int i = 1; i <= 30; i++) {
                    completions.add(String.valueOf(i));
                }
            } else {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("relicwars.admin")) {
            for (int i = 1; i <= 30; i++) {
                completions.add(String.valueOf(i));
            }
        }
        return completions;
    }
}
