import com.vanniktech.maven.publish.SonatypeHost

plugins {
	kotlin("jvm") version "2.4.21"
	`java-library`
	signing
	id("com.vanniktech.maven.publish") version "0.31.0"
}

group = "io.github.nayasis"

fun Project.findReleaseVersionProperty(): String? =
	findProperty("mavenReleaseVersion")?.toString()?.takeUnless { it.isBlank() || it == "unspecified" }

fun Project.findReleaseVersionFromBranch(): String? {
	val branchName = providers.environmentVariable("GITHUB_REF_NAME").orNull?.trim().takeUnless { it.isNullOrBlank() } ?: run {
		try {
			providers.exec {
				commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
				isIgnoreExitValue = true
			}.standardOutput.asText.get().trim().takeUnless { it.isBlank() || it == "HEAD" }
		} catch (_: Exception) {
			return null
		}
	}

	return branchName
		?.removePrefix("refs/heads/")
		?.takeIf { it.startsWith("release/") }
		?.removePrefix("release/")
		?.takeIf { it.isNotBlank() }
}

version = findReleaseVersionProperty()
	?: findReleaseVersionFromBranch()
	?: "0.1.0-SNAPSHOT"

repositories {
	mavenLocal()
	mavenCentral()
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(25))
	}
}

dependencies {

	implementation(platform("com.fasterxml.jackson:jackson-bom:2.21.7"))
	implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.11.0"))

	implementation("io.github.nayasis:basica-kt:0.4.0")
	implementation("commons-cli:commons-cli:1.11.0")
	implementation("org.jclarion:image4j:0.7")
	implementation("com.github.usefulness:webp-imageio:0.11.0")
	implementation("org.yaml:snakeyaml:2.5")
	implementation("ch.qos.logback:logback-classic:1.6.5")
	implementation("com.squareup.okhttp3:okhttp:5.5.0")
	implementation("com.fasterxml.jackson.core:jackson-annotations")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

	// reflection is used by SimpleDiContainer.
	implementation(kotlin("reflect"))
	implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
	api("org.jetbrains.kotlinx:kotlinx-coroutines-core")
	api("org.jetbrains.kotlinx:kotlinx-coroutines-swing")

	// test
	testImplementation(kotlin("test"))
	testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
	// theme for demo apps and UI tests. the library itself is look-and-feel agnostic.
	testImplementation("com.formdev:flatlaf:3.7.2")

}

// Build on the JDK 25 toolchain, keeping Java and Kotlin bytecode targets at 24.
kotlin {
	compilerOptions {
		jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_24)
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<JavaCompile> {
	options.release.set(24)
}

tasks.withType<Test> {
	useJUnitPlatform()
}

mavenPublishing {
	if(listOf("publishToMavenLocal","publishMavenPublicationToMavenLocal").none{gradle.startParameter.taskNames.contains(it)}) {
		signAllPublications()
	}
	publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
	pom {
		name        = project.name
		description = "Basic Swing library based on Kotlin"
		url         = "https://github.com/nayasis/basica-swing"
		licenses {
			license {
				name = "Apache License, Version 2.0"
				url  = "http://www.apache.org/licenses/LICENSE-2.0.txt"
			}
		}
		developers {
			developer {
				id    = "nayasis"
				name  = "nayasis"
				email = "nayasis@gmail.com"
			}
		}
		scm {
			url                 = "https://github.com/nayasis/basica-swing"
			connection          = "scm:git:github.com/nayasis/basica-swing.git"
			developerConnection = "scm:git:ssh://github.com/nayasis/basica-swing.git"
		}
	}
}
