package blackeagle.sp2dobczyceapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

@SuppressWarnings("WeakerAccess")
abstract class Settings {
    static final String CHANNEL_ID_NEWS = "blackeagle.sp2dobczyceapp.channels.news";
    static final String CHANNEL_ID_LESSON_TIME = "blackeagle.sp2dobczyceapp.channels.lessontime";

    static final int REFRESH_TIME_IN_MILLIS = 2 * 60 * 60 * 1000;

    static final int DARK_MODE_ALWAYS = 2;
    static final int DARK_MODE_AUTO = 1;
    static final int DARK_MODE_NEVER = 0;
    static final int NOTIFICATION_ID_UPDATE_RESULT = 1;
    static final int NOTIFICATION_ID_LESSON_FINISH = 2;
    static boolean isReady = false;
    static boolean showBadges = false;
    static boolean isTeacher;
    static String className = "";
    static boolean canNotify;
    static boolean canWorkInBackground;
    static boolean showFinishTimeNotification;
    static int finishTimeDelay;
    static int usersNumber = -1;
    static int luckyNumber1 = -1;
    static int luckyNumber2 = -1;
    static long updateDate = 0;
    static byte lessonPlanRule = (byte) LessonPlanManager.LessonPlan.RULE_SHOW_CLASSROOM | LessonPlanManager.LessonPlan.RULE_SHOW_SUBJECT | LessonPlanManager.LessonPlan.RULE_SHOW_TEACHER;
    static int darkModeState;
    private static boolean isLoaded = false;

    static boolean isUserLuckyNumber() {
        return isNumberSelected() &&
                (Settings.luckyNumber1 == Settings.usersNumber || Settings.luckyNumber2 == Settings.usersNumber);
    }

    static boolean isNumberSelected() {
        return usersNumber != -1;
    }

    static boolean isClassSelected() {
        return !"".equals(className);
    }

    static boolean applyNowDarkTheme() {
        switch (darkModeState) {
            case DARK_MODE_ALWAYS:
                return true;
            case DARK_MODE_NEVER:
                return false;
            case DARK_MODE_AUTO:
                Calendar c = Calendar.getInstance();
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int month = c.get(Calendar.MONTH);
                if (month < 4 || month > 9)
                    return hour > 17 || hour < 7;
                else
                    return hour > 19 || hour < 7;
            default:
                return false;
        }
    }

    static String getTeacherName() {
        int pos = className.indexOf('(') - 1;
        return pos < 0 ? className : className.substring(className.indexOf('.') + 1, pos);
    }

    static void loadSettings(Context context) {
        loadSettings(context, false);
    }

    static void loadSettings(Context context, boolean force) {
        if (!force && isLoaded)
            return;
        isLoaded = true;

        try {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
            SharedPreferences preferences =
                    context.getSharedPreferences("settings", Context.MODE_PRIVATE);

            isReady = preferences.getBoolean("isReady", false);
            isTeacher = preferences.getBoolean("isTeacher", false);
            className = preferences.getString("className", "");
            canNotify = preferences.getBoolean("canNotify", true);
            darkModeState = Integer.valueOf(preferences.getString("darkTheme", String.valueOf(DARK_MODE_NEVER)));
            lessonPlanRule = (byte) preferences.getInt("lessonPlanRule", LessonPlanManager.LessonPlan.RULE_SHOW_CLASSROOM | LessonPlanManager.LessonPlan.RULE_SHOW_SUBJECT | LessonPlanManager.LessonPlan.RULE_SHOW_TEACHER);
            canWorkInBackground = preferences.getBoolean("canWorkInBackground", true);
            showFinishTimeNotification = preferences.getBoolean("showFinishTimeNotification", false);
            finishTimeDelay = Integer.valueOf(preferences.getString("finishTimeDelay", "0"));
            usersNumber = Integer.valueOf(preferences.getString("usersNumber", "0"));
            luckyNumber1 = preferences.getInt("luckyNumber1", 0);
            luckyNumber2 = preferences.getInt("luckyNumber2", 0);
            updateDate = preferences.getLong("updateDate", 0);
            showBadges = preferences.getBoolean("showBadges", false);


            if (isReady) {
                int lastVersion = preferences.getInt("lastVersion", 0);
                if (lastVersion == 0)
                    updateToVersion(lastVersion, context);
            }

            LessonPlanManager.loadClassesData(context);
        } catch (Exception e) {
            //empty
        }
        UpdateService.startService(context);
        LessonFinishService.startService(context);
    }

