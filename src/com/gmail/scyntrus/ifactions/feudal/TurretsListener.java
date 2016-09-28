package com.gmail.scyntrus.ifactions.feudal;

import com.gmail.scyntrus.fmob.FactionMob;
import me.forseth11.Turrets.TurretAttackMobEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.Plugin;
import us.forseth11.feudal.core.Feudal;
import us.forseth11.feudal.kingdoms.Kingdom;
import us.forseth11.feudal.user.User;

public class TurretsListener implements Listener {

    Plugin plugin;

    TurretsListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerCommand(TurretAttackMobEvent e) {
        if (e.getTarget() instanceof FactionMob) {
            User user = Feudal.getUser(e.getTurret().getOwnerUUID());
            if (user != null) {
                Kingdom k = user.getCurrentKingdom();
                if (k != null) {
                    if (((FactionMob) e.getTarget()).getFactionName().equals(k.getName())) {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }
}
