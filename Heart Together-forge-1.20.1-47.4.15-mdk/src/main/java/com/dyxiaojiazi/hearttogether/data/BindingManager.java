package com.dyxiaojiazi.hearttogether.data;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class BindingManager {
    // 玩家UUID -> 绑定信息
    private static final Map<UUID, BindingInfo> bindingMap = new HashMap<>();
    // 组合键 -> 共享数据
    private static final Map<String, SharedHealthData> sharedDataMap = new HashMap<>();

    private static class BindingInfo {
        UUID partner;
        String sharedKey;
        float originalMaxHealth;

        BindingInfo(UUID partner, String sharedKey, float originalMaxHealth) {
            this.partner = partner;
            this.sharedKey = sharedKey;
            this.originalMaxHealth = originalMaxHealth;
        }
    }

    private static String generateKey(UUID id1, UUID id2) {
        return id1.compareTo(id2) < 0 ? id1 + ":" + id2 : id2 + ":" + id1;
    }

    private static void setMaxHealth(Player player, float maxHealth) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(maxHealth);
        }
    }

    public static void bind(ServerPlayer p1, ServerPlayer p2) {
        UUID id1 = p1.getUUID(), id2 = p2.getUUID();
        // 计算共享初始值（取平均）
        float avgHealth = (p1.getHealth() + p2.getHealth()) / 2;
        float avgMax = (p1.getMaxHealth() + p2.getMaxHealth()) / 2;
        float avgAbs = (p1.getAbsorptionAmount() + p2.getAbsorptionAmount()) / 2;

        SharedHealthData shared = new SharedHealthData(avgHealth, avgMax, avgAbs);
        String key = generateKey(id1, id2);
        sharedDataMap.put(key, shared);

        float origMax1 = p1.getMaxHealth();
        float origMax2 = p2.getMaxHealth();

        bindingMap.put(id1, new BindingInfo(id2, key, origMax1));
        bindingMap.put(id2, new BindingInfo(id1, key, origMax2));

        // 统一最大生命值
        setMaxHealth(p1, avgMax);
        setMaxHealth(p2, avgMax);
        // 设置当前生命和吸收
        p1.setHealth(avgHealth);
        p1.setAbsorptionAmount(avgAbs);
        p2.setHealth(avgHealth);
        p2.setAbsorptionAmount(avgAbs);
    }

    public static void unbind(ServerPlayer p1, ServerPlayer p2) {
        UUID id1 = p1.getUUID(), id2 = p2.getUUID();
        BindingInfo info1 = bindingMap.get(id1);
        if (info1 == null || !info1.partner.equals(id2)) return;

        SharedHealthData shared = sharedDataMap.get(info1.sharedKey);
        if (shared == null) return;

        float health = shared.getCurrentHealth();
        float absorption = shared.getAbsorption();

        // 恢复各自原始最大生命值
        BindingInfo info2 = bindingMap.get(id2);
        setMaxHealth(p1, info1.originalMaxHealth);
        setMaxHealth(p2, info2.originalMaxHealth);

        p1.setHealth(health);
        p1.setAbsorptionAmount(absorption);
        p2.setHealth(health);
        p2.setAbsorptionAmount(absorption);

        bindingMap.remove(id1);
        bindingMap.remove(id2);
        sharedDataMap.remove(info1.sharedKey);
    }

    public static boolean isBound(Player player) {
        return bindingMap.containsKey(player.getUUID());
    }

    public static boolean areBound(Player p1, Player p2) {
        UUID id1 = p1.getUUID(), id2 = p2.getUUID();
        BindingInfo info = bindingMap.get(id1);
        return info != null && info.partner.equals(id2) &&
                bindingMap.containsKey(id2) && bindingMap.get(id2).partner.equals(id1);
    }

    public static SharedHealthData getSharedData(Player player) {
        BindingInfo info = bindingMap.get(player.getUUID());
        return info == null ? null : sharedDataMap.get(info.sharedKey);
    }

    public static List<Player> getBoundPlayers(Player player) {
        BindingInfo info = bindingMap.get(player.getUUID());
        if (info == null) return Collections.emptyList();
        List<Player> result = new ArrayList<>();
        result.add(player);
        Player partner = player.getServer().getPlayerList().getPlayer(info.partner);
        if (partner != null) result.add(partner);
        return result;
    }

    public static Collection<List<UUID>> getAllBindings() {
        Set<List<UUID>> pairs = new HashSet<>();
        for (Map.Entry<UUID, BindingInfo> entry : bindingMap.entrySet()) {
            UUID id1 = entry.getKey();
            UUID id2 = entry.getValue().partner;
            if (id1.compareTo(id2) < 0) {
                pairs.add(Arrays.asList(id1, id2));
            }
        }
        return pairs;
    }
}