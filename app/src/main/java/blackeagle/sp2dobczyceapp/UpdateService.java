package blackeagle.sp2dobczyceapp;

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

import java.util.Arrays;

public class UpdateService extends Service {

    private static UpdateService thisService = null;
    Thread looperThread;
    BroadcastReceiver networkListener;

    static void startService(Context context) {
        Settings.loadSettings(context);
        if (!Settings.isReady)
            return;
        if (thisService != null)
            return;
        if (!Settings.canWorkInBackground)
            return;
        context.startService(new Intent(context, UpdateService.class));
    }

    static void stopService() {
        if (thisService == null)
            return;
        thisService.stopSelf();
        thisService = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Settings.loadSettings(this);
        thisService = this;

        runLooperThread();

        return START_STICKY;
    }

    private void startWaitingForNetwork() {
        networkListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                unregisterReceiver(this);
                networkListener = null;
                runLooperThread();
            }
        };

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);

        registerReceiver(networkListener, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (networkListener != null)
            unregisterReceiver(networkListener);
        if (looperThread != null)
            looperThread.interrupt();
        thisService = null;
    }

    private void runLooperThread() {
        looperThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(10000);
                        if (!Settings.isOnline(UpdateService.this) || !updateData()) {
                            startWaitingForNetwork();
                            looperThread = null;
                            return;
                        }

                        Thread.sleep(Settings.REFRESH_TIME);
                    } catch (Exception e) {
                        looperThread = null;
                        return;
                    }
                }
            }
        });
        looperThread.start();
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
                builder.setSubText("Twój szczęśliwy numerek " + Arrays.toString(Character.toChars(0x1F60A)));
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
