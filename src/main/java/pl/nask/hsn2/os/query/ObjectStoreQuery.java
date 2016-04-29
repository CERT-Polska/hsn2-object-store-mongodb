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

import java.util.ArrayList;
import java.util.List;

import pl.nask.hsn2.protobuff.ObjectStore.ObjectRequest;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectRequest.QueryStructure;

import com.mongodb.BasicDBObject;

public class ObjectStoreQuery {

	private List<Condition> conditions = new ArrayList<Condition>();

	public ObjectStoreQuery(List<QueryStructure> queryList) {
		for(QueryStructure queryStructure : queryList){
			switch (queryStructure.getType()) {
				case BY_ATTR_NAME:
					conditions.add(new NameCondition(queryStructure));
					break;
				case BY_ATTR_VALUE:
					conditions.add(new ValueCondition(queryStructure));
					break;
				default:
					throw new IllegalArgumentException("Unsupported query type: " + queryStructure.getType());
			}
		}
	}

	public final BasicDBObject filter(ObjectRequest request) {
		BasicDBObject result = new BasicDBObject();
		doFiltrate(result);
		return result;
	}

	private void doFiltrate(BasicDBObject ob){
		for (Condition condition : conditions) {
			condition.updateQuery(ob);
		}
	}
}
