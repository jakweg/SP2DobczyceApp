package blackeagle.sp2dobczyceapp;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Scanner;

@SuppressWarnings("WeakerAccess")
abstract class LessonPlanManager {

    static ArrayList<String> classesList = new ArrayList<>();
    static ArrayList<String> teacherList = new ArrayList<>();

    static void loadClassesData(Context context) {
        try {
            File file = new File(context.getApplicationInfo().dataDir + "/classesList");
            Scanner in = new Scanner(file);

            int count = Integer.valueOf(in.nextLine());
            classesList = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                classesList.add(in.nextLine());
            }

            count = Integer.valueOf(in.nextLine());
            teacherList = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                teacherList.add(in.nextLine());
            }

            in.close();
        } catch (Exception e) {
            //empty
        }
    }

    static void saveClassesData(Context context) {
        try {
            PrintWriter out = new PrintWriter(context.getApplicationInfo().dataDir + "/classesList");
            out.println(classesList.size());
            for (String mClass : classesList) {
                out.println(mClass);
            }
            out.println(teacherList.size());
            for (String mClass : teacherList) {
                out.println(mClass);
            }
            out.close();
        } catch (Exception e) {
            //empty
        }
    }

    static ArrayList<String> getVisibleText(String html) {
        ArrayList<String> list = new ArrayList<>();

        boolean isText = true;
        StringBuilder builder = new StringBuilder();
        for (char c : html.replaceAll("<br />", "\n").toCharArray()) {
            if (c == '<') {
                if (builder.length() != 0) {
                    String str = builder.toString();
                    if (!isEmptyOrSpace(str))
                        list.add(str);
                    builder.setLength(0);
                }
                isText = false;
            } else if (c == '>')
                isText = true;
            else if (isText)
                builder.append(c);
        }

        if (isText && builder.length() != 0) {
            String str = builder.toString();
            if (!isEmptyOrSpace(str))
                list.add(str);
            builder.setLength(0);
        }
        return list;
    }

    static boolean isEmptyOrSpace(String builder) {
        for (char ch : builder.toCharArray())
            if (!Character.isWhitespace(ch))
                return false;
        return true;
    }

    @Nullable
    static LessonPlan getPlan(String path) {
        return LessonPlan.loadFromFile(path);
    }

    static boolean downloadLists() {
        try {
            String html = getHtml("http://sp2dobczyce.pl/planlekcji/lista.html");

            String searched = "<tr><td></td><td><div class=\"blk\" id=\"oddzialy\">";
            String classesHtml = html.substring(html.indexOf(searched) + searched.length(),
                    html.indexOf("</div></td></tr>"));

            classesList = new ArrayList<>();
            for (String s : classesHtml.split("</a></p>")) {
                classesList.add(s.substring(s.lastIndexOf('>') + 1));
            }

            html = html.substring(html.indexOf("nauczyciele"));

            searched = "<tr><td></td><td><div class=\"nblk\" id=\"nauczyciele\">";
            String teachersHtml = html.substring(html.indexOf(searched) + searched.length(),
                    html.indexOf("</div></td></tr>"));

            teacherList = new ArrayList<>();
            for (String s : teachersHtml.split("</a></p>")) {
                teacherList.add(s.substring(s.lastIndexOf('>') + 1));
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean downloadAllPlans(Context context) {
        try {
            String path = context.getApplicationInfo().dataDir + "/";
            File planDir = new File(path + "plans");
            if (!planDir.exists() && !planDir.mkdir())
                throw new IOException("plan dir failed");

            String html = getHtml("http://sp2dobczyce.pl/planlekcji/lista.html");

            String searched = "<tr><td></td><td><div class=\"blk\" id=\"oddzialy\">";
            String classesHtml = html.substring(html.indexOf(searched) + searched.length(),
                    html.indexOf("</div></td></tr>"));

            for (String s : classesHtml.split("</a></p>")) {
                LessonPlan plan = LessonPlan.downloadPlan(s);
                plan.saveToFile(path + "plans/" + plan.name);
            }

            html = html.substring(html.indexOf("nauczyciele"));

            searched = "<tr><td></td><td><div class=\"nblk\" id=\"nauczyciele\">";
            String teachersHtml = html.substring(html.indexOf(searched) + searched.length(),
                    html.indexOf("</div></td></tr>"));


            for (String s : teachersHtml.split("</a></p>")) {
                LessonPlan plan = LessonPlan.downloadPlan(s);
                plan.saveToFile(path + "plans/" + plan.name);
            }


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    @Nullable
    static LessonPlan getDefaultPlan(Context context) {
        if (!Settings.isClassSelected())
            return null;
        return getPlan(context.getApplicationInfo().dataDir
                + "/plans/"
                + Settings.className);
    }

    static int[] getLessonsCount(Context context) {
        LessonPlan plan = getDefaultPlan(context);
        if (plan == null)
            return new int[]{8, 8, 8, 8, 8};

        int[] ints = {8, 8, 8, 8, 8};

        for (int i = 0; i < 5; i++) {

            for (int j = 8 - 1; j >= 0; j--) {
                if (!isEmptyOrSpace(plan.getDisplayedLesson(i, j, LessonPlan.RULE_SHOW_SUBJECT))) {
                    ints[i] = j + 1;
                    break;
                }
            }
        }

        return ints;
    }

    static String getHtml(String url) throws IOException {
        // Build and set timeout values for the request.
        URLConnection connection = (new URL(url)).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.connect();

        // Read and store the result line by line then return the entire string.
        InputStream in = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder html = new StringBuilder();
        for (String line; (line = reader.readLine()) != null; ) {
            html.append(line);
        }
        in.close();

        return html.toString();
    }

    static class Lesson implements Serializable {
        String name = "";
        String[] groupName = new String[2];
        String teacher = "";
        String[] groupTeacher = new String[2];
        String classroom = "";
        String[] groupClassroom = new String[2];

        boolean isGroupLesson;
        boolean isEmpty = false;

        /*

        private static String prepareString(String str){
            if(str == null)
                return "";
            return str.replaceAll(" ", "%") + " ";
        }
        private static String getPreparedString(String str){
            return str.replaceAll("%", " ");
        }

       public void saveToFile(PrintWriter out){
            if (isEmpty){
                out.print("0 ");
            } else{
                out.print("1 ");
                if(isGroupLesson){
                    out.print("1 ");
                    out.print(prepareString(groupName[0]));
                    out.print(prepareString(groupName[1]));
                    out.print(prepareString(groupTeacher[0]));
                    out.print(prepareString(groupTeacher[1]));
                    out.print(prepareString(groupClassroom[0]));
                    out.print(prepareString(groupClassroom[1]));
                } else {
                    out.print("0 ");
                    out.print(prepareString(name));
                    out.print(prepareString(teacher));
                    out.print(prepareString(classroom));
                }
            }
        }

        public static Lesson loadFromFile(Scanner in){
            Lesson lesson = new Lesson();
            try{
                String str = in.next();
                if(str.equals("NULL"))
                    return null;
                if(str.equals("0")){
                    lesson.isEmpty = true;
                } else {
                    if(in.next().equals("1")){
                        lesson.isGroupLesson = true;
                        lesson.groupName[0] = getPreparedString(in.next());
                        lesson.groupName[1] = getPreparedString(in.next());
                        lesson.groupTeacher[0] = getPreparedString(in.next());
                        lesson.groupTeacher[1] = getPreparedString(in.next());
                        lesson.groupClassroom[0] = getPreparedString(in.next());
                        lesson.groupClassroom[1] = getPreparedString(in.next());
                    } else {
                        lesson.name = getPreparedString(in.next());
                        lesson.teacher = getPreparedString(in.next());
                        lesson.classroom = getPreparedString(in.next());
                    }
                }
            } catch(Exception e){
                lesson.isEmpty = true;
            }
            return lesson;
        }
        */
        @Override
        public String toString() {
            if (isEmpty)
                return "empty";
            else if (isGroupLesson)
                return groupName[0] + " z " + groupTeacher[0] + " w " + groupClassroom[0] + " oraz "
                        + groupName[1] + " z " + groupTeacher[1] + " w " + groupClassroom[1];
            else
                return name + " z " + teacher + " w " + classroom;
        }
    }

    static class LessonPlan implements Serializable {
        public static final int RULE_SHOW_TEACHER = 1;
        public static final int RULE_SHOW_CLASSROOM = 2;
        public static final int RULE_SHOW_SUBJECT = 4;

        private String name;
        private String link;
        private boolean isTeacher;
        private Lesson[][] plan = new Lesson[5][8];//dzień, lekcja

        private LessonPlan() {
        }

        public static LessonPlan downloadPlan(String line) throws Exception {
            try {
                LessonPlan thisPlan = new LessonPlan();
                thisPlan.name = line.substring(line.lastIndexOf('>') + 1);
                String searched = "<a href=\"";
                thisPlan.link = "http://sp2dobczyce.pl/planlekcji/" + line.substring(line.indexOf(searched) + searched.length(),
                        line.indexOf("\" target="));

                thisPlan.isTeacher = thisPlan.name.contains(")");

                String html = getHtml(thisPlan.link);
                String[] tableRows = html.split("<tr>");
                if (thisPlan.isTeacher) {
                    for (int i = 0; i < 8; i++) {
                        if (tableRows[i + 4].contains("left"))
                            break;
                        thisPlan.loadTeacherLessonRow(tableRows[i + 4], i);
                    }
                } else {
                    for (int i = 0; i < 8; i++) {
                        if (tableRows[i + 4].contains("left"))
                            break;
                        thisPlan.loadStudentLessonRow(tableRows[i + 4], i);
                    }
                }

                return thisPlan;
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }

        public static LessonPlan loadFromFile(String path) {
            LessonPlan plan = null;
            try {
                ObjectInputStream ois =
                        new ObjectInputStream(new FileInputStream(path));

                plan = (LessonPlan) ois.readObject();
            } catch (InvalidClassException e) {
                Log.e("SP2DOBCZYCEAPP", "Lesson plan is too old - make update");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return plan;
        }

        public String getInitials() {
            try {
                if (isTeacher)
                    return name.substring(name.lastIndexOf("(") + 1, name.lastIndexOf(")"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return name;
        }

        String getLink() {
            return link;
        }

        boolean isTeacherPlan() {
            return isTeacher;
        }

        String getName() {
            return name;
        }

        public boolean isEmpty(int day, int lesson) {
            Lesson l = getLesson(day, lesson);
            return l == null || l.isEmpty;
        }

        public String getDisplayedLesson(int day, int lesson, int rules) {
            Lesson l = getLesson(day, lesson);
            /*if (l == null || l.isEmpty) {
                if ((rules & RULE_SHOW_ANYTHING_WHEN_EMPTY) != 0)
                    return "<pusto>";
                else
                    return "";
            }*/
            if (l == null || l.isEmpty)
                return " ";
            StringBuilder builder = new StringBuilder();
            if (l.isGroupLesson) {
                if ((rules & RULE_SHOW_SUBJECT) != 0) {
                    builder.append(l.groupName[0]);
                    builder.append(' ');
                }
                if ((rules & RULE_SHOW_TEACHER) != 0) {
                    builder.append(l.groupTeacher[0]);
                    builder.append(' ');
                }
                if ((rules & RULE_SHOW_CLASSROOM) != 0) {
                    builder.append(l.groupClassroom[0]);
                }
                builder.append('\n');

                if ((rules & RULE_SHOW_SUBJECT) != 0) {
                    builder.append(l.groupName[1]);
                    builder.append(' ');
                }
                if ((rules & RULE_SHOW_TEACHER) != 0) {
                    builder.append(l.groupTeacher[1]);
                    builder.append(' ');
                }
                if ((rules & RULE_SHOW_CLASSROOM) != 0)
                    builder.append(l.groupClassroom[1]);
            } else {
                if ((rules & RULE_SHOW_SUBJECT) != 0) {
                    builder.append(l.name);
                    builder.append(' ');
                }
                if ((rules & RULE_SHOW_TEACHER) != 0) {
                    builder.append(l.teacher);
                    builder.append(' ');
                }
                if ((rules & RULE_SHOW_CLASSROOM) != 0)
                    builder.append(l.classroom);
            }
            return builder.toString();
        }

        public void setLesson(int day, int lesson, @Nullable Lesson l) {
            plan[day][lesson] = l;
        }

        @Nullable
        public Lesson getLesson(int day, int lesson) {
            try {
                return plan[day][lesson];
            } catch (Exception e) {
                return null;
            }
        }

        private void loadTeacherLessonRow(String data, int lessonNumber) throws Exception {
            String[] tableRows = data.split("</td>");
            for (int i = 0; i < 5; i++) {
                plan[i][lessonNumber] = loadTeacherLesson(tableRows[i + 2]);
            }
        }

        private void loadStudentLessonRow(String data, int lessonNumber) throws Exception {
            String[] tableRows = data.split("</td>");
            for (int i = 0; i < 5; i++) {
                plan[i][lessonNumber] = loadStudentLesson(tableRows[i + 2]);
            }
        }

        private Lesson loadTeacherLesson(String line) throws Exception {
            Lesson lesson = new Lesson();
            ArrayList<String> visibleText = getVisibleText(line);
            try {

                if (line.contains("&nbsp;")) {
                    lesson.isEmpty = true;
                } else if (line.contains("NI")) {
                    lesson.name = "NI";
                    lesson.teacher = "";
                    lesson.classroom = "";
                } else if (visibleText.size() == 1) {
                    lesson.name = visibleText.get(0);
                    lesson.teacher = "";
                    lesson.classroom = "";
                } else if (line.contains("wf")) {
                    if (visibleText.size() > 2) {
                        StringBuilder builder = new StringBuilder();
                        for (int i = 0; i < visibleText.size() - 2; i++) {
                            builder.append(visibleText.get(i));
                        }
                        lesson.teacher = builder.toString();
                        lesson.classroom = visibleText.get(visibleText.size() - 1);
                        lesson.name = visibleText.get(visibleText.size() - 2);
                    } else {
                        lesson.name = "wf";
                        lesson.teacher = "";
                        lesson.classroom = "";
                    }
                } else if (visibleText.size() == 4) {
                    lesson.name = visibleText.get(2);
                    lesson.teacher = visibleText.get(0) + visibleText.get(1);
                    lesson.classroom = visibleText.get(3);
                } else {
                    lesson.name = visibleText.get(1);
                    lesson.teacher = visibleText.get(0);
                    lesson.classroom = visibleText.get(2);
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(lesson);
            }
            return lesson;
        }

        private Lesson loadStudentLesson(String line) throws Exception {
            Lesson lesson = new Lesson();
            ArrayList<String> visibleText = getVisibleText(line);
            try {
                if (line.contains("nbsp;")) {//jezeli wolna
                    lesson.isEmpty = true;
                } else if (line.contains("Basen")) {//basen
                    lesson.name = "Basen";
                    lesson.teacher = "";
                    lesson.classroom = "";
                } else if (line.contains("wych.rodz.") || line.contains("Wych.rodz.")) {
                    lesson.name = visibleText.get(0);
                    lesson.teacher = visibleText.get(1);
                    lesson.classroom = visibleText.get(2);
                } else if (line.contains("zaj.dod.")) {
                    lesson.name = "Zaj. dod.";
                    lesson.teacher = "";
                    lesson.classroom = "";
                } else if (line.contains("Tańce") || line.contains("tańce")) {//jezeli sa tance
                    lesson.name = "Tańce";
                    lesson.teacher = "";
                    lesson.classroom = "";
                } else if (line.contains("wf")) {
                    if (visibleText.size() == 3) {
                        lesson.name = visibleText.get(0);
                        lesson.teacher = visibleText.get(1);
                        lesson.classroom = visibleText.get(2);
                    } else if (visibleText.size() == 6) {
                        lesson.isGroupLesson = true;
                        lesson.groupName[0] = visibleText.get(0);
                        lesson.groupTeacher[0] = visibleText.get(1);
                        lesson.groupClassroom[0] = visibleText.get(2);

                        lesson.groupName[1] = visibleText.get(3);
                        lesson.groupTeacher[1] = visibleText.get(4);
                        lesson.groupClassroom[1] = visibleText.get(5);
                    } else if (visibleText.size() == 7) {
                        lesson.isGroupLesson = true;
                        if (visibleText.get(3).contains("wf")) {
                            lesson.groupName[0] = visibleText.get(0);
                            lesson.groupTeacher[0] = visibleText.get(1);
                            lesson.groupClassroom[0] = visibleText.get(2);

                            if (Settings.containsDigit(visibleText.get(4))) {
                                lesson.groupName[1] = visibleText.get(3);
                                lesson.groupTeacher[1] = "";
                                lesson.groupClassroom[1] = visibleText.get(6);
                            }
                        } else {
                            if (Settings.containsDigit(visibleText.get(5))) {
                                lesson.groupName[1] = visibleText.get(4) + visibleText.get(5);
                                lesson.groupTeacher[1] = "";
                                lesson.groupClassroom[1] = "";

                                lesson.groupName[0] = visibleText.get(0) + visibleText.get(1);
                                lesson.groupTeacher[0] = "";
                                lesson.groupClassroom[0] = visibleText.get(3);
                            } else {
                                lesson.groupName[1] = visibleText.get(4);
                                lesson.groupTeacher[1] = visibleText.get(5);
                                lesson.groupClassroom[1] = visibleText.get(6);

                                lesson.groupName[0] = visibleText.get(0) + visibleText.get(1);
                                lesson.groupTeacher[0] = "";
                                lesson.groupClassroom[0] = visibleText.get(3);
                            }

                            //lesson.groupName[0] = visibleText.get(0);
                            //lesson.groupTeacher[0] = visibleText.get(1);
                            //lesson.groupClassroom[0] = visibleText.get(2);
                        }
                    }/*
                    else {
                        lesson.isGroupLesson = true;
                        if (Settings.containsDigit(visibleText.get(1)))
                            lesson.groupName[0] = visibleText.get(0) + visibleText.get(1);
                        else
                            lesson.groupName[0] = visibleText.get(0);

                        if (visibleText.get(3).contains("wf")){
                            if (Settings.containsDigit(visibleText.get(4)))
                                lesson.groupName[1] = visibleText.get(3) + visibleText.get(4);
                            else
                                lesson.groupName[1] = visibleText.get(3);
                        } else {
                            if (Settings.containsDigit(visibleText.get(5)))
                                lesson.groupName[1] = visibleText.get(4) + visibleText.get(5);
                            else
                                lesson.groupName[1] = visibleText.get(4);
                        }

                        lesson.groupTeacher = new String[] {"",""};
                        lesson.groupClassroom = new String[] {"",""};
                    }*/
                } else if (line.contains("<span style=\"font-size:85%\">")) {//lekcja jest grupowa
                    lesson.isGroupLesson = true;

                    //pierwsza grupa:
                    lesson.groupName[0] = visibleText.get(0);
                    lesson.groupTeacher[0] = visibleText.get(1);
                    lesson.groupClassroom[0] = visibleText.get(2);

                    //i druga grupa:

                    if (visibleText.size() == 6) {//o ile ma druga grupa
                        lesson.groupName[1] = visibleText.get(3);
                        lesson.groupTeacher[1] = visibleText.get(4);
                        lesson.groupClassroom[1] = visibleText.get(5);

                    } else { //jednak tylko jedna ma
                        lesson.isGroupLesson = false;
                        lesson.name = lesson.groupName[0];
                        lesson.teacher = lesson.groupTeacher[0];
                        lesson.classroom = lesson.groupClassroom[0];
                    }
                } else {//zwykla lekcja
                    lesson.name = visibleText.get(0);
                    lesson.teacher = visibleText.get(1);
                    lesson.classroom = visibleText.get(2);
                }
            } catch (Exception e) {
                //System.out.println("ERR");
            }
            //System.out.println(lesson);
            return lesson;
        }

        public void saveToFile(String path) {
            try {
                ObjectOutputStream oos =
                        new ObjectOutputStream(new FileOutputStream(path));
                oos.writeObject(this);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
