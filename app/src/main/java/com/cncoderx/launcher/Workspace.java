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

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;

/**
 * The workspace is a wide area with a wallpaper and a finite number of pages.
 * Each page contains a number of icons, folders or widgets the user can
 * interact with. A workspace is meant to be used with a fixed width only.
 */
public class Workspace extends SmoothPagedView
        implements View.OnTouchListener, LauncherTransitionable, ViewGroup.OnHierarchyChangeListener {
    private static final String TAG = "Launcher.Workspace";

    // Y rotation to apply to the workspace screens
    private static final float WORKSPACE_OVERSCROLL_ROTATION = 24f;

    private static final int CHILDREN_OUTLINE_FADE_OUT_DELAY = 0;
    private static final int CHILDREN_OUTLINE_FADE_OUT_DURATION = 375;
    private static final int CHILDREN_OUTLINE_FADE_IN_DURATION = 100;

    private static final int BACKGROUND_FADE_OUT_DURATION = 350;
    private static final int ADJACENT_SCREEN_DROP_DURATION = 300;
    private static final int FLING_THRESHOLD_VELOCITY = 500;

    // These animators are used to fade the children's outlines
    private ObjectAnimator mChildrenOutlineFadeInAnimation;
    private ObjectAnimator mChildrenOutlineFadeOutAnimation;
    private float mChildrenOutlineAlpha = 0;

    // These properties refer to the background protection gradient used for AllApps and Customize
    private ValueAnimator mBackgroundFadeInAnimation;
    private ValueAnimator mBackgroundFadeOutAnimation;
    private Drawable mBackground;
    boolean mDrawBackground = true;
    private float mBackgroundAlpha = 0;

    private float mWallpaperScrollRatio = 1.0f;
    private int mOriginalPageSpacing;

    private final WallpaperManager mWallpaperManager;
    private IBinder mWindowToken;
    private static final float WALLPAPER_SCREENS_SPAN = 2f;

    private int mDefaultPage;

    /**
     * CellInfo for the cell that is currently being dragged
     */
    private CellLayout.CellInfo mDragInfo;

    /**
     * Target drop area calculated during last acceptDrop call.
     */
    private int[] mTargetCell = new int[2];
    private int mDragOverX = -1;
    private int mDragOverY = -1;

    static Rect mLandscapeCellLayoutMetrics = null;
    static Rect mPortraitCellLayoutMetrics = null;

//    /**
//     * The CellLayout that is currently being dragged over
//     */
//    private CellLayout mDragTargetLayout = null;
//    /**
//     * The CellLayout that we will show as glowing
//     */
//    private CellLayout mDragOverlappingLayout = null;
//
//    /**
//     * The CellLayout which will be dropped to
//     */
//    private CellLayout mDropToLayout = null;

//    private Launcher mLauncher;
//    private IconCache mIconCache;
//    private DragController mDragController;

    // These are temporary variables to prevent having to allocate a new object just to
    // return an (x, y) value from helper functions. Do NOT use them to maintain other state.
//    private int[] mTempCell = new int[2];
//    private int[] mTempEstimate = new int[2];
//    private float[] mDragViewVisualCenter = new float[2];
//    private float[] mTempDragCoordinates = new float[2];
//    private float[] mTempCellLayoutCenterCoordinates = new float[2];
//    private float[] mTempDragBottomRightCoordinates = new float[2];
//    private Matrix mTempInverseMatrix = new Matrix();

//    private SpringLoadedDragController mSpringLoadedDragController;
    private float mSpringLoadedShrinkFactor;

    private static final int DEFAULT_CELL_COUNT_X = 4;
    private static final int DEFAULT_CELL_COUNT_Y = 4;

    // State variable that indicates whether the pages are small (ie when you're
    // in all apps or customize mode)

    enum State { NORMAL, SPRING_LOADED, SMALL };
    private State mState = State.NORMAL;
    private boolean mIsSwitchingState = false;

    boolean mAnimatingViewIntoPlace = false;
//    boolean mIsDragOccuring = false;
    boolean mChildrenLayersEnabled = true;

    /** Is the user is dragging an item near the edge of a page? */
    private boolean mInScrollArea = false;

//    private final HolographicOutlineHelper mOutlineHelper = new HolographicOutlineHelper();
//    private Bitmap mDragOutline = null;
    private final Rect mTempRect = new Rect();
    private final int[] mTempXY = new int[2];
    private int[] mTempVisiblePagesRange = new int[2];
    private float mOverscrollFade = 0;
    private boolean mOverscrollTransformsSet;
    public static final int DRAG_BITMAP_PADDING = 2;
    private boolean mWorkspaceFadeInAdjacentScreens;

//    enum WallpaperVerticalOffset { TOP, MIDDLE, BOTTOM };
//    int mWallpaperWidth;
//    int mWallpaperHeight;
//    WallpaperOffsetInterpolator mWallpaperOffset;
//    boolean mUpdateWallpaperOffsetImmediately = false;
    private Runnable mDelayedResizeRunnable;
    private Runnable mDelayedSnapToPageRunnable;
//    private Point mDisplaySize = new Point();
//    private boolean mIsStaticWallpaper;
//    private int mWallpaperTravelWidth;
    private int mSpringLoadedPageSpacing;
    private int mCameraDistance;

    // Variables relating to the creation of user folders by hovering shortcuts over shortcuts
//    private static final int FOLDER_CREATION_TIMEOUT = 0;
//    private static final int REORDER_TIMEOUT = 250;
//    private final Alarm mFolderCreationAlarm = new Alarm();
//    private final Alarm mReorderAlarm = new Alarm();
//    private FolderRingAnimator mDragFolderRingAnimator = null;
//    private FolderIcon mDragOverFolderIcon = null;
//    private boolean mCreateUserFolderOnDrop = false;
//    private boolean mAddToExistingFolderOnDrop = false;
//    private DropTarget.DragEnforcer mDragEnforcer;
//    private float mMaxDistanceForFolderCreation;

    // Variables relating to touch disambiguation (scrolling workspace vs. scrolling a widget)
    private float mXDown;
    private float mYDown;
    final static float START_DAMPING_TOUCH_SLOP_ANGLE = (float) Math.PI / 6;
    final static float MAX_SWIPE_ANGLE = (float) Math.PI / 3;
    final static float TOUCH_SLOP_DAMPING_FACTOR = 4;

    // Relating to the animation of items being dropped externally
    public static final int ANIMATE_INTO_POSITION_AND_DISAPPEAR = 0;
    public static final int ANIMATE_INTO_POSITION_AND_REMAIN = 1;
    public static final int ANIMATE_INTO_POSITION_AND_RESIZE = 2;
    public static final int COMPLETE_TWO_STAGE_WIDGET_DROP_ANIMATION = 3;
    public static final int CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION = 4;

    // Related to dragging, folder creation and reordering
    private static final int DRAG_MODE_NONE = 0;
    private static final int DRAG_MODE_CREATE_FOLDER = 1;
    private static final int DRAG_MODE_ADD_TO_FOLDER = 2;
    private static final int DRAG_MODE_REORDER = 3;
    private int mDragMode = DRAG_MODE_NONE;
    private int mLastReorderX = -1;
    private int mLastReorderY = -1;

    private SparseArray<Parcelable> mSavedStates;
    private final ArrayList<Integer> mRestoredPages = new ArrayList<Integer>();

    // These variables are used for storing the initial and final values during workspace animations
    private int mSavedScrollX;
    private float mSavedRotationY;
    private float mSavedTranslationX;
    private float mCurrentScaleX;
    private float mCurrentScaleY;
    private float mCurrentRotationY;
    private float mCurrentTranslationX;
    private float mCurrentTranslationY;
    private float[] mOldTranslationXs;
    private float[] mOldTranslationYs;
    private float[] mOldScaleXs;
    private float[] mOldScaleYs;
//    private float[] mOldBackgroundAlphas;
    private float[] mOldAlphas;
    private float[] mNewTranslationXs;
    private float[] mNewTranslationYs;
    private float[] mNewScaleXs;
    private float[] mNewScaleYs;
//    private float[] mNewBackgroundAlphas;
    private float[] mNewAlphas;
    private float[] mNewRotationYs;
    private float mTransitionProgress;

    private final Runnable mBindPages = new Runnable() {
        @Override
        public void run() {
//            mLauncher.getModel().bindRemainingSynchronousPages();
        }
    };

    /**
     * Used to inflate the Workspace from XML.
     *
     * @param context The application's context.
     * @param attrs The attributes set containing the Workspace's customization values.
     */
    public Workspace(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Used to inflate the Workspace from XML.
     *
     * @param context The application's context.
     * @param attrs The attributes set containing the Workspace's customization values.
     * @param defStyle Unused.
     */
    public Workspace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContentIsRefreshable = false;
        mOriginalPageSpacing = mPageSpacing;

//        mDragEnforcer = new DropTarget.DragEnforcer(context);
        // With workspace, data is available straight from the get-go
        setDataIsReady();

//        mLauncher = (Launcher) context;
        final Resources res = getResources();
        mWorkspaceFadeInAdjacentScreens = res.getBoolean(R.bool.config_workspaceFadeAdjacentScreens);
        mFadeInAdjacentScreens = false;
        mWallpaperManager = WallpaperManager.getInstance(context);

//        int cellCountX = DEFAULT_CELL_COUNT_X;
//        int cellCountY = DEFAULT_CELL_COUNT_Y;

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.Workspace, defStyle, 0);

//        if (LauncherApplication.isScreenLarge()) {
//            // Determine number of rows/columns dynamically
//            // TODO: This code currently fails on tablets with an aspect ratio < 1.3.
//            // Around that ratio we should make cells the same size in portrait and
//            // landscape
//            TypedArray actionBarSizeTypedArray =
//                context.obtainStyledAttributes(new int[] { android.R.attr.actionBarSize });
//            final float actionBarHeight = actionBarSizeTypedArray.getDimension(0, 0f);
//
//            Point minDims = new Point();
//            Point maxDims = new Point();
//            mLauncher.getWindowManager().getDefaultDisplay().getCurrentSizeRange(minDims, maxDims);
//
//            cellCountX = 1;
//            while (CellLayout.widthInPortrait(res, cellCountX + 1) <= minDims.x) {
//                cellCountX++;
//            }
//
//            cellCountY = 1;
//            while (actionBarHeight + CellLayout.heightInLandscape(res, cellCountY + 1)
//                <= minDims.y) {
//                cellCountY++;
//            }
//        }

        mSpringLoadedShrinkFactor =
            res.getInteger(R.integer.config_workspaceSpringLoadShrinkPercentage) / 100.0f;
        mSpringLoadedPageSpacing =
                res.getDimensionPixelSize(R.dimen.workspace_spring_loaded_page_spacing);
        mCameraDistance = res.getInteger(R.integer.config_cameraDistance);

        // if the value is manually specified, use that instead
//        cellCountX = a.getInt(R.styleable.Workspace_cellCountX, cellCountX);
//        cellCountY = a.getInt(R.styleable.Workspace_cellCountY, cellCountY);
        mDefaultPage = a.getInt(R.styleable.Workspace_defaultScreen, 1);
        a.recycle();

        setOnHierarchyChangeListener(this);

//        LauncherModel.updateWorkspaceLayoutCells(cellCountX, cellCountY);
        setHapticFeedbackEnabled(false);

        initWorkspace();

        // Disable multitouch across the workspace/all apps/customize tray
        setMotionEventSplittingEnabled(true);

        // Unless otherwise specified this view is important for accessibility.
        if (getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    // estimate the size of a widget with spans hSpan, vSpan. return MAX_VALUE for each
    // dimension if unsuccessful
    public int[] estimateItemSize(int hSpan, int vSpan,
            ItemInfo itemInfo, boolean springLoaded) {
        int[] size = new int[2];
        if (getChildCount() > 0) {
            CellLayout cl = (CellLayout) getChildAt(0);
            Rect r = estimateItemPosition(cl, itemInfo, 0, 0, hSpan, vSpan);
            size[0] = r.width();
            size[1] = r.height();
            if (springLoaded) {
                size[0] *= mSpringLoadedShrinkFactor;
                size[1] *= mSpringLoadedShrinkFactor;
            }
            return size;
        } else {
            size[0] = Integer.MAX_VALUE;
            size[1] = Integer.MAX_VALUE;
            return size;
        }
    }

    public Rect estimateItemPosition(CellLayout cl, ItemInfo pendingInfo,
            int hCell, int vCell, int hSpan, int vSpan) {
        Rect r = new Rect();
        cl.cellToRect(hCell, vCell, hSpan, vSpan, r);
        return r;
    }

//    public void onDragStart(DragSource source, Object info, int dragAction) {
//        mIsDragOccuring = true;
//        updateChildrenLayersEnabled(false);
//        mLauncher.lockScreenOrientation();
//        setChildrenBackgroundAlphaMultipliers(1f);
//        // Prevent any Un/InstallShortcutReceivers from updating the db while we are dragging
//        InstallShortcutReceiver.enableInstallQueue();
//        UninstallShortcutReceiver.enableUninstallQueue();
//    }
//
//    public void onDragEnd() {
//        mIsDragOccuring = false;
//        updateChildrenLayersEnabled(false);
//        mLauncher.unlockScreenOrientation(false);
//
//        // Re-enable any Un/InstallShortcutReceiver and now process any queued items
//        InstallShortcutReceiver.disableAndFlushInstallQueue(getContext());
//        UninstallShortcutReceiver.disableAndFlushUninstallQueue(getContext());
//    }

    /**
     * Initializes various states for this workspace.
     */
    protected void initWorkspace() {
//        Context context = getContext();
        mCurrentPage = mDefaultPage;
//        Launcher.setScreen(mCurrentPage);
//        LauncherApplication app = (LauncherApplication)context.getApplicationContext();
//        mIconCache = app.getIconCache();
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
        setChildrenDrawnWithCacheEnabled(true);

//        final Resources res = getResources();
//        try {
//            mBackground = res.getDrawable(R.drawable.apps_customize_bg);
//        } catch (Resources.NotFoundException e) {
//            // In this case, we will skip drawing background protection
//        }

//        mWallpaperOffset = new WallpaperOffsetInterpolator();
//        Display display = mLauncher.getWindowManager().getDefaultDisplay();
//        display.getSize(mDisplaySize);
//        mWallpaperTravelWidth = (int) (mDisplaySize.x *
//                wallpaperTravelToScreenWidthRatio(mDisplaySize.x, mDisplaySize.y));

//        mMaxDistanceForFolderCreation = (0.55f * res.getDimensionPixelSize(R.dimen.app_icon_size));
        mFlingThresholdVelocity = (int) (FLING_THRESHOLD_VELOCITY * mDensity);
    }

    @Override
    protected int getScrollMode() {
        return SmoothPagedView.X_LARGE_MODE;
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        CellLayout cl = ((CellLayout) child);
        cl.setOnInterceptTouchListener(this);
        cl.setClickable(true);
        cl.setContentDescription(getContext().getString(
                R.string.workspace_description_format, getChildCount()));
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
    }

//    protected boolean shouldDrawChild(View child) {
//        final CellLayout cl = (CellLayout) child;
//        return super.shouldDrawChild(child) &&
//            (cl.getShortcutsAndWidgets().getAlpha() > 0 ||
//             cl.getBackgroundAlpha() > 0);
//    }

//    /**
//     * @return The open folder on the current screen, or null if there is none
//     */
//    Folder getOpenFolder() {
//        DragLayer dragLayer = mLauncher.getDragLayer();
//        int count = dragLayer.getChildCount();
//        for (int i = 0; i < count; i++) {
//            View child = dragLayer.getChildAt(i);
//            if (child instanceof Folder) {
//                Folder folder = (Folder) child;
//                if (folder.getInfo().opened)
//                    return folder;
//            }
//        }
//        return null;
//    }

    boolean isTouchActive() {
        return mTouchState != TOUCH_STATE_REST;
    }

    /**
     * Adds the specified child in the specified screen. The position and dimension of
     * the child are defined by x, y, spanX and spanY.
     *
     * @param child The child to add in one of the workspace's screens.
     * @param screen The screen in which to add the child.
     * @param x The X position of the child in the screen's grid.
     * @param y The Y position of the child in the screen's grid.
     * @param spanX The number of cells spanned horizontally by the child.
     * @param spanY The number of cells spanned vertically by the child.
     */
    void addInScreen(View child, int screen, int x, int y, int spanX, int spanY) {
        addInScreen(child, screen, x, y, spanX, spanY, false);
    }

    /**
     * Adds the specified child in the specified screen. The position and dimension of
     * the child are defined by x, y, spanX and spanY.
     *
     * @param child The child to add in one of the workspace's screens.
     * @param screen The screen in which to add the child.
     * @param x The X position of the child in the screen's grid.
     * @param y The Y position of the child in the screen's grid.
     * @param spanX The number of cells spanned horizontally by the child.
     * @param spanY The number of cells spanned vertically by the child.
     * @param insert When true, the child is inserted at the beginning of the children list.
     */
    void addInScreen(View child, int screen, int x, int y, int spanX, int spanY,
            boolean insert) {
        if (screen < 0 || screen >= getChildCount()) {
            Log.e(TAG, "The screen must be >= 0 and < " + getChildCount()
                + " (was " + screen + "); skipping child");
            return;
        }

        final CellLayout layout = (CellLayout) getChildAt(screen);
        child.setOnKeyListener(new IconKeyEventListener());

        LayoutParams genericLp = child.getLayoutParams();
        CellLayout.LayoutParams lp;
        if (genericLp == null || !(genericLp instanceof CellLayout.LayoutParams)) {
            lp = new CellLayout.LayoutParams(x, y, spanX, spanY);
        } else {
            lp = (CellLayout.LayoutParams) genericLp;
            lp.cellX = x;
            lp.cellY = y;
            lp.cellHSpan = spanX;
            lp.cellVSpan = spanY;
        }

        if (spanX < 0 && spanY < 0) {
            lp.isLockedToGrid = false;
        }

        // Get the canonical child id to uniquely represent this view in this screen
        int childId = getCellLayoutChildId(screen, x, y, spanX, spanY);
        if (!layout.addViewToCellLayout(child, insert ? 0 : -1, childId, lp, true)) {
            // TODO: This branch occurs when the workspace is adding views
            // outside of the defined grid
            // maybe we should be deleting these items from the LauncherModel?
            Log.w(TAG, "Failed to add to item at (" + lp.cellX + "," + lp.cellY + ") to CellLayout");
        }

        child.setHapticFeedbackEnabled(false);
        child.setOnLongClickListener(mLongClickListener);

//        if (child instanceof DropTarget) {
//            mDragController.addDropTarget((DropTarget) child);
//        }
    }

    /**
     * Creates a new unique child id, for a given cell span across all layouts.
     */
    static int getCellLayoutChildId(int screen, int localCellX, int localCellY, int spanX, int spanY) {
        return (screen & 0xFF) << 16 | (localCellX & 0xFF) << 8 | (localCellY & 0xFF);
    }

//        /**
//         * Check if the point (x, y) hits a given page.
//         */
//    private boolean hitsPage(int index, float x, float y) {
//        final View page = getChildAt(index);
//        if (page != null) {
//            float[] localXY = { x, y };
//            mapPointFromSelfToChild(page, localXY);
//            return (localXY[0] >= 0 && localXY[0] < page.getWidth()
//                    && localXY[1] >= 0 && localXY[1] < page.getHeight());
//        }
//        return false;
//    }

    @Override
    protected boolean hitsPreviousPage(float x, float y) {
        // mNextPage is set to INVALID_PAGE whenever we are stationary.
        // Calculating "next page" this way ensures that you scroll to whatever page you tap on
//        final int current = (mNextPage == INVALID_PAGE) ? mCurrentPage : mNextPage;

        // Only allow tap to next page on large devices, where there's significant margin outside
        // the active workspace
//        return LauncherApplication.isScreenLarge() && hitsPage(current - 1, x, y);

        return false;
    }

    @Override
    protected boolean hitsNextPage(float x, float y) {
        // mNextPage is set to INVALID_PAGE whenever we are stationary.
        // Calculating "next page" this way ensures that you scroll to whatever page you tap on
//        final int current = (mNextPage == INVALID_PAGE) ? mCurrentPage : mNextPage;

        // Only allow tap to next page on large devices, where there's significant margin outside
        // the active workspace
//        return LauncherApplication.isScreenLarge() && hitsPage(current + 1, x, y);

        return false;
    }

    /**
     * Called directly from a CellLayout (not by the framework), after we've been added as a
     * listener via setOnInterceptTouchEventListener(). This allows us to tell the CellLayout
     * that it should intercept touch events, which is not something that is normally supported.
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return (isSmall() || !isFinishedSwitchingState());
    }

    public boolean isSwitchingState() {
        return mIsSwitchingState;
    }

    /** This differs from isSwitchingState in that we take into account how far the transition
     *  has completed. */
    public boolean isFinishedSwitchingState() {
        return !mIsSwitchingState || (mTransitionProgress > 0.5f);
    }

    @Override
    protected void onWindowVisibilityChanged (int visibility) {
//        mLauncher.onWindowVisibilityChanged(visibility);
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
        if (isSmall() || !isFinishedSwitchingState()) {
            // when the home screens are shrunken, shouldn't allow side-scrolling
            return false;
        }
        return super.dispatchUnhandledMove(focused, direction);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            mXDown = ev.getX();
            mYDown = ev.getY();
            break;
        case MotionEvent.ACTION_POINTER_UP:
        case MotionEvent.ACTION_UP:
            if (mTouchState == TOUCH_STATE_REST) {
                final CellLayout currentPage = (CellLayout) getChildAt(mCurrentPage);
                if (!currentPage.lastDownOnOccupiedCell()) {
//                    onWallpaperTap(ev);
                }
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

//    protected void reinflateWidgetsIfNecessary() {
//        final int clCount = getChildCount();
//        for (int i = 0; i < clCount; i++) {
//            CellLayout cl = (CellLayout) getChildAt(i);
//            ShortcutAndWidgetContainer swc = cl.getShortcutsAndWidgets();
//            final int itemCount = swc.getChildCount();
//            for (int j = 0; j < itemCount; j++) {
//                View v = swc.getChildAt(j);
//
//                if (v.getTag() instanceof LauncherAppWidgetInfo) {
//                    LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) v.getTag();
//                    LauncherAppWidgetHostView lahv = (LauncherAppWidgetHostView) info.hostView;
//                    if (lahv != null && lahv.orientationChangedSincedInflation()) {
//                        mLauncher.removeAppWidget(info);
//                        // Remove the current widget which is inflated with the wrong orientation
//                        cl.removeView(lahv);
//                        mLauncher.bindAppWidget(info);
//                    }
//                }
//            }
//        }
//    }

    @Override
    protected void determineScrollingStart(MotionEvent ev) {
        if (isSmall()) {
            return;
        }
        if (!isFinishedSwitchingState()) {
            return;
        }

        float deltaX = Math.abs(ev.getX() - mXDown);
        float deltaY = Math.abs(ev.getY() - mYDown);

        if (Float.compare(deltaX, 0f) == 0) {
            return;
        }

        float slope = deltaY / deltaX;
        float theta = (float) Math.atan(slope);

        if (deltaX > mTouchSlop || deltaY > mTouchSlop) {
            cancelCurrentPageLongPress();
        }

        if (theta > MAX_SWIPE_ANGLE) {
            // Above MAX_SWIPE_ANGLE, we don't want to ever start scrolling the workspace
            return;
        } else if (theta > START_DAMPING_TOUCH_SLOP_ANGLE) {
            // Above START_DAMPING_TOUCH_SLOP_ANGLE and below MAX_SWIPE_ANGLE, we want to
            // increase the touch slop to make it harder to begin scrolling the workspace. This
            // results in vertically scrolling widgets to more easily. The higher the angle, the
            // more we increase touch slop.
            theta -= START_DAMPING_TOUCH_SLOP_ANGLE;
            float extraRatio = (float)
                    Math.sqrt((theta / (MAX_SWIPE_ANGLE - START_DAMPING_TOUCH_SLOP_ANGLE)));
            super.determineScrollingStart(ev, 1 + TOUCH_SLOP_DAMPING_FACTOR * extraRatio);
        } else {
            // Below START_DAMPING_TOUCH_SLOP_ANGLE, we don't do anything special
            super.determineScrollingStart(ev);
        }
    }

    @Override
    protected void onPageBeginMoving() {
        super.onPageBeginMoving();

        if (isHardwareAccelerated()) {
            updateChildrenLayersEnabled(false);
        } else {
            if (mNextPage != INVALID_PAGE) {
                // we're snapping to a particular screen
                enableChildrenCache(mCurrentPage, mNextPage);
            } else {
                // this is when user is actively dragging a particular screen, they might
                // swipe it either left or right (but we won't advance by more than one screen)
                enableChildrenCache(mCurrentPage - 1, mCurrentPage + 1);
            }
        }

        // Only show page outlines as we pan if we are on large screen
//        if (LauncherApplication.isScreenLarge()) {
//            showOutlines();
//            mIsStaticWallpaper = mWallpaperManager.getWallpaperInfo() == null;
//        }

        // If we are not fading in adjacent screens, we still need to restore the alpha in case the
        // user scrolls while we are transitioning (should not affect dispatchDraw optimizations)
        if (!mWorkspaceFadeInAdjacentScreens) {
            for (int i = 0; i < getChildCount(); ++i) {
                ((CellLayout) getPageAt(i)).setShortcutAndWidgetAlpha(1f);
            }
        }

        // Show the scroll indicator as you pan the page
        showScrollingIndicator(false);
    }

    @Override
    protected void onPageEndMoving() {
        super.onPageEndMoving();

        if (isHardwareAccelerated()) {
            updateChildrenLayersEnabled(false);
        } else {
            clearChildrenCache();
        }


//        if (mDragController.isDragging()) {
//            if (isSmall()) {
//                // If we are in springloaded mode, then force an event to check if the current touch
//                // is under a new page (to scroll to)
//                mDragController.forceTouchMove();
//            }
//        } else {
//            // If we are not mid-dragging, hide the page outlines if we are on a large screen
//            if (LauncherApplication.isScreenLarge()) {
//                hideOutlines();
//            }
//
//            // Hide the scroll indicator as you pan the page
//            if (!mDragController.isDragging()) {
//                hideScrollingIndicator(false);
//            }
//        }

        if (mDelayedResizeRunnable != null) {
            mDelayedResizeRunnable.run();
            mDelayedResizeRunnable = null;
        }

        if (mDelayedSnapToPageRunnable != null) {
            mDelayedSnapToPageRunnable.run();
            mDelayedSnapToPageRunnable = null;
        }
    }

    @Override
    protected void notifyPageSwitchListener() {
        super.notifyPageSwitchListener();
//        Launcher.setScreen(mCurrentPage);
    };

    @Override
    protected void updateCurrentPageScroll() {
        super.updateCurrentPageScroll();
//        computeWallpaperScrollRatio(mCurrentPage);
    }

    @Override
    protected void snapToPage(int whichPage) {
        super.snapToPage(whichPage);
//        computeWallpaperScrollRatio(whichPage);
    }

    @Override
    protected void snapToPage(int whichPage, int duration) {
        super.snapToPage(whichPage, duration);
//        computeWallpaperScrollRatio(whichPage);
    }

    protected void snapToPage(int whichPage, Runnable r) {
        if (mDelayedSnapToPageRunnable != null) {
            mDelayedSnapToPageRunnable.run();
        }
        mDelayedSnapToPageRunnable = r;
        snapToPage(whichPage, SLOW_PAGE_SNAP_ANIMATION_DURATION);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
//        syncWallpaperOffsetWithScroll();
    }

    void showOutlines() {
        if (!isSmall() && !mIsSwitchingState) {
            if (mChildrenOutlineFadeOutAnimation != null) mChildrenOutlineFadeOutAnimation.cancel();
            if (mChildrenOutlineFadeInAnimation != null) mChildrenOutlineFadeInAnimation.cancel();
            mChildrenOutlineFadeInAnimation = LauncherAnimUtils.ofFloat(this, "childrenOutlineAlpha", 1.0f);
            mChildrenOutlineFadeInAnimation.setDuration(CHILDREN_OUTLINE_FADE_IN_DURATION);
            mChildrenOutlineFadeInAnimation.start();
        }
    }

    void hideOutlines() {
        if (!isSmall() && !mIsSwitchingState) {
            if (mChildrenOutlineFadeInAnimation != null) mChildrenOutlineFadeInAnimation.cancel();
            if (mChildrenOutlineFadeOutAnimation != null) mChildrenOutlineFadeOutAnimation.cancel();
            mChildrenOutlineFadeOutAnimation = LauncherAnimUtils.ofFloat(this, "childrenOutlineAlpha", 0.0f);
            mChildrenOutlineFadeOutAnimation.setDuration(CHILDREN_OUTLINE_FADE_OUT_DURATION);
            mChildrenOutlineFadeOutAnimation.setStartDelay(CHILDREN_OUTLINE_FADE_OUT_DELAY);
            mChildrenOutlineFadeOutAnimation.start();
        }
    }

    public void showOutlinesTemporarily() {
        if (!mIsPageMoving && !isTouchActive()) {
            snapToPage(mCurrentPage);
        }
    }

//    public void setChildrenOutlineAlpha(float alpha) {
//        mChildrenOutlineAlpha = alpha;
//        for (int i = 0; i < getChildCount(); i++) {
//            CellLayout cl = (CellLayout) getChildAt(i);
//            cl.setBackgroundAlpha(alpha);
//        }
//    }

    public float getChildrenOutlineAlpha() {
        return mChildrenOutlineAlpha;
    }

    void disableBackground() {
        mDrawBackground = false;
    }
    void enableBackground() {
        mDrawBackground = true;
    }

    private void animateBackgroundGradient(float finalAlpha, boolean animated) {
        if (mBackground == null) return;
        if (mBackgroundFadeInAnimation != null) {
            mBackgroundFadeInAnimation.cancel();
            mBackgroundFadeInAnimation = null;
        }
        if (mBackgroundFadeOutAnimation != null) {
            mBackgroundFadeOutAnimation.cancel();
            mBackgroundFadeOutAnimation = null;
        }
        float startAlpha = getBackgroundAlpha();
        if (finalAlpha != startAlpha) {
            if (animated) {
                mBackgroundFadeOutAnimation =
                        LauncherAnimUtils.ofFloat(this, startAlpha, finalAlpha);
                mBackgroundFadeOutAnimation.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        setBackgroundAlpha(((Float) animation.getAnimatedValue()).floatValue());
                    }
                });
                mBackgroundFadeOutAnimation.setInterpolator(new DecelerateInterpolator(1.5f));
                mBackgroundFadeOutAnimation.setDuration(BACKGROUND_FADE_OUT_DURATION);
                mBackgroundFadeOutAnimation.start();
            } else {
                setBackgroundAlpha(finalAlpha);
            }
        }
    }

    public void setBackgroundAlpha(float alpha) {
        if (alpha != mBackgroundAlpha) {
            mBackgroundAlpha = alpha;
            invalidate();
        }
    }

    public float getBackgroundAlpha() {
        return mBackgroundAlpha;
    }

    float backgroundAlphaInterpolator(float r) {
        float pivotA = 0.1f;
        float pivotB = 0.4f;
        if (r < pivotA) {
            return 0;
        } else if (r > pivotB) {
            return 1.0f;
        } else {
            return (r - pivotA)/(pivotB - pivotA);
        }
    }

    private void updatePageAlphaValues(int screenCenter) {
        boolean isInOverscroll = mOverScrollX < 0 || mOverScrollX > mMaxScrollX;
        if (mWorkspaceFadeInAdjacentScreens &&
                mState == State.NORMAL &&
                !mIsSwitchingState &&
                !isInOverscroll) {
            for (int i = 0; i < getChildCount(); i++) {
                CellLayout child = (CellLayout) getChildAt(i);
                if (child != null) {
                    float scrollProgress = getScrollProgress(screenCenter, child, i);
                    float alpha = 1 - Math.abs(scrollProgress);
                    child.setAlpha(alpha);
//                    if (!mIsDragOccuring) {
//                        child.setBackgroundAlphaMultiplier(
//                                backgroundAlphaInterpolator(Math.abs(scrollProgress)));
//                    } else {
//                        child.setBackgroundAlphaMultiplier(1f);
//                    }
                }
            }
        }
    }

//    private void setChildrenBackgroundAlphaMultipliers(float a) {
//        for (int i = 0; i < getChildCount(); i++) {
//            CellLayout child = (CellLayout) getChildAt(i);
//            child.setBackgroundAlphaMultiplier(a);
//        }
//    }

    @Override
    protected void screenScrolled(int screenCenter) {
        final boolean isRtl = isLayoutRtl();
        super.screenScrolled(screenCenter);

        updatePageAlphaValues(screenCenter);
        enableHwLayersOnVisiblePages();

        if (mOverScrollX < 0 || mOverScrollX > mMaxScrollX) {
            int index = 0;
            float pivotX = 0f;
            final float leftBiasedPivot = 0.25f;
            final float rightBiasedPivot = 0.75f;
            final int lowerIndex = 0;
            final int upperIndex = getChildCount() - 1;
            if (isRtl) {
                index = mOverScrollX < 0 ? upperIndex : lowerIndex;
                pivotX = (index == 0 ? leftBiasedPivot : rightBiasedPivot);
            } else {
                index = mOverScrollX < 0 ? lowerIndex : upperIndex;
                pivotX = (index == 0 ? rightBiasedPivot : leftBiasedPivot);
            }

            CellLayout cl = (CellLayout) getChildAt(index);
            float scrollProgress = getScrollProgress(screenCenter, cl, index);
            final boolean isLeftPage = (isRtl ? index > 0 : index == 0);
//            cl.setOverScrollAmount(Math.abs(scrollProgress), isLeftPage);
            float rotation = -WORKSPACE_OVERSCROLL_ROTATION * scrollProgress;
            cl.setRotationY(rotation);
            setFadeForOverScroll(Math.abs(scrollProgress));
            if (!mOverscrollTransformsSet) {
                mOverscrollTransformsSet = true;
                cl.setCameraDistance(mDensity * mCameraDistance);
                cl.setPivotX(cl.getMeasuredWidth() * pivotX);
                cl.setPivotY(cl.getMeasuredHeight() * 0.5f);
//                cl.setOverscrollTransformsDirty(true);
            }
        } else {
            if (mOverscrollFade != 0) {
                setFadeForOverScroll(0);
            }
            if (mOverscrollTransformsSet) {
                mOverscrollTransformsSet = false;
//                ((CellLayout) getChildAt(0)).resetOverscrollTransforms();
//                ((CellLayout) getChildAt(getChildCount() - 1)).resetOverscrollTransforms();
            }
        }
    }

    @Override
    protected void overScroll(float amount) {
        acceleratedOverScroll(amount);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mWindowToken = getWindowToken();
        computeScroll();
//        mDragController.setWindowToken(mWindowToken);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mWindowToken = null;
    }

//    @Override
//    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//        if (mFirstLayout && mCurrentPage >= 0 && mCurrentPage < getChildCount()) {
//            mUpdateWallpaperOffsetImmediately = true;
//        }
//        super.onLayout(changed, left, top, right, bottom);
//    }

    @Override
    protected void onDraw(Canvas canvas) {
//        updateWallpaperOffsets();

        // Draw the background gradient if necessary
        if (mBackground != null && mBackgroundAlpha > 0.0f && mDrawBackground) {
            int alpha = (int) (mBackgroundAlpha * 255);
            mBackground.setAlpha(alpha);
            mBackground.setBounds(getScrollX(), 0, getScrollX() + getMeasuredWidth(),
                    getMeasuredHeight());
            mBackground.draw(canvas);
        }

        super.onDraw(canvas);

        // Call back to LauncherModel to finish binding after the first draw
        post(mBindPages);
    }

    boolean isDrawingBackgroundGradient() {
        return (mBackground != null && mBackgroundAlpha > 0.0f && mDrawBackground);
    }

//    @Override
//    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
//        if (!mLauncher.isAllAppsVisible()) {
//            final Folder openFolder = getOpenFolder();
//            if (openFolder != null) {
//                return openFolder.requestFocus(direction, previouslyFocusedRect);
//            } else {
//                return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
//            }
//        }
//        return false;
//    }

    @Override
    public int getDescendantFocusability() {
        if (isSmall()) {
            return ViewGroup.FOCUS_BLOCK_DESCENDANTS;
        }
        return super.getDescendantFocusability();
    }

//    @Override
//    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
//        if (!mLauncher.isAllAppsVisible()) {
//            final Folder openFolder = getOpenFolder();
//            if (openFolder != null) {
//                openFolder.addFocusables(views, direction);
//            } else {
//                super.addFocusables(views, direction, focusableMode);
//            }
//        }
//    }

    public boolean isSmall() {
        return mState == State.SMALL || mState == State.SPRING_LOADED;
    }

    void enableChildrenCache(int fromPage, int toPage) {
        if (fromPage > toPage) {
            final int temp = fromPage;
            fromPage = toPage;
            toPage = temp;
        }

        final int screenCount = getChildCount();

        fromPage = Math.max(fromPage, 0);
        toPage = Math.min(toPage, screenCount - 1);

        for (int i = fromPage; i <= toPage; i++) {
            final CellLayout layout = (CellLayout) getChildAt(i);
            layout.setChildrenDrawnWithCacheEnabled(true);
            layout.setChildrenDrawingCacheEnabled(true);
        }
    }

    void clearChildrenCache() {
        final int screenCount = getChildCount();
        for (int i = 0; i < screenCount; i++) {
            final CellLayout layout = (CellLayout) getChildAt(i);
            layout.setChildrenDrawnWithCacheEnabled(false);
            // In software mode, we don't want the items to continue to be drawn into bitmaps
            if (!isHardwareAccelerated()) {
                layout.setChildrenDrawingCacheEnabled(false);
            }
        }
    }


    private void updateChildrenLayersEnabled(boolean force) {
        boolean small = mState == State.SMALL || mIsSwitchingState;
        boolean enableChildrenLayers = force || small || mAnimatingViewIntoPlace || isPageMoving();

        if (enableChildrenLayers != mChildrenLayersEnabled) {
            mChildrenLayersEnabled = enableChildrenLayers;
            if (mChildrenLayersEnabled) {
                enableHwLayersOnVisiblePages();
            } else {
                for (int i = 0; i < getPageCount(); i++) {
                    final CellLayout cl = (CellLayout) getChildAt(i);
                    cl.disableHardwareLayers();
                }
            }
        }
    }

    private void enableHwLayersOnVisiblePages() {
        if (mChildrenLayersEnabled) {
            final int screenCount = getChildCount();
            getVisiblePages(mTempVisiblePagesRange);
            int leftScreen = mTempVisiblePagesRange[0];
            int rightScreen = mTempVisiblePagesRange[1];
            if (leftScreen == rightScreen) {
                // make sure we're caching at least two pages always
                if (rightScreen < screenCount - 1) {
                    rightScreen++;
                } else if (leftScreen > 0) {
                    leftScreen--;
                }
            }
            for (int i = 0; i < screenCount; i++) {
                final CellLayout layout = (CellLayout) getPageAt(i);
                if (!(leftScreen <= i && i <= rightScreen && shouldDrawChild(layout))) {
                    layout.disableHardwareLayers();
                }
            }
            for (int i = 0; i < screenCount; i++) {
                final CellLayout layout = (CellLayout) getPageAt(i);
                if (leftScreen <= i && i <= rightScreen && shouldDrawChild(layout)) {
                    layout.enableHardwareLayers();
                }
            }
        }
    }

    public void buildPageHardwareLayers() {
        // force layers to be enabled just for the call to buildLayer
        updateChildrenLayersEnabled(true);
        if (getWindowToken() != null) {
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                CellLayout cl = (CellLayout) getChildAt(i);
                cl.buildHardwareLayer();
            }
        }
        updateChildrenLayersEnabled(false);
    }
