package com.cncoderx.launcher.module.workspace;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.cncoderx.launcher.R;

public class AppWidget extends ViewGroup {
    private CardView cardView;
    private ImageView iconView;
    private TextView titleView;

    public AppWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater factory = LayoutInflater.from(context);
        factory.inflate(R.layout.app_widget, this, true);
        cardView = findViewById(R.id.app_widget_card);
        iconView = findViewById(android.R.id.icon);
        titleView = findViewById(android.R.id.title);

        Properties properties = getProperties(context, attrs);
        cardView.setCardBackgroundColor(properties.background);
        iconView.setImageDrawable(properties.icon);
        titleView.setText(properties.title);
    }

    public static Properties getProperties(Context context, AttributeSet attrs) {
        Properties properties = new Properties();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AppWidget);
        properties.screen = a.getInt(R.styleable.AppWidget_launcher_screen, 0);
        properties.index = a.getInt(R.styleable.AppWidget_launcher_index, 0);
        properties.title = a.getString(R.styleable.AppWidget_launcher_title);
        properties.icon = a.getDrawable(R.styleable.AppWidget_launcher_icon);
        properties.background = a.getColor(R.styleable.AppWidget_launcher_background, 0);
        properties.packageName = a.getString(R.styleable.AppWidget_launcher_packageName);
        properties.className = a.getString(R.styleable.AppWidget_launcher_className);
        a.recycle();
        return properties;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

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
