package pt.uminho.di.tests.management.service.operations;

import org.ws4d.java.service.Operation;
import pt.uminho.di.tests.management.ManagementConstants;
import pt.uminho.di.tests.management.service.ManagementService;

public abstract class ManagementOperation extends Operation {

	private ManagementService service;

	public ManagementOperation(String operationName, ManagementService svc) {
		super(operationName, ManagementConstants.ManagementPortQName);
		service = svc;
	}

	@Override
	public ManagementService getService() {
		return service;
	}

	protected abstract void initInput();
	protected abstract void initOutput();
}
