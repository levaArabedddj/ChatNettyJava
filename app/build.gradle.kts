plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.chatchat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.chatchat"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            storeFile = file("release-key.jks")
            storePassword = "200504"
            keyAlias = "releaseKey"
            keyPassword = "200504"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }




    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // ← Вот этот блок
    packaging {
        resources {
            // берём первый попавшийся INDEX.LIST и версии Netty, остальные игнорируем
            pickFirsts += listOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties"
            )
            // если будут другие дублирующиеся properties-файлы в META-INF,
            // можно использовать wildcard:
            // pickFirsts += "META-INF/*.properties"
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.netty.all)
}
