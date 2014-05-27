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
