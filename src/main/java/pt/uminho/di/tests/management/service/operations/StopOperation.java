package pt.uminho.di.tests.management.service.operations;

import org.apache.log4j.Logger;
import org.ws4d.java.schema.Element;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.parameter.ParameterValue;
import pt.uminho.di.tests.management.ManagedDevice;
import pt.uminho.di.tests.management.ManagementConstants;
import pt.uminho.di.tests.management.service.ManagementService;

public class StopOperation extends ManagementOperation {

	static Logger logger = Logger.getLogger(StopOperation.class);

	public StopOperation(ManagementService svc)
	{
		super(ManagementConstants.StopOperationName, svc);

		initInput();
	}

	@Override
	public ParameterValue invokeImpl(ParameterValue parameterValue, CredentialInfo cred) {

		logger.debug("Going to stop device...");
		// invoke stopDevice on device
		ManagedDevice device = getService().getDevice();

		if(device != null)
			device.stopDevice();

		return null;
	}

	@Override
	protected void initInput() {
		Element in = new Element(ManagementConstants.StopRequestQName);
		setInput(in);
	}

	@Override
	protected void initOutput() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
