package com.hyzalia.paint.commands.paste;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/** Commandes de gestion de l'outil paste multi-prefab Hyzalia. */
public final class HyzaliaPasteCommand extends AbstractCommandCollection {

    public HyzaliaPasteCommand() {
        super("hyzaliapaste", "server.hyzalia.paint.paste.desc");
        addAliases("hpaste");
        setPermissionGroups("hytale:WorldEditor");
        addSubCommand(new HyzaliaPasteAddCommand());
        addSubCommand(new HyzaliaPasteListCommand("list", "server.hyzalia.paint.paste.list.desc", false));
        addSubCommand(new HyzaliaPasteListCommand("manage", "server.hyzalia.paint.paste.manage.desc", true));
        addSubCommand(new HyzaliaPasteRemoveCommand());
        addSubCommand(new HyzaliaPasteWeightCommand());
    }
}
