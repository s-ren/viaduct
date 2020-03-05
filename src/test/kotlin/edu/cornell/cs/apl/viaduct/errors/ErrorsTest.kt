package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.viaduct.ErroneousExampleFileProvider
import edu.cornell.cs.apl.viaduct.parsing.SourceFile
import edu.cornell.cs.apl.viaduct.parsing.parse
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
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
            // Messages should terminate with a newline character.
            assertEquals("", messageLines.last())
            // They should have a blank line before that.
            assertTrue(isBlank(messageLines[messageLines.size - 2]))
            // They should have no more than one black line.
            assertFalse(isBlank(messageLines[messageLines.size - 3]))
        }
    }
}

/** Parse, check, and interpret a program. */
private fun run(file: File) {
    val program = SourceFile.from(file).parse().elaborated()
    program.check()
    // TODO: interpret
}

/**
 * Returns true if [line] contains only space and carrot (^) characters.
 * Carrots are considered blank since they are used to underline portions of the previous line.
 */
private fun isBlank(line: String): Boolean =
    line.all { it == ' ' || it == '^' }

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