package dev.paleolithic.testbot;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ClientRegistryLayer;
import net.minecraft.client.multiplayer.RegistryDataCollector;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ClientboundCodeOfConductPacket;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.configuration.ServerboundAcceptCodeOfConductPacket;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ServerboundSelectKnownPacks;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.server.packs.resources.ResourceProvider;

/**
 * Joins a server the way an unmodified client does (offline mode), prints
 * every experience-bar and action-bar update it receives, and runs a timed
 * script of commands.
 *
 * <pre>
 * VanillaBot host port name "0:time set noon;5:weather clear;60:quit"
 * </pre>
 *
 * Each script entry is {@code seconds-after-join:command}; {@code quit}
 * disconnects. Output lines start with the seconds since join:
 * {@code [12.0] XP fill=0.712 level=0} and {@code [12.0] BAR <text>}.
 */
public final class VanillaBot {

    private final String host;
    private final int port;
    private final String name;
    private final Connection connection;
    private final CountDownLatch joined = new CountDownLatch(1);
    private final CountDownLatch closed = new CountDownLatch(1);
    private final RegistryDataCollector registries = new RegistryDataCollector();
    private volatile long joinedAt;

    private VanillaBot(String host, int port, String name) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.connection = Connection.connectToServer(new InetSocketAddress(host, port),
                EventLoopGroupHolder.remote(false), null);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("usage: VanillaBot host port name script");
            System.exit(2);
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        VanillaBot bot = new VanillaBot(args[0], Integer.parseInt(args[1]), args[2]);
        System.exit(bot.run(parse(args[3])));
    }

    record Step(double at, String command) {}

    static List<Step> parse(String script) {
        List<Step> steps = new ArrayList<>();
        for (String part : script.split(";")) {
            if (part.isBlank()) continue;
            int colon = part.indexOf(':');
            steps.add(new Step(Double.parseDouble(part.substring(0, colon).trim()), part.substring(colon + 1).trim()));
        }
        return steps;
    }

    int run(List<Step> steps) throws InterruptedException {
        UUID offline = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        connection.initiateServerboundPlayConnection(host, port, LoginProtocols.SERVERBOUND,
                LoginProtocols.CLIENTBOUND, loginListener(), false);
        connection.send(new ServerboundHelloPacket(name, offline));

        Thread ticker = Thread.ofPlatform().daemon().start(() -> {
            while (closed.getCount() > 0) {
                connection.tick();
                try { Thread.sleep(50); } catch (InterruptedException e) { return; }
            }
        });

        if (!joined.await(60, TimeUnit.SECONDS)) {
            log("FAIL did not join within 60 seconds");
            return 1;
        }
        log("JOINED as " + name);
        for (Step step : steps) {
            long wait = joinedAt + (long) (step.at * 1000) - System.currentTimeMillis();
            if (wait > 0 && closed.await(wait, TimeUnit.MILLISECONDS)) break;
            if (closed.getCount() == 0) break;
            if (step.command.equals("quit")) {
                log("QUIT");
                connection.disconnect(net.minecraft.network.chat.Component.literal("bot done"));
                closed.countDown();
                break;
            }
            log("CMD /" + step.command);
            connection.send(new ServerboundChatCommandPacket(step.command));
        }
        closed.await(5, TimeUnit.SECONDS);
        ticker.interrupt();
        return 0;
    }

    // --- listeners --------------------------------------------------------

    private ClientLoginPacketListener loginListener() {
        return listener(ClientLoginPacketListener.class, ConnectionProtocol.LOGIN, packet -> {
            if (packet instanceof ClientboundLoginCompressionPacket p) {
                connection.setupCompression(p.getCompressionThreshold(), false);
            } else if (packet instanceof ClientboundLoginDisconnectPacket p) {
                log("FAIL login refused: " + p.reason().getString());
            } else if (packet instanceof ClientboundLoginFinishedPacket) {
                connection.setupInboundProtocol(ConfigurationProtocols.CLIENTBOUND, configurationListener());
                connection.send(ServerboundLoginAcknowledgedPacket.INSTANCE);
                connection.setupOutboundProtocol(ConfigurationProtocols.SERVERBOUND);
                connection.send(new ServerboundClientInformationPacket(ClientInformation.createDefault()));
            } else {
                return false;
            }
            return true;
        });
    }

    private ClientConfigurationPacketListener configurationListener() {
        return listener(ClientConfigurationPacketListener.class, ConnectionProtocol.CONFIGURATION, packet -> {
            if (common(packet)) return true;
            if (packet instanceof ClientboundSelectKnownPacks) {
                // Know no packs, so the server sends every registry in full.
                connection.send(new ServerboundSelectKnownPacks(List.of()));
            } else if (packet instanceof ClientboundRegistryDataPacket p) {
                registries.appendContents(p.registry(), p.entries());
            } else if (packet instanceof ClientboundUpdateTagsPacket p) {
                registries.appendTags(p.tags());
            } else if (packet instanceof ClientboundCodeOfConductPacket) {
                connection.send(ServerboundAcceptCodeOfConductPacket.INSTANCE);
            } else if (packet instanceof ClientboundFinishConfigurationPacket) {
                RegistryAccess.Frozen access = registries.collectGameRegistries(ResourceProvider.EMPTY,
                        ClientRegistryLayer.createRegistryAccess().compositeAccess(), false);
                connection.setupInboundProtocol(
                        GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(access)),
                        gameListener());
                connection.send(ServerboundFinishConfigurationPacket.INSTANCE);
                connection.setupOutboundProtocol(GameProtocols.SERVERBOUND_TEMPLATE.bind(
                        RegistryFriendlyByteBuf.decorator(access), new GameProtocols.Context() {
                            @Override public boolean hasInfiniteMaterials() { return false; }
                            @Override public boolean canUseCommandBlocks() { return false; }
                        }));
            } else {
                return false;
            }
            return true;
        });
    }

    private ClientGamePacketListener gameListener() {
        return listener(ClientGamePacketListener.class, ConnectionProtocol.PLAY, packet -> {
            if (common(packet)) return true;
            if (packet instanceof ClientboundLoginPacket) {
                connection.send(new ServerboundPlayerLoadedPacket());
                joinedAt = System.currentTimeMillis();
                joined.countDown();
            } else if (packet instanceof ClientboundPlayerCombatKillPacket) {
                log("DIED, respawning");
                connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            } else if (packet instanceof ClientboundSetHealthPacket p && p.getHealth() <= 0) {
                // Joined while dead: the death screen's respawn button.
                log("DEAD on join, respawning");
                connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            } else if (packet instanceof ClientboundSetExperiencePacket p) {
                log(String.format("XP fill=%.3f level=%d", p.getExperienceProgress(), p.getExperienceLevel()));
            } else if (packet instanceof ClientboundSetActionBarTextPacket p) {
                log("BAR " + p.text().getString());
            } else if (packet instanceof ClientboundSystemChatPacket p) {
                log((p.overlay() ? "BAR " : "CHAT ") + p.content().getString());
            } else {
                return false;
            }
            return true;
        });
    }

    /** Packets every phase must answer to stay connected. */
    private boolean common(Object packet) {
        if (packet instanceof ClientboundKeepAlivePacket p) {
            connection.send(new ServerboundKeepAlivePacket(p.getId()));
        } else if (packet instanceof ClientboundPingPacket p) {
            connection.send(new ServerboundPongPacket(p.getId()));
        } else if (packet instanceof ClientboundResourcePackPushPacket p) {
            log("PACK offered " + p.url());
            connection.send(new ServerboundResourcePackPacket(p.id(), ServerboundResourcePackPacket.Action.ACCEPTED));
            connection.send(new ServerboundResourcePackPacket(p.id(), ServerboundResourcePackPacket.Action.DOWNLOADED));
            connection.send(new ServerboundResourcePackPacket(p.id(),
                    ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
        } else {
            return false;
        }
        return true;
    }

    interface Handler {
        boolean handle(Object packet);
    }

    /**
     * Builds a packet listener from one handler. Vanilla packets call
     * {@code listener.handleX(packet)}; every one-argument call lands here,
     * so the bot ignores what it does not care about instead of implementing
     * a hundred methods.
     */
    @SuppressWarnings("unchecked")
    private <T> T listener(Class<T> type, ConnectionProtocol protocol, Handler handler) {
        InvocationHandler ih = (proxy, method, args) -> {
            String m = method.getName();
            switch (m) {
                case "protocol": return protocol;
                case "isAcceptingMessages": return connection.isConnected();
                case "shouldHandleMessage": return true;
                case "onDisconnect":
                    log("DISCONNECTED " + ((DisconnectionDetails) args[0]).reason().getString());
                    closed.countDown();
                    return null;
                case "hashCode": return System.identityHashCode(proxy);
                case "equals": return proxy == args[0];
                case "toString": return "VanillaBot " + type.getSimpleName();
                default: break;
            }
            if (args != null && args.length == 1 && m.startsWith("handle")) {
                try {
                    handler.handle(args[0]);
                } catch (Exception e) {
                    log("ERROR handling " + args[0].getClass().getSimpleName() + ": " + e);
                }
                return null;
            }
            if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
            return defaultValue(method);
        };
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, ih);
    }

    private static Object defaultValue(Method method) {
        Class<?> r = method.getReturnType();
        if (r == boolean.class) return false;
        if (r == int.class || r == long.class || r == short.class || r == byte.class) return 0;
        if (r == float.class || r == double.class) return 0.0;
        return null;
    }

    private void log(String line) {
        double t = joinedAt == 0 ? 0 : (System.currentTimeMillis() - joinedAt) / 1000.0;
        System.out.printf("[%5.1f] %s%n", t, line);
        System.out.flush();
    }
}
