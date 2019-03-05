package edu.cornell.cs.apl.viaduct.surface;

import edu.cornell.cs.apl.viaduct.StmtVisitor;

/** Variable assignment statement. */
public class AssignNode implements StmtNode {
  private final Variable variable;
  private final ExpressionNode rhs;

  public AssignNode(Variable var, ExpressionNode rhs) {
    this.variable = var;
    this.rhs = rhs;
  }

  public Variable getVariable() {
    return variable;
  }

  public ExpressionNode getRhs() {
    return rhs;
  }

  public <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(assign " + this.getVariable().toString() + " to " + this.getRhs().toString() + ")";
  }
}
