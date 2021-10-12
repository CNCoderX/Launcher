package com.cncoderx.launcher.module.workspace;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.widget.TableLayout;

import com.cncoderx.launcher.utils.Utils;

public class AppWidgetTable extends TableLayout {

    public AppWidgetTable(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setStretchAllColumns(true);
        setDividerDrawable(createDividerDrawable());
        setShowDividers(SHOW_DIVIDER_BEGINNING | SHOW_DIVIDER_MIDDLE | SHOW_DIVIDER_END);
        test();
    }

    private Drawable createDividerDrawable() {
        int size = Utils.dip2px(getContext(), 10);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setSize(size, size);
        return drawable;
    }

    private void test() {

    }

    public void addAppWidget() {
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    public void addAppWidget(AppWidget appWidget) {

    }

    public static class Properties {
        int screen;
        int index;
        String title;
        Drawable icon;
        int background;
        String packageName;
        String className;
    }
}
