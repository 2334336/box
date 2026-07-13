package com.github.tvbox.osc.bbox.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.github.tvbox.osc.bbox.ui.activity.HomeActivity;
import com.github.tvbox.osc.bbox.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

public class BootReceiver extends BroadcastReceiver {
    private static final String ACTION_AUTO_START = "tv.org.eu.bunnyabc.action.AUTO_START";
    private static final long[] START_DELAYS_MILLIS = {3_000L, 8_000L, 18_000L, 33_000L};
    private static final int START_REQUEST_CODE = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && action.startsWith(ACTION_AUTO_START)) {
            if (Hawk.get(HawkConfig.AUTO_START, false)) {
                launchHome(context);
            }
            return;
        }
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)
                && !"com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
            return;
        }
        if (!Hawk.get(HawkConfig.AUTO_START, false)) {
            cancelPendingRetries(context);
            return;
        }

        cancelPendingRetries(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        for (int i = 0; i < START_DELAYS_MILLIS.length; i++) {
            scheduleRetry(context, alarmManager, i, START_DELAYS_MILLIS[i]);
        }
    }

    private void launchHome(Context context) {
        PendingResult pendingResult = goAsync();
        Context appContext = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Settings.canDrawOverlays(appContext)) {
            launchHomeWithOverlay(appContext, pendingResult);
            return;
        }
        try {
            appContext.startActivity(createLaunchIntent(appContext));
        } finally {
            pendingResult.finish();
        }
    }

    private void launchHomeWithOverlay(Context context, PendingResult pendingResult) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            launchHomeWithoutOverlay(context, pendingResult);
            return;
        }

        View overlay = new View(context);
        overlay.setBackgroundColor(Color.argb(1, 0, 0, 0));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                1,
                1,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.alpha = 0.01f;

        try {
            windowManager.addView(overlay, params);
        } catch (RuntimeException e) {
            launchHomeWithoutOverlay(context, pendingResult);
            return;
        }

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            try {
                context.startActivity(createLaunchIntent(context));
            } finally {
                handler.postDelayed(() -> {
                    try {
                        windowManager.removeView(overlay);
                    } catch (RuntimeException ignored) {
                    }
                    pendingResult.finish();
                }, 800L);
            }
        }, 300L);
    }

    private void launchHomeWithoutOverlay(Context context, PendingResult pendingResult) {
        try {
            context.startActivity(createLaunchIntent(context));
        } finally {
            pendingResult.finish();
        }
    }

    private void scheduleRetry(Context context, AlarmManager alarmManager, int attempt, long delayMillis) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                START_REQUEST_CODE + attempt,
                createRetryIntent(context, attempt),
                pendingIntentFlags(PendingIntent.FLAG_CANCEL_CURRENT));
        long triggerAt = SystemClock.elapsedRealtime() + delayMillis;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent);
        }
    }

    public static void cancelPendingRetries(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        for (int i = 0; i < START_DELAYS_MILLIS.length; i++) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    START_REQUEST_CODE + i,
                    createRetryIntent(context, i),
                    pendingIntentFlags(PendingIntent.FLAG_NO_CREATE));
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    private static Intent createRetryIntent(Context context, int attempt) {
        Intent retry = new Intent(context, BootReceiver.class);
        retry.setAction(ACTION_AUTO_START + "." + attempt);
        return retry;
    }

    private static Intent createLaunchIntent(Context context) {
        Intent launch = new Intent(context, HomeActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return launch;
    }

    private static int pendingIntentFlags(int flags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return flags | PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }
}
