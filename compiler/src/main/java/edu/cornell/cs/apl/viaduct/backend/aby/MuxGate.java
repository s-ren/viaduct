/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.12
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package edu.cornell.cs.apl.viaduct.backend.aby;

public class MuxGate extends CircuitGate {
  private transient long swigCPtr;

  protected MuxGate(long cPtr, boolean cMemoryOwn) {
    super(ViaductABYJNI.MuxGate_SWIGUpcast(cPtr), cMemoryOwn);
    swigCPtr = cPtr;
  }

  protected static long getCPtr(MuxGate obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        ViaductABYJNI.delete_MuxGate(swigCPtr);
      }
      swigCPtr = 0;
    }
    super.delete();
  }

  public MuxGate(CircuitGate guard, CircuitGate lhs, CircuitGate rhs) {
    this(
        ViaductABYJNI.new_MuxGate(
            CircuitGate.getCPtr(guard),
            guard,
            CircuitGate.getCPtr(lhs),
            lhs,
            CircuitGate.getCPtr(rhs),
            rhs),
        true);
  }

  public void AddChildrenToTraversal(SWIGTYPE_p_std__vectorT_CircuitGate_p_t children) {
    ViaductABYJNI.MuxGate_AddChildrenToTraversal(
        swigCPtr, this, SWIGTYPE_p_std__vectorT_CircuitGate_p_t.getCPtr(children));
  }

  public SWIGTYPE_p_share BuildGate(
      SWIGTYPE_p_std__stackT_share_p_t shareStack, CircuitBuilders builders) {
    long cPtr =
        ViaductABYJNI.MuxGate_BuildGate(
            swigCPtr,
            this,
            SWIGTYPE_p_std__stackT_share_p_t.getCPtr(shareStack),
            CircuitBuilders.getCPtr(builders),
            builders);
    return (cPtr == 0) ? null : new SWIGTYPE_p_share(cPtr, false);
  }
}