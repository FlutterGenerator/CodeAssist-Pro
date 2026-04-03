package com.tyron.language.gradle;

import com.tyron.language.api.Language;
import org.jetbrains.annotations.NotNull;

public class GradleLanguage extends Language {

    public static final GradleLanguage INSTANCE = new GradleLanguage("GRADLE");

    protected GradleLanguage(@NotNull String id) {
        super(id);
    }
}
