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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamCommand implements CommandExecutor {

    private final RelicWars plugin;
    // 초대한 사람 -> 초대받은 사람
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    
    // TPA 대상자 -> TPA 요청자
    private final Map<UUID, UUID> pendingTpas = new HashMap<>();

    public TeamCommand(RelicWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§e[RelicWars] /team invite <유저명> - 팀 초대");
            player.sendMessage("§e[RelicWars] /team accept - 팀 초대 수락");
            player.sendMessage("§e[RelicWars] /team tpa - 팀원에게 텔레포트 요청");
            return true;
        }

        if (args[0].equalsIgnoreCase("invite") && args.length >= 2) {
            if (plugin.getTeamManager().hasTeam(player)) {
                player.sendMessage("§c[RelicWars] 이미 팀에 소속되어 있습니다.");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                player.sendMessage("§c[RelicWars] 대상을 찾을 수 없습니다.");
                return true;
            }

            if (target.equals(player)) {
                player.sendMessage("§c[RelicWars] 자기 자신을 초대할 수 없습니다.");
                return true;
            }

            if (plugin.getTeamManager().hasTeam(target)) {
                player.sendMessage("§c[RelicWars] 상대방이 이미 다른 팀에 소속되어 있습니다.");
                return true;
            }

            // 유물 합계 제한 검사
            int p1Relics = plugin.getRelicManager().countPlayerRelics(player);
            int p2Relics = plugin.getRelicManager().countPlayerRelics(target);
            int maxAllowed = plugin.getConfigManager().getTeamMaxOnFormation();
            
            if (p1Relics + p2Relics > maxAllowed) {
                player.sendMessage("§c[RelicWars] 두 명의 유물 합계가 " + maxAllowed + "개를 초과하여 팀을 맺을 수 없습니다.");
                return true;
            }

            pendingInvites.put(target.getUniqueId(), player.getUniqueId());
            player.sendMessage("§a[RelicWars] " + target.getName() + "님에게 팀 초대를 보냈습니다.");
            target.sendMessage("§a[RelicWars] " + player.getName() + "님이 듀오 팀(2시간 고정) 초대를 보냈습니다.");
            target.sendMessage("§a[RelicWars] 수락하려면 §e/team accept §a를 입력하세요.");
            
            // 60초 뒤 초대 만료
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                pendingInvites.remove(target.getUniqueId(), player.getUniqueId());
            }, 1200L);

            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            if (plugin.getTeamManager().hasTeam(player)) {
                player.sendMessage("§c[RelicWars] 이미 팀에 소속되어 있습니다.");
                return true;
            }

            UUID inviterId = pendingInvites.remove(player.getUniqueId());
            if (inviterId == null) {
                player.sendMessage("§c[RelicWars] 대기 중인 초대가 없습니다.");
                return true;
            }

            Player inviter = Bukkit.getPlayer(inviterId);
            if (inviter == null || !inviter.isOnline()) {
                player.sendMessage("§c[RelicWars] 초대한 플레이어가 오프라인입니다.");
                return true;
            }

            // 최종 결성
            plugin.getTeamManager().createTeam(inviter, player);
            inviter.sendMessage("§a[RelicWars] " + player.getName() + "님과 팀이 되었습니다! (2시간 동안 해체 불가)");
            player.sendMessage("§a[RelicWars] " + inviter.getName() + "님과 팀이 되었습니다! (2시간 동안 해체 불가)");
            return true;
        }

        if (args[0].equalsIgnoreCase("tpa")) {
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
            
            return true;
        }

        if (args[0].equalsIgnoreCase("tpaccept")) {
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
            return true;
        }

        return false;
    }
}
