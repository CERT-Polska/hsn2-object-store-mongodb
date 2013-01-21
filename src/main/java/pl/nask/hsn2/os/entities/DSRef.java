/*
 * Copyright (c) NASK, NCSC
 * 
 * This file is part of HoneySpider Network 2.0.
 * 
 * This is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.nask.hsn2.os.entities;

import pl.nask.hsn2.protobuff.Object.Reference;

public final class DSRef {

    private final long key;
    private final int store;

    public DSRef(Reference reference) {
        this.key = reference.getKey();
        this.store = reference.getStore();
    }
    public long getKey() {
        return key;
    }

    public int getStore() {
        return store;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (key ^ (key >>> 32));
        result = prime * result + store;
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DSRef other = (DSRef) obj;
        if (key != other.key)
            return false;
        if (store != other.store)
            return false;
        return true;
    }
    public Reference asReference() {
        return Reference.newBuilder().setKey(key).setStore(store).build();
    }


}
