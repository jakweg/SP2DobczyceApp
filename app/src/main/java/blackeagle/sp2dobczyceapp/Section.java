package blackeagle.sp2dobczyceapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.Nullable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Calendar;

class Section {
    private int maxHeight;
    private boolean isAnimating = false;
    private boolean isShown = true;

    private Section() {
    }

    @SuppressLint("SetTextI18n")
    static Section createSection(Context context, String section, int size, final LinearLayout layout, boolean darkTheme) {
        final Section thisSection = new Section();

        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        final View returnValue = inflater.inflate(R.layout.section, null);
        //returnValue.setLayoutParams(new LinearLayout.LayoutParams(
        //        (int)((float)size.x * 0.93f), LinearLayout.LayoutParams.WRAP_CONTENT));
        returnValue.setLayoutParams(new RelativeLayout.LayoutParams(
                (int) (size * 0.96f), ViewGroup.LayoutParams.WRAP_CONTENT));

        //returnValue.setBackgroundColor(Settings.getColor(context,
        //        darkTheme ? R.color.sectionBackgroundDark : R.color.sectionBackground));
        returnValue.setBackgroundResource(darkTheme ? R.drawable.section_background_dark : R.drawable.section_background);
        returnValue.measure(0, 0);

        StringBuilder builder = new StringBuilder();
        int count = 0;
        String[] lines = section.split("<br  />");
        if (lines.length == 1) {
            lines = section.split("<br />");
        }
        for (String line : lines) {
            if (UpdateManager.containsSubstituteForUser(line)) {
                builder.append("<b>");
                builder.append(line);
                builder.append("</b>");
                count++;
            } else {
                builder.append(line);
            }
            builder.append("<br/>");
        }

        String date = getDate(lines.length > 0 ? lines[0] : null);
        String dayName = lines.length > 1 ? lines[1] : "";

        final TextView titleView = (TextView) returnValue.findViewById(R.id.title);

        if (isDayName(dayName) && date != null) {
            titleView.setText(
                    String.format("%s (%s)\n%s",
                            dayName, date, UpdateManager.getUpdateInfo(count)));
        } else {
            titleView.setText("Informacja\n" + UpdateManager.getUpdateInfo(count));
        }

        @SuppressWarnings("deprecation")
        CharSequence content = Html.fromHtml(builder.toString());

        final TextView contentView = (TextView) returnValue.findViewById(R.id.content);
        contentView.setText(content);
        titleView.measure(0, 0);
        contentView.measure(0, 0);

        thisSection.isShown = count > 0;
        contentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                contentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                thisSection.maxHeight = contentView.getHeight();
                contentView.setHeight(thisSection.isShown ? thisSection.maxHeight : 0);
            }
        });

        final ImageView image = (ImageView) returnValue.findViewById(R.id.image);
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (thisSection.isAnimating)
                    return;
                Animation animation;
                if (thisSection.isShown) {
                    thisSection.isShown = false;
                    animation = new Animation() {
                        @Override
                        protected void applyTransformation(float interpolatedTime, Transformation t) {
                            contentView.setHeight((int) ((1.f - interpolatedTime) * thisSection.maxHeight));
                            image.setRotation(180.f - interpolatedTime * 180.f);
                            contentView.requestLayout();
                        }

                        @Override
                        public boolean willChangeBounds() {
                            return true;
                        }
                    };
                } else {
                    thisSection.isShown = true;
                    animation = new Animation() {
                        @Override
                        protected void applyTransformation(float interpolatedTime, Transformation t) {
                            contentView.setHeight((int) (interpolatedTime * thisSection.maxHeight));
                            image.setRotation(interpolatedTime * 180.f);
                            contentView.requestLayout();
                        }

                        @Override
                        public boolean willChangeBounds() {
                            return true;
                        }
                    };
                }
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        thisSection.isAnimating = true;
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        thisSection.isAnimating = false;
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                animation.setDuration(400);
                layout.startAnimation(animation);
            }
        };

        image.setImageDrawable(Settings.getDyedDrawable(context, R.drawable.ic_expand, darkTheme));
        image.setRotation(thisSection.isShown ? 180.f : 0);
        image.setOnClickListener(clickListener);
        titleView.setOnClickListener(clickListener);

        layout.addView(returnValue);
        layout.addView(createSeparator(context));

        return thisSection;
    }

    @Nullable
    private static String getDate(String date) {
        try {
            date = date.substring(0, date.length() - 2);
            int day = Integer.valueOf(date.substring(0, date.indexOf('.')));
            int month = Integer.valueOf(date.substring(date.indexOf('.') + 1, date.lastIndexOf('.')));
            int year = Integer.valueOf(date.substring(date.lastIndexOf('.') + 1));


            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month - 1, day);
            final int dayOffset = calendar.get(Calendar.DAY_OF_YEAR) - Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            switch (dayOffset) {
                case 0:
                    return "dzisiaj";
                case 1:
                    return "jutro";
                case 2:
                    return "pojutrze";
                case -1:
                    return "wczoraj";
                case -2:
                    return "przedwczoraj";
                default:
                    //return String.format("%s.%s.%sr.", day, month, year);
                    return date;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isDayName(String day) {
        day = day.toLowerCase();
        return day.equals("poniedziałek")
                || day.equals("wtorek")
                || day.equals("środa")
                || day.equals("czwartek")
                || day.equals("piątek");
    }

    static View createSeparator(Context context) {
        View view = new View(context);
        //noinspection deprecation
        view.setBackgroundColor(context.getResources().getColor(R.color.separatorColor));
        view.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) context.getResources().getDimension(R.dimen.separator_height)));

        return view;
    }
}
