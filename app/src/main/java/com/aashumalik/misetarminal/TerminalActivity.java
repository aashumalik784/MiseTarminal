package com.aashumalik.misetarminal;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TerminalActivity extends Activity {

    private TextView outputText;
    private EditText inputField;
    private ScrollView scrollView;
    private OutputStream shellInput;

    private static final String ROOTFS_URL =
            "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04.4/release/ubuntu-base-24.04.4-base-arm64.tar.gz";

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
            String nativeDir = getApplicationInfo().nativeLibraryDir;

            File rootfsTar = new File(baseDir, "ubuntu-base.tar.gz");
            File rootfsDir = new File(baseDir, "rootfs");
            if (!new File(rootfsDir, "bin").exists() && !rootfsTar.exists()) {
                appendOutput("Downloading Ubuntu base rootfs...\n");
                downloadFile(ROOTFS_URL, rootfsTar);
                appendOutput("Download complete.\n");
            }

            File scriptFile = new File(baseDir, "bootstrap.sh");
            copyAsset("mise-terminal/bootstrap.sh", scriptFile);
            scriptFile.setExecutable(true);

            appendOutput("Mise Tarminal starting setup...\n");
            ProcessBuilder pb = new ProcessBuilder("/system/bin/sh",
                    scriptFile.getAbsolutePath(), baseDir.getAbsolutePath(), nativeDir);
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

    private void downloadFile(String urlStr, File outFile) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        InputStream in = new BufferedInputStream(conn.getInputStream());
        FileOutputStream out = new FileOutputStream(outFile);
        byte[] buffer = new byte[8192];
        int len;
        long total = 0;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
            total += len;
        }
        in.close();
        out.close();
        conn.disconnect();
    }

    private void copyAsset(String assetPath, File outFile) throws Exception {
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
