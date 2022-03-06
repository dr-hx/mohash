package edu.ustb.sei.mde.mumodel;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.xtext.xbase.lib.CollectionExtensions;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.Functions.Function2;
import org.eclipse.xtext.xbase.lib.InputOutput;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.IteratorExtensions;
import org.eclipse.xtext.xbase.lib.Pair;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

@SuppressWarnings("all")
public class ModelMutator {
  public double elementCreationRate = 0.05;
  
  public int minCreatedElement = 0;
  
  public double elementDeletionRate = 0.05;
  
  public int minDeletedElement = 0;
  
  public double elementMoveRate = 0.02;
  
  public int minMovedElement = 0;
  
  public double sFeatureSetRate = 0.1;
  
  public int minFeatureSet = 0;
  
  public double mFeatureAddRate = 0.05;
  
  public int minFeatureAddition = 0;
  
  public double mFeatureDeletionRate = 0.02;
  
  public int minFeatureDeletion = 0;
  
  public double mFeatureReorderRate = 0.02;
  
  public int minFeatureReordering = 0;
  
  private final RandomUtils random = new RandomUtils();
  
  private final Map<EClass, Set<EObject>> typeIndex = new HashMap<EClass, Set<EObject>>();
  
  private final Set<EObject> allObjects = new HashSet<EObject>();
  
  private final Set<FeatureTuple> allExistingSFeatures = new HashSet<FeatureTuple>();
  
  private final Set<FeatureTuple> allExistingMFeatures = new HashSet<FeatureTuple>();
  
  private final Set<EObject> objectsToBeDeleted = new HashSet<EObject>();
  
  private final Set<EObject> objectsToBeCreated = new HashSet<EObject>();
  
  private final Set<EObject> objectsToBeMoved = new HashSet<EObject>();
  
  private final Set<FeatureTuple> featuresToBeSet = new HashSet<FeatureTuple>();
  
  private final Set<FeatureTuple> featuresToBeAdded = new HashSet<FeatureTuple>();
  
  private final Set<FeatureTuple> featuresToBeDeleted = new HashSet<FeatureTuple>();
  
  private final Set<FeatureTuple> featuresToBeReordered = new HashSet<FeatureTuple>();
  
  private final Set<EPackage> allPackages = new HashSet<EPackage>();
  
  private final Set<EClass> allEClasses = new HashSet<EClass>();
  
  private final Map<EClass, Set<EClass>> subclassMap = new HashMap<EClass, Set<EClass>>();
  
  private final Map<EClass, List<EStructuralFeature>> featureMap = new HashMap<EClass, List<EStructuralFeature>>();
  
  private final Map<EClass, List<EReference>> containingFeatureMap = new HashMap<EClass, List<EReference>>();
  
  protected void addType(final EObject obj) {
    final Function<EClass, Set<EObject>> _function = new Function<EClass, Set<EObject>>() {
      public Set<EObject> apply(final EClass t) {
        return new HashSet<EObject>();
      }
    };
    final Set<EObject> list = this.typeIndex.computeIfAbsent(obj.eClass(), _function);
    list.add(obj);
  }
  
  protected void removeIndex(final EObject obj) {
    final Set<EObject> list = this.typeIndex.getOrDefault(obj.eClass(), CollectionLiterals.<EObject>emptySet());
    list.remove(obj);
    final Consumer<EObject> _function = new Consumer<EObject>() {
      public void accept(final EObject c) {
        ModelMutator.this.removeIndex(c);
      }
    };
    obj.eContents().forEach(_function);
  }
  
  private final Set<EClass> ignoredClasses = new HashSet<EClass>();
  
  private final Set<EStructuralFeature> ignoredFeatures = new HashSet<EStructuralFeature>();
  
  public void setIgnoredClasses(final EClass... classes) {
    CollectionExtensions.<EClass>addAll(this.ignoredClasses, classes);
  }
  
  public void setIgnoredFeatures(final EStructuralFeature... features) {
    CollectionExtensions.<EStructuralFeature>addAll(this.ignoredFeatures, features);
  }
  
