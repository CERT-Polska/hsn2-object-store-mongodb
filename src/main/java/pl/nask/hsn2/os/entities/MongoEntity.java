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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.os.MongoConnector;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class MongoEntity extends BasicDBObject implements DBObject {

	private static final Logger LOGGER = LoggerFactory.getLogger(MongoEntity.class);

	private static final long serialVersionUID = 5504028319157480190L;
	private MongoConnector mc;

	public MongoEntity() {
		mc = MongoConnector.getInstance();
	}

	public final void addInitialAttributes(long jobId) {
		if (this.containsField("parent")) {
			Long parentId = Long.parseLong(this.get("parent").toString());
			MongoEntity parentObj = mc.getObjById(jobId, parentId);

			if (parentObj != null) {
				this.put("top_ancestor", parentObj.get("top_ancestor"));
				this.put("depth", Long.parseLong(parentObj.get("depth").toString()) + 1);
			} else {
				LOGGER.error("Cannot find object for 'parent' key! ({})", this.get("parent"));
				LOGGER.debug("Details of the error: {}", this);
			}
		}
		else {
			this.put("top_ancestor", this.get("_id"));
			this.put("depth", 0);
		}
	}

	public final void addCreationTime() {
		if (!this.containsField("creation_time"))
			this.put("creation_time", System.currentTimeMillis());
	}

	public static MongoEntity fromDBObject(DBObject source) {
		MongoEntity me = new MongoEntity();
		for (String key : source.keySet()) {
			me.put(key, source.get(key));
		}
		return me;
	}
}