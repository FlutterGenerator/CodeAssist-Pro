package com.tyron.completion.java.parse;

import static com.itsaky.androidide.config.JavacConfigProvider.PROP_ANDROIDIDE_JAVA_HOME;
import static com.itsaky.androidide.config.JavacConfigProvider.disableModules;
import static com.itsaky.androidide.config.JavacConfigProvider.enableModules;
import static com.itsaky.androidide.config.JavacConfigProvider.setLatestSourceVersion;
import static com.itsaky.androidide.config.JavacConfigProvider.setLatestSupportedSourceVersion;
import static com.itsaky.androidide.utils.Environment.JAVA_HOME;

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import com.tyron.builder.BuildModule;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.completion.java.compiler.services.NBAttr;
import com.tyron.completion.java.compiler.services.NBCheck;
import com.tyron.completion.java.compiler.services.NBClassFinder;
import com.tyron.completion.java.compiler.services.NBClassReader;
import com.tyron.completion.java.compiler.services.NBClassWriter;
import com.tyron.completion.java.compiler.services.NBEnter;
import com.tyron.completion.java.compiler.services.NBJavacTrees;
import com.tyron.completion.java.compiler.services.NBLog;
import com.tyron.completion.java.compiler.services.NBMemberEnter;
import com.tyron.completion.java.compiler.services.NBParserFactory;
import com.tyron.completion.java.compiler.services.NBResolve;
import com.tyron.completion.java.compiler.services.NBTreeMaker;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.lang.model.SourceVersion;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import org.apache.commons.io.output.NullWriter;

public class JavacParser {

  public void test() {}

  static JavacTaskImpl createJavacTask(
      final File file,
      final Iterable<? extends JavaFileObject> jfos,
      final File root,
      final List<File> cpInfo,
      final List<File> sourcePath,
      final JavacParser parser,
      final DiagnosticListener<? super JavaFileObject> diagnosticListener,
      final boolean detached) {
    Context context = new Context();
    NBLog.preRegister(context, new PrintWriter(new NullWriter()));

    List<String> options = new ArrayList<>();
    // TODO: 5/7/2026 its just a test, improve it
    System.setProperty(PROP_ANDROIDIDE_JAVA_HOME, JAVA_HOME.getAbsolutePath());
    int i = 1;
    if (i!=1) {
      setLatestSourceVersion(SourceVersion.RELEASE_8);
      setLatestSupportedSourceVersion(SourceVersion.RELEASE_11);
      disableModules();
      Collections.addAll(
        options,
        "-bootclasspath",
        joinPath(Arrays.asList(BuildModule.getAndroidJar(), BuildModule.getLambdaStubs())));
    } else {
      setLatestSourceVersion(SourceVersion.RELEASE_11);
      setLatestSupportedSourceVersion(SourceVersion.RELEASE_11);
      disableModules();
      cpInfo.addAll(Arrays.asList(BuildModule.getLambdaStubs(),BuildModule.getAndroidJar()));
    }
    setupCompileOptions(options);
    Collections.addAll(
        options,
        "-bootclasspath",
        joinPath(Arrays.asList(BuildModule.getAndroidJar(), BuildModule.getLambdaStubs())));
//    Collections.addAll(options, "-target", "1.8", "-source", "1.8");
    Collections.addAll(options, "-cp", joinPath(cpInfo));
    Collections.addAll(options, "-proc:none", "-g");
//    Collections.addAll(
//        options,
//        "-Xlint:cast",
//        "-Xlint:deprecation",
//        "-Xlint:empty",
//        "-Xlint" + ":fallthrough",
//        "-Xlint:finally",
//        "-Xlint:path",
//        "-Xlint:unchecked",
//        "-Xlint" + ":varargs",
//        "-Xlint:static");
    Collections.addAll(
            options,
            "-XDcompilePolicy=byfile",
            "-XD-Xprefer=source",
            "-XDide",
            "-XDkeepCommentsOverride=ignore",
            "-XDsuppressAbortOnBadClassFile",
            "-XDshould-stop.at=GENERATE",
            "-XDdiags.formatterOptions=-source",
            "-XDdiags.layout=%L%m|%L%m|%L%m",
            "-XDbreakDocCommentParsingOnError=false",
            "-Xlint:cast",
            "-Xlint:deprecation",
            "-Xlint:empty",
            "-Xlint:fallthrough",
            "-Xlint:finally",
            "-Xlint:path",
            "-Xlint:unchecked",
            "-Xlint:varargs",
            "-Xlint:static");

    JavacTool tool = JavacTool.create();
    JavacFileManager standardFileManager =
        tool.getStandardFileManager(
            diagnosticListener, Locale.getDefault(), StandardCharsets.UTF_8);
    JavacTaskImpl task =
        (JavacTaskImpl)
            tool.getTask(
                null,
                standardFileManager,
                diagnosticListener,
                options,
                Collections.singletonList("java.lang.Object"),
                jfos,
                context);

    NBClassReader.preRegister(context);
    NBAttr.preRegister(context);
    NBClassWriter.preRegister(context);
    NBClassFinder.preRegister(context);
    NBParserFactory.preRegister(context);
    NBTreeMaker.preRegister(context);
    NBJavacTrees.preRegister(context);
    NBResolve.preRegister(context);
    NBEnter.preRegister(context);
    NBMemberEnter.preRegister(context, true);
    NBCheck.preRegister(context);
    return task;
  }

  protected static void setupCompileOptions( final List<String> options) {
//    if (module == null) {
//      Collections.addAll(
//              options,
//              "-source",
//              DEFAULT_COMPILER_SOURCE_AND_TARGET_VERSION,
//              "-target",
//              DEFAULT_COMPILER_SOURCE_AND_TARGET_VERSION);
//      return;
//    }

    Collections.addAll(options,"--enable-preview");

    options.add("-source");
    options.add("17");
    options.add("-target");
    options.add("17");
  }

  /**
   * Combine source path or class path entries using the system separator, for example ':' in unix
   */
  private static String joinPath(Collection<File> classOrSourcePath) {
    return classOrSourcePath.stream()
        .map(File::getAbsolutePath)
        .collect(Collectors.joining(File.pathSeparator));
  }
}
