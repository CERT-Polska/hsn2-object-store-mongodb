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

package pl.nask.hsn2.os;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.os.entities.EntityAttribute;
import pl.nask.hsn2.os.entities.MongoEntity;
import pl.nask.hsn2.os.entities.ObjectStoreResponse;
import pl.nask.hsn2.os.query.ObjectStoreQuery;
import pl.nask.hsn2.protobuff.Object.Attribute;
import pl.nask.hsn2.protobuff.Object.Attribute.Builder;
import pl.nask.hsn2.protobuff.Object.Attribute.Type;
import pl.nask.hsn2.protobuff.Object.ObjectData;
import pl.nask.hsn2.protobuff.Object.Reference;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectRequest;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectResponse.ResponseType;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class ObjectStore {
	private final static Logger LOGGER = LoggerFactory.getLogger(ObjectStore.class);
	private static MongoConnector mongoConnector = MongoConnector.getInstance();
	ExecutorService cleanupService = Executors.newSingleThreadExecutor(new DaemonThreadFactory());
	private final static ObjectStore OS = new ObjectStore();
	
	private ObjectStore() {
	}

	public ObjectStoreResponse addObject(ObjectRequest objectRequest) {
		long jobId = objectRequest.getJob();
		long startTime = System.currentTimeMillis();
		ArrayList<DBObject> list = new ArrayList<DBObject>();
		ObjectStoreResponse response = new ObjectStoreResponse(ResponseType.SUCCESS_PUT);

		for (ObjectData objectData : objectRequest.getDataList()) {
			MongoEntity newObject = prepareNewObject(jobId, objectData);
			
			list.add(newObject);
			response.addId(newObject.getLong("_id"));
		}

		mongoConnector.putList(list);
		LOGGER.debug("Objects passed.{}",response.getIds().toString());
		LOGGER.debug("Count: {}, Addition time: {}", response.getIds().size(), System.currentTimeMillis() - startTime);
		return response;
	}

	private MongoEntity prepareNewObject(long jobId, ObjectData objectData){
		long objId = mongoConnector.getNextObjId();
		MongoEntity newObject = prepareObject(jobId, objId, objectData);
		newObject.addCreationTime();
		newObject.addInitialAttributes();
		return newObject;
	}
	
	public ObjectStoreResponse importObject(ObjectRequest objectRequest){
		long jobId = objectRequest.getJob();
		
		ObjectStoreResponse response = new ObjectStoreResponse(ResponseType.SUCCESS_PUT);
		ArrayList<DBObject> list = new ArrayList<DBObject>();

		for (ObjectData objectData : objectRequest.getDataList()) {
			long objId = objectData.getId();
			MongoEntity entity = prepareObject(jobId, objId, objectData);
			list.add(entity);
			response.addId(objId);
		}
		mongoConnector.putList(list);
		return response;
	}

	private MongoEntity prepareObject(long jobId, long objId, ObjectData objectData){
		MongoEntity newObject = new MongoEntity();
		
		newObject.put("_id", objId);
		newObject.put("job_id", jobId);
		for (Attribute attribute : objectData.getAttrsList()) {
			newObject.put(attribute.getName(), EntityAttribute.instanceFor(attribute).getValue());
		}
		return newObject;
	}

	public ObjectStoreResponse updateObject(ObjectRequest objectRequest) {

		try {			
			ObjectStoreResponse response = new ObjectStoreResponse(ResponseType.SUCCESS_UPDATE);
			List<ObjectData> dataList = objectRequest.getDataList();
			boolean overwrite = objectRequest.getOverwrite();
			
			ArrayList<Long> ids = new ArrayList<Long>();
			for (ObjectData objectData : dataList){
				ids.add(objectData.getId());
			}
			
			ArrayList<BasicDBObject> resultGet = mongoConnector.getObjById(objectRequest.getJob(), ids);
			if (resultGet.size() < dataList.size()){
				response.setType(ResponseType.PARTIAL_UPDATE);
			}

			for (ObjectData obj : dataList) {
				BasicDBObject tmp = null;
				for (BasicDBObject dbObject : resultGet) {
					if (Long.parseLong(dbObject.getString("_id")) == obj.getId()) {
						tmp = dbObject;
						break;
					}
				}

				if (tmp == null) {
					response.addMissing(obj.getId());
				}
				else{
					for (Attribute attr : obj.getAttrsList()) {
						EntityAttribute eattr = EntityAttribute.instanceFor(attr);
						if (tmp.containsField(attr.getName())) {
							response.addConflict(Long.parseLong(tmp.getString("_id")));
							if (overwrite) {
								tmp.put(attr.getName(), eattr.getValue());
							}
						}
						else {
							tmp.put(attr.getName(), eattr.getValue());
						}
					}
					mongoConnector.saveObject(tmp);
				}
			}

			return response;
		}
		catch (IllegalStateException e) {
			LOGGER.error("Object request: {}", objectRequest);
			throw e;
		}
	}

	private Builder getBuilder(String key, BasicDBObject objectFound) {
		Object value = objectFound.get(key);
		Builder builder  = Attribute.newBuilder().setName(key);

		
		if (value instanceof Integer || key.equalsIgnoreCase("depth")) {
			builder.setType(Type.INT).setDataInt(objectFound.getInt(key));
		}
		else if (value instanceof Long) {
			builder.setType(Type.OBJECT).setDataObject(objectFound.getLong(key));
		}
		else if (value instanceof BasicDBObject) {
			
			BasicDBObject mongoObj = (BasicDBObject) value;
			if (mongoObj.get("type").equals("TIME")) {
				builder.setType(Type.TIME).setDataTime(mongoObj.getLong("time"));
			}
			else if (mongoObj.get("type").equals("BYTES")) {
				builder.setType(Type.BYTES).setDataBytes(Reference.newBuilder().setKey(mongoObj.getLong("key")).setStore(mongoObj.getInt("store")).build());
			}
		}
		else if (value instanceof Boolean) {
			builder.setType(Type.BOOL).setDataBool(objectFound.getBoolean(key));
		}
		else {
			builder.setType(Type.STRING).setDataString(objectFound.getString(key));
		}
		return builder;
	}


	public ObjectStoreResponse getObject(ObjectRequest objectRequest) {
		Iterable<Long> ids = objectRequest.getObjectsList();
		ArrayList<BasicDBObject> result = mongoConnector.getObjById(objectRequest.getJob(), ids);
		ObjectStoreResponse response = new ObjectStoreResponse(ResponseType.SUCCESS_GET);
		ObjectData od;
		ArrayList<Long> helpList = new ArrayList<Long>();
		for (Long id : ids)
			helpList.add(id);

		List<Attribute> attrs;
		for (BasicDBObject objectFound : result) {
			attrs = new ArrayList<Attribute>();
			for (String key : objectFound.keySet()) {
				attrs.add(this.getBuilder(key, objectFound).build());
			}

			od = ObjectData.newBuilder()
					.setId(Long.parseLong(objectFound.get("_id").toString()))
					.addAllAttrs(attrs)
					.build();
			response.addData(od);
			helpList.remove(objectFound.get("_id"));
		}

		for (Long id : helpList) {
			response.addMissing(id);
			response.setType(ResponseType.PARTIAL_GET);
		}

		return response;
	}

	public ObjectStoreResponse query(ObjectRequest objectRequest) {

		ObjectStoreQuery objectStoreQuery = new ObjectStoreQuery(objectRequest.getQueryList());
		BasicDBObject queryObject = objectStoreQuery.filter(objectRequest);
		Set<Long> ids = mongoConnector.executeQuery(queryObject);

		ObjectStoreResponse objectStoreResponse = new ObjectStoreResponse(ResponseType.SUCCESS_QUERY);
		objectStoreResponse.addAllIds(ids);
		return objectStoreResponse;
	}

	public void removeJobData(final long jobId) {
		
		cleanupService.submit(new Runnable() {
			@Override
			public void run() {
				mongoConnector.removeByJobId(jobId);
			}
		});
		
	}	
	
	private class DaemonThreadFactory implements ThreadFactory {

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setDaemon(true);
			t.setName("Job Cleanup");
			return t;
		}
	}
	
	public static ObjectStore getInstance(){
		return OS;
	}
}
