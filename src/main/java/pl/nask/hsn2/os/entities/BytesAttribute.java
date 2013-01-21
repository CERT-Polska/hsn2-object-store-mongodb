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
import pl.nask.hsn2.protobuff.Object.Reference;

import com.mongodb.BasicDBObject;

public final class BytesAttribute extends EntityAttribute<BasicDBObject> {

    private final BasicDBObject value;
    public BytesAttribute(Attribute attribute) {
        super(attribute);
        this.value = new BasicDBObject();
        this.value.put("key", attribute.getDataBytes().getKey());
        this.value.put("store", attribute.getDataBytes().getStore());
        this.value.put("type", "BYTES");
    }

    @Override
    public BasicDBObject getValue() {
        return value;
    }

    @Override
    protected void setValue(Builder builder) {
    	Reference ref = Reference.newBuilder().setKey(this.value.getLong("key")).setStore(this.value.getInt("store")).build();
    	builder.setDataBytes(ref);
    }

    @Override
    protected Type getType() {
        return Type.BYTES;
    }
}
