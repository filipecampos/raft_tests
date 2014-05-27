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

public class WriteStatsOperation extends ManagementOperation {

	static Logger logger = Logger.getLogger(WriteStatsOperation.class);

	public WriteStatsOperation(ManagementService svc) {
		super(ManagementConstants.WriteStatsOperationName, svc);

		initInput();
		initOutput();
	}

	@Override
	public ParameterValue invokeImpl(ParameterValue parameterValue, CredentialInfo cred) {
		// get file name to write o
		String filename = ParameterValueManagement.getString(parameterValue, ManagementConstants.FilenameElementName);

		ManagedDevice device = getService().getDevice();

		if (device != null) {
			logger.debug(device.getEndpoint() + " is going to write stats on " + filename);
			device.writeStats(filename);
		} else {
			logger.error("Couldn't write stats!");
		}

		logger.debug("Returning...");

		return null;
	}

	@Override
	protected void initInput() {
		Element writeStatsElement = new Element(ManagementConstants.FilenameElementQName, SchemaUtil.TYPE_ANYURI);

		setInput(writeStatsElement);
	}

	@Override
	protected void initOutput() {
	}
}
