package com.pmb.musicplayer;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.pmb.musicplayer.player.PlayerFragment;
import com.pmb.openal.OpenALManager;

public class MainActivity extends AppCompatActivity {
    PlayerFragment playerFragment;
    SettingsFragment settingsFragment;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            // Create an instance of the AudioPlayerFragment
            playerFragment = new PlayerFragment();
            settingsFragment = new SettingsFragment(this);

            // Use FragmentManager to add the fragment to the container
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, playerFragment, "PlayerFragment")
                    .add(R.id.fragmentContainer, settingsFragment, "SettingsFragment")
                    .hide(settingsFragment)  // Hide the settings fragment
                    .commit();
        } else {
            // Restore the fragment references if saved instance state is not null
            playerFragment = (PlayerFragment) getSupportFragmentManager().findFragmentByTag("PlayFragment");
            settingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag("SettingsFragment");
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
        // Handle toolbar item clicks here
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            getSupportFragmentManager().beginTransaction()
                    .hide(playerFragment)
                    .show(settingsFragment)
                    .commit();
            return true;
        }
        else if (id == R.id.action_player) {
            getSupportFragmentManager().beginTransaction()
                    .hide(settingsFragment)
                    .show(playerFragment)
                    .commit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!permission()) {
            RequestPermission_Dialog();
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

    public void RequestPermission_Dialog() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", new Object[]{getApplicationContext().getPackageName()})));
                startActivityForResult(intent, 2000);
            } catch (Exception e) {
                Intent obj = new Intent();
                obj.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(obj, 2000);
            }
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, 1);
        }
    }

}

/* THINGS THE PROGRAM WILL HAVE:
- gui like the unity one (circle with vertical slider at the bottom)
- load several music at once, "separately" (different position points) (load all files in one folder, warning if 6+)
- stop stops all music
- normal folder playing (copy one music player?)
- ability to pause, resume, next, previous, choose timestamp, playback speed
- path descriptor (strict and with randomness) + way to save it (so need for file storage)
- manual and automatic paths

- advanced settings (position/distance fine tuning)
- hrtf selection (for better customization)
- visual: show album cover or whatever somewhere
- appearance customization
- equaliser
- playlists
- ability to separate tracks inside the program

SCREENS:
    position controller
    list of queued music
    file explorer
    settings / preferences
    favourites?
    youtube?
 */

