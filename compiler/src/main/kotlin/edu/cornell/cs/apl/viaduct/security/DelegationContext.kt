package edu.cornell.cs.apl.viaduct.security

import edu.cornell.cs.apl.viaduct.syntax.intermediate.DelegationDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

class DelegationContext(
    private val program: ProgramNode,
    private val parametermap: Map<String, Label>
) {

    var delegations: Set<Pair<Label, Label>> =
        program.delegationDeclarations.fold(mutableSetOf())
        { list: MutableSet<Pair<Label, Label>>, delegation: DelegationDeclarationNode ->
            val label1 = delegation.node1.value.interpret(parametermap)
            val label2 = delegation.node2.value.interpret(parametermap)
            list.add(Pair(label1, label2))
            list
        }

    val joinOfActors: Label =
        delegations.fold(Label.weakest) { acc, actor ->
            acc.join(actor.first)
        }

    val meetOfDelegators: Label =
        delegations.fold(Label.strongest) { acc, actor ->
            acc.meet(actor.second)
        }

    fun Label.equals(that: Label) =
        (this.join(joinOfActors) == that.join(joinOfActors)) &&
            (this.meet(meetOfDelegators) == that.meet(meetOfDelegators))


    fun actsFor(first: Label, second: Label): Boolean =
        equals(first.meet(second), second)


    fun flowsTo(first: Label, second: Label): Boolean =
        actsFor(
            (second.confidentiality() and first.integrity()),
            (first.confidentiality() and second.integrity())
        )

    fun lessThanOrEqualTo(first: Label, second: Label): Boolean =
        flowsTo(first, second)

    fun imply(first: Label, second: Label) {

    }
}
