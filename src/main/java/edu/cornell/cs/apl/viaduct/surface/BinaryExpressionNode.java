package edu.cornell.cs.apl.viaduct.surface;

/**
 * Superclass of binary operation expressions.
 *
 * <p>Specific operations (like addition or boolean AND) should inherit from this class.
 */
public abstract class BinaryExpressionNode implements ExpressionNode {
  private final ExpressionNode lhs;
  private final ExpressionNode rhs;

  public BinaryExpressionNode(ExpressionNode lhs, ExpressionNode rhs) {
    this.lhs = lhs;
    this.rhs = rhs;
  }

  public ExpressionNode getLhs() {
    return this.lhs;
  }

  public ExpressionNode getRhs() {
    return this.rhs;
  }
}
