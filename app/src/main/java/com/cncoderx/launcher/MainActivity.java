package com.cncoderx.launcher;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.cncoderx.launcher.module.workspace.CellLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
//    private Workspace workspace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        setContentView(R.layout.activity_main);


        CellLayout cellLayout = findViewById(R.id.cell_layout);
        for (int i = 0; i < 10; i++) {
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(R.drawable.ic_launcher);

            int col = i % cellLayout.getCountX();
            int row = i / cellLayout.getCountX();
            CellLayout.LayoutParams layoutParams = new CellLayout.LayoutParams(col, row, 1, 1);
            cellLayout.addViewToCellLayout(imageView, i, i + 1, layoutParams, true);
        }

//        setContentView(R.layout.app_widget_grid);

//        workspace = findViewById(R.id.workspace);

//        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
//        recyclerView.setAdapter(appItemAdapter, true);

//        recyclerView.setBoardCallback(new BoardView.BoardCallback() {
//            @Override
//            public boolean canDragItemAtPosition(int column, int row) {
//                return false;
//            }
//
//            @Override
//            public boolean canDropItemAtPosition(int oldColumn, int oldRow, int newColumn, int newRow) {
//                return false;
//            }
//        });
//        {
//            AppItemAdapter itemAdapter = new AppItemAdapter(this);
//            loadAppItems(itemAdapter);
//            recyclerView.addColumn(ColumnProperties.Builder.newBuilder(itemAdapter)
//                    .setLayoutManager(new GridLayoutManager(this, 2))
//                    .setHasFixedItemSize(true)
//                    .build());
//        }
//        {
//            AppItemAdapter itemAdapter = new AppItemAdapter(this);
//            loadAppItems(itemAdapter);
//            recyclerView.addColumn(ColumnProperties.Builder.newBuilder(itemAdapter)
//                    .setLayoutManager(new GridLayoutManager(this, 2))
//                    .setHasFixedItemSize(true)
//                    .build());
//        }
    }

    private void loadAppItems(AppItemAdapter itemAdapter) {
        List<AppItem> itemList = new ArrayList<>();
        {
            AppItem appItem = new AppItem();
            appItem.setId(0);
            appItem.setBgColor(0xff60a917);
            appItem.setAppIcon(ContextCompat.getDrawable(this, R.drawable.ic_phone));
            appItem.setAppName("电话");
            itemList.add(appItem);
        }
        {
            AppItem appItem = new AppItem();
            appItem.setId(1);
            appItem.setBgColor(0xfffa6800);
            appItem.setAppIcon(ContextCompat.getDrawable(this, R.drawable.ic_mms));
            appItem.setAppName("短信");
            itemList.add(appItem);
        }
        {
            AppItem appItem = new AppItem();
            appItem.setId(2);
            appItem.setBgColor(0xffaa00ff);
            appItem.setAppIcon(ContextCompat.getDrawable(this, R.drawable.ic_gallery));
            appItem.setAppName("相册");
            itemList.add(appItem);
        }
        {
            AppItem appItem = new AppItem();
            appItem.setId(3);
            appItem.setBgColor(0xff1ba1e2);
            appItem.setAppIcon(ContextCompat.getDrawable(this, R.drawable.ic_camera));
            appItem.setAppName("拍照");
            itemList.add(appItem);
        }
        {
            AppItem appItem = new AppItem();
            appItem.setId(4);
            appItem.setBgColor(0xffaa00ff);
            appItem.setAppIcon(ContextCompat.getDrawable(this, R.drawable.ic_gallery));
            appItem.setAppName("相册");
            itemList.add(appItem);
        }
        {
            AppItem appItem = new AppItem();
            appItem.setId(5);
            appItem.setBgColor(0xff1ba1e2);
            appItem.setAppIcon(ContextCompat.getDrawable(this, R.drawable.ic_camera));
            appItem.setAppName("拍照");
            itemList.add(appItem);
        }
        itemAdapter.setItemList(itemList);
    }
}
