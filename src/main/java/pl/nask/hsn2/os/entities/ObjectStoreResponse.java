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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import pl.nask.hsn2.protobuff.Object.ObjectData;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectResponse;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectResponse.Builder;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectResponse.ResponseType;

public class ObjectStoreResponse {
	private List<ObjectData> data = new ArrayList<ObjectData>();
	private List<Long> missing = new ArrayList<Long>();
	private List<Long> ids = new ArrayList<Long>();
	private List<Long> conflicts = new ArrayList<Long>();
	private ResponseType type;

	public ObjectStoreResponse(ResponseType type) {
		this.type = type;
	}

	public ObjectStoreResponse(Collection<Long> dataIds) {
		ids.addAll(dataIds);
	}

	public void addId(Long id){
		ids.add(id);
	}

	public ResponseType getType() {
		return type;
	}

	public void setType(ResponseType type) {
		this.type = type;
	}

	public void addMissing(Long id) {
		missing.add(id);
	}

	public void addData(ObjectData objectData) {
		data.add(objectData);
	}

	public void addConflict(Long id) {
		this.conflicts.add(id);
	}

	public ObjectResponse toObjectResponse() {
		Builder builder = ObjectResponse.newBuilder().setType(type);
		switch (type) {
			case PARTIAL_GET:
				builder.addAllMissing(missing);
			case SUCCESS_GET:
				builder.addAllData(data);
				break;

			case PARTIAL_UPDATE:
				builder.addAllMissing(missing);
			case SUCCESS_UPDATE:
				builder.addAllConflicts(conflicts);
				break;

			case SUCCESS_PUT:
			case SUCCESS_QUERY:
				builder.addAllObjects(ids);
				break;
			default:
				throw new IllegalStateException("Invalid type of ObjectResponse!");
		}
		return builder.build();
	}

	public Collection<Long> getIds() {
		return ids;
	}

	public void addAllIds(Collection<? extends Long> ids){
		this.ids.addAll(ids);
	}
}
