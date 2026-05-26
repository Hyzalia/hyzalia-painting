package com.hyzalia.paint;

import com.hypixel.hytale.builtin.buildertools.tooloperations.LayersStratumOperation;
import com.hypixel.hytale.builtin.buildertools.tooloperations.ToolOperation;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hyzalia.paint.commands.stratum.StratumCommand;
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
        logToolOperationExecuteBlockBridgeHint();
        ToolOperation.OPERATIONS.put(LAYERS_STRATUM_TOOL_ID, LayersStratumOperation::new);
        LOGGER.atInfo().log("[" + PaintingConstants.SERVER_NAME
                + "] BuilderTool '" + LAYERS_STRATUM_TOOL_ID + "' enregistré.");
        getCommandRegistry().registerCommand(new PrefabSaveFluidCommand());
        LOGGER.atInfo().log("[" + PaintingConstants.SERVER_NAME + "] Commande /prefabsavefluid enregistrée.");
        getCommandRegistry().registerCommand(new StratumCommand());
        LOGGER.atInfo().log("[" + PaintingConstants.SERVER_NAME + "] Commandes /stratum enregistrées.");
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
     * Diagnostic au démarrage : depuis la prerelease, {@code ToolOperation#executeBlock} est
     * {@code protected} (remplace {@code execute0}). Une sous-classe dans le même package logique
     * peut l’overrider sans early-bridge ; le JAR early-bridge reste optionnel pour compatibilité.
     */
    private static void logToolOperationExecuteBlockBridgeHint() {
        try {
            Method m = ToolOperation.class.getDeclaredMethod("executeBlock", int.class, int.class, int.class);
            int mod = m.getModifiers();
            if (Modifier.isProtected(mod) || Modifier.isPublic(mod)) {
                LOGGER.atInfo().log("[" + PaintingConstants.SERVER_NAME
                        + "] ToolOperation#executeBlock est accessible (protected/public).");
                return;
            }
            LOGGER.atSevere().log("[" + PaintingConstants.SERVER_NAME + "] ERREUR CONFIG: ToolOperation#executeBlock "
                    + "n'est ni protected ni public (modifiers=0x" + Integer.toHexString(mod) + "). "
                    + "Vérifier early-bridge (HyzaliaPaint-EarlyBridge-execute0-*.jar dans earlyplugins/) "
                    + "et lancer avec --accept-early-plugins.");
        } catch (ReflectiveOperationException e) {
            LOGGER.atWarning().log("[" + PaintingConstants.SERVER_NAME
                    + "] Impossible d'inspecter ToolOperation#executeBlock: " + e.getMessage());
        }
    }
}
