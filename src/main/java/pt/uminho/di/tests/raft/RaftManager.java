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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.ws4d.java.CoreFramework;
import org.ws4d.java.authorization.AuthorizationException;
import org.ws4d.java.client.DefaultClient;
import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.eventing.ClientSubscription;
import org.ws4d.java.eventing.EventSource;
import org.ws4d.java.eventing.EventingException;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.SecurityKey;
import org.ws4d.java.service.Device;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.Service;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.parameter.ParameterValueManagement;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.HelloData;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;

import pt.uminho.di.raft.Constants;
import pt.uminho.di.tests.management.ManagementConstants;

public class RaftManager extends DefaultClient// implements Runnable
{
	static Logger logger = Logger.getLogger(RaftManager.class);
	protected final Lock lock = new ReentrantLock();
	protected final Condition disseminated = lock.newCondition();
	protected boolean diss;
	Random random;
	AtomicLong disseminatedCounter;
	AtomicLong counter;
	int kill_leader;
	int kill_replicas;
	int num_servers;
	int num_clients;
	int fanout;
	int myport;
	protected HashMap<URI, Service> client_services;
	protected HashMap<URI, Service> server_services;
	protected HashMap<URI, URI> server_raft_services;
	protected ArrayList<ClientSubscription> subscriptions;
	String ip;
	int base_port;
	int total_servers;
	int total_clients;
	private String test_name;
	private int num_invocations;
	private int invocation_timeout;
	private int start_to_kill;
	private int time_between_kills;
	private URI leaderKey;
	long start_of_dissemination;

	public RaftManager()
	{
		counter = new AtomicLong();
		disseminatedCounter = new AtomicLong();
		client_services = new HashMap<URI, Service>();
		server_services = new HashMap<URI, Service>();
		server_raft_services = new HashMap<URI, URI>();
		subscriptions = new ArrayList<ClientSubscription>();

		registerHelloListening();

		registerServiceListening();

		random = new Random(System.nanoTime());
	}

	@Override
	public void helloReceived(HelloData helloData)
	{
		try {
			Thread.sleep(300);
		} catch (InterruptedException ex) {
			logger.error(ex);
		}

		EndpointReference epr = helloData.getEndpointReference();
		URI address = epr.getAddress();
		DeviceReference devRef = getDeviceReference(helloData);
		try
		{
			Iterator devicePortTypes = devRef.getDevicePortTypes(true);
			boolean foundType = false;

			while((!foundType) && (devicePortTypes.hasNext()))
			{
				QName portType = (QName) devicePortTypes.next();
				if(portType.equals(ManagementConstants.ManagementClientTypeQName))
				{
					foundType = true;
					if(!client_services.containsKey(address))
					{
						logger.info("Added new managed client " + epr);
						Device device = devRef.getDevice();
						client_services.put(address, getManagementService(device));
						counter.incrementAndGet();
					}
				} else if(portType.equals(ManagementConstants.ManagementServerTypeQName))
				{
					foundType = true;
					if(!server_services.containsKey(address))
					{
						logger.info("Added new managed server " + epr);
						Device device = devRef.getDevice();
						server_services.put(address, getManagementService(device));
						server_raft_services.put(address, getRaftServiceAddress(device));
						counter.incrementAndGet();
					}
				} else
				{
					logger.info("Got type " + portType);
				}
			}
		} catch(CommunicationException ex)
		{
			logger.debug(ex.getMessage(), ex);
		}

		long val = counter.get();
		logger.info("Received Hello from " + address + "! Received " + val + " hellos!");

		if((client_services.size() == num_clients) && (server_services.size() == num_servers))
		{
			try {
				Thread.sleep(3000);
			} catch (InterruptedException ex) {
				logger.error(ex);
			}
			executeBeforeDissemination();
		}
	}

