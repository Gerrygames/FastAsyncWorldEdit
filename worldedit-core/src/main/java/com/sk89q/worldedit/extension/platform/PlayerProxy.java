/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.extension.platform;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.gamemode.GameMode;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

public class PlayerProxy extends AbstractPlayerActor {

    private final Player basePlayer;
    private final Actor permActor;
    private final Actor cuiActor;
    private final World world;
    //FAWE start
    private Vector3 offset = Vector3.ZERO;
    //FAWE end

    public PlayerProxy(Player player) {
        this(player, player, player, player.getWorld());
    }

    public PlayerProxy(Player basePlayer, Actor permActor, Actor cuiActor, World world) {
        super(basePlayer.getRawMeta());
        checkNotNull(basePlayer);
        checkNotNull(permActor);
        checkNotNull(cuiActor);
        checkNotNull(world);
        this.basePlayer = basePlayer;
        this.permActor = permActor;
        this.cuiActor = cuiActor;
        this.world = world;
    }

    @Override
    public UUID getUniqueId() {
        return basePlayer.getUniqueId();
    }

    @Override
    public BaseItemStack getItemInHand(HandSide handSide) {
        return basePlayer.getItemInHand(handSide);
    }

    @Override
    public void giveItem(BaseItemStack itemStack) {
        basePlayer.giveItem(itemStack);
    }

    @Override
    public BlockBag getInventoryBlockBag() {
        return basePlayer.getInventoryBlockBag();
    }

    @Override
    public String getName() {
        return basePlayer.getName();
    }

    @Override
    public String getDisplayName() {
        return basePlayer.getDisplayName();
    }

    @Override
    public BaseEntity getState() {
        throw new UnsupportedOperationException("Can't getState() on a player");
    }

    @Override
    public Location getLocation() {
        Location loc = this.basePlayer.getLocation();
        return new Location(loc.getExtent(), loc.add(offset), loc.getDirection());
    }

    @Override
    public boolean setLocation(Location location) {
        return basePlayer.setLocation(location);
    }

    @Override
    public boolean trySetPosition(Vector3 pos, float pitch, float yaw) {
        return basePlayer.trySetPosition(pos, pitch, yaw);
    }

    @Override
    public World getWorld() {
        return world == null ? basePlayer.getWorld() : world;
    }

    @Override
    public void printRaw(String msg) {
        basePlayer.print(TextComponent.of(msg));
    }

    @Override
    public void printDebug(String msg) {
        basePlayer.printDebug(TextComponent.of(msg));
    }

    @Override
    public void print(String msg) {
        basePlayer.printInfo(TextComponent.of(msg));
    }

    @Override
    public void printError(String msg) {
        basePlayer.printError(TextComponent.of(msg));
    }

    @Override
    public void print(Component component) {
        basePlayer.print(component);
    }

    @Override
    public String[] getGroups() {
        return permActor.getGroups();
    }

    @Override
    public boolean hasPermission(String perm) {
        return permActor.hasPermission(perm);
    }

    @Override
    public boolean togglePermission(String permission) {
        return permActor.togglePermission(permission);
    }

    @Override
    public void setPermission(String permission, boolean value) {
        permActor.setPermission(permission, value);
    }

    @Override
    public void dispatchCUIEvent(CUIEvent event) {
        cuiActor.dispatchCUIEvent(event);
    }

    @Override
    public void sendAnnouncements() {
        basePlayer.sendAnnouncements();
    }

    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        return basePlayer.getFacet(cls);
    }

    @Override
    public SessionKey getSessionKey() {
        return basePlayer.getSessionKey();
    }

    @Override
    public GameMode getGameMode() {
        return basePlayer.getGameMode();
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        basePlayer.setGameMode(gameMode);
    }

    @Override
    public <B extends BlockStateHolder<B>> void sendFakeBlock(BlockVector3 pos, B block) {
        basePlayer.sendFakeBlock(pos, block);
    }

    @Override
    public void floatAt(int x, int y, int z, boolean alwaysGlass) {
        basePlayer.floatAt(x, y, z, alwaysGlass);
    }

    @Override
    public Locale getLocale() {
        return basePlayer.getLocale();
    }

    //FAWE start
    public static Player unwrap(Player player) {
        if (player instanceof PlayerProxy) {
            return unwrap(((PlayerProxy) player).getBasePlayer());
        }
        return player;
    }

    public void setOffset(Vector3 position) {
        this.offset = position;
    }

    @Override
    public BaseBlock getBlockInHand(HandSide handSide) throws WorldEditException {
        return basePlayer.getBlockInHand(handSide);
    }

    @Override
    public boolean runAction(Runnable ifFree, boolean checkFree, boolean async) {
        return basePlayer.runAction(ifFree, checkFree, async);
    }

    @Override
    public void sendTitle(Component title, Component sub) {
        basePlayer.sendTitle(title, sub);
    }

    public Player getBasePlayer() {
        return basePlayer;
    }
    //FAWE end
}
