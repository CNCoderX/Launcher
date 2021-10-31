
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
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Default launcher application.
 */
public final class Launcher extends Activity implements LauncherModel.Callbacks {
    static final String TAG = Launcher.class.getSimpleName();

    private FrameLayout mLauncherView;
    private Workspace mWorkspace;

    private Bundle mSavedState;

    static final int SCREEN_COUNT = 5;
    static final int DEFAULT_SCREEN = 2;

    private boolean mPaused = true;
    private boolean mOnResumeNeedsLoad;

    private int mNewShortcutAnimatePage = -1;
    private ArrayList<View> mNewShortcutAnimateViews = new ArrayList<>();

    private boolean mVisible = false;
    private boolean mWorkspaceLoading = true;

    private SharedPreferences mSharedPrefs;
    private LayoutInflater mInflater;

    private ArrayList<Runnable> mOnResumeCallbacks = new ArrayList<Runnable>();
    private static ArrayList<PendingAddArguments> sPendingAddList = new ArrayList<PendingAddArguments>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }

        setContentView(R.layout.launcher);

        mSavedState = savedInstanceState;
        restoreState(mSavedState);

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mInflater = LayoutInflater.from(this);

        mLauncherView = findViewById(R.id.launcher);
        mWorkspace = mLauncherView.findViewById(R.id.workspace);

        mLauncherView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        mWorkspace.setHapticFeedbackEnabled(false);
    }

    private boolean waitUntilResume(Runnable run, boolean deletePreviousRunnables) {
        if (mPaused) {
            Log.i(TAG, "Deferring update until onResume");
            if (deletePreviousRunnables) {
                while (mOnResumeCallbacks.remove(run)) {
                }
            }
            mOnResumeCallbacks.add(run);
            return true;
        } else {
            return false;
        }
    }

    private boolean waitUntilResume(Runnable run) {
        return waitUntilResume(run, false);
    }

    @Override
    public boolean setLoadOnResume() {
        if (mPaused) {
            Log.i(TAG, "setLoadOnResume");
            mOnResumeNeedsLoad = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public int getCurrentWorkspaceScreen() {
        if (mWorkspace != null) {
            return mWorkspace.getCurrentPage();
        } else {
            return SCREEN_COUNT / 2;
        }
    }

    /**
     * Refreshes the shortcuts shown on the workspace.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void startBinding() {
        // If we're starting binding all over again, clear any bind calls we'd postponed in
        // the past (see waitUntilResume) -- we don't need them since we're starting binding
        // from scratch again
        mOnResumeCallbacks.clear();

        final Workspace workspace = mWorkspace;
        mNewShortcutAnimatePage = -1;
        mNewShortcutAnimateViews.clear();

        int count = workspace.getChildCount();
        for (int i = 0; i < count; i++) {
            // Use removeAllViewsInLayout() to avoid an extra requestLayout() and invalidate().
            final CellLayout layoutParent = (CellLayout) workspace.getChildAt(i);
            layoutParent.removeAllViewsInLayout();
        }
        mWidgetsToAdvance.clear();
    }

    /**
     * Bind the items start-end from the list.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void bindItems(final ArrayList<ItemInfo> shortcuts, final int start, final int end) {
        if (waitUntilResume(new Runnable() {
            public void run() {
                bindItems(shortcuts, start, end);
            }
        })) {
            return;
        }

        // Get the list of added shortcuts and intersect them with the set of shortcuts here
        Set<String> newApps = new HashSet<String>();
        newApps = mSharedPrefs.getStringSet(InstallShortcutReceiver.NEW_APPS_LIST_KEY, newApps);

        Workspace workspace = mWorkspace;
        for (int i = start; i < end; i++) {
            final ItemInfo item = shortcuts.get(i);

            ShortcutInfo info = (ShortcutInfo) item;
            String uri = info.intent.toUri(0);
            View shortcut = createShortcut(info);
            workspace.addInScreen(shortcut, item.screen, item.cellX, item.cellY, 1, 1, false);
            boolean animateIconUp = false;
            synchronized (newApps) {
                if (newApps.contains(uri)) {
                    animateIconUp = newApps.remove(uri);
                }
            }
            if (animateIconUp) {
                // Prepare the view to be animated up
                shortcut.setAlpha(0f);
                shortcut.setScaleX(0f);
                shortcut.setScaleY(0f);
                mNewShortcutAnimatePage = item.screen;
                if (!mNewShortcutAnimateViews.contains(shortcut)) {
                    mNewShortcutAnimateViews.add(shortcut);
                }
            }
        }

        workspace.requestLayout();
    }

    @Override
    public void onPageBoundSynchronously(int page) {
        mSynchronouslyBoundPages.add(page);
    }

    /**
     * Callback saying that there aren't any more items to bind.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void finishBindingItems() {
        if (waitUntilResume(new Runnable() {
            public void run() {
                finishBindingItems();
            }
        })) {
            return;
        }
        if (mSavedState != null) {
            if (!mWorkspace.hasFocus()) {
                mWorkspace.getChildAt(mWorkspace.getCurrentPage()).requestFocus();
            }
            mSavedState = null;
        }

        mWorkspace.restoreInstanceStateForRemainingPages();

        // If we received the result of any pending adds while the loader was running (e.g. the
        // widget configuration forced an orientation change), process them now.
        for (int i = 0; i < sPendingAddList.size(); i++) {
            completeAdd(sPendingAddList.get(i));
        }
        sPendingAddList.clear();

        // Update the market app icon as necessary (the other icons will be managed in response to
        // package changes in bindSearchablesChanged()
//        updateAppMarketIcon();

        // Animate up any icons as necessary
        if (mVisible || mWorkspaceLoading) {
            Runnable newAppsRunnable = new Runnable() {
                @Override
                public void run() {
                    runNewAppsAnimation(false);
                }
            };

            boolean willSnapPage = mNewShortcutAnimatePage > -1 &&
                    mNewShortcutAnimatePage != mWorkspace.getCurrentPage();
            if (canRunNewAppsAnimation()) {
                // If the user has not interacted recently, then either snap to the new page to show
                // the new-apps animation or just run them if they are to appear on the current page
                if (willSnapPage) {
                    mWorkspace.snapToPage(mNewShortcutAnimatePage, newAppsRunnable);
                } else {
                    runNewAppsAnimation(false);
                }
            } else {
                // If the user has interacted recently, then just add the items in place if they
                // are on another page (or just normally if they are added to the current page)
                runNewAppsAnimation(willSnapPage);
            }
        }

        mWorkspaceLoading = false;
    }

    private boolean canRunNewAppsAnimation() {
//        long diff = System.currentTimeMillis() - mDragController.getLastGestureUpTime();
//        return diff > (NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS * 1000);

        return true;
    }

    /**
     * Runs a new animation that scales up icons that were added while Launcher was in the
     * background.
     *
     * @param immediate whether to run the animation or show the results immediately
     */
    private void runNewAppsAnimation(boolean immediate) {
        AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
        Collection<Animator> bounceAnims = new ArrayList<Animator>();

        // Order these new views spatially so that they animate in order
        Collections.sort(mNewShortcutAnimateViews, new Comparator<View>() {
            @Override
            public int compare(View a, View b) {
                CellLayout.LayoutParams alp = (CellLayout.LayoutParams) a.getLayoutParams();
                CellLayout.LayoutParams blp = (CellLayout.LayoutParams) b.getLayoutParams();
                int cellCountX = LauncherModel.getCellCountX();
                return (alp.cellY * cellCountX + alp.cellX) - (blp.cellY * cellCountX + blp.cellX);
            }
        });

        // Animate each of the views in place (or show them immediately if requested)
        if (immediate) {
            for (View v : mNewShortcutAnimateViews) {
                v.setAlpha(1f);
                v.setScaleX(1f);
                v.setScaleY(1f);
            }
        } else {
            for (int i = 0; i < mNewShortcutAnimateViews.size(); ++i) {
                View v = mNewShortcutAnimateViews.get(i);
                ValueAnimator bounceAnim = LauncherAnimUtils.ofPropertyValuesHolder(v,
                        PropertyValuesHolder.ofFloat("alpha", 1f),
                        PropertyValuesHolder.ofFloat("scaleX", 1f),
                        PropertyValuesHolder.ofFloat("scaleY", 1f));
                bounceAnim.setDuration(InstallShortcutReceiver.NEW_SHORTCUT_BOUNCE_DURATION);
                bounceAnim.setStartDelay(i * InstallShortcutReceiver.NEW_SHORTCUT_STAGGER_DELAY);
                bounceAnim.setInterpolator(new SmoothPagedView.OvershootInterpolator());
                bounceAnims.add(bounceAnim);
            }
            anim.playTogether(bounceAnims);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mWorkspace != null) {
                        mWorkspace.postDelayed(mBuildLayersRunnable, 500);
                    }
                }
            });
            anim.start();
        }

        // Clean up
        mNewShortcutAnimatePage = -1;
        mNewShortcutAnimateViews.clear();
        new Thread("clearNewAppsThread") {
            public void run() {
                mSharedPrefs.edit()
                        .putInt(InstallShortcutReceiver.NEW_APPS_PAGE_KEY, -1)
                        .putStringSet(InstallShortcutReceiver.NEW_APPS_LIST_KEY, null)
                        .commit();
            }
        }.start();
    }

    @Override
    public void bindSearchablesChanged() {
//        boolean searchVisible = updateGlobalSearchIcon();
//        boolean voiceVisible = updateVoiceSearchIcon(searchVisible);
//        if (mSearchDropTargetBar != null) {
//            mSearchDropTargetBar.onSearchPackagesChanged(searchVisible, voiceVisible);
//        }
    }

    View createShortcut(ShortcutInfo info) {
        return createShortcut(R.layout.app_widget,
                (ViewGroup) mWorkspace.getChildAt(mWorkspace.getCurrentPage()), info);
    }

    View createShortcut(int layoutResId, ViewGroup parent, ShortcutInfo info) {
        return mInflater.inflate(layoutResId, parent, false);
//        BubbleTextView favorite = (BubbleTextView) mInflater.inflate(layoutResId, parent, false);
//        favorite.applyFromShortcutInfo(info, mIconCache);
//        favorite.setOnClickListener(this);
//        return favorite;
    }
}

interface LauncherTransitionable {
    View getContent();
    void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace);
    void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace);
    void onLauncherTransitionStep(Launcher l, float t);
    void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace);
}