//
//    protected void onWallpaperTap(MotionEvent ev) {
//        final int[] position = mTempCell;
//        getLocationOnScreen(position);
//
//        int pointerIndex = ev.getActionIndex();
//        position[0] += (int) ev.getX(pointerIndex);
//        position[1] += (int) ev.getY(pointerIndex);
//
//        mWallpaperManager.sendWallpaperCommand(getWindowToken(),
//                ev.getAction() == MotionEvent.ACTION_UP
//                        ? WallpaperManager.COMMAND_TAP : WallpaperManager.COMMAND_SECONDARY_TAP,
//                position[0], position[1], 0, null);
//    }

    /*
     * This interpolator emulates the rate at which the perceived scale of an object changes
     * as its distance from a camera increases. When this interpolator is applied to a scale
     * animation on a view, it evokes the sense that the object is shrinking due to moving away
     * from the camera.
     */
    static class ZInterpolator implements TimeInterpolator {
        private float focalLength;

        public ZInterpolator(float foc) {
            focalLength = foc;
        }

        @Override
        public float getInterpolation(float input) {
            return (1.0f - focalLength / (focalLength + input)) /
                (1.0f - focalLength / (focalLength + 1.0f));
        }
    }

    /*
     * The exact reverse of ZInterpolator.
     */
    static class InverseZInterpolator implements TimeInterpolator {
        private ZInterpolator zInterpolator;

        public InverseZInterpolator(float foc) {
            zInterpolator = new ZInterpolator(foc);
        }

        @Override
        public float getInterpolation(float input) {
            return 1 - zInterpolator.getInterpolation(1 - input);
        }
    }

    /*
     * ZInterpolator compounded with an ease-out.
     */
    static class ZoomOutInterpolator implements TimeInterpolator {
        private final DecelerateInterpolator decelerate = new DecelerateInterpolator(0.75f);
        private final ZInterpolator zInterpolator = new ZInterpolator(0.13f);

        @Override
        public float getInterpolation(float input) {
            return decelerate.getInterpolation(zInterpolator.getInterpolation(input));
        }
    }

    /*
     * InvereZInterpolator compounded with an ease-out.
     */
    static class ZoomInInterpolator implements TimeInterpolator {
        private final InverseZInterpolator inverseZInterpolator = new InverseZInterpolator(0.35f);
        private final DecelerateInterpolator decelerate = new DecelerateInterpolator(3.0f);

        @Override
        public float getInterpolation(float input) {
            return decelerate.getInterpolation(inverseZInterpolator.getInterpolation(input));
        }
    }

    private final ZoomInInterpolator mZoomInInterpolator = new ZoomInInterpolator();

    private void initAnimationArrays() {
        final int childCount = getChildCount();
        if (mOldTranslationXs != null) {
            return;
        }
        mOldTranslationXs = new float[childCount];
        mOldTranslationYs = new float[childCount];
        mOldScaleXs = new float[childCount];
        mOldScaleYs = new float[childCount];
//        mOldBackgroundAlphas = new float[childCount];
        mOldAlphas = new float[childCount];
        mNewTranslationXs = new float[childCount];
        mNewTranslationYs = new float[childCount];
        mNewScaleXs = new float[childCount];
        mNewScaleYs = new float[childCount];
//        mNewBackgroundAlphas = new float[childCount];
        mNewAlphas = new float[childCount];
        mNewRotationYs = new float[childCount];
    }

    Animator getChangeStateAnimation(final State state, boolean animated) {
        return getChangeStateAnimation(state, animated, 0);
    }

    Animator getChangeStateAnimation(final State state, boolean animated, int delay) {
        if (mState == state) {
            return null;
        }

        // Initialize animation arrays for the first time if necessary
        initAnimationArrays();

        AnimatorSet anim = animated ? LauncherAnimUtils.createAnimatorSet() : null;

        // Stop any scrolling, move to the current page right away
        setCurrentPage(getNextPage());

        final State oldState = mState;
        final boolean oldStateIsNormal = (oldState == State.NORMAL);
        final boolean oldStateIsSpringLoaded = (oldState == State.SPRING_LOADED);
        final boolean oldStateIsSmall = (oldState == State.SMALL);
        mState = state;
        final boolean stateIsNormal = (state == State.NORMAL);
        final boolean stateIsSpringLoaded = (state == State.SPRING_LOADED);
        final boolean stateIsSmall = (state == State.SMALL);
        float finalScaleFactor = 1.0f;
//        float finalBackgroundAlpha = stateIsSpringLoaded ? 1.0f : 0f;
        float translationX = 0;
        float translationY = 0;
        boolean zoomIn = true;

        if (state != State.NORMAL) {
            finalScaleFactor = mSpringLoadedShrinkFactor - (stateIsSmall ? 0.1f : 0);
            setPageSpacing(mSpringLoadedPageSpacing);
            if (oldStateIsNormal && stateIsSmall) {
                zoomIn = false;
                setLayoutScale(finalScaleFactor);
                updateChildrenLayersEnabled(false);
            } else {
//                finalBackgroundAlpha = 1.0f;
                setLayoutScale(finalScaleFactor);
            }
        } else {
            setPageSpacing(mOriginalPageSpacing);
            setLayoutScale(1.0f);
        }

        final int duration = zoomIn ?
                getResources().getInteger(R.integer.config_workspaceUnshrinkTime) :
                getResources().getInteger(R.integer.config_appsCustomizeWorkspaceShrinkTime);
        for (int i = 0; i < getChildCount(); i++) {
            final CellLayout cl = (CellLayout) getChildAt(i);
            float finalAlpha = (!mWorkspaceFadeInAdjacentScreens || stateIsSpringLoaded ||
                    (i == mCurrentPage)) ? 1f : 0f;
            float currentAlpha = cl.getAlpha();
            float initialAlpha = currentAlpha;

            // Determine the pages alpha during the state transition
            if ((oldStateIsSmall && stateIsNormal) ||
                (oldStateIsNormal && stateIsSmall)) {
                // To/from workspace - only show the current page unless the transition is not
                //                     animated and the animation end callback below doesn't run;
                //                     or, if we're in spring-loaded mode
                if (i == mCurrentPage || !animated || oldStateIsSpringLoaded) {
                    finalAlpha = 1f;
                } else {
                    initialAlpha = 0f;
                    finalAlpha = 0f;
                }
            }

            mOldAlphas[i] = initialAlpha;
            mNewAlphas[i] = finalAlpha;
            if (animated) {
                mOldTranslationXs[i] = cl.getTranslationX();
                mOldTranslationYs[i] = cl.getTranslationY();
                mOldScaleXs[i] = cl.getScaleX();
                mOldScaleYs[i] = cl.getScaleY();
//                mOldBackgroundAlphas[i] = cl.getBackgroundAlpha();

                mNewTranslationXs[i] = translationX;
                mNewTranslationYs[i] = translationY;
                mNewScaleXs[i] = finalScaleFactor;
                mNewScaleYs[i] = finalScaleFactor;
//                mNewBackgroundAlphas[i] = finalBackgroundAlpha;
            } else {
                cl.setTranslationX(translationX);
                cl.setTranslationY(translationY);
                cl.setScaleX(finalScaleFactor);
                cl.setScaleY(finalScaleFactor);
//                cl.setBackgroundAlpha(finalBackgroundAlpha);
                cl.setShortcutAndWidgetAlpha(finalAlpha);
            }
        }

        if (animated) {
            for (int index = 0; index < getChildCount(); index++) {
                final int i = index;
                final CellLayout cl = (CellLayout) getChildAt(i);
                float currentAlpha = cl.getAlpha();
                if (mOldAlphas[i] == 0 && mNewAlphas[i] == 0) {
                    cl.setTranslationX(mNewTranslationXs[i]);
                    cl.setTranslationY(mNewTranslationYs[i]);
                    cl.setScaleX(mNewScaleXs[i]);
                    cl.setScaleY(mNewScaleYs[i]);
//                    cl.setBackgroundAlpha(mNewBackgroundAlphas[i]);
                    cl.setShortcutAndWidgetAlpha(mNewAlphas[i]);
                    cl.setRotationY(mNewRotationYs[i]);
                } else {
                    LauncherViewPropertyAnimator a = new LauncherViewPropertyAnimator(cl);
                    a.translationX(mNewTranslationXs[i])
                        .translationY(mNewTranslationYs[i])
                        .scaleX(mNewScaleXs[i])
                        .scaleY(mNewScaleYs[i])
                        .setDuration(duration)
                        .setInterpolator(mZoomInInterpolator);
                    anim.play(a);

                    if (mOldAlphas[i] != mNewAlphas[i] || currentAlpha != mNewAlphas[i]) {
                        LauncherViewPropertyAnimator alphaAnim =
                            new LauncherViewPropertyAnimator(cl);
                        alphaAnim.alpha(mNewAlphas[i])
                            .setDuration(duration)
                            .setInterpolator(mZoomInInterpolator);
                        anim.play(alphaAnim);
                    }
//                    if (mOldBackgroundAlphas[i] != 0 ||
//                        mNewBackgroundAlphas[i] != 0) {
//                        ValueAnimator bgAnim =
//                                LauncherAnimUtils.ofFloat(cl, 0f, 1f).setDuration(duration);
//                        bgAnim.setInterpolator(mZoomInInterpolator);
//                        bgAnim.addUpdateListener(new LauncherAnimatorUpdateListener() {
//                                public void onAnimationUpdate(float a, float b) {
//                                    cl.setBackgroundAlpha(
//                                            a * mOldBackgroundAlphas[i] +
//                                            b * mNewBackgroundAlphas[i]);
//                                }
//                            });
//                        anim.play(bgAnim);
//                    }
                }
            }
            anim.setStartDelay(delay);
        }

        if (stateIsSpringLoaded) {
            // Right now we're covered by Apps Customize
            // Show the background gradient immediately, so the gradient will
            // be showing once AppsCustomize disappears
            animateBackgroundGradient(getResources().getInteger(
                    R.integer.config_appsCustomizeSpringLoadedBgAlpha) / 100f, false);
        } else {
            // Fade the background gradient away
            animateBackgroundGradient(0f, true);
        }
        return anim;
    }

    @Override
    public void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace) {
        mIsSwitchingState = true;
        updateChildrenLayersEnabled(false);
        cancelScrollingIndicatorAnimations();
    }

    @Override
    public void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace) {
    }

    @Override
    public void onLauncherTransitionStep(Launcher l, float t) {
        mTransitionProgress = t;
    }

    @Override
    public void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace) {
        mIsSwitchingState = false;
//        mWallpaperOffset.setOverrideHorizontalCatchupConstant(false);
        updateChildrenLayersEnabled(false);
        // The code in getChangeStateAnimation to determine initialAlpha and finalAlpha will ensure
        // ensure that only the current page is visible during (and subsequently, after) the
        // transition animation.  If fade adjacent pages is disabled, then re-enable the page
        // visibility after the transition animation.
        if (!mWorkspaceFadeInAdjacentScreens) {
            for (int i = 0; i < getChildCount(); i++) {
                final CellLayout cl = (CellLayout) getChildAt(i);
                cl.setShortcutAndWidgetAlpha(1f);
            }
        }
    }

    @Override
    public View getContent() {
        return this;
    }

