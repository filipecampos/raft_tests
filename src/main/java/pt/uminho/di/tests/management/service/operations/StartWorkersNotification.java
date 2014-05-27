package pt.uminho.di.tests.management.service.operations;

import org.apache.log4j.Logger;
import org.ws4d.java.schema.Element;
import org.ws4d.java.service.DefaultEventSource;
import pt.uminho.di.tests.management.ManagementConstants;

public class StartWorkersNotification extends DefaultEventSource {

	static Logger logger = Logger.getLogger(StartWorkersNotification.class);

	public StartWorkersNotification()
	{
		super(ManagementConstants.StartWorkersNotificationName, ManagementConstants.ManagementPortQName);

		initOutput();
	}

	protected void initOutput() {
		Element out = new Element(ManagementConstants.StartWorkersElementQName);

		setOutput(out);
	}

}
