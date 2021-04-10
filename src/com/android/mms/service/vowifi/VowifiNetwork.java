package com.android.mms.service.vowifi;

import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.os.Parcel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.net.URL;
import java.net.URLConnection;
import javax.net.SocketFactory;

import com.android.mms.service.vowifi.Constants.APNType;
import com.android.okhttp.internalandroidapi.Dns;
import com.android.okhttp.internalandroidapi.HttpURLConnectionFactory;

/**
 * Describes the status of a network interface.
 * <p>Use {@link ConnectivityManager#getVowifiNetwork()} to get an instance that represents
 * the current network connection.
 */
public class VowifiNetwork implements Parcelable {
    private static final String TAG = "VowifiNetwork";

    // Default connection pool values. These are evaluated at startup, just
    // like the OkHttp code. Also like the OkHttp code, we will throw parse
    // exceptions at class loading time if the properties are set but are not
    // valid integers.
    private static final boolean httpKeepAlive =
            Boolean.parseBoolean(System.getProperty("http.keepAlive", "true"));
    private static final int httpMaxConnections =
            httpKeepAlive ? Integer.parseInt(System.getProperty("http.maxConnections", "5")) : 0;
    private static final long httpKeepAliveDurationMs =
            Long.parseLong(System.getProperty("http.keepAliveDuration", "300000"));  // 5 minutes.

    private int mWifiNetworkId;
    private int mSessionId;
    private int mSubId;
    private int mApnType;
    private State mState;
    private String mLocalIP4Addr;
    private String mLocalIP6Addr;
    private String mPcscfIP4Addr;
    private String mPcscfIP6Addr;
    private String mDnsIP4Addr;
    private String mDnsIP6Addr;
    private boolean mCurUsedIPv4 = true;

    private VowifiSocketFactory mVowifiSocketFactory = null;
    private HttpURLConnectionFactory mConnectionFactory = null;

    public enum State {
        CONNECTING, CONNECTED, DISCONNECTED, UNKNOWN
    }

    public VowifiNetwork(int netId, int sessionId, int subId, State state) {
        mWifiNetworkId = netId;
        mSessionId = sessionId;
        mSubId = subId;
        mApnType = APNType.APN_TYPE_MMS;
        mState = state;
    }

    public VowifiNetwork(int netId, int sessionId, int subId, int apnType) {
        mWifiNetworkId = netId;
        mSessionId = sessionId;
        mSubId = subId;
        mApnType = apnType;
        mState = State.UNKNOWN;
    }

    public VowifiNetwork(int wifiNetId, int sessionId, String localIpv4, String localIpv6,
            String pcscfIpv4, String pcscfIpv6, String dnsIPv4, String dnsIpv6, boolean ipv4Pref,
            boolean isSos) {
        mWifiNetworkId = wifiNetId;
        mSessionId = sessionId;
        mLocalIP4Addr = localIpv4;
        mLocalIP6Addr = localIpv6;
        mPcscfIP4Addr = pcscfIpv4;
        mPcscfIP6Addr = pcscfIpv6;
        mDnsIP4Addr = dnsIPv4;
        mDnsIP6Addr = dnsIpv6;
        mCurUsedIPv4 = ipv4Pref;
        mState = State.UNKNOWN;
    }

    public VowifiNetwork(VowifiNetwork source) {
        if (source != null) {
            synchronized (source) {
                mWifiNetworkId = source.mWifiNetworkId;
                mSessionId = source.mSessionId;
                mSubId = source.mSubId;
                mApnType = source.mApnType;
                mState = source.mState;
                mLocalIP4Addr = source.mLocalIP4Addr;
                mLocalIP6Addr = source.mLocalIP6Addr;
                mPcscfIP4Addr = source.mPcscfIP4Addr;
                mPcscfIP6Addr = source.mPcscfIP6Addr;
                mDnsIP4Addr = source.mDnsIP4Addr;
                mDnsIP6Addr = source.mDnsIP6Addr;
                mCurUsedIPv4 = source.mCurUsedIPv4;
            }
        }
    }

    public void setState(State state) {
        synchronized (this) {
            mState = state;
        }
    }

    public State getState() {
        synchronized (this) {
            return mState;
        }
    }

    public int getApnType() {
        synchronized (this) {
            return mApnType;
        }
    }

    public int getSubId() {
        synchronized (this) {
            return mSubId;
        }
    }

