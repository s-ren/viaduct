package cup.gradle

class CupPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            apply(plugin = "java")

            dependencies {
                implementation("com.github.vbmacher:java-cup-runtime:11b-20160615")
            }

            val compileCup by tasks.registering(CupCompileTask::class)
            tasks.compileJava { dependsOn(compileCup) }
            sourceSets.main { java.srcDir(compileCup.generateDir) }
        }
    }
}

