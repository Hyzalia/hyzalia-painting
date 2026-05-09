package com.hyzalia.paint;

import com.hypixel.hytale.builtin.buildertools.tooloperations.LayersStratumOperation;
import com.hypixel.hytale.builtin.buildertools.tooloperations.ToolOperation;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hyzalia.paint.prefab.PrefabSaveFluidCommand;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Point d’entrée du plugin Hyzalia — Painting (logique minimale ; le pack d’assets reste dans les resources). */
public class PaintingPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String LAYERS_STRATUM_TOOL_ID = "LayersStratum";

    private static PaintingPlugin instance;

    public PaintingPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log(
                "[" + PaintingConstants.SERVER_NAME + "] Plugin chargé (v" + getManifest().getVersion() + ")");
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("[" + PaintingConstants.SERVER_NAME + "] Initialisation.");
        Class<?> superCls = LayersStratumOperation.class.getSuperclass();
        boolean superIsCanonicalToolOp = superCls == ToolOperation.class;
        ClassLoader opCl = ToolOperation.class.getClassLoader();
        ClassLoader bridgeCl = LayersStratumOperation.class.getClassLoader();
        LOGGER.atInfo().log("[" + PaintingConstants.SERVER_NAME + "] BuilderTools: "
                + "getSuperclass()==ToolOperation.class => " + superIsCanonicalToolOp
                + " ; CL ToolOperation=" + String.valueOf(opCl)
                + " LayersStratumCL=" + String.valueOf(bridgeCl));
        logToolOperationExecute0BridgeHint();
        ToolOperation.OPERATIONS.put(LAYERS_STRATUM_TOOL_ID, LayersStratumOperation::new);
        LOGGER.atInfo().log("[" + PaintingConstants.SERVER_NAME
                + "] BuilderTool '" + LAYERS_STRATUM_TOOL_ID + "' enregistré.");
        getCommandRegistry().registerCommand(new PrefabSaveFluidCommand());
        LOGGER.atInfo().log("[" + PaintingConstants.SERVER_NAME + "] Commande /prefabsavefluid enregistrée.");
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("[" + PaintingConstants.SERVER_NAME + "] " + PaintingConstants.SLOGAN);
    }

    @Override
    public void shutdown() {
        LOGGER.atInfo().log("[" + PaintingConstants.SERVER_NAME + "] Arrêt.");
        instance = null;
    }

    public static PaintingPlugin getInstance() {
        return instance;
    }

    /**
     * Si {@code execute0} n’est pas {@code public} sur le {@code ToolOperation} chargé par le serveur,
     * le bridge {@code LayersStratumOperation} (PluginClassLoader) ne peut pas override un membre
     * {@code package-private} → {@link java.lang.AbstractMethodError}. Le correctif est le JAR
     * <strong>early-bridge</strong> (ASM) dans {@code earlyplugins/}, pas le JAR du mod.
     */
    private static void logToolOperationExecute0BridgeHint() {
        try {
            Method m = ToolOperation.class.getDeclaredMethod("execute0", int.class, int.class, int.class);
            int mod = m.getModifiers();
            if (Modifier.isPublic(mod)) {
                LOGGER.atInfo().log("[" + PaintingConstants.SERVER_NAME
                        + "] ToolOperation#execute0 est public (early-bridge actif ou API serveur elargie).");
                return;
            }
            LOGGER.atSevere().log("[" + PaintingConstants.SERVER_NAME + "] ERREUR CONFIG: ToolOperation#execute0 "
                    + "n'est pas public (modifiers=0x" + Integer.toHexString(mod) + "). "
                    + "Ne pas mettre hyzalia-paint-*.jar dans earlyplugins/ (le log [EarlyPlugin] Found doit montrer "
                    + "HyzaliaPaint-EarlyBridge-execute0-*.jar). Copier depuis early-bridge/build/libs/ "
                    + "le JAR HyzaliaPaint-EarlyBridge-execute0-*.jar seul dans earlyplugins/, "
                    + "garder le mod dans mods/, lancer avec --accept-early-plugins.");
        } catch (ReflectiveOperationException e) {
            LOGGER.atWarning().log("[" + PaintingConstants.SERVER_NAME
                    + "] Impossible d'inspecter ToolOperation#execute0: " + e.getMessage());
        }
    }
}
