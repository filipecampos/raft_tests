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
