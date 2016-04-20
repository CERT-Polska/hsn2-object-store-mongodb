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

import pl.nask.hsn2.protobuff.Object.Attribute;
import pl.nask.hsn2.protobuff.Object.Attribute.Builder;
import pl.nask.hsn2.protobuff.Object.Attribute.Type;

public abstract class EntityAttribute<T> {
    private final String name;

    public static EntityAttribute instanceFor(Attribute attribute) {
        if (attribute != null){
            switch (attribute.getType()) {
            case BOOL:
                return new BooleanAttribute(attribute);
            case BYTES:
                return new BytesAttribute(attribute);
            case EMPTY:
                return new EmptyAttribute(attribute);
            case FLOAT:
                return new FloatAttribute(attribute);
            case INT:
                return new IntAttribute(attribute);
            case OBJECT:
                return new ObjectAttribute(attribute);
            case STRING:
                return new StringAttribute(attribute);
            case TIME:
                return new TimeAttribute(attribute);
            default:
                throw new IllegalStateException("Invalid type of Attribute!");
            }
        } else {
            return null;
        }
    }

    protected EntityAttribute(Attribute attribute) {
        this.name = attribute.getName();
    }

    protected EntityAttribute(String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    public abstract T getValue();

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        T value = getValue();
        result = prime * result + (value == null ? 0 : value.hashCode());
        return result;
    }

    public final Attribute asAttribute() {
        Builder builder = Attribute.newBuilder().setName(name).setType(getType());
        setValue(builder);
        return builder.build();
    }

    protected abstract void setValue(Attribute.Builder builder);

    protected abstract Type getType();

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EntityAttribute other = (EntityAttribute) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        T value = getValue();
        if (value == null) {
            if (other.getValue() != null)
                return false;
        } else if (!value.equals(other.getValue()))
            return false;
        return true;
    }


}