  private boolean ignored(final EClass cls) {
    boolean _xifexpression = false;
    boolean _contains = this.ignoredClasses.contains(cls);
    if (_contains) {
      _xifexpression = true;
    } else {
      boolean _xifexpression_1 = false;
      boolean _isEmpty = cls.getESuperTypes().isEmpty();
      boolean _tripleEquals = (Boolean.valueOf(_isEmpty) == Boolean.valueOf(false));
      if (_tripleEquals) {
        final Function1<EClass, Boolean> _function = new Function1<EClass, Boolean>() {
          public Boolean apply(final EClass it) {
            return Boolean.valueOf(ModelMutator.this.ignored(it));
          }
        };
        _xifexpression_1 = IterableExtensions.<EClass>exists(cls.getESuperTypes(), _function);
      } else {
        _xifexpression_1 = false;
      }
      _xifexpression = _xifexpression_1;
    }
    return _xifexpression;
  }
  
  private boolean ignored(final EStructuralFeature ref) {
    return ((this.ignoredFeatures.contains(ref) || this.ignored(ref.getEContainingClass())) || ((ref.getEType() instanceof EClass) && this.ignored(((EClass) ref.getEType()))));
  }
  
  protected void init(final List<EObject> roots) {
    this.copyModel(roots);
    final Consumer<EPackage> _function = new Consumer<EPackage>() {
      public void accept(final EPackage p) {
        final Function1<EClassifier, Boolean> _function = new Function1<EClassifier, Boolean>() {
          public Boolean apply(final EClassifier it) {
            return Boolean.valueOf((it instanceof EClass));
          }
        };
        final Function1<EClassifier, EClass> _function_1 = new Function1<EClassifier, EClass>() {
          public EClass apply(final EClassifier it) {
            return ((EClass) it);
          }
        };
        final Function1<EClass, Boolean> _function_2 = new Function1<EClass, Boolean>() {
          public Boolean apply(final EClass it) {
            return Boolean.valueOf(((Boolean.valueOf(it.isAbstract()) == Boolean.valueOf(false)) && (!ModelMutator.this.ignored(it))));
          }
        };
        final Consumer<EClass> _function_3 = new Consumer<EClass>() {
          public void accept(final EClass it) {
            ModelMutator.this.allEClasses.add(it);
            ModelMutator.this.addToParentClass(it);
            final Function1<EStructuralFeature, Boolean> _function = new Function1<EStructuralFeature, Boolean>() {
              public Boolean apply(final EStructuralFeature it) {
                return Boolean.valueOf((!(((((it.isDerived() || it.isTransient()) || it.isVolatile()) || (Boolean.valueOf(it.isChangeable()) == Boolean.valueOf(false))) || 
                  ((it instanceof EReference) && ((((EReference) it).isContainer() || ((EReference) it).isContainment()) || ((((EReference) it).getEOpposite() != null) && (((EReference) it).getEOpposite().getName().compareTo(it.getName()) < 0))))) || ModelMutator.this.ignored(it))));
              }
            };
            ModelMutator.this.featureMap.put(it, IterableExtensions.<EStructuralFeature>toList(IterableExtensions.<EStructuralFeature>filter(it.getEAllStructuralFeatures(), _function)));
          }
        };
        IterableExtensions.<EClass>filter(IterableExtensions.<EClassifier, EClass>map(IterableExtensions.<EClassifier>filter(p.getEClassifiers(), _function), _function_1), _function_2).forEach(_function_3);
      }
    };
    this.allPackages.forEach(_function);
    final Consumer<EClass> _function_1 = new Consumer<EClass>() {
      public void accept(final EClass cls) {
        final Function1<EReference, Boolean> _function = new Function1<EReference, Boolean>() {
          public Boolean apply(final EReference it) {
            return Boolean.valueOf((it.isContainment() && (!ModelMutator.this.ignored(it))));
          }
        };
        final Iterable<EReference> refs = IterableExtensions.<EReference>filter(cls.getEAllReferences(), _function);
        final Consumer<EReference> _function_1 = new Consumer<EReference>() {
          public void accept(final EReference ref) {
            final EClass target = ref.getEReferenceType();
            final Set<EClass> subclasses = ModelMutator.this.subclassMap.get(target);
            final Function1<EClass, Boolean> _function = new Function1<EClass, Boolean>() {
              public Boolean apply(final EClass it) {
                boolean _isAbstract = it.isAbstract();
                return Boolean.valueOf((Boolean.valueOf(_isAbstract) == Boolean.valueOf(false)));
              }
            };
            final Consumer<EClass> _function_1 = new Consumer<EClass>() {
              public void accept(final EClass tar) {
                final Function<EClass, List<EReference>> _function = new Function<EClass, List<EReference>>() {
                  public List<EReference> apply(final EClass it) {
                    return new ArrayList<EReference>();
                  }
                };
                final List<EReference> contain = ModelMutator.this.containingFeatureMap.computeIfAbsent(tar, _function);
                contain.add(ref);
              }
            };
            IterableExtensions.<EClass>filter(subclasses, _function).forEach(_function_1);
          }
        };
        refs.forEach(_function_1);
      }
    };
    this.allEClasses.forEach(_function_1);
    final Consumer<EObject> _function_2 = new Consumer<EObject>() {
      public void accept(final EObject obj) {
        final List<EStructuralFeature> features = ModelMutator.this.featureMap.get(obj.eClass());
        for (final EStructuralFeature feature : features) {
          boolean _isMany = feature.isMany();
          if (_isMany) {
            Object _eGet = obj.eGet(feature);
            final List<?> values = ((List<?>) _eGet);
            final Consumer<Object> _function = new Consumer<Object>() {
              public void accept(final Object value) {
                FeatureTuple _featureTuple = new FeatureTuple(obj, feature, value);
                ModelMutator.this.allExistingMFeatures.add(_featureTuple);
              }
            };
            values.forEach(_function);
          } else {
            final Object value = obj.eGet(feature);
            FeatureTuple _featureTuple = new FeatureTuple(obj, feature, value);
            ModelMutator.this.allExistingSFeatures.add(_featureTuple);
          }
        }
      }
    };
    this.allObjects.forEach(_function_2);
  }
  
