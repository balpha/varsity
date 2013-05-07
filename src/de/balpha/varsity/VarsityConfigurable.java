package de.balpha.varsity;

import com.intellij.application.options.editor.EditorOptionsPanel;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class VarsityConfigurable implements Configurable {
    private PropertiesComponent mProps;
    private ConfigForm mForm;
    public VarsityConfigurable() {
        mProps = PropertiesComponent.getInstance();
        mForm = new ConfigForm();
        mForm.setVerifier();
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Varsity";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return mForm.getPanel();
    }

    @Override
    public boolean isModified() {
        return mProps.getBoolean("foldprimitives", true) != mForm.getFoldPrimitives()
                ||
            mProps.getOrInitInt("minchars", 3) != mForm.getMinChars();

    }

    @Override
    public void apply() throws ConfigurationException {
        mProps.setValue("foldprimitives", mForm.getFoldPrimitives() ? "true" : "false");
        mProps.setValue("minchars", mForm.getMinChars() + "");

        // the following is copied from https://github.com/JetBrains/intellij-community/blob/master/platform/lang-impl/src/com/intellij/application/options/editor/CodeFoldingConfigurable.java
        final List<Pair<Editor, Project>> toUpdate = new ArrayList<Pair<Editor, Project>>();
        for (final Editor editor : EditorFactory.getInstance().getAllEditors()) {
            final Project project = editor.getProject();
            if (project != null && !project.isDefault()) {
                toUpdate.add(Pair.create(editor, project));
            }
        }

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                for (Pair<Editor, Project> each : toUpdate) {
                    if (each.second == null || each.second.isDisposed()) {
                        continue;
                    }
                    final CodeFoldingManager foldingManager = CodeFoldingManager.getInstance(each.second);
                    if (foldingManager != null) {
                        foldingManager.buildInitialFoldings(each.first);
                    }
                }
                EditorOptionsPanel.reinitAllEditors();
            }
        }, ModalityState.NON_MODAL);
    }

    @Override
    public void reset() {
        mForm.setFoldPrimitives(mProps.getBoolean("foldprimitives", true));
        mForm.setMinChars(mProps.getOrInitInt("minchars", 3));
    }

    @Override
    public void disposeUIResources() {

    }
}
