package blackeagle.sp2dobczyceapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class EditLessonPlanActivity extends AppCompatActivity {

    private final static int APPLY_REQUEST_CODE = 1;
    ViewPager mViewPager;
    private boolean isTeacherPlan;
    private String lessonPlanName;
    private LessonPlanManager.LessonPlan thisPlan = null;
    private int currentPage = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Settings.loadSettings(this);
        if (!Settings.isReady) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        boolean isDarkTheme = Settings.applyNowDarkTheme();
        if (isDarkTheme)
            setTheme(R.style.DarkTheme);

        setContentView(R.layout.activity_lesson_plan);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null)
            savedInstanceState = getIntent().getExtras();

        if (savedInstanceState == null) {
            if (Settings.isClassSelected())
                lessonPlanName = Settings.getClassOrTeacherName();
            else
                lessonPlanName = LessonPlanManager.classesList.get(0);
            isTeacherPlan = Settings.isTeacher;
        } else {
            isTeacherPlan = savedInstanceState.getBoolean("isTeacher");
            lessonPlanName = savedInstanceState.getString("name");
            currentPage = savedInstanceState.getInt("page", -1);
        }


        String filename = getApplicationInfo().dataDir
                + "/plans/"
                + lessonPlanName;

        thisPlan = LessonPlanManager.getPlan(filename);
        if (thisPlan == null) {
            Toast.makeText(this, R.string.cannot_load_lesson_plan, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        createTabbedView();

        final LinearLayout lessonTimeLayout = (LinearLayout) findViewById(R.id.lesson_time_layout);
        lessonTimeLayout.setBackgroundColor(Settings.getColor(this,
                isDarkTheme ? R.color.backgroundDark : R.color.background));
        LessonTimeManager.makeLayout(this, lessonTimeLayout);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                                Snackbar.make(lessonTimeLayout, "Dotknij lekcji aby ją edytować", BaseTransientBottomBar.LENGTH_SHORT).show();
                            else
                                Toast.makeText(EditLessonPlanActivity.this, "Dotknij lekcji aby ją edytować", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            //empty
                        }
                    }
                });
            }
        }).start();

        disableOrientationChanges();
    }

    @SuppressLint("SwitchIntDef")
    void createTabbedView() {
        setTitle("Edycja planu " + thisPlan.getInitials());

        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(
                getSupportFragmentManager(), thisPlan, this);

        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                currentPage = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        if (currentPage == -1) {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);

            switch (calendar.get(Calendar.DAY_OF_WEEK)) {
                case Calendar.MONDAY:
                    mViewPager.setCurrentItem(hour < 16 ? 0 : 1);
                    break;
                case Calendar.TUESDAY:
                    mViewPager.setCurrentItem(hour < 16 ? 1 : 2);
                    break;
                case Calendar.WEDNESDAY:
                    mViewPager.setCurrentItem(hour < 16 ? 2 : 3);
                    break;
                case Calendar.THURSDAY:
                    mViewPager.setCurrentItem(hour < 16 ? 3 : 4);
                    break;
                case Calendar.FRIDAY:
                    mViewPager.setCurrentItem(4);
                    break;
                default:
                    mViewPager.setCurrentItem(0);
                    break;
            }
        } else
            mViewPager.setCurrentItem(currentPage);
    }

    private void disableOrientationChanges() {
        int deviceRotation = getWindowManager().getDefaultDisplay().getRotation();

        if (deviceRotation == Surface.ROTATION_0)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else if (deviceRotation == Surface.ROTATION_180)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        else if (deviceRotation == Surface.ROTATION_90)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else if (deviceRotation == Surface.ROTATION_270)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add("Zapisz");
        item.setIcon(R.drawable.ic_save);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                saveAndQuit();
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case APPLY_REQUEST_CODE:
                if (resultCode == RESULT_OK && data != null) {
                    thisPlan.setLesson(data.getExtras().getInt("day", -1),
                            data.getExtras().getInt("lessonNumber", -1),
                            (LessonPlanManager.Lesson) data.getExtras().getSerializable("lesson"));
                    createTabbedView();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void saveAndQuit() {
        thisPlan.saveToFile(getApplicationInfo().dataDir + "/plans/" + thisPlan.getName());

        finish();
        Intent intent = new Intent(this, LessonPlanActivity.class);
        intent.putExtra("isTeacher", isTeacherPlan);
        intent.putExtra("name", lessonPlanName);
        intent.putExtra("page", currentPage);
        startActivity(intent);
        LessonPlanWidget.refreshWidgets(getApplicationContext());
    }

    public static class PlaceholderFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";
        private Activity activity;
        private LessonPlanManager.LessonPlan lessonPlan;
        private int thisDay;

        public PlaceholderFragment() {
        }

        public static PlaceholderFragment newInstance(int sectionNumber, LessonPlanManager.LessonPlan lessonPlan, Activity activity) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            fragment.activity = activity;
            fragment.thisDay = sectionNumber;
            fragment.lessonPlan = lessonPlan;
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_lesson_plan, container, false);

            rootView.setBackgroundColor(Settings.getColor(inflater.getContext(),
                    Settings.applyNowDarkTheme() ? R.color.sectionBackgroundDark : R.color.sectionBackground));

            ViewGroup viewGroup = (ViewGroup) rootView;

            ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(inflater.getContext(),
                    Settings.applyNowDarkTheme() ? R.style.DarkTheme : R.style.AppTheme);
            for (int i = 0; i < 8; i++) {
                viewGroup.addView(createLessonView(contextThemeWrapper, i,
                        0xffffffff));
            }

            return viewGroup;
        }

        private View createLessonView(final ContextThemeWrapper contextThemeWrapper, final int lessonNumber, int displayRules) {
            View view = View.inflate(contextThemeWrapper, R.layout.one_lesson, null);

            ((TextView) view.findViewById(R.id.lesson_name)).setText(
                    lessonPlan.getDisplayedLesson(thisDay, lessonNumber, displayRules));
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(contextThemeWrapper.getBaseContext(), InsertLessonDataActivity.class);
                    intent.putExtra("lesson", lessonPlan.getLesson(thisDay, lessonNumber));
                    intent.putExtra("day", thisDay);
                    intent.putExtra("lessonNumber", lessonNumber);
                    activity.startActivityForResult(intent, APPLY_REQUEST_CODE);
                }
            });

            return view;
        }
    }

    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        private final LessonPlanManager.LessonPlan lessonPlan;
        private Activity activity = null;

        SectionsPagerAdapter(FragmentManager fm, @NonNull LessonPlanManager.LessonPlan lessonPlan, Activity activity) {
            super(fm);
            this.lessonPlan = lessonPlan;
            this.activity = activity;
        }

        @Override
        public Fragment getItem(int position) {
            return PlaceholderFragment.newInstance(position, lessonPlan, activity);
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Pon";
                case 1:
                    return "Wt";
                case 2:
                    return "Śr";
                case 3:
                    return "Cz";
                case 4:
                    return "Pt";
            }
            return null;
        }
    }
}
