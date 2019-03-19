package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** Generic interface for expression visitors. */
public interface ExpressionNode extends AstNode {
  <R> R accept(ExprVisitor<R> v);
}