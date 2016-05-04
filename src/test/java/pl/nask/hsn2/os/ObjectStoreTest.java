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

package pl.nask.hsn2.os;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import mockit.NonStrictExpectations;

import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import pl.nask.hsn2.os.entities.ObjectStoreResponse;
import pl.nask.hsn2.protobuff.Object.Attribute;
import pl.nask.hsn2.protobuff.Object.ObjectData;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectRequest;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectRequest.Builder;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectRequest.QueryStructure;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectRequest.QueryStructure.QueryType;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectRequest.RequestType;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectResponse.ResponseType;

public class ObjectStoreTest {

	
	private static final boolean TESTS_ENABLED = false;
	
	
	private static final String MONGODB_HOST = "";
	private static final AtomicLong idsCounter = new AtomicLong(1000l);
	
	
	private ObjectStore objectStore;


	private Collection<Long> ids;
	
	private ObjectRequest.Builder getDataForPutReqest(){
		ObjectRequest.Builder orb;
		orb = ObjectRequest.newBuilder()
							.setType(RequestType.PUT)
							.addData(ObjectData.newBuilder().build());
		return orb ;
	}

	@Test(enabled=TESTS_ENABLED) 
	public void putObjectWrongParent() throws Exception {	
		ObjectRequest.Builder objectRequest = getDataForPutReqest();
		MongoConnector.initConnection(MONGODB_HOST, 27017);
		objectStore = ObjectStore.getInstance();
		ObjectData od = ObjectData.newBuilder().addAttrs(
					Attribute.newBuilder()
					.setName("parent")
					.setType(Attribute.Type.INT)
					.setDataInt(100).build()
					).build();
		ObjectStoreResponse resp = objectStore.addObject(objectRequest.setJob(1l).setTaskId(2).addData(od).setOverwrite(true).build());
		Assert.assertEquals(ResponseType.FAILURE,resp.getType());
		
	}
	
	@Test(enabled=TESTS_ENABLED)
	public void putNewObjects() throws Exception {
		MongoConnector.initConnection(MONGODB_HOST, 27017);
		objectStore = ObjectStore.getInstance();
		int objectCount = 10;
		Builder or = ObjectRequest.newBuilder();
		for(int i = 0; i<objectCount; i++) {
			ObjectData od = createObjectData(i+1).build();
			or.addData(od);
		}
		or.setJob(1).setType(RequestType.PUT).setOverwrite(true);
		
		ObjectStoreResponse resp = objectStore.addObject(or.build());
		Assert.assertEquals(ResponseType.SUCCESS_PUT, resp.getType());
		ids = resp.getIds();
		Assert.assertTrue(resp.getIds().size() == objectCount);
	}
	
	
	
	@Test(enabled=TESTS_ENABLED,dependsOnMethods="putNewObjects") 
	public void getObjectNonExistent() throws Exception {
		MongoConnector.initConnection(MONGODB_HOST, 27017);
		objectStore = ObjectStore.getInstance();

		ObjectRequest or = ObjectRequest.newBuilder()
				.addObjects(ids.toArray(new Long[]{})[ids.size()-1].intValue()+20)
				.setJob(50l).setType(RequestType.GET)
				.build();
		
		
		ObjectStoreResponse resp = objectStore.getObject(or);
		Assert.assertEquals(ResponseType.PARTIAL_GET, resp.getType());
		Assert.assertTrue(resp.toObjectResponse().getDataCount() == 0);
		Assert.assertTrue(resp.toObjectResponse().getMissingCount() == 1);
	}
	
