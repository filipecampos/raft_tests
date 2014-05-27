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

public class SetMembershipOperation extends ManagementOperation {

	static Logger logger = Logger.getLogger(SetMembershipOperation.class);

	public SetMembershipOperation(ManagementService svc)
	{
		super(ManagementConstants.SetMembershipOperationName, svc);

		initInput();
		initOutput();
	}

	@Override
	public ParameterValue invokeImpl(ParameterValue parameterValue, CredentialInfo cred) {
		ManagedDevice device = getService().getDevice();

		if(device != null)
		{
			// get list of target endpoints
			String prefix = ManagementConstants.EndpointElementName;
			int count = parameterValue.getChildrenCount(prefix);
			logger.debug("Got " + count + " new targets");

			if(count > 0)
			{
				String[] targets = new String[count];

				for(int i=0; i < count; i++)
				{
					String indexed_prefix = prefix + "[" + i + "]";
					targets[i] = ParameterValueManagement.getString(parameterValue,indexed_prefix);
				}

				device.setKnownDevices(targets);
			}
		}

		return createOutputValue();
	}

	@Override
	protected void initInput() {
		Element endpoint = new Element(ManagementConstants.EndpointElementQName, SchemaUtil.TYPE_ANYURI);
		// set unlimited number of endpoint elements
		endpoint.setMaxOccurs(-1);
		ComplexType targetsListType = new ComplexType(ManagementConstants.TargetsListTypeQName, ComplexType.CONTAINER_SEQUENCE);
		targetsListType.addElement(endpoint);

		ComplexType setMembershipType = new ComplexType(ManagementConstants.SetMembershipRequestTypeQName, ComplexType.CONTAINER_SEQUENCE);
		setMembershipType.addElement(new Element(ManagementConstants.TargetsListElementQName, targetsListType));

		Element in = new Element(ManagementConstants.SetMembershipRequestMessageQName, targetsListType);
		setInput(in);
	}

	@Override
	protected void initOutput() {
		Element out = new Element(ManagementConstants.SetMembershipResponseMessageQName);
		setOutput(out);
	}

}
