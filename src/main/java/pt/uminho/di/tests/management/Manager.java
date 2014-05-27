package pt.uminho.di.tests.management;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
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

public class Manager extends DefaultClient// implements Runnable
{
	static Logger logger = Logger.getLogger(Manager.class);
	protected final Lock lock = new ReentrantLock();
	protected final Condition disseminated = lock.newCondition();
	protected boolean diss;
	Random random;
	AtomicLong disseminatedCounter;
	AtomicLong counter;
	int num_servers;
	int num_clients;
	int fanout;
	int myport;
	protected HashMap<URI, Service> client_services;
	protected HashMap<URI, Service> server_services;
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
	long start_of_dissemination;
	String[] servers;

	public Manager() {
		counter = new AtomicLong();
		disseminatedCounter = new AtomicLong();
		client_services = new HashMap<URI, Service>();
		server_services = new HashMap<URI, Service>();
		subscriptions = new ArrayList<ClientSubscription>();

		registerHelloListening();

		registerServiceListening();

		random = new Random(System.nanoTime());
	}

	@Override
	public void helloReceived(HelloData helloData) {
		EndpointReference epr = helloData.getEndpointReference();
		URI address = epr.getAddress();
		DeviceReference devRef = getDeviceReference(helloData);
		try {
			Iterator devicePortTypes = devRef.getDevicePortTypes(true);
			boolean foundType = false;

			while ((!foundType) && (devicePortTypes.hasNext())) {
				QName portType = (QName) devicePortTypes.next();
				if (portType.equals(ManagementConstants.ManagementClientTypeQName)) {
					foundType = true;
					if (!client_services.containsKey(address)) {
						logger.info("Added new managed client " + epr);
						client_services.put(address, getManagementService(devRef));
						counter.incrementAndGet();
					}
				} else if (portType.equals(ManagementConstants.ManagementServerTypeQName)) {
					foundType = true;
					if (!server_services.containsKey(address)) {
						logger.info("Added new managed server " + epr + " with address " + address);
						server_services.put(address, getManagementService(devRef));
						counter.incrementAndGet();
					}
				} else {
					logger.info("Got type " + portType);
				}
			}
		} catch (CommunicationException ex) {
			logger.debug(ex.getMessage(), ex);
		}

		long val = counter.get();
		logger.info("Received Hello from " + address + "! Received " + val + " hellos!");

		if ((client_services.size() == num_clients) && (server_services.size() == num_servers)) {
			executeBeforeDissemination();
		}
	}

