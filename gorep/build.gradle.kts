plugins {
    id("idea")
    id("kotlin-multiplatform")
    id("kotlinx-serialization")
    id("com.github.johnrengelman.shadow").version("5.2.0")
}
idea {
    module {
        isDownloadSources = true
    }
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
    if (pw.binom.Target.LINUX_ARM32HFP_SUPPORT) {
        linuxArm32Hfp { // Use your target instead.
            binaries {
                executable {
                    entryPoint = nativeEntryPoint
                }
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
    if (pw.binom.Target.MINGW_X86_SUPPORT) {
        mingwX86 { // Use your target instead.
            binaries {
                executable {
                    entryPoint = nativeEntryPoint
                }
            }
        }
    }
    macosX64 {
        binaries {
            executable()
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
            kotlin.srcDir("build/gen")
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

        if (pw.binom.Target.MINGW_X86_SUPPORT) {
            val mingwX86Main by getting {
                dependencies {
                    dependsOn(mingwX64Main)
                }
            }
        }

        if (pw.binom.Target.LINUX_ARM32HFP_SUPPORT) {
            val linuxArm32HfpMain by getting {
                dependencies {
                    dependsOn(linuxX64Main)
                }
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

    val generateVersion = create("generateVersion") {
        val sourceDir = project.buildDir.resolve("gen/pw/binom/gorep")
        sourceDir.mkdirs()
        val versionSource = sourceDir.resolve("version.kt")
        outputs.files(versionSource)
        inputs.property("version", project.version)

        versionSource.writeText(
                """package pw.binom.gorep
            
const val GOREP_VERSION = "${project.version}""""
        )
    }

    if (pw.binom.Target.LINUX_ARM32HFP_SUPPORT) {
        this["compileKotlinLinuxArm32Hfp"].dependsOn(generateVersion)
    }
    if (pw.binom.Target.LINUX_ARM64_SUPPORT) {
        this["compileKotlinLinuxArm64"].dependsOn(generateVersion)
    }
//    this["compileKotlinLinuxMips32"].dependsOn(generateVersion)
//    this["compileKotlinLinuxMipsel32"].dependsOn(generateVersion)
    this["compileKotlinLinuxX64"].dependsOn(generateVersion)
    this["compileKotlinMacosX64"].dependsOn(generateVersion)
    this["compileKotlinMingwX64"].dependsOn(generateVersion)
    if (pw.binom.Target.MINGW_X86_SUPPORT) {
        this["compileKotlinMingwX86"].dependsOn(generateVersion)
    }
//    this["compileKotlinJsIr"].dependsOn(generateVersion)
//    this["compileKotlinJsLegacy"].dependsOn(generateVersion)
    this["compileKotlinJvm"].dependsOn(generateVersion)
}