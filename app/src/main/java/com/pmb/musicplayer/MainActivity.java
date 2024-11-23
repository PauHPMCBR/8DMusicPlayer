package com.pmb.musicplayer;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
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
    SettingsFragment settingsFragment;
    HelpFragment helpFragment;

    private HRTFFileManager fileManager;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            fileManager = new HRTFFileManager(this);

            if (permission()) {
                fileManager.setupHRTFFiles();
            }
            else {
                requestPermissionDialog();
            }

            // Create an instance of the AudioPlayerFragment
            playerFragment = new PlayerFragment();
            settingsFragment = new SettingsFragment(this);
            helpFragment = new HelpFragment();

            // Use FragmentManager to add the fragment to the container
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, playerFragment, "PlayerFragment")
                    .add(R.id.fragmentContainer, settingsFragment, "SettingsFragment")
                    .add(R.id.fragmentContainer, helpFragment, "HelpFragment")
                    .hide(settingsFragment)  // Hide the settings fragment
                    .commit();
        } else {
            // Restore the fragment references if saved instance state is not null
            playerFragment = (PlayerFragment) getSupportFragmentManager().findFragmentByTag("PlayFragment");
            settingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag("SettingsFragment");
            helpFragment = (HelpFragment) getSupportFragmentManager().findFragmentByTag("HelpFragment");
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                .hide(playerFragment)
                .hide(settingsFragment)
                .hide(helpFragment);
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            transaction.show(settingsFragment).commit();
            return true;
        }
        else if (id == R.id.action_player) {
            transaction.show(playerFragment).commit();
            return true;
        }
        else if (id == R.id.action_help) {
            transaction.show(helpFragment).commit();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!permission()) {
            requestPermissionDialog();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        OpenALManager.cleanupOpenAL();
    }

    public boolean permission() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int write = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
            return write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED;
        }
    }

    public void requestPermissionDialog() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            ActivityResultLauncher<Intent> permissionsActivityResultLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            fileManager.setupHRTFFiles();
                        }
                    });
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                permissionsActivityResultLauncher.launch(intent);
            } catch (Exception e) {
                Intent obj = new Intent();
                obj.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                permissionsActivityResultLauncher.launch(obj);
            }
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fileManager.setupHRTFFiles();
            } else {
                Toast.makeText(this, "Storage permission is required", Toast.LENGTH_LONG).show();
            }
        }
    }
}
