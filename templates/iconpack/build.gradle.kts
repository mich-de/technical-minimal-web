plugins {
  alias(libs.plugins.android.application)
}

// Icon pack "LeanBox Icons" (formato ADW/Nova: appfilter.xml + drawable.xml).
// Le icone si rigenerano con: python3 -I iconpack/tools/gen_icons.py
android {
    namespace = "com.michde.leanbox.icons"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.michde.leanbox.icons"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}
