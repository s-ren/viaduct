package edu.cornell.cs.apl.viaduct.imp.parsing;

/** Classes that have a source location. */
public interface Located {
  SourceRange getSourceLocation();
}