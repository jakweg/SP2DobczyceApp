package blackeagle.sp2dobczyceapp;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import me.leolin.shortcutbadger.ShortcutBadger;

class UpdateManager {

    private final static Object mutex = new Object();

    static Result getDataFromFile(Context context) {
        synchronized (mutex) {
            Result result = new Result();
            result.updated = false;
            result.isFromFile = true;
            try {
                File file = new File(context.getApplicationInfo().dataDir + "/substitute");
                Scanner scanner = new Scanner(file);

                while (scanner.hasNextLine()) {
                    String s = scanner.nextLine().replaceAll("\\n", "\n");
                    result.days.add(s);
                }


                int count = 0;
                for (String day : result.days) {
                    for (String line : day.split("<br {2}/>")) {
                        if (containsSubstituteForUser(line)) {
                            count++;
                        }
                    }
                }
                result.allNewsCount = count;

                scanner.close();
                result.success = true;
            } catch (Exception e) {
                e.printStackTrace();
                result.success = false;
            }
            return result;
        }
    }

    @NonNull
    static Result update(Context context) {
        synchronized (mutex) {
            Result result = new Result();
            try {
                //String html = LessonPlanManager.getHtml("http://sp2dobczyce.pl/gim/index.html");
                String html = LessonPlanManager.getHtml("http://sp2dobczyce.pl/zastepstwa/index.html");

                String searched = "       <div class=\"clear\"></div>";
                html = html.substring(html.indexOf(searched) + searched.length(), +html.lastIndexOf("</div>                  <div class=\"clear\"></div>"));

                List<String> days = new ArrayList<>(Arrays.asList(html.split("<div class=\"table\">")));
                int i = 0;
                for (Iterator<String> it = days.iterator(); it.hasNext(); ) {
                    String section = normalizeSection(it.next());
                    if (section == null) {
                        it.remove();
                        continue;
                    }
                    days.set(i++, section);
                    result.days.add(section);
                }


                ArrayList<String> substitutesList = new ArrayList<>();
                for (String day : days) {
                    for (String line : day.split("<br {2}/>")) {
                        if (containsSubstituteForUser(line)) {
                            substitutesList.add(line);
                        }
                    }
                }

                result.allNewsCount = substitutesList.size();
                try {
                    if (Settings.showBadges)
                        ShortcutBadger.applyCount(context.getApplicationContext(), result.allNewsCount);
                    else
                        ShortcutBadger.removeCount(context.getApplicationContext());
                } catch (Exception e) {/*xd*/ }

                try {
                    String lastContent = "";
                    if (new File(context.getApplicationInfo().dataDir + "/substitute").exists()) {
                        FileInputStream lastFile = new FileInputStream(
                                new File(context.getApplicationInfo().dataDir + "/substitute"));
                        byte[] buffer = new byte[lastFile.available()];
                        //noinspection ResultOfMethodCallIgnored
                        lastFile.read(buffer);
                        lastFile.close();
                        lastContent = new String(buffer, "UTF-8");
                    }

                    PrintWriter out = new PrintWriter(context.getApplicationInfo().dataDir + "/substitute");
                    StringBuilder newContent = new StringBuilder();
                    for (String day : days) {
                        newContent.append(day.replaceAll("\n", "\\n"));
                        newContent.append('\n');
                    }
                    String newString = newContent.toString();
                    out.print(newString);
                    out.close();
                    result.updated = !newString.equals(lastContent);
                } catch (Exception e) {
                    e.printStackTrace();
                }


                final ArrayList<String> lastUserSubstitutes = loadLastUserSubstitutes(context);
                saveLastUserSubstitutes(context, substitutesList);
                for (Iterator<String> it = substitutesList.iterator(); it.hasNext(); ) {
                    String s1 = it.next();
                    for (String s2 : lastUserSubstitutes) {
                        if (s1.equals(s2)) {
                            it.remove();
                            break;
                        }
                    }
                }
                result.newCount = substitutesList.size();

                try {
                    html = LessonPlanManager.getHtml("http://sp2dobczyce.pl/kontakt/");
                    searched = "<td style=\"width: 775px; background-color: #1eb0d9; text-align: center;\"><span style=\"font-size: 24pt;\"><strong>";
                    html = html.substring(html.indexOf(searched) + searched.length(), html.indexOf("</strong></span></td>"));

                    if (html.contains("brak")) {
                        Settings.luckyNumber1 = -1;
                        Settings.luckyNumber2 = -1;
                        result.hasChangedLuckyNumbers = false;
                    } else {
                        StringBuilder builder = new StringBuilder();
                        for (char c : html.toCharArray()) {
                            if (c != ' ')
                                builder.append(c);
                        }
                        String[] numbers = builder.toString().split(",");
                        int number1 = Integer.valueOf(numbers[0]);
                        int number2 = Integer.valueOf(numbers[1]);

                        result.hasChangedLuckyNumbers = Settings.luckyNumber1 != number1 || number2 != Settings.luckyNumber2;
                        result.updated = result.updated || result.hasChangedLuckyNumbers;

                        Settings.luckyNumber1 = number1;
                        Settings.luckyNumber2 = number2;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Settings.luckyNumber1 = -1;
                    Settings.luckyNumber2 = -1;
                    result.hasChangedLuckyNumbers = false;
                }
                result.success = true;
                Settings.updateDate = System.currentTimeMillis();

            } catch (Exception e) {
                e.printStackTrace();
                result.success = false;
            }

            Settings.saveSettings(context);
            return result;
        }
    }

    @Nullable
    private static String normalizeSection(String str) {
        StringBuilder builder = new StringBuilder();
        final char[] chars = str.replaceAll("<br {2}/>", "<br />").replaceAll("\t- ", "-").replaceAll("\t", " ").toCharArray();
        boolean isSpace = false;
        boolean wasText = false;
        for (char c : chars) {
            if (c == ' ') {
                if (!isSpace) {
                    builder.append(' ');
                    isSpace = true;
                }
            } else {
                wasText = true;
                isSpace = false;
            }

            builder.append(c);
        }

        return wasText ? builder.toString() : null;
    }

    private static void saveLastUserSubstitutes(Context context, ArrayList<String> list) {
        try {
            PrintWriter out = new PrintWriter(context.getApplicationInfo().dataDir + "/userSubstitute");
            for (String s : list) {
                out.println(s);
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ArrayList<String> loadLastUserSubstitutes(Context context) {
        ArrayList<String> list = new ArrayList<>();
        try {
            File file = new File(context.getApplicationInfo().dataDir + "/userSubstitute");
            Scanner in = new Scanner(file);

            while (in.hasNextLine()) {
                list.add(in.nextLine());
            }

            in.close();
        } catch (FileNotFoundException e) {
            //empty
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    static boolean containsSubstituteForUser(String line) {
        try {
            if (line.contains("wycieczka")
                    || line.contains("wyjazd")
                    || line.contains("rekolekcje")
                    || line.contains("pielgrzymka"))
                return false;

            line = line.toLowerCase();

            if (Settings.isTeacher) {
                String searchString = Settings.getTeacherName().toLowerCase();

                if (line.contains(searchString) && (Character.isDigit(line.charAt(0)) || line.length() > 50))
                    return true;
            } else if (Settings.isClassSelected()) {
                String searchString = Settings.className.toLowerCase();
                final char classChar1 = searchString.charAt(0);
                final char classChar2 = searchString.charAt(searchString.length() - 1);
                if (searchString.length() == 4)
                    searchString = new String(new char[]{classChar1, classChar2});

                int pos = line.indexOf(searchString);
                if (pos != -1) {
                    int pos2 = line.indexOf(" za ");
                    if (pos2 == -1 || pos2 > pos) {
                        return true;
                    }
                } else if (line.indexOf(classChar1) != -1 &&
                        line.indexOf(classChar1) + 3 == line.indexOf(classChar2) &&
                        line.charAt(line.indexOf(classChar2) + 1) == '-') {
                    return true;//wf np: "3b-e coś"
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean containsFormallyClothesRequirement(String section) {
        return section.contains("strój apelowy");
    }

    static String getNewsUpdateInfo(int count) {
        if (count == 0)
            return "Brak nowych zastępstw dla ciebie";
        if (count == 1)
            return "Jedno nowe zastępstwo dla ciebie";
        if (count < 5)
            return "Dostępne są " + String.valueOf(count) + " nowe zastępstwa";

        return "Dostępne jest " + String.valueOf(count) + " nowych zastępstw";
    }

    static String getUpdateInfo(int count) {
        if (count == 0)
            return "Brak zastępstw dla ciebie";
        if (count == 1)
            return "Jedno zastępstwo dla ciebie";
        if (count < 5)
            return "Dostępne są " + String.valueOf(count) + " zastępstwa";

        return "Dostępne jest " + String.valueOf(count) + " zastępstw";
    }

    static class Result {
        boolean success = false;
        boolean updated = true;
        boolean isFromFile = false;
        int newCount = 0;
        int allNewsCount = 0;
        boolean hasChangedLuckyNumbers = false;
        List<String> days = new ArrayList<>();

        boolean areNewsForUser() {
            return newCount != 0 || (hasChangedLuckyNumbers && Settings.isUserLuckyNumber());
        }

        void createViews(Context context, LinearLayout parent, int size) {
            boolean darkMode = Settings.applyNowDarkTheme();
            if (isNoSubstitute())
                Section.createNoSubstituteLayout(context, parent);
            else
                for (String day : days) {
                Section.createSection(context, day, size, parent, darkMode);
            }
            parent.addView(Section.createSeparator(context));
        }

        boolean isNoSubstitute() {
            if (days.isEmpty())
                return true;
            if (days.size() != 1)
                return false;
            StringBuilder temp = new StringBuilder(days.get(0).length());
            for (char c : days.get(0).toCharArray()) {
                if (Character.isLetter(c))
                    temp.append(c);
            }
            return (temp.toString().equalsIgnoreCase("brakzastępstw"));
        }

        void createLuckyNumberView(Context context, LinearLayout parent, int size) {
            if (Settings.luckyNumber1 <= 0 || Settings.luckyNumber2 <= 0)
                return;
            boolean darkMode = Settings.applyNowDarkTheme();
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (size * 0.96f), LinearLayout.LayoutParams.WRAP_CONTENT);
            int margin = (int) context.getResources().getDimension(R.dimen.sectionMargin);
            params.setMargins(0, margin, 0, margin);
            layout.setLayoutParams(params);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                layout.setElevation(margin * 3 / 5);
            layout.setBackgroundResource(darkMode ? R.drawable.section_background_dark : R.drawable.section_background);

            TextView textView = new TextView(context);
            textView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            textView.setMaxLines(2);
            textView.setPadding(margin, margin, margin, margin);
            //noinspection deprecation
            textView.setText(Html.fromHtml(
                    String.format("Szczęśliwe numerki to <b>%s</b> i <b>%s</b>", Settings.luckyNumber1, Settings.luckyNumber2)));
            layout.addView(textView);
            parent.addView(layout);
        }
    }
}
