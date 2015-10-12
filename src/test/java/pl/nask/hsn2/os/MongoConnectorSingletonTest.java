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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.testng.Assert;
import org.testng.annotations.Test;



public class MongoConnectorSingletonTest {
	
	
	private class OsMongoConnector implements Callable<Void> {
		MongoConnector mc;	
		public MongoConnector getMongoConnector() {
			return mc;
		}
		@Override
		public Void call() throws Exception {
			MongoConnector.initConnection("", 27017);
			mc = MongoConnector.getInstance();
			return null;
		}
	}
	
	@Test(enabled=false)
	public void mongoConnectorSingletonTest() throws InterruptedException {
		int MAX_THREADS = 10;
		ArrayList<OsMongoConnector> l = new ArrayList<OsMongoConnector>(MAX_THREADS );
		for(int i=0;i<MAX_THREADS;i++) {
			l.add(new OsMongoConnector());
		}
		ExecutorService ex = Executors.newFixedThreadPool(MAX_THREADS);
		ex.invokeAll(l);
		for(int i=0;i<l.size();i++) {
			OsMongoConnector s = l.get(i);
			OsMongoConnector s2 = l.get((i+1)%l.size());
			Assert.assertEquals(s.getMongoConnector(),s2.getMongoConnector(), "["+i+"]");

		}
	}
	
	
}
