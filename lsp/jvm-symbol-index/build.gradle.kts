import com.itsaky.androidide.build.config.BuildConfig

plugins {
	id("com.android.library")
	id("kotlin-android")
}

android {
	namespace = "${BuildConfig.PACKAGE_NAME}.lsp.java.indexing"
}

kotlin {
	compilerOptions{
		jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
	}
}

dependencies {
	api(libs.google.protobuf.java)
	api(libs.google.protobuf.kotlin)
	api(libs.kotlinx.coroutines.core)
	api(libs.kotlinx.metadata)
	api(projects.event.eventbus)
	implementation(projects.buildingLogic)

	api(projects.common)
	api(projects.lsp.indexing)
	api(projects.lsp.jvmSymbolModels)
	api(projects.kotlinAnalysisApi)
	api(projects.project)
	api(projects.logging)
}
