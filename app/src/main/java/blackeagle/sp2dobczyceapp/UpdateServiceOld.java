package blackeagle.sp2dobczyceapp;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;

@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated
public class UpdateServiceOld extends Service {

    private final static String ACTION_STOP_SERVICE = "UpdateService.stop";
    private static boolean isStarting = false;
    LocalBroadcastManager localBroadcastManager = null;
    AlarmManager alarmManager = null;
    BroadcastReceiver networkListener;
    BroadcastReceiver batteryListener;
    BroadcastReceiver stopListener;
    boolean isStarted = false;

    static void startService(Context context) {
        if (isStarting)
            return;
        try {
            isStarting = true;
            Settings.loadSettings(context);
            if (!Settings.isReady)
                return;
            if (!Settings.canWorkInBackground)
                return;
            //noinspection deprecation
            context.getApplicationContext().startService(new Intent(context, UpdateServiceOld.class));
        } finally {
            isStarting = false;
        }
    }

    static void stopService(Context context) {
        LocalBroadcastManager mgr = LocalBroadcastManager.getInstance(context.getApplicationContext());
        mgr.sendBroadcastSync(new Intent(ACTION_STOP_SERVICE));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isStarted)
            return START_STICKY;
        isStarted = true;
        Settings.loadSettings(this);

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Settings.createNotificationChannels(getApplicationContext());

        if (!Settings.canWorkInBackground) {
            stopSelf();
            return START_NOT_STICKY;
        }

        stopListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopSelf();
            }
        };
        localBroadcastManager.registerReceiver(stopListener, new IntentFilter(ACTION_STOP_SERVICE));

        if (Settings.updateDate + Settings.REFRESH_TIME_IN_MILLIS < System.currentTimeMillis())
            makeUpdate();
        else
            restartWhenNeeded();

        return START_STICKY;
    }

    private void startWaitingForNetwork() {
        restartAt(System.currentTimeMillis() + Settings.REFRESH_TIME_IN_MILLIS);//tak dla bezpieczeństwa
        networkListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                unregisterReceiver(this);
                networkListener = null;
                makeUpdate();
            }
        };

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkListener, filter);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private void startWaitingForBattery() {
        restartAt(System.currentTimeMillis() + Settings.REFRESH_TIME_IN_MILLIS);//tak dla bezpieczeństwa
        batteryListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                unregisterReceiver(this);
                batteryListener = null;
                makeUpdate();
            }
        };

        IntentFilter filter = new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        registerReceiver(batteryListener, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (networkListener != null)
                unregisterReceiver(networkListener);
            networkListener = null;
            if (batteryListener != null)
                unregisterReceiver(batteryListener);
            batteryListener = null;
            if (stopListener != null)
                localBroadcastManager.unregisterReceiver(stopListener);
            stopListener = null;
        } catch (Exception e) {
            //empty
        }
    }

    private void restartWhenNeeded() {

        if (alarmManager != null) {
            long triggerAtMillis = Settings.updateDate + Settings.REFRESH_TIME_IN_MILLIS;
            long now = System.currentTimeMillis();
            if (now > triggerAtMillis)
                restartAt(now + Settings.REFRESH_TIME_IN_MILLIS);
            else
                restartAt(triggerAtMillis);

        }
        stopSelf();
    }

    private void restartAt(long millis) {
        //noinspection deprecation
        Intent intent = new Intent(this, UpdateServiceOld.class);
        alarmManager.set(AlarmManager.RTC_WAKEUP, millis,
                PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private void makeUpdate() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10 * 1000);

                    //noinspection deprecation
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Settings.isPowerSaveMode(UpdateServiceOld.this)) {
                        startWaitingForBattery();
                        return;
                    }
                    //noinspection deprecation
                    if (!Settings.isOnline(UpdateServiceOld.this) || !updateData()) {
                        startWaitingForNetwork();
                        return;
                    }

                    restartWhenNeeded();
                    return;
                } catch (Exception e) {
                    //empty
                }
                stopSelf();
            }
        }).start();
    }

    private boolean updateData() {
        try {
            UpdateManager.Result result = UpdateManager.update(this);
            if (!result.success)
                return false;
            if (!result.updated || !result.areNewsForUser())
                return true;

            Notification notification = UpdateManager.createNotification(result, this);

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null)
                manager.notify(Settings.NOTIFICATION_ID_UPDATE_RESULT, notification);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
