/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cncoderx.launcher;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Represents an item in the launcher.
 */
class ItemInfo {

    /**
     * Intent extra to store the profile. Format: UserHandle
     */
    static final String EXTRA_PROFILE = "profile";

    static final int NO_ID = -1;

    /**
     * The id in the settings database for this item
     */
    long id = NO_ID;
    
    /**
     * One of {@link LauncherSettings.Shortcuts#ITEM_TYPE_APPLICATION},
     * {@link LauncherSettings.Shortcuts#ITEM_TYPE_SHORTCUT},
     * {@link LauncherSettings.Shortcuts#ITEM_TYPE_FOLDER}, or
     * {@link LauncherSettings.Shortcuts#ITEM_TYPE_APPWIDGET}.
     */
    int itemType;
    
    /**
     * Indicates the screen in which the shortcut appears.
     */
    int screen = -1;
    
    /**
     * Indicates the X position of the associated cell.
     */
    int cellX = -1;

    /**
     * Indicates the Y position of the associated cell.
     */
    int cellY = -1;

    /**
     * Indicates the X cell span.
     */
    int spanX = 1;

    /**
     * Indicates the Y cell span.
     */
    int spanY = 1;

    /**
     * Indicates the minimum X cell span.
     */
    int minSpanX = 1;

    /**
     * Indicates the minimum Y cell span.
     */
    int minSpanY = 1;

    /**
     * Indicates that this item needs to be updated in the db
     */
    boolean requiresDbUpdate = false;

    /**
     * Title of the item
     */
    CharSequence title;

    /**
     * Content description for the item.
     */
    CharSequence contentDescription;

    /**
     * The position of the item in a drag-and-drop operation.
     */
    int[] dropPos = null;

    UserHandle user;

    ItemInfo() {
        user = android.os.Process.myUserHandle();
    }

    ItemInfo(ItemInfo info) {
        id = info.id;
        cellX = info.cellX;
        cellY = info.cellY;
        spanX = info.spanX;
        spanY = info.spanY;
        screen = info.screen;
        itemType = info.itemType;
        user = info.user;
        contentDescription = info.contentDescription;
        // tempdebug:
//        LauncherModel.checkItemInfo(this);
    }

    /** Returns the package name that the intent will resolve to, or an empty string if
     *  none exists. */
    static String getPackageName(Intent intent) {
        if (intent != null) {
            String packageName = intent.getPackage();
            if (packageName == null && intent.getComponent() != null) {
                packageName = intent.getComponent().getPackageName();
            }
            if (packageName != null) {
                return packageName;
            }
        }
        return "";
    }

    protected void updateUser(Intent intent) {
        if (intent != null && intent.hasExtra(EXTRA_PROFILE)) {
            user = (UserHandle) intent.getParcelableExtra(EXTRA_PROFILE);
        }
    }

    /**
     * Write the fields of this item to the DB
     * 
     * @param context A context object to use for getting a UserManager
     *            instance.
     * @param values
     */
    void onAddToDatabase(Context context, ContentValues values) {
        values.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, itemType);
        values.put(LauncherSettings.Shortcuts.SCREEN, screen);
        values.put(LauncherSettings.Shortcuts.CELLX, cellX);
        values.put(LauncherSettings.Shortcuts.CELLY, cellY);
        values.put(LauncherSettings.Shortcuts.SPANX, spanX);
        values.put(LauncherSettings.Shortcuts.SPANY, spanY);
        long serialNumber = ((UserManager) context.getSystemService(Context.USER_SERVICE))
                .getSerialNumberForUser(user);
        values.put(LauncherSettings.Shortcuts.PROFILE_ID, serialNumber);
    }

    void updateValuesWithCoordinates(ContentValues values, int cellX, int cellY) {
        values.put(LauncherSettings.Shortcuts.CELLX, cellX);
        values.put(LauncherSettings.Shortcuts.CELLY, cellY);
    }

    static byte[] flattenBitmap(Bitmap bitmap) {
        // Try go guesstimate how much space the icon will take when serialized
        // to avoid unnecessary allocations/copies during the write.
        int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            Log.w("Favorite", "Could not write icon");
            return null;
        }
    }

    static void writeBitmap(ContentValues values, Bitmap bitmap) {
        if (bitmap != null) {
            byte[] data = flattenBitmap(bitmap);
            values.put(LauncherSettings.Shortcuts.ICON, data);
        }
    }

    /**
     * It is very important that sub-classes implement this if they contain any references
     * to the activity (anything in the view hierarchy etc.). If not, leaks can result since
     * ItemInfo objects persist across rotation and can hence leak by holding stale references
     * to the old view hierarchy / activity.
     */
    void unbind() {
    }

    @Override
    public String toString() {
        return "Item(id=" + this.id + " type=" + this.itemType + " screen=" + screen
                + " cellX=" + cellX + " cellY=" + cellY + " spanX=" + spanX
                + " spanY=" + spanY + " dropPos=" + dropPos + " user=" + user
                + ")";
    }
}
