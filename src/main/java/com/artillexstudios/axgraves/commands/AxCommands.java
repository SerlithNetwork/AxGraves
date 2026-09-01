package com.artillexstudios.axgraves.commands;

import com.artillexstudios.axgraves.commands.subcommands.Help;
import com.artillexstudios.axgraves.commands.subcommands.List;
import com.artillexstudios.axgraves.commands.subcommands.Reload;
import com.artillexstudios.axgraves.commands.subcommands.Teleport;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.math.BlockPosition;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class AxCommands {

    public static LiteralCommandNode<CommandSourceStack> buildAdminCommands() {
        return Commands.literal("axgraves")
                .requires(s -> s.getSender().hasPermission("axgraves.admin"))
                .then(Commands.literal("help")
                        .requires(s -> s.getSender().hasPermission("axgraves.help"))
                        .executes(context -> {
                            Help.INSTANCE.execute(context.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(Commands.literal("reload")
                        .requires(s -> s.getSender().hasPermission("axgraves.reload"))
                        .executes(context -> {
                            Reload.INSTANCE.execute(context.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .build();
    }

    @SuppressWarnings("UnstableApiUsage")
    public static LiteralCommandNode<CommandSourceStack> buildUserCommands() {
        return Commands.literal("graves")
                .then(Commands.literal("list")
                        .requires(s -> s.getSender().hasPermission("axgraves.list"))
                        .executes(context -> {
                            List.INSTANCE.execute(context.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(Commands.literal("tp")
                        .requires(s -> s.getSender() instanceof Player player && player.hasPermission("axgraves.tp"))
                        .then(Commands.argument("world", ArgumentTypes.world())
                                .then(Commands.argument("location", ArgumentTypes.blockPosition())
                                        .executes(context -> {
                                            World world = context.getArgument("world", World.class);
                                            BlockPosition position = context.getArgument("location", BlockPosition.class);
                                            Teleport.INSTANCE.execute((Player) context.getSource().getSender(), world, position.x(),  position.y(), position.z());
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                        .executes(context -> {
                            Teleport.INSTANCE.execute((Player) context.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .build();
    }

}
