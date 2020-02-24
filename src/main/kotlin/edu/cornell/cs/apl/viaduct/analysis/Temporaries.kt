package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.visitors.StatementReducer
import edu.cornell.cs.apl.viaduct.syntax.intermediate.visitors.traverse

/** Returns the set of temporaries read by this expression. */
fun ExpressionNode.reads(): Set<Temporary> =
    this.traverse(Reads())

/** Returns the set of temporaries read by this statement. */
fun StatementNode.reads(): Set<Temporary> =
    this.traverse(Reads())

/**
 * Returns a map from [Temporary] variables declared in [process] to the sets of [StatementNode]s
 * that (potentially) read those temporaries. Note that these sets do not contain [BlockNode]s.
 */
fun readers(process: ProcessDeclarationNode): Map<Temporary, Set<StatementNode>> {
    // Mapping from statements to the temporaries they read
    val reads = Reads().let {
        process.traverse(it)
        it.reads
    }

    // Invert [reads] to get the result
    val entries: List<Pair<Temporary, StatementNode>> =
        reads.entries.flatMap { entry -> entry.value.map { tmp -> Pair(tmp, entry.key) } }
    val grouped = entries.groupBy({ it.first }, { it.second })
    return grouped.mapValues { it.value.toSet() }
}

/**
 * Returns a map from [Temporary] variables declared in [process] to the statements that define
 * them.
 */
fun definitionSites(process: ProcessDeclarationNode): Map<Temporary, SimpleStatementNode> {
    val definitionSites = mutableMapOf<Temporary, SimpleStatementNode>()

    process.traverse(
        object : EffectfulStatementVisitor {
            override fun leave(node: LetNode, value: Unit) {
                definitionSites[node.temporary.value] = node
            }

            override fun leave(node: InputNode) {
                definitionSites[node.temporary.value] = node
            }

            override fun leave(node: ReceiveNode) {
                definitionSites[node.temporary.value] = node
            }
        }
    )

    return definitionSites
}

/**
 * Computes a map from [StatementNode]s to the sets of temporaries they read.
 * A temporary is read if it appears in a [ReadNode].
 *
 * Running [traverse] on a [StatementNode] will populate the [reads] map with the children
 * of that statement. Note that the statement itself will not be included. In addition,
 * [BlockNode]s are never added to [reads].
 */
private class Reads : StatementReducer<SetWithUnion<Temporary>> {
    val reads = mutableMapOf<StatementNode, Set<Temporary>>()

    override val initial: SetWithUnion<Temporary>
        get() = SetWithUnion.top()

    override val combine: (SetWithUnion<Temporary>, SetWithUnion<Temporary>) -> SetWithUnion<Temporary>
        get() = SetWithUnion<Temporary>::meet

    override fun leave(node: ReadNode): SetWithUnion<Temporary> =
        SetWithUnion(node.temporary.value)

    override fun leave(
        node: BlockNode,
        statements: List<SetWithUnion<Temporary>>
    ): SetWithUnion<Temporary> {
        node.statements.forEachIndexed { i, statement ->
            if (statement !is BlockNode) {
                reads[statement] = statements[i]
            }
        }
        return super.leave(node, statements)
    }
}