  protected void init(final Resource resource) {
    this.init(resource.getContents());
  }
  
  protected void init(final EObject root) {
    this.init(Collections.<EObject>singletonList(root));
  }
  
  protected void copyModel(final List<EObject> everything) {
    final Consumer<EObject> _function = new Consumer<EObject>() {
      public void accept(final EObject it) {
        ModelMutator.this.cache(it);
        final Procedure1<EObject> _function = new Procedure1<EObject>() {
          public void apply(final EObject it) {
            ModelMutator.this.cache(it);
          }
        };
        IteratorExtensions.<EObject>forEach(it.eAllContents(), _function);
      }
    };
    EcoreUtil.<EObject>copyAll(everything).forEach(_function);
  }
  
  protected void cache(final EObject it) {
    boolean _ignored = this.ignored(it.eClass());
    boolean _not = (!_ignored);
    if (_not) {
      this.allObjects.add(it);
      EPackage _ePackage = it.eClass().getEPackage();
      this.allPackages.add(_ePackage);
      this.addType(it);
    }
  }
  
  private void addToParentClass(final EClass subclass) {
    final Function<EClass, Set<EClass>> _function = new Function<EClass, Set<EClass>>() {
      public Set<EClass> apply(final EClass it) {
        return new HashSet<EClass>();
      }
    };
    final Set<EClass> sp = this.subclassMap.computeIfAbsent(subclass, _function);
    sp.add(subclass);
    final EList<EClass> par = subclass.getEAllSuperTypes();
    final Consumer<EClass> _function_1 = new Consumer<EClass>() {
      public void accept(final EClass p) {
        final Function<EClass, Set<EClass>> _function = new Function<EClass, Set<EClass>>() {
          public Set<EClass> apply(final EClass it) {
            return new HashSet<EClass>();
          }
        };
        final Set<EClass> subs = ModelMutator.this.subclassMap.computeIfAbsent(p, _function);
        subs.add(subclass);
      }
    };
    par.forEach(_function_1);
  }
  
  private Set<EObject> computeSubtree(final Map<EObject, Set<EObject>> map, final EObject object) {
    final HashSet<EObject> set = new HashSet<EObject>();
    EList<EObject> _eContents = object.eContents();
    for (final EObject child : _eContents) {
      Set<EObject> _computeSubtree = this.computeSubtree(map, child);
      Iterables.<EObject>addAll(set, _computeSubtree);
    }
    map.put(object, set);
    return set;
  }
  
  protected void determineObjectsToBeDeleted() {
    int _size = this.allObjects.size();
    double _multiply = (this.elementDeletionRate * _size);
    long _round = Math.round(_multiply);
    final int num = Math.max(this.minDeletedElement, ((int) _round));
    final List<EObject> cand = this.random.<EObject>shuffle(this.allObjects);
    final HashMap<EObject, Set<EObject>> subtreeMap = new HashMap<EObject, Set<EObject>>();
    for (final EObject c : cand) {
      {
        boolean _contains = this.objectsToBeDeleted.contains(c);
        boolean _equals = (_contains == false);
        if (_equals) {
          final Set<EObject> subtree = this.computeSubtree(subtreeMap, c);
          int _size_1 = this.objectsToBeDeleted.size();
          int _size_2 = subtree.size();
          int _plus = (_size_1 + _size_2);
          boolean _lessEqualsThan = (_plus <= (num * 1.1));
          if (_lessEqualsThan) {
            Iterables.<EObject>addAll(this.objectsToBeDeleted, subtree);
          }
        }
        int _size_3 = this.objectsToBeDeleted.size();
        boolean _greaterEqualsThan = (_size_3 >= num);
        if (_greaterEqualsThan) {
          return;
        }
      }
    }
  }
  
