package pt.uminho.di.tests.management.service.operations;

import org.apache.log4j.Logger;
import org.ws4d.java.schema.Element;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.parameter.ParameterValue;
import pt.uminho.di.tests.management.ManagedDevice;
import pt.uminho.di.tests.management.ManagementConstants;
import pt.uminho.di.tests.management.service.ManagementService;

public class StartOperation extends ManagementOperation {

	static Logger logger = Logger.getLogger(StartOperation.class);

	public StartOperation(ManagementService svc)
	{
		super(ManagementConstants.StartOperationName, svc);

		initInput();
	}

	@Override
	public ParameterValue invokeImpl(ParameterValue parameterValue, CredentialInfo cred) {

		logger.debug("Going to start device...");
		// invoke stopDevice on device
		ManagedDevice device = getService().getDevice();

		if(device != null)
			device.startDevice();

		return null;
	}

	@Override
	protected void initInput() {
		Element in = new Element(ManagementConstants.StartRequestQName);
		setInput(in);
	}

	@Override
	protected void initOutput() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
