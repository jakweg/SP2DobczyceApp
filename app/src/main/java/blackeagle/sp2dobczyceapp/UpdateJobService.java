package blackeagle.sp2dobczyceapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class UpdateJobService extends JobService {
    static final int UPDATE_REQUEST = 1;
    JobParameters mParams;
    Thread mThread = new Thread(new Runnable() {
        @Override
        public void run() {
            boolean success = true;
            try {
                UpdateManager.Result result = UpdateManager.update(UpdateJobService.this);
                success = result.success;
                if (!result.updated || !result.areNewsForUser()) return;

                Notification notification = UpdateManager.createNotification(result, UpdateJobService.this);

                Settings.createNotificationChannels(getApplicationContext());
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (manager != null)
                    manager.notify(Settings.NOTIFICATION_ID_UPDATE_RESULT, notification);
            } catch (Exception e) {
                e.printStackTrace();
                success = false;
            } finally {
                jobFinished(mParams, !success);
                if (success)
                    setUpUpdating(UpdateJobService.this);
            }
        }
    });

    static void setUpUpdating(Context context) {
        if (!Settings.isReady) return;
        if (!Settings.canWorkInBackground) return;

        JobScheduler scheduler = ((JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE));
        if (scheduler == null) return;
        boolean isAlready = false;
        for (JobInfo ji : scheduler.getAllPendingJobs())
            if (ji.getId() == UPDATE_REQUEST) {
                isAlready = true;
                break;
            }

        if (isAlready) return;
        scheduler.cancel(UPDATE_REQUEST);
        scheduler.schedule(new JobInfo.Builder(UPDATE_REQUEST,
                new ComponentName(context, UpdateJobService.class))
                //.setRequiresBatteryNotLow(true)
                .setMinimumLatency(Settings.REFRESH_TIME_IN_MILLIS)
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build());
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        mParams = params;
        Settings.loadSettings(this);
        mThread.start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        try {
            mThread.interrupt();
            mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }
}
