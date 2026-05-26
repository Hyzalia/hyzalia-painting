package com.hyzalia.paint.commands.stratum;

/** {@code /stratum bottom [y]} — définit {@code zbStratumBottomY} sur le brush LayersStratum tenu en main. */
public final class StratumBottomYCommand extends StratumYCommands.AbstractStratumYCommand {

    public StratumBottomYCommand() {
        super("bottom", "server.hyzalia.paint.stratum.bottom.desc");
    }

    @Override
    protected boolean setsTop() {
        return false;
    }
}
