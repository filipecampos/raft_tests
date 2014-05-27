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
