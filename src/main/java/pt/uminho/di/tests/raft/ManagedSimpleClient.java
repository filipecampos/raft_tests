package pt.uminho.di.tests.raft;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ws4d.java.CoreFramework;
import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.DefaultDevice;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.Service;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.parameter.ParameterValueManagement;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.EprInfo;
import org.ws4d.java.types.HelloData;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.IDGenerator;

import pt.uminho.di.raft.Constants;
import pt.uminho.di.raft.entities.BasicClient;
import pt.uminho.di.raft.service.operations.InsertCommandOperation;
import pt.uminho.di.tests.management.ManagedDevice;
import pt.uminho.di.tests.management.ManagementConstants;
import pt.uminho.di.tests.management.service.ManagementService;

public class ManagedSimpleClient extends BasicClient implements Runnable, ManagedDevice {

	static Logger logger = Logger.getLogger(ManagedSimpleClient.class);
	protected Long[] start;
	protected Long[] finish;
	protected String[] results;
	protected long time;
	ManagementService mgmtService;
	EndpointReference ownEpr;

	public ManagedSimpleClient(Integer p, Integer n) {
		super(p, n);
		start = new Long[n];
		finish = new Long[n];
		results = new String[n];
	}

	public ManagementService getMgmtService() {
		return mgmtService;
	}

	public void setMgmtService(ManagementService mgmtService) {
		this.mgmtService = mgmtService;
	}

	@Override
	public String getStats() {
		StringBuilder sb = new StringBuilder("Lats(ns);");
		String address;

		if (mgmtService != null) {
			address = ((EprInfo) mgmtService.getEprInfos().next()).getXAddress().getHostWithPort();
		} else {
			address = "client";
		}
		// write lats
		sb.append(address);
		sb.append(';');
		for (int i = 0; i < start.length; i++) {
			sb.append(finish[i] - start[i]);
			sb.append(';');
		}

		sb.append("\nResponses;");
		sb.append(address);
		sb.append(';');
		for (int i = 0; i < results.length; i++) {
			sb.append(results[i]);
			sb.append(';');
		}
		sb.append("\nThroughput(ops/s);");
		sb.append(address);
		sb.append(';');
		sb.append((1000 * getNum()) / time);

		sb.append("\n");

		return sb.toString();
	}

