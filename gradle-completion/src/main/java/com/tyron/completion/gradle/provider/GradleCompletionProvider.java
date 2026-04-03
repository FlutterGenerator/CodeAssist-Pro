package com.tyron.completion.gradle.provider;

import com.tyron.completion.CompletionParameters;
import com.tyron.completion.CompletionProvider;
import com.tyron.completion.model.CompletionItem;
import com.tyron.completion.model.CompletionList;
import com.tyron.completion.model.DrawableKind;
import com.tyron.completion.model.SnippetCompletionItem;
import io.github.rosemoe.sora.lang.completion.SnippetDescription;
import io.github.rosemoe.sora.lang.completion.snippet.parser.CodeSnippetParser;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradleCompletionProvider extends CompletionProvider {

    @Override
    public boolean accept(File file) {
        String name = file.getName();
        return name.endsWith(".gradle");
    }

    @Override
    public CompletionList complete(CompletionParameters parameters) {
        List<CompletionItem> items = new ArrayList<>();
        
        String fileName = parameters.getFile().getName();
        String prefix = parameters.getPrefix();
        String content = parameters.getContents();
        int index = (int) parameters.getIndex();
        
        String context = getContext(content, index);
        
        if (fileName.equals("build.gradle")) {
            if (context.isEmpty()) {
                addTopLevelItems(items, prefix);
            } else if (context.equals("android")) {
                addAndroidItems(items, prefix);
            } else if (context.equals("dependencies")) {
                addDependencyItems(items, prefix, content, index);
            } else if (context.equals("repositories") || context.equals("buildscript.repositories")) {
                addRepositoryItems(items, prefix);
            } else if (context.equals("android.signingConfigs") || context.endsWith(".signingConfig")) {
                addSigningConfigItems(items, prefix);
            } else if (context.equals("android.defaultConfig")) {
                addDefaultConfigItems(items, prefix);
            } else if (context.equals("android.buildTypes")) {
                addBuildTypeItems(items, prefix);
            } else if (context.startsWith("android.buildTypes.")) {
                addBuildTypeContentItems(items, prefix);
            }
        } else if (fileName.equals("settings.gradle")) {
             if (context.isEmpty()) {
                addSettingsTopLevelItems(items, prefix);
             }
        }
        
        return CompletionList.builder(prefix).addItems(items).build();
    }

    private String getContext(String content, int index) {
        // Simple context detection based on curly braces
        int depth = 0;
        StringBuilder context = new StringBuilder();
        List<String> blocks = new ArrayList<>();
        
        int i = 0;
        while (i < index && i < content.length()) {
            char c = content.charAt(i);
            if (c == '{') {
                // Find block name before '{'
                String blockName = getPreviousIdentifier(content, i - 1);
                blocks.add(blockName);
                depth++;
            } else if (c == '}') {
                if (!blocks.isEmpty()) {
                    blocks.remove(blocks.size() - 1);
                }
                depth--;
            }
            i++;
        }
        
        for (int j = 0; j < blocks.size(); j++) {
            if (j > 0) context.append(".");
            context.append(blocks.get(j));
        }
        
        return context.toString();
    }

    private String getPreviousIdentifier(String content, int index) {
        int start = index;
        while (start >= 0 && Character.isWhitespace(content.charAt(start))) {
            start--;
        }
        int end = start;
        while (start >= 0 && (Character.isJavaIdentifierPart(content.charAt(start)) || content.charAt(start) == '.')) {
            start--;
        }
        return content.substring(start + 1, end + 1).trim();
    }

    private void addTopLevelItems(List<CompletionItem> items, String prefix) {
        addSnippetItem(items, "plugins", "plugins {\n    $0\n}", DrawableKind.Keyword, prefix);
        addSnippetItem(items, "android", "android {\n    $0\n}", DrawableKind.Keyword, prefix);
        addSnippetItem(items, "dependencies", "dependencies {\n    $0\n}", DrawableKind.Keyword, prefix);
        addSnippetItem(items, "repositories", "repositories {\n    $0\n}", DrawableKind.Keyword, prefix);
        addSnippetItem(items, "buildscript", "buildscript {\n    $0\n}", DrawableKind.Keyword, prefix);
        addSnippetItem(items, "apply plugin", "apply plugin: '$0'", DrawableKind.Method, prefix);
        addItem(items, "ext", "ext", DrawableKind.Keyword);
    }

    private void addAndroidItems(List<CompletionItem> items, String prefix) {
        addItem(items, "compileSdk", "compileSdk $0", DrawableKind.Field);
        addItem(items, "buildToolsVersion", "buildToolsVersion \"$0\"", DrawableKind.Field);
        addSnippetItem(items, "defaultConfig", "defaultConfig {\n    $0\n}", DrawableKind.Keyword, prefix);
        addSnippetItem(items, "buildTypes", "buildTypes {\n    $0\n}", DrawableKind.Keyword, prefix);
        addSnippetItem(items, "signingConfigs", "signingConfigs {\n    $0\n}", DrawableKind.Keyword, prefix);
        addSnippetItem(items, "compileOptions", "compileOptions {\n    $0\n}", DrawableKind.Keyword, prefix);
        addSnippetItem(items, "buildFeatures", "buildFeatures {\n    $0\n}", DrawableKind.Keyword, prefix);
        addItem(items, "namespace", "namespace \"$0\"", DrawableKind.Field);
    }

    private void addDefaultConfigItems(List<CompletionItem> items, String prefix) {
        addItem(items, "applicationId", "applicationId \"$0\"", DrawableKind.Field);
        addItem(items, "minSdk", "minSdk $0", DrawableKind.Field);
        addItem(items, "targetSdk", "targetSdk $0", DrawableKind.Field);
        addItem(items, "versionCode", "versionCode $0", DrawableKind.Field);
        addItem(items, "versionName", "versionName \"$0\"", DrawableKind.Field);
        addItem(items, "testInstrumentationRunner", "testInstrumentationRunner \"$0\"", DrawableKind.Field);
    }

    private void addDependencyItems(List<CompletionItem> items, String prefix, String content, int index) {
        addItem(items, "implementation", "implementation ", DrawableKind.Method);
        addItem(items, "testImplementation", "testImplementation ", DrawableKind.Method);
        addItem(items, "androidTestImplementation", "androidTestImplementation ", DrawableKind.Method);
        addItem(items, "api", "api ", DrawableKind.Method);
        addItem(items, "compileOnly", "compileOnly ", DrawableKind.Method);
        addItem(items, "runtimeOnly", "runtimeOnly ", DrawableKind.Method);
        addItem(items, "annotationProcessor", "annotationProcessor ", DrawableKind.Method);
        
        // Smart suggestions after configuration
        String line = getLineUntilCursor(content, index);
        if (line.matches(".*(implementation|api|compileOnly|runtimeOnly|testImplementation|androidTestImplementation)\\s*$")) {
            addSnippetItem(items, "project", "project(':$0')", DrawableKind.Method, prefix);
            addSnippetItem(items, "platform", "platform('$0')", DrawableKind.Method, prefix);
            addSnippetItem(items, "fileTree", "fileTree(dir: '$0', include: ['$1'])", DrawableKind.Method, prefix);
            addItem(items, "files", "files('$0')", DrawableKind.Method);
        }
    }

    private String getLineUntilCursor(String content, int index) {
        int start = index - 1;
        while (start >= 0 && content.charAt(start) != '\n') {
            start--;
        }
        return content.substring(start + 1, index);
    }

    private void addRepositoryItems(List<CompletionItem> items, String prefix) {
        addItem(items, "google", "google()", DrawableKind.Method);
        addItem(items, "mavenCentral", "mavenCentral()", DrawableKind.Method);
        addSnippetItem(items, "maven", "maven {\n    url '$0'\n}", DrawableKind.Method, prefix);
        addSnippetItem(items, "ivy", "ivy {\n    url '$0'\n}", DrawableKind.Method, prefix);
    }

    private void addSigningConfigItems(List<CompletionItem> items, String prefix) {
        addSnippetItem(items, "release", "release {\n    $0\n}", DrawableKind.Keyword, prefix);
        addSnippetItem(items, "debug", "debug {\n    $0\n}", DrawableKind.Keyword, prefix);
        addItem(items, "storeFile", "storeFile file('$0')", DrawableKind.Field);
        addItem(items, "storePassword", "storePassword \"$0\"", DrawableKind.Field);
        addItem(items, "keyAlias", "keyAlias \"$0\"", DrawableKind.Field);
        addItem(items, "keyPassword", "keyPassword \"$0\"", DrawableKind.Field);
    }

    private void addBuildTypeItems(List<CompletionItem> items, String prefix) {
        addSnippetItem(items, "release", "release {\n    $0\n}", DrawableKind.Keyword, prefix);
        addSnippetItem(items, "debug", "debug {\n    $0\n}", DrawableKind.Keyword, prefix);
    }

    private void addBuildTypeContentItems(List<CompletionItem> items, String prefix) {
        addItem(items, "minifyEnabled", "minifyEnabled true", DrawableKind.Field);
        addItem(items, "shrinkResources", "shrinkResources true", DrawableKind.Field);
        addItem(items, "proguardFiles", "proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'", DrawableKind.Method);
        addItem(items, "signingConfig", "signingConfig signingConfigs.$0", DrawableKind.Field);
    }

    private void addSettingsTopLevelItems(List<CompletionItem> items, String prefix) {
        addSnippetItem(items, "include", "include ':$0'", DrawableKind.Method, prefix);
        addItem(items, "rootProject.name", "rootProject.name = '$0'", DrawableKind.Field);
        addSnippetItem(items, "pluginManagement", "pluginManagement {\n    $0\n}", DrawableKind.Keyword, prefix);
        addSnippetItem(items, "dependencyResolutionManagement", "dependencyResolutionManagement {\n    $0\n}", DrawableKind.Keyword, prefix);
    }

    private void addItem(List<CompletionItem> items, String label, String insertText, DrawableKind kind) {
        CompletionItem item = new CompletionItem(label, "", insertText, kind);
        items.add(item);
    }

    private void addSnippetItem(List<CompletionItem> items, String label, String snippet, DrawableKind kind, String prefix) {
        try {
            var codeSnippet = CodeSnippetParser.parse(snippet);
            SnippetDescription description = new SnippetDescription(prefix.length(), codeSnippet, true);
            SnippetCompletionItem item = new SnippetCompletionItem(label, "Snippet", kind, description);
            items.add(item);
        } catch (Exception e) {
            addItem(items, label, snippet.replace("$0", ""), kind);
        }
    }
}
