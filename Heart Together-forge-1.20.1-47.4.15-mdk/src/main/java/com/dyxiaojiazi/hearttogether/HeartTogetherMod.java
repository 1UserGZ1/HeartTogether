package com.dyxiaojiazi.hearttogether;

import com.dyxiaojiazi.hearttogether.command.HeartCommand;
import com.dyxiaojiazi.hearttogether.event.HealthSyncHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(HeartTogetherMod.MOD_ID)
public class HeartTogetherMod {
    public static final String MOD_ID = "hearttogether";
    private static final Logger LOGGER = LogUtils.getLogger();

    public HeartTogetherMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        // 注册通用事件总线
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new HealthSyncHandler());
        // 指令注册使用 RegisterCommandsEvent
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        HeartCommand.register(event.getDispatcher());
    }
}