    public void setIpv4Addr(String ipv4Addr) {
        synchronized (this) {
            mLocalIP4Addr = ipv4Addr;
        }
    }

    public String getIpv4Addr() {
        synchronized (this) {
            return mLocalIP4Addr;
        }
    }

    public void setIpv6Addr(String ipv6Addr) {
        synchronized (this) {
            mLocalIP6Addr = ipv6Addr;
        }
    }

    public String getIpv6Addr() {
        synchronized (this) {
            return mLocalIP6Addr;
        }
    }


    public void setIpv4PcscfAddr(String ipv4Addr) {
        synchronized (this) {
            mPcscfIP4Addr = ipv4Addr;
        }
    }

    public String getIpv4PcscfAddr() {
        synchronized (this) {
            return mPcscfIP4Addr;
        }
    }

    public void setIpv6PcscfAddr(String ipv6Addr) {
        synchronized (this) {
            mPcscfIP6Addr = ipv6Addr;
        }
    }

    public String getIpv6PcscfAddr() {
        synchronized (this) {
            return mPcscfIP6Addr;
        }
    }

    public void setIPv4DnsAddr(String ipv4Addr) {
        synchronized (this) {
            mDnsIP4Addr = ipv4Addr;
        }
    }

    public String getIPv4DnsAddr() {
        synchronized (this) {
            return mDnsIP4Addr;
        }
    }

    public void setIPv6DnsAddr(String ipv6Addr) {
        synchronized (this) {
            mDnsIP6Addr = ipv6Addr;
        }
    }

    public String getIPv6DnsAddr() {
        synchronized (this) {
            return mDnsIP6Addr;
        }
    }

    public void setCurUsedIPv4(boolean isIPv4) {
        synchronized (this) {
            mCurUsedIPv4 = isIPv4;
        }
    }

    public void setWifiNetId(int wifiNetId) {
        synchronized (this) {
            mWifiNetworkId = wifiNetId;
        }
    }

    public int getWifiNetId() {
        synchronized (this) {
            return mWifiNetworkId;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mWifiNetworkId);
        dest.writeInt(mSessionId);
        dest.writeInt(mSubId);
        dest.writeInt(mApnType);
        dest.writeString(mState.name());
        dest.writeString(mLocalIP4Addr);
        dest.writeString(mLocalIP6Addr);
        dest.writeString(mPcscfIP4Addr);
        dest.writeString(mPcscfIP6Addr);
        dest.writeString(mDnsIP4Addr);
        dest.writeString(mDnsIP6Addr);
        dest.writeInt(mCurUsedIPv4 ? 1 : 0);
    }

    public static final Creator<VowifiNetwork> CREATOR = new Creator<VowifiNetwork>() {
        @Override
        public VowifiNetwork createFromParcel(Parcel in) {
            int netId = in.readInt();
            int sessionId = in.readInt();
            int subId = in.readInt();
            int apnType = in.readInt();
            VowifiNetwork netInfo = new VowifiNetwork(netId, sessionId, subId, apnType);
            netInfo.mState = State.valueOf(in.readString());
            netInfo.mLocalIP4Addr = in.readString();
            netInfo.mLocalIP6Addr = in.readString();
            netInfo.mPcscfIP4Addr = in.readString();
            netInfo.mPcscfIP6Addr = in.readString();
            netInfo.mDnsIP4Addr = in.readString();
            netInfo.mDnsIP6Addr = in.readString();
            netInfo.mCurUsedIPv4 = in.readInt() != 0;
            return netInfo;
        }

        @Override
        public VowifiNetwork[] newArray(int size) {
            return new VowifiNetwork[size];

        }

    };


    /**
     * Opens the specified {@link URL} on this {@code VowifiNetwork}, such that all traffic will
     * be sent on this Network. The URL protocol must be {@code HTTP} or {@code HTTPS}.
     *
     * @return a {@code URLConnection} to the resource referred to by this URL.
     * @throws MalformedURLException if the URL protocol is not HTTP or HTTPS.
     * @throws IOException if an error occurs while opening the connection.
     * @see java.net.URL#openConnection()
     */
    public URLConnection openConnection(URL url) throws IOException {
        return openConnection(url, java.net.Proxy.NO_PROXY);
    }