	public void initConstants(String[] args)
	{
		switch(args.length)
		{
		case 9:
			time_between_kills = Integer.parseInt(args[8]);
		case 8:
			kill_replicas = Integer.parseInt(args[7]);
		case 7:
			kill_leader = Integer.parseInt(args[6]);
		case 6:
			start_to_kill = Integer.parseInt(args[5]);
		case 5:
			invocation_timeout = Integer.parseInt(args[4]);
		case 4:
			num_invocations = Integer.parseInt(args[3]);
		case 3:
			num_servers = Integer.parseInt(args[2]);
		case 2:
			num_clients = Integer.parseInt(args[1]);
		case 1:
			test_name = args[0];
		}
	}

	public void executeBeforeDissemination()
	{
		logger.info("Got all the " + num_clients + " clients and the " + num_servers + " servers.");

		// RAFT only
		setKnownServers(server_services, true);

		startServers();

		setParameters();

		try {
			Thread.sleep(2000);
		} catch (InterruptedException ex) {
			logger.error(ex);
		}

		setKnownServers(client_services, false);
		informClientsOfLeader();

		try {
			Thread.sleep(2000);
		} catch (InterruptedException ex) {
			logger.error(ex);
		}

		unregisterHelloListening();

		startDissemination();

		logger.info("Ended execute method... Dissemination started at " + start_of_dissemination);

		if((kill_leader > 0) || (kill_replicas > 0))
		{
			logger.info("Going to kill " + kill_leader + " leader and " + kill_replicas +" replicas...");
			killServers();
		}
	}

	private void killServers()
	{
		long drift = System.currentTimeMillis() - start_of_dissemination;
		try {
			Thread.sleep(start_to_kill-drift);
		} catch (InterruptedException ex) {
			logger.error(ex);
		}
		drift = System.currentTimeMillis();
		while(kill_replicas > 0)
		{
			kill_server("follower");
			kill_replicas--;
			try {
				drift = System.currentTimeMillis() - drift;
				Thread.sleep(time_between_kills-drift);
			} catch (InterruptedException ex) {
				logger.error(ex);
			}
		}

		drift = System.currentTimeMillis();
		if(kill_leader == 1)
		{
			// kill leader
			kill_server("leader");
			try {
				drift = System.currentTimeMillis() - drift;
				Thread.sleep(time_between_kills-drift);
			} catch (InterruptedException ex) {
				logger.error(ex);
			}
		}
	}

	private void kill_server(String type)
	{
		long start_kill = System.currentTimeMillis();
		logger.info((start_kill - start_of_dissemination) + "ms. Killing " + type + "...");
		boolean killed = false;
		java.util.Iterator<URI> iter = server_services.keySet().iterator();

		while(!killed && iter.hasNext())
		{
			URI key = iter.next();
			Service server = server_services.get(key);

			// get state through getStats operation
			String state = getStats(server);
			logger.info("Server " + key + " is currently " + state);
			// if it is the leader invoke stop operation
			if(state.equalsIgnoreCase(type))
			{

				stop(server);
				long end_kill = System.currentTimeMillis();
				logger.info("Stopped " + type + " at " + end_kill + " took " + (end_kill - start_kill) + " ms.");
				server_services.remove(key);

				killed = true;
			}

		}
	}

