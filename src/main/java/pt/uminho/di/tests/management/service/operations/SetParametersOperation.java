package pt.uminho.di.tests.management.service.operations;

import org.apache.log4j.Logger;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.schema.SchemaUtil;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.parameter.ParameterValueManagement;

import pt.uminho.di.tests.management.ManagedDevice;
import pt.uminho.di.tests.management.ManagementConstants;
import pt.uminho.di.tests.management.service.ManagementService;

public class SetParametersOperation extends ManagementOperation {

	static Logger logger = Logger.getLogger(SetParametersOperation.class);

	public SetParametersOperation(ManagementService svc)
	{
		super(ManagementConstants.SetParametersOperationName, svc);

		initInput();
		initOutput();
	}

	@Override
	public ParameterValue invokeImpl(ParameterValue parameterValue, CredentialInfo cred) {
		logger.debug("Received " + parameterValue);
		ManagedDevice device = getService().getDevice();

		if(device != null)
		{
			// get list of target endpoints
			String paramNamePrefix = ManagementConstants.NameElementName;
			String paramValuePrefix = ManagementConstants.ValueElementName;
			int count = parameterValue.getChildrenCount(ManagementConstants.ParameterElementName);
			logger.debug("Got " + count + " parameters!");

			if(count > 0)
			{
				for(int i=0; i < count; i++)
				{
					String prefix = ManagementConstants.ParameterElementName + "[" + i + "]/";
					String indexed_name_prefix = prefix + paramNamePrefix;
					String indexed_value_prefix = prefix + paramValuePrefix;
					String paramName = ParameterValueManagement.getString(parameterValue,indexed_name_prefix);
					String paramValue = ParameterValueManagement.getString(parameterValue,indexed_value_prefix);
					logger.debug("Got Parameter " + paramName + " with value " + paramValue);
					device.setParameter(paramName, paramValue);
				}
			}
		}

		return createOutputValue();
	}

	@Override
	protected void initInput() {
		Element parameterName = new Element(ManagementConstants.NameElementQName, SchemaUtil.TYPE_STRING);
		Element parameterValue = new Element(ManagementConstants.ValueElementQName, SchemaUtil.TYPE_STRING);

		ComplexType parameterType = new ComplexType(ManagementConstants.ParameterTypeQName, ComplexType.CONTAINER_SEQUENCE);
		parameterType.addElement(parameterName);
		parameterType.addElement(parameterValue);
		parameterName.setMaxOccurs(1);
		parameterValue.setMaxOccurs(1);

		Element parameter = new Element(ManagementConstants.ParameterElementQName, parameterType);
		// set unlimited number of endpoint elements
		parameter.setMaxOccurs(-1);
		ComplexType parametersListType = new ComplexType(ManagementConstants.ParametersListTypeQName, ComplexType.CONTAINER_SEQUENCE);
		parametersListType.addElement(parameter);

		ComplexType setParametersType = new ComplexType(ManagementConstants.SetParametersRequestTypeQName, ComplexType.CONTAINER_SEQUENCE);
		setParametersType.addElement(new Element(ManagementConstants.ParametersListElementQName, parametersListType));

		Element in = new Element(ManagementConstants.SetParametersRequestMessageQName, parametersListType);
		setInput(in);
	}

	@Override
	protected void initOutput() {
		Element out = new Element(ManagementConstants.SetParametersResponseMessageQName);
		setOutput(out);
	}

}
