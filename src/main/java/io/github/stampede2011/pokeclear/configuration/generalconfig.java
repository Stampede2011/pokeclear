package io.github.stampede2011.pokeclear.configuration;

import io.github.stampede2011.pokeclear.cleartype;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;

import java.util.Arrays;
import java.util.List;

@ConfigSerializable
public class generalconfig {
    @Setting(comment = "Prefix to be added before clear messages.")
    public static String prefix = "&8[&bPoke&3Clear&8] ";
    @Setting(comment = "Message saying how long till next clear. %interval% automatically gets replaced, please leave it in.")
    public static String nextClearIntervalMessage = "&cNext clear in %interval% seconds";
    @Setting(comment = "True, if you want a 1 minute warning until clear.")
    public static boolean doWarningMessage = false;
    @Setting(comment = "Warning Message saying 1 minute until next clear.")
    public static String warningIntervalMessage = "&cPokemon spawns will be clearing in &l1 minute";
    @Setting(comment = "True, if you want to include how long until next clear.")
    public static boolean doNextClearMessage = false;
    @Setting(comment = "How frequent in seconds PokeClear should auto run. Value is in seconds")
    public static int interval = 900;
    @Setting(comment = "Define what to clear in the specified interval. Can be either ALL OR a collection of the following extra criteria REGULAR, MEGA, TOTEM, SHINY, LEGEND, PARTICLE. Default clears all regular and totem pokemon. ALL takes always priority over others.")
    public static List<cleartype> autoClearTypes = Arrays.asList(new cleartype[] { cleartype.REGULAR, cleartype.TOTEM });
    @Setting(comment = "Message to display to the chat when clearing pokes")
    public static String clearMessage = "&3Pokemon spawns have now been refreshed.";
    @Setting(comment = "Items to NOT remove during a clear when ITEM is in cleartypes.")
    public static List<ItemType> itemBlacklist = Arrays.asList(ItemTypes.PURPLE_SHULKER_BOX, ItemTypes.NETHER_STAR);
}
