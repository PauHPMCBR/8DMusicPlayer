package com.pmb.musicplayer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import io.noties.markwon.Markwon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HelpFragment extends Fragment {
    private static final String TAG = "HelpFragment";
    private static final String HELP_FILE = "help.md";

    private TextView markdownView;
    private Markwon markwon;

    public static HelpFragment newInstance() {
        return new HelpFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize basic Markwon
        markwon = Markwon.create(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help, container, false);
        markdownView = view.findViewById(R.id.markdown_view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadHelpContent();
    }

    private void loadHelpContent() {
        try {
            String markdownContent = readFromAssets(HELP_FILE);
            markwon.setMarkdown(markdownView, markdownContent);
        } catch (IOException e) {
            Log.e(TAG, "Error loading help content", e);
            showError("Error loading help content");
        }
    }

    private String readFromAssets(String fileName) throws IOException {
        StringBuilder buffer = new StringBuilder();
        try (InputStream inputStream = requireContext().getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append('\n');
            }
        }
        return buffer.toString();
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }
}