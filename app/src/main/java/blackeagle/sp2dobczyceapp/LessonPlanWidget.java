package blackeagle.sp2dobczyceapp;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import java.util.Calendar;

public class LessonPlanWidget extends AppWidgetProvider {


    static int[] widgetIds;

    @SuppressLint("SwitchIntDef")
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        try {
            Settings.loadSettings(context);
            RemoteViews views;
            if (!Settings.isReady) {
                views = new RemoteViews(context.getPackageName(), R.layout.widget_not_ready);
                views.setOnClickPendingIntent(R.id.error_button,
                        PendingIntent.getActivity(context, 0,
                                new Intent(context, WelcomeActivity.class), Intent.FILL_IN_ACTION));
            } else if (!Settings.isClassSelected()) {
                views = new RemoteViews(context.getPackageName(), R.layout.widget_not_ready);
                views.setOnClickPendingIntent(R.id.error_button,
                        PendingIntent.getActivity(context, 0,
                                new Intent(context, SettingsActivity.class), Intent.FILL_IN_ACTION));
                views.setTextViewText(R.id.title, "Nie wybrano klasy");
            } else {
                boolean isDarkTheme = Settings.applyNowDarkTheme();
                views = new RemoteViews(context.getPackageName(), isDarkTheme ? R.layout.widget_dark_layout : R.layout.widget_layout);
                views.removeAllViews(R.id.main_layout);
                views.removeAllViews(R.id.lesson_time_layout);
                views.setOnClickPendingIntent(R.id.title,
                        PendingIntent.getActivity(context, 0,
                                new Intent(context, LessonPlanActivity.class), Intent.FILL_IN_ACTION));

                LessonTimeManager.makeLayout(context, views, isDarkTheme);

                LessonPlanManager.LessonPlan plan = LessonPlanManager.getDefaultPlan(context);
                if (plan == null)
                    return;
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                switch (calendar.get(Calendar.DAY_OF_WEEK)) {
                    case Calendar.MONDAY:
                        makeLessonLayout(context, hour < 16 ? 0 : 1, plan, views, isDarkTheme);
                        break;
                    case Calendar.TUESDAY:
                        makeLessonLayout(context, hour < 16 ? 1 : 2, plan, views, isDarkTheme);
                        break;
                    case Calendar.WEDNESDAY:
                        makeLessonLayout(context, hour < 16 ? 2 : 3, plan, views, isDarkTheme);
                        break;
                    case Calendar.THURSDAY:
                        makeLessonLayout(context, hour < 16 ? 3 : 4, plan, views, isDarkTheme);
                        break;
                    case Calendar.FRIDAY:
                        makeLessonLayout(context, 4, plan, views, isDarkTheme);
                        break;
                    default:
                        makeLessonLayout(context, 0, plan, views, isDarkTheme);
                        break;
                }
            }


            appWidgetManager.updateAppWidget(appWidgetId, views);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void makeLessonLayout(Context context, int day, @NonNull LessonPlanManager.LessonPlan plan, RemoteViews parent,
                                         boolean isDarkTheme) {
        switch (day) {
            case 0:
                parent.setTextViewText(R.id.day_name, "Poniedziałek");
                break;
            case 1:
                parent.setTextViewText(R.id.day_name, "Wtorek");
                break;
            case 2:
                parent.setTextViewText(R.id.day_name, "Środa");
                break;
            case 3:
                parent.setTextViewText(R.id.day_name, "Czwartek");
                break;
            case 4:
                parent.setTextViewText(R.id.day_name, "Piątek");
                break;
        }

        for (int i = 0; i < 8; i++) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.one_widget_lesson);
            views.setTextViewText(R.id.lesson_name, plan.getDisplayedLesson(day, i,
                    (Settings.isTeacher ? LessonPlanManager.LessonPlan.RULE_SHOW_TEACHER :
                            LessonPlanManager.LessonPlan.RULE_SHOW_SUBJECT) |
                            LessonPlanManager.LessonPlan.RULE_SHOW_CLASSROOM));
            if (isDarkTheme)
                views.setTextColor(R.id.lesson_name, 0xffc1c1c1);
            parent.addView(R.id.main_layout, views);
        }
    }

    public static void refreshWidgets(Context context) {
        if (widgetIds == null)
            return;
        Intent intent = new Intent(context, LessonPlanWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = widgetIds;
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        widgetIds = appWidgetIds;
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
    }

    @Override
    public void onDisabled(Context context) {
    }
}

