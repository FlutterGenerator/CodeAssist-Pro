package com.tyron.code.language.lsp;

import androidx.annotation.Nullable;

import com.itsaky.androidide.lsp.api.ILanguageClient;
import com.itsaky.androidide.lsp.debug.IDebugClient;
import com.itsaky.androidide.lsp.models.CodeActionItem;
import com.itsaky.androidide.lsp.models.DiagnosticItem;
import com.itsaky.androidide.lsp.models.DiagnosticResult;
import com.itsaky.androidide.lsp.models.PerformCodeActionParams;
import com.itsaky.androidide.lsp.models.ShowDocumentParams;
import com.itsaky.androidide.lsp.models.ShowDocumentResult;
import com.itsaky.androidide.lsp.util.DiagnosticUtil;
import com.itsaky.androidide.models.Location;
import com.tyron.code.ui.editor.EditorContainerFragment;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kotlin.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleLanguageClientImpl implements ILanguageClient {

  public static final int MAX_DIAGNOSTIC_FILES = 10;
  public static final int MAX_DIAGNOSTIC_ITEMS_PER_FILE = 20;
  protected static final Logger LOG = LoggerFactory.getLogger(SimpleLanguageClientImpl.class);
  private static SimpleLanguageClientImpl mInstance;
  private final Map<File, List<DiagnosticItem>> diagnostics = new HashMap<>();
  protected EditorContainerFragment fragment;

  private SimpleLanguageClientImpl(EditorContainerFragment provider) {
    setFragment(provider);
  }

  public void setFragment(EditorContainerFragment provider) {
    this.fragment = provider;
  }

  public static SimpleLanguageClientImpl initialize(EditorContainerFragment provider) {
    if (mInstance != null) {
      throw new IllegalStateException("Client is already initialized");
    }

    mInstance = new SimpleLanguageClientImpl(provider);

    return getInstance();
  }

  public static SimpleLanguageClientImpl getInstance() {
    if (mInstance == null) {
      throw new IllegalStateException("Client not initialized");
    }

    return mInstance;
  }

  public static void shutdown() {
    if (mInstance != null) {
      mInstance.fragment = null;
    }
    mInstance = null;
  }

  public static boolean isInitialized() {
    return mInstance != null;
  }

  @Nullable
  @Override
  public IDebugClient getDebugClient() {
    return ILanguageClient.super.getDebugClient();
  }

  @Override
  public void publishDiagnostics(DiagnosticResult result) {
    if (result == DiagnosticResult.NO_UPDATE || !canUseActivity()) {
      // No update is expected
      return;
    }

    boolean error = result == null;
    // activity.handleDiagnosticsResultVisibility(error || result.getDiagnostics().isEmpty());

    if (error) {
      return;
    }

    File file = result.getFile().toFile();
    if (!file.exists() || !file.isFile()) {
      return;
    }

    final var editorView = fragment.getEditorForFile(file);
    if (editorView != null) {
        // editorView.getEditor();
        final var container = new DiagnosticsContainer();
        try {
          container.addDiagnostics(
              result.getDiagnostics().stream()
                  .map(DiagnosticItem::asDiagnosticRegion)
                  .collect(Collectors.toList()));
        } catch (Throwable err) {
          LOG.error("Unable to map DiagnosticItem to DiagnosticRegion", err);
        }
        editorView.setDiagnostics(container);
    }

    diagnostics.put(file, result.getDiagnostics());
    // activity.setDiagnosticsAdapter(newDiagnosticsAdapter());
  }

  @Nullable
  @Override
  public DiagnosticItem getDiagnosticAt(final File file, final int line, final int column) {
    return DiagnosticUtil.binarySearchDiagnostic(this.diagnostics.get(file), line, column);
  }

  @Override
  public void performCodeAction(PerformCodeActionParams params) {

  }

  @Override
  public void performCodeAction(CodeActionItem actionItem) {
    ILanguageClient.super.performCodeAction(actionItem);
  }

  @Override
  public void performCodeAction(File file, CodeActionItem actionItem) {
    ILanguageClient.super.performCodeAction(file, actionItem);
  }

  @Override
  public ShowDocumentResult showDocument(ShowDocumentParams params) {
    return null;
  }

  @Override
  public void showLocations(List<Location> locations) {

  }

  private boolean canUseActivity() {
      if (fragment == null) return false;
      assert fragment.getActivity() != null;
      return !fragment.getActivity().isFinishing()
              && !fragment.getActivity().isDestroyed()
              && !fragment.getActivity().getSupportFragmentManager().isDestroyed()
              && !fragment.getActivity().getSupportFragmentManager().isStateSaved();
  }

  private Unit noOp(final Object obj) {
    return Unit.INSTANCE;
  }
}
