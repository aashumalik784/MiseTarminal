package com.aashumalik.misetarminal;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class TerminalActivity extends Activity {

    private TextView outputText;
    private EditText inputField;
    private ScrollView scrollView;
    private OutputStream shellInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        outputText = findViewById(R.id.outputText);
        inputField = findViewById(R.id.inputField);
        scrollView = findViewById(R.id.scrollView);
        outputText.setMovementMethod(new ScrollingMovementMethod());

        inputField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                sendCommand(inputField.getText().toString());
                inputField.setText("");
                return true;
            }
            return false;
        });

        new Thread(this::runSetupAndShell).start();
    }

    private void runSetupAndShell() {
        try {
            File baseDir = getFilesDir();
            File assetsCopyDir = new File(baseDir, "assets_copy");
            assetsCopyDir.mkdirs();

            copyAsset("mise-terminal/prebuilt/proot-arm64", new File(assetsCopyDir, "proot-arm64"));
            copyAsset("mise-terminal/prebuilt/libtalloc.so.2", new File(assetsCopyDir, "libtalloc.so.2"));
            copyAsset("mise-terminal/prebuilt/libandroid-shmem.so", new File(assetsCopyDir, "libandroid-shmem.so"));

            File scriptFile = new File(baseDir, "bootstrap.sh");
            copyAsset("mise-terminal/bootstrap.sh", scriptFile);
            scriptFile.setExecutable(true);

            appendOutput("Mise Tarminal starting setup...\n");
            ProcessBuilder pb = new ProcessBuilder("/system/bin/sh", scriptFile.getAbsolutePath(), baseDir.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            shellInput = process.getOutputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                appendOutput(line + "\n");
            }
            process.waitFor();

            appendOutput("\nSetup finished. You can now enter commands below.\n");
        } catch (Exception e) {
            appendOutput("Setup error: " + e.getMessage() + "\n");
        }
    }

    private void copyAsset(String assetPath, File outFile) throws Exception {
        if (outFile.exists() && outFile.length() > 0) return;
        InputStream in = getAssets().open(assetPath);
        FileOutputStream out = new FileOutputStream(outFile);
        byte[] buffer = new byte[4096];
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
        in.close();
        out.close();
    }

    private void sendCommand(String command) {
        appendOutput("$ " + command + "\n");
        try {
            if (shellInput != null) {
                shellInput.write((command + "\n").getBytes());
                shellInput.flush();
            }
        } catch (Exception e) {
            appendOutput("Error: " + e.getMessage() + "\n");
        }
    }

    private void appendOutput(String text) {
        runOnUiThread(() -> {
            outputText.append(text);
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }
}
