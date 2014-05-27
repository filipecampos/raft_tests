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
import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.service.DefaultDevice;
import org.ws4d.java.types.QNameSet;
import pt.uminho.di.tests.management.ManagementConstants;
import pt.uminho.di.tests.management.service.ManagementService;
import pt.uminho.di.tests.monitoring.MonitoringThread;

public class MonitoredSimpleClient extends ManagedSimpleClient {

	static Logger logger = Logger.getLogger(MonitoredSimpleClient.class);

	MonitoringThread monitoringThread;
	String id;

	public MonitoredSimpleClient(String id, Integer p, Integer n)
	{
		super(p, n);
		this.id = id;
	}

	@Override
	public void run()
	{
		monitoringThread = new MonitoringThread();
		monitoringThread.start();
		monitoringThread.setFilename("client" + id + ".csv");
		super.run();
		monitoringThread.setRunning(false);
		try
		{
			monitoringThread.join();
		} catch(InterruptedException ex)
		{
			logger.error(ex.getMessage(), ex);
		}
	}

	public static void main(String[] args) {
		// configure loggers
		PropertyConfigurator.configure("log4j.properties");

		String id = "0";
		Integer num = 10;
		Integer period = 5000;

		switch(args.length)
		{
			case 3:
			{
				period = Integer.parseInt(args[2]);
				num = Integer.parseInt(args[1]);
				id = args[0];
			}
		}

		// mandatory: Starting the DPWS Framework.
		CoreFramework.start(null);

		// Create client...
		MonitoredSimpleClient client = new MonitoredSimpleClient(id, period, num);

		DefaultDevice device = new DefaultDevice(DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);

		device.setPortTypes(new QNameSet(ManagementConstants.ManagementClientTypeQName));

		ManagementService mgmtService = new ManagementService();
		mgmtService.setDevice(client);
		device.addService(mgmtService);

		client.setMgmtService(mgmtService);
		client.setOwnEPR(device.getEndpointReference());

//            Log.setLogLevel(Log.DEBUG_LEVEL_NO_LOGGING);
		Log.setLogLevel(Log.DEBUG_LEVEL_ERROR);
//            Log.setLogLevel(Log.DEBUG_LEVEL_DEBUG);

		try
		{
			device.start();
		} catch(IOException ex)
		{
			logger.error(ex.getMessage(), ex);

			try {
				device.stop();
			} catch (IOException ex1) {
				logger.error(ex1.getMessage(), ex1);
			}
			client.stopDevice();
		}
	}

}
