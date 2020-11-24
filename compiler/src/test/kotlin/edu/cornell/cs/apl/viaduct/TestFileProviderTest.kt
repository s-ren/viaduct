package edu.cornell.cs.apl.viaduct

import java.io.File
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class TestFileProviderTest {
    @ParameterizedTest
    @ArgumentsSource(PositiveTestFileProvider::class)
    fun `positive tests`(file: File) {
        println(file)
    }

    @ParameterizedTest
    @ArgumentsSource(NegativeTestFileProvider::class)
    fun `negative tests`(file: File) {
        println(file)
    }
}