  protected void determineObjectsToBeMoved() {
    int _size = this.allObjects.size();
    double _multiply = (this.elementMoveRate * _size);
    long _round = Math.round(_multiply);
    final int num = Math.max(this.minMovedElement, ((int) _round));
    final List<EObject> cand = this.random.<EObject>shuffle(this.allObjects);
    for (final EObject c : cand) {
      {
        boolean _contains = this.objectsToBeDeleted.contains(c);
        boolean _equals = (_contains == false);
        if (_equals) {
          this.objectsToBeMoved.add(c);
        }
        int _size_1 = this.objectsToBeMoved.size();
        boolean _greaterEqualsThan = (_size_1 >= num);
        if (_greaterEqualsThan) {
          return;
        }
      }
    }
  }
  
  private EClass selectTypeOfNewObject() {
    final int pos = this.random.nextInt(this.allObjects.size());
    int total = 0;
    Set<Map.Entry<EClass, Set<EObject>>> _entrySet = this.typeIndex.entrySet();
    for (final Map.Entry<EClass, Set<EObject>> p : _entrySet) {
      {
        int _tal = total;
        int _size = p.getValue().size();
        total = (_tal + _size);
        if ((pos <= total)) {
          return p.getKey();
        }
      }
    }
    return null;
  }
  
  protected void determineObjectsToBeCreated() {
    int _size = this.allObjects.size();
    double _multiply = (this.elementCreationRate * _size);
    long _round = Math.round(_multiply);
    final int num = Math.max(this.minCreatedElement, ((int) _round));
    for (int i = 0; (i < num); i++) {
      {
        final EClass type = this.selectTypeOfNewObject();
        final EObject newElm = EcoreUtil.create(type);
        this.initElement(newElm);
        this.objectsToBeCreated.add(newElm);
        this.addType(newElm);
      }
    }
  }
  
  public void initElement(final EObject object) {
    final Function1<EStructuralFeature, Boolean> _function = new Function1<EStructuralFeature, Boolean>() {
      public Boolean apply(final EStructuralFeature it) {
        return Boolean.valueOf((it instanceof EAttribute));
      }
    };
    final Function1<EStructuralFeature, EAttribute> _function_1 = new Function1<EStructuralFeature, EAttribute>() {
      public EAttribute apply(final EStructuralFeature it) {
        return ((EAttribute) it);
      }
    };
    final Iterable<EAttribute> attrs = IterableExtensions.<EStructuralFeature, EAttribute>map(IterableExtensions.<EStructuralFeature>filter(this.featureMap.get(object.eClass()), _function), _function_1);
    final Consumer<EAttribute> _function_2 = new Consumer<EAttribute>() {
      public void accept(final EAttribute attr) {
        boolean _isMany = attr.isMany();
        if (_isMany) {
          object.eSet(attr, ModelMutator.this.random.randomValueList(attr.getEAttributeType(), attr.getLowerBound(), attr.getUpperBound()));
        } else {
          object.eSet(attr, ModelMutator.this.random.randomValue(attr.getEAttributeType()));
        }
      }
    };
    attrs.forEach(_function_2);
    final Function1<EStructuralFeature, Boolean> _function_3 = new Function1<EStructuralFeature, Boolean>() {
      public Boolean apply(final EStructuralFeature it) {
        return Boolean.valueOf((((it instanceof EReference) && (!((EReference) it).isContainment())) && (it.getLowerBound() > 0)));
      }
    };
    final Function1<EStructuralFeature, EReference> _function_4 = new Function1<EStructuralFeature, EReference>() {
      public EReference apply(final EStructuralFeature it) {
        return ((EReference) it);
      }
    };
    final Iterable<EReference> feats = IterableExtensions.<EStructuralFeature, EReference>map(IterableExtensions.<EStructuralFeature>filter(this.featureMap.get(object.eClass()), _function_3), _function_4);
    final Consumer<EReference> _function_5 = new Consumer<EReference>() {
      public void accept(final EReference feat) {
        final int low = feat.getLowerBound();
        for (int i = 0; (i < low); i++) {
          {
            EObject tar = ModelMutator.this.selectObjectOfType(feat.getEReferenceType());
            if (((tar != null) && (Boolean.valueOf(ModelMutator.this.objectsToBeDeleted.contains(tar)) != Boolean.valueOf(true)))) {
              boolean _isMany = feat.isMany();
              if (_isMany) {
                Object _eGet = object.eGet(feat);
                final List<EObject> list = ((List<EObject>) _eGet);
                list.add(tar);
              } else {
                object.eSet(feat, tar);
              }
            }
          }
        }
      }
    };
    feats.forEach(_function_5);
  }
  
