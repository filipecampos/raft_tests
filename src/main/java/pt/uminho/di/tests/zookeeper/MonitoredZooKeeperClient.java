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

import java.io.IOException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ws4d.java.CoreFramework;
import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.service.DefaultDevice;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.util.Log;
import pt.uminho.di.tests.management.ManagementConstants;
import pt.uminho.di.tests.management.service.ManagementService;
import pt.uminho.di.tests.monitoring.MonitoringThread;

public class MonitoredZooKeeperClient extends ManagedZooKeeperClient {

    static final Logger logger = Logger.getLogger(MonitoredZooKeeperClient.class);
    MonitoringThread monitoringThread;
    String id;

    public MonitoredZooKeeperClient(String id, String connectionString, int sessionTimeout, int num, int period) throws IOException {
        super(connectionString, sessionTimeout, num, period);
        this.id = id;
    }

    public MonitoredZooKeeperClient(String id, String address, int port, int sessionTimeout, int num, int period) throws IOException {
        super(address, port, sessionTimeout, num, period);
        this.id = id + ":" + address;
    }

    public static void main(String[] args) {
        // configure loggers
        PropertyConfigurator.configure("log4j.properties");

        String id = "0";
        String connectionString = "192.168.111.226:2181";
        int num = 10;
        long period = 1000;
        int sessionTimeout = 60000;

        if (args.length >= 4) {
            id = args[0];
            connectionString = args[1];
            num = Integer.parseInt(args[2]);
            period = Long.parseLong(args[3]);
        }
        else
        {
            logger.error("Couldn't read args!");
            System.exit(1);
        }


        // mandatory: Starting the DPWS Framework.
        CoreFramework.start(null);

        MonitoredZooKeeperClient client = null;
        DefaultDevice device = null;
        try {
            // Create client...
            logger.info("Creating client " + id + " connect:" + connectionString + "; timeout:" + sessionTimeout + "; iters:" + num + "; period: " + period);
            client = new MonitoredZooKeeperClient(id, connectionString, sessionTimeout, num, (int) period);

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

    @Override
    public void run() {
        monitoringThread = new MonitoringThread();
        monitoringThread.start();
        monitoringThread.setFilename("client" + id + ".csv");

        super.run();

        if (monitoringThread != null) {
            monitoringThread.setRunning(false);
            try {
                monitoringThread.join();
            } catch (InterruptedException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void stopDevice() {
        if (monitoringThread != null) {
            monitoringThread.setRunning(false);
            try {
                monitoringThread.join();
            } catch (InterruptedException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        super.stopDevice();
    }
}