    /**
     * Opens the specified {@link URL} on this {@code VowifiNetwork}, such that all traffic will be
     * sent on this Network. The URL protocol must be {@code HTTP} or {@code HTTPS}.
     *
     * @param proxy the proxy through which the connection will be established.
     * @return a {@code URLConnection} to the resource referred to by this URL.
     * @throws MalformedURLException if the URL protocol is not HTTP or HTTPS.
     * @throws IllegalArgumentException if the argument proxy is null.
     * @throws IOException if an error occurs while opening the connection.
     * @see java.net.URL#openConnection()
     */
    public URLConnection openConnection(URL url, java.net.Proxy proxy) throws IOException {
        if (mConnectionFactory == null) {
            // Set configuration on the HttpURLConnectionFactory that will be good for all
            // connections created by this Network. Configuration that might vary is left
            // until openConnection() and passed as arguments.
            HttpURLConnectionFactory urlConnectionFactory = new HttpURLConnectionFactory();
            urlConnectionFactory.setDns(new Dns() { // Let traffic go via dnsLookup
                @Override
                public List<InetAddress> lookup(String hostname) throws UnknownHostException {
                    Log.d(TAG, "Dns lookup, getAllByName for '" + hostname + "'.");
                    InetAddress[] addrs = VowifiNetwork.this.getAllByName(hostname);
                    if (addrs == null || addrs.length < 1) {
                        throw new UnknownHostException(hostname);
                    }

                    return Arrays.asList(addrs);
                }
            });
            // A private connection pool just for this Network.
            urlConnectionFactory.setNewConnectionPool(httpMaxConnections, httpKeepAliveDurationMs,
                    TimeUnit.MILLISECONDS);

            mConnectionFactory = urlConnectionFactory;
        }

        return mConnectionFactory.openConnection(url, getSocketFactory(), proxy);
    }

    /**
     * Operates the same as {@code InetAddress.getAllByName} except that host
     * resolution is done on this network.
     *
     * @param host the hostname or literal IP string to be resolved.
     * @return the array of addresses associated with the specified host.
     * @throws UnknownHostException if the address lookup fails.
     */
    public InetAddress[] getAllByName(String host) throws UnknownHostException {
        InetAddress[] addrs = InetAddress.getAllByNameOnNet(host, mWifiNetworkId);

        if (addrs == null || addrs.length < 1) {
            Log.e(TAG, "Failed to get all address for the host '" + host + "'.");
            return null;
        }

        int index = 0;
        InetAddress[] result = new InetAddress[addrs.length];
        ArrayList<InetAddress> waitForJoin = new ArrayList<InetAddress>();
        for (int i = 0; i < addrs.length; i++) {
            int addrLength = addrs[i].getAddress().length;
            if ((mCurUsedIPv4 && addrLength == 4)
                    || (!mCurUsedIPv4 && addrLength == 16)) {
                Log.d(TAG, "Add to list, as found the matched addr: " + addrs[i].getHostAddress());
                result[index] = addrs[i];
                index++;
            } else {
                waitForJoin.add(addrs[i]);
            }
        }

        for (InetAddress address : waitForJoin) {
            Log.d(TAG, "Add to list, as secondary addr: " + address.getHostAddress());
            result[index] = address;
            index++;
        }

        return result;
    }

    /**
     * Operates the same as {@code InetAddress.getByName} except that host
     * resolution is done on this network.
     *
     * @param host
     *            the hostName to be resolved to an address or {@code null}.
     * @return the {@code InetAddress} instance representing the host.
     * @throws UnknownHostException
     *             if the address lookup fails.
     */
    public InetAddress getByName(String host) throws UnknownHostException {
        InetAddress[] addrs = InetAddress.getAllByNameOnNet(host, mWifiNetworkId);

        if (addrs == null || addrs.length < 1) {
            Log.e(TAG, "Failed to get address for the host '" + host + "'.");
            return null;
        }

        for (int i = 0; i < addrs.length; i++) {
            int addrLength = addrs[i].getAddress().length;
            if ((mCurUsedIPv4 && addrLength == 4)
                    || (!mCurUsedIPv4 && addrLength == 16)) {
                Log.d(TAG, "Found the matched addr: " + addrs[i].getHostAddress());
                return addrs[i];
            }
        }

        return null;
    }

