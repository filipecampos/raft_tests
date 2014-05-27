package pt.uminho.di.tests.zookeeper;

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
import org.ws4d.java.CoreFramework;
import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.service.DefaultDevice;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.util.IDGenerator;
import org.ws4d.java.util.Log;
import pt.uminho.di.tests.management.ManagedDevice;
import pt.uminho.di.tests.management.ManagementConstants;
import pt.uminho.di.tests.management.service.ManagementService;
import pt.uminho.di.tests.raft.ManagedSimpleClient;

public class ManagedZooKeeperClient extends ManagedSimpleClient implements Watcher, ManagedDevice {

	static final Logger logger = Logger.getLogger(ManagedZooKeeperClient.class);

	ZooKeeper zooKeeper;

	public ManagedZooKeeperClient(String connectionString, int sessionTimeout, int num, int period) throws IOException
	{
		super(num, period);
		zooKeeper = new ZooKeeper(connectionString, sessionTimeout, this);
		logger.info("Connecting to zookeeper servers : " + connectionString);

		initializeLogDirectories();
	}

	public ManagedZooKeeperClient(String address, int port, int sessionTimeout, int num, int period) throws IOException
	{
		this(address + ":" + port, sessionTimeout, num, period);
	}

	public void initializeLogDirectories()
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

	public boolean createEntry(String command, String parameters, int iter)
	{
		boolean success = false;
		// create log entry
		String uuid = IDGenerator.getUUID();
		String path = "/log/entries/" + uuid + ".dat";
		String dataStr = command + ";" + parameters;
		byte[] data = dataStr.getBytes();

		try
		{
			if(start[iter] == null)
				start[iter] = System.nanoTime();
			String returnPath = zooKeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			finish[iter] = System.nanoTime();
			success = path.equalsIgnoreCase(returnPath);
			results[iter] = Boolean.toString(success);

		} catch(KeeperException ex)
		{
			logger.error(ex.getMessage(), ex);
			success = false;
		} catch(InterruptedException ex)
		{
			logger.error(ex.getMessage(), ex);
			success = false;
		}

		return success;
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

	public static void main(String[] args)
	{
		// configure loggers
		PropertyConfigurator.configure("log4j.properties");

		String connectionString = "192.168.111.220:2181";
		int num = 10;
		long period = 1000;
		int sessionTimeout = 60000;

		if (args.length == 3) {
			connectionString = args[0];
			num = Integer.parseInt(args[1]);
			period = Long.parseLong(args[2]);
		}


		// mandatory: Starting the DPWS Framework.
		CoreFramework.start(args);

		ManagedZooKeeperClient client = null;
		DefaultDevice device = null;
		try
		{
			// Create client...
			client = new ManagedZooKeeperClient(connectionString, sessionTimeout, num, (int) period);

			device = new DefaultDevice(DPWSCommunicationManager.COMMUNICATION_MANAGER_ID);

			device.setPortTypes(new QNameSet(ManagementConstants.ManagementClientTypeQName));

			ManagementService mgmtService = new ManagementService();
			mgmtService.setDevice(client);
			device.addService(mgmtService);

			client.setMgmtService(mgmtService);
			client.setOwnEPR(device.getEndpointReference());

//            Log.setLogLevel(Log.DEBUG_LEVEL_NO_LOGGING);
			Log.setLogLevel(Log.DEBUG_LEVEL_ERROR);
//            Log.setLogLevel(Log.DEBUG_LEVEL_DEBUG);

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

	public void process(WatchedEvent event)
	{
		logger.info("Invoked prcess event " + event);
	}

	@Override
	public void startDevice()
	{
		logger.info("Starting Device...Doing nothing!");
	}

	@Override
	public Boolean myInvokeInsertCommandOp(String command, String parameters, int iter)
	{
		return createEntry(command, parameters, iter);
	}

}
