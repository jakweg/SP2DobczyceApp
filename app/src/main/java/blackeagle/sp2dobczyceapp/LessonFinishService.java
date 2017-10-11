package blackeagle.sp2dobczyceapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import java.util.Calendar;

public class LessonFinishService extends Service {
    private static LessonFinishService thisService = null;
    Thread workingThread;
    Notification notification;
    Notification.Builder builder;
    int[] lessonCounts;
    private LessonTimeManager.LessonState lessonState;
    private int timeToFinishLesson = 0;

    static void startService(Context context) {
        Settings.loadSettings(context);
        if (!Settings.isReady)
            return;
        if (thisService != null)
            return;
        if (!Settings.showFinishTimeNotification)
            return;
        context.startService(new Intent(context, LessonFinishService.class));
    }

    static void stopService() {
        if (thisService == null)
            return;
        thisService.stopSelf();
        thisService = null;
    }

    private static int getCurrentMinute() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    private static boolean isWeekend() {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY;
    }

    /**
     * @return true if "jest 5 zastepstw", false if "sa 3 zastepstwa"
     */
    private static boolean shouldUseSingleNoun(int number) {
        return !((number > 20 && number % 10 > 1 && number % 10 < 5)
                || (number < 10 && number % 10 < 5));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Settings.loadSettings(this);
        thisService = this;

        runNotification();

        return START_STICKY;
    }

    private void sleepToLesson() throws InterruptedException {
        try {
            int currentMinute = getCurrentMinute();
            if (isWeekend()) {
                Thread.sleep((24 * 60 - currentMinute) * 60 * 1000);
                return;
            }

            LessonTimeManager.LessonState currentLesson = LessonTimeManager.getCurrentLesson(lessonCounts);

            switch (currentLesson.thisState) {
                case LessonTimeManager.LESSON:
                case LessonTimeManager.BREAK:
                case LessonTimeManager.ABOUT_TO_START_LESSON:
                    break;

                case LessonTimeManager.AFTER_LESSON:
                    //Thread.sleep((24 * 60 - currentMinute) * 60 * 1000);
                    Thread.sleep(((24 * 60 - currentMinute) + 7 * 60 + 15 + 1) * 60 * 1000);
                    break;
                case LessonTimeManager.BEFORE_LESSON:
                    Thread.sleep(((7 * 60 + 15) - currentMinute + 1) * 60 * 1000);
                    break;
            }
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Exception e) {
            //empty
        }
    }

    private void runNotification() {
        workingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    lessonCounts = LessonPlanManager.getLessonsCount(LessonFinishService.this);
                    while (Settings.showFinishTimeNotification) {
                        sleepToLesson();
                        runSchoolLooper();
                    }
                    stopService();
                } catch (InterruptedException e) {
                    //empty
                    stopService();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        workingThread.start();
    }

    private void runSchoolLooper() throws InterruptedException {
        lessonState = LessonTimeManager.getCurrentLesson(lessonCounts);
        while ((lessonState.isInSchool() ||
                lessonState.thisState == LessonTimeManager.ABOUT_TO_START_LESSON)
                && Settings.showFinishTimeNotification) {

            timeToFinishLesson = lessonState.getSecondsToEnd();
            updateNotification();

            if (timeToFinishLesson > 180)
                Thread.sleep(60 * 1000);
            else
                Thread.sleep(1000);

            lessonState = LessonTimeManager.getCurrentLesson(lessonCounts);
        }
        cancelNotification();
    }

    private void cancelNotification() {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .cancel(Settings.NOTIFICATION_ID_LESSON_FINISH);
    }

    private void updateNotification() {
        if (notification == null) {
            builder = new Notification.Builder(this);
            builder.setSmallIcon(R.drawable.ic_school);
            builder.setPriority(Notification.PRIORITY_HIGH);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setColor(Settings.getColor(this, R.color.colorPrimary));
                builder.setCategory(Notification.CATEGORY_ALARM);
                builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            }
        }

        switch (lessonState.thisState) {
            case LessonTimeManager.LESSON:
                builder.setContentTitle("Aktualnie trwa " + String.valueOf(lessonState.lessonNumber + 1) + " lekcja");
                builder.setContentText(getLeftTimeString());
                break;
            case LessonTimeManager.BREAK:
                builder.setContentTitle("Aktualnie trwa " + String.valueOf(lessonState.lessonNumber + 1) + " przerwa");
                builder.setContentText(getLeftTimeString());
                break;
            case LessonTimeManager.ABOUT_TO_START_LESSON:
                builder.setContentTitle("Wkrótce rozpoczną się lekcje");
                builder.setContentText(getLeftTimeString());
                break;
        }

        notification = builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify(Settings.NOTIFICATION_ID_LESSON_FINISH, notification);
    }

    private String getLeftTimeString() {
        int minute = timeToFinishLesson / 60;
        int second = timeToFinishLesson % 60;

        if (minute == 1)
            return "Pozostała jedna minuta";
        if (minute > 1)
            return shouldUseSingleNoun(minute) ?
                    "Pozostało " + minute + " minut" :
                    "Pozostały " + minute + " minuty";
        else
            return shouldUseSingleNoun(second) ?
                    "Pozostało " + second + " sekund" :
                    "Pozostały " + second + " sekundy";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (workingThread != null)
            workingThread.interrupt();
        cancelNotification();
        thisService = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
