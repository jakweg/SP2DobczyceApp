package blackeagle.sp2dobczyceapp;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

public class AverageMarkActivity extends AppCompatActivity {

    ArrayList<SubjectInfo> subjectList = new ArrayList<>();
    boolean changed = false;
    TextView summaryText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isDarkTheme = Settings.applyNowDarkTheme();
        if (isDarkTheme)
            setTheme(R.style.DarkTheme);

        setContentView(R.layout.activity_average_mark);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        setSupportActionBar(toolbar);
        summaryText = findViewById(R.id.summary_text);
        summaryText.setBackgroundColor(Settings.getColor(this,
                isDarkTheme ? R.color.sectionBackgroundDark : R.color.sectionBackground));

        if (!Settings.shownAverageAlert) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(AverageMarkActivity.this);
            builder.setTitle("Oblicz średnią");
            builder.setMessage(Html.fromHtml(
                    "Tutaj możesz łatwo obliczyć średnią z ocen na koniec roku\nPrzedmioty z oceną \'<b>-</b>\' nie są wliczane do średniej"));
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Settings.shownAverageAlert = true;
                    Settings.saveSettings(AverageMarkActivity.this);
                }
            });

            builder.create().show();
        }

        loadFromFile();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (changed)
            saveToFile();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home)
            finish();
        return true;
    }

    void saveToFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Collections.sort(subjectList, new Comparator<SubjectInfo>() {
                        @Override
                        public int compare(SubjectInfo o1, @SuppressWarnings("ComparatorMethodParameterNotUsed") SubjectInfo o2) {
                            if (o1.mark != 0)
                                return -1;
                            return 1;
                        }
                    });

                    PrintWriter stream = new PrintWriter(getApplicationInfo().dataDir + "/average");
                    stream.write(String.valueOf(subjectList.size()));
                    stream.write('\n');
                    for (SubjectInfo s : subjectList) {
                        stream.write(s.name);
                        stream.write('\n');
                        stream.write(String.valueOf(s.mark));
                        stream.write('\n');
                    }
                    stream.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    void loadFromFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File file = new File(getApplicationInfo().dataDir + "/average");
                    if (!file.exists()) {
                        setUpDefaultSubjects();
                        return;
                    }
                    Scanner scanner = new Scanner(new FileReader(file));
                    int count = Integer.valueOf(scanner.nextLine());
                    for (int i = 0; i < count; i++)
                        subjectList.add(new SubjectInfo(scanner.nextLine(), Integer.valueOf(scanner.nextLine())));

                    scanner.close();

                    createListLayout();
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(AverageMarkActivity.this, "Nie można wczytać :(", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    void setUpDefaultSubjects() {
        changed = true;
        subjectList.add(new SubjectInfo("J.polski", 0));
        subjectList.add(new SubjectInfo("J.angielski", 0));
        subjectList.add(new SubjectInfo("J.niemiecki", 0));
        subjectList.add(new SubjectInfo("Matematyka", 0));
        subjectList.add(new SubjectInfo("Historia", 0));
        subjectList.add(new SubjectInfo("WOS", 0));
        subjectList.add(new SubjectInfo("Geografia", 0));
        subjectList.add(new SubjectInfo("Biologia", 0));
        subjectList.add(new SubjectInfo("Chemia", 0));
        subjectList.add(new SubjectInfo("Fizyka", 0));
        subjectList.add(new SubjectInfo("Informatyka", 0));
        subjectList.add(new SubjectInfo("Wychowanie fizyczne", 0));
        subjectList.add(new SubjectInfo("Technika", 0));
        subjectList.add(new SubjectInfo("EDB", 0));
        subjectList.add(new SubjectInfo("Zaj. artystyczne", 0));
        subjectList.add(new SubjectInfo("Plastyka", 0));
        subjectList.add(new SubjectInfo("Muzyka", 0));
        subjectList.add(new SubjectInfo("Przyroda", 0));
        createListLayout();
    }

    void createListLayout() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ListView listView = findViewById(R.id.main_list);
                listView.setAdapter(new SubjectAdapter(AverageMarkActivity.this));
                recalculateAverage();
            }
        });
    }

    void onMarkChanged() {
        changed = true;
        recalculateAverage();
    }

    @SuppressLint("SetTextI18n")
    void recalculateAverage() {
        int count = 0;
        int sum = 0;
        for (SubjectInfo info : subjectList) {
            if (info.mark == 0)
                continue;
            count++;
            sum += info.mark;
        }

        if (count == 0) {
            summaryText.setText("Zaznacz oceny aby zobatrzyć średnią");
        } else {
            float average = (float) sum / (float) count;
            summaryText.setText(Html.fromHtml(String.format(
                    java.util.Locale.US, "Twoja średnia wynosi <b>%.2f</b><br/>%s",
                    average, average >= 4.75f ?
                            "Masz czerwony pasek na świadectwie \uD83D\uDE04"
                            : "Nie zasługujesz na pasek na świadectwie \uD83D\uDE1E")
                    .replace('.', ',')));
        }
    }

    class SubjectInfo {
        String name;
        Integer mark;

        SubjectInfo(String name, Integer mark) {
            this.name = name;
            this.mark = mark;
        }
    }

    class SubjectAdapter extends BaseAdapter implements View.OnClickListener {

        private final static int TAG_MINUS = 1;
        private final static int TAG_PLUS = 2;

        private AverageMarkActivity activity;
        private LayoutInflater inflater;

        SubjectAdapter(AverageMarkActivity activity) {
            this.activity = activity;
            inflater = LayoutInflater.from(activity);
        }

        @Override
        public int getCount() {
            return activity.subjectList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @SuppressLint("InflateParams")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = inflater.inflate(R.layout.subject_layout, null);

            SubjectInfo info = activity.subjectList.get(position);
            TextView markText = convertView.findViewById(R.id.average_mark);
            ((TextView) convertView.findViewById(R.id.subject_name)).setText(info.name);

            View view = convertView.findViewById(R.id.button_minus);
            view.setTag(TAG_MINUS);
            view.setTag(R.integer.TAG_POSITION, position);
            view.setTag(R.integer.TAG_TEXT_VIEW, markText);
            view.setOnClickListener(this);
            view = convertView.findViewById(R.id.button_plus);
            view.setTag(TAG_PLUS);
            view.setTag(R.integer.TAG_POSITION, position);
            view.setTag(R.integer.TAG_TEXT_VIEW, markText);
            view.setOnClickListener(this);

            (markText).setText(info.mark == 0 ? "-" : String.valueOf(info.mark));
            return convertView;
        }

        @Override
        public void onClick(View v) {
            Object temp = v.getTag();
            if (temp == null || !(temp instanceof Integer))
                return;
            int pos = (int) v.getTag(R.integer.TAG_POSITION);
            TextView textView = (TextView) v.getTag(R.integer.TAG_TEXT_VIEW);
            SubjectInfo info = subjectList.get(pos);
            switch ((int) temp) {
                case TAG_PLUS:
                    if (info.mark == 6)
                        break;
                    info.mark++;
                    textView.setText(String.valueOf(info.mark));
                    activity.onMarkChanged();
                    break;
                case TAG_MINUS:
                    if (info.mark == 0)
                        break;
                    info.mark--;
                    textView.setText(info.mark == 0 ? "-" : String.valueOf(info.mark));
                    activity.onMarkChanged();
                    break;
            }
        }
    }
}