  private List<EObject> objectsAfterElementMutationList = null;
  
  protected boolean isContainer(final EObject parent, final EObject child) {
    EObject obj = child;
    while ((obj != null)) {
      {
        if ((obj == parent)) {
          return true;
        }
        obj = obj.eContainer();
      }
    }
    return false;
  }
  
  protected Pair<EObject, EReference> selectContainer(final EObject object) {
    final EClass cls = object.eClass();
    final List<EReference> containerFeatures = this.containingFeatureMap.get(cls);
    if ((containerFeatures == null)) {
      return null;
    }
    int retry = 0;
    while ((retry < 10)) {
      {
        final EReference ref = this.random.<EReference>selectOne(containerFeatures);
        final EClass containerClass = ref.getEContainingClass();
        final Set<EClass> candContainerClasses = this.subclassMap.get(containerClass);
        final EClass candContainerClass = this.random.<EClass>selectOne(candContainerClasses);
        if ((candContainerClass != null)) {
          final Set<EObject> candContainers = this.typeIndex.get(candContainerClass);
          if ((candContainers != null)) {
            final Function1<EObject, Boolean> _function = new Function1<EObject, Boolean>() {
              public Boolean apply(final EObject it) {
                boolean _isContainer = ModelMutator.this.isContainer(object, it);
                return Boolean.valueOf((!_isContainer));
              }
            };
            final EObject container = this.random.<EObject>selectOne(IterableExtensions.<EObject>filter(candContainers, _function));
            if ((container != null)) {
              boolean _isMany = ref.isMany();
              if (_isMany) {
                return Pair.<EObject, EReference>of(container, ref);
              } else {
                Object _eGet = container.eGet(ref);
                boolean _tripleEquals = (_eGet == null);
                if (_tripleEquals) {
                  return Pair.<EObject, EReference>of(container, ref);
                }
              }
            }
          }
        }
        retry++;
      }
    }
    return null;
  }
  
  protected List<EObject> getObjectsAfterElementMutation() {
    if ((this.objectsAfterElementMutationList == null)) {
      final Set<EObject> all = new HashSet<EObject>();
      all.addAll(this.allObjects);
      all.removeAll(this.objectsToBeDeleted);
      all.addAll(this.objectsToBeCreated);
      this.objectsAfterElementMutationList = IterableExtensions.<EObject>toList(all);
    }
    return this.objectsAfterElementMutationList;
  }
  
  private EObject selectObjectOfType(final EClass clazz) {
    Object _xblockexpression = null;
    {
      final Set<EClass> subclasses = this.subclassMap.getOrDefault(clazz, Collections.<EClass>emptySet());
      final Function1<EClass, Set<EObject>> _function = new Function1<EClass, Set<EObject>>() {
        public Set<EObject> apply(final EClass it) {
          return ModelMutator.this.typeIndex.getOrDefault(it, Collections.<EObject>emptySet());
        }
      };
      final Iterable<Set<EObject>> cands = IterableExtensions.<EClass, Set<EObject>>map(subclasses, _function);
      final Function1<Set<EObject>, Integer> _function_1 = new Function1<Set<EObject>, Integer>() {
        public Integer apply(final Set<EObject> it) {
          return Integer.valueOf(it.size());
        }
      };
      final Function2<Integer, Integer, Integer> _function_2 = new Function2<Integer, Integer, Integer>() {
        public Integer apply(final Integer l, final Integer r) {
          return Integer.valueOf(((l).intValue() + (r).intValue()));
        }
      };
      final Integer totalSize = IterableExtensions.<Integer, Integer>fold(IterableExtensions.<Set<EObject>, Integer>map(cands, _function_1), Integer.valueOf(0), _function_2);
      int poss = this.random.nextInt((totalSize).intValue());
      int acc = 0;
      for (final Set<EObject> candSet : cands) {
        {
          int _acc = acc;
          int _size = candSet.size();
          acc = (_acc + _size);
          if ((poss < acc)) {
            this.random.<EObject>selectOne(candSet);
          }
        }
      }
      _xblockexpression = null;
    }
    return ((EObject)_xblockexpression);
  }
  
