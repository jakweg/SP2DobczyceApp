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
import android.support.annotation.RequiresApi;

import java.util.Calendar;

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

        makeUpdate();

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
        if (networkListener != null)
            unregisterReceiver(networkListener);
        networkListener = null;
        if (batteryListener != null)
            unregisterReceiver(batteryListener);
        batteryListener = null;
        thisService = null;
    }

    private void restartWhenNeeded() {
        Calendar calendar = Calendar.getInstance();
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("sId", AlarmReceiver.SERVICE_ID_UPDATE);

        ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                .set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + Settings.REFRESH_TIME_IN_MILLIS,
                        PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
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
                } catch (Exception e) {
                    //empty
                }
                stopService();
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

            Notification.Builder builder = new Notification.Builder(this);
            builder.setSmallIcon(R.drawable.ic_school);
            builder.setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher_round));
            builder.setContentTitle(getString(R.string.school_news));
            builder.setAutoCancel(true);
            builder.setPriority(Notification.PRIORITY_HIGH);
            builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), PendingIntent.FLAG_ONE_SHOT,
                    new Intent(thisService, MainActivity.class), Intent.FILL_IN_ACTION));
            if (Settings.canNotify)
                builder.setDefaults(Notification.DEFAULT_ALL);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setCategory(Notification.CATEGORY_EVENT);
                builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            }

            if (result.newCount > 0)
                builder.setContentText(UpdateManager.getNewsUpdateInfo(result.newCount));
            if (result.hasChangedLuckyNumbers && Settings.isUserLuckyNumber())
                builder.setSubText("Twój szczęśliwy numerek \uD83D\uDE0A");
            else if (Settings.luckyNumber1 != 0 && Settings.luckyNumber2 != 0)
                builder.setSubText(String.format("Szczęśliwe numerki: %s i %s", Settings.luckyNumber1, Settings.luckyNumber2));

            Notification notification = builder.build();

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
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
