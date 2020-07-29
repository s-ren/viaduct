package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.viaduct.ErroneousExampleFileProvider
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.parsing.SourceFile
import edu.cornell.cs.apl.viaduct.parsing.isBlankOrUnderline
import edu.cornell.cs.apl.viaduct.parsing.parse
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.passes.splitMain
import edu.cornell.cs.apl.viaduct.protocols.MainProtocol
import edu.cornell.cs.apl.viaduct.selection.SimpleSelection
import edu.cornell.cs.apl.viaduct.selection.SimpleSelector
import edu.cornell.cs.apl.viaduct.selection.simpleProtocolCost
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class ErrorsTest {
    @ParameterizedTest
    @ArgumentsSource(ErroneousExampleFileProvider::class)
    fun `erroneous example files throw the expected compilation error`(file: File) {
        assertThrows<CompilationError> { run(file) }
        try {
            run(file)
        } catch (e: CompilationError) {
            assertEquals(expectedError(file), e::class)
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ErroneousExampleFileProvider::class)
    fun `error messages end in a single blank line`(file: File) {
        try {
            run(file)
            assert(false)
        } catch (e: CompilationError) {
            val messageLines = e.toString().split(Regex("\\R"))
            assertTrue(isBlankOrUnderline(messageLines.last())) {
                "Error message should end in a blank line."
            }
            assertFalse(isBlankOrUnderline(messageLines[messageLines.size - 2])) {
                "Error message should have no more than one blank line at the end."
            }
        }
    }
}

/** Parses, checks, interprets, and splits a program. */
private fun run(file: File) {
    val program = SourceFile.from(file).parse().elaborated()
    program.check()
    program.split()
    // TODO: interpret
}

/** Selects protocols for and splits the [MainProtocol] in [this] program. */
private fun ProgramNode.split() {
    val nameAnalysis = NameAnalysis(Tree(this))
    val typeAnalysis = TypeAnalysis(nameAnalysis)
    val informationFlowAnalysis = InformationFlowAnalysis(nameAnalysis)

    val protocolAssignment =
        SimpleSelection(SimpleSelector(nameAnalysis, informationFlowAnalysis), ::simpleProtocolCost)
            .select(nameAnalysis.tree.root.main, nameAnalysis, informationFlowAnalysis)
    val protocolAnalysis = ProtocolAnalysis(nameAnalysis, protocolAssignment)

    this.splitMain(protocolAnalysis, typeAnalysis)
}

/** Returns the subclass of [CompilationError] that running [file] is supposed to throw. */
private fun expectedError(file: File): KClass<CompilationError> {
    val comment = file.useLines { it.first() }
    val expectedErrorName = comment.removeSurrounding("/*", "*/").trim()
    val packageName = CompilationError::class.java.packageName
    val kClass = Class.forName("$packageName.$expectedErrorName").kotlin

    assert(kClass.isSubclassOf(CompilationError::class))
    assert(!kClass.isAbstract)

    @Suppress("UNCHECKED_CAST")
    return kClass as KClass<CompilationError>
}