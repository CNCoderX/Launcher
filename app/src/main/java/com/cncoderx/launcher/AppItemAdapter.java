package com.cncoderx.launcher;

import android.content.Context;
import android.graphics.Point;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.cncoderx.launcher.utils.AppUtils;
import com.cncoderx.launcher.utils.Utils;
import com.woxthebox.draglistview.DragItemAdapter;

public class AppItemAdapter extends DragItemAdapter<AppItem, DragItemAdapter.ViewHolder> {
    private final int itemHeight;

    public AppItemAdapter(Context context) {
        itemHeight = calcItemHeight(context);
    }

    private int calcItemHeight(Context context) {
        final int space = Utils.dip2px(context, 10);
        Point outSize = new Point();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getSize(outSize);
        return (outSize.y - AppUtils.getStatusBarHeight(context) - space * 4) / 3;
    }

    @Override
    public void onBindViewHolder(DragItemAdapter.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        AppItem item = getItemList().get(position);

        CardView cardView = (CardView) holder.itemView;
        cardView.setCardBackgroundColor(item.getBgColor());

        ImageView iconView = holder.itemView.findViewById(android.R.id.icon);
        iconView.setImageDrawable(item.getAppIcon());

        TextView textView = holder.itemView.findViewById(android.R.id.title);
        textView.setText(item.getAppName());
    }

    @Override
    public long getUniqueItemId(int position) {
        return getItemList().get(position).getId();
    }

    @NonNull
    @Override
    public DragItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater factory = LayoutInflater.from(parent.getContext());
        View view = factory.inflate(R.layout.app_widget, parent, false);
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = itemHeight;
        return new DragItemAdapter.ViewHolder(view, R.id.app_widget_card, true){};
    }
}
