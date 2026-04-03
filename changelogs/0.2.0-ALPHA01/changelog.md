### CodeAssist Pro v0.2.0-ALPHA01

### Added Groovy Language Support
- Added `GroovyLanguage` and `GroovyAnalyzer`.

### Added Gradle Language Support
- Added `GradleLanguage`, `Gradle` and `GradleAnalyzer`.
- Updated `languages.json` to include Gradle.
- Implemented `gradle-completion` module for specialized Gradle DSL support.
- Enhanced `GradleCompletionProvider` with context-aware suggestions for `android`, `dependencies`, `signingConfigs`, `buildTypes`, and `repositories` blocks.
- Added support for interactive code snippets using `SnippetCompletionItem` (e.g., `apply plugin`, `project()`, `include`).
- Implemented smart dependency suggestions: proposing `project()`, `platform()`, or `fileTree()` immediately after configuration keywords like `implementation`.
- Migrated `GradleAnalyzer` to the `app` module to provide real-time diagnostics for `.gradle` files.

### Updated Language Manager
- Registered new language supports in `LanguageManager`.

### Improved Kotlin Analysis
- Updates to `KotlinAnalyzer`.

### Improved Java Completion
- Updates to `IdentifierCompletionProvider`.
- Added Java code generation actions: Constructor, Getters/Setters, toString, and equals/hashCode.
- Implemented field selection UI for code generation using multi-choice dialogs.
- Organized Java editor actions into "Generate..." and "Java Helpers" submenus.
- Restricted Java-specific actions to only appear when editing `.java` files.
- Improved code generation logic with automatic indentation and import management.

### Improved XML Completion
- Added `AndroidDrawableHandler` for drawable resource files.
- Added support for suggesting drawable tags, attributes, and values in XML.
- Added `NamespaceInsertHandler` for intelligent XML namespace completion.
- Implemented namespace prefix suggestions (`xmlns:android`, `xmlns:app`, `xmlns:tools`) in Layout, Drawable, Values, and Manifest handlers.
