package cup.gradle

class CupCompileTask : DefaultTask() {
    @InputDirectory
    @Optional
    val sourceDir: File = project.file("src/main/cup")

    @OutputDirectory
    @Optional
    val generateDir: File = project.file("${project.buildDir}/generated-src/cup")

    @Input
    @Optional
    val cupArguments: List<String> = listOf("-interface")

    @TaskAction
    fun compileAll() {
        val cupFiles = project.fileTree(sourceDir) { include("**/*.cup") }

        if (cupFiles.filter { !it.isDirectory }.isEmpty) {
            logger.warn("no cup files found")
        }

        cupFiles.visit {
            if (!this.isDirectory) {
                compileFile(this.file)
            }
        }
    }

    private fun compileFile(cupFile: File) {
        val className: String = cupFile.nameWithoutExtension
        val outputDirectory = TODO()

        project.mkdir(outputDirectory)


    }
}
