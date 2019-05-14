package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;

/** Reduce the confidentiality and/or integrity of the result of an expression. */
public class DowngradeNode extends ExpressionNode {
  private final ExpressionNode expression;
  private final Label label;

  public DowngradeNode(ExpressionNode expression, Label label) {
    this.expression = expression;
    this.label = label;
  }

  public ExpressionNode getExpression() {
    return expression;
  }

  public Label getLabel() {
    return label;
  }

  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public String toString() {
    return "(downgrade "
        + this.getExpression().toString()
        + " to "
        + this.getLabel().toString()
        + ")";
  }
}
