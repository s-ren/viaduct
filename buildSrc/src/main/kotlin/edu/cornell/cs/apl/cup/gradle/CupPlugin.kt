package edu.cornell.cs.apl.cup.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.registering

class CupPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            apply(plugin = "java")

            dependencies {
                "runtimeOnly"("com.github.vbmacher:java-cup-runtime:11b-20160615")
            }

            val compileCup by tasks.registering(CupCompileTask::class)
            tasks.named("compileJava") { dependsOn(compileCup) }
            sourceSets.main { java.srcDir(compileCup.generateDir) }
        }
    }
}