	@Override
	public void writeStats(String filename) {
		try {
			File file = new File(filename);
			if (file.exists()) {
				file.delete();
				try {
					file.createNewFile();
				} catch (IOException ex) {
					logger.error(ex.getMessage(), ex);
				}
			}
			FileWriter fileWriter = new FileWriter(filename);
			fileWriter.append(getStats());
			fileWriter.flush();
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	@Override
	public String getEndpoint() {
		return ownEpr.getAddress().toString();
	}

	@Override
	public void startDevice() {
		// start the dissemination or the search for devices
		searchRaftDevices();
	}

	@Override
	public void stopDevice() {
		CoreFramework.stop();
	}

	@Override
	public void setKnownDevices(String[] targets) {
		logger.debug("Received these targets: " + targets);
		if (targets.length > 0) {
			// set known devices - first server considered as leader
			for (int i = 0; i < targets.length; i++) {
				if ((targets[i] != null) && (!targets[i].isEmpty())) {
					//insert leader service
					EndpointReference epr = new EndpointReference(new URI(targets[i]));

					try {
						Service raftSvc = this.getServiceReference(epr, DPWSCommunicationManager.COMMUNICATION_MANAGER_ID).getService();

						if (raftSvc != null) {
							insertService(epr, raftSvc);
						} else {
							logger.error("Did not get service from " + epr);
						}

						if (i == 0) {
							this.setLeaderService(epr);
						}

						Thread.sleep(500);
					} catch (CommunicationException ex) {
						logger.error(ex.getMessage(), ex);
					} catch (InterruptedException ex) {
						logger.error(ex.getMessage(), ex);
					}
				}
			}
		}
	}

	@Override
	protected void execute() {
		logger.debug("Do nothing....");
	}

	protected void myexecute() {
		String command = "";
		String parameters = "";
		int n = getNum();
		long sleepTime = getPeriod();
		boolean success = false;

		logger.info("Starting for loop...");
		try {
			long start_loop = System.currentTimeMillis();
			for (int i = 0; i < n; i++) {
				success = false;
				logger.debug("Invoking " + i + " iteration...");
				// invoke insert command
				command = "x=" + i;
				parameters = "y,z";

				int times = 1;
				long startTime = System.nanoTime();
				while (!success) {
					success = myInvokeInsertCommandOp(command, parameters, i);
					logger.debug("Invoked " + (times++) + " times. Success? " + success);
				}
				logger.debug("Took " + (System.nanoTime() - startTime) + " ns.");

				// sleep
				Thread.sleep(sleepTime);
			}
			time = System.currentTimeMillis() - start_loop;

		} catch (InterruptedException ex) {
			logger.error(ex.getMessage(), ex);
		}
		logger.info("Finished for loop at " + System.currentTimeMillis() + " and took " + time + "ms.");

		if (mgmtService != null) {
			mgmtService.fireEndDisseminationNotification();
			logger.debug("Fired EndDissemination Notification!");
		}
	}

	@Override
	public void setParameter(String paramName, String paramValue) {
		if (paramName.equalsIgnoreCase("invocation_timeout")) {
			setPeriod(Integer.parseInt(paramValue));
			logger.debug("Set invocation_timeout " + getPeriod());
		} else if (paramName.equalsIgnoreCase("num_invocations")) {
			setNum(Integer.parseInt(paramValue));
			start = new Long[getNum()];
			finish = new Long[getNum()];
			results = new String[getNum()];
			logger.debug("Set num_invocations " + getNum());
		}
	}

	@Override
	public void helloReceived(HelloData helloData) {
		logger.debug("Received hello from epr " + helloData.getEndpointReference());
		if(!helloData.getEndpointReference().equals(ownEpr))
		{
			logger.debug("Received hello from another epr " + helloData.getEndpointReference());
			super.helloReceived(helloData);
		}
		else
			logger.debug("Received hello from own epr " + ownEpr);
	}

	public static void main(String[] args) {
		// configure loggers
		PropertyConfigurator.configure("log4j.properties");

		Integer num = 10;
		Integer period = 5000;

		switch (args.length) {
		case 2: {
			period = Integer.parseInt(args[1]);
			num = Integer.parseInt(args[0]);
		}
		}

		// mandatory: Starting the DPWS Framework.
		CoreFramework.start(args);

		// Create client...
		ManagedSimpleClient client = new ManagedSimpleClient(period, num);

		DefaultDevice device = new DefaultDevice(DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);

		device.setPortTypes(new QNameSet(ManagementConstants.ManagementClientTypeQName));

		ManagementService mgmtService = new ManagementService();
		mgmtService.setDevice(client);
		device.addService(mgmtService);

		client.setMgmtService(mgmtService);
		client.setOwnEPR(device.getEndpointReference());

		try {
			device.start();
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);

			try {
				device.stop();
			} catch (IOException ex1) {
				logger.error(ex1.getMessage(), ex1);
			}
			client.stopDevice();
		}
	}

	protected void setOwnEPR(EndpointReference endpointReference) {
		ownEpr = endpointReference;
	}

	@Override
	public void run() {
		myexecute();
	}

	public Boolean myInvokeInsertCommandOp(String command, String parameters, int iter) {
		Boolean responseSuccess = false;
		String uid = IDGenerator.getUUID();
		InsertCommandOperation op = new InsertCommandOperation(null);
		ParameterValue pv = op.getRequestPV(uid, command, parameters);

		try {
			if (currentLeader != null) {
				Operation leaderOp = devicesInsertCommandOp.get(currentLeader);
				logger.info("Invoking Request PV: " + pv + " on " + currentLeader);
				if(start[iter] == null)
					start[iter] = System.nanoTime();
				ParameterValue response = leaderOp.invoke(pv, CredentialInfo.EMPTY_CREDENTIAL_INFO);
				finish[iter] = System.nanoTime();
				logger.debug("Received Response PV: " + response);

				String responseSuccessStr = ParameterValueManagement.getString(response, Constants.SuccessElementName);
				results[iter] = responseSuccessStr;
				responseSuccess = Boolean.parseBoolean(responseSuccessStr);

				String result = "";
				if (response.getChildrenCount(Constants.ResultElementName) > 0) {
					result = ParameterValueManagement.getString(response, Constants.ResultElementName);
				}

				logger.info("Received Response : " + response);
				if (!responseSuccess) {

					if (response.getChildrenCount(Constants.LeaderAddressElementName) > 0) {
						// invoked a follower server. retrieving leader epr...
						String responseLeaderStr = ParameterValueManagement.getString(response, Constants.LeaderAddressElementName);
						EndpointReference responseLeader = new EndpointReference(new URI(responseLeaderStr));
						logger.debug("Got leader epr: " + responseLeaderStr);

						// set new leader
						setLeaderService(responseLeader);
					}
				}

			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			// select another leader
			logger.info("Connecting to another server at iter " + iter + ". Current leader is: " + currentLeader);
			connectToAnotherServer();
			responseSuccess = false;
		}

		return responseSuccess;
	}

	protected void connectToAnotherServer() {
		try {
			// in order to compare to zookeeper's client
			int sleepTime = r.nextInt(1000);
			logger.info("Sleeping for " + sleepTime + " ms before contacting new leader at " + System.currentTimeMillis());
			Thread.sleep(sleepTime);
		} catch (InterruptedException ex) {
			logger.error(ex.getMessage(), ex);
		}

		super.connectToAnotherServer();
	}
}
