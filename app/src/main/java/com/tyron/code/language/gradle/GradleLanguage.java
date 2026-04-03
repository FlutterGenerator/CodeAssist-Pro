package com.tyron.code.language.gradle;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.tyron.code.language.textmate.EmptyTextMateLanguage;
import com.tyron.completion.CompletionParameters;
import com.tyron.completion.gradle.provider.GradleCompletionProvider;
import com.tyron.completion.model.CompletionList;
import com.tyron.editor.Editor;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException;
import io.github.rosemoe.sora.lang.completion.CompletionHelper;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.util.MyCharacter;
import io.github.rosemoe.sora.widget.SymbolPairMatch;
import java.util.ArrayList;
import java.util.Objects;

public class GradleLanguage extends EmptyTextMateLanguage implements Language {

    private final Editor mEditor;
    private final GradleAnalyzer mAnalyzer;

    public GradleLanguage(Editor editor) {
        mEditor = editor;
        mAnalyzer = GradleAnalyzer.create(editor,this);
    }

    @NonNull
    @Override
    public AnalyzeManager getAnalyzeManager() {
        return mAnalyzer;
    }

    @Override
    public void requireAutoComplete(@NonNull ContentReference content, @NonNull CharPosition position, @NonNull CompletionPublisher publisher, @NonNull Bundle extraArguments) throws CompletionCancelledException {
        String prefix = CompletionHelper.computePrefix(content, position, MyCharacter::isJavaIdentifierPart);
        
        CompletionParameters parameters = CompletionParameters.builder()
                .setColumn(position.getColumn())
                .setLine(position.getLine())
                .setIndex(position.getIndex())
                .setEditor(mEditor)
                .setFile(mEditor.getCurrentFile())
                .setProject(mEditor.getProject())
                .setModule(Objects.requireNonNull(mEditor.getProject()).getModule(mEditor.getCurrentFile()))
                .setContents(content.toString())
                .setPrefix(prefix)
                .build();
                
        GradleCompletionProvider provider = new GradleCompletionProvider();
        CompletionList list = provider.complete(parameters);
        
        publisher.setUpdateThreshold(0);
        publisher.addItems(new ArrayList<>(list.items));
    }

    @Override
    public int getIndentAdvance(@NonNull ContentReference content, int line, int column) {
        return 0;
    }

    @Override
    public boolean useTab() {
        return true;
    }

    @Override
    public SymbolPairMatch getSymbolPairs() {
        return new SymbolPairMatch.DefaultSymbolPairs();
    }

    @Override
    public NewlineHandler[] getNewlineHandlers() {
        return new NewlineHandler[0];
    }
}
