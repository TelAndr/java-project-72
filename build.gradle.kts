plugins {
    id("java")
    id ("checkstyle")
    id("org.sonarqube") version "6.2.0.5505"
    id("jacoco")
    id("application")
}

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

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("io.javalin:javalin:7.2.2")
    implementation("org.slf4j:slf4j-api:2.0.18")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.6")
    implementation("com.zaxxer:HikariCP:5.1.0")
}

tasks.test {
    useJUnitPlatform()
}