/*
 * Copyright (c) NASK, NCSC
 *
 * This file is part of HoneySpider Network 2.1.
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

package pl.nask.hsn2.os.query;

import pl.nask.hsn2.protobuff.Object.Attribute;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectRequest.QueryStructure;

import com.mongodb.BasicDBObject;

public class ValueCondition implements Condition {
	private Attribute attribute;
	private boolean negate;

	public ValueCondition(QueryStructure queryStructure) {
		attribute = queryStructure.getAttrValue();
		negate = queryStructure.getNegate();
	}

	@Override
	public final void updateQuery(BasicDBObject condList) {
		if (negate){
			condList.append(attribute.getName(), new BasicDBObject("$ne", getAttrValue()));
		}
		else {
			condList.append(attribute.getName(), getAttrValue());
		}
	}

	private Object getAttrValue() {
        switch (attribute.getType()) {
        case BOOL: return attribute.getDataBool();
        case BYTES: return attribute.getDataBytes();
        case FLOAT: return attribute.getDataFloat();
        case INT: return attribute.getDataInt();
        case OBJECT: return attribute.getDataObject();
        case STRING: return attribute.getDataString();
        case TIME: return attribute.getDataTime();
        case EMPTY:
        default:
            return null;
        }
    }
}