    static void saveSettings(Context context) {
        try {
            SharedPreferences preferences =
                    context.getSharedPreferences("settings", Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isReady", isReady);
            editor.putBoolean("isTeacher", isTeacher);
            editor.putString("className", className);
            editor.putString("darkTheme", String.valueOf(darkModeState));//string!!!
            editor.putInt("lessonPlanRule", lessonPlanRule);
            editor.putBoolean("canNotify", canNotify);
            editor.putBoolean("canWorkInBackground", canWorkInBackground);
            editor.putBoolean("showFinishTimeNotification", showFinishTimeNotification);
            editor.putString("finishTimeDelay", String.valueOf(finishTimeDelay));
            editor.putString("usersNumber", String.valueOf(usersNumber));
            editor.putInt("luckyNumber1", luckyNumber1);
            editor.putInt("luckyNumber2", luckyNumber2);
            editor.putLong("updateDate", updateDate);
            editor.putBoolean("showBadges", showBadges);

            if (isReady)
                editor.putInt("lastVersion", 1);

            editor.apply();

            LessonPlanManager.saveClassesData(context);

        } catch (Exception e) {
            //empty
        }
    }

    static void updateToVersion(int lastVersion, Context context) {
        try {
            switch (lastVersion) {
                case 0:
                    String path = context.getApplicationInfo().dataDir;
                    File dir = new File(path + "/plans");
                    if (!dir.exists() && !dir.mkdir())
                        Log.e("SP2ERR", "CANNOT CREATE UPDATE DIR");

                    dir = new File(path + "/cPlans");
                    for (File file : dir.listFiles()) {
                        if (!file.renameTo(new File(path + "/plans/" + file.getName())))
                            Log.e("SP2ERR", "CANNOT UPDATE");
                    }
                    dir = new File(path + "/tPlans");
                    for (File file : dir.listFiles()) {
                        if (!file.renameTo(new File(path + "/plans/" + file.getName())))
                            Log.e("SP2ERR", "CANNOT UPDATE");
                    }
            }
        } catch (Exception e) {
            //empty
        }
    }

    static boolean isOnline(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            return cm != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
        } catch (Exception e) {
            return false;
        }
    }

    static boolean containsDigit(String str) {
        for (char c : str.toCharArray()) {
            if (Character.isDigit(c))
                return true;
        }
        return false;
    }

    static String getLastUpdateTime() {
        if (updateDate == 0)
            return "";
        int today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        GregorianCalendar c = new GregorianCalendar();
        c.setTimeInMillis(updateDate);
        int lastTime = c.get(Calendar.DAY_OF_YEAR);
        switch (today - lastTime) {
            case 0:
                return "Zaktualizowano dzisiaj";
            case 1:
                return "Zaktualizowano wczoraj";
            case 2:
                return "Zaktualizowano przedwczoraj";
            /*case -1:
                return "Jutro";
            case -2:
                return "Pojutrze";*/
            default:
                return String.format(Locale.getDefault(), "Zaktualizowano %02d.%02d.%s", c.get(Calendar.DAY_OF_MONTH),
                        c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR));
        }
    }

    @ColorInt
    static int getColor(Context context, @ColorRes int res) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getColor(res);
        } else {
            //noinspection deprecation
            return context.getResources().getColor(res);
        }
    }

    public static void createShortcut(Context applicationContext, String title, @Nullable String className) {
        try {
            Intent shortcutIntent = new Intent(applicationContext, LessonPlanActivity.class);
            if (className != null) {
                shortcutIntent.putExtra("name", className);
            }
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(applicationContext, R.mipmap.ic_lesson_plan));
            //addIntent.putExtra("duplicate", false);
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            applicationContext.sendBroadcast(addIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean isPowerSaveMode(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return false;
        PowerManager service = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return service != null && service.isPowerSaveMode();
    }

    static Drawable getDyedDrawable(Context context, @DrawableRes int id, boolean isDarkTheme) {
        try {
            Drawable drawable;
        /*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = ResourcesCompat.getDrawable(context.getResources(), id, null);
        } else {
            drawable = ContextCompat.getDrawable(context, id);
        }
        if (drawable != null)
            drawable.setColorFilter(new PorterDuffColorFilter(
                    Settings.getColor(context, isDarkTheme ? R.color.white : R.color.black),
                    PorterDuff.Mode.SRC_IN));*/
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                drawable = ContextCompat.getDrawable(context, id);
            } else {
                //noinspection deprecation
                drawable = ContextCompat.getDrawable(context, id);
            }
            drawable.setColorFilter(new PorterDuffColorFilter(
                    Settings.getColor(context, isDarkTheme ? R.color.white : R.color.black),
                    PorterDuff.Mode.SRC_IN));
            return drawable;
        } catch (Exception e) {
            return null;
        }
    }

    static void createNotificationChannels(Context context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)
            return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null)
            return;

        NotificationChannel newsChannel = new NotificationChannel(CHANNEL_ID_NEWS, "Kanał zastępstw", NotificationManager.IMPORTANCE_DEFAULT);
        newsChannel.setShowBadge(true);
        newsChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        newsChannel.enableVibration(true);
        newsChannel.enableLights(true);

        NotificationChannel lessonTimeChannel = new NotificationChannel(CHANNEL_ID_LESSON_TIME, "Kanał odliczania", NotificationManager.IMPORTANCE_MIN);
        lessonTimeChannel.setShowBadge(false);
        lessonTimeChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        lessonTimeChannel.enableVibration(false);
        lessonTimeChannel.enableLights(false);

        ArrayList<NotificationChannel> list = new ArrayList<>();
        list.add(newsChannel);
        list.add(lessonTimeChannel);
        manager.createNotificationChannels(list);
    }

    interface OnSettingsChangeListener {
        void onClassChange();

        void onThemeChange();
    }
}
