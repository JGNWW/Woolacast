import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.baselineprofile)
}

/**
 * Doorloopt de app op een emulator en schrijft op welke code daarbij draait.
 * Die code compileert Android dan bij de installatie al vooraf, in plaats van
 * hem bij de eerste starts regel voor regel te vertalen: sneller opstarten en
 * soepeler scrollen.
 *
 *     ./gradlew :app:generateBaselineProfile
 *
 * Zonder toestel kan dat ook via de workflow "Baseline Profile maken".
 */
android {
    namespace = "nl.woolacast.baselineprofile"
    compileSdk = 36

    defaultConfig {
        // Een profiel opnemen kan pas vanaf Android 9.
        minSdk = 28
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions.managedDevices.allDevices {
        create<ManagedVirtualDevice>("pixel6Api34") {
            device = "Pixel 6"
            apiLevel = 34
            systemImageSource = "aosp"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

baselineProfile {
    managedDevices += "pixel6Api34"
    useConnectedDevices = false
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.uiautomator)
}
