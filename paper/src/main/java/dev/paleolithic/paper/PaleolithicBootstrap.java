package dev.paleolithic.paper;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

/** Registers core's game data (tags, later loot, recipes, world generation) as a data pack. */
public final class PaleolithicBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY.newHandler(event -> {
            try {
                URL pack = Objects.requireNonNull(getClass().getResource("/paleolithic_pack"),
                        "paleolithic_pack missing from the plugin jar");
                URI uri = pack.toURI();
                event.registrar().discoverPack(uri, "game_data");
            } catch (Exception e) {
                context.getLogger().error("Could not register the Paleolithic data pack", e);
            }
        }));
    }
}
