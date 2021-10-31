package com.cncoderx.launcher;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;

public class LauncherProvider extends ContentProvider {
    private static final String TAG = LauncherProvider.class.getSimpleName();
    private static final boolean LOGD = false;

    private static final String DATABASE_NAME = "launcher.db";

    private static final int DATABASE_VERSION = 1;

    static final String AUTHORITY = "com.cncoderx.launcher.settings";

    static final String TABLE_SHORTCUTS = "shortcuts";
    static final String PARAMETER_NOTIFY = "notify";

    static final String DEFAULT_WORKSPACE_RESOURCE_ID =
            "DEFAULT_WORKSPACE_RESOURCE_ID";

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        ((LauncherApplication) getContext()).setLauncherProvider(this);
        return true;
    }

    @Override
    public String getType(Uri uri) {
        SqlArguments args = new SqlArguments(uri, null, null);
        if (TextUtils.isEmpty(args.where)) {
            return "vnd.android.cursor.dir/" + args.table;
        } else {
            return "vnd.android.cursor.item/" + args.table;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);

        return result;
    }

    private static long dbInsertAndCheck(DatabaseHelper helper,
            SQLiteDatabase db, String table, String nullColumnHack, ContentValues values) {
        if (!values.containsKey(LauncherSettings.Shortcuts._ID)) {
            throw new RuntimeException("Error: attempting to add item without specifying an id");
        }
        return db.insert(table, nullColumnHack, values);
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final long rowId = dbInsertAndCheck(mOpenHelper, db, args.table, null, initialValues);
        if (rowId <= 0) {
            return null;
        }

        uri = ContentUris.withAppendedId(uri, rowId);
        sendNotify(uri);

        return uri;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                if (dbInsertAndCheck(mOpenHelper, db, args.table, null, values[i]) < 0) {
                    return 0;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        sendNotify(uri);
        return values.length;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.delete(args.table, args.where, args.args);
        if (count > 0) {
            sendNotify(uri);
        }

        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);
        if (count > 0) {
            sendNotify(uri);
        }

        return count;
    }

    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter(PARAMETER_NOTIFY);
        if (notify == null || "true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }

    public long generateNewId() {
        return mOpenHelper.generateNewId();
    }

    /**
     * @param origWorkspaceResId that can be 0 to use default or non-zero for specific resource
     */
    synchronized public void loadDefaultWorkspaceIfNecessary(int origWorkspaceResId,
                                                             boolean overridePrevious) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (overridePrevious) {
            int workspaceResId = origWorkspaceResId;

            // Use default workspace resource if none provided
            if (workspaceResId == 0) {
                workspaceResId = sp.getInt(DEFAULT_WORKSPACE_RESOURCE_ID, R.xml.default_workspace);
            }

            // Populate shortcuts table with initial shortcuts
            SharedPreferences.Editor editor = sp.edit();
            if (origWorkspaceResId != 0) {
                editor.putInt(DEFAULT_WORKSPACE_RESOURCE_ID, origWorkspaceResId);
            }
            if (overridePrevious) {
                if (LOGD) {
                    Log.d(TAG, "Clearing old launcher database");
                }
                // Workspace has already been loaded, clear the database.
                deleteDatabase();
            }
            mOpenHelper.loadWorkspace(mOpenHelper.getWritableDatabase(), workspaceResId);
            editor.commit();
        }
    }

    public void deleteDatabase() {
        // Are you sure? (y/n)
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final File dbFile = new File(db.getPath());
        mOpenHelper.close();
        if (dbFile.exists()) {
            SQLiteDatabase.deleteDatabase(dbFile);
        }
        mOpenHelper = new DatabaseHelper(getContext());
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String TAG_WORKSPACE = "workspace";
        private static final String TAG_SHORTCUT = "shortcut";
//        private static final String TAG_CLOCK = "clock";
//        private static final String TAG_SEARCH = "search";
//        private static final String TAG_APPWIDGET = "appwidget";
//        private static final String TAG_SHORTCUT = "shortcut";
//        private static final String TAG_FOLDER = "folder";
//        private static final String TAG_EXTRA = "extra";

        private final Context mContext;
        private long mMaxId = -1;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;

            // In the case where neither onCreate nor onUpgrade gets called, we read the maxId from
            // the DB here
            if (mMaxId == -1) {
                mMaxId = initializeMaxId(getWritableDatabase());
            }
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            if (LOGD) {
                Log.d(TAG, "creating new launcher database");
            }

            mMaxId = 1;
            final UserManager um = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
            // Default profileId to the serial number of this user.
            long userSerialNumber = um.getSerialNumberForUser(
                    android.os.Process.myUserHandle());

            db.execSQL("CREATE TABLE " + TABLE_SHORTCUTS + " (" +
                    LauncherSettings.Shortcuts._ID + " INTEGER PRIMARY KEY," +
                    LauncherSettings.Shortcuts.TITLE + " TEXT," +
                    LauncherSettings.Shortcuts.INTENT + " TEXT," +
                    LauncherSettings.Shortcuts.SCREEN + " INTEGER," +
                    LauncherSettings.Shortcuts.CELLX + " INTEGER," +
                    LauncherSettings.Shortcuts.CELLY + " INTEGER," +
                    LauncherSettings.Shortcuts.SPANX + " INTEGER," +
                    LauncherSettings.Shortcuts.SPANY + " INTEGER," +
                    LauncherSettings.Shortcuts.ITEM_TYPE + " INTEGER," +
                    LauncherSettings.Shortcuts.ICON_TYPE + " INTEGER," +
                    LauncherSettings.Shortcuts.ICON_PACKAGE + " TEXT," +
                    LauncherSettings.Shortcuts.ICON_RESOURCE + " TEXT," +
                    LauncherSettings.Shortcuts.ICON + " BLOB," +
                    LauncherSettings.Shortcuts.URI + " TEXT," +
                    LauncherSettings.Shortcuts.DISPLAY_MODE + " INTEGER," +
                    LauncherSettings.Shortcuts.PROFILE_ID + " INTEGER DEFAULT " + userSerialNumber +
                    ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (LOGD) {
                Log.d(TAG, "onUpgrade triggered");
            }
        }

        // Generates a new ID to use for an object in your database. This method should be only
        // called from the main UI thread. As an exception, we do call it when we call the
        // constructor from the worker thread; however, this doesn't extend until after the
        // constructor is called, and we only pass a reference to LauncherProvider to LauncherApp
        // after that point
        public long generateNewId() {
            if (mMaxId < 0) {
                throw new RuntimeException("Error: max id was not initialized");
            }
            mMaxId += 1;
            return mMaxId;
        }

        private long initializeMaxId(SQLiteDatabase db) {
            Cursor c = db.rawQuery("SELECT MAX(_id) FROM " + TABLE_SHORTCUTS, null);

            // get the result
            final int maxIdIndex = 0;
            long id = -1;
            if (c != null && c.moveToNext()) {
                id = c.getLong(maxIdIndex);
            }
            if (c != null) {
                c.close();
            }

            if (id == -1) {
                throw new RuntimeException("Error: could not query max id");
            }

            return id;
        }

        private static final void beginDocument(XmlPullParser parser, String firstElementName)
                throws XmlPullParserException, IOException {
            int type;
            while ((type = parser.next()) != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT) {
                ;
            }

            if (type != XmlPullParser.START_TAG) {
                throw new XmlPullParserException("No start tag found");
            }

            if (!parser.getName().equals(firstElementName)) {
                throw new XmlPullParserException("Unexpected start tag: found " + parser.getName() +
                        ", expected " + firstElementName);
            }
        }

        /**
         * Loads the default set of shortcut packages from an xml file.
         *
         * @param db                  The database to write the values into
         * @param workspaceResourceId The specific container id of items to load
         */
        private int loadWorkspace(SQLiteDatabase db, int workspaceResourceId) {
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            ContentValues values = new ContentValues();

            PackageManager packageManager = mContext.getPackageManager();
            int i = 0;
            try {
                XmlResourceParser parser = mContext.getResources().getXml(workspaceResourceId);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                beginDocument(parser, TAG_WORKSPACE);

                final int depth = parser.getDepth();

                int type;
                while (((type = parser.next()) != XmlPullParser.END_TAG ||
                        parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

                    if (type != XmlPullParser.START_TAG) {
                        continue;
                    }

                    TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.Shortcut);
                    String screen = a.getString(R.styleable.Shortcut_launcher_screen);
                    String x = a.getString(R.styleable.Shortcut_launcher_x);
                    String y = a.getString(R.styleable.Shortcut_launcher_y);

                    values.clear();
                    values.put(LauncherSettings.Shortcuts.SCREEN, screen);
                    values.put(LauncherSettings.Shortcuts.CELLX, x);
                    values.put(LauncherSettings.Shortcuts.CELLY, y);

                    final String name = parser.getName();
                    if (TAG_SHORTCUT.equals(name)) {
                        long id = addAppShortcut(db, values, a, packageManager, intent);
                        if (id >= 0) {
                            i++;
                        }
                    }

                    a.recycle();
                }
            } catch (XmlPullParserException e) {
                Log.w(TAG, "Got exception parsing shortcuts.", e);
            } catch (IOException e) {
                Log.w(TAG, "Got exception parsing shortcuts.", e);
            } catch (RuntimeException e) {
                Log.w(TAG, "Got exception parsing shortcuts.", e);
            }

            return i;
        }

        private long addAppShortcut(SQLiteDatabase db, ContentValues values, TypedArray a,
                                    PackageManager packageManager, Intent intent) {
            long id = -1;
            ActivityInfo info;
            String packageName = a.getString(R.styleable.Shortcut_launcher_packageName);
            String className = a.getString(R.styleable.Shortcut_launcher_className);
            try {
                ComponentName cn;
                try {
                    cn = new ComponentName(packageName, className);
                    info = packageManager.getActivityInfo(cn, 0);
                } catch (PackageManager.NameNotFoundException nnfe) {
                    String[] packages = packageManager.currentToCanonicalPackageNames(
                            new String[] { packageName });
                    cn = new ComponentName(packages[0], className);
                    info = packageManager.getActivityInfo(cn, 0);
                }
                id = generateNewId();
                intent.setComponent(cn);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                values.put(LauncherSettings.Shortcuts.INTENT, intent.toUri(0));
                values.put(LauncherSettings.Shortcuts.TITLE, info.loadLabel(packageManager).toString());
                values.put(LauncherSettings.Shortcuts.ITEM_TYPE, LauncherSettings.Shortcuts.ITEM_TYPE_APPLICATION);
                values.put(LauncherSettings.Shortcuts.SPANX, 1);
                values.put(LauncherSettings.Shortcuts.SPANY, 1);
                values.put(LauncherSettings.Shortcuts._ID, generateNewId());
                if (dbInsertAndCheck(this, db, TABLE_SHORTCUTS, null, values) < 0) {
                    return -1;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Unable to add shortcut: " + packageName +
                        "/" + className, e);
            }
            return id;
        }
    }

    static class SqlArguments {
        public final String table;
        public final String where;
        public final String[] args;

        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                table = url.getPathSegments().get(0);
                where = null;
                args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
        }
    }
}
