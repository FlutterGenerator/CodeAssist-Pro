package com.tyron.completion.gradle;

import android.content.Context;
import com.tyron.completion.CompletionProvider;
import com.tyron.completion.gradle.provider.GradleCompletionProvider;
import com.tyron.language.fileTypes.FileTypeManager;
import com.tyron.language.gradle.GradleFileType;
import com.tyron.language.gradle.GradleLanguage;

public class GradleCompletionModule {

    public static void initialize(Context context) {
        FileTypeManager.getInstance().registerFileType(GradleFileType.INSTANCE);
        
        CompletionProvider.registerCompletionProvider(
            GradleLanguage.INSTANCE, new GradleCompletionProvider());
    }
}
