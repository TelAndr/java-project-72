import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("java")
    id ("checkstyle")
    id("org.sonarqube") version "6.2.0.5505"
    id("jacoco")
    id("application")
    id("io.freefair.lombok") version "9.5.0"
    id("com.github.ben-manes.versions") version "0.54.0"
}

//application {
//    mainClass.set("hexlet.code.App")
//}

sonar {
    properties {
        property("sonar.projectKey", "TelAndr_java-project-72")
        property("sonar.organization", "telandr1987")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

sonarqube {
    properties {
        property ("sonar.projectKey", "TelAndr_java-project-72")
        property ("sonar.host.url", "https://sonarcloud.io")
        property ("sonar.login", "${System.getenv("SONAR_TOKEN")}") // Используйте переменную окружения для вашего токена
        property ("sonar.coverage.jacoco.xmlReportPaths", file("build/reports/jacoco/test/jacocoTestReport.xml"))
    }
}

jacoco {    toolVersion = "0.8.11"}
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                //value = "COVERED"
                minimum = 0.20.toBigDecimal()
            }
        }
    }
}
tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

group = "hexlet.code"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    implementation("gg.jte:jte:3.2.3")
    implementation("io.javalin:javalin:7.2.2")
    implementation("io.javalin:javalin-bundle:7.2.2")
    implementation("io.javalin:javalin-rendering-jte:7.2.2")
    implementation("org.slf4j:slf4j-api:2.0.18")
    implementation("com.konghq:unirest-java:3.14.5")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.eclipse.jetty:jetty-server:11.0.24")
    implementation("org.eclipse.jetty:jetty-servlet:11.0.24")
    implementation("org.eclipse.jetty:jetty-http:11.0.24")
    implementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    runtimeOnly("com.h2database:h2:2.2.224")
    implementation("org.apache.commons:commons-text:1.15.0")
    implementation("org.jsoup:jsoup:1.22.2")
}

tasks.test {
    useJUnitPlatform()
}