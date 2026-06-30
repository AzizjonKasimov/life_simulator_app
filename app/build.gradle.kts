import java.io.FileInputStream
import java.util.Properties
import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}

android {
    namespace = "com.azizjonkasimov.lifesimulator"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.azizjonkasimov.lifesimulator"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
        vectorDrawables { useSupportLibrary = true }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.org.json)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

tasks.register("copyReleaseApkToRoot") {
    group = "distribution"
    description = "Copies the signed release APK to the project root for phone installation."

    doLast {
        val builtApk = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
        val unsignedApk = layout.buildDirectory.file("outputs/apk/release/app-release-unsigned.apk").get().asFile
        if (!builtApk.exists()) {
            if (unsignedApk.exists()) {
                logger.warn(
                    "Unsigned release APK was produced, so no root install APK was copied. " +
                        "Create local release signing files first; see README.md.",
                )
                return@doLast
            }
            throw GradleException("Release APK was not produced. Run assembleRelease first.")
        }

        val versionName = android.defaultConfig.versionName ?: "dev"
        val versionedApk = rootProject.file("LifeSimulator-$versionName.apk")
        val latestApk = rootProject.file("LifeSimulator-latest.apk")
        builtApk.copyTo(versionedApk, overwrite = true)
        builtApk.copyTo(latestApk, overwrite = true)
        logger.lifecycle("Install APK copied to ${latestApk.path}")
        logger.lifecycle("Versioned APK copied to ${versionedApk.path}")
    }
}

afterEvaluate {
    tasks.named("assembleRelease") {
        finalizedBy("copyReleaseApkToRoot")
    }
}
