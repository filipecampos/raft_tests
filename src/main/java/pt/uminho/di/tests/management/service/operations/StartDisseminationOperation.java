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
import org.ws4d.java.CoreFramework;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.parameter.ParameterValue;
import pt.uminho.di.tests.management.ManagementConstants;
import pt.uminho.di.tests.management.service.ManagementService;

public class StartDisseminationOperation extends ManagementOperation {

	static Logger logger = Logger.getLogger(StartDisseminationOperation.class);

	public StartDisseminationOperation(ManagementService svc) {
		super(ManagementConstants.StartDisseminationOperationName, svc);

		initInput();
	}

	@Override
	public ParameterValue invokeImpl(ParameterValue parameterValue, CredentialInfo cred) throws InvocationException {
		long start = System.currentTimeMillis();
		logger.debug("Going to start dissemination...");

		logger.debug("But first going to notify consumers to start their workers...");
		this.getService().fireStartWorkersNotification();
		logger.debug("Consumers notified.");

		try {
			Runnable producer = (Runnable) getService().getDevice();

			if (producer != null) {
				CoreFramework.getThreadPool().execute(producer);
			} else {
				logger.error("No client to start dissemination...");
			}

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}

		long time = System.currentTimeMillis() - start;

		logger.info("Returning from StartDissemination Op. Took " + time + "ms.");

		return null;
	}

	@Override
	protected void initInput() {
	}

	@Override
	protected void initOutput() {
	}
}