//    void addApplicationShortcut(ShortcutInfo info, CellLayout target, long container, int screen,
//            int cellX, int cellY, boolean insertAtFirst, int intersectX, int intersectY) {
//        View view = mLauncher.createShortcut(R.layout.application, target, (ShortcutInfo) info);
//
//        final int[] cellXY = new int[2];
//        target.findCellForSpanThatIntersects(cellXY, 1, 1, intersectX, intersectY);
//        addInScreen(view, container, screen, cellXY[0], cellXY[1], 1, 1, insertAtFirst);
//        LauncherModel.addOrMoveItemInDatabase(mLauncher, info, container, screen, cellXY[0],
//                cellXY[1]);
//    }

    public void setFinalScrollForPageChange(int screen) {
        if (screen >= 0) {
            mSavedScrollX = getScrollX();
            CellLayout cl = (CellLayout) getChildAt(screen);
            mSavedTranslationX = cl.getTranslationX();
            mSavedRotationY = cl.getRotationY();
            final int newX = getChildOffset(screen) - getRelativeChildOffset(screen);
            setScrollX(newX);
            cl.setTranslationX(0f);
            cl.setRotationY(0f);
        }
    }

    public void resetFinalScrollForPageChange(int screen) {
        if (screen >= 0) {
            CellLayout cl = (CellLayout) getChildAt(screen);
            setScrollX(mSavedScrollX);
            cl.setTranslationX(mSavedTranslationX);
            cl.setRotationY(mSavedRotationY);
        }
    }

    public void getViewLocationRelativeToSelf(View v, int[] location) {
        getLocationInWindow(location);
        int x = location[0];
        int y = location[1];

        v.getLocationInWindow(location);
        int vX = location[0];
        int vY = location[1];

        location[0] = vX - x;
        location[1] = vY - y;
    }

