plugins {
    id("kotlin-multiplatform")
    id("kotlinx-serialization")
    id("com.github.johnrengelman.shadow").version("5.2.0")
}

val nativeEntryPoint = "pw.binom.gorep.main"

kotlin {
    linuxX64 {
        binaries {
            executable {
                entryPoint = nativeEntryPoint
            }
        }
    }
    linuxArm32Hfp { // Use your target instead.
        binaries {
            executable {
                entryPoint = nativeEntryPoint
            }
        }
    }
    mingwX64 { // Use your target instead.
        binaries {
            executable {
                entryPoint = nativeEntryPoint
            }
        }
    }
    mingwX86 { // Use your target instead.
        binaries {
            executable {
                entryPoint = nativeEntryPoint
            }
        }
    }
    jvm()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                api("pw.binom.io:core:${pw.binom.Versions.BINOM_VERSION}")
                api("pw.binom.io:env:${pw.binom.Versions.BINOM_VERSION}")
                api("pw.binom.io:file:${pw.binom.Versions.BINOM_VERSION}")
                api("pw.binom.io:logger:${pw.binom.Versions.BINOM_VERSION}")
                api("pw.binom.io:compression:${pw.binom.Versions.BINOM_VERSION}")
                api("pw.binom.io:ssl:${pw.binom.Versions.BINOM_VERSION}")
                api("pw.binom.io:process:${pw.binom.Versions.BINOM_VERSION}")
                api("pw.binom.io:webdav:${pw.binom.Versions.BINOM_VERSION}")
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:${pw.binom.Versions.SERIALIZATION_VERSION}")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:${pw.binom.Versions.SERIALIZATION_VERSION}")
            }
        }

        val nativeMain by creating {
            dependencies {
                dependsOn(commonMain)
            }
        }
        val linuxX64Main by getting {
            dependencies {
                dependsOn(nativeMain)
            }
        }
        val mingwX64Main by getting {
            dependencies {
                dependsOn(linuxX64Main)
            }
        }

        val mingwX86Main by getting {
            dependencies {
                dependsOn(mingwX64Main)
            }
        }

        val linuxArm32HfpMain by getting {
            dependencies {
                dependsOn(linuxX64Main)
            }
        }

        val jvmMain by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib:${pw.binom.Versions.KOTLIN_VERSION}")
            }
        }

        val commonTest by getting {
            dependencies {
                api(kotlin("test-common"))
                api(kotlin("test-annotations-common"))
            }
        }
        val jvmTest by getting {
            dependsOn(commonTest)
            dependencies {
                api(kotlin("test-junit"))
            }
        }
        val linuxX64Test by getting {
            dependsOn(commonTest)
        }
    }
}

tasks {
    val jvmJar by getting(Jar::class)

    val shadowJar by creating(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
        from(jvmJar.archiveFile)
        group = "build"
        configurations = listOf(project.configurations["jvmRuntimeClasspath"])
        exclude(
            "META-INF/*.SF",
            "META-INF/*.DSA",
            "META-INF/*.RSA",
            "META-INF/*.txt",
            "META-INF/NOTICE",
            "LICENSE",
        )
        manifest {
            attributes("Main-Class" to "pw.binom.gorep.JvmMain")
        }
    }
}