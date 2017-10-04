package blackeagle.sp2dobczyceapp;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

@SuppressWarnings("WeakerAccess")
abstract class LessonTimeManager {
    static final int BREAK = 0;
    static final int LESSON = 1;
    static final int AFTER_LESSON = 2;
    static final int BEFORE_LESSON = 3;
    static final int ABOUT_TO_START_LESSON = 4;

    static int getLessonBeginning(int lesson) {
        switch (lesson) {
            case 0:
                return 8 * 60;
            case 1:
                return 8 * 60 + 55;
            case 2:
                return 9 * 60 + 50;
            case 3:
                return 10 * 60 + 45;
            case 4:
                return 11 * 60 + 50;
            case 5:
                return 12 * 60 + 50;
            case 6:
                return 13 * 60 + 45;
            case 7:
                return 14 * 60 + 35;
        }
        return 0;
    }

    static int getBreakLength(int breakNumber) {
        switch (breakNumber) {
            case 0:
            case 1:
            case 2:
            case 5:
                return 10;
            case 3:
                return 20;
            case 4:
                return 15;
            case 6:
                return 5;
        }
        return 0;
    }

    static void makeLayout(Context context, RemoteViews parent, boolean darkTheme) {

        for (int i = 0; i < 8; i++) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.one_widget_lesson);
            int beginning = getLessonBeginning(i);
            int end = beginning + 45;
            views.setTextViewText(R.id.lesson_name, String.format(Locale.getDefault(), "%d. %d:%02d-%d:%02d",
                    i + 1, beginning / 60, beginning % 60, end / 60, end % 60));
            if (darkTheme)
                views.setTextColor(R.id.lesson_name, 0xffc1c1c1);

            parent.addView(R.id.lesson_time_layout, views);
        }
    }

    static void makeLayout(Context context, LinearLayout parent) {
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(context,
                Settings.applyNowDarkTheme() ? R.style.DarkTheme : R.style.AppTheme);

        for (int i = 0; i < 8; i++) {
            View view = View.inflate(contextThemeWrapper, R.layout.lesson_time_layout, null);
            int beginning = getLessonBeginning(i);
            int end = beginning + 45;
            ((TextView) view.findViewById(R.id.lesson_time)).setText(context.getString(R.string.lesson_time_hour,
                    i + 1, beginning / 60, beginning % 60, end / 60, end % 60));
            parent.addView(view);
        }
    }

    static int getCurrentSecond() {
        Calendar instance = Calendar.getInstance();
        return instance.get(Calendar.HOUR_OF_DAY) * 3600 +
                instance.get(Calendar.MINUTE) * 60 +
                instance.get(Calendar.SECOND) +
                Settings.finishTimeDelay;
    }

    static LessonState getCurrentLesson(int[] counts) {
        Calendar c = Calendar.getInstance();

        LessonState state = new LessonState();

        state.thisState = BEFORE_LESSON;
        int currentSecond = getCurrentSecond();
        if (currentSecond < (7 * 60 + 15) * 60)
            return state;

        state.thisState = ABOUT_TO_START_LESSON;
        if (currentSecond < 8 * 60 * 60)
            return state;

        int day = c.get(Calendar.DAY_OF_WEEK) - 2;//poniedziałek daje 2

        for (int i = 0; i < counts[day]; i++) {
            if (currentSecond < (getLessonBeginning(i) + 45) * 60) {
                state.thisState = LESSON;
                state.lessonNumber = i;
                return state;
            } else if (currentSecond < (getLessonBeginning(i) + 45 + getBreakLength(i)) * 60) {
                state.thisState = BREAK;
                state.lessonNumber = i;
                return state;
            }
        }
        state.thisState = AFTER_LESSON;
        return state;
    }

    static class LessonState {

        int thisState = -1;
        int lessonNumber = -1;

        boolean isInSchool() {
            return thisState == LESSON
                    || thisState == BREAK;
        }

        int getSecondsToEnd() {
            int currentSec = getCurrentSecond();
            switch (thisState) {
                case LESSON:
                    return (getLessonBeginning(lessonNumber) + 45) * 60 - currentSec;
                case BREAK:
                    return (getLessonBeginning(lessonNumber) + getBreakLength(lessonNumber) + 45) * 60 - currentSec;
                case ABOUT_TO_START_LESSON:
                    return getLessonBeginning(0) * 60 - currentSec;
                default:
                    return 0;
            }
        }

    }
}