//    static Rect getCellLayoutMetrics(Launcher launcher, int orientation) {
//        Resources res = launcher.getResources();
//        Display display = launcher.getWindowManager().getDefaultDisplay();
//        Point smallestSize = new Point();
//        Point largestSize = new Point();
//        display.getCurrentSizeRange(smallestSize, largestSize);
//        if (orientation == CellLayout.LANDSCAPE) {
//            if (mLandscapeCellLayoutMetrics == null) {
//                int paddingLeft = res.getDimensionPixelSize(R.dimen.workspace_left_padding_land);
//                int paddingRight = res.getDimensionPixelSize(R.dimen.workspace_right_padding_land);
//                int paddingTop = res.getDimensionPixelSize(R.dimen.workspace_top_padding_land);
//                int paddingBottom = res.getDimensionPixelSize(R.dimen.workspace_bottom_padding_land);
//                int width = largestSize.x - paddingLeft - paddingRight;
//                int height = smallestSize.y - paddingTop - paddingBottom;
//                mLandscapeCellLayoutMetrics = new Rect();
//                CellLayout.getMetrics(mLandscapeCellLayoutMetrics, res,
//                        width, height, LauncherModel.getCellCountX(), LauncherModel.getCellCountY(),
//                        orientation);
//            }
//            return mLandscapeCellLayoutMetrics;
//        } else if (orientation == CellLayout.PORTRAIT) {
//            if (mPortraitCellLayoutMetrics == null) {
//                int paddingLeft = res.getDimensionPixelSize(R.dimen.workspace_left_padding_land);
//                int paddingRight = res.getDimensionPixelSize(R.dimen.workspace_right_padding_land);
//                int paddingTop = res.getDimensionPixelSize(R.dimen.workspace_top_padding_land);
//                int paddingBottom = res.getDimensionPixelSize(R.dimen.workspace_bottom_padding_land);
//                int width = smallestSize.x - paddingLeft - paddingRight;
//                int height = largestSize.y - paddingTop - paddingBottom;
//                mPortraitCellLayoutMetrics = new Rect();
//                CellLayout.getMetrics(mPortraitCellLayoutMetrics, res,
//                        width, height, LauncherModel.getCellCountX(), LauncherModel.getCellCountY(),
//                        orientation);
//            }
//            return mPortraitCellLayoutMetrics;
//        }
//        return null;
//    }