	protected void setParameters() {
		// set leader server on clients
		logger.debug("Setting parameters on clients...");
		java.util.Iterator<Service> iter = client_services.values().iterator();

		String prefix = ManagementConstants.ParameterElementName + "[";
		while(iter.hasNext())
		{
			Service client = iter.next();
			Operation setParametersOp = client.getOperation(ManagementConstants.ManagementPortQName, ManagementConstants.SetParametersOperationName, null, null);
			ParameterValue pv = setParametersOp.createInputValue();

			ParameterValueManagement.setString(pv, prefix + "0]/" + ManagementConstants.NameElementName, "num_invocations");
			ParameterValueManagement.setString(pv, prefix + "0]/" + ManagementConstants.ValueElementName, "" + num_invocations);

			ParameterValueManagement.setString(pv, prefix + "1]/" + ManagementConstants.NameElementName, "invocation_timeout");
			ParameterValueManagement.setString(pv, prefix + "1]/" + ManagementConstants.ValueElementName, "" + invocation_timeout);
			try
			{
				logger.debug("Setting parameters " + pv + " on " + client.getEprInfos().next());
				setParametersOp.invoke(pv, CredentialInfo.EMPTY_CREDENTIAL_INFO);
				logger.debug("Set parameters " + pv + " on " + client.getEprInfos().next());
			} catch (InvocationException ex) {
				logger.error(ex.getMessage(), ex);
			} catch (CommunicationException ex) {
				logger.error(ex.getMessage(), ex);
			} catch (AuthorizationException ex) {
				logger.error(ex.getMessage(), ex);
			}
		}
		logger.info("Parameters were set on clients!");

		// set servers parameters
		logger.debug("Setting parameters on servers...");
		java.util.Iterator<URI> keyIter = server_services.keySet().iterator();

		boolean setLeader = false;
		while(keyIter.hasNext())
		{
			URI key = keyIter.next();
			Service server = server_services.get(key);
			Operation setParametersOp = server.getOperation(ManagementConstants.ManagementPortQName, ManagementConstants.SetParametersOperationName, null, null);
			ParameterValue pv = setParametersOp.createInputValue();

			int random_timeout = 60000;
			String state = "Follower";
			if(!setLeader)
			{
				random_timeout = 150 + random.nextInt(20);
				state = "Leader";
				setLeader = true;
				leaderKey = key;
			}
			else
			{
				random_timeout = 200 + random.nextInt(100);
			}

			ParameterValueManagement.setString(pv, prefix + "0]/" + ManagementConstants.NameElementName, "election_timeout");
			ParameterValueManagement.setString(pv, prefix + "0]/" + ManagementConstants.ValueElementName, "" + random_timeout);
			ParameterValueManagement.setString(pv, prefix + "1]/" + ManagementConstants.NameElementName, "state");
			ParameterValueManagement.setString(pv, prefix + "1]/" + ManagementConstants.ValueElementName, state);
			try
			{
				logger.info("Setting parameters " + pv + " on " + server.getEprInfos().next());
				setParametersOp.invoke(pv, CredentialInfo.EMPTY_CREDENTIAL_INFO);
				logger.debug("Set parameters " + pv + " on " + server.getEprInfos().next());
			} catch (InvocationException ex) {
				logger.error(ex.getMessage(), ex);
			} catch (CommunicationException ex) {
				logger.error(ex.getMessage(), ex);
			} catch (AuthorizationException ex) {
				logger.error(ex.getMessage(), ex);
			}
		}
		logger.info("Parameters were set on servers!");
	}

	protected void setKnownServers(HashMap<URI,Service> services, boolean servers)
	{
		java.util.ArrayList<URI> servers_eprs = new java.util.ArrayList<URI>(server_services.keySet());

		java.util.Iterator<URI> eprs_iter = services.keySet().iterator();

		logger.debug("Set known devices for " + services.size() + " servers...");
		// inform servers of other servers
		while(eprs_iter.hasNext())
		{
			java.util.ArrayList<URI> knownDevices = (java.util.ArrayList<URI>) servers_eprs.clone();
			URI svc_epr = eprs_iter.next();
			Service svc = services.get(svc_epr);
			if(servers)
			{
				knownDevices.remove(svc_epr);
			}


			Operation setMembershipOp = svc.getOperation(ManagementConstants.ManagementPortQName, ManagementConstants.SetMembershipOperationName, null, null);
			ParameterValue membershipPV = setMembershipOp.createInputValue();

			String prefix = ManagementConstants.EndpointElementName + "[";
			int size = knownDevices.size();
			logger.info("Going to set " + size + " targets for " + svc.getEprInfos().next());
			for(int j = 0; j < size; j++)
			{
				ParameterValueManagement.setString(membershipPV, prefix + j + "]", server_raft_services.get(knownDevices.get(j)).toString());
			}

			logger.info("Going to send " + membershipPV);
			try
			{
				setMembershipOp.invoke(membershipPV, CredentialInfo.EMPTY_CREDENTIAL_INFO);
			} catch(InvocationException ex)
			{
				logger.error(ex.getMessage(), ex);
			} catch(CommunicationException ex)
			{
				logger.error(ex.getMessage(), ex);
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
				logger.error(ex);
			}
		}
	}