	@Test(enabled=TESTS_ENABLED,dependsOnMethods="putNewObjects")
	public void getObject() throws Exception {
		MongoConnector.initConnection(MONGODB_HOST, 27017);
		objectStore = ObjectStore.getInstance();
		
		ObjectRequest or = ObjectRequest.newBuilder()
				.addObjects(ids.toArray(new Long[]{})[ids.size()-1].intValue())
				.setJob(50l).setType(RequestType.GET)
				.build();
		ObjectStoreResponse resp = objectStore.getObject(or);
		
		
		Assert.assertEquals(ResponseType.SUCCESS_GET, resp.getType());
		Assert.assertTrue(resp.toObjectResponse().getDataCount() == 1);
		Assert.assertTrue(resp.toObjectResponse().getMissingCount() == 0);
	}
	
	
	
	
	@Test(enabled=TESTS_ENABLED,dependsOnMethods="putNewObjects")  //TODO clarify with data contract
	public void udpateObjectNoData() throws Exception {
		
		MongoConnector.initConnection(MONGODB_HOST, 27017);
		objectStore = ObjectStore.getInstance();
		
		Builder objectRequest = ObjectRequest.newBuilder().setType(RequestType.UPDATE).setJob(10).setOverwrite(true).setTaskId(2);
		ObjectStoreResponse resp = objectStore.updateObject(objectRequest .build());
		Assert.assertEquals(ResponseType.FAILURE,resp.getType());
		
		
	}

	private ObjectData.Builder createObjectData(int numAttr) {
		ObjectData.Builder od = ObjectData.newBuilder();
		final int id = 50;
		Attribute a ;
		if(numAttr>0) {
			 a = Attribute.newBuilder().setName("job_id").setType(Attribute.Type.INT).setDataInt(id).build();
			od.addAttrs(a);
		}
		numAttr--;
		while(numAttr > 0) {
			a = Attribute.newBuilder().setName("attr"+numAttr).setType(Attribute.Type.BOOL).setDataBool(numAttr%2==0).build();
			od.addAttrs(a);
			numAttr--;
		}
		return od;
		
	}
	@Test(enabled = TESTS_ENABLED)//TODO clarify
	public void updateObjectNonExistent() throws Exception {
		MongoConnector.initConnection(MONGODB_HOST, 27017);
		objectStore = ObjectStore.getInstance();
		MongoConnector mc = MongoConnector.getInstance();
		long id = mc.getNextObjId()+1;
		
		ObjectData.Builder od = ObjectData.newBuilder();
		Attribute a = Attribute.newBuilder().setName("_id").setType(Attribute.Type.INT).setDataInt((int) id).build();
		od.addAttrs(a);
		
		Builder objectRequest = ObjectRequest.newBuilder().setType(RequestType.UPDATE).setJob(10).setTaskId(2).setOverwrite(true);
		objectRequest.addData(od);
		ObjectStoreResponse resp = objectStore.updateObject(objectRequest .build());
		AssertJUnit.assertEquals(ResponseType.PARTIAL_UPDATE,resp.getType());
	}
	
	
	@Test(enabled = TESTS_ENABLED,dependsOnMethods="putNewObjects")
	public void queryByNameTest() throws Exception{
		MongoConnector.initConnection(MONGODB_HOST, 27017);
		objectStore = ObjectStore.getInstance();
		
		QueryStructure qr = QueryStructure.newBuilder().setType(QueryType.BY_ATTR_NAME).setAttrName("attr1").build();
		Builder objectRequest = ObjectRequest.newBuilder().setType(RequestType.QUERY).setJob(10).setTaskId(2);
		objectRequest.addQuery(qr);
		
		ObjectStoreResponse resp = objectStore.query(objectRequest.build());
		Assert.assertSame(resp.getType() == ResponseType.SUCCESS_QUERY, resp.toObjectResponse().getDataCount()>0);
		
		
	}
	
	@Test(enabled=TESTS_ENABLED) 
	public void mockTest() throws IOException {
		mockMongoConnector();
		MongoConnector m = MongoConnector.getInstance();
		Assert.assertEquals(1000l,m.getNextObjId());
		
	}

	private NonStrictExpectations mockMongoConnector() throws IOException {
		return new NonStrictExpectations() {
			
			MongoConnector mc;
			{
				MongoConnector.initConnection(anyString,anyInt);
				MongoConnector.getInstance();result = mc;
				MongoConnector.getInstance();result = mc;
				mc.getNextObjId();result = idsCounter.getAndAdd(100l);
			}
		};
	}
	
	
	
	
	
}
