package edu.ustb.sei.mde.mumodel;

import com.google.common.base.Objects;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

@SuppressWarnings("all")
public class FeatureTuple {
  public final EObject host;
  
  public final EStructuralFeature feature;
  
  public final Object value;
  
  public FeatureTuple(final EObject host, final EStructuralFeature feature, final Object value) {
    this.host = host;
    this.feature = feature;
    this.value = value;
  }
  
  public int hashCode() {
    int _hashCode = this.host.hashCode();
    int _multiply = (_hashCode * 31);
    int _hashCode_1 = this.feature.hashCode();
    return (_multiply + _hashCode_1);
  }
  
  public boolean equals(final Object obj) {
    boolean _xifexpression = false;
    if (((obj == null) || (!(obj instanceof FeatureTuple)))) {
      return false;
    } else {
      boolean _xblockexpression = false;
      {
        final FeatureTuple right = ((FeatureTuple) obj);
        boolean _xifexpression_1 = false;
        if (((this.host != right.host) || (this.feature != right.feature))) {
          return false;
        } else {
          boolean _xifexpression_2 = false;
          boolean _isMany = this.feature.isMany();
          if (_isMany) {
            return Objects.equal(this.value, right.value);
          } else {
            _xifexpression_2 = true;
          }
          _xifexpression_1 = _xifexpression_2;
        }
        _xblockexpression = _xifexpression_1;
      }
      _xifexpression = _xblockexpression;
    }
    return _xifexpression;
  }
  
  public String toString() {
    String _plus = (this.host + "\t");
    String _name = this.feature.getName();
    String _plus_1 = (_plus + _name);
    String _plus_2 = (_plus_1 + "\t");
    String _plus_3 = (_plus_2 + this.value);
    return (_plus_3 + "\n");
  }
}