  protected void determineFeaturesToBeSet() {
    int _size = this.allExistingSFeatures.size();
    double _multiply = (this.sFeatureSetRate * _size);
    long _round = Math.round(_multiply);
    final int num = Math.max(this.minFeatureSet, ((int) _round));
    final List<EObject> cand = this.getObjectsAfterElementMutation();
    int oldSize = 0;
    int retry = 0;
    while ((this.featuresToBeSet.size() < num)) {
      {
        oldSize = this.featuresToBeSet.size();
        final EObject src = this.random.<EObject>selectOne(cand);
        final Function1<EStructuralFeature, Boolean> _function = new Function1<EStructuralFeature, Boolean>() {
          public Boolean apply(final EStructuralFeature it) {
            return Boolean.valueOf(((Boolean.valueOf(it.isMany()) == Boolean.valueOf(false)) && ((!(it instanceof EReference)) || (Boolean.valueOf(((EReference) it).isContainment()) == Boolean.valueOf(false)))));
          }
        };
        final Iterable<EStructuralFeature> features = IterableExtensions.<EStructuralFeature>filter(this.featureMap.get(src.eClass()), _function);
        final EStructuralFeature feature = this.random.<EStructuralFeature>selectOne(features);
        if ((feature != null)) {
          Object _xifexpression = null;
          if ((feature instanceof EReference)) {
            _xifexpression = this.selectObjectOfType(((EReference)feature).getEReferenceType());
          } else {
            Object _xifexpression_1 = null;
            Class<?> _instanceClass = feature.getEType().getInstanceClass();
            boolean _tripleEquals = (_instanceClass == String.class);
            if (_tripleEquals) {
              String _xblockexpression = null;
              {
                Object _eGet = src.eGet(feature);
                final String old = ((String) _eGet);
                _xblockexpression = this.random.randomEdit(old);
              }
              _xifexpression_1 = _xblockexpression;
            } else {
              EClassifier _eType = feature.getEType();
              _xifexpression_1 = this.random.randomValue(((EDataType) _eType));
            }
            _xifexpression = _xifexpression_1;
          }
          final Object value = _xifexpression;
          if ((value != null)) {
            final FeatureTuple ft = new FeatureTuple(src, feature, value);
            this.featuresToBeSet.add(ft);
          }
        }
        int _size_1 = this.featuresToBeSet.size();
        boolean _tripleEquals_1 = (_size_1 == oldSize);
        if (_tripleEquals_1) {
          retry++;
        } else {
          retry = 0;
        }
        if ((retry > 5)) {
          return;
        }
      }
    }
  }
  
  protected void determineFeaturesToBeAdded() {
    int _size = this.allExistingMFeatures.size();
    double _multiply = (this.mFeatureAddRate * _size);
    long _round = Math.round(_multiply);
    final int num = Math.max(this.minFeatureAddition, ((int) _round));
    final List<EObject> cand = this.getObjectsAfterElementMutation();
    int oldSize = 0;
    int retry = 0;
    while ((this.featuresToBeAdded.size() < num)) {
      {
        oldSize = this.featuresToBeAdded.size();
        final EObject src = this.random.<EObject>selectOne(cand);
        final Function1<EStructuralFeature, Boolean> _function = new Function1<EStructuralFeature, Boolean>() {
          public Boolean apply(final EStructuralFeature it) {
            return Boolean.valueOf((it.isMany() && ((!(it instanceof EReference)) || (Boolean.valueOf(((EReference) it).isContainment()) == Boolean.valueOf(false)))));
          }
        };
        final Iterable<EStructuralFeature> features = IterableExtensions.<EStructuralFeature>filter(this.featureMap.get(src.eClass()), _function);
        final EStructuralFeature feature = this.random.<EStructuralFeature>selectOne(features);
        if ((feature != null)) {
          Object _xifexpression = null;
          if ((feature instanceof EReference)) {
            _xifexpression = this.selectObjectOfType(((EReference)feature).getEReferenceType());
          } else {
            EClassifier _eType = feature.getEType();
            _xifexpression = this.random.randomValue(((EDataType) _eType));
          }
          final Object value = _xifexpression;
          if ((value != null)) {
            final FeatureTuple ft = new FeatureTuple(src, feature, value);
            this.featuresToBeAdded.add(ft);
          }
        }
        int _size_1 = this.featuresToBeAdded.size();
        boolean _tripleEquals = (_size_1 == oldSize);
        if (_tripleEquals) {
          retry++;
        } else {
          retry = 0;
        }
        if ((retry > 5)) {
          return;
        }
      }
    }
  }
  
