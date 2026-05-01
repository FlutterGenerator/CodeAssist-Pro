package com.tyron.code.ui.settings;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import android.app.Activity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.transition.MaterialSharedAxis;
import com.itsaky.androidide.app.configuration.IJdkDistributionProvider;
import com.tyron.common.SharedPreferenceKeys;
import com.tyron.resources.R;
import com.itsaky.androidide.utils.Environment;

import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CompilerSettingsFragment extends PreferenceFragmentCompat {

    public static final int PICK_ZIP = 1001;
    private static final int PICK_ZIP_REQUEST = 944;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.compiler_preferences, rootKey);

        Preference openjdk = findPreference(SharedPreferenceKeys.OPENJDK);
        Toast.makeText(requireContext(),new File(Environment.JAVA_HOME,"openjdk-21.0.1").exists()+"",Toast.LENGTH_LONG).show();
        try {
            Toast.makeText(requireContext(), IJdkDistributionProvider.getInstance().forJavaHome(Environment.DEFAULT_JAVA_HOME).toString(), Toast.LENGTH_LONG).show();
        }catch (Exception e){
            e.printStackTrace();
        }
        if (openjdk != null) {
            openjdk.setOnPreferenceClickListener(pref -> {
                showOpenJdkSettings();
                return true;
            });
        }
    }
    private void showOpenJdkSettings() {
        openChooser("application/zip", PICK_ZIP_REQUEST);
    }
    private void openChooser(String type, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(type);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select File"), requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;

           if (requestCode == PICK_ZIP_REQUEST) {
                extractJdk(uri);
            }
        }
    }

    private void extractJdk(Uri zipUri) {
        // تشغيل العملية في خيط خلفي (Background Thread)
        new Thread(() -> {
            try {
                // المسار: /data/user/0/dev.mutwakil.kotlinide/files/jdk
                File outputDir = new File(Environment.DEFAULT_JAVA_HOME).getParentFile();
                if (!outputDir.exists()) outputDir.mkdirs();

                InputStream is = requireActivity().getContentResolver().openInputStream(zipUri);
                ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
                ZipEntry ze;
                byte[] buffer = new byte[8192];

                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Extracting JDK...", Toast.LENGTH_SHORT).show());

                while ((ze = zis.getNextEntry()) != null) {
                    File f = new File(outputDir, ze.getName());
                    if (ze.isDirectory()) {
                        f.mkdirs();
                    } else {
                        f.getParentFile().mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(f)) {
                            int count;
                            while ((count = zis.read(buffer)) != -1) {
                                fos.write(buffer, 0, count);
                            }
                        }
                    }
                    zis.closeEntry();
                }
                zis.close();
                new File(outputDir,"openjdk-21.0.1").renameTo(new File(outputDir,"java-21-openjdk"));
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "JDK Ready at: " + outputDir.getAbsolutePath(), Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                Log.e("JDK_ERROR", "Extraction failed", e);
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

}
