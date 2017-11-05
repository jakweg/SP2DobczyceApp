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
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;

public class UpdateService extends Service {

    private static UpdateService thisService = null;
    private static boolean isStarting = false;
    BroadcastReceiver networkListener;
    BroadcastReceiver batteryListener;

    static void startService(Context context) {
        if (isStarting)
            return;
        try {
            isStarting = true;
            Settings.loadSettings(context);
            if (!Settings.isReady)
                return;
            if (thisService != null)
                return;
            if (!Settings.canWorkInBackground)
                return;
            context.startService(new Intent(context, UpdateService.class));
        } finally {
            isStarting = false;
        }
    }

    static void stopService() {
        if (thisService == null)
            return;
        thisService.stopSelf();
        thisService = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        thisService = this;
        Settings.loadSettings(this);

        Settings.createNotificationChannels(getApplicationContext());

        if (!Settings.canWorkInBackground) {
            stopService();
            return START_NOT_STICKY;
        }

        if (Settings.updateDate + Settings.REFRESH_TIME_IN_MILLIS < System.currentTimeMillis())
            makeUpdate();
        else
            restartWhenNeeded();

        return START_STICKY;
    }

    private void startWaitingForNetwork() {
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
            thisService = null;
        } catch (Exception e) {
            //empty
        }
    }

    private void restartWhenNeeded() {
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("sId", AlarmReceiver.SERVICE_ID_UPDATE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            long triggerAtMillis = Settings.updateDate + Settings.REFRESH_TIME_IN_MILLIS;
            long now = System.currentTimeMillis();
            if (now > triggerAtMillis)
                triggerAtMillis = now + Settings.REFRESH_TIME_IN_MILLIS;

            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis,
                    PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
        }
        stopService();
    }

    private void makeUpdate() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10 * 1000);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Settings.isPowerSaveMode(UpdateService.this)) {
                        startWaitingForBattery();
                        return;
                    }
                    if (!Settings.isOnline(UpdateService.this) || !updateData()) {
                        startWaitingForNetwork();
                        return;
                    }

                    restartWhenNeeded();
                    return;
                } catch (Exception e) {
                    //empty
                }
                stopService();
            }
        }).start();
    }

    @NonNull
    private Notification createNotification(@NonNull UpdateManager.Result result) {
        boolean useOldNotification = Build.VERSION.SDK_INT < Build.VERSION_CODES.O;
        //noinspection deprecation
        NotificationCompat.Builder builder = useOldNotification ?
                new NotificationCompat.Builder(this) :
                new NotificationCompat.Builder(this, Settings.CHANNEL_ID_NEWS);

        builder.setSmallIcon(R.drawable.ic_school_png);
        builder.setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher_round));
        builder.setContentTitle(getString(R.string.school_news));
        builder.setAutoCancel(true);
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("fromNotification", true);
        builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), PendingIntent.FLAG_UPDATE_CURRENT,
                intent, Intent.FILL_IN_ACTION));

        if (useOldNotification) {
            builder.setPriority(Notification.PRIORITY_HIGH);
            if (Settings.canNotify)
                builder.setDefaults(Notification.DEFAULT_ALL);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setCategory(Notification.CATEGORY_EVENT);
                builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            }
        }

        if (result.newCount > 0) { //zastępstwa
            builder.setContentText(UpdateManager.getNewsUpdateInfo(result.newCount));
            if (result.hasChangedLuckyNumbers && Settings.isUserLuckyNumber())
                builder.setSubText("Twój szczęśliwy numerek \uD83D\uDE0A");
            else
                builder.setSubText(String.format("Szczęśliwe numerki: %s i %s", Settings.luckyNumber1, Settings.luckyNumber2));
        } else if (result.hasChangedLuckyNumbers && Settings.isUserLuckyNumber()) { //brak zastepstw ale numerek
            builder.setContentText("Twój szczęśliwy numerek \uD83D\uDE0A");
            builder.setSubText(String.format("Szczęśliwe numerki: %s i %s", Settings.luckyNumber1, Settings.luckyNumber2));
        }

        return builder.build();
    }

    private boolean updateData() {
        try {
            UpdateManager.Result result = UpdateManager.update(this);
            if (!result.success)
                return false;
            if (!result.updated || !result.areNewsForUser())
                return true;

            Notification notification = createNotification(result);

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
