package com.tyron.code;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.developer.crashx.config.CrashConfig;
import com.google.android.material.color.DynamicColors;
import com.itsaky.androidide.app.configuration.IJdkDistributionProvider;
import com.itsaky.androidide.lsp.kotlin.completion.KotlinSnippetRepository;
import com.itsaky.androidide.utils.Environment;
import com.tyron.actions.ActionManager;
import com.tyron.builder.BuildModule;
import com.tyron.code.event.EventManager;
import com.tyron.code.ui.editor.action.CloseAllEditorAction;
import com.tyron.code.ui.editor.action.CloseFileEditorAction;
import com.tyron.code.ui.editor.action.CloseOtherEditorAction;
import com.tyron.code.ui.editor.action.DiagnosticInfoAction;
import com.tyron.code.ui.editor.action.PreviewLayoutAction;
import com.tyron.code.ui.editor.action.text.TextActionGroup;
import com.tyron.code.ui.file.action.GitActionGroup;
import com.tyron.code.ui.file.action.ImportFileActionGroup;
import com.tyron.code.ui.file.action.NewFileActionGroup;
import com.tyron.code.ui.file.action.file.DeleteFileAction;
import com.tyron.code.ui.file.action.file.InstallApkFileAction;
import com.tyron.code.ui.main.action.compile.CompileActionGroup;
import com.tyron.code.ui.main.action.other.FormatAction;
import com.tyron.code.ui.main.action.other.OpenSettingsAction;
import com.tyron.code.ui.main.action.other.SSHKeyManagerAction;
import com.tyron.code.ui.main.action.project.ProjectActionGroup;
import com.tyron.code.ui.settings.ApplicationSettingsFragment;
import com.tyron.common.ApplicationProvider;
import com.tyron.common.Prefs;
import com.tyron.completion.CompletionProvider;
import com.tyron.completion.gradle.GradleCompletionModule;
import com.tyron.completion.index.CompilerService;
import com.tyron.completion.java.CompletionModule;
import com.tyron.completion.java.JavaCompilerProvider;
import com.tyron.completion.java.JavaCompletionProvider;
import com.tyron.completion.main.CompletionEngine;
import com.tyron.completion.xml.XmlCompletionModule;
import com.tyron.completion.xml.XmlIndexProvider;
import com.tyron.completion.xml.v2.AndroidXmlCompletionProvider;
import com.tyron.editor.selection.ExpandSelectionProvider;
import com.tyron.language.fileTypes.FileTypeManager;
import com.tyron.language.java.JavaFileType;
import com.tyron.language.java.JavaLanguage;
import com.tyron.language.xml.XmlFileType;
import com.tyron.language.xml.XmlLanguage;
import com.tyron.selection.java.JavaExpandSelectionProvider;
import com.tyron.selection.xml.XmlExpandSelectionProvider;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver;
import io.sentry.android.core.SentryAndroid;

public class ApplicationLoader extends Application {

    private static ApplicationLoader sInstance;

    public static ApplicationLoader getInstance() {
        return sInstance;
    }

    private EventManager mEventManager;

    // no memory leaks since applicationContext is a singleton
    public static Context applicationContext;

