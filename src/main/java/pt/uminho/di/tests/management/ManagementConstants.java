package pt.uminho.di.tests.management;

import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;

public abstract class ManagementConstants {

	// measuring parameters
	public final static long terminateAtUpdate = 5;
	public final static int numberOfDevices = 30;

	// ms
	public final static long updateInitialDelay = 3000;
	public final static long updatePeriod = 1000;


	public final static String NameSpace = "http://gsd.di.uminho.pt/ws/2014/01/management";

	public final static String ManagementClientTypeName = "ManagementClientType";
	public final static QName ManagementClientTypeQName = new QName(ManagementClientTypeName, NameSpace);

	public final static String ManagementServerTypeName = "ManagementServerType";
	public final static QName ManagementServerTypeQName = new QName(ManagementServerTypeName, NameSpace);

	// Management Service
	public final static String ManagementServiceName = "ManagementService";
	public final static QName ManagementServiceQName = new QName(ManagementServiceName, NameSpace);

	public final static String ManagementPortName = "ManagementPortType";
	public final static QName ManagementPortQName = new QName(ManagementPortName, NameSpace);

	public final static URI ManagementServiceId = new URI(ManagementServiceName);

	// Operations
	public final static String GetEndpointOperationName = "GetEndpoint";
	public final static QName GetEndpointOperationQName = new QName(GetEndpointOperationName, NameSpace);

	public final static String EndpointElementName = "Endpoint";
	public final static QName EndpointElementQName = new QName(EndpointElementName, NameSpace);



	public final static String SetMembershipOperationName = "SetMembership";
	public final static QName SetMembershipOperationQName = new QName(SetMembershipOperationName, NameSpace);

	public final static String StartDisseminationOperationName = "StartDissemination";
	public final static QName StartDisseminationOperationQName = new QName(StartDisseminationOperationName, NameSpace);

	public final static String EndDisseminationNotificationName = "EndDissemination";
	public final static QName EndDisseminationNotificationQName = new QName(EndDisseminationNotificationName, NameSpace);

	public final static String StartWorkersNotificationName = "StartWorkers";
	public final static QName StartWorkersNotificationQName = new QName(StartWorkersNotificationName, NameSpace);

	public final static String StopOperationName = "Stop";
	public final static QName StopOperationQName = new QName(StopOperationName, NameSpace);

	public final static String StartOperationName = "Start";
	public final static QName StartOperationQName = new QName(StartOperationName, NameSpace);

	public final static String WriteStatsOperationName = "WriteStats";
	public final static QName WriteStatsOperationQName = new QName(WriteStatsOperationName, NameSpace);

	public final static String GetStatsOperationName = "GetStats";
	public final static QName GetStatsOperationQName = new QName(GetStatsOperationName, NameSpace);

	public final static String SetPublisherOperationName = "SetPublisher";
	public final static QName SetPublisherOperationQName = new QName(SetPublisherOperationName, NameSpace);


	// elements
	public final static String FilenameElementName = "Filename";
	public final static QName FilenameElementQName = new QName(FilenameElementName, NameSpace);

	public final static String WriteStatsResponseName = "WroteStats";
	public final static QName WriteStatsResponseQName = new QName(WriteStatsResponseName, NameSpace);

	public final static String GetStatsResponseName = "GotStats";
	public final static QName GetStatsResponseQName = new QName(GetStatsResponseName, NameSpace);

	public final static String SetMembershipRequestMessageName = "NewMembership";
	public final static QName SetMembershipRequestMessageQName = new QName(SetMembershipRequestMessageName, NameSpace);

	public final static String SetMembershipResponseMessageName = "MembershipSet";
	public final static QName SetMembershipResponseMessageQName = new QName(SetMembershipResponseMessageName, NameSpace);

	public final static String SetMembershipRequestTypeName = "NewMembershipType";
	public final static QName SetMembershipRequestTypeQName = new QName(SetMembershipRequestTypeName, NameSpace);

	public final static String EndDisseminationElementName = "EndDissemination";
	public final static QName EndDisseminationElementQName = new QName(EndDisseminationElementName, NameSpace);

	public final static String StartWorkersElementName = "StartWorkers";
	public final static QName StartWorkersElementQName = new QName(StartWorkersElementName, NameSpace);

	public final static String StopRequestName = "Stop";
	public final static QName StopRequestQName = new QName(StopRequestName, NameSpace);

	public final static String StartRequestName = "Start";
	public final static QName StartRequestQName = new QName(StartRequestName, NameSpace);

	public static String TargetsListTypeName = "TargetsListType";
	public static QName TargetsListTypeQName = new QName(TargetsListTypeName, NameSpace);

	public static String TargetsListElementName = "TargetsList";
	public static QName TargetsListElementQName = new QName(TargetsListElementName, NameSpace);

	public static String SetParametersOperationName = "SetParameters";
	public static QName SetParametersOperationQName = new QName(SetParametersOperationName, NameSpace);

	public static String SetParametersRequestTypeName = "SetParametersType";
	public static QName SetParametersRequestTypeQName = new QName(SetParametersRequestTypeName, NameSpace);

	public static String SetParametersRequestMessageName = "SetParameters";
	public static QName SetParametersRequestMessageQName = new QName(SetParametersRequestMessageName, NameSpace);

	public static String SetParametersResponseMessageName = "ParametersSet";
	public static QName SetParametersResponseMessageQName = new QName(SetParametersResponseMessageName, NameSpace);

	public static String NameElementName = "Name";
	public static QName NameElementQName = new QName(NameElementName, NameSpace);

	public static String ValueElementName = "Value";
	public static QName ValueElementQName = new QName(ValueElementName, NameSpace);

	public static String ParameterElementName = "Parameter";
	public static QName ParameterElementQName = new QName(ParameterElementName, NameSpace);

	public static String ParametersListElementName = "Parameters";
	public static QName ParametersListElementQName = new QName(ParametersListElementName, NameSpace);

	public static String ParametersListTypeName = "ParametersType";
	public static QName ParametersListTypeQName = new QName(ParametersListTypeName, NameSpace);

	public static String ParameterTypeName = "ParameterType";
	public static QName ParameterTypeQName = new QName(ParameterTypeName, NameSpace);



}
