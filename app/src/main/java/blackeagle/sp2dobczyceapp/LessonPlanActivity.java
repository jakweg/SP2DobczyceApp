package blackeagle.sp2dobczyceapp;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class LessonPlanActivity extends AppCompatActivity {

    ViewPager mViewPager;
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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null)
            savedInstanceState = getIntent().getExtras();

        if (savedInstanceState == null) {
            if (Settings.isClassSelected())
                lessonPlanName = Settings.className;
            else
                lessonPlanName = LessonPlanManager.classesList.get(0);
        } else {
            lessonPlanName = savedInstanceState.getString("name", Settings.isClassSelected() ?
                    Settings.className : LessonPlanManager.classesList.get(0));
            currentPage = savedInstanceState.getInt("page", -1);
        }

        if (getIntent().getBooleanExtra("fma", false))//from main activity
            toolbar.setNavigationIcon(R.drawable.ic_back);


        if (!loadLessonPlan()) {
            Toast.makeText(this, R.string.cannot_load_lesson_plan, Toast.LENGTH_SHORT).show();
            finish();
        }
        createTabbedView();

        final LinearLayout lessonTimeLayout = findViewById(R.id.lesson_time_layout);
        lessonTimeLayout.setBackgroundColor(Settings.getColor(this,
                isDarkTheme ? R.color.backgroundDark : R.color.background));
        LessonTimeManager.makeLayout(this, lessonTimeLayout);


        if (!Settings.isClassSelected()) {
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
                                    Snackbar.make(lessonTimeLayout, R.string.no_class_selected, BaseTransientBottomBar.LENGTH_INDEFINITE)
                                            .setAction(R.string.set_class_now, new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    finish();
                                                    Intent intent = new Intent(LessonPlanActivity.this, MainActivity.class);
                                                    intent.putExtra("openSettings", true);
                                                    startActivity(intent);
                                                }
                                            }).show();
                                else
                                    Toast.makeText(LessonPlanActivity.this, R.string.no_class_selected, Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                //empty
                            }
                        }
                    });
                }
            }).start();
        }
    }

    private boolean loadLessonPlan() {
        String filename = getApplicationInfo().dataDir
                + "/plans/"
                + lessonPlanName;

        thisPlan = LessonPlanManager.getPlan(filename);
        return thisPlan != null;
    }

    @SuppressLint("SwitchIntDef")
    void createTabbedView() {
        if (thisPlan == null) {
            return;
        }
        setTitle(getTitle() + " " + thisPlan.getInitials());

        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(
                getSupportFragmentManager(), thisPlan);

        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabs);
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

    @SuppressLint("MissingSuperCall")
