package io.github.stampede2011.pokeclear;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.pixelmongenerations.common.entity.pixelmon.Entity1Base;
import com.pixelmongenerations.common.entity.pixelmon.EntityPixelmon;
import com.pixelmongenerations.core.enums.EnumSpecies;
import io.github.stampede2011.pokeclear.commands.pokeclearcommand;
import io.github.stampede2011.pokeclear.configuration.generalconfig;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Plugin(
        id = "pokeclear",
        name = "PokeClear",
        version = "2.0.6",
        authors = {"Smackzter", "SparkVGX", "Polymeta"},
        dependencies = {@Dependency(id = "pixelmon")},
        description = "Clears all loaded pokemon based on flags you input"
)
public class pokeclear {

    @Inject
    private Logger logger;
    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> loader;
    private CommentedConfigurationNode node;
    private generalconfig config;

    public generalconfig getConfig() {
        return this.config;
    }

    public Logger getLogger() {
        return this.logger;
    }

    @Listener
    public void gameStartEvent(GameStartingServerEvent event) {
        try {
            loadConfig();
        } catch (IOException | ninja.leaping.configurate.objectmapping.ObjectMappingException e) {
            e.printStackTrace();
        }

        int interval = generalconfig.interval;
        this.logger.info("Starting scheduled pokeclear with an interval of " + interval + " seconds.");
        this.logger.info("Server restart is required if interval is changed in the config file.");

        Sponge.getScheduler().createTaskBuilder().execute(() -> {
            int count = 0;
            for (World world : Sponge.getServer().getWorlds()) {
                count += clearPokes(world, generalconfig.autoClearTypes);
            }
            Sponge.getServer().getConsole().sendMessage(Text.of("Automated Clear executed. Total removed pokemon: " + count));
            Sponge.getServer().getBroadcastChannel().send(GetPokeClearMessage());

        }).delay(interval, TimeUnit.SECONDS)
                .interval(interval, TimeUnit.SECONDS)
                .submit(this);

        if (generalconfig.doWarningMessage) {
            Sponge.getScheduler().createTaskBuilder().execute(() -> {
                Sponge.getServer().getBroadcastChannel().send(GetWarningMessage());

            }).delay(interval - 60L, TimeUnit.SECONDS)
                    .interval(interval, TimeUnit.SECONDS)
                    .submit(this);
        }

        CommandSpec pokeClearCmd = CommandSpec.builder()
                .permission("pokeclear.use").executor(new pokeclearcommand(this))
                .arguments(GenericArguments.flags().flag(new String[]{"-all"})
                        .flag(new String[]{"-mega"}).flag(new String[]{"-totem"})
                        .flag(new String[]{"-shiny"}).flag(new String[]{"-legend"})
                        .flag(new String[]{"-particle"}).flag(new String[]{"-item"})
                        .buildWith(GenericArguments.none()))
                .description(Text.of("Clears all \"normal\" pokemon, can be expanded upon via flags"))
                .build();
        Sponge.getCommandManager().register(this, pokeClearCmd, "pokeclear", "pokec", "pokekill");
        this.logger.info("PokeClear has started.");


    }

    private Text GetPokeClearMessage() {
        return Text.of(
                TextSerializers.FORMATTING_CODE.deserialize(generalconfig.prefix),
                TextSerializers.FORMATTING_CODE.deserialize(generalconfig.clearMessage),
                generalconfig.doNextClearMessage ?
                        TextSerializers.FORMATTING_CODE.deserialize(generalconfig.nextClearIntervalMessage.replace("%interval%", Integer.toString(generalconfig.interval)))
                        : "");
    }
    private Text GetWarningMessage() {
        return Text.of(
                TextSerializers.FORMATTING_CODE.deserialize(generalconfig.prefix),
                TextSerializers.FORMATTING_CODE.deserialize(generalconfig.warningIntervalMessage)
        );
    }

    @Listener
    public void onReloadEvent(GameReloadEvent event) {
        try {
            loadConfig();
        }
        catch (IOException|ninja.leaping.configurate.objectmapping.ObjectMappingException ex) {

            this.logger.error("Error reloading config", ex);
        }
    }