	protected void startServers()
	{
		java.util.Iterator<Service> services_iter = server_services.values().iterator();

		logger.debug("Invoking start of dissemination on " + server_services.size() + " servers...");
		while(services_iter.hasNext())
		{
			Service server = services_iter.next();
			Operation op = server.getOperation(ManagementConstants.ManagementPortQName, ManagementConstants.StartOperationName, null, null);
			ParameterValue pv = op.createInputValue();
			try
			{
				op.invoke(pv, CredentialInfo.EMPTY_CREDENTIAL_INFO);
			} catch(InvocationException ex)
			{
				logger.error(ex.getMessage(), ex);
			} catch(CommunicationException ex)
			{
				logger.error(ex.getMessage(), ex);
			} catch(AuthorizationException ex)
			{
				logger.error(ex.getMessage(), ex);
			}
		}
	}

	protected void informClientsOfLeader()
	{
		if(leaderKey != null)
		{
			Service leader_service = server_services.get(leaderKey);
			Device leaderDevice = null;
			try {
				leaderDevice = getDeviceReference(new EndpointReference(leaderKey), DPWSCommunicationManager.COMMUNICATION_MANAGER_ID).getDevice();
			} catch (CommunicationException ex) {
				logger.error(ex.getMessage(), ex);
			}

			if(leaderDevice != null)
			{
				logger.debug("Device " + leader_service.getEprInfos().next() + " selected as the leader.");
				ServiceReference svcRef = leaderDevice.getServiceReference(new URI(Constants.RaftServiceName), SecurityKey.EMPTY_KEY);
				logger.debug("Got svcRef " + svcRef);

				EprInfo raftServiceEprInfo = (EprInfo) svcRef.getEprInfos().next();
				logger.debug("Raft Service Epr Info: " + raftServiceEprInfo);

				// set leader server on clients
				logger.debug("Setting server on clients...");
				java.util.Iterator<Service> iter = client_services.values().iterator();
				EprInfo leaderEprInfos = (EprInfo) leader_service.getEprInfos().next();
				logger.debug("Leader EPR is " + leaderEprInfos);
				String prefix = ManagementConstants.EndpointElementName + "[0]";
				while(iter.hasNext())
				{
					Service client = iter.next();
					Operation setMembershipOp = client.getOperation(ManagementConstants.ManagementPortQName, ManagementConstants.SetMembershipOperationName, null, null);
					ParameterValue pv = setMembershipOp.createInputValue();

					ParameterValueManagement.setString(pv, prefix, raftServiceEprInfo.getXAddressAsString());
					try
					{
						logger.debug("Setting membership " + raftServiceEprInfo.getXAddressAsString() + " on " + client.getEprInfos().next());
						setMembershipOp.invoke(pv, CredentialInfo.EMPTY_CREDENTIAL_INFO);
						logger.debug("Set membership " + raftServiceEprInfo.getXAddressAsString() + " on " + client.getEprInfos().next());
					} catch (InvocationException ex) {
						logger.error(ex.getMessage(), ex);
					} catch (CommunicationException ex) {
						logger.error(ex.getMessage(), ex);
					} catch (AuthorizationException ex) {
						logger.error(ex.getMessage(), ex);
					}

					try
					{
						// let clients have some time to contact leader
						Thread.sleep(1000);
					} catch(InterruptedException ex)
					{
						logger.error(ex.getMessage(), ex);
					}
				}

				try
				{
					// let clients have some time to contact leader
					Thread.sleep(2000);
				} catch(InterruptedException ex)
				{
					logger.error(ex.getMessage(), ex);
				}
			}
			logger.info("Finished setting server on clients.");
		}
	}

