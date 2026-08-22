plugins {
    `java-library`
}

dependencies {
    api(project(":cellulosesz-api"))

    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    implementation(libs.jackson.databind)
    implementation(libs.jackson.yaml)
    implementation(libs.jackson.kotlin)
    implementation(libs.classgraph)
    implementation(libs.adventure.minimessage)
}
