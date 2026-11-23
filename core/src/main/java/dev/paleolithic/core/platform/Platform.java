package dev.paleolithic.core.platform;

/**
 * What core needs from the platform it runs on. Each adapter (Fabric,
 * NeoForge, Paper) supplies one implementation. Kept small on purpose: core
 * works on vanilla types, so most hooks are just events the adapter forwards
 * to {@link dev.paleolithic.core.Paleolithic}.
 */
public interface Platform {

    /** Short platform name for logs, for example "Fabric". */
    String name();
}
