package blackeagle.sp2dobczyceapp;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;

import java.util.ArrayList;

public class WelcomeActivity extends AppCompatActivity {

    private static final int PAGE_NO_INTERNET = 0;
    private static final int PAGE_LOADING = 1;
    private static final int PAGE_CHOOSE_CLASS = 2;
    private static final int PAGE_FINAL = 4;
    private static final int PAGE_CHOOSE_NUMBER = 3;
    private int currentPage = -1;
    private LinearLayout mainLayout;

    private String selectedClass = null;
    private boolean isTeacher = false;

    private boolean hasDownloadedLists = false;

    private boolean isDownloading = false;

    private boolean isReadyToFinish = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitleTextColor(Settings.getColor(this, R.color.white));
        myToolbar.setTitle(R.string.welcome_text);
        setSupportActionBar(myToolbar);

        mainLayout = ((LinearLayout) findViewById(R.id.main_layout));

        disableOrientationChanges();

        if (Settings.isOnline(this)) {
            setNextPage(PAGE_LOADING);
            startLoadingList();
        } else
            setNextPage(PAGE_NO_INTERNET);
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

    private void startLoadingList() {
        Thread downloadThread;
        if (hasDownloadedLists) {
            downloadThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    isDownloading = true;
                    if (LessonPlanManager.downloadAllPlans(WelcomeActivity.this)) {
                        if (currentPage == PAGE_LOADING) {
                            isReadyToFinish = true;
                        }
                    } else {
                        setNextPage(PAGE_NO_INTERNET);
                    }
                    isDownloading = false;
                }
            });
        } else {
            downloadThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    isDownloading = true;
                    if (LessonPlanManager.downloadLists()) {
                        hasDownloadedLists = true;
                        setNextPage(PAGE_CHOOSE_CLASS);

                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        startLoadingList();
                    } else {
                        hasDownloadedLists = false;
                        setNextPage(PAGE_NO_INTERNET);
                    }

                    isDownloading = false;
                }
            });
        }
        downloadThread.start();
    }

    private void setNextPage(final int nextPage) {
        currentPage = nextPage;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainLayout.removeAllViews();
                LayoutInflater inflater = LayoutInflater.from(WelcomeActivity.this);
                View view;
                switch (nextPage) {
                    case PAGE_NO_INTERNET:
                        hasDownloadedLists = false;
                        view = inflater.inflate(R.layout.no_internet_layout, mainLayout);
                        view.findViewById(R.id.retry_button).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                onRetryClick();
                            }
                        });
                        break;
                    case PAGE_LOADING:
                        view = inflater.inflate(R.layout.loading_layout, mainLayout);
                        Animation animation = new RotateAnimation(0.f, 360.f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                        animation.setDuration(1000);
                        animation.setRepeatMode(Animation.RESTART);
                        animation.setRepeatCount(Animation.INFINITE);
                        animation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                                if (isReadyToFinish) {
                                    UpdateService.startService(WelcomeActivity.this);
                                    finish();
                                    startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
                                }
                            }
                        });
                        view.findViewById(R.id.image).startAnimation(animation);
                        break;
                    case PAGE_CHOOSE_CLASS: {
                        view = inflater.inflate(R.layout.choose_class_layout, mainLayout);

                        final Spinner spinner = (Spinner) view.findViewById(R.id.spinner);

                        ((Switch) view.findViewById(R.id.teacher_switch)).setChecked(true);
                        ((Switch) view.findViewById(R.id.teacher_switch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                isTeacher = isChecked;
                                ArrayAdapter<String> spinnerArrayAdapter;
                                if (isChecked) {
                                    spinnerArrayAdapter = new ArrayAdapter<>(WelcomeActivity.this, android.R.layout.simple_spinner_item, LessonPlanManager.teacherList);
                                } else {
                                    spinnerArrayAdapter = new ArrayAdapter<>(WelcomeActivity.this, android.R.layout.simple_spinner_item, LessonPlanManager.classesList);
                                }
                                spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                                spinner.setAdapter(spinnerArrayAdapter);
                                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                        if (isTeacher)
                                            selectedClass = LessonPlanManager.teacherList.get(position);
                                        else
                                            selectedClass = LessonPlanManager.classesList.get(position);

                                    }

                                    @Override
                                    public void onNothingSelected(AdapterView<?> parent) {

                                    }
                                });
                            }
                        });
                        ((Switch) view.findViewById(R.id.teacher_switch)).setChecked(false);

                        view.findViewById(R.id.next_button).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (isTeacher)
                                    setNextPage(PAGE_FINAL);
                                else
                                    setNextPage(PAGE_CHOOSE_NUMBER);
                            }
                        });
                        view.findViewById(R.id.skip_button).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                setNextPage(PAGE_FINAL);
                                isTeacher = false;
                                selectedClass = "";
                            }
                        });
                        break;
                    }
                    case PAGE_CHOOSE_NUMBER: {
                        view = inflater.inflate(R.layout.choose_number_layout, mainLayout);
                        final Spinner spinner = (Spinner) view.findViewById(R.id.spinner);

                        ArrayList<String> list = new ArrayList<>(30);
                        for (int i = 0; i < 30; i++)
                            list.add(String.valueOf(i + 1));


                        ArrayAdapter<String> spinnerArrayAdapter =
                                new ArrayAdapter<>(WelcomeActivity.this, android.R.layout.simple_spinner_item, list);
                        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner.setAdapter(spinnerArrayAdapter);

                        view.findViewById(R.id.next_button).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Settings.usersNumber = Integer.valueOf((String) spinner.getSelectedItem());
                                setNextPage(PAGE_FINAL);
                            }
                        });
                        view.findViewById(R.id.skip_button).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                setNextPage(PAGE_FINAL);
                                Settings.usersNumber = 0;
                            }
                        });
                        break;
                    }
                    case PAGE_FINAL:
                        view = inflater.inflate(R.layout.final_layout, mainLayout);
                        view.findViewById(R.id.next_button).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Settings.className = selectedClass;
                                Settings.isTeacher = isTeacher;
                                Settings.isReady = true;
                                Settings.saveSettings(WelcomeActivity.this);
                                if (isDownloading) {
                                    setNextPage(PAGE_LOADING);
                                } else {
                                    UpdateService.startService(WelcomeActivity.this);
                                    finish();
                                    startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
                                }
                            }
                        });
                        break;
                }
                Animation anim = new AlphaAnimation(0.f, 1.f);
                anim.setDuration(300);
                mainLayout.setAnimation(anim);
            }
        });

    }

    private void onRetryClick() {
        if (Settings.isOnline(this)) {
            setNextPage(PAGE_LOADING);
            startLoadingList();
        }
    }
}
