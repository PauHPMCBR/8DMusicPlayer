package com.pmb.musicplayer;

import static android.os.Build.VERSION.SDK_INT;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;

import com.pmb.musicplayer.player.PlayerFragment;
import com.pmb.openal.OpenALManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat {
    final static String ALSOFT_CONF_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/alsoft.conf";
    final static String HRTF_PATH = "/storage/emulated/0/hrtf";
    final static CharSequence[] AVAILABLE_REVERBS = {"None", "Generic", "PaddedCell", "Room", "Bathroom",
            "Livingroom", "Stoneroom", "Auditorium", "ConcertHall", "Cave", "Arena", "Hangar",
            "CarpetedHallway", "Hallway", "StoneCorridor", "Alley", "Forest", "City", "Mountains",
            "Quarry", "Plain", "ParkingLot", "SewerPipe", "Underwater", "Drugged", "Dizzy", "Psychotic"};

    static String selectedHRTF;
    static String selectedReverb;
    static float selectedMaxRadius;
    static float selectedStereoAngle;
    static boolean showHiddenFiles;
    static boolean showOnlyAudioFiles;

    final MainActivity mainActivity;

    public SettingsFragment(MainActivity mainActivity) {
        super();
        this.mainActivity = mainActivity;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        ListPreference selectedHrtfPref = findPreference("selectedHrtf");
        if (selectedHrtfPref != null) {
            File hrtfFolder = new File(HRTF_PATH);
            File[] hrtfFiles = hrtfFolder.listFiles();
            assert hrtfFiles != null;
            CharSequence[] hrtfNames = Arrays.stream(hrtfFiles)
                    .map(File::getName)
                    .map(name -> name.substring(0, name.lastIndexOf('.')))
                    .toArray(CharSequence[]::new);

            selectedHrtfPref.setDefaultValue(hrtfNames[0]);
            selectedHrtfPref.setEntries(hrtfNames);
            selectedHrtfPref.setEntryValues(hrtfNames);
            selectedHrtfPref.setOnPreferenceChangeListener((preference, newValue) -> {
                Log.d("Preferences", String.format("Selected HRTF: %s", newValue));
                selectedHRTF = (String) newValue;
                reloadOpenAL();
                return true;
            });

            selectedHRTF = selectedHrtfPref.getValue();
        }

        ListPreference selectedReverbPref = findPreference("selectedReverb");
        if (selectedReverbPref != null) {
            selectedReverbPref.setDefaultValue(AVAILABLE_REVERBS[0]);
            selectedReverbPref.setEntries(AVAILABLE_REVERBS);
            selectedReverbPref.setEntryValues(AVAILABLE_REVERBS);
            selectedReverbPref.setOnPreferenceChangeListener((preference, newValue) -> {
                Log.d("Preferences", String.format("Selected Reverb: %s", newValue));
                selectedReverb = (String) newValue;
                writeAlsoftConf();
                Toast.makeText(mainActivity, "Restarting...", Toast.LENGTH_SHORT).show();
                new Handler(Looper.getMainLooper()).postDelayed(this::reloadApplication, 300); // Delay to ensure writeAlsoftConf completes
                return true;
            });

            selectedReverb = selectedReverbPref.getValue();
        }

        EditTextPreference maxRadiusPref = findPreference("maxRadius");
        if (maxRadiusPref != null) {
            maxRadiusPref.setOnPreferenceChangeListener((preference, newValue) -> {
                float newMaxRadius;
                try {
                    newMaxRadius = Float.parseFloat((String) newValue);
                    if (newMaxRadius <= 0) throw new Exception("Non-positive max radius");
                } catch (Exception e) {
                    Log.d("Preferences", String.format("Invalid selected max radius: %s", newValue));
                    return false;
                }
                Log.d("Preferences", String.format("Selected max radius: %s", newValue));

                selectedMaxRadius = newMaxRadius;
                PlayerFragment.MAX_RADIUS = selectedMaxRadius;
                mainActivity.playerFragment.setupSeekBarListeners(); //update display
                return true;
            });

            selectedMaxRadius = Float.parseFloat(Objects.requireNonNull(maxRadiusPref.getText()));
        }

        SeekBarPreference stereoAnglePref = findPreference("stereoAngle");
        if (stereoAnglePref != null) {
            stereoAnglePref.setOnPreferenceChangeListener((preference, newValue) -> {
                if (stereoAnglePref.getValue() == (int)newValue) return false;
                float newAngle = (float)((int)newValue) / 100f;
                stereoAnglePref.setSummary("" + newAngle);
                selectedStereoAngle = newAngle;
                OpenALManager.setStereoAngle(selectedStereoAngle);
                return true;
            });

            selectedStereoAngle = (float)stereoAnglePref.getValue() / 100f;
        }

        CheckBoxPreference showOnlyAudioFilesPref = findPreference("showOnlyAudioFiles");
        if (showOnlyAudioFilesPref != null) {
            showOnlyAudioFilesPref.setOnPreferenceChangeListener((preference, newValue) -> {
                showOnlyAudioFiles = (boolean) newValue;
                return true;
            });

            showOnlyAudioFiles = showOnlyAudioFilesPref.isChecked();
        }

        CheckBoxPreference showHiddenFilesPref = findPreference("showHiddenFiles");
        if (showHiddenFilesPref != null) {
            showHiddenFilesPref.setOnPreferenceChangeListener((preference, newValue) -> {
                showHiddenFiles = (boolean) newValue;
                return true;
            });

            showHiddenFiles = showHiddenFilesPref.isChecked();
        }


        Map<String, ?> preferences = PreferenceManager.getDefaultSharedPreferences(requireContext()).getAll();

        preferences.forEach((key, value) ->{
            Log.d("Preferences", String.format("%s -> %s", key, value));
        });

        checkAndCreateAlsoftConf();
        PlayerFragment.MAX_RADIUS = selectedMaxRadius;
        if (!OpenALManager.initOpenAL(selectedHRTF)) {
            Log.d("Preferences", "Failed to initialize OpenAL");
        }
        OpenALManager.setStereoAngle(selectedStereoAngle);
    }

    void reloadApplication() {
        Intent intent = mainActivity.getPackageManager().getLaunchIntentForPackage(mainActivity.getPackageName());
        assert intent != null;
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // To clear the current activity stack
        startActivity(intent);
        System.exit(0);  // Optional, to ensure the app closes completely
    }

    static void reloadOpenAL() {
        OpenALManager.stopMusic();
        OpenALManager.cleanupOpenAL();
        OpenALManager.initOpenAL(selectedHRTF);
    }

    static void writeAlsoftConf() {
        try {
            File file = new File(ALSOFT_CONF_PATH);
            FileWriter writer = new FileWriter(file);
            writer.write("hrtf-paths = " + HRTF_PATH + '\n');
            writer.write("default-reverb = " + selectedReverb + '\n');
            writer.write("nfc = true" + '\n'); //check if that even does anything
            writer.close();
            Log.d("Preferences", "Alsoft conf modified successfully: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e("Preferences", "Error modifying Alsoft conf: " + e.getMessage(), e);
        }
    }
    static void checkAndCreateAlsoftConf() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Log.d("Preferences", "no MANAGE_ALL_FILES_ACCESS_PERMISSION");
            }
        }

        File file = new File(ALSOFT_CONF_PATH);

        if (!file.exists()) {
            File directory = file.getParentFile();
            assert directory != null;
            if (!directory.exists()) {
                boolean dirCreated = directory.mkdirs();
                if (!dirCreated) {
                    Log.d("Preferences", "Failed to create directory: " + directory.getAbsolutePath());
                    Log.d("Preferences", "Directory exists: " + directory.exists());
                    Log.d("Preferences", "Directory is writable: " + directory.canWrite());
                    return;
                }
            }

            try {
                boolean fileCreated = file.createNewFile();
                if (fileCreated) {
                    writeAlsoftConf();
                } else {
                    Log.d("Preferences", "Failed to create file: " + file.getAbsolutePath());
                }
            } catch (IOException e) {
                Log.e("Preferences", "Error creating file: " + e.getMessage(), e);
            }
        } else {
            Log.d("Preferences", "File already exists: " + file.getAbsolutePath());
            writeAlsoftConf();
        }
    }
}
