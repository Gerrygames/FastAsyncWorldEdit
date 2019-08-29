/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.command;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.brush.BrushSettings;
import com.boydti.fawe.object.brush.TargetMode;
import com.boydti.fawe.object.brush.scroll.ScrollAction;
import com.boydti.fawe.object.brush.visualization.VisualMode;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.StringMan;
import com.google.common.collect.Iterables;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.command.argument.Arguments;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.annotation.Range;
import com.sk89q.worldedit.internal.command.CommandArgParser;
import com.sk89q.worldedit.util.HandSide;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.Switch;

import java.util.List;

/**
 * Tool commands.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class ToolUtilCommands {
    private final WorldEdit we;

    public ToolUtilCommands(WorldEdit we) {
        this.we = we;
    }

    @Command(
            name = "mask",
            aliases = {"/mask"},
            desc = "Set the brush destination mask"
    )
    @CommandPermissions({"worldedit.brush.options.mask", "worldedit.mask.brush"})
    public void mask(Player player, LocalSession session,
                     @Switch(name = 'h', desc = "TODO")
                             boolean offHand,
                     @Arg(desc = "The destination mask", def = "")
                             Mask maskOpt,
                     Arguments arguments)
            throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.s());
            return;
        }
        if (maskOpt == null) {
            BBC.BRUSH_MASK_DISABLED.send(player);
            tool.setMask(null);
            return;
        }
        BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
        String lastArg = Iterables.getLast(CommandArgParser.spaceSplit(arguments.get())).getSubstring();
        System.out.println(lastArg + " TODO check this is not the whole command");
        settings.addSetting(BrushSettings.SettingType.MASK, lastArg);
        settings.setMask(maskOpt);
        tool.update();
        BBC.BRUSH_MASK.send(player);
    }

    @Command(
            name = "material",
            aliases = {"mat", "/material", "pattern"},
            desc = "Set the brush material"
    )
    @CommandPermissions("worldedit.brush.options.material")
    public void material(Player player, LocalSession session,
                         @Arg(desc = "brush material pattern", def = "") Pattern patternOpt,
                         @Switch(name = 'h', desc = "TODO")
                                 boolean offHand,
                         Arguments arguments) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.s());
            return;
        }
        if (patternOpt == null) {
            BBC.BRUSH_MATERIAL.send(player);
            tool.setFill(null);
            return;
        }
        BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
        settings.setFill(patternOpt);
        String lastArg = Iterables.getLast(CommandArgParser.spaceSplit(arguments.get())).getSubstring();
        settings.addSetting(BrushSettings.SettingType.FILL, lastArg);
        tool.update();
        BBC.BRUSH_MATERIAL.send(player);
    }

    @Command(
            name = "range",
            desc = "Set the brush range"
    )
    @CommandPermissions("worldedit.brush.options.range")
    public void range(Player player, LocalSession session,
                      @Arg(desc = "Range")
                              int range) throws WorldEditException {
        range = Math.max(0, Math.min(256, range));
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.s());
            return;
        }
        tool.setRange(range);
        BBC.BRUSH_RANGE.send(player);
    }

    @Command(
            name = "size",
            desc = "Set the brush size"
    )
    @CommandPermissions("worldedit.brush.options.size")
    public void size(Player player, LocalSession session,
                     @Arg(desc = "The size of the brush", def = "5")
                             int radius,
                     @Switch(name = 'h', desc = "TODO")
                             boolean offHand) throws WorldEditException {
        we.checkMaxBrushRadius(radius);
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.s());
            return;
        }
        BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
        settings.setSize(radius);
        tool.update();
        BBC.BRUSH_SIZE.send(player);
    }

    @Command(
        name = "tracemask",
        aliases = {"tarmask", "tm", "targetmask"},
        desc = "Set the mask used to stop tool traces"
    )
    @CommandPermissions("worldedit.brush.options.tracemask")
    public void traceMask(Player player, LocalSession session,
                          @Arg(desc = "The trace mask to set", def = "")
                             Mask maskOpt) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            BBC.BRUSH_NONE.send(player);
            return;
        }
        tool.setTraceMask(maskOpt);
        BBC.BRUSH_TARGET_MASK_SET.send(player, maskOpt.toString());
    }

    @Command(
            name = "none",
            aliases = {"/none"},
            desc = "Unbind a bound tool from your current item"
    )
    public void none(Player player, LocalSession session) throws WorldEditException {
        session.setTool(player, null);
        BBC.TOOL_NONE.send(player);
    }

    @Command(
            name = "/",
            aliases = {","},
            desc = "Toggle the super pickaxe function"
    )
    @CommandPermissions("worldedit.superpickaxe")
    public void togglePickaxe(Player player, LocalSession session,
                              @Arg(desc = "state", def = "on") String state) throws WorldEditException {
        if (session.hasSuperPickAxe()) {
            if ("on".equals(state)) {
                BBC.SUPERPICKAXE_ENABLED.send(player);
                return;
            }

            session.disableSuperPickAxe();
            BBC.SUPERPICKAXE_DISABLED.send(player);
        } else {
            if ("off".equals(state)) {

                BBC.SUPERPICKAXE_DISABLED.send(player);
                return;
            }
            session.enableSuperPickAxe();
            BBC.SUPERPICKAXE_ENABLED.send(player);
        }
    }

    @Command(
            name = "primary",
            desc = "Set the right click brush",
            descFooter = "Set the right click brush"
    )
    @CommandPermissions("worldedit.brush.primary")
    public void primary(Player player, LocalSession session,
                        @Arg(desc = "The brush command", variable = true) List<String> commandStr) throws WorldEditException {
        BaseItem item = player.getItemInHand(HandSide.MAIN_HAND);
        BrushTool tool = session.getBrushTool(player, false);
        session.setTool(item, null, player);
        String cmd = "brush " + StringMan.join(commandStr, " ");
        CommandEvent event = new CommandEvent(player, cmd);
        PlatformCommandManager.getInstance().handleCommandOnCurrentThread(event);
        BrushTool newTool = session.getBrushTool(item, player, false);
        if (newTool != null && tool != null) {
            newTool.setSecondary(tool.getSecondary());
        }
    }

    @Command(
            name = "secondary",
            desc = "Set the left click brush",
            descFooter = "Set the left click brush"
    )
    @CommandPermissions("worldedit.brush.secondary")
    public void secondary(Player player, LocalSession session,
                          @Arg(desc = "The brush command", variable = true) List<String> commandStr)
            throws WorldEditException {
        BaseItem item = player.getItemInHand(HandSide.MAIN_HAND);
        BrushTool tool = session.getBrushTool(player, false);
        session.setTool(item, null, player);
        String cmd = "brush " + StringMan.join(commandStr, " ");
        CommandEvent event = new CommandEvent(player, cmd);
        PlatformCommandManager.getInstance().handleCommandOnCurrentThread(event);
        BrushTool newTool = session.getBrushTool(item, player, false);
        if (newTool != null && tool != null) {
            newTool.setPrimary(tool.getPrimary());
        }
    }

    @Command(
            name = "visualize",
            aliases = {"visual", "vis"},
            desc = "Toggle between different visualization modes",
            descFooter = "Toggle between different visualization modes\n" +
                    "0 = No visualization\n" +
                    "1 = Single block at target position\n" +
                    "2 = Glass showing what blocks will be changed"
    )
    @CommandPermissions("worldedit.brush.visualize")
    public void visual(Player player, LocalSession session, @Arg(name = "mode", desc = "int", def = "0") @Range(min = 0, max = 2) int mode)
            throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            BBC.BRUSH_NONE.send(player);
            return;
        }
        VisualMode[] modes = VisualMode.values();
        VisualMode newMode = modes[MathMan.wrap(mode, 0, modes.length - 1)];
        tool.setVisualMode(player, newMode);
        BBC.BRUSH_VISUAL_MODE_SET.send(player, newMode);
    }

    @Command(
            name = "target",
            aliases = {"tar"},
            desc = "Toggle between different target modes"
    )
    @CommandPermissions("worldedit.brush.target")
    public void target(Player player, LocalSession session,
                       @Arg(name = "mode", desc = "int", def = "0") int mode) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            BBC.BRUSH_NONE.send(player);
            return;
        }
        TargetMode[] modes = TargetMode.values();
        TargetMode newMode = modes[MathMan.wrap(mode, 0, modes.length - 1)];
        tool.setTargetMode(newMode);
        BBC.BRUSH_TARGET_MODE_SET.send(player, newMode);
    }

    @Command(
            name = "targetoffset",
            aliases = {"to"},
            desc = "Set the targeting mask"
    )
    @CommandPermissions("worldedit.brush.targetoffset")
    public void targetOffset(Player player, EditSession editSession, LocalSession session,
                             int offset) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            BBC.BRUSH_NONE.send(player);
            return;
        }
        tool.setTargetOffset(offset);
        BBC.BRUSH_TARGET_OFFSET_SET.send(player, offset);
    }

    @Command(
            name = "scroll",
            desc = "Toggle between different target modes"
    )
    @CommandPermissions("worldedit.brush.scroll")
    public void scroll(Player player, EditSession editSession, LocalSession session,
                       @Switch(name = 'h', desc = "TODO")
                               boolean offHand,
                       @Arg(desc = "Target Modes")
                               String modes,
                       @Arg(desc = "The scroll action", variable = true)
                               List<String> commandStr) throws WorldEditException {
        // TODO NOT IMPLEMENTED Convert ScrollAction to an argument converter
        BrushTool bt = session.getBrushTool(player, false);
        if (bt == null) {
            BBC.BRUSH_NONE.send(player);
            return;
        }
        BrushSettings settings = offHand ? bt.getOffHand() : bt.getContext();
        ScrollAction action = ScrollAction.fromArguments(bt, player, session, StringMan.join(commandStr, " "), true);
        settings.setScrollAction(action);
        if (modes.equalsIgnoreCase("none")) {
            BBC.BRUSH_SCROLL_ACTION_UNSET.send(player);
        } else if (action != null) {
            settings.addSetting(BrushSettings.SettingType.SCROLL_ACTION, modes);
            BBC.BRUSH_SCROLL_ACTION_SET.send(player, modes);
        }
        bt.update();
    }



    @Command(
            name = "smask",
            aliases = {"/smask", "/sourcemask", "sourcemask"},
            desc = "Set the brush source mask",
            descFooter = "Set the brush source mask"
    )
    @CommandPermissions({"worldedit.brush.options.mask", "worldedit.mask.brush"})
    public void smask(Player player, LocalSession session, EditSession editSession,
                      @Arg(desc = "The destination mask", def = "")
                              Mask maskArg,
                      @Switch(name = 'h', desc = "TODO")
                              boolean offHand,
                      Arguments arguments) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.s());
            return;
        }
        if (maskArg == null) {
            BBC.BRUSH_SOURCE_MASK_DISABLED.send(player);
            tool.setSourceMask(null);
            return;
        }
        BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
        String lastArg = Iterables.getLast(CommandArgParser.spaceSplit(arguments.get())).getSubstring();
        settings.addSetting(BrushSettings.SettingType.SOURCE_MASK, lastArg);
        settings.setSourceMask(maskArg);
        tool.update();
        BBC.BRUSH_SOURCE_MASK.send(player);
    }

    @Command(
            name = "transform",
            desc = "Set the brush transform"
    )
    @CommandPermissions({"worldedit.brush.options.transform", "worldedit.transform.brush"})
    public void transform(Player player, LocalSession session, EditSession editSession,
                          @Arg(desc = "The transform", def = "") ResettableExtent transform,
                          @Switch(name = 'h', desc = "TODO")
                                  boolean offHand,
                          Arguments arguments) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.s());
            return;
        }
        if (transform == null) {
            BBC.BRUSH_TRANSFORM_DISABLED.send(player);
            tool.setTransform(null);
            return;
        }
        BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
        String lastArg = Iterables.getLast(CommandArgParser.spaceSplit(arguments.get())).getSubstring();
        settings.addSetting(BrushSettings.SettingType.TRANSFORM, lastArg);
        settings.setTransform(transform);
        tool.update();
        BBC.BRUSH_TRANSFORM.send(player);
    }
}
