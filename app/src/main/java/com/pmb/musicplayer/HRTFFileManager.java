package com.pmb.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HRTFFileManager {
    private static final String TAG = "HRTFFileManager";
    private static final String PREFS_NAME = "hrtf_prefs";
    private static final String FIRST_RUN_KEY = "is_first_run";

    private final WeakReference<Context> contextRef;
    private final ExecutorService executor;

    public HRTFFileManager(Context context) {
        this.contextRef = new WeakReference<>(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void setupHRTFFiles() {
        executor.execute(() -> {
            Context context = contextRef.get();
            if (context == null) {
                Log.e(TAG, "Error setting up HRTF files: Context is null");
                return;
            }

            if (!isFirstRun()) {
                Log.d(TAG, "HRTF files setup completed successfully");
                return;
            }

            try {
                // Create output directory
                File outputDir = new File(Environment.getExternalStorageDirectory(), "hrtf");
                if (!outputDir.exists()) {
                    boolean created = outputDir.mkdirs();
                    if (!created) {
                        throw new IOException("Failed to create output directory");
                    }
                }

                // Get list of files from assets
                String[] files = context.getAssets().list("hrtfs");
                if (files == null || files.length == 0) {
                    throw new IOException("No HRTF files found in assets");
                }

                // Copy each file
                for (String fileName : files) {
                    File outputFile = new File(outputDir, fileName);

                    // Skip if file already exists
                    if (outputFile.exists()) {
                        continue;
                    }

                    // Copy file from assets to external storage
                    try (InputStream input = context.getAssets().open("hrtfs/" + fileName);
                         OutputStream output = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[4096];
                        int read;
                        while ((read = input.read(buffer)) != -1) {
                            output.write(buffer, 0, read);
                        }
                    }
                }

                markFirstRunComplete();
                Log.d(TAG, "HRTF files setup completed successfully");

            } catch (Exception e) {
                Log.e(TAG, "Error setting up HRTF files", e);
            }
        });
    }

    private boolean isFirstRun() {
        Context context = contextRef.get();
        if (context == null) return false;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(FIRST_RUN_KEY, true);
    }

    private void markFirstRunComplete() {
        Context context = contextRef.get();
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(FIRST_RUN_KEY, false).apply();
    }
}
