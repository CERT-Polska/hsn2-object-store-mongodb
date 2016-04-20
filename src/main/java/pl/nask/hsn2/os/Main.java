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
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonController;
import org.apache.commons.daemon.DaemonInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.logger.LoggerForLog4j;
import pl.nask.hsn2.logger.LoggerManager;

public class Main implements Daemon {
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	private static LoggerManager loggerManager = LoggerForLog4j.getInstance();
	private static String connectorAddress = "127.0.0.1";
	private static String mongoAddress = "127.0.0.1";
	private static int mongoPort = 27017;
	private static String objectStoreQueueNameLow = "os:l";
	private static String objectStoreQueueNameHigh = "os:h";
	private static int maxListenerLpThreads = 5;
	private static int maxListenerHpThreads = 5;
	private static ExecutorService executor;
	private static String jobFinishedIgnore = "none";

	public static void main(String[] args) throws DaemonInitException, BusException, InterruptedException {
		Main os = new Main();
		os.init(new JsvcArgWrapper(args));
		os.start();
		Thread.currentThread().join();
		os.stop();
		os.destroy();
	}

	private static void createConnectors(List<Callable<Object>> connectors, int count, String queueName) {
		for (int i = 0; i < count; i++) {
			ObjectStoreConnector connector = new ObjectStoreConnector(queueName, jobFinishedIgnore);
			connectors.add(Executors.callable(connector));
		}
	}

	@SuppressWarnings("static-access")
	private static CommandLine parseArguments(String[] args) {
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("logLevel").withArgName("level").hasArg().withDescription("use given level for log").create("ll"));
		options.addOption(OptionBuilder.withLongOpt("mongoConnector").withArgName("address").hasArg().withDescription("Mongo connector address").create("mongoCon"));
		options.addOption(OptionBuilder.withLongOpt("connector").withArgName("address").hasArg().withDescription("Connector address").create("con"));
		options.addOption(OptionBuilder.withLongOpt("queueName").withArgName("name").hasArg().withDescription("ObjectStore low priority queue name").create("qn"));
		options.addOption(OptionBuilder.withLongOpt("highQueueName").withArgName("name").hasArg().withDescription("ObjectStore high priority queue name").create("hqn"));
		options.addOption(OptionBuilder.withLongOpt("maxListenerThreads").withArgName("num").hasArg().withDescription("listener threads pool size for low priority queue").create("maxLT"));
		options.addOption(OptionBuilder.withLongOpt("help").withDescription("prints help").create("h"));
		options.addOption(OptionBuilder.withLongOpt("jobFinishedIgnore").withArgName("all|fail|none").hasArg().withDescription("Ignore job finished. default: none").create("jfi"));
		try {
			CommandLine p = new PosixParser().parse(options, args);
			if (p.hasOption("help")) {
				HelpFormatter f = new HelpFormatter();
				f.printHelp("java -jar ...", options);
				System.exit(0);
			}
			return p;
		} catch (ParseException e) {
			throw new IllegalArgumentException("Cannot parse command line options", e);
		}
	}

	private static void applyArguments(CommandLine cmd) {
		if (cmd.hasOption("logLevel"))
			loggerManager.setLogLevel(cmd.getOptionValue("logLevel"));
		if (cmd.hasOption("connector"))
			connectorAddress = cmd.getOptionValue("connector");
		if (cmd.hasOption("mongoConnector"))
			mongoAddress = cmd.getOptionValue("mongoConnector");
		if (cmd.hasOption("queueName"))
			objectStoreQueueNameLow = cmd.getOptionValue("queueName");
		if (cmd.hasOption("highQueueName"))
			objectStoreQueueNameHigh = cmd.getOptionValue("highQueueName");
		if (cmd.hasOption("maxListenerThreads"))
			maxListenerLpThreads = Integer.parseInt(cmd.getOptionValue("maxListenerThreads"));
		if (cmd.hasOption("maxListenerHpThreads"))
			maxListenerHpThreads = Integer.parseInt(cmd.getOptionValue("maxListenerHpThreads"));
		if (cmd.hasOption("jobFinishedIgnore"))
			jobFinishedIgnore = cmd.getOptionValue("jobFinishedIgnore");
	}

	@Override
	public final void init(DaemonContext context) throws DaemonInitException {
		CommandLine cmd = parseArguments(context.getArguments());
		applyArguments(cmd);
	}

	@Override
	public final void start() throws BusException {
		ConnectorImpl.initConnection(connectorAddress);
		try {
			MongoConnector.initConnection(mongoAddress, mongoPort);
		} catch (IOException e) {
			LOGGER.error("Problem with MongoDB!", e);
			System.exit(1);
		}
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				System.exit(1);
			}
		});

		executor = Executors.newFixedThreadPool(maxListenerLpThreads + maxListenerHpThreads);
		List<Callable<Object>> connectors = new ArrayList<>();
		createConnectors(connectors, maxListenerLpThreads, objectStoreQueueNameLow);
		createConnectors(connectors, maxListenerHpThreads, objectStoreQueueNameHigh);
		// executor.invokeAll(connectors);
		for (Callable<Object> t : connectors) {
			executor.submit(t);
		}

	}

	@Override
	public final void stop() {
		executor.shutdownNow();

	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
	}

	private static class JsvcArgWrapper implements DaemonContext {

		private final String[] args;

		public JsvcArgWrapper(String[] p) {
			args = p.clone();
		}

		@Override
		public DaemonController getController() {
			return null;
		}

		@Override
		public String[] getArguments() {
			return args;
		}

	}
}
