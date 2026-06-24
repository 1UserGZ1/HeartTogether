package com.dyxiaojiazi.hearttogether.event;

import com.dyxiaojiazi.hearttogether.data.BindingManager;
import com.dyxiaojiazi.hearttogether.data.SharedHealthData;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber
public class HealthSyncHandler {

    // 同步所有存活绑定玩家的生命、吸收和最大生命
    private static void syncAlivePlayers(Player player, SharedHealthData data) {
        List<Player> players = BindingManager.getBoundPlayers(player);
        for (Player p : players) {
            if (p != null && p.isAlive()) {
                p.setHealth(data.getCurrentHealth());
                p.setAbsorptionAmount(data.getAbsorption());
                if (p.getMaxHealth() != data.getMaxHealth()) {
                    p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(data.getMaxHealth());
                }
            }
        }
    }

    // 触发受击反馈（音效 + 红色闪烁）
    private static void triggerHurtFeedback(Player player) {
        if (player != null && player.isAlive()) {
            player.playSound(SoundEvents.PLAYER_HURT, 1.0F, 1.0F);
            player.level().broadcastEntityEvent(player, (byte) 2);
        }
    }

    // 每 tick 同步护盾值（捕获外部护盾变化）
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player == null || player.level().isClientSide()) return;
        if (!BindingManager.isBound(player)) return;

        SharedHealthData data = BindingManager.getSharedData(player);
        if (data == null) return;

        float currentAbsorption = player.getAbsorptionAmount();
        float sharedAbsorption = data.getAbsorption();

        if (Math.abs(currentAbsorption - sharedAbsorption) > 0.001f) {
            data.setAbsorption(currentAbsorption);
            syncAlivePlayers(player, data);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        if (!BindingManager.isBound(player)) return;

        SharedHealthData data = BindingManager.getSharedData(player);
        if (data == null) return;

        float amount = event.getAmount();
        float absorption = data.getAbsorption();
        float newHealth;

        // 护盾吸收
        if (absorption > 0) {
            float absorbed = Math.min(absorption, amount);
            absorption -= absorbed;
            amount -= absorbed;
            data.setAbsorption(absorption);
            if (amount <= 0) {
                event.setCanceled(true);
                syncAlivePlayers(player, data);
                return;
            }
        }

        // 实际扣血
        newHealth = data.getCurrentHealth() - amount;
        if (newHealth < 0) newHealth = 0;
        data.setCurrentHealth(newHealth);
        data.setAbsorption(absorption);

        List<Player> boundPlayers = BindingManager.getBoundPlayers(player);
        if (boundPlayers.isEmpty()) {
            event.setCanceled(true);
            return;
        }

        event.setCanceled(true);

        // 同步血量和护盾
        syncAlivePlayers(player, data);

        // 为所有绑定玩家触发受击反馈
        for (Player p : boundPlayers) {
            if (p.isAlive()) {
                triggerHurtFeedback(p);
            }
        }

        // 处理死亡
        if (newHealth <= 0) {
            // 先让所有绑定玩家进入死亡状态（触发原版死亡流程）
            for (Player p : boundPlayers) {
                if (p.isAlive()) {
                    p.die(p.damageSources().generic());
                }
            }
            // 为“非直接受伤”的绑定玩家补发殉情消息
            for (Player p : boundPlayers) {
                if (p != player) { // player 是直接受伤者，已有原版死亡消息
                    String martyrMsg = p.getName().getString() + " 殉情了";
                    Component msg = Component.literal(martyrMsg);
                    // 广播给所有在线玩家
                    p.getServer().getPlayerList().getPlayers().forEach(online ->
                            online.sendSystemMessage(msg, false)
                    );
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        if (!BindingManager.isBound(player)) return;

        SharedHealthData data = BindingManager.getSharedData(player);
        if (data == null) return;

        float newHealth = data.getCurrentHealth() + event.getAmount();
        if (newHealth > data.getMaxHealth()) newHealth = data.getMaxHealth();
        data.setCurrentHealth(newHealth);
        event.setCanceled(true);
        syncAlivePlayers(player, data);
    }

    // 药水事件留空，由 Tick 统一处理护盾同步
    @SubscribeEvent
    public static void onPotionAdded(MobEffectEvent.Added event) {}
    @SubscribeEvent
    public static void onPotionExpiry(MobEffectEvent.Expired event) {}
    @SubscribeEvent
    public static void onPotionRemoved(MobEffectEvent.Remove event) {}

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (!BindingManager.isBound(player)) return;

        SharedHealthData data = BindingManager.getSharedData(player);
        if (data == null) return;

        if (data.getCurrentHealth() <= 0) {
            data.setCurrentHealth(data.getMaxHealth());
            data.setAbsorption(0);
        }
        syncAlivePlayers(player, data);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!BindingManager.isBound(player)) return;

        SharedHealthData data = BindingManager.getSharedData(player);
        if (data == null) return;

        if (data.getCurrentHealth() <= 0) {
            data.setCurrentHealth(data.getMaxHealth());
            data.setAbsorption(0);
        }
        syncAlivePlayers(player, data);
    }
}