// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.3.1")
        classpath(kotlin("gradle-plugin", version = "1.7.10"))
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://android-sdk.is.com/")
        maven("https://jitpack.io")
//        maven("https://s01.oss.sonatype.org/content/groups/staging/")
    }
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}