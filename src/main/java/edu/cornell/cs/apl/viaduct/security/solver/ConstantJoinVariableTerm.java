package edu.cornell.cs.apl.viaduct.security.solver;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.util.CoHeytingAlgebra;
import edu.cornell.cs.apl.viaduct.util.dataflow.DataFlowEdge;

/** Join of a constant element and a variable. */
@AutoValue
public abstract class ConstantJoinVariableTerm<A extends CoHeytingAlgebra<A>>
    implements RightHandTerm<A> {
  public ConstantJoinVariableTerm<A> create(A lhs, ConstraintSystem<A>.VariableTerm rhs) {
    return new AutoValue_ConstantJoinVariableTerm<>(lhs, rhs);
  }

  protected abstract A getLhs();

  protected abstract ConstraintSystem<A>.VariableTerm getRhs();

  @Override
  public ConstraintValue<A> getNode() {
    return getRhs();
  }

  @Override
  public DataFlowEdge<A> getInEdge() {
    return new SubtractionEdge<>(getLhs());
  }
}