//jeżeli wywołam metodę super.onSaveInstance(...); wywołują się błędy!
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //super.onSaveInstanceState(outState);//wywołuje crash!
        outState.putString("name", lessonPlanName);
        outState.putInt("page", currentPage);
    }

    void restartPlan() {
        Bundle b = new Bundle();
        onSaveInstanceState(b);
        finish();
        Intent intent = new Intent(this, LessonPlanActivity.class);
        intent.putExtras(b);
        startActivity(intent, b);
    }

    void restartPlan(String newPlan) {
        finish();
        Intent intent = new Intent(this, LessonPlanActivity.class);
        intent.putExtra("name", newPlan);
        intent.putExtra("page", currentPage);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(R.string.open_in_browser);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        item.setIcon(R.drawable.ic_open_in_browser);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(thisPlan.getLink()));
                    startActivity(browserIntent);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });

        item = menu.add("Edytuj");
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                finish();
                Intent intent = new Intent(LessonPlanActivity.this, EditLessonPlanActivity.class);
                intent.putExtra("name", thisPlan.getName());
                intent.putExtra("page", currentPage);
                startActivity(intent);
                return true;
            }
        });


        item = menu.add("Odśwież");
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (Settings.isOnline(LessonPlanActivity.this)) {
                    final ProgressDialog dialog = ProgressDialog.show(LessonPlanActivity.this, "Wczytywanie danych",
                            "Pobieranie wszytkich planów...\nTo może zająć chwilę", true);
                    dialog.setIndeterminate(true);
                    dialog.setCancelable(false);
                    dialog.show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (LessonPlanManager.downloadLists() &&
                                        LessonPlanManager.downloadAllPlans(LessonPlanActivity.this)) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(LessonPlanActivity.this, "Zakończono pomyślnie", Toast.LENGTH_SHORT).show();
                                            dialog.cancel();
                                            LessonPlanActivity.this.restartPlan();
                                        }
                                    });
                                } else
                                    throw new Exception();
                            } catch (Exception e) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(LessonPlanActivity.this,
                                                "Wystąpił błąd - aplikacja może nie działać poprawnie!", Toast.LENGTH_LONG).show();
                                        dialog.cancel();
                                        LessonPlanActivity.this.restartPlan();
                                    }
                                });
                            }
                        }
                    }).start();
                } else {
                    Toast.makeText(LessonPlanActivity.this, "Jesteś offline", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        SubMenu subMenu = menu.addSubMenu("Opcje pokazywania");

        item = subMenu.add(R.string.show_lesson_name);
        item.setCheckable(true);
        item.setChecked((Settings.lessonPlanRule & LessonPlanManager.LessonPlan.RULE_SHOW_SUBJECT) != 0);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Settings.lessonPlanRule ^= LessonPlanManager.LessonPlan.RULE_SHOW_SUBJECT;
                Settings.saveSettings(LessonPlanActivity.this);
                restartPlan();
                return true;
            }
        });

        if (thisPlan != null)
            item = subMenu.add(thisPlan.isTeacherPlan() ? R.string.show_class_name : R.string.show_teacher_name);
        else
            item = subMenu.add(Settings.isTeacher ? R.string.show_class_name : R.string.show_teacher_name);
        item.setCheckable(true);
        item.setChecked((Settings.lessonPlanRule & LessonPlanManager.LessonPlan.RULE_SHOW_TEACHER) != 0);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Settings.lessonPlanRule ^= LessonPlanManager.LessonPlan.RULE_SHOW_TEACHER;
                Settings.saveSettings(LessonPlanActivity.this);
                restartPlan();
                return true;
            }
        });

        item = subMenu.add(R.string.show_classroom);
        item.setCheckable(true);
        item.setChecked((Settings.lessonPlanRule & LessonPlanManager.LessonPlan.RULE_SHOW_CLASSROOM) != 0);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Settings.lessonPlanRule ^= LessonPlanManager.LessonPlan.RULE_SHOW_CLASSROOM;
                Settings.saveSettings(LessonPlanActivity.this);
                restartPlan();
                return true;
            }
        });

        subMenu = menu.addSubMenu(R.string.other_class);
        int id = 100;
        for (String s :
                LessonPlanManager.classesList) {
            subMenu.add(100, id++, 100, s);
        }

        subMenu = menu.addSubMenu(R.string.other_teachers);
        id = 200;
        for (String s :
                LessonPlanManager.teacherList) {
            subMenu.add(200, id++, 200, s);
        }

        item = menu.add(R.string.create_shortcut);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final String title = thisPlan.getName();
                if (!Settings.isClassSelected()) {
                    Settings.createShortcut(getApplicationContext(), "Plan lekcji " + thisPlan.getInitials(),
                            thisPlan.getName());
                    Toast.makeText(LessonPlanActivity.this, R.string.shortcut_created, Toast.LENGTH_SHORT).show();
                } else if (title.equals(Settings.className)) {
                    Settings.createShortcut(getApplicationContext(), "Plan lekcji", null);
                    Toast.makeText(LessonPlanActivity.this, R.string.shortcut_created, Toast.LENGTH_SHORT).show();
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(LessonPlanActivity.this);
                    builder.setTitle(R.string.create_shortcut);
                    builder.setMessage(getString(R.string.create_shortcut_to_other_class,
                            Settings.className, title));
                    builder.setNegativeButton(Settings.className, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Settings.createShortcut(getApplicationContext(), "Plan lekcji", null);
                            Toast.makeText(LessonPlanActivity.this, R.string.shortcut_created, Toast.LENGTH_SHORT).show();
                        }
                    });
                    builder.setPositiveButton(title, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Settings.createShortcut(getApplicationContext(), "Plan lekcji " + thisPlan.getInitials(),
                                    thisPlan.getName());
                            Toast.makeText(LessonPlanActivity.this, R.string.shortcut_created, Toast.LENGTH_SHORT).show();
                        }
                    });

                    builder.create().show();
                }
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home)
            finish();

        CharSequence title = item.getTitle();
        if (title != null) {
            if (id >= 200) {
                restartPlan(title.toString());//nauczyciele
                return true;
            } else if (id >= 100) {
                restartPlan(title.toString());//uczniowie
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public static class PlaceholderFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";
        private LessonPlanManager.LessonPlan lessonPlan;
        private int thisDay;

        public PlaceholderFragment() {
        }

        public static PlaceholderFragment newInstance(int sectionNumber, LessonPlanManager.LessonPlan lessonPlan) {
            PlaceholderFragment fragment = new PlaceholderFragment();
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
                        Settings.lessonPlanRule));
            }

            return viewGroup;
        }

        private View createLessonView(ContextThemeWrapper contextThemeWrapper, int lessonNumber, int displayRules) {
            View view = View.inflate(contextThemeWrapper, R.layout.one_lesson, null);

            if (lessonPlan == null)
                return view;

            ((TextView) view.findViewById(R.id.lesson_name)).setText(
                    lessonPlan.getDisplayedLesson(thisDay, lessonNumber, displayRules));

            return view;
        }
    }

    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        private LessonPlanManager.LessonPlan lessonPlan;

        SectionsPagerAdapter(FragmentManager fm, LessonPlanManager.LessonPlan lessonPlan) {
            super(fm);
            this.lessonPlan = lessonPlan;
        }

        @Override
        public Fragment getItem(int position) {
            return PlaceholderFragment.newInstance(position, lessonPlan);
        }

        @Override
        public int getCount() {
            return lessonPlan == null ? 0 : 5;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            /*switch (position){
                case 0: return "Poniedziałek";
                case 1: return "Wtorek";
                case 2: return "Środa";
                case 3: return "Czwartek";
                case 4: return "Piątek";
            }*/
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
