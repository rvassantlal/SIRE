package sire.membership;

import sire.attestation.Policy;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MembershipManager {
    private final int timeout = 30;
    private final long certTimeout = 30 * 60000;
    private final Map<String, AppContext> membership;

    public MembershipManager() {
        membership = new TreeMap<>();
    }

    public boolean containsApp(String appId) {
        return membership.containsKey(appId);
    }

    public boolean hasDevice(String appId, String deviceId) {
        return membership.get(appId).hasDevice(deviceId);
    }

    public void updateDeviceTimestamp(String appId, String deviceId, Timestamp timestamp) {
        membership.get(appId).updateDeviceTimestamp(deviceId, timestamp);
    }

    public boolean isDeviceValid(String appId, String deviceId) {
        return membership.get(appId).isDeviceValid(deviceId);
    }

    public void removeDevice(String appId, String deviceId) {
        membership.get(appId).removeDevice(deviceId);
    }

    public void setDeviceAsAttested(String appId, String deviceId, byte[] certificate, Timestamp timestamp) {
        membership.get(appId).setDeviceAsAttested(deviceId, certificate, timestamp);
    }

    public List<DeviceContext> getMembership(String appId) {
        return membership.get(appId).getMembership();
    }

/*    public void setPolicy(String appId, String policy, boolean type) {
        if(!membership.containsKey(appId))
            membership.put(appId, new AppContext(appId, this.timeout, this.certTimeout, new Policy(policy, type)));
        else
            membership.get(appId).setPolicy(policy, type);
    }

    public void removePolicy(String appId) {
        membership.get(appId).removePolicy();
    }

    public String getPolicy(String appId) {
        return membership.get(appId).getPolicy().getPolicy();
    }*/

    public void addDevice(String appId, String deviceId, Timestamp timestamp, DeviceContext.DeviceType deviceType) {
        if(!membership.containsKey(appId))
            membership.put(appId, new AppContext(appId, timeout, certTimeout));
        membership.get(appId).addDevice(deviceId, new DeviceContext(deviceId, timestamp, deviceType));
    }
}