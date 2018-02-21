package blackeagle.sp2dobczyceapp;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootCompleteReceiver extends BroadcastReceiver {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                UpdateService.startService(context);
                LessonFinishService.startService(context);
            } else {
                UpdateService.JobScheduler.startService(context, intent);
                context.startForegroundService(new Intent(context, LessonFinishService.class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
