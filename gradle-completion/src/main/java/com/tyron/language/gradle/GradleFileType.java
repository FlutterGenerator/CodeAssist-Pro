package com.tyron.language.gradle;

import android.graphics.drawable.Drawable;
import com.tyron.language.api.Language;
import com.tyron.language.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;

public class GradleFileType extends LanguageFileType {

    public static final GradleFileType INSTANCE = new GradleFileType(GradleLanguage.INSTANCE);

    protected GradleFileType(@NotNull Language instance) {
        super(instance);
    }

    @Override
    public @NotNull String getName() {
        return "Gradle";
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Gradle";
    }

    @Override
    public @NotNull String getDescription() {
        return "Gradle build script";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "gradle";
    }

    @Override
    public @NotNull Drawable getIcon() {
        return null;
    }
}
