package com.tyron.code;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StartupManager {

  private final ArrayDeque<Runnable> mMainThreadActivities;
  private final ArrayDeque<Runnable> mBackgroundActivities;
  private final ExecutorService mExecutor;
  private final Handler mMainHandler;

  public StartupManager() {
    mMainThreadActivities = new ArrayDeque<>();
    mBackgroundActivities = new ArrayDeque<>();
    mExecutor = Executors.newSingleThreadExecutor();
    mMainHandler = new Handler(Looper.getMainLooper());
  }

  public void addStartupActivity(Runnable runnable) {
    mMainThreadActivities.add(runnable);
  }

  public void addBackgroundActivity(Runnable runnable) {
    mBackgroundActivities.add(runnable);
  }

  public void startup() {
    while (!mMainThreadActivities.isEmpty()) {
      mMainThreadActivities.remove().run();
    }

    mExecutor.execute(() -> {
      while (!mBackgroundActivities.isEmpty()) {
        mBackgroundActivities.remove().run();
      }
      mExecutor.shutdown();
    });
  }
}