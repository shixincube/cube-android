package cube.utils.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import cube.utils.NetworkUtil;
import cube.utils.TimeUtils;
import cube.utils.log.LogUtil;
import java.util.ArrayList;
import java.util.List;

public final class ConnectionChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "ConnectionChangeReceiver";

    private volatile static ConnectionChangeReceiver instance = null;

    private List<ConnectionChangeListener> connectionChangeListeners = new ArrayList<ConnectionChangeListener>();

    private int     MAX_INTERVAL_TIME    = 4;// min server keep 15 min
    private int     currentTime          = 1;
    private boolean isRegister           = false;
    private boolean lastConnectionStatus = false;

    private ConnectionChangeReceiver() {
        connectionChangeListeners.clear();
    }

    public static ConnectionChangeReceiver getInstance() {
        if (instance == null) {
            synchronized (ConnectionChangeReceiver.class) {
                if (instance == null) {
                    LogUtil.i(TAG, "ConnectionChange:new");
                    instance = new ConnectionChangeReceiver();
                }
            }
        }
        return ConnectionChangeReceiver.instance;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_TIME_TICK.equals(intent.getAction())) {
            if (currentTime >= MAX_INTERVAL_TIME) {
                currentTime = 1;
                for (int i = 0; i < connectionChangeListeners.size(); i++) {
                    connectionChangeListeners.get(i).onTimeTick(MAX_INTERVAL_TIME);
                }
            }
            else {
                currentTime++;
            }
        }
        else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            boolean isNetworkAvailable = NetworkUtil.isNetAvailable(context);
            if (lastConnectionStatus != isNetworkAvailable) {
                lastConnectionStatus = isNetworkAvailable;
                if (!connectionChangeListeners.isEmpty()) {
                    for (int i = 0; i < connectionChangeListeners.size(); i++) {
                        connectionChangeListeners.get(i).onConnectionChange(isNetworkAvailable);
                    }
                }
            }
        }
        else if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction()) || Intent.ACTION_DATE_CHANGED.equals(intent.getAction())) {
            LogUtil.i(TAG, "date time changed!");
            TimeUtils.calculateOffsetTime();
        }
    }

    public void register(Context context) {
        if (!isRegister) {
            synchronized (ConnectionChangeReceiver.this) {
                if (!isRegister) {
                    if (context == null) {
                        return;
                    }
                    else {
                        IntentFilter filter = new IntentFilter();
                        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                        filter.addAction(Intent.ACTION_TIME_CHANGED);
                        filter.addAction(Intent.ACTION_DATE_CHANGED);
                        filter.addAction(Intent.ACTION_TIME_TICK);
                        context.registerReceiver(this, filter);
                    }
                }
            }
        }
    }

    public void unregister(Context context) {
        if (isRegister) {
            synchronized (ConnectionChangeReceiver.this) {
                if (isRegister) {
                    if (context != null) {
                        context.unregisterReceiver(this);
                    }
                }
            }
        }
    }

    public void addConnectionChangeListener(ConnectionChangeListener connectionChangeListener) {
        if (connectionChangeListener != null && !connectionChangeListeners.contains(connectionChangeListener)) {
            connectionChangeListeners.add(connectionChangeListener);
        }
    }

    public void removeConnectionChangeListener(ConnectionChangeListener connectionChangeListener) {
        if (connectionChangeListener != null) {
            connectionChangeListeners.remove(connectionChangeListener);
        }
    }
}