//    /*
//    *
//    * Convert the 2D coordinate xy from the parent View's coordinate space to this CellLayout's
//    * coordinate space. The argument xy is modified with the return result.
//    *
//    */
//   void mapPointFromSelfToChild(View v, float[] xy) {
//       mapPointFromSelfToChild(v, xy, null);
//   }

   /*
    *
    * Convert the 2D coordinate xy from the parent View's coordinate space to this CellLayout's
    * coordinate space. The argument xy is modified with the return result.
    *
    * if cachedInverseMatrix is not null, this method will just use that matrix instead of
    * computing it itself; we use this to avoid redundant matrix inversions in
    * findMatchingPageForDragOver
    *
    */
//   void mapPointFromSelfToChild(View v, float[] xy, Matrix cachedInverseMatrix) {
//       if (cachedInverseMatrix == null) {
//           v.getMatrix().invert(mTempInverseMatrix);
//           cachedInverseMatrix = mTempInverseMatrix;
//       }
//       int scrollX = getScrollX();
//       if (mNextPage != INVALID_PAGE) {
//           scrollX = mScroller.getFinalX();
//       }
//       xy[0] = xy[0] + scrollX - v.getLeft();
//       xy[1] = xy[1] + getScrollY() - v.getTop();
//       cachedInverseMatrix.mapPoints(xy);
//   }
//
//
//   void mapPointFromSelfToHotseatLayout(Hotseat hotseat, float[] xy) {
//       hotseat.getLayout().getMatrix().invert(mTempInverseMatrix);
//       xy[0] = xy[0] - hotseat.getLeft() - hotseat.getLayout().getLeft();
//       xy[1] = xy[1] - hotseat.getTop() - hotseat.getLayout().getTop();
//       mTempInverseMatrix.mapPoints(xy);
//   }

   /*
    *
    * Convert the 2D coordinate xy from this CellLayout's coordinate space to
    * the parent View's coordinate space. The argument xy is modified with the return result.
    *
    */
   void mapPointFromChildToSelf(View v, float[] xy) {
       v.getMatrix().mapPoints(xy);
       int scrollX = getScrollX();
       if (mNextPage != INVALID_PAGE) {
           scrollX = mScroller.getFinalX();
       }
       xy[0] -= (scrollX - v.getLeft());
       xy[1] -= (getScrollY() - v.getTop());
   }

   static private float squaredDistance(float[] point1, float[] point2) {
        float distanceX = point1[0] - point2[0];
        float distanceY = point2[1] - point2[1];
        return distanceX * distanceX + distanceY * distanceY;
   }

