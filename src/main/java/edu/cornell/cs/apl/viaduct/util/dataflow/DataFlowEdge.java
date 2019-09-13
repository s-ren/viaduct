package edu.cornell.cs.apl.viaduct.util.dataflow;

/** Edges in a data flow graph. These are allowed to modify values as they pass through them. */
public interface DataFlowEdge<A> {
  /**
   * Apply this function to the value incoming from the source node before passing it onto the
   * destination node.
   */
  A propagate(A in);
}
