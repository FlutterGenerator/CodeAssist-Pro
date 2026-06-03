import com.google.protobuf.gradle.id
import com.itsaky.androidide.plugins.conf.configureProtoc

plugins {
	id("java-library")
	id("org.jetbrains.kotlin.jvm")
//	alias(libs.plugins.google.protobuf)
	id("com.google.protobuf")
}

configureProtoc(protobuf = protobuf, protocVersion = libs.versions.protobuf.asProvider())

kotlin {
	compilerOptions{
		jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
	}
}

protobuf {
//	protoc {
//		artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.asProvider().get()}"
//	}
	plugins {
		id("kotlin-ext") {
			artifact = "dev.hsbrysk:protoc-gen-kotlin-ext:${libs.versions.protoc.gen.kotlin.ext.get()}:jdk8@jar"
		}
	}
	generateProtoTasks {
		all().forEach { task ->
			task.plugins {
				id("kotlin-ext") {
					outputSubDir = "kotlin"
				}
			}
			task.builtins {
				getByName("java") {
					option("lite")
				}
			}
		}
	}
}

dependencies {
	api(libs.google.protobuf.java)
	api(libs.google.protobuf.kotlin)
}