//    /*
//     *
//     * Returns true if the passed CellLayout cl overlaps with dragView
//     *
//     */
//    boolean overlaps(CellLayout cl, DragView dragView,
//            int dragViewX, int dragViewY, Matrix cachedInverseMatrix) {
//        // Transform the coordinates of the item being dragged to the CellLayout's coordinates
//        final float[] draggedItemTopLeft = mTempDragCoordinates;
//        draggedItemTopLeft[0] = dragViewX;
//        draggedItemTopLeft[1] = dragViewY;
//        final float[] draggedItemBottomRight = mTempDragBottomRightCoordinates;
//        draggedItemBottomRight[0] = draggedItemTopLeft[0] + dragView.getDragRegionWidth();
//        draggedItemBottomRight[1] = draggedItemTopLeft[1] + dragView.getDragRegionHeight();
//
//        // Transform the dragged item's top left coordinates
//        // to the CellLayout's local coordinates
//        mapPointFromSelfToChild(cl, draggedItemTopLeft, cachedInverseMatrix);
//        float overlapRegionLeft = Math.max(0f, draggedItemTopLeft[0]);
//        float overlapRegionTop = Math.max(0f, draggedItemTopLeft[1]);
//
//        if (overlapRegionLeft <= cl.getWidth() && overlapRegionTop >= 0) {
//            // Transform the dragged item's bottom right coordinates
//            // to the CellLayout's local coordinates
//            mapPointFromSelfToChild(cl, draggedItemBottomRight, cachedInverseMatrix);
//            float overlapRegionRight = Math.min(cl.getWidth(), draggedItemBottomRight[0]);
//            float overlapRegionBottom = Math.min(cl.getHeight(), draggedItemBottomRight[1]);
//
//            if (overlapRegionRight >= 0 && overlapRegionBottom <= cl.getHeight()) {
//                float overlap = (overlapRegionRight - overlapRegionLeft) *
//                         (overlapRegionBottom - overlapRegionTop);
//                if (overlap > 0) {
//                    return true;
//                }
//             }
//        }
//        return false;
//    }
//
//    /*
//     *
//     * This method returns the CellLayout that is currently being dragged to. In order to drag
//     * to a CellLayout, either the touch point must be directly over the CellLayout, or as a second
//     * strategy, we see if the dragView is overlapping any CellLayout and choose the closest one
//     *
//     * Return null if no CellLayout is currently being dragged over
//     *
//     */
//    private CellLayout findMatchingPageForDragOver(
//            DragView dragView, float originX, float originY, boolean exact) {
//        // We loop through all the screens (ie CellLayouts) and see which ones overlap
//        // with the item being dragged and then choose the one that's closest to the touch point
//        final int screenCount = getChildCount();
//        CellLayout bestMatchingScreen = null;
//        float smallestDistSoFar = Float.MAX_VALUE;
//
//        for (int i = 0; i < screenCount; i++) {
//            CellLayout cl = (CellLayout) getChildAt(i);
//
//            final float[] touchXy = {originX, originY};
//            // Transform the touch coordinates to the CellLayout's local coordinates
//            // If the touch point is within the bounds of the cell layout, we can return immediately
//            cl.getMatrix().invert(mTempInverseMatrix);
//            mapPointFromSelfToChild(cl, touchXy, mTempInverseMatrix);
//
//            if (touchXy[0] >= 0 && touchXy[0] <= cl.getWidth() &&
//                    touchXy[1] >= 0 && touchXy[1] <= cl.getHeight()) {
//                return cl;
//            }
//
//            if (!exact) {
//                // Get the center of the cell layout in screen coordinates
//                final float[] cellLayoutCenter = mTempCellLayoutCenterCoordinates;
//                cellLayoutCenter[0] = cl.getWidth()/2;
//                cellLayoutCenter[1] = cl.getHeight()/2;
//                mapPointFromChildToSelf(cl, cellLayoutCenter);
//
//                touchXy[0] = originX;
//                touchXy[1] = originY;
//
//                // Calculate the distance between the center of the CellLayout
//                // and the touch point
//                float dist = squaredDistance(touchXy, cellLayoutCenter);
//
//                if (dist < smallestDistSoFar) {
//                    smallestDistSoFar = dist;
//                    bestMatchingScreen = cl;
//                }
//            }
//        }
//        return bestMatchingScreen;
//    }

