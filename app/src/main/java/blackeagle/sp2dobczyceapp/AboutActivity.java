package blackeagle.sp2dobczyceapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
    }

    public void onContactClick(View view) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"jakubek.weg@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Kontakt odnośnie aplikacji");
        startActivity(Intent.createChooser(intent, ""));
    }

    public void onGithubClick(View view) {
    }
}
