dependencies {
    api(project(":cellulosesz-api"))

    implementation(libs.jackson.databind)
    implementation(libs.jackson.yaml)
    implementation(libs.classgraph)
}
