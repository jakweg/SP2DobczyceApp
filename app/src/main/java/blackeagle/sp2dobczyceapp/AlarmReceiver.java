package blackeagle.sp2dobczyceapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

    final static int SERVICE_ID_FINISH_TIME = 1;
    final static int SERVICE_ID_UPDATE = 2;

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getIntExtra("sId", 0)) {
            case SERVICE_ID_FINISH_TIME:
                LessonFinishService.startService(context);
                break;
            case SERVICE_ID_UPDATE:
                UpdateService.startService(context);
                break;
        }
    }
}
