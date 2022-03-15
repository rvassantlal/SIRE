package sire.serverProxyUtils;

import sire.configuration.Policy;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.TreeMap;

public class AppContext implements Serializable {
    final String id;
    TreeMap<String, DeviceContext> devices;
    Policy policy;

    public AppContext(String id) {
        this.id = id;
        this.devices = new TreeMap<>();
        this.policy = new Policy();
    }

    public AppContext(String id, Policy policy) {
        this.id = id;
        this.devices = new TreeMap<>();
        this.policy = policy;
    }

    public String getId() {
        return id;
    }

    public DeviceContext getDevice(String deviceId) {
        return this.devices.get(deviceId);
    }

    public List<DeviceContext> getMembership() {
        return devices.values().stream().toList();
    }

    public void addDevice(String deviceId, DeviceContext device){
        if(this.devices.containsKey(deviceId))
            return;
        else
            this.devices.put(deviceId, device);
    }

    public void removeDevice(String deviceId) {
        this.devices.remove(deviceId);
    }

    public void updateDeviceTimestamp(String deviceId, Timestamp timestamp) {
        DeviceContext temp = this.devices.get(deviceId);
        temp.setLastPing(timestamp);
        this.devices.put(deviceId, temp);
    }

    @Override
    public String toString() {
        return "AppContext{" +
                "id='" + id + '\'' +
                ", devices=" + devices +
                ", policy=" + policy +
                '}';
    }

    public void setPolicy(String policy, boolean type) {
        this.policy.setPolicy(policy, type);
    }

    public void removePolicy() {
        this.policy = new Policy();
    }

    public Policy getPolicy() {
        return policy;
    }
}
