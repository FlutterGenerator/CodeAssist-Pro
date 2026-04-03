package com.tyron.completion.java.action.generators;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.tyron.actions.ActionGroup;
import com.tyron.actions.ActionPlaces;
import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.actions.Presentation;
import com.tyron.completion.java.R;
import java.io.File;

public class JavaGenerateActionGroup extends ActionGroup {

    public static final String ID = "javaGenerateActionGroup";

    @Override
    public void update(@NonNull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setVisible(false);

        if (!ActionPlaces.EDITOR.equals(event.getPlace())) {
            return;
        }

        File file = event.getData(CommonDataKeys.FILE);
        if (file == null || !file.getName().endsWith(".java")) {
            return;
        }

        presentation.setVisible(true);
        presentation.setText("Generate...");
    }

    @Override
    public boolean isPopup() {
        return true;
    }

    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        return new AnAction[]{
                new GenerateConstructorAction(),
                new GenerateGettersAndSettersAction(),
                new GenerateToStringAction(),
                new GenerateEqualsAndHashCodeAction()
        };
    }
}
