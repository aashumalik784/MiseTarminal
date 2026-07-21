package com.aashumalik.misetarminal;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import android.app.Activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Mise Tarminal - a small original terminal-style shell runner.
 * This does not use any Termux code. It launches the device's own
 * shell (/system/bin/sh) and streams its output into a scrolling
 * text view, similar in spirit to a basic terminal emulator.
 */
public class TerminalActivity extends Activity {

    private TextView outputText;
    private EditText inputField;
    private ScrollView scrollView;

    private Process shellProcess;
    private OutputStream shellInput;
    private String currentDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        outputText = findViewById(R.id.outputText);
        inputField = findViewById(R.id.inputField);
        scrollView = findViewById(R.id.scrollView);
        outputText.setMovementMethod(new ScrollingMovementMethod());

        currentDir = getFilesDir().getAbsolutePath();

        startShell();

        inputField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                sendCommand(inputField.getText().toString());
                inputField.setText("");
                return true;
            }
            return false;
        });
    }

    private void startShell() {
        try {
            ProcessBuilder pb = new ProcessBuilder("/system/bin/sh");
            pb.redirectErrorStream(true);
            pb.directory(new File(currentDir));
            shellProcess = pb.start();
            shellInput = shellProcess.getOutputStream();

            Thread readerThread = new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(shellProcess.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        appendOutput(line + "\n");
                    }
                } catch (Exception e) {
                    appendOutput("[shell ended: " + e.getMessage() + "]\n");
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

        } catch (Exception e) {
            appendOutput("Failed to start shell: " + e.getMessage() + "\n");
        }
    }

    private void sendCommand(String command) {
        appendOutput("$ " + command + "\n");
        try {
            shellInput.write((command + "\n").getBytes());
            shellInput.flush();
        } catch (Exception e) {
            appendOutput("Error sending command: " + e.getMessage() + "\n");
        }
    }

    private void appendOutput(String text) {
        runOnUiThread(() -> {
            outputText.append(text);
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (shellProcess != null) {
            shellProcess.destroy();
        }
    }
}
