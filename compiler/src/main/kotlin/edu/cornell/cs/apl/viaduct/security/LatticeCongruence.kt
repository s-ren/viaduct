package edu.cornell.cs.apl.viaduct.security

import edu.cornell.cs.apl.viaduct.algebra.FreeDistributiveLattice
import edu.cornell.cs.apl.viaduct.algebra.Lattice

interface LatticeCongruence<A : Lattice<A>> {
    val congruence: Set<Pair<A, A>>
    val joinOfActors: A
    val meetOfDelegators: A
}

class FreedistributedLatticeCongruence<A>(
    override val congruence: Set<Pair<FreeDistributiveLattice<A>, FreeDistributiveLattice<A>>>
) : LatticeCongruence<FreeDistributiveLattice<A>> {
    constructor() : this(emptySet())

    /*constructor(program: ProgramNode, parametermap: Map<String, Label>) :
        this(
            program.delegationDeclarations.fold(mutableSetOf())
            { list: MutableSet<Pair<Label, Label>>, delegation: DelegationDeclarationNode ->
                val label1 = delegation.node1.value.interpret(parametermap)
                val label2 = delegation.node2.value.interpret(parametermap)
                list.add(Pair(label1, label2))
                list
            }
        )*/
    override val joinOfActors: FreeDistributiveLattice<A> =
        if (congruence.isEmpty()) FreeDistributiveLattice.bottom()
        else congruence.fold(congruence.first().first)
        { acc, actor ->
            acc.join(actor.first)
        }

    override val meetOfDelegators: FreeDistributiveLattice<A> =
        if (congruence.isEmpty()) FreeDistributiveLattice.top()
        else congruence.fold(congruence.first().first)
        { acc, actor ->
            acc.meet(actor.first)
        }
    
}
