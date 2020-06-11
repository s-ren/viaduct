package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.backend.BackendCompiler
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.passes.selectProtocols
import edu.cornell.cs.apl.viaduct.passes.splitMain
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine
import guru.nidi.graphviz.engine.GraphvizServerEngine
import guru.nidi.graphviz.engine.GraphvizV8Engine
import java.io.File
import java.io.StringWriter
import java.io.Writer

class Compile : CliktCommand(help = "Compile ideal protocol to secure distributed program") {
    val input: File? by inputProgram()

    val output: File? by outputFile()

    // TODO: option to print inferred label for each variable.
    // TODO: option to print selected protocol for each variable.

    val constraintGraphOutput: File? by option(
        "-c",
        "--constraint-graph",
        metavar = "FILE.EXT",
        help = """
            Write the generated label constraint graph to FILE.EXT

            File extension (EXT) determines the output format.
            Supported formats are the same as the ones in Graphviz.
            Most common ones are svg, png, dot, and json.
        """
    ).file(canBeDir = false)

    // TODO: use this flag.
    val verbose by option("-v", "--verbose", help = "Print debugging information").flag()

    // output intermediate representation instead of backend code.
    val intermediate by
        option("-i", "--intermediate",
            help = "Output intermediate representation"
        ).flag(default = false)

    override fun run() {
        val program = input.parse().elaborated()

        val nameAnalysis = NameAnalysis(Tree(program))
        val typeAnalysis = TypeAnalysis(nameAnalysis)
        val informationFlowAnalysis = InformationFlowAnalysis(nameAnalysis)

        // Dump label constraint graph to a file if requested.
        dumpGraph(informationFlowAnalysis::exportConstraintGraph, constraintGraphOutput)

        // Perform static checks.
        nameAnalysis.check()
        typeAnalysis.check()
        informationFlowAnalysis.check()

        // Select protocols.
        val protocolAssignment: (Variable) -> Protocol =
            program.main.selectProtocols(nameAnalysis, informationFlowAnalysis)
        val protocolAnalysis = ProtocolAnalysis(nameAnalysis, protocolAssignment)

        // Split the program.
        val splitProgram: ProgramNode = program.splitMain(protocolAnalysis, typeAnalysis)

        if (!intermediate) {
            BackendCompiler.compile(splitProgram, output)
        } else {
            output.print(splitProgram)
        }
    }

    companion object {
        /** Stop graphviz-java from using the deprecated Nashorn Javascript engine. */
        init {
            Graphviz.useEngine(GraphvizCmdLineEngine(), GraphvizV8Engine(), GraphvizServerEngine())
        }
    }
}

/**
 * Outputs the graph generated by [graphWriter] to [file] if [file] is not `null`. Does nothing
 * otherwise. The output format is determined automatically from [file]'s extension.
 */
private fun dumpGraph(graphWriter: (Writer) -> Unit, file: File?) {
    if (file == null) {
        return
    }

    when (val format = formatFromFileExtension(file)) {
        Format.DOT ->
            file.bufferedWriter().use(graphWriter)
        else -> {
            val writer = StringWriter()
            graphWriter(writer)
            Graphviz.fromString(writer.toString()).render(format).toFile(file)
        }
    }
}

/** Infers Graphviz output format from [file]'s extension. */
private fun formatFromFileExtension(file: File): Format =
    when (file.extension.toLowerCase()) {
        "png" ->
            Format.PNG
        "svg" ->
            Format.SVG
        "dot" ->
            Format.DOT
        "xdot" ->
            Format.XDOT
        "txt" ->
            Format.PLAIN
        "ps" ->
            Format.PS2
        "json" ->
            Format.JSON0
        else ->
            throw UnknownGraphvizExtension(file)
    }

private class UnknownGraphvizExtension(file: File) :
    Error("Unknown Graphviz extension '${file.extension}' in $file.")
