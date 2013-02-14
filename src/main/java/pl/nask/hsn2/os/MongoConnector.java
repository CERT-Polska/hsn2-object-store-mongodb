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

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.os.entities.MongoEntity;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;

public class MongoConnector {
	private DB db;

	private final BasicDBObject counterQuery = new BasicDBObject("_id", "objId");
	private final BasicDBObject counterUpdate = new BasicDBObject("$inc", new BasicDBObject("counter", 1));
	private static volatile MongoConnector instance;

	private final static Logger LOGGER = LoggerFactory.getLogger(ObjectStore.class);

	private final static WriteConcern writeConcern = WriteConcern.SAFE;
	
	private Map<Long, DBCollection> collections = Collections.synchronizedMap(new HashMap<Long, DBCollection>());

	private MongoConnector(String host, int port) throws IOException {
		Mongo mongo = null;
		try {
			mongo = new Mongo(host, port);
			mongo.dropDatabase("object-store");
			db = mongo.getDB("object-store");
		} catch (UnknownHostException e) {
			LOGGER.error("MongoDB host is unreachable: {}", e.getMessage());
			throw new IOException("MongoDB is unreachable", e);
		} catch (MongoException.Network e) {
			LOGGER.error("Couldn't connect to MongoDB: {}", e.getMessage());
			throw new IOException("MongoDB is unreachable", e);
		}
	}

	public static void initConnection(String host, int port) throws IOException {
		if (instance == null){
			instance = new MongoConnector(host, port);
			LOGGER.info("Connected with MongoDB on {}:{}", host, port);
		}
		else{
			throw new IllegalStateException("MongoConnector is already initialized!");
		}
	}

	public static MongoConnector getInstance() {
		if(instance != null){
			return instance;
		}
		else{
			throw new IllegalStateException("MongoConnector isn't initialized!");
		}
	}

	public MongoEntity getObjById(long jobId, Long objId) {
		MongoEntity me = new MongoEntity();
		me.put("_id", objId);
		DBObject objFound = this.getCollectionForJob(jobId).findOne(me);
		if (objFound == null) {
			return null;
		} else {
			return MongoEntity.fromDBObject(objFound);
		}
	}

	public void putList(long jobId, ArrayList<DBObject> list) {
		this.getCollectionForJob(jobId).insert(list, writeConcern);
	}

	public ArrayList<BasicDBObject> getObjById(long jobId, Iterable<Long> objIds) {
		BasicDBObject query = new BasicDBObject();
		ArrayList<BasicDBObject> result = new ArrayList<BasicDBObject>();

		query.put("_id", new BasicDBObject("$in", objIds));
		query.put("job_id", jobId);
		DBCursor cur = this.getCollectionForJob(jobId).find(query);
		while (cur.hasNext()) {
			result.add((BasicDBObject)cur.next());
		}
		return result;
	}

	public void saveObject(long jobId, BasicDBObject object) {
		this.getCollectionForJob(jobId).save(object, writeConcern);
	}

	public long getNextObjId() {
		DBCollection counters = db.getCollection("counters");
		BasicDBObject result = (BasicDBObject) counters.findAndModify(counterQuery, new BasicDBObject(), new BasicDBObject(), false, counterUpdate, true, true);
		return result.getLong("counter");
	}

	public Set<Long> executeQuery(long jobId, BasicDBObject query) {
		DBCursor cur = this.getCollectionForJob(jobId).find(query);
		Set<Long> result = new HashSet<Long>();
		while (cur.hasNext())
			result.add(Long.parseLong(cur.next().get("_id").toString()));
		return result;
	}

	public void removeByJobId(long jobId) {
		long time = System.currentTimeMillis();
		DBCollection removedCollection = collections.remove(jobId);
		removedCollection.drop();
		time = System.currentTimeMillis() - time;
		LOGGER.debug("Job {} data removed in {} ms", jobId, time);
	}

	/**
	 * Get MongoDB collection for current job. Create new if absent.
	 * 
	 * @param jobId
	 *            Job ID.
	 * @return Collection for current job.
	 */
	private DBCollection getCollectionForJob(long jobId) {
		synchronized (collections) {
			DBCollection tempCollection = collections.get(jobId);
			if (tempCollection == null) {
				tempCollection = db.createCollection("job" + jobId, null);
				tempCollection.ensureIndex(new BasicDBObject("parent", 1));
				collections.put(jobId, tempCollection);
			}
			return tempCollection;
		}
	}
}