    /**
     * Returns a {@link SocketFactory} bound to this network.  Any {@link Socket} created by
     * this factory will have its traffic sent over this {@code Network}.  Note that if this
     * {@code Network} ever disconnects, this factory and any {@link Socket} it produced in the
     * past or future will cease to work.
     *
     * @return a {@link SocketFactory} which produces {@link Socket} instances bound to this
     *         {@code Network}.
     */
    public SocketFactory getSocketFactory() {
        if (mVowifiSocketFactory == null) {
            mVowifiSocketFactory = new VowifiSocketFactory();
        }
        return mVowifiSocketFactory;
    }

    /**
     * A {@code SocketFactory} that produces {@code Socket}'s bound to this network.
     */
    private class VowifiSocketFactory extends SocketFactory {
        private static final String TAG = "VowifiSocketFactory";

        String mLocalAddress = null;

        public VowifiSocketFactory() {
            super();

            // prefer to use IPv4 address, for this network is used by MMS/UT
            if (mCurUsedIPv4 && !TextUtils.isEmpty(mLocalIP4Addr)) {
                Log.d(TAG, "set mLocalAddress as " + mLocalIP4Addr);
                mLocalAddress = mLocalIP4Addr;
            } else if (!mCurUsedIPv4 && !TextUtils.isEmpty(mLocalIP6Addr)) {
                Log.d(TAG, "set mLocalAddress as " + mLocalIP6Addr);
                mLocalAddress = mLocalIP6Addr;
            } else {
                Log.e(TAG, "Shouldn't be here, do not set vowifi local address.");
            }
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
                throws IOException {
            InetSocketAddress localAddress = null;
            if (TextUtils.isEmpty(mLocalAddress)) {
                localAddress = new InetSocketAddress(localHost, localPort);
            } else {
                localAddress = new InetSocketAddress(mLocalAddress, localPort);
            }

            return connectToHost(host, port, localAddress);
        }


        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress,
                int localPort) throws IOException {
            Socket socket = new Socket();
            if (TextUtils.isEmpty(mLocalAddress)) {
                socket.bind(new InetSocketAddress(localAddress, localPort));
            } else {
                socket.bind(new InetSocketAddress(mLocalAddress, localPort));
            }
            socket.connect(new InetSocketAddress(address, port));

            return socket;

        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            Socket socket = createSocket();
            socket.connect(new InetSocketAddress(host, port));
            return socket;
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            InetSocketAddress localAddress = null;
            if (!TextUtils.isEmpty(mLocalAddress)) {
                localAddress = new InetSocketAddress(mLocalAddress, 0);
            }
            return connectToHost(host, port, localAddress);
        }

        @Override
        public Socket createSocket() throws IOException {
            Socket socket = new Socket();
            if (!TextUtils.isEmpty(mLocalAddress)) {
                socket.bind(new InetSocketAddress(mLocalAddress, 0));
            }
            return socket;
        }

        private Socket connectToHost(String host, int port, SocketAddress localAddress)
                throws IOException {
            // Lookup addresses only on this Network.
            InetAddress[] hostAddresses = getAllByName(host);
            if (hostAddresses == null || hostAddresses.length < 1) {
                throw new UnknownHostException(host);
            }

            // Try all addresses.
            for (int i = 0; i < hostAddresses.length; i++) {
                try {
                    Socket socket = createSocket();
                    if (localAddress != null) {
                        socket.bind(localAddress);
                    }
                    socket.connect(new InetSocketAddress(hostAddresses[i], port));
                    return socket;
                } catch (IOException e) {
                    if (i == (hostAddresses.length - 1)) throw e;
                }
            }
            throw new UnknownHostException(host);
        }

    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        builder.append("sessionId: " + mSessionId)
                .append(", subId: " + mSubId)
                .append(", apnType: " + mApnType)
                .append(", LocalIP4Addr: " + mLocalIP4Addr)
                .append(", LocalIP6Addr: " + mLocalIP6Addr)
                .append(", PcscfIP4Addr: " + mPcscfIP4Addr)
                .append(", PcscfIP6Addr: " + mPcscfIP6Addr)
                .append(", DnsIP4Addr: " + mDnsIP4Addr)
                .append(", DnsIP6Addr: " + mDnsIP6Addr)
                .append(", CurUsedIPv4: " + mCurUsedIPv4)
                .append(", wifiNetId: " + mWifiNetworkId)
                .append("]");

        return builder.toString();
    }

}
