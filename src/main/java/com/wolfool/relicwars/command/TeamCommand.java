package com.wolfool.relicwars.command;

import com.wolfool.relicwars.RelicWars;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamCommand implements CommandExecutor {

    private final RelicWars plugin;
    
    // TPA 대상자 -> TPA 요청자
    private final Map<UUID, UUID> pendingTpas = new HashMap<>();

    public TeamCommand(RelicWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /team invite <플레이어>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage("§c대상을 찾을 수 없습니다.");
                    return true;
                }
                if (target.equals(player)) {
                    player.sendMessage("§c자신을 초대할 수 없습니다.");
                    return true;
                }
                plugin.getTeamManager().invitePlayer(player, target);
            }
            case "accept" -> {
                plugin.getTeamManager().acceptInvite(player);
            }
            case "leave" -> {
                plugin.getTeamManager().leaveTeam(player);
            }
            case "info" -> {
                String teamId = plugin.getTeamManager().getTeamId(player);
                if (teamId == null) {
                    player.sendMessage("§c소속된 팀이 없습니다.");
                    return true;
                }
                player.sendMessage("§b=== 팀 정보 ===");
                for (UUID memberUuid : plugin.getTeamManager().getTeamMembers(teamId)) {
                    Player p = Bukkit.getPlayer(memberUuid);
                    if (p != null && p.isOnline()) {
                        int hp = (int) p.getHealth();
                        int relics = plugin.getRelicManager().countPlayerRelics(p);
                        player.sendMessage("§e- " + p.getName() + " §7(HP: " + hp + ", 유물: " + relics + "개)");
                    } else {
                        player.sendMessage("§7- 오프라인 멤버 (" + memberUuid + ")");
                    }
                }
                player.sendMessage("§b총 유물 수: §e" + plugin.getTeamManager().getTeamRelicCount(teamId));
            }
            case "tpa" -> {
                if (!plugin.getTeamManager().hasTeam(player)) {
                    player.sendMessage("§c[RelicWars] 팀에 소속되어 있지 않습니다.");
                    return true;
                }

                String teamId = plugin.getTeamManager().getTeamId(player);
                Player teammate = null;
                for (UUID memberId : plugin.getTeamManager().getTeamMembers(teamId)) {
                    if (!memberId.equals(player.getUniqueId())) {
                        teammate = Bukkit.getPlayer(memberId);
                        break;
                    }
                }

                if (teammate == null || !teammate.isOnline()) {
                    player.sendMessage("§c[RelicWars] 팀원이 오프라인이거나 찾을 수 없습니다.");
                    return true;
                }

                pendingTpas.put(teammate.getUniqueId(), player.getUniqueId());
                player.sendMessage("§a[RelicWars] " + teammate.getName() + "님에게 텔레포트 요청을 보냈습니다.");

                Component acceptButton = Component.text(" [여기를 클릭하여 수락] ")
                        .color(NamedTextColor.YELLOW)
                        .clickEvent(ClickEvent.runCommand("/team tpaccept"));

                teammate.sendMessage(Component.text("§a[RelicWars] " + player.getName() + "님이 텔레포트를 요청했습니다.").append(acceptButton));

                final UUID targetId = teammate.getUniqueId();
                final UUID reqId = player.getUniqueId();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    pendingTpas.remove(targetId, reqId);
                }, 600L); // 30초 만료
            }
            case "tpaccept" -> {
                UUID requesterId = pendingTpas.remove(player.getUniqueId());
                if (requesterId == null) {
                    player.sendMessage("§c[RelicWars] 대기 중인 텔레포트 요청이 없습니다.");
                    return true;
                }

                Player requester = Bukkit.getPlayer(requesterId);
                if (requester == null || !requester.isOnline()) {
                    player.sendMessage("§c[RelicWars] 요청한 플레이어가 오프라인입니다.");
                    return true;
                }

                requester.teleport(player.getLocation());
                requester.sendMessage("§a[RelicWars] " + player.getName() + "님이 텔레포트 요청을 수락했습니다.");
                player.sendMessage("§a[RelicWars] " + requester.getName() + "님의 텔레포트 요청을 수락했습니다.");
            }
            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6=== RelicWars 팀 명령어 ===");
        player.sendMessage("§e/team invite <유저> §7- 유저를 팀에 초대합니다.");
        player.sendMessage("§e/team accept §7- 팀 초대를 수락합니다.");
        player.sendMessage("§e/team leave §7- 현재 팀에서 탈퇴합니다.");
        player.sendMessage("§e/team info §7- 소속 팀의 정보를 확인합니다.");
        player.sendMessage("§e/team tpa §7- 팀원에게 텔레포트를 요청합니다.");
        player.sendMessage("§e/team tpaccept §7- 팀원의 텔레포트 요청을 수락합니다.");
    }
}
