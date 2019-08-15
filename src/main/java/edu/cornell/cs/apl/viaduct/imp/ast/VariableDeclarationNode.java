package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;

import javax.annotation.Nullable;

/** Variable declaration. */
@AutoValue
public abstract class VariableDeclarationNode extends StmtNode {
  public static VariableDeclarationNode create(
      Variable variable, ImpType type, @Nullable Label label)
  {
    return new AutoValue_VariableDeclarationNode(variable, type, label);
  }

  public abstract Variable getVariable();

  public abstract ImpType getType();

  @Nullable
  public abstract Label getLabel();

  @Override
  public final <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }
}
