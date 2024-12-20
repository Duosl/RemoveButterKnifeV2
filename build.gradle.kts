plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
    id("org.jetbrains.compose") version "1.7.3"
}

group = "com.duosl.plugin.rbk"
version = "2.0.2"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2024.1.7")
//    localPath.set("/Applications/IntelliJ IDEA CE.app/Contents")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(
        "com.intellij.java",
        "org.jetbrains.kotlin",
    ))
}

dependencies {
    // 添加 Compose Desktop 需要的依赖项，compose.desktop.currentOs 这个 value 便来自于上面添加的 org.jetbrains.compose 插件
    implementation(compose.desktop.currentOs)
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("243.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
