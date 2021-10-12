package com.cncoderx.launcher.module.workspace;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.DragStartHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppWidgetAdapter extends RecyclerView.Adapter {
    private final List<AppWidget> appWidgets = new ArrayList<>();

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return appWidgets.size();
    }
}
