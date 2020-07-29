package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.attributes.circularAttribute
import edu.cornell.cs.apl.viaduct.errors.IllegalInternalCommunicationError
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InternalCommunicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.toPersistentSet

/** Associates each [StatementNode] with the [Protocol]s involved in its execution. */
class ProtocolAnalysis(
    private val nameAnalysis: NameAnalysis,
    val protocolAssignment: (Variable) -> Protocol
) {
    /** The [ProcessDeclarationNode] this [Node] is in. */
    private val Node.process: ProcessDeclarationNode by attribute {
        when (val parent = nameAnalysis.tree.parent(this)!!) {
            is ProcessDeclarationNode ->
                parent
            else ->
                parent.process
        }
    }

    /**
     * Returns the protocol that coordinates the execution of [statement].
     *
     * @throws IllegalInternalCommunicationError if [statement] is an [InternalCommunicationNode].
     */
    fun primaryProtocol(statement: SimpleStatementNode): Protocol =
        when (statement) {
            is LetNode -> {
                val protocol = protocolAssignment(statement.temporary.value)
                when (statement.value) {
                    is InputNode ->
                        assert(protocol == Local(statement.value.host.value))
                    is ReceiveNode ->
                        throw IllegalInternalCommunicationError(statement.process, statement.value)
                }
                protocol
            }
            is DeclarationNode ->
                protocolAssignment(statement.variable.value)
            is UpdateNode ->
                protocolAssignment(statement.variable.value)
            is OutputNode ->
                Local(statement.host.value)
            is SendNode ->
                throw IllegalInternalCommunicationError(statement.process, statement)
        }

    /**
     * The [primaryProtocol]s of [SimpleStatementNode]s that read the temporary defined by this
     * statement.
     *
     * We define this as a separate attribute so the (potentially expensive) computation is cached.
     */
    private val LetNode.directReaders: PersistentSet<Protocol> by attribute {
        nameAnalysis.readers(this)
            .filterIsInstance<SimpleStatementNode>()
            .map(::primaryProtocol)
            .toPersistentSet()
    }

    /** Used to compute [protocols]. */
    private val StatementNode.protocols: PersistentSet<Protocol> by circularAttribute(
        persistentHashSetOf()
    ) {
        when (this) {
            is LetNode -> {
                val indirectReaders =
                    nameAnalysis.readers(this)
                        .filter { it !is SimpleStatementNode }
                        .map { it.protocols }.unions()
                directReaders.addAll(indirectReaders).add(primaryProtocol(this))
            }

            is SimpleStatementNode ->
                persistentHashSetOf(primaryProtocol(this))

            is IfNode ->
                thenBranch.protocols.addAll(elseBranch.protocols)
            is InfiniteLoopNode ->
                body.protocols
            is BreakNode ->
                // Every protocol executing the loop executes the breaks in the loop.
                nameAnalysis.correspondingLoop(this).protocols
            is AssertionNode ->
                // All protocols execute every assertion.
                this.process.body.protocols

            is BlockNode ->
                statements.map { it.protocols }.unions()
        }
    }

    /** Returns the set of protocols that execute [statement]. */
    fun protocols(statement: StatementNode): Set<Protocol> = statement.protocols
}

/** Returns the union of all sets in this collection. */
private fun <E> Iterable<PersistentSet<E>>.unions(): PersistentSet<E> =
    this.fold(persistentHashSetOf(), PersistentSet<E>::addAll)