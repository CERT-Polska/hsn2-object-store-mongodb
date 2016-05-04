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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.RuntimeErrorException;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import pl.nask.hsn2.os.entities.ObjectStoreResponse;
import pl.nask.hsn2.protobuff.Object.Attribute;
import pl.nask.hsn2.protobuff.Object.Attribute.Type;
import pl.nask.hsn2.protobuff.Object.ObjectData;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectRequest;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectRequest.RequestType;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectResponse.ResponseType;

public class OsMultiThreadTest {
	
	private static final int MAX_THREADS = 10;
	private static final int MESSAGES_PER_THREAD = 10000;
	
	private ObjectStore objectStore;
	private List<PutWorker> workerList = new ArrayList<PutWorker>();
	
	
	private static final AtomicInteger objCounter = new AtomicInteger(0);
	
	@BeforeClass
	public  void createOS() throws IOException {
		MongoConnector.initConnection("", 27017);
		objectStore = ObjectStore.getInstance();
	}
	
	@AfterTest
	public void printReportersLog() {
	}


	@Test(enabled=false,timeOut = 200000)
	public void putMultiThreadTest() throws InterruptedException {
		ExecutorService ex = Executors.newFixedThreadPool(MAX_THREADS);
		for( int i = 0; i < MAX_THREADS; i++)
			workerList.add(new PutWorker(objectStore));
		
		long startTime = System.currentTimeMillis();
		ex.invokeAll(workerList);
		long endTime = System.currentTimeMillis();
		for(PutWorker w: workerList) {
			Assert.assertFalse(w.isFailed());
			Assert.assertEquals(MESSAGES_PER_THREAD, w.getAddedIds().size());
		}
		Reporter.log("JobTook:"+(endTime - startTime)+"ms");
		Assert.assertEquals(MAX_THREADS * MESSAGES_PER_THREAD,objCounter.get());
	}
	
	
	class PutWorker implements Callable<Void> {
		private ObjectStore os;
		private boolean failed = false;
		public boolean isFailed() { return failed;}
		private final Set<Long> orl = new TreeSet<Long>();
		
		PutWorker(ObjectStore o)  {
			this.os = o;
			
		}
		public Set<Long> getAddedIds() {
			return orl;
		}
		@Override
		public Void call() throws Exception {
			Reporter.log("taskStarted");
			for(int i=0; i<MESSAGES_PER_THREAD;i++) {
				ObjectRequest req = createRequest();
				ObjectStoreResponse resp = os.addObject(req);
				orl.add(resp.getIds().toArray(new Long[] {})[0]);
				if(!failed)
					failed = ResponseType.SUCCESS_PUT != resp.getType();
				
			}
			throw new RuntimeErrorException(null, "END OF TEST");
			
		}	
		
		 private ObjectRequest createRequest() {
			 	int id = objCounter.incrementAndGet();
		        List<Attribute> attributes = new ArrayList<Attribute>();
		        attributes.add(Attribute.newBuilder().setDataString("test"+id+".pl").setType(Type.STRING).setName("url_original").build());
		        attributes.add(Attribute.newBuilder().setName("test_no").setType(Type.INT).setDataInt(id).build());
		        attributes.add(Attribute.newBuilder().setName("time").setType(Type.TIME).setDataTime(System.currentTimeMillis()).build());

		        ObjectData objectData = ObjectData.newBuilder().addAllAttrs(attributes).build();
		        ObjectRequest request = ObjectRequest.newBuilder().addData(objectData).setJob(1).setOverwrite(true).setType(RequestType.PUT).build();
		        return request;
		    }
	}
}
