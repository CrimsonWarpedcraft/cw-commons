import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.spotbugs.snom.SpotBugsTask
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    checkstyle
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.shadow)
    `java-library`
    `maven-publish`
}

group = "com.crimsonwarpedcraft.cwcommons"

fun getTime(): String {
    val sdf = SimpleDateFormat("yyMMdd-HHmm")
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}

version = (if (!hasProperty("ver")) {
    "${getTime()}-SNAPSHOT"
} else {
    val ver = property("ver") as String
    val base = if (ver.startsWith("v")) ver.drop(1) else ver.replace('/', '-')
    if (ver.startsWith("v") && !ver.lowercase().contains("-rc-")) base else "$base-SNAPSHOT"
}).uppercase()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
        content {
            includeModule("io.papermc.paper", "paper-api")
            includeGroup("io.papermc.adventure")
            includeModule("net.md-5", "bungeecord-chat")
        }
    }

    maven {
        name = "minecraft"
        url = uri("https://libraries.minecraft.net")
        content {
            includeModule("com.mojang", "brigadier")
        }
    }

    mavenCentral()
}

val mockitoAgent = configurations.create("mockitoAgent")
val integrationTestSourceSet = sourceSets.create("integrationTest")

configurations[integrationTestSourceSet.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())
configurations[integrationTestSourceSet.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.commandapi.paper.shade) {
        // Upstream metadata references unpublished modules already bundled in this JAR.
        // https://github.com/CommandAPI/CommandAPI/issues/704
        isTransitive = false
    }
    compileOnly(libs.spotbugs.annotations)
    compileOnly(libs.mongodb.driver.sync)
    api(libs.jackson.dataformat.yaml)
    api(libs.hibernate.validator)
    implementation(libs.sqlite.jdbc)
    "spotbugsPlugins"(libs.findsecbugs.plugin)
    testCompileOnly(libs.spotbugs.annotations)
    testImplementation(libs.paper.api)
    testImplementation(libs.commandapi.paper.shade) {
        // Upstream metadata references unpublished modules already bundled in this JAR.
        // https://github.com/CommandAPI/CommandAPI/issues/704
        isTransitive = false
    }
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mongodb.driver.sync)
    testImplementation(libs.mockito.core)
    testRuntimeOnly(libs.junit.platform.launcher)
    mockitoAgent(libs.mockito.core) { isTransitive = false }
    add(integrationTestSourceSet.implementationConfigurationName, sourceSets.main.get().output)
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-javaagent:${mockitoAgent.asPath}")

    val jar = tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile }
    val pom = tasks.named<GenerateMavenPom>("generatePomFileForShadowPublication").map { it.destination }
    inputs.file(jar).withPropertyName("shadowJar").withPathSensitivity(PathSensitivity.NONE)
    inputs.file(pom).withPropertyName("publicationPom").withPathSensitivity(PathSensitivity.NONE)
    systemProperty("packaging.jar", jar.get().asFile.absolutePath)
    systemProperty("packaging.pom", pom.get().absolutePath)
    systemProperty("packaging.jacksonVersion", libs.versions.jackson.get())
    systemProperty("packaging.validatorVersion", libs.versions.hibernate.validator.get())
}

tasks.register<Test>("integrationTest") {
    description = "Runs tests against pre-provisioned external services."
    group = "verification"
    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath
    useJUnitPlatform()
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
    shouldRunAfter(tasks.test)

    doFirst {
        val requiredVariables = listOf(
            "CW_COMMONS_MONGO_URI",
            "CW_COMMONS_MONGO_DATABASE"
        )
        val missingVariables = requiredVariables.filter {
            providers.environmentVariable(it).orNull.isNullOrBlank()
        }
        if (missingVariables.isNotEmpty()) {
            throw GradleException(
                "integrationTest requires environment variables: ${missingVariables.joinToString()}"
            )
        }
    }
}

tasks.javadoc {
    val mongoDocsVersion = libs.versions.mongodb.get().substringBeforeLast(".")
    title = "CwCommons ${project.version} API"
    // Keep documentation errors visible without failing the release; -missing suppresses
    // missing-documentation lint messages.
    isFailOnError = false
    (options as StandardJavadocDocletOptions).apply {
        windowTitle = "CwCommons ${project.version} API"
        docTitle = "CwCommons ${project.version} API"
        encoding = "UTF-8"
        docEncoding = "UTF-8"
        charSet = "UTF-8"
        source = "25"
        addStringOption("Xdoclint:all,-missing", "-quiet")
        // Online -link targets need an accessible element-list or package-list. Fetch failures
        // are reported as errors and leave types unlinked. Paper/CommandAPI are omitted
        // because their hosted element-lists are unstable.
        // Jackson 2.23.0 is not hosted on javadoc.io yet; retain the available 2.22.0 reference.
        links(
            "https://docs.oracle.com/en/java/javase/25/docs/api/",
            "https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-databind/2.22.0/",
            "https://mongodb.github.io/mongo-java-driver/$mongoDocsVersion/apidocs/driver-sync/",
            "https://javadoc.io/doc/jakarta.validation/jakarta.validation-api/3.1.1/"
        )
    }
}

// Bundled as a GitHub Release asset so every version's API docs stay downloadable, even though
// the published site (docs.yml) only serves the latest. Standalone (not wired into `build`) so
// normal builds stay fast; the `release` task pulls it into build/libs/ for upload.
val javadocJar = tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    maxWarnings = 0
}

configurations.named("checkstyle") {
    resolutionStrategy.capabilitiesResolution
        .withCapability("com.google.collections:google-collections") {
            select("com.google.guava:guava:23.0")
        }
}

tasks.withType<Checkstyle>().configureEach {
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
}

tasks.withType<SpotBugsTask>().configureEach {
    reports.create("html") {
        required.set(true)
    }
    reports.create("xml") {
        required.set(false)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    // Bundle the complete SQLite driver (including SPI and natives). Select by artifact so
    // resources and multi-release classes from the unbundled API libraries cannot leak in.
    dependencies {
        include(dependency(libs.sqlite.jdbc.get()))
    }
}

tasks.jar {
    enabled = false
}

tasks.assemble {
    dependsOn(tasks.named("shadowJar"))
}

tasks.register("printProjectName") {
    doLast { println(rootProject.name) }
}

tasks.register("release") {
    dependsOn(tasks.named("build"))
    dependsOn(javadocJar)

    doLast {
        tasks.named<ShadowJar>("shadowJar").get().archiveFile.get().asFile
            .renameTo(layout.buildDirectory.get().asFile.resolve("libs/${rootProject.name}.jar"))
    }
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            artifactId = "cw-commons"
            artifact(tasks.named<ShadowJar>("shadowJar"))
            // Published so JitPack hosts per-version Javadoc at
            // https://jitpack.io/com/github/CrimsonWarpedcraft/cw-commons/<version>/javadoc/
            artifact(javadocJar)
            pom.withXml {
                val deps = asNode().appendNode("dependencies")
                configurations["api"].dependencies.forEach { dep ->
                    val node = deps.appendNode("dependency")
                    node.appendNode("groupId", dep.group)
                    node.appendNode("artifactId", dep.name)
                    node.appendNode("version", dep.version)
                    node.appendNode("scope", "compile")
                }
            }
        }
    }
}
