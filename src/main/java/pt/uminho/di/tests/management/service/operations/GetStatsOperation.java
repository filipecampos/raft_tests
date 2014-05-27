package pt.uminho.di.tests.management.service.operations;

import org.apache.log4j.Logger;
import org.ws4d.java.schema.Element;
import org.ws4d.java.schema.SchemaUtil;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.parameter.ParameterValueManagement;

import pt.uminho.di.tests.management.ManagedDevice;
import pt.uminho.di.tests.management.ManagementConstants;
import pt.uminho.di.tests.management.service.ManagementService;

public class GetStatsOperation extends ManagementOperation {

	static Logger logger = Logger.getLogger(GetStatsOperation.class);

	public GetStatsOperation(ManagementService svc) {
		super(ManagementConstants.GetStatsOperationName, svc);

		initInput();
		initOutput();
	}

	@Override
	public ParameterValue invokeImpl(ParameterValue parameterValue, CredentialInfo cred) {
		// get stats to return to manager
		ManagedDevice device = getService().getDevice();
		String response = "";

		if (device != null) {
			logger.debug(device.getEndpoint() + " is going to get stats...");
			response = device.getStats();
		} else {
			logger.error("Couldn't write stats!");
		}

		logger.debug("Creating return message...");
		ParameterValue ret = createOutputValue();
		ParameterValueManagement.setString(ret, ManagementConstants.GetStatsResponseName, response);
		logger.debug("Created return message.");

		return ret;
	}

	@Override
	protected void initInput() {
	}

	@Override
	protected void initOutput() {
		Element out = new Element(ManagementConstants.GetStatsResponseQName, SchemaUtil.TYPE_STRING);
		setOutput(out);
	}
}
