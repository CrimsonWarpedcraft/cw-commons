import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.spotbugs.snom.SpotBugsTask
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

plugins {
    checkstyle
    id("com.github.spotbugs") version "6.5.5"
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

version = if (!hasProperty("ver")) {
    "${getTime()}-SNAPSHOT"
} else {
    val ver = property("ver") as String
    val base = if (ver.startsWith("v")) ver.drop(1) else ver.replace('/', '-')
    if (ver.startsWith("v") && !ver.contains("-rc-")) base else "$base-SNAPSHOT"
}

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
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.70-stable")
    compileOnly("dev.jorel:commandapi-paper-shade:11.2.0")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.10.2")
    compileOnly("org.mongodb:mongodb-driver-sync:5.8.0")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.22.0")
    api("org.hibernate.validator:hibernate-validator:9.1.0.Final")
    implementation("org.xerial:sqlite-jdbc:3.53.2.0")
    "spotbugsPlugins"("com.h3xstream.findsecbugs:findsecbugs-plugin:1.14.0")
    testCompileOnly("com.github.spotbugs:spotbugs-annotations:4.10.2")
    testImplementation("io.papermc.paper:paper-api:26.1.2.build.70-stable")
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

checkstyle {
    toolVersion = "13.5.0"
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
