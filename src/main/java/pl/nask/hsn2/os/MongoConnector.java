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
import java.util.HashSet;
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
import com.mongodb.WriteResult;

public class MongoConnector {
	private DBCollection collection;
	private DB db;

	private final BasicDBObject counterQuery = new BasicDBObject("_id", "objId");
	private final BasicDBObject counterUpdate = new BasicDBObject("$inc", new BasicDBObject("counter", 1));
	private static volatile MongoConnector instance;

	private final static Logger LOGGER = LoggerFactory.getLogger(ObjectStore.class);

	private final static WriteConcern writeConcern = WriteConcern.SAFE;

	private MongoConnector(String host, int port) throws IOException {
		Mongo mongo = null;
		try {
			mongo = new Mongo(host, port);
			mongo.dropDatabase("object-store");
			db = mongo.getDB("object-store");
			collection = db.createCollection("allJobsInOne", null);
			collection.ensureIndex(new BasicDBObject("parent", 1));	
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

	public MongoEntity getObjById(Long objId) {
		MongoEntity me = new MongoEntity();
		me.put("_id", objId);
		DBObject objFound = this.collection.findOne(me);
		if (objFound == null) {
			return null;
		} else {
			return MongoEntity.fromDBObject(objFound);
		}
	}

	public void putList(ArrayList<DBObject> list) {
		this.collection.insert(list, writeConcern);
	}

	public ArrayList<BasicDBObject> getObjById(long jobId, Iterable<Long> objIds) {
		BasicDBObject query = new BasicDBObject();
		ArrayList<BasicDBObject> result = new ArrayList<BasicDBObject>();

		query.put("_id", new BasicDBObject("$in", objIds));
		query.put("job_id", jobId);
		DBCursor cur = this.collection.find(query);
		while (cur.hasNext()) {
			result.add((BasicDBObject)cur.next());
		}
		return result;
	}

	public void saveObject(BasicDBObject object) {
		this.collection.save(object, writeConcern);
	}

	public long getNextObjId() {
		DBCollection counters = db.getCollection("counters");
		BasicDBObject result = (BasicDBObject) counters.findAndModify(counterQuery, new BasicDBObject(), new BasicDBObject(), false, counterUpdate, true, true);
		return result.getLong("counter");
	}

	public Set<Long> executeQuery(BasicDBObject query) {
		DBCursor cur = this.collection.find(query);
		Set<Long> result = new HashSet<Long>();
		while (cur.hasNext())
			result.add(Long.parseLong(cur.next().get("_id").toString()));
		return result;
	}
	
	public void removeByJobId(long jobId) {
		long start = System.currentTimeMillis();
		DBObject ref = new BasicDBObject();
		ref.put("job_id", jobId);
		int counter = 0;

		WriteResult res;
		while ((res = collection.remove(ref)).getN() != 0) {
			counter += res.getN();
		}
		long stop = System.currentTimeMillis();
		long time = (stop - start) / 1000;
		LOGGER.debug("Objects removed: {} in {} s", counter, time);
	}
}