package edu.cornell.cs.apl.viaduct.surface;

import edu.cornell.cs.apl.viaduct.ExprVisitor;

/** Boolean negation. */
public class NotNode implements ExpressionNode {
  private final ExpressionNode expression;

  public NotNode(ExpressionNode expression) {
    this.expression = expression;
  }

  public ExpressionNode getExpression() {
    return expression;
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(! " + this.getExpression().toString() + ")";
  }
}
