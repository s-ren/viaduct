package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.concatenated
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.viaduct.parsing.parse
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import kotlinx.collections.immutable.toPersistentList

/**
 * Represents a surface level program which is a sequence of top level declarations.
 *
 * Instances are usually created by [parse].
 */
class ProgramNode(
    declarations: List<TopLevelDeclarationNode>,
    override val sourceLocation: SourceLocation
) : Node(), List<TopLevelDeclarationNode> by declarations {
    // Make an immutable copy
    val declarations: List<TopLevelDeclarationNode> = declarations.toPersistentList()

    override val asDocument: Document
        get() =
            declarations.concatenated(Document.forcedLineBreak + Document.forcedLineBreak)
                .plus(Document.forcedLineBreak)

    override fun toString(): String =
        "Program (" + sourceLocation.sourcePath + ")"
}
