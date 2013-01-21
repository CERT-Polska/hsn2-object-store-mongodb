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

public final class IntAttribute extends EntityAttribute<Integer> {

    private final int value;

    public IntAttribute(String name, int value){
    	super(name);
    	this.value = value;
    }
    public IntAttribute(Attribute attribute) {
        super(attribute);
        this.value = attribute.getDataInt();
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    protected void setValue(Builder builder) {
        builder.setDataInt(getValue());
    }

    @Override
    protected Type getType() {
        return Type.INT;
    }
}