	public void startDissemination()
	{
		int num = client_services.size();
		logger.debug("Subscribing EndDissemination Op on " + num + " clients.");

		java.util.Iterator<Service> iter = client_services.values().iterator();

		int num_subs = 0;

		while(iter.hasNext())
		{
			Service client = iter.next();
			EventSource endDisseminationOp = client.getEventSource(ManagementConstants.ManagementPortQName, ManagementConstants.EndDisseminationElementName, null, null);
			try
			{
				ClientSubscription subscription = endDisseminationOp.subscribe(this, 0, CredentialInfo.EMPTY_CREDENTIAL_INFO);
				if(subscription != null)
				{
					logger.debug("Subscribed to client with event sink: " + subscription.getEventSink().toString());
					subscriptions.add(subscription);
					num_subs++;
				}
			} catch(EventingException ex)
			{
				logger.error(ex.getMessage(), ex);
			} catch(IOException ex)
			{
				logger.error(ex.getMessage(), ex);
			} catch(CommunicationException ex)
			{
				logger.error(ex.getMessage(), ex);
			}

		}

		logger.info("Subscribed to " + num_subs + " clients!");

		logger.debug("Invoking start of dissemination on " + num + " clients...");
		// start messages dissemination
		iter = client_services.values().iterator();

		start_of_dissemination = System.currentTimeMillis();

		while(iter.hasNext())
		{
			Service client = iter.next();
			Operation op = client.getOperation(ManagementConstants.ManagementPortQName, ManagementConstants.StartDisseminationOperationName, null, null);
			ParameterValue pv = op.createInputValue();
			try
			{
				op.invoke(pv, CredentialInfo.EMPTY_CREDENTIAL_INFO);
			} catch(InvocationException ex)
			{
				logger.error(ex.getMessage(), ex);
			} catch(CommunicationException ex)
			{
				logger.error(ex.getMessage(), ex);
			} catch(AuthorizationException ex)
			{
				logger.error(ex.getMessage(), ex);
			}
		}

		logger.info("Ended startDissemination method...");
	}

	@Override
	public ParameterValue eventReceived(ClientSubscription subscription, URI actionURI, ParameterValue parameterValue)
	{
		long now = System.nanoTime();

		logger.info("Received event " + parameterValue);

		long dissCounter = disseminatedCounter.incrementAndGet();

		// unsubscribe event
		try {
			subscription.unsubscribe();
		} catch (EventingException ex) {
			logger.error(ex.getMessage(), ex);
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
		} catch (CommunicationException ex) {
			logger.error(ex.getMessage(), ex);
		}
		logger.info("Unsubscribed event " + actionURI);

		if(dissCounter == client_services.size())
		{
			lock.lock();
			try
			{
				diss = true;
				disseminated.signal();
			} finally
			{
				lock.unlock();
			}
		}

		long time = System.nanoTime() - now;
		logger.info("Returning took " + time + "ns... Clients terminated: " + dissCounter);

		return null;
	}

	public void executeAfterDissemination()
	{
		logger.info("Waiting for dissemination end...");

		lock.lock();

		try
		{
			while(!diss)
			{
				disseminated.await();
			}

		} catch(InterruptedException ex)
		{
			logger.error(ex.getMessage(), ex);
		} finally
		{
			lock.unlock();
		}

		try
		{
			String filename = test_name + "_"
					+ num_servers + "s_"
					+ num_clients + "c_"
					+ num_invocations + "x_"
					+ invocation_timeout + "ms_";

			try
			{
				// after dissemination wait some time
				Thread.sleep(5000);
			} catch(InterruptedException ex)
			{
				logger.error(ex.getMessage(), ex);
			}

			logger.info("Invoking WriteStats on devices...");
			// signal all clients to write stats
			java.util.Iterator<Service> iter = client_services.values().iterator();
			int count = 0;
			while(iter.hasNext())
			{
				Service svc = iter.next();

				Object epr = svc.getEprInfos().next();
				writeStats(svc, filename + (count++) + ".csv");
				logger.debug("Invoked WriteStats on " + epr);

				try
				{
					// after dissemination wait some time
					Thread.sleep(200);
				} catch(InterruptedException ex)
				{
					logger.error(ex.getMessage(), ex);
				}

				stop(svc);
				logger.info("Stopped service on " + epr);
			}

			iter = server_services.values().iterator();
			while(iter.hasNext())
			{
				Service svc = iter.next();
				Object epr = svc.getEprInfos().next();
				stop(svc);
				logger.info("Stopped service on " + epr);
			}

			logger.info("Ended execution.");

		} catch(Exception ex)
		{
			logger.error(ex.getMessage(), ex);
			shutdown();
		}

		shutdown();
	}

