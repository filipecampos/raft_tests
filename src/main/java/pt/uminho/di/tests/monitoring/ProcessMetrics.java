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

public class ProcessMetrics
{
	// cpu

	double cpuUsage;
	// mem
	long virtual;
	long resident;
	long pageFaults;
	// network
	String netInterface;
	long rx;
	long tx;

	public ProcessMetrics()
	{
	}

	public ProcessMetrics(double cpuUsage, long virtual, long resident, long pageFaults, String netInterface, long rx, long tx)
	{
		this.cpuUsage = cpuUsage;
		this.virtual = virtual;
		this.resident = resident;
		this.pageFaults = pageFaults;
		this.netInterface = netInterface;
		this.rx = rx;
		this.tx = tx;
	}

	public double getCpuUsage()
	{
		return cpuUsage;
	}

	public void setCpuUsage(double cpuUsage)
	{
		this.cpuUsage = cpuUsage;
	}

	public String getNetInterface()
	{
		return netInterface;
	}

	public void setNetInterface(String netInterface)
	{
		this.netInterface = netInterface;
	}

	public long getPageFaults()
	{
		return pageFaults;
	}

	public void setPageFaults(long pageFaults)
	{
		this.pageFaults = pageFaults;
	}

	public long getResident()
	{
		return resident;
	}

	public void setResident(long resident)
	{
		this.resident = resident;
	}

	public long getRx()
	{
		return rx;
	}

	public void setRx(long rx)
	{
		this.rx = rx;
	}

	public long getTx()
	{
		return tx;
	}

	public void setTx(long tx)
	{
		this.tx = tx;
	}

	public long getVirtual()
	{
		return virtual;
	}

	public void setVirtual(long virtual)
	{
		this.virtual = virtual;
	}

	public StringBuilder toSB()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(getCpuUsage());
		sb.append(";");
		sb.append(getVirtual());
		sb.append(";");
		sb.append(getResident());
		sb.append(";");
		sb.append(getPageFaults());
		sb.append(";");
		sb.append(getNetInterface());
		sb.append(";");
		sb.append(getTx());
		sb.append(";");
		sb.append(getRx());
		sb.append(";\n");

		return sb;
	}
}
