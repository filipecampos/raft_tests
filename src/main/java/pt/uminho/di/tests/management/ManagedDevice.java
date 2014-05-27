package pt.uminho.di.tests.management;

public interface ManagedDevice {

    public String getStats();

    public void writeStats(String filename);

    public String getEndpoint();

    public void startDevice();

    public void stopDevice();

    public void setKnownDevices(String[] targets);

    public void setParameter(String paramName, String paramValue);

}