	protected void stop(Service service)
	{
		Operation stopOp = service.getOperation(ManagementConstants.ManagementPortQName, ManagementConstants.StopOperationName, null, null);

		ParameterValue pv = stopOp.createInputValue();
		try
		{
			stopOp.invoke(pv, CredentialInfo.EMPTY_CREDENTIAL_INFO);
		} catch(InvocationException ex)
		{
			logger.error(ex.getMessage(), ex);
		} catch(CommunicationException ex)
		{
			logger.error(ex.getMessage(), ex);
		} catch(Exception ex)
		{
			logger.error(ex.getMessage(), ex);
		}
	}

	protected void writeStats(Service service, String filename)
	{
		Operation writeStatsOp = service.getOperation(ManagementConstants.ManagementPortQName, ManagementConstants.WriteStatsOperationName, null, null);

		ParameterValue pv = writeStatsOp.createInputValue();
		ParameterValueManagement.setString(pv, ManagementConstants.FilenameElementName, filename);
		try
		{
			writeStatsOp.invoke(pv, CredentialInfo.EMPTY_CREDENTIAL_INFO);
		} catch(InvocationException ex)
		{
			logger.error(ex.getMessage(), ex);
		} catch(Exception ex)
		{
			logger.error(ex.getMessage(), ex);
		}

	}

	protected String getStats(Service service)
	{
		String ret = "";

		Operation getStatsOp = service.getOperation(ManagementConstants.ManagementPortQName, ManagementConstants.GetStatsOperationName, null, null);

		ParameterValue pv = getStatsOp.createInputValue();
		try
		{
			ParameterValue retPV = getStatsOp.invoke(pv, CredentialInfo.EMPTY_CREDENTIAL_INFO);

			if(retPV != null)
			{
				logger.debug("Received Stats: " + retPV);
				ret = ParameterValueManagement.getString(retPV, ManagementConstants.GetStatsResponseName);
			}
		} catch(InvocationException ex)
		{
			logger.error(ex.getMessage(), ex);
		} catch(Exception ex)
		{
			logger.error(ex.getMessage(), ex);
		}

		return ret;
	}

	protected void shutdown()
	{
		try
		{
			Thread.sleep(500);
		} catch(InterruptedException ex)
		{
			logger.error(ex);
		}

		logger.info("Shutting down...");

		CoreFramework.stopIgnoringInstancesCount();
		System.exit(0);
	}

	private Service getManagementService(Device device)
	{
		Service svc = null;
		try
		{
			svc = device.getServiceReference(ManagementConstants.ManagementServiceId, SecurityKey.EMPTY_KEY).getService();
		} catch(CommunicationException ex)
		{
			logger.error(ex.getMessage(), ex);
		}
		return svc;
	}


	private URI getRaftServiceAddress(Device device) {
		EprInfo eprInfo = (EprInfo) device.getServiceReference(Constants.RaftServiceId, SecurityKey.EMPTY_KEY).getEprInfos().next();

		return eprInfo.getXAddress();
	}

	public static void main(String[] args)
	{
		CoreFramework.start(null);

		RaftManager manager = new RaftManager();

		logger.info("Manager executing...");
		try
		{
			manager.initConstants(args);

			manager.executeAfterDissemination();
		} catch(Exception ex)
		{
			logger.error(ex.getMessage(), ex);
		}

		logger.info("Manager terminated executing.");

	}

}
