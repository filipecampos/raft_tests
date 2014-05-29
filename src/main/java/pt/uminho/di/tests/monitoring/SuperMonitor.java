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
package pt.uminho.di.tests.monitoring;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ws4d.java.CoreFramework;
import org.ws4d.java.service.DefaultDevice;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.util.Log;

import pt.uminho.di.tests.management.ManagedDevice;
import pt.uminho.di.tests.management.ManagementConstants;
import pt.uminho.di.tests.management.service.ManagementService;

public class SuperMonitor extends DefaultDevice implements ManagedDevice {

	static final Logger logger = Logger.getLogger(SuperMonitor.class);
	long pid = -1;
	String interfaceName = null;
	MonitoringThread monitoringThread;

	public SuperMonitor(Long pid, String interface_name) {
		this.pid = pid;
		interfaceName = interface_name;

		monitoringThread = new MonitoringThread(pid);

		if ((interfaceName != null) && (!interfaceName.isEmpty())) {
			monitoringThread.setInterfaceName(interfaceName);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				monitoringThread.setRunning(false);
				monitoringThread.writeStatsToFile();
			}
		});
	}

	public static void main(String[] args) {
		// configure loggers
		PropertyConfigurator.configure("log4j.properties");

		if (args.length < 1) {
			logger.error("Args: <process pid> <interface_name>[optional]");
			return;
		} else {
			Long pid = Long.parseLong(args[0]);
			String interface_name = null;
			if (args.length == 2) {
				interface_name = args[1];
			}

			// mandatory: Starting the DPWS Framework.
			CoreFramework.start(null);

			SuperMonitor device = null;

			logger.info("Starting SuperMonitor...");
			// Create the server device.
			device = new SuperMonitor(pid, interface_name);

			device.setPortTypes(new QNameSet(ManagementConstants.ManagementServerTypeQName));

			ManagementService mgmtService = new ManagementService();
			mgmtService.setDevice(device);
			device.addService(mgmtService);

			//            Log.setLogLevel(Log.DEBUG_LEVEL_NO_LOGGING);
			Log.setLogLevel(Log.DEBUG_LEVEL_ERROR);
			//            Log.setLogLevel(Log.DEBUG_LEVEL_DEBUG);

			// Starting the device.
			device.startDevice();
		}
	}

	@Override
	public void startDevice() {
		try {
			start();
			monitoringThread.start();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			monitoringThread.setRunning(false);
			monitoringThread.writeStatsToFile();
			try {
				stop();
			} catch (IOException ex) {
				logger.error(ex.getMessage(), ex);
			}
			CoreFramework.stop();
		}
	}

	@Override
	public void stopDevice() {
		long start = System.currentTimeMillis();
		logger.debug("Stopping SuperMonitor...");
		try {
			// kill zookeeper server
			Process p = Runtime.getRuntime().exec("kill -9 " + pid);
			p.waitFor();

			// stop monitoring thread
			monitoringThread.setRunning(false);
			monitoringThread.writeStatsToFile();

			this.stop();
			CoreFramework.stopIgnoringInstancesCount();

		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}

		long finish = System.currentTimeMillis();
		logger.info("Stopped Zookeeper server with PID " + pid + " in " + (finish - start) + " ms.");
		System.exit(0);
	}

	@Override
	public String getStats() {
		return null;
	}

	@Override
	public void writeStats(String filename) {

	}

	@Override
	public String getEndpoint() {
		return null;
	}

	@Override
	public void setKnownDevices(String[] targets) {

	}

	@Override
	public void setParameter(String paramName, String paramValue) {

	}
}
