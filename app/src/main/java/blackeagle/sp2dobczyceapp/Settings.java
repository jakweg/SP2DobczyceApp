package blackeagle.sp2dobczyceapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;

import java.util.Calendar;

@SuppressWarnings("WeakerAccess")
abstract class Settings {
    interface OnSettingsChangeListener {
        void onClassChange();
        void onThemeChange();
    }

    static boolean isReady = false;

    static boolean isTeacher;
    static String className = "";
    static boolean canNotify;
    static boolean canWorkInBackground;
    static boolean showFinishTimeNotification;
    static int finishTimeDelay;
    static int usersNumber = -1;
    static int luckyNumber1 = -1;
    static int luckyNumber2 = -1;
    static boolean isUserLuckyNumber() {
        return isNumberSelected() &&
                (Settings.luckyNumber1 == Settings.usersNumber || Settings.luckyNumber2 == Settings.usersNumber); }
    static boolean isNumberSelected() { return usersNumber != -1; }
    static boolean isClassSelected(){
        return !"".equals(className);
    }

    static byte lessonPlanRule = (byte)0x0FFFFF;

    static int darkModeState;
    static final int DARK_MODE_ALWAYS = 2;
    static final int DARK_MODE_AUTO = 1;
    static final int DARK_MODE_NEVER = 0;

    static final int NOTIFICATION_ID_UPDATE_RESULT = 1;
    static final int NOTIFICATION_ID_LESSON_FINISH = 2;

    static boolean applyNowDarkTheme() {
        switch (darkModeState){
            case DARK_MODE_ALWAYS:
                return true;
            case DARK_MODE_NEVER:
                return false;
            case DARK_MODE_AUTO:
                int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                return hour > 19 || hour < 7;
            default:
                return false;
        }
    }

    static String getTeacherName() {
        int pos = className.indexOf('(') - 1;
        return pos < 0 ? className : className.substring(className.indexOf('.') + 1, pos);
    }

    static String getClassOrTeacherName(){
        return className;
    }

    private static boolean isLoaded = false;
    static void loadSettings(Context context) {
        loadSettings(context, false);
    }
    static void loadSettings(Context context, boolean force) {
        if (!force && isLoaded)
            return;
        isLoaded = true;

        try {
            SharedPreferences preferences =
                    context.getSharedPreferences("settings", Context.MODE_PRIVATE);

            isReady = preferences.getBoolean("isReady", false);
            isTeacher = preferences.getBoolean("isTeacher", false);
            className = preferences.getString("className", "");
            canNotify = preferences.getBoolean("canNotify", true);
            darkModeState = Integer.valueOf(preferences.getString("darkTheme", String.valueOf(DARK_MODE_NEVER)));
            lessonPlanRule = (byte) preferences.getInt("lessonPlanRule", 0x0FFFFF);
            canWorkInBackground = preferences.getBoolean("canWorkInBackground", true);
            showFinishTimeNotification = preferences.getBoolean("showFinishTimeNotification", false);
            finishTimeDelay = Integer.valueOf(preferences.getString("finishTimeDelay", "0"));
            usersNumber = Integer.valueOf(preferences.getString("usersNumber", "0"));
            luckyNumber1 = preferences.getInt("luckyNumber1", 0);
            luckyNumber2 = preferences.getInt("luckyNumber2", 0);

            LessonPlanManager.loadClassesData(context);
        } catch (Exception e) {
            //empty
        }
        UpdateService.startService(context);
        LessonFinishService.startService(context);
    }

    static void saveSettings(Context context){
        try {
            SharedPreferences preferences =
                    context.getSharedPreferences("settings", Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isReady", isReady);
            editor.putBoolean("isTeacher", isTeacher);
            editor.putString("className",className);
            editor.putString("darkTheme",  String.valueOf(darkModeState));//string!!!
            editor.putInt("lessonPlanRule", lessonPlanRule);
            editor.putBoolean("canNotify", canNotify);
            editor.putBoolean("canWorkInBackground", canWorkInBackground);
            editor.putBoolean("showFinishTimeNotification", showFinishTimeNotification);
            editor.putString("finishTimeDelay",String.valueOf(finishTimeDelay));
            editor.putString("usersNumber", String.valueOf(usersNumber));
            editor.putInt("luckyNumber1", luckyNumber1);
            editor.putInt("luckyNumber2", luckyNumber2);

            editor.apply();

            LessonPlanManager.saveClassesData(context);

        } catch (Exception e){
            //empty
        }
    }

    static boolean isOnline(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            return cm.getActiveNetworkInfo().isConnectedOrConnecting();
        } catch (Exception e) {
            return false;
        }
    }

    static boolean containsDigit(String str){
        for (char c : str.toCharArray()) {
            if (Character.isDigit(c))
                return true;
        }
        return false;
    }

    @ColorInt
    static int getColor(Context context, @ColorRes int res){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getColor(res);
        } else {
            //noinspection deprecation
            return context.getResources().getColor(res);
        }
    }

    public static void createShortcut(Context applicationContext, String title, @Nullable String className,
                                      boolean isTeacher) {
        try {
            Intent shortcutIntent = new Intent(applicationContext, LessonPlanActivity.class);
            if (className != null) {
                shortcutIntent.putExtra("name", className);
                shortcutIntent.putExtra("isTeacher", isTeacher);
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
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