	public void initConstants(String[] args) {
		switch (args.length) {
		case 8:
			time_between_kills = Integer.parseInt(args[7]);
		case 7:
			servers = args[6].contains(".") ? args[6].split(",") : null;
			logger.info("Gonna kill " + args[6]);
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

	public void executeBeforeDissemination() {
		logger.info("Got all the " + num_clients + " clients and the " + num_servers + " servers.");

		// RAFT only
		//        setKnownServers();

		//        startServers();

		//        selectLeader();

		setParameters();

		startDissemination();
		start_of_dissemination = System.currentTimeMillis();

		logger.info("Started dissemination...");

		if ((servers != null) && (servers.length > 0)) {
			killServers();
		}
		logger.info("Ended execute method...");
	}

	private void killServers() {
		for (int i = 0; i < servers.length; i++) {
			String server_ip = servers[i];

			long sleepTime = time_between_kills;
			if (i == 0) {
				sleepTime = start_to_kill;
			}

			logger.info("Going to sleep for " + sleepTime + "ms before killing " + server_ip);
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException ex) {
				logger.error(ex);
			}

			kill_server(server_ip);
		}
	}

	private void kill_server(String server_ip) {
		long start_kill = System.currentTimeMillis();
		logger.info((start_kill - start_of_dissemination) + "ms. Killing " + server_ip + "...");
		boolean killed = false;
		Set<Entry<URI,Service>> entries = server_services.entrySet();
		java.util.Iterator<Entry<URI,Service>> iter = entries.iterator();
		EprInfo epr;

		while (!killed && iter.hasNext()) {
			Entry<URI,Service> entry = iter.next();

			Iterator eprs = entry.getValue().getEprInfos();
			while(!killed && eprs.hasNext())
			{
				epr = (EprInfo) eprs.next();

				if (epr.getHost().contains(server_ip)) {

					stop(entry.getValue());
					long end_kill = System.currentTimeMillis();
					logger.info("Stopped " + server_ip + " at " + (end_kill - start_of_dissemination) + "ms took " + (end_kill - start_kill) + " ms.");
					server_services.remove(entry.getKey());

					killed = true;
				}
			}
		}
	}

	protected void setParameters() {
		// set leader server on clients
		logger.debug("Setting parameters on clients...");
		java.util.Iterator<Service> iter = client_services.values().iterator();

		String prefix = ManagementConstants.ParameterElementName + "[";
		while (iter.hasNext()) {
			Service client = iter.next();
			Operation setParametersOp = client.getOperation(ManagementConstants.ManagementPortQName, ManagementConstants.SetParametersOperationName, null, null);
			ParameterValue pv = setParametersOp.createInputValue();

			ParameterValueManagement.setString(pv, prefix + "0]/" + ManagementConstants.NameElementName, "num_invocations");
			ParameterValueManagement.setString(pv, prefix + "0]/" + ManagementConstants.ValueElementName, "" + num_invocations);

			ParameterValueManagement.setString(pv, prefix + "1]/" + ManagementConstants.NameElementName, "invocation_timeout");
			ParameterValueManagement.setString(pv, prefix + "1]/" + ManagementConstants.ValueElementName, "" + invocation_timeout);
			try {
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
	}

	protected void setKnownServers() {
		java.util.ArrayList<URI> servers_eprs = new java.util.ArrayList<URI>(server_services.keySet());
		java.util.Iterator<URI> eprs_iter = server_services.keySet().iterator();

		logger.debug("Set known devices for " + server_services.size() + " servers...");
		// inform servers of other servers
		while (eprs_iter.hasNext()) {
			URI svc_epr = eprs_iter.next();
			Service svc = server_services.get(svc_epr);
			java.util.ArrayList<URI> knownDevices = (java.util.ArrayList<URI>) servers_eprs.clone();
			knownDevices.remove(svc_epr);

			Operation setMembershipOp = svc.getOperation(ManagementConstants.ManagementPortQName, ManagementConstants.SetMembershipOperationName, null, null);
			ParameterValue membershipPV = setMembershipOp.createInputValue();

			String prefix = ManagementConstants.EndpointElementName + "[";
			int size = knownDevices.size();
			logger.debug("Going to set " + size + " targets for " + svc.getEprInfos().next());
			for (int j = 0; j < size; j++) {
				ParameterValueManagement.setString(membershipPV, prefix + j + "]", knownDevices.get(j).toString());
			}

			logger.info("Going to send " + membershipPV);
			try {
				setMembershipOp.invoke(membershipPV, CredentialInfo.EMPTY_CREDENTIAL_INFO);
			} catch (InvocationException ex) {
				logger.error(ex.getMessage(), ex);
			} catch (CommunicationException ex) {
				logger.error(ex.getMessage(), ex);
			}
		}
		logger.info("Known servers were set!");
	}

	protected void startServers() {
		java.util.Iterator<Service> services_iter = server_services.values().iterator();

		logger.info("Invoking start of dissemination on " + server_services.size() + " servers...");
		while (services_iter.hasNext()) {
			Service server = services_iter.next();
			Operation op = server.getOperation(ManagementConstants.ManagementPortQName, ManagementConstants.StartDisseminationOperationName, null, null);
			ParameterValue pv = op.createInputValue();
			try {
				op.invoke(pv, CredentialInfo.EMPTY_CREDENTIAL_INFO);
			} catch (InvocationException ex) {
				logger.error(ex.getMessage(), ex);
			} catch (CommunicationException ex) {
				logger.error(ex.getMessage(), ex);
			} catch (AuthorizationException ex) {
				logger.error(ex.getMessage(), ex);
			}
		}
	}

	protected void selectLeader() {
		int num = server_services.size();
		URI leaderURI = null;
		Service leader_service = null;

		if (num > 1) {
			// select leader randomly
			int leader_index = random.nextInt(num);

			leaderURI = (URI) server_services.keySet().toArray()[leader_index];
			leader_service = server_services.get(leaderURI);
		} else {
			// only one server is online
			if (!server_services.isEmpty()) {
				leaderURI = (URI) server_services.keySet().toArray()[0];
				leader_service = server_services.get(leaderURI);
			}
		}

		if ((leaderURI != null) && (leader_service != null)) {
			Device leaderDevice = null;
			try {
				leaderDevice = getDeviceReference(new EndpointReference(leaderURI), DPWSCommunicationManager.COMMUNICATION_MANAGER_ID).getDevice();
			} catch (CommunicationException ex) {
				logger.error(ex.getMessage(), ex);
			}

			if (leaderDevice != null) {
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
				while (iter.hasNext()) {
					Service client = iter.next();
					Operation setMembershipOp = client.getOperation(ManagementConstants.ManagementPortQName, ManagementConstants.SetMembershipOperationName, null, null);
					ParameterValue pv = setMembershipOp.createInputValue();

					ParameterValueManagement.setString(pv, prefix, raftServiceEprInfo.getXAddressAsString());
					try {
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

					try {
						// let clients have some time to contact leader
						Thread.sleep(500);
					} catch (InterruptedException ex) {
						logger.error(ex.getMessage(), ex);
					}
				}

				try {
					// let clients have some time to contact leader
					Thread.sleep(2000);
				} catch (InterruptedException ex) {
					logger.error(ex.getMessage(), ex);
				}
			}
			logger.info("Finished setting server on clients.");
		}
	}

	public void startDissemination() {
		int num = client_services.size();
		logger.debug("Subscribing EndDissemination Op on " + num + " clients.");

		java.util.Iterator<Service> iter = client_services.values().iterator();

		int num_subs = 0;

		while (iter.hasNext()) {
			Service client = iter.next();
			EventSource endDisseminationOp = client.getEventSource(ManagementConstants.ManagementPortQName, ManagementConstants.EndDisseminationElementName, null, null);
			try {
				ClientSubscription subscription = endDisseminationOp.subscribe(this, 0, CredentialInfo.EMPTY_CREDENTIAL_INFO);
				if (subscription != null) {
					logger.debug("Subscribed to client with event sink: " + subscription.getEventSink().toString());
					subscriptions.add(subscription);
					num_subs++;
				}
			} catch (EventingException ex) {
				logger.error(ex.getMessage(), ex);
			} catch (IOException ex) {
				logger.error(ex.getMessage(), ex);
			} catch (CommunicationException ex) {
				logger.error(ex.getMessage(), ex);
			}

		}

		logger.info("Subscribed to " + num_subs + " clients!");

		logger.debug("Invoking start of dissemination on " + num + " clients...");
		// start messages dissemination
		iter = client_services.values().iterator();

		while (iter.hasNext()) {
			Service client = iter.next();
			Operation op = client.getOperation(ManagementConstants.ManagementPortQName, ManagementConstants.StartDisseminationOperationName, null, null);
			ParameterValue pv = op.createInputValue();
			try {
				op.invoke(pv, CredentialInfo.EMPTY_CREDENTIAL_INFO);
			} catch (InvocationException ex) {
				logger.error(ex.getMessage(), ex);
			} catch (CommunicationException ex) {
				logger.error(ex.getMessage(), ex);
			} catch (AuthorizationException ex) {
				logger.error(ex.getMessage(), ex);
			}
		}

		logger.info("Ended startDissemination method...");
	}

	@Override
	public ParameterValue eventReceived(ClientSubscription subscription, URI actionURI, ParameterValue parameterValue) {
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

		if (dissCounter == client_services.size()) {
			lock.lock();
			try {
				diss = true;
				disseminated.signal();
			} finally {
				lock.unlock();
			}
		}

		long time = System.nanoTime() - now;
		logger.info("Returning took " + time + "ns... Clients terminated: " + dissCounter);

		return null;
	}

	public void executeAfterDissemination() {
		logger.info("Waiting for dissemination end...");

		lock.lock();

		try {
			while (!diss) {
				disseminated.await();
			}

		} catch (InterruptedException ex) {
			logger.error(ex.getMessage(), ex);
		} finally {
			lock.unlock();
		}

		try {
			String filename = test_name + "_"
					+ num_servers + "s_"
					+ num_clients + "c_"
					+ num_invocations + "x_"
					+ invocation_timeout + "ms_";

			try {
				// after dissemination wait some time
				Thread.sleep(5000);
			} catch (InterruptedException ex) {
				logger.error(ex.getMessage(), ex);
			}

			logger.info("Invoking WriteStats on devices...");
			// signal all clients to write stats
			java.util.Iterator<Service> iter = client_services.values().iterator();
			int counter = 0;
			while (iter.hasNext()) {
				Service svc = iter.next();

				Object epr = svc.getEprInfos().next();
				logger.debug("Invoking WriteStats on " + epr);
				writeStats(svc, filename + (counter++) + ".csv");
				logger.debug("Invoked WriteStats on " + epr);

				try {
					// after dissemination wait some time
					Thread.sleep(200);
				} catch (InterruptedException ex) {
					logger.error(ex.getMessage(), ex);
				}

				stop(svc);
				logger.info("Stopped service on " + epr);
			}

			iter = server_services.values().iterator();
			while (iter.hasNext()) {
				Service svc = iter.next();
				Object epr = svc.getEprInfos().next();
				stop(svc);
				logger.info("Stopped service on " + epr);
			}

			logger.info("Ended execution.");

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			shutdown();
		}

		shutdown();
	}

	protected void stop(Service service) {
		Operation stopOp = service.getOperation(ManagementConstants.ManagementPortQName, ManagementConstants.StopOperationName, null, null);

		ParameterValue pv = stopOp.createInputValue();
		try {
			stopOp.invoke(pv, CredentialInfo.EMPTY_CREDENTIAL_INFO);
		} catch (InvocationException ex) {
			logger.error(ex.getMessage(), ex);
		} catch (CommunicationException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	protected void writeStats(Service service, String filename) {
		Operation writeStatsOp = service.getOperation(ManagementConstants.ManagementPortQName, ManagementConstants.WriteStatsOperationName, null, null);

		ParameterValue pv = writeStatsOp.createInputValue();
		ParameterValueManagement.setString(pv, ManagementConstants.FilenameElementName, filename);
		try {
			writeStatsOp.invoke(pv, CredentialInfo.EMPTY_CREDENTIAL_INFO);
		} catch (InvocationException ex) {
			logger.error(ex.getMessage(), ex);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}

	}

	protected String getStats(Service service) {
		String ret = "";

		Operation getStatsOp = service.getOperation(ManagementConstants.ManagementPortQName, ManagementConstants.GetStatsOperationName, null, null);

		ParameterValue pv = getStatsOp.createInputValue();
		try {
			ParameterValue retPV = getStatsOp.invoke(pv, CredentialInfo.EMPTY_CREDENTIAL_INFO);

			if (retPV != null) {
				logger.debug("Received Stats: " + retPV);
				ret = ParameterValueManagement.getString(retPV, ManagementConstants.GetStatsResponseName);
			}
		} catch (InvocationException ex) {
			logger.error(ex.getMessage(), ex);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}

		return ret;
	}

	protected void shutdown() {
		try {
			Thread.sleep(500);
		} catch (InterruptedException ex) {
			logger.error(ex);
		}

		logger.info("Shutting down...");

		CoreFramework.stop();
		System.exit(0);
	}

	private Service getManagementService(DeviceReference devRef) {
		Service svc = null;
		try {
			Device device = devRef.getDevice();
			svc = device.getServiceReference(ManagementConstants.ManagementServiceId, SecurityKey.EMPTY_KEY).getService();
		} catch (CommunicationException ex) {
			logger.error(ex.getMessage(), ex);
		}
		return svc;
	}

	public static void main(String[] args) {
		CoreFramework.start(null);

		Manager manager = new Manager();

		logger.info("Manager executing...");
		try {
			manager.initConstants(args);

			manager.executeAfterDissemination();
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}

		logger.info("Manager terminated executing.");

	}
}
