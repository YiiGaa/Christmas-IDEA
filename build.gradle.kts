plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "com.stoprefactoring"
version = "3.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        intellijIdea("2025.2.4")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        bundledPlugins(
            "org.jetbrains.plugins.terminal",
            "com.intellij.java",
            "com.intellij.modules.lsp"
        )
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252.25557"
        }

        changeNotes = """
            Initial version
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

tasks.prepareSandbox {
    // 将 lsp 文件夹直接拷贝到插件安装目录的根部，而不是塞进 lib/*.jar
    from("src/main/resources/lsp") {
        into("${pluginName.get()}/lsp")
    }
}

tasks.withType<org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask> {
    // 強制測試環境開啟內部模式
    systemProperty("idea.is.internal", "true")

    // 如果你想在開發控制台看到 Node 的所有輸出，也可以加上這行
    logging.captureStandardOutput(org.gradle.api.logging.LogLevel.INFO)
}

//sourceSets {
//    main {
//        kotlin.srcDirs("src/main/kotlin")
//        resources.srcDirs("src/main/resources")
//    }
//}


kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
