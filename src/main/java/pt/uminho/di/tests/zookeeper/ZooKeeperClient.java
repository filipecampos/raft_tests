/*******************************************************************************
 * Copyright (c) 2014 Filipe Campos.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package pt.uminho.di.tests.zookeeper;

import java.io.FileWriter;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.ws4d.java.util.IDGenerator;
import pt.uminho.di.tests.monitoring.MonitoringThread;

public class ZooKeeperClient implements Watcher {

	static final Logger logger = Logger.getLogger(ZooKeeperClient.class);

	ZooKeeper zooKeeper;
	long[] lats;
	String[] results;

	public ZooKeeperClient(String connectionString, int sessionTimeout, int n) throws IOException
	{
		zooKeeper = new ZooKeeper(connectionString, sessionTimeout, this);
		logger.info("Connected to zookeeper servers : " + connectionString);
		lats = new long[n];
		results = new String[n];
	}

	public ZooKeeperClient(String address, int port, int sessionTimeout, int n) throws IOException
	{
		this(address + ":" + port, sessionTimeout, n);
	}

	public void initializedLogDirectories()
	{
		// create directory if it doesn't exist
		String[] pathParts = {"log", "entries"};

		StringBuilder pathSB = new StringBuilder();
		for (String pathElement : pathParts) {
			pathSB.append('/').append(pathElement);
			String pathString = pathSB.toString();
			//send requests to create all parts of the path without waiting for the
			//results of previous calls to return
			try
			{
				Stat response = zooKeeper.exists(pathString, false);
				logger.info(pathString + " exists? Response: " + response);
				if(response == null)
				{
					String reply = zooKeeper.create(pathString, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
					logger.info("Create " + pathString + " : " + reply);
				}
			} catch(KeeperException ex)
			{
				logger.error(ex.getMessage(), ex);
			} catch(InterruptedException ex)
			{
				logger.error(ex.getMessage(), ex);
			}
		}
	}

	public void createEntry(int iter)
	{
		// create log entry
		String uuid = IDGenerator.getUUID();
		String path = "/log/entries/" + uuid + ".dat";
		String dataStr = "bla: yep";
		byte[] data = dataStr.getBytes();

		try
		{
			long start = System.nanoTime();
			String returnPath = zooKeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			long finish = System.nanoTime();
			lats[iter] = finish - start;
			boolean success = path.equalsIgnoreCase(returnPath);
			results[iter] = Boolean.toString(success);
			logger.debug("Path: " + path + "; ReturnPath: " + returnPath + "; success: " + success);

		} catch(KeeperException ex)
		{
			logger.error(ex.getMessage(), ex);
		} catch(InterruptedException ex)
		{
			logger.error(ex.getMessage(), ex);
		}
	}

	public void closeConnection()
	{
		try
		{
			zooKeeper.close();
			logger.info("Closed connection to zookeeper server.");
		} catch(InterruptedException ex)
		{
			logger.error(ex.getMessage(), ex);
		}
	}

	public String getStats()
	{
		StringBuilder sb = new StringBuilder("Lats(ns);");
		String address = "client";

		// write lats
		sb.append(address);
		sb.append(';');
		for (int i = 0; i < lats.length; i++) {
			sb.append(lats[i]);
			sb.append(';');
		}

		sb.append("\n");

		sb.append("Responses;");
		sb.append(address);
		sb.append(';');
		for (int i = 0; i < results.length; i++) {
			sb.append(results[i]);
			sb.append(';');
		}
		sb.append("\n");

		return sb.toString();
	}

	public void writeStats(String filename)
	{
		try
		{
			FileWriter file = new FileWriter(filename);
			file.append(getStats());
			file.flush();
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	public static void main(String[] args)
	{
		// configure loggers
		PropertyConfigurator.configure("log4j.properties");

		String address = "192.168.82.30";
		int port = 2181;
		int num = 10;
		long period = 1000;
		int sessionTimeout = 60000;

		if(args.length == 4)
		{
			address = args[0];
			port = Integer.parseInt(args[1]);
			num = Integer.parseInt(args[2]);
			period = Long.parseLong(args[3]);
		}

		try
		{
			MonitoringThread monitoringThread = new MonitoringThread();
			monitoringThread.setFilename("client-" + address + ".csv");
			monitoringThread.start();

			ZooKeeperClient client = new ZooKeeperClient(address, port, sessionTimeout, num);

			client.initializedLogDirectories();

			for(int i = 0; i < num; i++)
			{
				client.createEntry(i);
				Thread.sleep(period);
			}

			monitoringThread.setRunning(false);

			monitoringThread.join();
		} catch(IOException ex)
		{
			logger.error(ex.getMessage(), ex);
		} catch(InterruptedException ex)
		{
			logger.error(ex.getMessage(), ex);
		}
	}

	public void process(WatchedEvent event)
	{
		logger.info("Invoked prcess event " + event);
	}

}
