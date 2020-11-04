/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package edu.cornell.cs.apl.viaduct.libsnarkwrapper;

public class Term {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected Term(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(Term obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  @SuppressWarnings("deprecation")
  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        libsnarkwrapperJNI.delete_Term(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setCoeff(long value) {
    libsnarkwrapperJNI.Term_coeff_set(swigCPtr, this, value);
  }

  public long getCoeff() {
    return libsnarkwrapperJNI.Term_coeff_get(swigCPtr, this);
  }

  public void setWireID(int value) {
    libsnarkwrapperJNI.Term_wireID_set(swigCPtr, this, value);
  }

  public int getWireID() {
    return libsnarkwrapperJNI.Term_wireID_get(swigCPtr, this);
  }

  public Term() {
    this(libsnarkwrapperJNI.new_Term(), true);
  }
}
