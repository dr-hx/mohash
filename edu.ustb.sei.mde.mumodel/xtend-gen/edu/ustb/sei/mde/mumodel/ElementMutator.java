package edu.ustb.sei.mde.mumodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class ElementMutator {
  private final EClass type;
  
  private double featureChangeRate = 0.3;
  
  private double featureValueChangeRate = 0.5;
  
  private int minChangedFeatures = 1;
  
  private int minChangedFeatureValues = 1;
  
  private double possOfElementRemoval = 0.4;
  
  private double possOfElementInsertion = 0.5;
  
  private double possOfElementReorder = 0.1;
  
  private double possOfValueSet = 0.9;
  
  private double possOfValueUnset = 0.1;
  
  private double possOfCharRemoval = 0.2;
  
  private double possOfCharAlter = 0.1;
  
  private double possOfCharInsert = 0.7;
  
  private final RandomUtils random = new RandomUtils();
  
  private final Map<EObject, EObject> mapping = new HashMap<EObject, EObject>();
  
  private final ArrayList<EObject> objectsOfType = new ArrayList<EObject>();
  
  private final List<EStructuralFeature> features;
  
  private final Set<EClass> focusedTypes;
  
  private final Map<EClass, List<EObject>> focusedObjects;
  
  public ElementMutator(final EClass type) {
    this.type = type;
    final Function1<EStructuralFeature, Boolean> _function = new Function1<EStructuralFeature, Boolean>() {
      public Boolean apply(final EStructuralFeature it) {
        return Boolean.valueOf(((((it.isDerived() || it.isTransient()) || it.isVolatile()) || (Boolean.valueOf(it.isChangeable()) == Boolean.valueOf(false))) || ((it instanceof EReference) && ((EReference) it).isContainer())));
      }
    };
    this.features = IterableExtensions.<EStructuralFeature>toList(IterableExtensions.<EStructuralFeature>filter(type.getEAllStructuralFeatures(), _function));
    final Function1<EStructuralFeature, Boolean> _function_1 = new Function1<EStructuralFeature, Boolean>() {
      public Boolean apply(final EStructuralFeature it) {
        return Boolean.valueOf((it instanceof EReference));
      }
    };
    final Function1<EStructuralFeature, EReference> _function_2 = new Function1<EStructuralFeature, EReference>() {
      public EReference apply(final EStructuralFeature it) {
        return ((EReference) it);
      }
    };
    final Function1<EReference, EClass> _function_3 = new Function1<EReference, EClass>() {
      public EClass apply(final EReference it) {
        return it.getEReferenceType();
      }
    };
    this.focusedTypes = IterableExtensions.<EClass>toSet(IterableExtensions.<EReference, EClass>map(IterableExtensions.<EStructuralFeature, EReference>map(IterableExtensions.<EStructuralFeature>filter(this.features, _function_1), _function_2), _function_3));
    HashMap<EClass, List<EObject>> _hashMap = new HashMap<EClass, List<EObject>>();
    this.focusedObjects = _hashMap;
  }
  
  protected void focusIfNeeded(final EObject o) {
    final Consumer<EClass> _function = new Consumer<EClass>() {
      public void accept(final EClass t) {
        boolean _isSuperTypeOf = t.isSuperTypeOf(o.eClass());
        if (_isSuperTypeOf) {
          final Function<EClass, List<EObject>> _function = new Function<EClass, List<EObject>>() {
            public List<EObject> apply(final EClass it) {
              return new ArrayList<EObject>();
            }
          };
          final List<EObject> list = ElementMutator.this.focusedObjects.computeIfAbsent(t, _function);
          list.add(o);
        }
      }
    };
    this.focusedTypes.forEach(_function);
  }
  
  protected void prepare(final List<EObject> contents) {
    Collection<EObject> _copyAll = EcoreUtil.<EObject>copyAll(contents);
    final List<EObject> copiedOriginal = ((List<EObject>) _copyAll);
    this.buildMapping(contents, copiedOriginal);
    this.init(copiedOriginal);
  }
  
  protected void buildMapping(final List<EObject> original, final List<EObject> copy) {
    throw new UnsupportedOperationException("TODO: auto-generated method stub");
  }
  
  protected void init(final List<EObject> contents) {
    for (final EObject r : contents) {
      {
        boolean _isSuperTypeOf = this.type.isSuperTypeOf(r.eClass());
        if (_isSuperTypeOf) {
          this.objectsOfType.add(r);
        }
        this.focusIfNeeded(r);
        final Consumer<EObject> _function = new Consumer<EObject>() {
          public void accept(final EObject c) {
            boolean _isSuperTypeOf = ElementMutator.this.type.isSuperTypeOf(c.eClass());
            if (_isSuperTypeOf) {
              ElementMutator.this.objectsOfType.add(c);
              ElementMutator.this.focusIfNeeded(c);
            }
          }
        };
        r.eAllContents().forEachRemaining(_function);
      }
    }
  }
  
  private final Map<EStructuralFeature, Object> objectState = new HashMap<EStructuralFeature, Object>();
  
  protected void push(final EObject object) {
    this.objectState.clear();
    for (final EStructuralFeature feature : this.features) {
      {
        final Object oldValue = object.eGet(feature);
        boolean _isMany = feature.isMany();
        if (_isMany) {
          final ArrayList<Object> copy = new ArrayList<Object>(((List<Object>) oldValue));
          this.objectState.put(feature, copy);
        } else {
          this.objectState.put(feature, oldValue);
        }
      }
    }
  }
  
  protected void pop(final EObject object) {
    for (final EStructuralFeature feature : this.features) {
      object.eSet(feature, this.objectState.get(feature));
    }
  }
  
  protected void randomEditList(final List<Object> list, final Supplier<?> randomValue) {
    int _size = list.size();
    double _multiply = (this.featureValueChangeRate * _size);
    double changes = Math.max(_multiply, this.minChangedFeatureValues);
    final List<Double> actionRates = Collections.<Double>unmodifiableList(CollectionLiterals.<Double>newArrayList(Double.valueOf(this.possOfElementRemoval), Double.valueOf(this.possOfElementInsertion), Double.valueOf(this.possOfElementReorder)));
    for (int i = 0; (i < list.size()); i++) {
      boolean _shouldHappen = this.random.shouldHappen(this.featureValueChangeRate);
      if (_shouldHappen) {
        final int action = this.random.select(((double[])Conversions.unwrapArray(actionRates, double.class)));
        switch (action) {
          case 0:
            list.remove(i);
            i--;
            break;
          case 1:
            final Object v = randomValue.get();
            if ((v != null)) {
              list.add(i, v);
              i++;
            }
            break;
          case 2:
            final int id = this.random.nextInt(list.size());
            Collections.swap(list, i, id);
            break;
        }
        changes--;
      }
    }
    while ((changes > 0)) {
      {
        final Object value = randomValue.get();
        if ((value != null)) {
          list.add(value);
        }
        changes--;
      }
    }
  }
  
  protected Object randomEdit(final Object oldValue, final Function<Object, Object> randomValue) {
    Object _xifexpression = null;
    if ((oldValue == null)) {
      _xifexpression = randomValue.apply(null);
    } else {
      Object _xblockexpression = null;
      {
        final List<Double> actionRates = Collections.<Double>unmodifiableList(CollectionLiterals.<Double>newArrayList(Double.valueOf(this.possOfValueSet), Double.valueOf(this.possOfValueUnset)));
        final int action = this.random.select(((double[])Conversions.unwrapArray(actionRates, double.class)));
        Object _xifexpression_1 = null;
        if ((action == this.possOfValueSet)) {
          _xifexpression_1 = randomValue.apply(oldValue);
        } else {
          _xifexpression_1 = null;
        }
        _xblockexpression = _xifexpression_1;
      }
      _xifexpression = _xblockexpression;
    }
    return _xifexpression;
  }
  
  public boolean randomEdit(final Boolean b) {
    boolean _xifexpression = false;
    if ((b == null)) {
      _xifexpression = this.random.nextBoolean();
    } else {
      _xifexpression = (!(b).booleanValue());
    }
    return _xifexpression;
  }
  
  public int randomEdit(final Integer c) {
    int _xifexpression = (int) 0;
    if ((c == null)) {
      _xifexpression = this.random.nextInt(100);
    } else {
      int _xblockexpression = (int) 0;
      {
        long _round = Math.round((this.featureValueChangeRate * (c).intValue()));
        final int range = Math.max(this.minChangedFeatureValues, ((int) _round));
        final int offset = this.random.nextInt((-range), range);
        _xblockexpression = ((c).intValue() + offset);
      }
      _xifexpression = _xblockexpression;
    }
    return _xifexpression;
  }
  
  public double randomEdit(final Double c) {
    double _xifexpression = (double) 0;
    if ((c == null)) {
      _xifexpression = this.random.nextDouble(100);
    } else {
      double _xblockexpression = (double) 0;
      {
        final double range = Math.max(this.minChangedFeatureValues, (this.featureValueChangeRate * (c).doubleValue()));
        final double offset = this.random.nextDouble((-range), range);
        _xblockexpression = ((c).doubleValue() + offset);
      }
      _xifexpression = _xblockexpression;
    }
    return _xifexpression;
  }
  
  public String randomEdit(final String string) {
    if ((string == null)) {
      Object _randomValue = this.random.randomValue(EcorePackage.eINSTANCE.getEString());
      return ((String) _randomValue);
    }
    final List<Double> actionPoss = Collections.<Double>unmodifiableList(CollectionLiterals.<Double>newArrayList(Double.valueOf(this.possOfCharRemoval), Double.valueOf(this.possOfCharAlter), Double.valueOf(this.possOfCharInsert)));
    int _length = string.length();
    double _multiply = (this.featureValueChangeRate * _length);
    long _round = Math.round(_multiply);
    int numberOfChanges = Math.max(this.minChangedFeatureValues, ((int) _round));
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; (i < string.length()); i++) {
      boolean _shouldHappen = this.random.shouldHappen(this.featureValueChangeRate);
      if (_shouldHappen) {
        final int action = this.random.select(((double[])Conversions.unwrapArray(actionPoss, double.class)));
        switch (action) {
          case 0:
            break;
          case 1:
            builder.append(this.random.nextChar());
            break;
          case 2:
            builder.append(this.random.nextChar());
            i--;
            break;
        }
        numberOfChanges--;
      } else {
        builder.append(string.charAt(i));
      }
    }
    while ((numberOfChanges > 0)) {
      {
        builder.append(this.random.nextChar());
        numberOfChanges--;
      }
    }
    return builder.toString();
  }
  
  public Object randomValue(final EDataType type, final Object oldValue) {
    Object _xblockexpression = null;
    {
      Class<?> instanceClass = type.getInstanceClass();
      Object _xifexpression = null;
      if (((instanceClass == int.class) || (instanceClass == Integer.class))) {
        _xifexpression = Integer.valueOf(this.randomEdit(((Integer) oldValue)));
      } else {
        Object _xifexpression_1 = null;
        if (((instanceClass == boolean.class) || (instanceClass == Boolean.class))) {
          _xifexpression_1 = Boolean.valueOf(this.randomEdit(((Boolean) oldValue)));
        } else {
          Object _xifexpression_2 = null;
          if ((instanceClass == String.class)) {
            _xifexpression_2 = this.randomEdit(((String) oldValue));
          } else {
            Double _xifexpression_3 = null;
            if (((instanceClass == double.class) || (instanceClass == double.class))) {
              _xifexpression_3 = Double.valueOf(this.randomEdit(((Double) oldValue)));
            } else {
              _xifexpression_3 = null;
            }
            _xifexpression_2 = _xifexpression_3;
          }
          _xifexpression_1 = _xifexpression_2;
        }
        _xifexpression = ((Object)_xifexpression_1);
      }
      _xblockexpression = ((Object)_xifexpression);
    }
    return _xblockexpression;
  }
  
  protected void mutate(final EObject object) {
    int _size = this.features.size();
    double _multiply = (this.featureChangeRate * _size);
    long _max = Math.max(this.minChangedFeatures, Math.round(_multiply));
    final int numOfChangedFeatures = ((int) _max);
    final Set<EStructuralFeature> featuresToBeChanged = this.random.<EStructuralFeature>select(this.features, numOfChangedFeatures);
    for (final EStructuralFeature feature : featuresToBeChanged) {
      {
        final Object oldValue = object.eGet(feature);
        if ((feature instanceof EReference)) {
          final List<EObject> focusedObjects = this.focusedObjects.getOrDefault(((EReference)feature).getEReferenceType(), Collections.<EObject>emptyList());
          boolean _isMany = ((EReference)feature).isMany();
          if (_isMany) {
            final Supplier<Object> _function = new Supplier<Object>() {
              public Object get() {
                return ElementMutator.this.random.<EObject>selectOne(focusedObjects);
              }
            };
            this.randomEditList(((List<Object>) oldValue), _function);
          } else {
            final Function<Object, Object> _function_1 = new Function<Object, Object>() {
              public Object apply(final Object it) {
                return ElementMutator.this.random.<EObject>selectOne(focusedObjects);
              }
            };
            object.eSet(feature, this.randomEdit(oldValue, _function_1));
          }
        } else {
          final EDataType eAttributeType = ((EAttribute) feature).getEAttributeType();
          boolean _isMany_1 = feature.isMany();
          if (_isMany_1) {
            final Supplier<Object> _function_2 = new Supplier<Object>() {
              public Object get() {
                return ElementMutator.this.random.randomValue(eAttributeType);
              }
            };
            this.randomEditList(((List<Object>) oldValue), _function_2);
          } else {
            final Function<Object, Object> _function_3 = new Function<Object, Object>() {
              public Object apply(final Object it) {
                return ElementMutator.this.randomValue(eAttributeType, it);
              }
            };
            object.eSet(feature, this.randomEdit(oldValue, _function_3));
          }
        }
      }
    }
  }
}
