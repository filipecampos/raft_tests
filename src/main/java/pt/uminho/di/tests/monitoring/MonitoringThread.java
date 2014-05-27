package pt.uminho.di.tests.monitoring;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

public class MonitoringThread extends Thread {
	static final Logger logger = Logger.getLogger(MonitoringThread.class);

	long pid = -1;
	long period = 1000;
	boolean running;
	Sigar sigar;
	ArrayList<ProcessMetrics> metrics;
	String interfaceName = null;
	boolean writtenFile = false;
	String filename = "metrics.csv";

	public MonitoringThread()
	{
		sigar = new Sigar();
		metrics = new ArrayList<ProcessMetrics>();
	}

	public MonitoringThread(long pid)
	{
		this();
		// set pid of the process to monitor
		this.pid = pid;
		filename = pid + "-" + filename;
	}

	public boolean isRunning()
	{
		return running;
	}

	public void setRunning(boolean running)
	{
		this.running = running;
	}

	public long getPeriod()
	{
		return period;
	}

	public void setPeriod(long period)
	{
		this.period = period;
	}

	public String getFilename()
	{
		return filename;
	}

	public void setFilename(String filename)
	{
		this.filename = filename;
	}

	public String getInterfaceName() {
		return interfaceName;
	}

	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName;
	}

	public long getPid() {
		return pid;
	}

	@Override
	public void run()
	{
		if(pid == -1)
		{
			// get current process PID
			pid = sigar.getPid();
		}

		if(interfaceName == null)
		{
			try
			{
				NetInterfaceConfig ifConfig = sigar.getNetInterfaceConfig();
				if(!NetFlags.NULL_HWADDR.equals(ifConfig.getHwaddr()))
				{
					interfaceName = ifConfig.getName();
				}
			} catch(SigarException ex)
			{
				logger.error(ex.getMessage(), ex);
			}
		}

		logger.info("Running monitoring cycle...");
		running = true;
		try
		{
			while(running)
			{
				collectMetrics();

				sleep(period);
			}
		}
		catch(Exception e)
		{
			running = false;
		}
		logger.info("Finished monitoring cycle at " + System.currentTimeMillis());

		writeStatsToFile();
	}

	public static void main(String[] args)
	{
		if(args.length < 1)
		{
			logger.error("This application only receives an argument: the pid of the process to monitor!");
			return;
		}
		else
		{
			final MonitoringThread monitoringThread = new MonitoringThread(Long.parseLong(args[0]));
			if(args.length == 2)
				monitoringThread.setInterfaceName(args[1]);

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					monitoringThread.setRunning(false);
					monitoringThread.writeStatsToFile();
				}
			}
					);
			try
			{
				monitoringThread.start();
			}
			catch(Exception ex)
			{
				logger.error(ex.getMessage(), ex);
				monitoringThread.writeStatsToFile();
			}

		}
	}

	private void collectMetrics()
	{
		logger.debug("Collecting metrics...");
		try
		{
			//get process CPU usage
			ProcCpu procCpu = sigar.getProcCpu(pid);

			//get process memory usage
			ProcMem procMem = sigar.getProcMem(pid);

			// get network interface stats
			NetInterfaceStat netStat = sigar.getNetInterfaceStat(interfaceName);

			ProcessMetrics measurement = new ProcessMetrics(procCpu.getPercent(),
					procMem.getSize(), procMem.getResident(), procMem.getPageFaults(),
					interfaceName, netStat.getRxBytes(), netStat.getTxBytes());

			metrics.add(measurement);

		} catch(SigarException ex)
		{
			logger.error(ex.getMessage(), ex);
		}
	}

	public void writeStatsToFile()
	{
		if(!writtenFile)
		{
			logger.info("Writing metrics to " + filename + "...");
			File file = new File(filename);
			if(file.exists())
			{
				file.delete();
				try
				{
					file.createNewFile();
				} catch(IOException ex)
				{
					logger.error(ex.getMessage(), ex);
				}
			}
			FileWriter fw = null;
			try
			{
				fw = new FileWriter(file, true);
			} catch(IOException ex)
			{
				logger.error(ex.getMessage(), ex);
			}

			if(fw != null)
			{
				StringBuilder sb = new StringBuilder();
				// write title
				sb.append("CPU(%);VirtualMem(bytes);ResidentMem(bytes);PageFaults;Interface;TX(bytes);RX(bytes);\n");
				for(ProcessMetrics m : metrics)
				{
					sb.append(m.toSB());
				}

				try {
					fw.append(sb.toString());
					fw.flush();
					writtenFile = true;
				} catch (IOException ex) {
					logger.error(ex.getMessage(), ex);
				}
			}
		}
		else
			logger.warn(filename + " was already written.");
	}
}
