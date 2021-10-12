package com.cncoderx.launcher;

import android.app.Application;

import com.cncoderx.launcher.module.workspace.LauncherProvider;

public class LauncherApplication extends Application {
    private LauncherProvider launcherProvider;

    public LauncherProvider getLauncherProvider() {
        return launcherProvider;
    }

    public void setLauncherProvider(LauncherProvider launcherProvider) {
        this.launcherProvider = launcherProvider;
    }
}