  protected void determineFeaturesToBeDeleted() {
    int totalSize = 0;
    final Function1<FeatureTuple, Boolean> _function = new Function1<FeatureTuple, Boolean>() {
      public Boolean apply(final FeatureTuple it) {
        return Boolean.valueOf((!((Boolean.valueOf(it.feature.isMany()) == Boolean.valueOf(false)) && (it.feature instanceof EAttribute))));
      }
    };
    Iterable<FeatureTuple> _filter = IterableExtensions.<FeatureTuple>filter(this.allExistingSFeatures, _function);
    final Iterable<FeatureTuple> allFeatures = Iterables.<FeatureTuple>concat(_filter, this.allExistingMFeatures);
    int _size = this.allExistingSFeatures.size();
    int _size_1 = this.allExistingMFeatures.size();
    int _plus = (_size + _size_1);
    final ArrayList<FeatureTuple> remainingFeatures = new ArrayList<FeatureTuple>(_plus);
    final HashSet<FeatureTuple> deletedFeatures = new HashSet<FeatureTuple>();
    for (final FeatureTuple it : allFeatures) {
      {
        totalSize = (totalSize + 1);
        if ((this.objectsToBeDeleted.contains(it.host) || this.objectsToBeDeleted.contains(it.value))) {
          deletedFeatures.add(it);
        } else {
          remainingFeatures.add(it);
        }
      }
    }
    long _round = Math.round((this.mFeatureDeletionRate * totalSize));
    final int num = Math.max(this.minFeatureDeletion, ((int) _round));
    int _size_2 = deletedFeatures.size();
    final int extra = (num - _size_2);
    Iterables.<FeatureTuple>addAll(this.featuresToBeDeleted, deletedFeatures);
    if ((extra > 0)) {
      final Set<FeatureTuple> sel = this.random.<FeatureTuple>select(remainingFeatures, extra);
      Iterables.<FeatureTuple>addAll(this.featuresToBeDeleted, sel);
    }
  }
  
  protected void determineFeaturesToBeReordered() {
    int _size = this.allExistingMFeatures.size();
    double _multiply = (this.mFeatureReorderRate * _size);
    long _round = Math.round(_multiply);
    final int num = Math.max(this.minFeatureReordering, ((int) _round));
    final List<FeatureTuple> list = this.random.<FeatureTuple>shuffle(IterableExtensions.<FeatureTuple>toList(this.allExistingMFeatures));
    for (final FeatureTuple ft : list) {
      boolean _contains = this.featuresToBeDeleted.contains(ft);
      boolean _tripleEquals = (Boolean.valueOf(_contains) == Boolean.valueOf(false));
      if (_tripleEquals) {
        this.featuresToBeReordered.add(ft);
        int _size_1 = this.featuresToBeReordered.size();
        boolean _greaterEqualsThan = (_size_1 >= num);
        if (_greaterEqualsThan) {
          return;
        }
      }
    }
  }
  
  protected void plan() {
    this.determineObjectsToBeDeleted();
    this.determineObjectsToBeCreated();
    this.determineObjectsToBeMoved();
    this.determineFeaturesToBeSet();
    this.determineFeaturesToBeAdded();
    this.determineFeaturesToBeDeleted();
    this.determineFeaturesToBeReordered();
  }
  
