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
import org.ws4d.java.CoreFramework;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Log;
import pt.uminho.di.raft.Constants;
import pt.uminho.di.raft.entities.Server;
import pt.uminho.di.raft.entities.ServerClient;
import pt.uminho.di.raft.entities.states.State;
import pt.uminho.di.raft.entities.workers.InsertingTask;
import pt.uminho.di.raft.service.RaftService;
import pt.uminho.di.tests.management.ManagedDevice;
import pt.uminho.di.tests.management.ManagementConstants;
import pt.uminho.di.tests.management.service.ManagementService;

public class ManagedRaftServer extends Server implements ManagedDevice {

	static Logger logger = Logger.getLogger(ManagedRaftServer.class);

	public ManagedRaftServer(String id)
	{
		super(id);

		QNameSet deviceTypes = new QNameSet(Constants.RaftDeviceQName);
		deviceTypes.add(ManagementConstants.ManagementServerTypeQName);
		setPortTypes(deviceTypes);
	}

	@Override
	public String getStats()
	{
		return this.getState().name().toLowerCase();
	}

	@Override
	public void writeStats(String filename)
	{
		// not needed
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getEndpoint()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void startDevice()
	{
		logger.info(getIdString() + " Starting device...");
		startTimeoutTask();
		logger.info(getIdString() + " Started device.");
	}

	@Override
	public void stopDevice()
	{
		long start = System.currentTimeMillis();
		logger.info(getIdString() + " Stopping device at " + start + "ms. Last Log Index is: " + getLastLogIndex());
		try
		{
			stopServer();
			this.stop();
			CoreFramework.stopIgnoringInstancesCount();

		} catch(IOException ex)
		{
			logger.error(ex.getMessage(), ex);
		} catch(Exception ex)
		{
			logger.error(ex.getMessage(), ex);
		}

		long finish = System.currentTimeMillis();
		logger.info(getIdString() + " Stopped device in " + (finish - start) + " ms. Last Log Index is: " + getLastLogIndex());

		System.exit(0);
	}

	@Override
	public void setKnownDevices(String[] targets)
	{
		logger.debug(getIdString() + " Setting known devices..." + targets);
		if((targets != null) && (targets.length > 0))
		{
			for(int i = 0; i < targets.length; i++)
			{
				logger.debug(getIdString() + " Inserting device " + targets[i]);
				InsertingTask task = new InsertingTask(getClient(), this, new EndpointReference(new URI(targets[i])));
				CoreFramework.getThreadPool().execute(task);

			}
		}
		logger.debug(getIdString() + " Set known devices.");
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

		ManagedRaftServer device = null;

		try {
			logger.info("Starting ManagedRaftServer...");
			// Create the server device.
			device = new ManagedRaftServer(id);

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

			// Starting the device.
			device.start();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			try {
				device.stop();
			} catch (IOException ex) {
				logger.error(ex.getMessage(), ex);
			}
			CoreFramework.stop();
		}
	}

	@Override
	public void setParameter(String paramName, String paramValue)
	{
		logger.info(getIdString() + " Setting parameter " + paramName + " with value " + paramValue + " at " + System.currentTimeMillis());
		if(paramName.equalsIgnoreCase("election_timeout"))
		{
			setElectionTimeout(Integer.parseInt(paramValue));
		}
		else if(paramName.equalsIgnoreCase("id"))
		{
			setId(paramValue);
		}
		else if(paramName.equalsIgnoreCase("state"))
		{
			State state = State.valueOf(paramValue);
			if(state == State.Leader)
				increaseTerm();

			startRole(state);
		}
		else if(paramName.equalsIgnoreCase("entries"))
		{
			// set log size (Integer.parseInt(paramValue));
		}

	}
}
