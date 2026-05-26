package com.hyzalia.paint.commands.stratum;

/** {@code /stratum top [y]} — définit {@code zaStratumTopY} sur le brush LayersStratum tenu en main. */
public final class StratumTopYCommand extends StratumYCommands.AbstractStratumYCommand {

    public StratumTopYCommand() {
        super("top", "server.hyzalia.paint.stratum.top.desc");
    }

    @Override
    protected boolean setsTop() {
        return true;
    }
}
