package pt.uminho.di.tests.management.service;

import pt.uminho.di.tests.management.service.operations.EndDisseminationNotification;
import pt.uminho.di.tests.management.service.operations.WriteStatsOperation;
import pt.uminho.di.tests.management.service.operations.GetStatsOperation;
import pt.uminho.di.tests.management.service.operations.GetEndpointOperation;
import pt.uminho.di.tests.management.service.operations.SetMembershipOperation;
import pt.uminho.di.tests.management.service.operations.StartDisseminationOperation;
import pt.uminho.di.tests.management.service.operations.StopOperation;
import pt.uminho.di.tests.management.service.operations.StartWorkersNotification;
import org.apache.log4j.Logger;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.DefaultService;
import org.ws4d.java.service.parameter.ParameterValue;
import pt.uminho.di.tests.management.ManagedDevice;
import pt.uminho.di.tests.management.ManagementConstants;
import pt.uminho.di.tests.management.service.operations.SetParametersOperation;
import pt.uminho.di.tests.management.service.operations.StartOperation;

public class ManagementService extends DefaultService {

	static Logger logger = Logger.getLogger(ManagementService.class);

	private ManagedDevice device;

	protected GetEndpointOperation getEndpointOp;
	protected SetMembershipOperation setMembershipOp;
	protected SetParametersOperation setParametersOp;
	protected StartDisseminationOperation startDisseminationOp;
	protected StopOperation stopOp;
	protected StartOperation startOp;
	protected WriteStatsOperation writeStatsOp;
	protected GetStatsOperation getStatsOp;

	protected EndDisseminationNotification endDisseminationNot;

	protected StartWorkersNotification startWorkersNot;

	public ManagementService()
	{
		this.setServiceId(ManagementConstants.ManagementServiceId);

		initializeOperations();
	}

	protected void initializeOperations()
	{
		getEndpointOp = new GetEndpointOperation(this);
		addOperation(getEndpointOp);
		setMembershipOp = new SetMembershipOperation(this);
		addOperation(setMembershipOp);
		setParametersOp = new SetParametersOperation(this);
		addOperation(setParametersOp);
		startDisseminationOp = new StartDisseminationOperation(this);
		addOperation(startDisseminationOp);
		startOp = new StartOperation(this);
		addOperation(startOp);
		stopOp = new StopOperation(this);
		addOperation(stopOp);
		writeStatsOp = new WriteStatsOperation(this);
		addOperation(writeStatsOp);
		getStatsOp = new GetStatsOperation(this);
		addOperation(getStatsOp);

		endDisseminationNot = new EndDisseminationNotification();
		addEventSource(endDisseminationNot);
	}

	public ManagedDevice getDevice() {
		return device;
	}

	public void setDevice(ManagedDevice device) {
		this.device = device;
	}

	public void initializeStartWorkersNotification()
	{
		startWorkersNot = new StartWorkersNotification();
		addEventSource(startWorkersNot);
	}

	public void fireEndDisseminationNotification()
	{
		try
		{
			ParameterValue pv = endDisseminationNot.createOutputValue();
			endDisseminationNot.fire(pv, 0, CredentialInfo.EMPTY_CREDENTIAL_INFO);
		}
		catch(Exception e)
		{
			logger.error(e.getMessage(), e);
		}
	}

	public void fireStartWorkersNotification()
	{
		if(startWorkersNot != null)
		{
			try
			{
				ParameterValue pv = startWorkersNot.createOutputValue();
				startWorkersNot.fire(pv, 0, CredentialInfo.EMPTY_CREDENTIAL_INFO);
			}
			catch(Exception e)
			{
				logger.error(e.getMessage(), e);
			}
		}
	}
}
