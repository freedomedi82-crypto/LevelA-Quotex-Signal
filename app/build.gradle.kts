plugins {
    id("com.android.application")
}

android {
    namespace = "com.levela.quotexsignal"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.levela.quotexsignal"
        minSdk = 24
        targetSdk = 37
        versionCode = 3
        versionName = "0.3.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("LEVELA_KEYSTORE_PATH")
            val keystorePassword = System.getenv("LEVELA_KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("LEVELA_KEY_ALIAS")
            val keyPassword = System.getenv("LEVELA_KEY_PASSWORD")
            if (!keystorePath.isNullOrBlank() && !keystorePassword.isNullOrBlank()
                && !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
