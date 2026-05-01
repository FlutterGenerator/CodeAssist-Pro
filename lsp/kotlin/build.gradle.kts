import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

plugins {
	id("com.android.library")
	id("kotlin-android")
	id("kotlin-kapt")
}

android {
	namespace = "dev.mutwakil.codeassist.lsp.kotlin"

	compileSdk = 36

	kotlin.compilerOptions {
		freeCompilerArgs.addAll("-Xcontext-parameters")
	}
	compileOptions{
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
}

kapt {
	arguments {
		arg("eventBusIndex", "dev.mutwakil.codeassist.events.LspKotlinEventsIndex")
	}
}
kotlin{
	compilerOptions{
		jvmTarget = JvmTarget.JVM_17
	}
}
dependencies {
	kapt(projects.annotationProcessors)

	implementation(project(":actions-api"))
	implementation(project(":lsp:api"))
	implementation(project(":lsp:jvm-symbol-index"))
	implementation(project(":completion-api"))
	implementation(project(":editor-api"))
	implementation(project(":event:eventbus-events"))
	implementation(project(":kotlin-analysis-api"))
	implementation(project(":project"))

	implementation(libs.common.jsonrpc)
	implementation(libs.common.kotlin)
	implementation(libs.common.kotlin.coroutines.core)
	implementation(libs.common.kotlin.coroutines.android)

	implementation(project(":common"))
    implementation(project(":building-logic"))
    implementation(project(":lsp:indexing"))
	implementation(projects.utilities.shared)
	implementation(projects.codeEditor)
//	implementation(projects.logging.logger)
}
