package blackeagle.sp2dobczyceapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

public class InsertLessonDataActivity extends Activity {

    Switch groupSwitch;
    EditText nameView1;
    EditText nameView2;
    EditText teacherView1;
    EditText teacherView2;
    EditText classroomView1;
    EditText classroomView2;

    int thisDay, thisLesson;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Settings.applyNowDarkTheme() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            setTheme(R.style.DarkDialogTheme);

        setContentView(R.layout.insert_data_layout);

        Bundle extras = getIntent().getExtras();

        LessonPlanManager.Lesson lesson = (LessonPlanManager.Lesson) extras.getSerializable("lesson");
        thisDay = extras.getInt("day", -1);
        thisLesson = extras.getInt("lessonNumber", -1);
        if (lesson == null || thisDay == -1 || thisLesson == -1){
            finish();
            return;
        }

        groupSwitch = (Switch) findViewById(R.id.group_lesson_switch);
        groupSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                findViewById(R.id.group_2_layout).setVisibility(isChecked ? View.VISIBLE : View.GONE);
                findViewById(R.id.group_info).setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        groupSwitch.setChecked(lesson.isGroupLesson);

        nameView1 = ((EditText) findViewById(R.id.textViewName1));
        nameView2 = ((EditText) findViewById(R.id.textViewName2));
        teacherView1 = ((EditText) findViewById(R.id.textViewTeacher1));
        teacherView2 = ((EditText) findViewById(R.id.textViewTeacher2));
        classroomView1 = ((EditText) findViewById(R.id.textViewClassroom1));
        classroomView2 = ((EditText) findViewById(R.id.textViewClassroom2));

        if (lesson.isGroupLesson){
            nameView1.setText(lesson.groupName[0]);
            nameView2.setText(lesson.groupName[1]);
            teacherView1.setText(lesson.groupTeacher[0]);
            teacherView2.setText(lesson.groupTeacher[1]);
            classroomView1.setText(lesson.groupClassroom[0]);
            classroomView2.setText(lesson.groupClassroom[1]);
        } else {
            nameView1.setText(lesson.name);
            teacherView1.setText(lesson.teacher);
            classroomView1.setText(lesson.classroom);
        }

        disableOrientationChanges();

    }

    private void disableOrientationChanges(){
        int deviceRotation = getWindowManager().getDefaultDisplay().getRotation();

        if(deviceRotation == Surface.ROTATION_0)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else if(deviceRotation == Surface.ROTATION_180)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        else if(deviceRotation == Surface.ROTATION_90)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else if(deviceRotation == Surface.ROTATION_270)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    }

    public void onSaveClick(View view) {
        LessonPlanManager.Lesson lesson = new LessonPlanManager.Lesson();
        lesson.isEmpty = false;
        if (groupSwitch.isChecked()){
            lesson.isGroupLesson = true;
            lesson.groupName[0] = nameView1.getText().toString();
            lesson.groupName[1] = nameView2.getText().toString();
            lesson.groupTeacher[0] = teacherView1.getText().toString();
            lesson.groupTeacher[1] = teacherView2.getText().toString();
            lesson.groupClassroom[0] = classroomView1.getText().toString();
            lesson.groupClassroom[1] = classroomView2.getText().toString();
        } else {
            lesson.isGroupLesson = false;
            lesson.name = nameView1.getText().toString();
            lesson.teacher = teacherView1.getText().toString();
            lesson.classroom = classroomView1.getText().toString();
        }
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable("lesson", lesson);
        bundle.putInt("day", thisDay);
        bundle.putInt("lessonNumber", thisLesson);
        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
        finish();
    }
}
