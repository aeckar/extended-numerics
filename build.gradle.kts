plugins {
    id("root.publication")

    // Ensure same plugin version for all sub-modules
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
}
