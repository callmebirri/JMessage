import java.time.Instant

plugins {
    kotlin("jvm") version "2.4.10"
    id("com.gradleup.shadow") version "9.6.1"
}

val generateGitProperties by tasks.registering {
    group = "build"
    description = "Generates git.properties file with commit information"

    val outputFile = layout.buildDirectory.file("generated/resources/git/git.properties")

    inputs.property("git.head", providers.exec {
        commandLine("git", "rev-parse", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.map { it.trim() }.orElse("unknown"))

    outputs.file(outputFile)

    doLast {
        fun runGit(command: String): String = providers.exec {
            commandLine("git", *command.split(" ").toTypedArray())
            isIgnoreExitValue = true
        }.standardOutput.asText.map { it.trim() }.getOrElse("unknown")

        val props = mapOf(
            "git.commit.id" to runGit("rev-parse HEAD"),
            "git.commit.id.abbrev" to runGit("rev-parse --short HEAD"),
            "git.commit.message.short" to runGit("log -1 --pretty=%s"),
            "git.commit.time" to runGit("log -1 --pretty=%cI"),
            "git.branch" to runGit("rev-parse --abbrev-ref HEAD"),
            "git.build.time" to Instant.now().toString(),
            "git.dirty" to (runGit("status --porcelain").isNotBlank()).toString()
        )

        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(props.entries.joinToString("\n") { "${it.key}=${it.value}" })
        }
    }
}

val gitCommitShort = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim() }.getOrElse("unknown")

sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.dir("generated/resources/git"))
        }
    }
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly(platform("net.kyori:adventure-bom:4.26.1"))
    compileOnly("net.kyori:adventure-api")
    compileOnly("net.kyori:adventure-text-minimessage")
    compileOnly("net.kyori:adventure-platform-bukkit:4.4.1")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    processResources {
        dependsOn(generateGitProperties)

        val props = mapOf(
            "name" to project.name,
            "version" to project.version.toString(),
            "gitCommit" to gitCommitShort,
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("${project.name}-${project.version}+${gitCommitShort}.jar")

        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/MANIFEST.MF")
        exclude("META-INF/NOTICE*")
        exclude("META-INF/versions/**")
        exclude("META-INF/maven/**")
        exclude("META-INF/proguard/**")
    }

    build {
        dependsOn(shadowJar)
    }

    register("printGitInfo") {
        group = "help"
        description = "Prints current git information"
        doLast {
            file(layout.buildDirectory.file("generated/resources/git/git.properties").get().asFile)
                .takeIf { it.exists() }
                ?.readLines()
                ?.forEach { println(it) }
                ?: println("Git properties not generated yet. Run 'build' first.")
        }
    }
}