//    @Override
//    public void getHitRect(Rect outRect) {
//        // We want the workspace to have the whole area of the display (it will find the correct
//        // cell layout to drop to in the existing drag/drop logic.
//        outRect.set(0, 0, mDisplaySize.x, mDisplaySize.y);
//    }

//    /**
//     * Add the item specified by dragInfo to the given layout.
//     * @return true if successful
//     */
//    public boolean addExternalItemToScreen(ItemInfo dragInfo, CellLayout layout) {
//        if (layout.findCellForSpan(mTempEstimate, dragInfo.spanX, dragInfo.spanY)) {
//            onDropExternal(dragInfo.dropPos, (ItemInfo) dragInfo, (CellLayout) layout, false);
//            return true;
//        }
//        mLauncher.showOutOfSpaceMessage(mLauncher.isHotseatLayout(layout));
//        return false;
//    }
//
//    public Bitmap createWidgetBitmap(ItemInfo widgetInfo, View layout) {
//        int[] unScaledSize = mLauncher.getWorkspace().estimateItemSize(widgetInfo.spanX,
//                widgetInfo.spanY, widgetInfo, false);
//        int visibility = layout.getVisibility();
//        layout.setVisibility(VISIBLE);
//
//        int width = MeasureSpec.makeMeasureSpec(unScaledSize[0], MeasureSpec.EXACTLY);
//        int height = MeasureSpec.makeMeasureSpec(unScaledSize[1], MeasureSpec.EXACTLY);
//        Bitmap b = Bitmap.createBitmap(unScaledSize[0], unScaledSize[1],
//                Bitmap.Config.ARGB_8888);
//        Canvas c = new Canvas(b);
//
//        layout.measure(width, height);
//        layout.layout(0, 0, unScaledSize[0], unScaledSize[1]);
//        layout.draw(c);
//        c.setBitmap(null);
//        layout.setVisibility(visibility);
//        return b;
//    }

    public void setFinalTransitionTransform(CellLayout layout) {
        if (isSwitchingState()) {
            int index = indexOfChild(layout);
            mCurrentScaleX = layout.getScaleX();
            mCurrentScaleY = layout.getScaleY();
            mCurrentTranslationX = layout.getTranslationX();
            mCurrentTranslationY = layout.getTranslationY();
            mCurrentRotationY = layout.getRotationY();
            layout.setScaleX(mNewScaleXs[index]);
            layout.setScaleY(mNewScaleYs[index]);
            layout.setTranslationX(mNewTranslationXs[index]);
            layout.setTranslationY(mNewTranslationYs[index]);
            layout.setRotationY(mNewRotationYs[index]);
        }
    }
    public void resetTransitionTransform(CellLayout layout) {
        if (isSwitchingState()) {
            mCurrentScaleX = layout.getScaleX();
            mCurrentScaleY = layout.getScaleY();
            mCurrentTranslationX = layout.getTranslationX();
            mCurrentTranslationY = layout.getTranslationY();
            mCurrentRotationY = layout.getRotationY();
            layout.setScaleX(mCurrentScaleX);
            layout.setScaleY(mCurrentScaleY);
            layout.setTranslationX(mCurrentTranslationX);
            layout.setTranslationY(mCurrentTranslationY);
            layout.setRotationY(mCurrentRotationY);
        }
    }

//    @Override
//    public boolean supportsFlingToDelete() {
//        return true;
//    }
//
//    @Override
//    public void onFlingToDelete(DragObject d, int x, int y, PointF vec) {
//        // Do nothing
//    }
//
//    @Override
//    public void onFlingToDeleteCompleted() {
//        // Do nothing
//    }
//
//    public boolean isDropEnabled() {
//        return true;
//    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
//        Launcher.setScreen(mCurrentPage);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        // We don't dispatch restoreInstanceState to our children using this code path.
        // Some pages will be restored immediately as their items are bound immediately, and 
        // others we will need to wait until after their items are bound.
        mSavedStates = container;
    }

    public void restoreInstanceStateForChild(int child) {
        if (mSavedStates != null) {
            mRestoredPages.add(child);
            CellLayout cl = (CellLayout) getChildAt(child);
            cl.restoreInstanceState(mSavedStates);
        }
    }

    public void restoreInstanceStateForRemainingPages() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            if (!mRestoredPages.contains(i)) {
                restoreInstanceStateForChild(i);
            }
        }
        mRestoredPages.clear();
    }

    @Override
    public void scrollLeft() {
        if (!isSmall() && !mIsSwitchingState) {
            super.scrollLeft();
        }
//        Folder openFolder = getOpenFolder();
//        if (openFolder != null) {
//            openFolder.completeDragExit();
//        }
    }

    @Override
    public void scrollRight() {
        if (!isSmall() && !mIsSwitchingState) {
            super.scrollRight();
        }
//        Folder openFolder = getOpenFolder();
//        if (openFolder != null) {
//            openFolder.completeDragExit();
//        }
    }

//    @Override
//    public boolean onEnterScrollArea(int x, int y, int direction) {
//        // Ignore the scroll area if we are dragging over the hot seat
//        boolean isPortrait = !LauncherApplication.isScreenLandscape(getContext());
//        if (mLauncher.getHotseat() != null && isPortrait) {
//            Rect r = new Rect();
//            mLauncher.getHotseat().getHitRect(r);
//            if (r.contains(x, y)) {
//                return false;
//            }
//        }
//
//        boolean result = false;
//        if (!isSmall() && !mIsSwitchingState) {
//            mInScrollArea = true;
//
//            final int page = getNextPage() +
//                       (direction == DragController.SCROLL_LEFT ? -1 : 1);
//
//            // We always want to exit the current layout to ensure parity of enter / exit
//            setCurrentDropLayout(null);
//
//            if (0 <= page && page < getChildCount()) {
//                CellLayout layout = (CellLayout) getChildAt(page);
//                setCurrentDragOverlappingLayout(layout);
//
//                // Workspace is responsible for drawing the edge glow on adjacent pages,
//                // so we need to redraw the workspace when this may have changed.
//                invalidate();
//                result = true;
//            }
//        }
//        return result;
//    }
//
//    @Override
//    public boolean onExitScrollArea() {
//        boolean result = false;
//        if (mInScrollArea) {
//            invalidate();
//            CellLayout layout = getCurrentDropLayout();
//            setCurrentDropLayout(layout);
//            setCurrentDragOverlappingLayout(layout);
//
//            result = true;
//            mInScrollArea = false;
//        }
//        return result;
//    }
//
//    private void onResetScrollArea() {
//        setCurrentDragOverlappingLayout(null);
//        mInScrollArea = false;
//    }
//
//    /**
//     * Returns a specific CellLayout
//     */
//    CellLayout getParentCellLayoutForView(View v) {
//        ArrayList<CellLayout> layouts = getWorkspaceAndHotseatCellLayouts();
//        for (CellLayout layout : layouts) {
//            if (layout.getShortcutsAndWidgets().indexOfChild(v) > -1) {
//                return layout;
//            }
//        }
//        return null;
//    }
//
//    /**
//     * Returns a list of all the CellLayouts in the workspace.
//     */
//    ArrayList<CellLayout> getWorkspaceAndHotseatCellLayouts() {
//        ArrayList<CellLayout> layouts = new ArrayList<CellLayout>();
//        int screenCount = getChildCount();
//        for (int screen = 0; screen < screenCount; screen++) {
//            layouts.add(((CellLayout) getChildAt(screen)));
//        }
//        if (mLauncher.getHotseat() != null) {
//            layouts.add(mLauncher.getHotseat().getLayout());
//        }
//        return layouts;
//    }

//    /**
//     * We should only use this to search for specific children.  Do not use this method to modify
//     * ShortcutsAndWidgetsContainer directly. Includes ShortcutAndWidgetContainers from
//     * the hotseat and workspace pages
//     */
//    ArrayList<ShortcutAndWidgetContainer> getAllShortcutAndWidgetContainers() {
//        ArrayList<ShortcutAndWidgetContainer> childrenLayouts =
//                new ArrayList<ShortcutAndWidgetContainer>();
//        int screenCount = getChildCount();
//        for (int screen = 0; screen < screenCount; screen++) {
//            childrenLayouts.add(((CellLayout) getChildAt(screen)).getShortcutsAndWidgets());
//        }
//        if (mLauncher.getHotseat() != null) {
//            childrenLayouts.add(mLauncher.getHotseat().getLayout().getShortcutsAndWidgets());
//        }
//        return childrenLayouts;
//    }
//
//    public View getViewForTag(Object tag) {
//        ArrayList<ShortcutAndWidgetContainer> childrenLayouts =
//                getAllShortcutAndWidgetContainers();
//        for (ShortcutAndWidgetContainer layout: childrenLayouts) {
//            int count = layout.getChildCount();
//            for (int i = 0; i < count; i++) {
//                View child = layout.getChildAt(i);
//                if (child.getTag() == tag) {
//                    return child;
//                }
//            }
//        }
//        return null;
//    }