  protected void apply() {
    final Consumer<EObject> _function = new Consumer<EObject>() {
      public void accept(final EObject o) {
        ModelMutator.this.removeIndex(o);
        EcoreUtil.delete(o);
      }
    };
    this.objectsToBeDeleted.forEach(_function);
    final Consumer<EObject> _function_1 = new Consumer<EObject>() {
      public void accept(final EObject o) {
        final Pair<EObject, EReference> pair = ModelMutator.this.selectContainer(o);
        if ((pair != null)) {
          boolean _isMany = pair.getValue().isMany();
          if (_isMany) {
            Object _eGet = pair.getKey().eGet(pair.getValue());
            final List<Object> list = ((List<Object>) _eGet);
            list.add(o);
          } else {
            InputOutput.<Pair<EObject, EReference>>println(pair);
            pair.getKey().eSet(pair.getValue(), o);
          }
        }
      }
    };
    Iterables.<EObject>concat(this.objectsToBeCreated, this.objectsToBeMoved).forEach(_function_1);
    final Consumer<FeatureTuple> _function_2 = new Consumer<FeatureTuple>() {
      public void accept(final FeatureTuple tuple) {
        tuple.host.eSet(tuple.feature, tuple.value);
      }
    };
    this.featuresToBeSet.forEach(_function_2);
    final Consumer<FeatureTuple> _function_3 = new Consumer<FeatureTuple>() {
      public void accept(final FeatureTuple tuple) {
        EcoreUtil.remove(tuple.host, tuple.feature, tuple.value);
      }
    };
    this.featuresToBeDeleted.forEach(_function_3);
    final Consumer<FeatureTuple> _function_4 = new Consumer<FeatureTuple>() {
      public void accept(final FeatureTuple tuple) {
        boolean _isMany = tuple.feature.isMany();
        if (_isMany) {
          Object _eGet = tuple.host.eGet(tuple.feature);
          final List<Object> list = ((List<Object>) _eGet);
          try {
            list.add(tuple.value);
          } catch (final Throwable _t) {
            if (_t instanceof ArrayStoreException) {
              InputOutput.<EStructuralFeature>println(tuple.feature);
              InputOutput.<Object>println(tuple.value);
            } else {
              throw Exceptions.sneakyThrow(_t);
            }
          }
        } else {
          tuple.host.eSet(tuple.feature, tuple.value);
        }
      }
    };
    this.featuresToBeAdded.forEach(_function_4);
    final Consumer<FeatureTuple> _function_5 = new Consumer<FeatureTuple>() {
      public void accept(final FeatureTuple tuple) {
        Object _eGet = tuple.host.eGet(tuple.feature);
        final List<Object> list = ((List<Object>) _eGet);
        final int id = list.indexOf(tuple.value);
        if ((id == (-1))) {
          final EClass cls = ((EClass) tuple.host);
        }
        final int newID = ModelMutator.this.random.nextInt(list.size());
        Collections.swap(list, id, newID);
      }
    };
    this.featuresToBeReordered.forEach(_function_5);
  }
  
  protected void buildResource(final Resource result) {
    final Consumer<EObject> _function = new Consumer<EObject>() {
      public void accept(final EObject o) {
        EObject _eContainer = o.eContainer();
        boolean _tripleEquals = (_eContainer == null);
        if (_tripleEquals) {
          EList<EObject> _contents = result.getContents();
          _contents.add(o);
        }
      }
    };
    this.getObjectsAfterElementMutation().forEach(_function);
  }
  
  public void mutateModel(final EObject rootObject, final Resource result) {
    this.init(rootObject);
    this.plan();
    this.apply();
    this.buildResource(result);
  }
  
  public void mutateResource(final Resource resource, final Resource result) {
    this.init(resource);
    this.plan();
    this.apply();
    this.buildResource(result);
  }
  
  public static void saveAs(final EObject rootObject, final Resource result) {
    final EObject copied = EcoreUtil.<EObject>copy(rootObject);
    result.getContents().add(copied);
  }
  
  public static void saveAsIdBasedResource(final Resource resource, final XMIResourceImpl result) {
    final Collection<EObject> copied = EcoreUtil.<EObject>copyAll(resource.getContents());
    result.getContents().addAll(copied);
    final Procedure1<EObject> _function = new Procedure1<EObject>() {
      public void apply(final EObject e) {
        final String id = result.getID(e);
        if ((id == null)) {
          result.setID(e, EcoreUtil.generateUUID());
        }
      }
    };
    IteratorExtensions.<EObject>forEach(result.getAllContents(), _function);
  }
  
  public static void saveAsIdBasedResource(final EObject rootObject, final XMIResourceImpl result) {
    final EObject copied = EcoreUtil.<EObject>copy(rootObject);
    result.getContents().add(copied);
    final Procedure1<EObject> _function = new Procedure1<EObject>() {
      public void apply(final EObject e) {
        final String id = result.getID(e);
        if ((id == null)) {
          result.setID(e, EcoreUtil.generateUUID());
        }
      }
    };
    IteratorExtensions.<EObject>forEach(result.getAllContents(), _function);
  }
}
