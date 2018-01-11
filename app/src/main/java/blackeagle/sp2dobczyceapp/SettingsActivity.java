package blackeagle.sp2dobczyceapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.support.v7.app.AppCompatDelegate;
import android.text.InputType;
import android.widget.Toast;

import me.leolin.shortcutbadger.ShortcutBadger;


@SuppressLint("ExportedPreferenceActivity")
public class SettingsActivity extends PreferenceActivity implements Settings.OnSettingsChangeListener {


    String lastClass;
    Intent returnData = new Intent();
    private boolean isDarkTheme;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Settings.loadSettings(this);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        if (!Settings.isReady) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }
        lastClass = Settings.className;

        final MyPreferenceFragment fragment = new MyPreferenceFragment();
        fragment.changeListener = this;
        getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();

    }

    @Override
    public void onClassChange() {
        try {
            ShortcutBadger.removeCount(getApplicationContext());
        } catch (Exception e) {/*xd*/ }
        LessonFinishService.stopService(this);
        LessonFinishService.startService(getApplicationContext());
        LessonPlanWidget.refreshWidgets(getApplicationContext());
        returnData.putExtra("changedClass", true);
        setResult(RESULT_OK, returnData);
    }

    @Override
    public void onThemeChange() {
        returnData.putExtra("changedTheme", true);
        LessonPlanWidget.refreshWidgets(getApplicationContext());
        setResult(RESULT_OK, returnData);
        if (isDarkTheme != Settings.applyNowDarkTheme()) {
            startActivity(getIntent());
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Settings.loadSettings(this, true);
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, @StyleRes int resId, boolean first) {
        isDarkTheme = Settings.applyNowDarkTheme();
        theme.applyStyle(isDarkTheme ? R.style.DarkThemeSettings : R.style.AppThemeSettings, true);
    }

    public static class MyPreferenceFragment extends PreferenceFragment {

        Settings.OnSettingsChangeListener changeListener = null;

        Context context;

        boolean isDarkTheme;
        @ColorInt
        int blackIconColor;
        @ColorInt
        int whiteIconColor;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            context = getActivity();

            isDarkTheme = Settings.applyNowDarkTheme();
            blackIconColor = Settings.getColor(context, R.color.white);
            whiteIconColor = Settings.getColor(context, R.color.black);

            getPreferenceManager().setSharedPreferencesName("settings");

            PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

            PreferenceCategory category = new PreferenceCategory(context);
            category.setTitle(R.string.main_category);
            screen.addPreference(category);

            final ListPreference listPreference = new ListPreference(context);
            final EditTextPreference luckyNumberPreference = new EditTextPreference(context);
            final CheckBoxPreference teacherCheckBox = new CheckBoxPreference(context);
            final CharSequence[] classes = LessonPlanManager.classesList
                    .toArray(new CharSequence[LessonPlanManager.classesList.size()]);
            final CharSequence[] teachers = LessonPlanManager.teacherList
                    .toArray(new CharSequence[LessonPlanManager.teacherList.size()]);

            final Preference.OnPreferenceChangeListener preferenceListener = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue instanceof String)
                        Settings.className = (String) newValue;
                    else {
                        Settings.isTeacher = (boolean) newValue;
                        Settings.className = Settings.isTeacher ?
                                LessonPlanManager.teacherList.get(0) : LessonPlanManager.classesList.get(0);
                    }

                    if (Settings.isTeacher) {
                        listPreference.setTitle(context.getString(R.string.your_surname, Settings.className));
                        listPreference.setEntries(teachers);
                        listPreference.setEntryValues(teachers);
                        listPreference.setValue(Settings.className);
                        luckyNumberPreference.setEnabled(false);
                    } else {
                        listPreference.setTitle(context.getString(R.string.your_class, Settings.className));
                        listPreference.setEntries(classes);
                        listPreference.setEntryValues(classes);
                        listPreference.setValue(Settings.className);
                        luckyNumberPreference.setEnabled(true);
                    }

                    if (changeListener != null)
                        changeListener.onClassChange();
                    return true;
                }
            };

            teacherCheckBox.setTitle(R.string.i_am_teacher);
            teacherCheckBox.setKey("isTeacher");
            teacherCheckBox.setOnPreferenceChangeListener(preferenceListener);
            category.addPreference(teacherCheckBox);

            listPreference.setTitle(context.getString(teacherCheckBox.isChecked() ? R.string.your_surname : R.string.your_class,
                    Settings.className));
            listPreference.setEntries(teacherCheckBox.isChecked() ? teachers : classes);
            listPreference.setEntryValues(teacherCheckBox.isChecked() ? teachers : classes);
            listPreference.setKey("className");
            listPreference.setValue(Settings.className);
            listPreference.setSummary(R.string.choose_your_class_or_surname);
            listPreference.setOnPreferenceChangeListener(preferenceListener);
            listPreference.setIcon(Settings.getDyedDrawable(context, R.drawable.ic_people, isDarkTheme));
            category.addPreference(listPreference);

            luckyNumberPreference.setKey("usersNumber");
            luckyNumberPreference.setDefaultValue("0");
            luckyNumberPreference.setEnabled(!Settings.isTeacher);
            luckyNumberPreference.setIcon(Settings.getDyedDrawable(context, R.drawable.ic_filter_7, isDarkTheme));
            luckyNumberPreference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
            luckyNumberPreference.setTitle("Szczęśliwy numerek");
            luckyNumberPreference.setSummary("Dostaniesz powiadomienie gdy zostaniesz szczęśliwcem");
            luckyNumberPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    try {
                        int number = Integer.valueOf((String) newValue);
                        if (number == 0) {
                            Toast.makeText(context, "Usunięto szczęśliwy numerek", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        if (number <= 30)
                            return true;
                    } catch (Exception e) {
                        //empty
                    }
                    Toast.makeText(context, "Nieprawidłowa liczba", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
            category.addPreference(luckyNumberPreference);

            category = new PreferenceCategory(context);
            category.setTitle(R.string.appearance);
            screen.addPreference(category);

            ListPreference darkThemeList = new ListPreference(context);
            CharSequence[] darkThemeEntries = new CharSequence[]{"nigdy", "tylko w nocy", "zawsze"};
            CharSequence[] darkThemeValues = new CharSequence[]{"0", "1", "2"};
            darkThemeList.setEntries(darkThemeEntries);
            darkThemeList.setEntryValues(darkThemeValues);
            darkThemeList.setIcon(Settings.getDyedDrawable(context, R.drawable.ic_style, isDarkTheme));
            darkThemeList.setTitle(R.string.dark_theme);
            darkThemeList.setSummary(R.string.choose_dark_theme);
            darkThemeList.setKey("darkTheme");
            darkThemeList.setDefaultValue("0");
            darkThemeList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    try {
                        Settings.darkModeState = Integer.valueOf((String) newValue);
                        if (changeListener != null)
                            changeListener.onThemeChange();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                }
            });
            category.addPreference(darkThemeList);

            try {
                if (ShortcutBadger.isBadgeCounterSupported(context.getApplicationContext())) {
                    CheckBoxPreference showBadgesCheckBox = new CheckBoxPreference(context);
                    showBadgesCheckBox.setTitle("Liczba zastępstw na pulpicie");
                    showBadgesCheckBox.setKey("showBadges");
                    showBadgesCheckBox.setSummary("Pokazuj liczbę zastępstw na ikonie w ekranie głównym");
                    showBadgesCheckBox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            Settings.showBadges = (boolean) newValue;
                            if (!((boolean) newValue))
                                ShortcutBadger.removeCount(context.getApplicationContext());
                            return true;
                        }
                    });
                    category.addPreference(showBadgesCheckBox);
                }
            } catch (Exception e) {
                //xd
            }

            category = new PreferenceCategory(context);
            category.setTitle(R.string.working_in_background);
            screen.addPreference(category);

            CheckBoxPreference allowWorkInBgCheckBox = new CheckBoxPreference(context);
            allowWorkInBgCheckBox.setDefaultValue(true);
            allowWorkInBgCheckBox.setKey("canWorkInBackground");
            allowWorkInBgCheckBox.setTitle("Zezwalaj na działanie w tle");
            allowWorkInBgCheckBox.setSummaryOff("Nie dostaniesz powiadomień");
            allowWorkInBgCheckBox.setSummaryOn("Będziesz dostawać powiadomienia");
            allowWorkInBgCheckBox.setIcon(Settings.getDyedDrawable(context, R.drawable.ic_sync, isDarkTheme));
            allowWorkInBgCheckBox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (Settings.canWorkInBackground = (boolean) newValue)
                        UpdateService.startService(context);
                    else
                        UpdateService.stopService(context);
                    return true;
                }
            });
            category.addPreference(allowWorkInBgCheckBox);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Preference notifyPreference = new Preference(context);
                notifyPreference.setIcon(Settings.getDyedDrawable(context, R.drawable.ic_notifications, isDarkTheme));
                notifyPreference.setTitle("Ustawienia powiadomień");
                notifyPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent i = new Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                        i.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                        i.putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, Settings.CHANNEL_ID_NEWS);
                        startActivity(i);
                        return true;
                    }
                });
                category.addPreference(notifyPreference);
            } else {
                CheckBoxPreference notifyCheckBox = new CheckBoxPreference(context);
                notifyCheckBox.setTitle(R.string.notify_with_sound);
                notifyCheckBox.setKey("canNotify");
                //notifyCheckBox.setSummaryOn("Otrzymasz powiadomienia z dźwiękiem");
                //notifyCheckBox.setSummaryOff("Powiadomienie nie zabrzmi");
                notifyCheckBox.setIcon(Settings.getDyedDrawable(context, R.drawable.ic_notifications, isDarkTheme));
                notifyCheckBox.setDefaultValue(true);
                category.addPreference(notifyCheckBox);
            }

            category = new PreferenceCategory(context);
            category.setTitle("Czas do końca lekcji");
            screen.addPreference(category);

            CheckBoxPreference showFinishTimeCheckBox = new CheckBoxPreference(context);
            showFinishTimeCheckBox.setIcon(Settings.getDyedDrawable(context, R.drawable.ic_access_time, isDarkTheme));
            showFinishTimeCheckBox.setKey("showFinishTimeNotification");
            showFinishTimeCheckBox.setDefaultValue(false);
            showFinishTimeCheckBox.setTitle("Pokazuj powiadomienie");
            showFinishTimeCheckBox.setSummary("Powiadomienie które odlicza czas do końca");
            showFinishTimeCheckBox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (Settings.showFinishTimeNotification = (boolean) newValue)
                        LessonFinishService.startService(context);
                    else
                        LessonFinishService.stopService(context);
                    return true;
                }
            });
            category.addPreference(showFinishTimeCheckBox);

            EditTextPreference delayEditTextPreference = new EditTextPreference(context);
            delayEditTextPreference.setKey("finishTimeDelay");
            delayEditTextPreference.setDefaultValue("0");
            delayEditTextPreference.getEditText().setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_CLASS_NUMBER);
            delayEditTextPreference.setTitle("Opóźnienie względem dzwonka");
            delayEditTextPreference.setSummary("Dzwonek szkolny nie idzie równo z czasem w twoim telefonie. Ustaw różnicę w sekundach");
            delayEditTextPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    try {
                        int number = Integer.valueOf((String) newValue);
                        if (number <= 60 && number >= -60)
                            return true;
                    } catch (Exception e) {
                        //empty
                    }
                    Toast.makeText(context, "Nieprawidłowa liczba", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });

            category.addPreference(delayEditTextPreference);

            category = new PreferenceCategory(context);
            category.setTitle("Github");

            Preference gitPreference = new Preference(context);
            gitPreference.setIcon(Settings.getDyedDrawable(context, R.drawable.ic_developer, isDarkTheme));
            gitPreference.setTitle("Otwórz GitHub");
            gitPreference.setSummary(R.string.about_github);
            gitPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(getString(R.string.github_link)));
                    startActivity(intent);
                    return false;
                }
            });

            screen.addPreference(category);
            category.addPreference(gitPreference);

            setPreferenceScreen(screen);
        }

    }
}
