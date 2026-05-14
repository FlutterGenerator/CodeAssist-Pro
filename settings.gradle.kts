
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { url = uri("https://repo.eclipse.org/content/repositories/ee4j-snapshots/") }
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
        maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
        maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
    }
}

dependencyResolutionManagement {

    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.eclipse.org/content/repositories/ee4j-snapshots/") }
        mavenLocal()
        maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
        maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
        maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
    }
}

plugins{
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "CodeAssist-Pro"

include(
    ":annotation-processors",
    ":annotations",
    ":app",
    ":jaxp:xml",
    ":jaxp:jaxp-internal",
    ":build-logic",
    ":kotlinc",
    ":viewbinding-lib",
    ":viewbinding-inject",
    ":actions-api",
    ":editor-api",
    ":completion-api",
    ":fileeditor-api",
    ":layout-preview",
    ":appcompat-widgets",
    ":proteus-core",
    ":cardview",
    ":constraintlayout",
    ":vector-parser",
    ":event-manager",
    ":common",
    ":apksigner",
    ":code-editor",
    ":dependency-resolver",
    ":android-stubs",
    ":treeview",
    ":javapoet",
    ":kotlin-completion",
    ":java-completion",
    ":xml-completion",
    ":gradle-completion",
    ":google-java-format",
    ":ktfmt",
    ":javac",
    ":project",
    ":manifmerger",
    ":logging",
    ":language-api",
    ":xml-repository",
    ":android-common-resources",
    ":java-stubs",
    ":resources",
    ":lsp:kotlin",
    ":lsp:api",
    ":lsp:models",
    ":kotlin-analysis-api",
    ":lsp:jvm-symbol-index",
    ":lsp:indexing",
    ":lsp:jvm-symbol-models",
    ":event:eventbus",
    ":event:eventbus-android",
    ":event:eventbus-events",
    ":utilities:shared",
    ":utilities:lookup",
//    ":lsp:java",
    "editor",
    ":jaxp",
    "subprojects:fuzzysearch",
    "build-tools:compose-compiler-plugin"
)