    @Override
    public void onCreate() {
        long t = System.currentTimeMillis();
        super.onCreate();
        log("super.OnCreate",t);

        sInstance = this;
        applicationContext = this;

        new Thread(this::addProviders,"bc-provider-init").start();
        
        setupTheme();
        log("setupTheme",t);

        mEventManager = new EventManager();
        Prefs.init(this, getDefaultPreferences());
        log("Prefs.init",t);
        ApplicationProvider.initialize(applicationContext);
        log("ApplicationProvider.init",t);


        new Thread(() -> SentryAndroid.init(ApplicationLoader.this)).start();

        try {
            boolean isLoggingEnabled =
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .getBoolean("ca_logging", false);
            if (isLoggingEnabled) Logger.initialize(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        CrashConfig.Builder.create()
                .backgroundMode(CrashConfig.BACKGROUND_MODE_SHOW_CUSTOM)
                .enabled(true)
                .showErrorDetails(true)
                .showRestartButton(true)
                .logErrorOnRestart(true)
                .trackActivities(true)
                .apply();

        runStartup();
        log("runStartup",t);
    }

    private void runStartup() {
        StartupManager startupManager = new StartupManager();

        startupManager.addStartupActivity(() -> {
            FileTypeManager manager = FileTypeManager.getInstance();
            manager.registerFileType(JavaFileType.INSTANCE);
            manager.registerFileType(XmlFileType.INSTANCE);
        });

        startupManager.addStartupActivity(() -> {
            ExpandSelectionProvider.registerProvider(
                    JavaLanguage.INSTANCE, new JavaExpandSelectionProvider());
            ExpandSelectionProvider.registerProvider(
                    XmlLanguage.INSTANCE, new XmlExpandSelectionProvider());
        });

        startupManager.addStartupActivity(() -> {
            ActionManager manager = ActionManager.getInstance();
            manager.registerAction(CompileActionGroup.ID, new CompileActionGroup());
            manager.registerAction(ProjectActionGroup.ID, new ProjectActionGroup());
            manager.registerAction(PreviewLayoutAction.ID, new PreviewLayoutAction());
            manager.registerAction(FormatAction.ID, new FormatAction());
            manager.registerAction(SSHKeyManagerAction.ID, new SSHKeyManagerAction());
            manager.registerAction(OpenSettingsAction.ID, new OpenSettingsAction());
            manager.registerAction(CloseFileEditorAction.ID, new CloseFileEditorAction());
            manager.registerAction(CloseOtherEditorAction.ID, new CloseOtherEditorAction());
            manager.registerAction(CloseAllEditorAction.ID, new CloseAllEditorAction());
            manager.registerAction(TextActionGroup.ID, new TextActionGroup());
            manager.registerAction(DiagnosticInfoAction.ID, new DiagnosticInfoAction());
            manager.registerAction(NewFileActionGroup.ID, new NewFileActionGroup());
            manager.registerAction(DeleteFileAction.ID, new DeleteFileAction());
            manager.registerAction(ImportFileActionGroup.ID, new ImportFileActionGroup());
            manager.registerAction(InstallApkFileAction.ID, new InstallApkFileAction());
            manager.registerAction(GitActionGroup.ID, new GitActionGroup());
        });

        startupManager.addBackgroundActivity(() -> {
            CompletionModule.initialize(applicationContext);
            XmlCompletionModule.initialize(applicationContext);
            GradleCompletionModule.initialize(applicationContext);
            BuildModule.initialize(applicationContext);
        });

        startupManager.addBackgroundActivity(() -> {
            CompletionEngine engine = CompletionEngine.getInstance();
            CompilerService index = CompilerService.getInstance();
            if (index.isEmpty()) {
                index.registerIndexProvider(JavaCompilerProvider.KEY, new JavaCompilerProvider());
                index.registerIndexProvider(XmlIndexProvider.KEY, new XmlIndexProvider());
            }
        });

        startupManager.addBackgroundActivity(() -> {
            CompletionProvider.registerCompletionProvider(
                    JavaLanguage.INSTANCE, new JavaCompletionProvider());
            CompletionProvider.registerCompletionProvider(
                    XmlLanguage.INSTANCE, new AndroidXmlCompletionProvider());
            XmlCompletionModule.registerActions(ActionManager.getInstance());
            CompletionModule.registerActions(ActionManager.getInstance());
        });

        startupManager.addBackgroundActivity(() -> {
            FileProviderRegistry.getInstance()
                    .addFileProvider(new AssetsFileResolver(applicationContext.getAssets()));
            try {
                GrammarRegistry.getInstance().loadGrammars("textmate/languages.json");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Environment.init(ApplicationLoader.this);
//            IJdkDistributionProvider.getInstance().loadDistributions();
            KotlinSnippetRepository.INSTANCE.init();
        });

        startupManager.startup();
    }

    /**
     * Can be used to communicate within the application globally
     *
     * @return the EventManager
     */
    @NonNull
    public EventManager getEventManager() {
        return mEventManager;
    }

    private void setupTheme() {
        ApplicationSettingsFragment.ThemeProvider provider =
                new ApplicationSettingsFragment.ThemeProvider(this);
        int theme = provider.getThemeFromPreferences();
        AppCompatDelegate.setDefaultNightMode(theme);
        DynamicColors.applyToActivitiesIfAvailable(this);
    }

    public static SharedPreferences getDefaultPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext);
    }

    public static void showToast(String message) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show();
    }

    @VisibleForTesting
    public static void setApplicationContext(Context context) {
        applicationContext = context;
    }

    public static Context getApp() {
        return applicationContext;
    }

    private void addProviders() {
        try {
            Security.removeProvider("BC"); // must remove the old bc provider
        } catch (Exception ignored) {
        }

        try {
            // insert the new bc provider at first
            Security.insertProviderAt(new BouncyCastleProvider(), 1);
        } catch (Exception ignored) {
        }
    }
    private void log(String tag,long start){
        Log.d("STARTUP_PERF",tag+": "+(System.currentTimeMillis()-start)+"ms");
    }
}