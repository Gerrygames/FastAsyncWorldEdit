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

package com.sk89q.worldedit.registry.state;

import com.fastasyncworldedit.core.registry.state.PropertyKey;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Describes a state property of a block.
 *
 * <p>Example states include "variant" (indicating material or type) and
 * "facing" (indicating orientation).</p>
 */
public interface Property<T> {

    /**
     * Returns the name of this state.
     *
     * @return The state name
     */
    String getName();

    /**
     * Return a list of available values for this state.
     *
     * @return the list of state values
     */
    List<T> getValues();

    /**
     * Gets the value for the given string, or null.
     *
     * @param string The string
     * @return The value, or null
     * @throws IllegalArgumentException When the value is invalid.
     */
    @Nullable
    T getValueFor(String string) throws IllegalArgumentException;

    //FAWE start
    default int getIndex(T value) {
        return getValues().indexOf(value);
    }

    default int getIndexFor(CharSequence string) throws IllegalArgumentException {
        return getIndex(getValueFor(string.toString()));
    }

    default PropertyKey getKey() {
        return PropertyKey.getOrCreate(getName());
    }
    //FAWE end
}
