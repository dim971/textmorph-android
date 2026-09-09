import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    // AGP 9 brings Kotlin support in-box; the separate kotlin-android plugin is
    // gone, and the Compose compiler comes with buildFeatures.compose.
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
}

version = "0.1.0"
group = "io.github.dim971"

android {
    namespace = "io.github.dim971.textmorph"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// Configured, never run: publishing needs credentials and a signing key this
// repository does not carry. `./gradlew publishToMavenLocal` works for trying a
// consumer against an unreleased build.
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "textmorph-compose", version.toString())

    pom {
        name.set("TextMorph for Jetpack Compose")
        description.set(
            "Text continuity for native interfaces: the characters, words and digits that " +
                "survive a change of value move instead of fading.",
        )
        url.set("https://github.com/dim971/textmorph-android")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("dim971")
                name.set("Dimitri Merault")
            }
        }
        scm {
            url.set("https://github.com/dim971/textmorph-android")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        // The library ships. A warning here is a warning in every consumer's
        // build log, and one that is tolerated becomes one that is ignored.
        allWarningsAsErrors = true
    }
}

// Explicit API is a rule about the published surface, so it applies to the
// library's own sources and not to the tests, which have no published surface
// and would only gain `internal` on every declaration.
tasks
    .withType<KotlinJvmCompile>()
    .matching { !it.name.contains("Test") }
    .configureEach {
        compilerOptions.freeCompilerArgs.add("-Xexplicit-api=strict")
    }

dependencies {
    implementation(platform(libs.compose.bom))
    api(libs.compose.foundation)
    api(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    // only to read the golden fixtures; never reaches the published artefact
    testImplementation(libs.gson)
}
