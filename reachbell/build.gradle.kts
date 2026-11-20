plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

group = "com.reachbell"
version = "1.0.0"

android {
    namespace = "com.reachbell"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

dependencies {
    api(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.org.json)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.reachbell"
            artifactId = "reachbell-android"
            version = project.version.toString()

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("ReachBell Android SDK")
                description.set("Android client SDK for ReachBell: registers the device's FCM token and identifies and tags the subscriber.")
                url.set("https://github.com/reachbell/reachbell-android")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("dotspheres")
                        name.set("DotSpheres Technologies Pvt Ltd")
                        organization.set("DotSpheres Technologies Pvt Ltd")
                    }
                }
                scm {
                    url.set("https://github.com/reachbell/reachbell-android")
                    connection.set("scm:git:https://github.com/reachbell/reachbell-android.git")
                    developerConnection.set("scm:git:ssh://git@github.com/reachbell/reachbell-android.git")
                }
            }
        }
    }
}
