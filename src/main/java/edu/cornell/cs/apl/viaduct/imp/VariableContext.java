package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.util.SymbolTable;
import java.util.NoSuchElementException;

/** The variable context. Maintains a stack scopes that map from variables some data. */
public class VariableContext<V> extends SymbolTable<Variable, V> {
  @Override
  public V get(Variable v) {
    try {
      return super.get(v);
    } catch (NoSuchElementException e) {
      throw new UndeclaredVariableException(v);
    }
  }

  @Override
  public void put(Variable v, V value) {
    if (this.contains(v)) {
      throw new RedeclaredVariableException(v);
    }
    super.put(v, value);
  }
}