//    // Removes ALL items that match a given package name, this is usually called when a package
//    // has been removed and we want to remove all components (widgets, shortcuts, apps) that
//    // belong to that package.
//    void removeItemsByPackageName(final ArrayList<String> packages, final UserHandle user) {
//        HashSet<String> packageNames = new HashSet<String>();
//        packageNames.addAll(packages);
//
//        // Just create a hash table of all the specific components that this will affect
//        HashSet<ComponentName> cns = new HashSet<ComponentName>();
//        ArrayList<CellLayout> cellLayouts = getWorkspaceAndHotseatCellLayouts();
//        for (CellLayout layoutParent : cellLayouts) {
//            ViewGroup layout = layoutParent.getShortcutsAndWidgets();
//            int childCount = layout.getChildCount();
//            for (int i = 0; i < childCount; ++i) {
//                View view = layout.getChildAt(i);
//                Object tag = view.getTag();
//
//                if (tag instanceof ShortcutInfo) {
//                    ShortcutInfo info = (ShortcutInfo) tag;
//                    ComponentName cn = info.intent.getComponent();
//                    if ((cn != null) && packageNames.contains(cn.getPackageName())
//                            && info.user.equals(user)) {
//                        cns.add(cn);
//                    }
//                } else if (tag instanceof FolderInfo) {
//                    FolderInfo info = (FolderInfo) tag;
//                    for (ShortcutInfo s : info.contents) {
//                        ComponentName cn = s.intent.getComponent();
//                        if ((cn != null) && packageNames.contains(cn.getPackageName())
//                                && info.user.equals(user)) {
//                            cns.add(cn);
//                        }
//                    }
//                } else if (tag instanceof LauncherAppWidgetInfo) {
//                    LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) tag;
//                    ComponentName cn = info.providerName;
//                    if ((cn != null) && packageNames.contains(cn.getPackageName())
//                            && info.user.equals(user)) {
//                        cns.add(cn);
//                    }
//                }
//            }
//        }
//
//        // Remove all the things
//        removeItemsByComponentName(cns, user);
//    }
//
//    // Removes items that match the application info specified, when applications are removed
//    // as a part of an update, this is called to ensure that other widgets and application
//    // shortcuts are not removed.
//    void removeItemsByApplicationInfo(final ArrayList<ApplicationInfo> appInfos, UserHandle user) {
//        // Just create a hash table of all the specific components that this will affect
//        HashSet<ComponentName> cns = new HashSet<ComponentName>();
//        for (ApplicationInfo info : appInfos) {
//            cns.add(info.componentName);
//        }
//
//        // Remove all the things
//        removeItemsByComponentName(cns, user);
//    }
//
//    void removeItemsByComponentName(final HashSet<ComponentName> componentNames,
//            final UserHandle user) {
//        ArrayList<CellLayout> cellLayouts = getWorkspaceAndHotseatCellLayouts();
//        for (final CellLayout layoutParent: cellLayouts) {
//            final ViewGroup layout = layoutParent.getShortcutsAndWidgets();
//
//            // Avoid ANRs by treating each screen separately
//            post(new Runnable() {
//                public void run() {
//                    final ArrayList<View> childrenToRemove = new ArrayList<View>();
//                    childrenToRemove.clear();
//
//                    int childCount = layout.getChildCount();
//                    for (int j = 0; j < childCount; j++) {
//                        final View view = layout.getChildAt(j);
//                        Object tag = view.getTag();
//                        if ((tag instanceof ShortcutInfo || tag instanceof LauncherAppWidgetInfo)
//                                && !((ItemInfo) tag).user.equals(user)) {
//                            continue;
//                        }
//                        if (tag instanceof ShortcutInfo) {
//                            final ShortcutInfo info = (ShortcutInfo) tag;
//                            final Intent intent = info.intent;
//                            final ComponentName name = intent.getComponent();
//
//                            if (name != null) {
//                                if (componentNames.contains(name)) {
//                                    LauncherModel.deleteItemFromDatabase(mLauncher, info);
//                                    childrenToRemove.add(view);
//                                }
//                            }
//                        } else if (tag instanceof FolderInfo) {
//                            final FolderInfo info = (FolderInfo) tag;
//                            final ArrayList<ShortcutInfo> contents = info.contents;
//                            final int contentsCount = contents.size();
//                            final ArrayList<ShortcutInfo> appsToRemoveFromFolder =
//                                    new ArrayList<ShortcutInfo>();
//
//                            for (int k = 0; k < contentsCount; k++) {
//                                final ShortcutInfo appInfo = contents.get(k);
//                                final Intent intent = appInfo.intent;
//                                final ComponentName name = intent.getComponent();
//
//                                if (name != null) {
//                                    if (componentNames.contains(name)
//                                            && user.equals(appInfo.user)) {
//                                        appsToRemoveFromFolder.add(appInfo);
//                                    }
//                                }
//                            }
//                            for (ShortcutInfo item: appsToRemoveFromFolder) {
//                                info.remove(item);
//                                LauncherModel.deleteItemFromDatabase(mLauncher, item);
//                            }
//                        } else if (tag instanceof LauncherAppWidgetInfo) {
//                            final LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) tag;
//                            final ComponentName provider = info.providerName;
//                            if (provider != null) {
//                                if (componentNames.contains(provider)) {
//                                    LauncherModel.deleteItemFromDatabase(mLauncher, info);
//                                    childrenToRemove.add(view);
//                                }
//                            }
//                        }
//                    }
//
//                    childCount = childrenToRemove.size();
//                    for (int j = 0; j < childCount; j++) {
//                        View child = childrenToRemove.get(j);
//                        // Note: We can not remove the view directly from CellLayoutChildren as this
//                        // does not re-mark the spaces as unoccupied.
//                        layoutParent.removeViewInLayout(child);
//                        if (child instanceof DropTarget) {
//                            mDragController.removeDropTarget((DropTarget)child);
//                        }
//                    }
//
//                    if (childCount > 0) {
//                        layout.requestLayout();
//                        layout.invalidate();
//                    }
//                }
//            });
//        }
//
//        // Clean up new-apps animation list
//        final Context context = getContext();
//        post(new Runnable() {
//            @Override
//            public void run() {
//                String spKey = LauncherApplication.getSharedPreferencesKey();
//                SharedPreferences sp = context.getSharedPreferences(spKey,
//                        Context.MODE_PRIVATE);
//                Set<String> newApps = sp.getStringSet(InstallShortcutReceiver.NEW_APPS_LIST_KEY,
//                        null);
//
//                // Remove all queued items that match the same package
//                if (newApps != null) {
//                    synchronized (newApps) {
//                        Iterator<String> iter = newApps.iterator();
//                        while (iter.hasNext()) {
//                            try {
//                                Intent intent = Intent.parseUri(iter.next(), 0);
//                                if (componentNames.contains(intent.getComponent())) {
//                                    iter.remove();
//                                }
//
//                                // It is possible that we've queued an item to be loaded, yet it has
//                                // not been added to the workspace, so remove those items as well.
//                                ArrayList<ItemInfo> shortcuts;
//                                shortcuts = LauncherModel.getWorkspaceShortcutItemInfosWithIntent(
//                                        intent);
//                                for (ItemInfo info : shortcuts) {
//                                    LauncherModel.deleteItemFromDatabase(context, info);
//                                }
//                            } catch (URISyntaxException e) {}
//                        }
//                    }
//                }
//            }
//        });
//    }
//
//    void updateShortcuts(ArrayList<ApplicationInfo> apps) {
//        ArrayList<ShortcutAndWidgetContainer> childrenLayouts = getAllShortcutAndWidgetContainers();
//        for (ShortcutAndWidgetContainer layout: childrenLayouts) {
//            int childCount = layout.getChildCount();
//            for (int j = 0; j < childCount; j++) {
//                final View view = layout.getChildAt(j);
//                Object tag = view.getTag();
//                if (tag instanceof ShortcutInfo) {
//                    ShortcutInfo info = (ShortcutInfo) tag;
//                    // We need to check for ACTION_MAIN otherwise getComponent() might
//                    // return null for some shortcuts (for instance, for shortcuts to
//                    // web pages.)
//                    final Intent intent = info.intent;
//                    final ComponentName name = intent.getComponent();
//                    if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION &&
//                            Intent.ACTION_MAIN.equals(intent.getAction()) && name != null) {
//                        final int appCount = apps.size();
//                        for (int k = 0; k < appCount; k++) {
//                            ApplicationInfo app = apps.get(k);
//                            if (app.componentName.equals(name)) {
//                                BubbleTextView shortcut = (BubbleTextView) view;
//                                info.updateIcon(mIconCache);
//                                info.title = app.title.toString();
//                                shortcut.applyFromShortcutInfo(info, mIconCache);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }

    void moveToDefaultScreen(boolean animate) {
        if (!isSmall()) {
            if (animate) {
                snapToPage(mDefaultPage);
            } else {
                setCurrentPage(mDefaultPage);
            }
        }
        getChildAt(mDefaultPage).requestFocus();
    }

    @Override
    public void syncPages() {
    }

    @Override
    public void syncPageItems(int page, boolean immediate) {
    }

    @Override
    protected String getCurrentPageDescription() {
        int page = (mNextPage != INVALID_PAGE) ? mNextPage : mCurrentPage;
        return String.format(getContext().getString(R.string.workspace_scroll_format),
                page + 1, getChildCount());
    }

    void setFadeForOverScroll(float fade) {
        if (!isScrollingIndicatorEnabled()) return;

        mOverscrollFade = fade;
        float reducedFade = 0.5f + 0.5f * (1 - fade);
        final ViewGroup parent = (ViewGroup) getParent();
//        final ImageView qsbDivider = (ImageView) (parent.findViewById(R.id.qsb_divider));
//        final ImageView dockDivider = (ImageView) (parent.findViewById(R.id.dock_divider));
        final View scrollIndicator = getScrollingIndicator();

        cancelScrollingIndicatorAnimations();
//        if (qsbDivider != null) qsbDivider.setAlpha(reducedFade);
//        if (dockDivider != null) dockDivider.setAlpha(reducedFade);
        scrollIndicator.setAlpha(1 - fade);
    }
}
