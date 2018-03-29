package blackeagle.sp2dobczyceapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ScrollView;

public class AboutActivity extends AppCompatActivity {

    int clicks = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Settings.applyNowDarkTheme())
            setTheme(R.style.DarkTheme);
        setContentView(R.layout.activity_about);

        final ViewGroup viewGroup = ((ScrollView) findViewById(R.id.scrollBox));
        viewGroup.postDelayed(new Runnable() {
            @Override
            public void run() {
                Animation translateAnimation = new AlphaAnimation(0.f, 1.f);
                translateAnimation.setDuration(400);
                viewGroup.setAlpha(1.f);
                viewGroup.startAnimation(translateAnimation);
            }
        }, 150);


        findViewById(R.id.app_icon).setOnClickListener(new View.OnClickListener() {
            boolean isAnimating = false;

            @Override
            public void onClick(View v) {
                if (isAnimating)
                    return;
                if (clicks == 10) {
                    Animation animation = new RotateAnimation(0.f, 360.f,
                            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    animation.setDuration(1000);
                    animation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            isAnimating = true;
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            isAnimating = false;
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                    v.startAnimation(animation);
                    clicks = 0;
                } else {
                    clicks++;
                    Animation animation = new ScaleAnimation(1.05f, 1.f, 1.05f, 1.f,
                            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    animation.setDuration(300);
                    v.startAnimation(animation);
                }
            }
        });
    }

    public void onContactClick(View view) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"jakubek.weg@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Kontakt odnośnie aplikacji");
        startActivity(Intent.createChooser(intent, ""));
    }

    public void onGithubClick(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link)));
        startActivity(intent);
    }
}
