package com.pmb.musicplayer;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

import com.pmb.musicplayer.player.PlayerFragment;
import com.pmb.openal.OpenALManager;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int STORAGE_PERMISSION_CODE = 1001;

    PlayerFragment playerFragment;
    private SettingsFragment settingsFragment;
    private HelpFragment helpFragment;
    private HRTFFileManager fileManager;
    private ActivityResultLauncher<Intent> permissionsActivityResultLauncher;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupPermissionLauncher();

        if (savedInstanceState == null) {
            fileManager = new HRTFFileManager(this);
            initializeApp();
        } else {
            restoreFragments();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupPermissionLauncher() {
        permissionsActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            onPermissionsGranted();
                        } else {
                            onPermissionsDenied();
                        }
                    }
                }
        );
    }

    private void initializeApp() {
        if (!hasIOPermissions()) {
            showPermissionExplanationDialog();
        } else {
            setupAppComponents();
        }
    }

    private void setupAppComponents() {
        fileManager.setupHRTFFiles();

        // Create fragments
        playerFragment = new PlayerFragment();
        settingsFragment = new SettingsFragment(this);
        helpFragment = new HelpFragment();

        // Add fragments to container
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, playerFragment, "PlayerFragment")
                .add(R.id.fragmentContainer, settingsFragment, "SettingsFragment")
                .add(R.id.fragmentContainer, helpFragment, "HelpFragment")
                .hide(settingsFragment)
                .hide(helpFragment)
                .commit();
    }

    private void restoreFragments() {
        playerFragment = (PlayerFragment) getSupportFragmentManager().findFragmentByTag("PlayerFragment");
        settingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag("SettingsFragment");
        helpFragment = (HelpFragment) getSupportFragmentManager().findFragmentByTag("HelpFragment");
    }

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Storage Permission Required")
                .setMessage("This app needs access to storage to load and play music files. Without this permission, the app cannot function properly.")
                .setPositiveButton("Grant Permission", (dialog, which) -> requestPermissionDialog())
                .setNegativeButton("Exit App", (dialog, which) -> finishAffinity())
                .setCancelable(false)
                .show();
    }

    private void onPermissionsGranted() {
        setupAppComponents();
        Toast.makeText(this, "Permissions granted, initializing app", Toast.LENGTH_SHORT).show();
    }

    private void onPermissionsDenied() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app cannot function without storage access. Would you like to grant permissions now?")
                .setPositiveButton("Try Again", (dialog, which) -> requestPermissionDialog())
                .setNegativeButton("Exit App", (dialog, which) -> finishAffinity())
                .setCancelable(false)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (!hasIOPermissions()) {
            showPermissionExplanationDialog();
            return true;
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                .hide(playerFragment)
                .hide(settingsFragment)
                .hide(helpFragment);

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            transaction.show(settingsFragment).commit();
            return true;
        } else if (id == R.id.action_player) {
            transaction.show(playerFragment).commit();
            return true;
        } else if (id == R.id.action_help) {
            transaction.show(helpFragment).commit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!hasIOPermissions()) {
            showPermissionExplanationDialog();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        OpenALManager.cleanupOpenAL();
    }

    private boolean hasIOPermissions() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int write = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
            return write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissionDialog() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                permissionsActivityResultLauncher.launch(intent);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                permissionsActivityResultLauncher.launch(intent);
            }
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPermissionsGranted();
            } else {
                onPermissionsDenied();
            }
        }
    }
}
