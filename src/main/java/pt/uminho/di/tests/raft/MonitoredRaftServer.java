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
package pt.uminho.di.tests.raft;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ws4d.java.util.Log;
import org.ws4d.java.CoreFramework;
import pt.uminho.di.raft.entities.ServerClient;
import pt.uminho.di.raft.service.RaftService;
import pt.uminho.di.tests.management.service.ManagementService;
import pt.uminho.di.tests.monitoring.MonitoringThread;

public class MonitoredRaftServer extends ManagedRaftServer {

	static Logger logger = Logger.getLogger(MonitoredRaftServer.class);

	MonitoringThread monitoringThread;

	public MonitoredRaftServer(String id)
	{
		super(id);
	}

	@Override
	public void startDevice()
	{
		monitoringThread = new MonitoringThread();
		monitoringThread.setFilename("raft_server" + getId() + ".csv");
		monitoringThread.start();
		super.startDevice();
	}

	@Override
	public void stopDevice()
	{
		long start = System.currentTimeMillis();
		this.setState(null);

		logger.info(getIdString() + " Stopping device at " + start + "ms. Last Log Index is: " + getLastLogIndex());

		try
		{
			getRaftService().stop();
			stop(true);
			long pid = monitoringThread.getPid();
			// monitoringThread writing stats
			monitoringThread.setRunning(false);
			monitoringThread.join();

			// kill this server
			Process p = Runtime.getRuntime().exec("kill -9 " + pid);
			p.waitFor();
		}
		catch(InterruptedException ex)
		{
			logger.error(ex.getMessage(), ex);
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	public static void main(String[] args) {
		// configure loggers

		// mandatory: Starting the DPWS Framework.
		CoreFramework.start(null);
		// configure loggers
		PropertyConfigurator.configure("log4j.properties");

		String id = "";
		Integer election_timeout = 600000; // 10 minutes

		switch (args.length) {
		case 2:
			election_timeout = Integer.parseInt(args[1]);
		case 1:
			id = args[0];
		}

		MonitoredRaftServer device = null;

		boolean started = false;
		try {
			logger.info("Starting ManagedRaftServer...");
			// Create the server device.
			device = new MonitoredRaftServer(id);

			// Create raft service and add it to the device.
			RaftService service = new RaftService(device);
			device.addService(service);

			ManagementService mgmtService = new ManagementService();
			mgmtService.setDevice(device);
			device.addService(mgmtService);

			ServerClient client = new ServerClient(device);
			device.setClient(client);
			device.setElectionTimeout(election_timeout);


			//            Log.setLogLevel(Log.DEBUG_LEVEL_NO_LOGGING);
			Log.setLogLevel(Log.DEBUG_LEVEL_ERROR);
//            Log.setLogLevel(Log.DEBUG_LEVEL_DEBUG);
//            Log.setLogLevel(Log.DEBUG_LEVEL_INFO);

			// Starting the device.

			device.start();
			started = true;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			if(!started)
			{
				try {
					device.start();
				} catch (IOException ex) {
					logger.error(ex.getMessage(), ex);
				}
			}
			else
			{
				try {
					device.stop();
				} catch (IOException ex) {
					logger.error(ex.getMessage(), ex);
				}
			}
			CoreFramework.stop();
		}
	}
}
