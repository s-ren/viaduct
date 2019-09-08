package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ImpAstVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** A statement node that supports visitors. */
public abstract class StatementNode extends ImpAstNode {
  private String id;

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public abstract <R> R accept(StmtVisitor<R> v);

  @Override
  public final <R> R accept(ImpAstVisitor<R> visitor) {
    return visitor.visit(this);
  }
}