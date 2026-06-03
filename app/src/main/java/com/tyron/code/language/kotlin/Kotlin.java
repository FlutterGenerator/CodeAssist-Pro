package com.tyron.code.language.kotlin;

import com.tyron.code.ApplicationLoader;
import com.tyron.code.language.Language;
import com.tyron.common.SharedPreferenceKeys;
import com.tyron.editor.Editor;
import java.io.File;

public class Kotlin implements Language {
  @Override
  public boolean isApplicable(File ext) {
    return ext.getName().endsWith(".kt");
  }

  @Override
  public io.github.rosemoe.sora.lang.Language get(Editor editor) {
    return useLegacyKotlinLsp() ? new KotlinLanguage(editor) : new KotlinLanguage2(editor);
  }
  public boolean useLegacyKotlinLsp(){
    return ApplicationLoader.getDefaultPreferences().getBoolean(SharedPreferenceKeys.LEGACY_KOTLIN_LSP, false);
  }
}
