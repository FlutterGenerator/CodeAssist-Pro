import com.itsaky.androidide.build.config.BuildConfig

plugins {
	id("com.android.library")
	id("kotlin-android")
}

android {
	namespace = "${BuildConfig.PACKAGE_NAME}.lsp.indexing"
}

kotlin {
	compilerOptions{
		jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
	}
}

dependencies {
	api(libs.androidx.annotation)
	api(libs.androidx.sqlite.ktx)
	api(libs.androidx.sqlite.framework)
	api(libs.kotlinx.coroutines.core)
	api(libs.tooling.slf4j)

//	api(projects.logge
}
