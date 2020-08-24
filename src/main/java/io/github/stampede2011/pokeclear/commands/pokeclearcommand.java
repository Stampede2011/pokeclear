package io.github.stampede2011.pokeclear.commands;

import io.github.stampede2011.pokeclear.cleartype;
import io.github.stampede2011.pokeclear.pokeclear;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class pokeclearcommand implements CommandExecutor
{
    private pokeclear plugin;

    public pokeclearcommand(pokeclear plugin) { this.plugin = plugin; }



    @NotNull
    public CommandResult execute(@NotNull CommandSource src, @NotNull CommandContext args) {
        ArrayList<cleartype> commandArgs = new ArrayList<cleartype>();

        if (args.getOne("all").isPresent()) {

            src.sendMessage(Text.of(new Object[] { TextColors.YELLOW, "Detected ALL, other filters will be discarded." }));
            commandArgs.add(cleartype.ALL);
        } else {

            if (args.getOne("mega").isPresent())
            {
                commandArgs.add(cleartype.MEGA);
            }
            if (args.getOne("totem").isPresent())
            {
                commandArgs.add(cleartype.TOTEM);
            }
            if (args.getOne("shiny").isPresent())
            {
                commandArgs.add(cleartype.SHINY);
            }
            if (args.getOne("legend").isPresent())
            {
                commandArgs.add(cleartype.LEGEND);
            }
            if (args.getOne("particle").isPresent())
            {
                commandArgs.add(cleartype.PARTICLE);
            }
            if (args.getOne("item").isPresent())
            {
                commandArgs.add(cleartype.ITEM);
            }
        }

        if (commandArgs.isEmpty()) {
            src.sendMessage(Text.of(new Object[] { TextColors.YELLOW, "No flags found, using default behaviour (regular pokemon)." }));
        }

        commandArgs.add(cleartype.REGULAR);

        AtomicInteger count = new AtomicInteger();
        Sponge.getServer().getWorlds().forEach(world ->
                count.addAndGet(this.plugin.clearPokes(world, commandArgs)));

        src.sendMessage(Text.of(new Object[] { TextColors.GREEN, "Executed Clear. Removed pokemon: " + count.intValue() }));

        return CommandResult.success();
    }
}
