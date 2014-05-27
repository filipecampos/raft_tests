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

public class GetEndpointOperation extends ManagementOperation {

	static Logger logger = Logger.getLogger(GetEndpointOperation.class);

	public GetEndpointOperation(ManagementService svc)
	{
		super(ManagementConstants.GetEndpointOperationName, svc);

		initOutput();
	}

	@Override
	public ParameterValue invokeImpl(ParameterValue parameterValue, CredentialInfo cred) {
		String endpoint = "";
		ManagedDevice device = getService().getDevice();

		if(device != null)
			endpoint = device.getEndpoint();

		ParameterValue response = createOutputValue();
		ParameterValueManagement.setString(response, ManagementConstants.EndpointElementName, endpoint);

		return response;
	}

	@Override
	protected void initInput() {
	}

	@Override
	protected void initOutput() {
		Element element = new Element(ManagementConstants.EndpointElementQName, SchemaUtil.TYPE_ANYURI);

		setOutput(element);
	}

}
