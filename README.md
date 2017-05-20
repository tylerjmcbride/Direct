# Direct
Direct is a library that provides a simplified interface to wrap around the Wi-Fi Peer-to-Peer API. In essence, this interface acts as a facade around the Wi-Fi Peer-to-Peer API by hiding its implementation details. This library focuses on [service discovery](https://developer.android.com/training/connect-devices-wirelessly/nsd-wifi-direct.html).

## Initial Setup
The following must be added to the Android Manifest XML. As this library deals exclusively with service discovery, an API level of 16 is required. 
```xml
<uses-sdk android:minSdkVersion="16" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
```

## Instantiation
This library provides two useful classes which are used to interacte with the Wi-Fi P2P framework.

### Instantiating a Host
The instance is very important and must persist throughout the lifecycle of your application so long as you are maintaining connections with clients.
```java
WifiDirectHost host = new WifiDirectHost(getApplication(), "UNIQUE_SERVICE_TAG", "UNIQUE_INSTANCE_TAG");
```

### Instantiating a Client
The instance is very important and must persist throughout the lifecycle of your application so long as you are maintaining connection with a host.
```java
WifiDirectHost host = new WifiDirectClient(getApplication(), "UNIQUE_SERVICE_TAG");
```

## Service Discovery

### Hosting a Service
#### Starting a Service
```java
host.startService(new ObjectCallback() {
    @Override
    public void onReceived(Object object) {
        // Invoked when an object has been received from a client
    }
}, new ClientCallback() {
    @Override
    public void onConnected(WifiP2pDevice clientDevice) {
        // Invoked when a Client has connected
    }

    @Override
    public void onDisconnected(WifiP2pDevice clientDevice) {
        // Invoked when a Client has disconnected
    }
}, new ServiceCallback() {
    @Override
    public void onServiceStopped() {
        // Invoked when the service is no longer available
    }
}, new ResultCallback() {
    @Override
    public void onSuccess() {
        // Invoked when the request to the Wi-Fi P2P Framework to start a service was successful
    }

    @Override
    public void onFailure() {
        // Invoked when the request to the Wi-Fi P2P Framework to start a service was unsuccessful
    }
});
```
#### Stopping a Service
The result of this function call does not guarantee that the service has stopped, it is simply a request to the Wi-Fi Peer-to-Peer Framework; however, it is possible to capture this event through the ServiceCallback passed as an argument when starting a service.
```java
host.stopService(new ResultCallback() {
    @Override
    public void onSuccess() {
        // Invoked when the request to the Wi-Fi P2P Framework to stop the service was successful
    }

    @Override
    public void onFailure() {
        // Invoked when the request to the Wi-Fi P2P Framework to stop the service was unsuccessful
    }
});
```
### Connecting to a Service
#### Service Discovery
Prior to connecting to a service, the client must discover said service.
```java
client.startDiscovery(new DiscoveryCallback() {
    @Override
    public void onDiscovered(WifiP2pDevice hostDevice) {
        // A new service has been discovered
    }

    @Override
    public void onLost(WifiP2pDevice hostDevice) {
        // Previously discovered service is no longer available
    }
}, new ResultCallback() {
    @Override
    public void onSuccess() {
        // Invoked when the request to the Wi-Fi P2P Framework to start the service discovery was successful
    }

    @Override
    public void onFailure() {
        // Invoked when the request to the Wi-Fi P2P Framework to start the service discovery was unsuccessful
    }
});
```
#### Making the Connection
After the client has discovered a service that they wish to make a connection with, that client may now connect to that service with the respective host WifiP2pDevice.
```java
```

