package com.dyxiaojiazi.hearttogether.command;

import com.dyxiaojiazi.hearttogether.data.BindingManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class HeartCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("heart")
                .then(Commands.literal("bind")
                        .then(Commands.argument("player1", EntityArgument.player())
                                .then(Commands.argument("player2", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer p1 = EntityArgument.getPlayer(ctx, "player1");
                                            ServerPlayer p2 = EntityArgument.getPlayer(ctx, "player2");
                                            return bindPlayers(ctx, p1, p2);
                                        })
                                )
                        )
                )
                .then(Commands.literal("unbind")
                        .then(Commands.argument("player1", EntityArgument.player())
                                .then(Commands.argument("player2", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer p1 = EntityArgument.getPlayer(ctx, "player1");
                                            ServerPlayer p2 = EntityArgument.getPlayer(ctx, "player2");
                                            return unbindPlayers(ctx, p1, p2);
                                        })
                                )
                        )
                )
                .then(Commands.literal("list")
                        .executes(HeartCommand::listBindings)
                )
        );
    }

    private static int bindPlayers(CommandContext<CommandSourceStack> ctx, ServerPlayer p1, ServerPlayer p2) {
        if (p1 == p2) {
            ctx.getSource().sendFailure(Component.translatable("heart.command.bind.same"));
            return 0;
        }
        if (BindingManager.isBound(p1) || BindingManager.isBound(p2)) {
            ctx.getSource().sendFailure(Component.translatable("heart.command.bind.already"));
            return 0;
        }
        BindingManager.bind(p1, p2);
        // 修改：用 () -> 包装 Component
        ctx.getSource().sendSuccess(() -> Component.translatable("heart.command.bind.success", p1.getName(), p2.getName()), true);
        return 1;
    }

    private static int unbindPlayers(CommandContext<CommandSourceStack> ctx, ServerPlayer p1, ServerPlayer p2) {
        if (!BindingManager.areBound(p1, p2)) {
            ctx.getSource().sendFailure(Component.translatable("heart.command.unbind.notbound"));
            return 0;
        }
        BindingManager.unbind(p1, p2);
        // 修改：用 () -> 包装 Component
        ctx.getSource().sendSuccess(() -> Component.translatable("heart.command.unbind.success", p1.getName(), p2.getName()), true);
        return 1;
    }

    private static int listBindings(CommandContext<CommandSourceStack> ctx) {
        Collection<List<UUID>> bindings = BindingManager.getAllBindings();
        if (bindings.isEmpty()) {
            // 修改：用 () -> 包装 Component
            ctx.getSource().sendSuccess(() -> Component.translatable("heart.command.list.empty"), false);
        } else {
            // 修改：用 () -> 包装 Component
            ctx.getSource().sendSuccess(() -> Component.translatable("heart.command.list.header"), false);
            MinecraftServer server = ctx.getSource().getServer();
            for (List<UUID> pair : bindings) {
                UUID id1 = pair.get(0), id2 = pair.get(1);
                String name1 = server.getPlayerList().getPlayer(id1) != null ?
                        server.getPlayerList().getPlayer(id1).getName().getString() : id1.toString();
                String name2 = server.getPlayerList().getPlayer(id2) != null ?
                        server.getPlayerList().getPlayer(id2).getName().getString() : id2.toString();
                // 修改：用 () -> 包装 Component.literal
                ctx.getSource().sendSuccess(() -> Component.literal(name1 + " <-> " + name2), false);
            }
        }
        return 1;
    }
}