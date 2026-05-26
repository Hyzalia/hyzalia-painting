package com.hyzalia.paint.commands.stratum;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/** Commandes de réglage fin des bornes Y du brush {@code LayersStratum}. */
public final class StratumCommand extends AbstractCommandCollection {

    public StratumCommand() {
        super("stratum", "server.hyzalia.paint.stratum.desc");
        addAliases("strat");
        setPermissionGroups("hytale:WorldEditor");
        addSubCommand(new StratumTopYCommand());
        addSubCommand(new StratumBottomYCommand());
        addSubCommand(new StratumShowCommand());
    }
}
