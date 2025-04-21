plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    id("signing")
}

android {
    namespace = "com.tans.tapm.autoinit"
    compileSdk = properties["ANDROID_COMPILE_SDK"].toString().toInt()

    defaultConfig {
        minSdk = properties["ANDROID_MIN_SDK"].toString().toInt()

        version = properties["VERSION_NAME"].toString()

        setProperty("archivesBaseName", "${project.name}-${properties["VERSION_NAME"].toString()}")
        buildConfigField("String", "VERSION", "\"${properties["VERSION_NAME"].toString()}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.annotaion)
    implementation(libs.androidx.startup.runtime)
    implementation(project(":tapm-core"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}


publishing {
    repositories {
        maven {
            name = "MavenCentralRelease"
            credentials {
                username = properties["MAVEN_USERNAME"].toString()
                password = properties["MAVEN_PASSWORD"].toString()
            }
            url = uri(properties["RELEASE_REPOSITORY_URL"].toString())
        }
        maven {
            name = "MavenCentralSnapshot"
            credentials {
                username = properties["MAVEN_USERNAME"].toString()
                password = properties["MAVEN_PASSWORD"].toString()
            }
            url = uri(properties["SNAPSHOT_REPOSITORY_URL"].toString())
        }
        maven {
            name = "MavenLocal"
            url = uri(File(rootProject.projectDir, "maven"))
        }
    }

    publications {
        val defaultPublication = this.create("Default", MavenPublication::class.java)
        with(defaultPublication) {
            groupId = properties["GROUP_ID"].toString()
            artifactId = project.name
            version = properties["VERSION_NAME"].toString()

            afterEvaluate {
                artifact(tasks.getByName("bundleReleaseAar"))
            }
            val sourceCode by tasks.creating(Jar::class.java) {
                archiveClassifier.convention("sources")
                archiveClassifier.set("sources")
                from(android.sourceSets.getByName("main").java.srcDirs)
            }
            artifact(sourceCode)
            pom {
                name = "tApm"
                description = "Android application performance monitoring libs."
                url = "https://github.com/tans5/tApm.git"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "tanpengcheng"
                        name = "tans5"
                        email = "tans.tan096@gmail.com"
                    }
                }
                scm {
                    url.set("https://github.com/tans5/tApm.git")
                }
            }

            pom.withXml {
                val dependencies = asNode().appendNode("dependencies")
                configurations.implementation.get().allDependencies.all {
                    val dependency = this
                    if (dependency.group == null || dependency.version == null || dependency.name == "unspecified") {
                        return@all
                    }
                    val dependencyNode = dependencies.appendNode("dependency")
                    dependencyNode.appendNode("groupId", dependency.group)
                    dependencyNode.appendNode("artifactId", dependency.name)
                    dependencyNode.appendNode("version", dependency.version)
                    dependencyNode.appendNode("scope", "implementation")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications.getByName("Default"))
}