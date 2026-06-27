import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.spotbugs.snom.SpotBugsTask
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    checkstyle
    id("com.github.spotbugs") version "6.5.8"
    id("com.gradleup.shadow") version "9.4.2"
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

val mockitoAgent by configurations.creating

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.72-stable")
    compileOnly("dev.jorel:commandapi-paper-shade:11.2.0")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.10.2")
    compileOnly("org.mongodb:mongodb-driver-sync:5.8.0")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.22.0")
    api("org.hibernate.validator:hibernate-validator:9.1.1.Final")
    implementation("org.xerial:sqlite-jdbc:3.53.2.0")
    "spotbugsPlugins"("com.h3xstream.findsecbugs:findsecbugs-plugin:1.14.0")
    testCompileOnly("com.github.spotbugs:spotbugs-annotations:4.10.2")
    testImplementation("io.papermc.paper:paper-api:26.1.2.build.72-stable")
    testImplementation("dev.jorel:commandapi-paper-shade:11.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.0")
    testImplementation("org.mongodb:mongodb-driver-sync:5.8.0")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.0")
    mockitoAgent("org.mockito:mockito-core:5.23.0") { isTransitive = false }
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
}

tasks.javadoc {
    title = "CwCommons ${project.version} API"
    // A lint nit must never fail the docs release job; -missing also avoids noise from
    // package-private records/lambdas. Real reference errors still surface as warnings.
    isFailOnError = false
    (options as StandardJavadocDocletOptions).apply {
        windowTitle = "CwCommons ${project.version} API"
        docTitle = "CwCommons ${project.version} API"
        encoding = "UTF-8"
        docEncoding = "UTF-8"
        charSet = "UTF-8"
        source = "25"
        addStringOption("Xdoclint:all,-missing", "-quiet")
        // Online -link targets: unreachable element-lists only warn (types render unlinked),
        // so a wrong/missing target never produces broken hyperlinks. Paper/CommandAPI omitted
        // (their hosted element-lists are unstable).
        links(
            "https://docs.oracle.com/en/java/javase/25/docs/api/",
            "https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-databind/2.22.0/",
            "https://javadoc.io/doc/org.mongodb/mongodb-driver-sync/5.8.0/",
            "https://javadoc.io/doc/jakarta.validation/jakarta.validation-api/3.1.1/"
        )
    }
}

// Bundled as a GitHub Release asset so every version's API docs stay downloadable, even though
// the published site (docs.yml) only serves the latest. Standalone (not wired into `build`) so
// normal builds stay fast; the `release` task pulls it into build/libs/ for upload.
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

checkstyle {
    toolVersion = "13.6.0"
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
    mergeServiceFiles()
    exclude("com/fasterxml/**")
    exclude("org/yaml/**")
    exclude("org/hibernate/**")
    exclude("jakarta/**")
    exclude("org/jboss/**")
    minimize {
        exclude(dependency("org.xerial:sqlite-jdbc:.*"))
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
