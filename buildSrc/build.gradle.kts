plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("cup-plugin") {
            id = "cup"
            implementationClass = "CupPlugin"
        }
    }
}

repositories {
    jcenter()
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}
