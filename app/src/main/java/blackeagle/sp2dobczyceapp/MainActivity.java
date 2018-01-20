package blackeagle.sp2dobczyceapp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int OPEN_SETTINGS_ID = 1001;

    LinearLayout mainLayout;
    SwipeRefreshLayout refreshLayout;

    UpdateManager.Result refreshResult = null;
    boolean isMeasured = false;
    boolean ignoreNoUpdate = false;
    boolean isDarkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Settings.loadSettings(this);
            if (!Settings.isReady) {
                startActivity(new Intent(this, WelcomeActivity.class));
                finish();
                return;
            }

            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
            if (isDarkTheme = Settings.applyNowDarkTheme())
                setTheme(R.style.DarkTheme);

            setContentView(R.layout.activity_main);

            Toolbar myToolbar = findViewById(R.id.toolbar);
            myToolbar.setTitleTextColor(0xffffffff);
            myToolbar.setSubtitleTextColor(0xffffffff);
            myToolbar.setTitle(R.string.app_name);
            setSupportActionBar(myToolbar);

            mainLayout = findViewById(R.id.main_layout);
            mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mainLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    isMeasured = true;
                    if (refreshResult != null)
                        createViewByResult();
                }
            });

            refreshLayout = findViewById(R.id.refresh_layout);
            refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    if (Settings.isOnline(MainActivity.this)) {
                        requestRefresh();
                    } else {
                        stopRefreshing();
                        showSnackbarMessage(R.string.no_internet);
                    }
                }
            });

            Intent intent = getIntent();

            ignoreNoUpdate = intent != null && intent.getBooleanExtra("fromNotification", false);

            if (savedInstanceState == null && Settings.isOnline(this)) {
                requestRefresh();
            } else {
                ignoreNoUpdate = false;
                refreshResult = UpdateManager.getDataFromFile(MainActivity.this);
                if (isMeasured)
                    createViewByResult();
            }

            if (intent != null && intent.getBooleanExtra("openSettings", false)) {
                startActivityForResult(new Intent(this, SettingsActivity.class), OPEN_SETTINGS_ID);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Nie udało się uruchomić Activity", Toast.LENGTH_LONG).show();
            startActivity(getIntent());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(R.string.timetable);
        item.setIcon(R.drawable.ic_event);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(MainActivity.this, LessonPlanActivity.class);
                intent.putExtra("fma", true);//from main activity
                startActivity(intent);
                return true;
            }
        });

        item = menu.add(R.string.settings);
        item.setIcon(R.drawable.ic_settings);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivityForResult(intent, OPEN_SETTINGS_ID);
                return true;
            }
        });

        item = menu.add(R.string.average_activity_title);
        item.setIcon(R.drawable.ic_toc);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                startActivity(new Intent(MainActivity.this, AverageMarkActivity.class));
                return true;
            }
        });

        item = menu.add("O aplikacji");
        item.setIcon(R.drawable.ic_info);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isDarkTheme != Settings.applyNowDarkTheme()) {
            finish();
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OPEN_SETTINGS_ID:
                if (data != null) {
                    if (data.getBooleanExtra("changedClass", false)) {
                        if (Settings.isOnline(this)) {
                            requestRefresh();
                        } else {
                            refreshResult = UpdateManager.getDataFromFile(MainActivity.this);
                            if (isMeasured)
                                createViewByResult();
                        }
                    }
                    if (data.getBooleanExtra("changedTheme", false)) {
                        finish();
                        startActivity(getIntent());
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    void requestRefresh() {
        refreshLayout.setRefreshing(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                refreshResult = UpdateManager.update(MainActivity.this);
                stopRefreshing();
                if (isMeasured)
                    createViewByResult();

            }
        }).start();
    }

    void stopRefreshing() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(false);
            }
        });
    }

    void showSnackbarMessage(@StringRes final int id) {
        showSnackbarMessage(getString(id));
    }

    void showSnackbarMessage(final String msg) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            Snackbar.make(mainLayout, msg, BaseTransientBottomBar.LENGTH_SHORT).show();
        else
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    void createViewByResult() {
        if (isDarkTheme != Settings.applyNowDarkTheme()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        if (refreshResult == null)
            return;
        runOnUiThread(new Runnable() {
            @SuppressWarnings("ConstantConditions")
            @Override
            public void run() {
                if (refreshResult.success) {

                    mainLayout.removeAllViews();
                    int width = mainLayout.getWidth();
                    mainLayout.addView(Section.createSeparator(MainActivity.this));
                    refreshResult.createLuckyNumberView(MainActivity.this, mainLayout, width);
                    mainLayout.addView(Section.createSeparator(MainActivity.this));
                    refreshResult.createViews(MainActivity.this, mainLayout, width);
                    if (!refreshResult.isFromFile) {
                        if (refreshResult.updated) {
                            if (refreshResult.newCount > 0) {
                                if (refreshResult.newCount == 1)
                                    showSnackbarMessage(R.string.one_new);
                                else if (refreshResult.newCount < 5)
                                    showSnackbarMessage(getString(R.string.something_is_new1, refreshResult.newCount));
                                else
                                    showSnackbarMessage(getString(R.string.something_is_new2, refreshResult.newCount));
                            } else
                                showSnackbarMessage(R.string.updated);
                        } else if (!ignoreNoUpdate)
                            showSnackbarMessage(R.string.nothing_is_new);
                    }

                    ignoreNoUpdate = false;

                    ActionBar bar = getSupportActionBar();
                    bar.setTitle(refreshResult.allNewsCount > 0 ?
                            ("Zastępstwa (" + String.valueOf(refreshResult.allNewsCount) + ")")
                            : "Zastępstwa");
                    bar.setSubtitle(Settings.getLastUpdateTime());

                    AlphaAnimation animation = new AlphaAnimation(0.f, 1.f);
                    animation.setDuration(300);
                    mainLayout.startAnimation(animation);

                } else {
                    showSnackbarMessage(R.string.cannot_refresh);
                }
            }
        });
    }
}
