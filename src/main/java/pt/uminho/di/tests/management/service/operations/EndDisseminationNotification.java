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
import org.ws4d.java.service.DefaultEventSource;
import pt.uminho.di.tests.management.ManagementConstants;

public class EndDisseminationNotification extends DefaultEventSource {

	static Logger logger = Logger.getLogger(EndDisseminationNotification.class);

	public EndDisseminationNotification()
	{
		super(ManagementConstants.EndDisseminationNotificationName, ManagementConstants.ManagementPortQName);

		initOutput();
	}

	protected void initOutput() {
		Element out = new Element(ManagementConstants.EndDisseminationElementQName);

		setOutput(out);
	}

}
