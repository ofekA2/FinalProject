plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
buildscript{
    repositories{
        google(); mavenCentral()
    }
    dependencies{
        classpath("com.android.tools.build:gradle:8.1.3")
        classpath("com.google.gms:google-services:4.3.15")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.9.2")
    }
}