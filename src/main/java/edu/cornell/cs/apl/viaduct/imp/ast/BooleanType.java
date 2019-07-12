package edu.cornell.cs.apl.viaduct.imp.ast;

public final class BooleanType implements ImpType {
  private static final BooleanType INSTANCE = new BooleanType();

  private BooleanType() {}

  public static BooleanType create() {
    return INSTANCE;
  }

  @Override
  public String toString() {
    return "bool";
  }
}