    private void loadConfig() throws ObjectMappingException, IOException {
        this.node = (CommentedConfigurationNode)this.loader.load();
        TypeToken<generalconfig> type = TypeToken.of(generalconfig.class);
        this.config = (generalconfig)this.node.getValue(type, new generalconfig());
        this.node.setValue(type, this.config);
        this.loader.save(this.node);
    }




    private boolean pokeWithParticles(Entity entity) { return entity.getKeys()
            .stream()
            .anyMatch(key -> key.getId()
                    .equals("entity-particles:active")); }



    public int clearPokes(World world, List<cleartype> typesToClear) {
        AtomicInteger kills = new AtomicInteger(0);
        if (typesToClear.contains(cleartype.ALL)) {

            world.getEntities(EntityPixelmon.class::isInstance)
                    .stream()
                    .map(EntityPixelmon.class::cast)
                    .forEach(pokemon -> {
                        if (!pokemon.isInRanchBlock && pokemon.battleController == null && !pokemon.hasOwner()) {


                            kills.getAndIncrement();
                            pokemon.setDead();
                        }
                    });
            return kills.intValue();
        }



        List<EntityPixelmon> allPokes = (List)world.getEntities(EntityPixelmon.class::isInstance).stream().map(EntityPixelmon.class::cast).collect(Collectors.toList());

        ArrayList<EntityPixelmon> tobeCleared = new ArrayList<EntityPixelmon>();

        if (typesToClear.contains(cleartype.REGULAR))
        {
            tobeCleared.addAll((Collection)allPokes.stream().filter(poke ->
                    (!poke.isShiny() &&
                            !poke.hasOwner() && !poke.isInRanchBlock &&
                            !poke.isBossPokemon() && poke.battleController == null &&

                            !EnumSpecies.legendaries.contains((poke.getSpecies()).name) &&
                            !EnumSpecies.ultrabeasts.contains((poke.getSpecies()).name)))
                    .collect(Collectors.toList()));
        }
        if (typesToClear.contains(cleartype.MEGA))
        {
            tobeCleared.addAll((Collection)allPokes.stream().filter(poke -> (poke.isMega && poke.battleController == null && !poke.isInRanchBlock &&


                    !poke.hasOwner()))
                    .collect(Collectors.toList()));
        }
        if (typesToClear.contains(cleartype.TOTEM))
        {
            tobeCleared.addAll((Collection)allPokes.stream().filter(poke -> (poke.isTotem() && poke.battleController == null && !poke.isInRanchBlock &&


                    !poke.hasOwner()))
                    .collect(Collectors.toList()));
        }
        if (typesToClear.contains(cleartype.SHINY))
        {
            tobeCleared.addAll((Collection)allPokes.stream().filter(poke -> (poke.isShiny() && poke.battleController == null && !poke.isInRanchBlock &&


                    !poke.hasOwner()))
                    .collect(Collectors.toList()));
        }
        if (typesToClear.contains(cleartype.LEGEND))
        {
            tobeCleared.addAll((Collection)allPokes.stream().filter(poke -> (EnumSpecies.legendaries.contains((poke.getSpecies()).name) || (EnumSpecies.ultrabeasts.contains((poke.getSpecies()).name) && poke.battleController == null && !poke.isInRanchBlock &&


                    !poke.hasOwner())))
                    .collect(Collectors.toList()));
        }
        if (typesToClear.contains(cleartype.PARTICLE))
        {
            tobeCleared.addAll((Collection)allPokes.stream().filter(poke -> (pokeWithParticles((Entity)poke) && poke.battleController == null && !poke.isInRanchBlock &&


                    !poke.hasOwner()))
                    .collect(Collectors.toList()));
        }
        if (typesToClear.contains(cleartype.ITEM))
        {
            int removed = 0;
            for (World worlds : Sponge.getServer().getWorlds()) {
                for (Entity entity : worlds.getEntities()) {
                    if (entity instanceof org.spongepowered.api.entity.Item) {
                        if (!generalconfig.itemBlacklist.contains(((Item) entity).getItemType())) {
                            entity.remove();
                            removed++;
                        }
                    }
                }
            }
            kills.getAndAdd(removed);
        }
        kills.getAndAdd(tobeCleared.size());
        tobeCleared.forEach(Entity1Base::func_70106_y);
        return kills.intValue();
    }

}