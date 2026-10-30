plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val requestedReleaseVersion = providers.environmentVariable("JJ_RELEASE_VERSION").orNull
val releaseCode = requestedReleaseVersion?.let { version ->
    val match = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""").matchEntire(version)
        ?: error("JJ_RELEASE_VERSION deve ser MAJOR.MINOR.PATCH, ex.: 0.2.0")
    val (major, minor, patch) = match.destructured
    val numericCode = major.toInt() * 1_000_000 + minor.toInt() * 1_000 + patch.toInt() + 1
    require(numericCode > 1 && numericCode <= 2_100_000_000) { "versionCode inválido" }
    numericCode
}

val signingVariables = listOf("JJ_KEYSTORE_PATH", "JJ_STORE_PASSWORD", "JJ_KEY_ALIAS", "JJ_KEY_PASSWORD")
    .associateWith { providers.environmentVariable(it).orNull }
val signingConfigured = signingVariables.values.all { !it.isNullOrBlank() }
require(signingVariables.values.none { !it.isNullOrBlank() } || signingConfigured) {
    "Informe todas as variáveis JJ_* de assinatura para a versão release."
}

android {
    namespace = "dev.jj.launcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.jj.launcher"
        minSdk = 26
        targetSdk = 35
        versionCode = releaseCode ?: 2
        versionName = requestedReleaseVersion ?: "0.2.0-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (signingConfigured) {
        signingConfigs {
            create("jjRelease") {
                storeFile = file(requireNotNull(signingVariables["JJ_KEYSTORE_PATH"]))
                storePassword = requireNotNull(signingVariables["JJ_STORE_PASSWORD"])
                keyAlias = requireNotNull(signingVariables["JJ_KEY_ALIAS"])
                keyPassword = requireNotNull(signingVariables["JJ_KEY_PASSWORD"])
            }
        }
        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("jjRelease")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.05.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
