package edu.ustb.sei.mde.mumodel

import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Map
import java.util.Set
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EDataType
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EReference
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl
import org.eclipse.emf.ecore.EcorePackage
import java.util.concurrent.atomic.AtomicInteger

class ModelMutator {
	public var elementCreationRate = 0.05;
	public var minCreatedElement = 0;
	public var elementDeletionRate = 0.05;
	public var minDeletedElement = 0;
	public var elementMoveRate = 0.02;
	public var minMovedElement = 0;
	public var sFeatureSetRate = 0.1;
	public var minFeatureSet = 0;
	public var mFeatureAddRate = 0.05;
	public var minFeatureAddition = 0;
	public var mFeatureDeletionRate = 0.02;
	public var minFeatureDeletion = 0;
	public var mFeatureReorderRate = 0.02;
	public var minFeatureReordering = 0;
	
	
	
	val random = new RandomUtils();
	
	val Map<EClass, Set<EObject>> typeIndex = new HashMap;
	val Set<EObject> allObjects = new HashSet
	val Set<FeatureTuple> allExistingSFeatures = new HashSet
	val Set<FeatureTuple> allExistingMFeatures = new HashSet
	
	
	val Set<EObject> objectsToBeDeleted = new HashSet
	val Set<EObject> objectsToBeCreated = new HashSet
	val Set<EObject> objectsToBeMoved = new HashSet; // objectsToBeMoved is disjoint with objectsToBeDeleted
	
	val Set<FeatureTuple> featuresToBeSet = new HashSet;
	val Set<FeatureTuple> featuresToBeAdded = new HashSet;
	val Set<FeatureTuple> featuresToBeDeleted = new HashSet;
	val Set<FeatureTuple> featuresToBeReordered = new HashSet;
	
	val Set<EPackage> allPackages = new HashSet;
	val Set<EClass> allEClasses = new HashSet
	val Map<EClass, Set<EClass>> subclassMap = new HashMap
	val Map<EClass, List<EStructuralFeature>> featureMap = new HashMap
	val Map<EClass, List<EReference>> containingFeatureMap = new HashMap
	
	protected def void addType(EObject obj) {
		val list = typeIndex.computeIfAbsent(obj.eClass, [t| new HashSet()])
		list += obj
	}
	
	protected def void removeIndex(EObject obj) {
		val list = typeIndex.getOrDefault(obj.eClass, emptySet)
		list.remove(obj)
		obj.eContents.forEach[c| removeIndex(c)]
	}
	
	val Set<EClass> ignoredClasses = new HashSet
	val Set<EStructuralFeature> ignoredFeatures = new HashSet
	
	def void setIgnoredClasses(EClass... classes) {
		ignoredClasses.addAll(classes)
	}
	
	def void setIgnoredFeatures(EStructuralFeature... features) {
		ignoredFeatures.addAll(features)
	}
	
	private def boolean ignored(EClass cls) {
		if(ignoredClasses.contains(cls)) true
		else if(cls.ESuperTypes.isEmpty===false) cls.ESuperTypes.exists[it.ignored]
		else false
	}
	private def boolean ignored(EStructuralFeature ref) {
		ignoredFeatures.contains(ref) || ref.EContainingClass.ignored || (ref.EType instanceof EClass && (ref.EType as EClass).ignored)
	}
	
	protected def void init(List<EObject> roots) {
		copyModel(roots)
		
		allPackages.forEach[p|
			p.EClassifiers.filter[it instanceof EClass].map[it as EClass].filter[it.abstract===false && !it.ignored].forEach[
				allEClasses+=it
				addToParentClass(it)
				featureMap.put(it, it.EAllStructuralFeatures.filter[!(
					it.derived || it.transient || it.volatile || it.changeable===false
					|| (
						it instanceof EReference && ((it as EReference).isContainer || (it as EReference).containment  
						|| ((it as EReference).EOpposite !== null && (it as EReference).EOpposite.name.compareTo(it.name) < 0))
					) || it.ignored
				)].toList);
			]
		]
		
		allEClasses.forEach[cls|
			val refs = cls.EAllReferences.filter[it.containment && !it.ignored]
			refs.forEach[ref|
				val target = ref.EReferenceType
				val subclasses = subclassMap.get(target)
				subclasses.filter[it.abstract===false].forEach[tar|
					val contain = containingFeatureMap.computeIfAbsent(tar, [new ArrayList]);
					contain += ref
				]
			]
		]
		
		allObjects.forEach[obj|
			val features = featureMap.get(obj.eClass);
			for(feature : features) {
				if(feature.isMany) {
					val values = obj.eGet(feature) as List<?>
					values.forEach[value|
						allExistingMFeatures += new FeatureTuple(obj, feature, value)
					]
				} else {
					val value = obj.eGet(feature)
					allExistingSFeatures += new FeatureTuple(obj, feature, value)
				}
			}
		]
	}
	
	protected def void init(Resource resource) {
		init(resource.contents)
	}
	
	protected def void init(EObject root) {
		init(Collections.singletonList(root))
	}
	
	protected def void copyModel(List<EObject> everything) {
		EcoreUtil.copyAll(everything).forEach[
			cache(it)
			it.eAllContents.forEach[				
				cache(it)
			]
		]
	}
	
	protected def void cache(EObject it) {
		if(!it.eClass.ignored) {			
			allObjects += it
			allPackages += it.eClass.EPackage
			addType(it)
		}
	}
	
	private def void addToParentClass(EClass subclass) {
		val sp = subclassMap.computeIfAbsent(subclass, [new HashSet])
		sp += subclass
		val par = subclass.EAllSuperTypes
		par.forEach[p|
			val subs = subclassMap.computeIfAbsent(p, [new HashSet])
			subs += subclass
		]
	}
	
	private def Set<EObject> computeSubtree(Map<EObject, Set<EObject>> map, EObject object) {
		val set = new HashSet<EObject>
		for(child : object.eContents) {
			set += computeSubtree(map, child)
		}
		map.put(object, set)
		return set
	}
	
	protected def void determineObjectsToBeDeleted() {
		val num = Math.max(minDeletedElement, Math.round(elementDeletionRate*allObjects.size) as int)
		val cand = random.shuffle(allObjects)
		val subtreeMap = new HashMap<EObject, Set<EObject>>
		
		for(c : cand) {
			if(objectsToBeDeleted.contains(c)==false) {
				val subtree = computeSubtree(subtreeMap, c)
				if(objectsToBeDeleted.size + subtree.size <= num * 1.1) 
					objectsToBeDeleted += subtree
			}
			if(objectsToBeDeleted.size >= num) return;
		}
	}
	
	protected def void determineObjectsToBeMoved() {
		val num = Math.max(minMovedElement, Math.round(elementMoveRate*allObjects.size) as int)
		val cand = random.shuffle(allObjects)
		for(c : cand) {
			if(objectsToBeDeleted.contains(c)==false) {
				objectsToBeMoved += c
			}
			if(objectsToBeMoved.size >= num) return;
		}
	}
	
	private def EClass selectTypeOfNewObject() {
		val pos = random.nextInt(allObjects.size)
		var total = 0;
		for(p : typeIndex.entrySet) {
			total += p.value.size
			if(pos <= total) return p.key
		}
		return null
	}
	
	protected def void determineObjectsToBeCreated() {
		val num = Math.max(minCreatedElement, Math.round(elementCreationRate*allObjects.size) as int)
		
		for(var i = 0; i < num; i++) {
			val type = selectTypeOfNewObject;
			val newElm = EcoreUtil.create(type)
			initElement(newElm)
			objectsToBeCreated += newElm
			addType(newElm)
		}
	}
	
	def void initElement(EObject object) {
		val attrs = featureMap.get(object.eClass).filter[it instanceof EAttribute].map[it as EAttribute]
		attrs.forEach[attr|
			if(attr.many) {
				object.eSet(attr, random.randomValueList(attr.EAttributeType, attr.lowerBound, attr.upperBound));
			} else {
				object.eSet(attr, random.randomValue(attr.EAttributeType));
			}
		]
		
		val feats = featureMap.get(object.eClass).filter[it instanceof EReference && !(it as EReference).containment && it.lowerBound > 0].map[it as EReference]
		feats.forEach[feat|
			val low = feat.lowerBound
			for(var i=0;i<low;i++) {
				var tar = selectObjectOfType(feat.EReferenceType)
				if(tar!==null && objectsToBeDeleted.contains(tar)!==true) {
					if(feat.many) {
						val list = object.eGet(feat) as List<EObject>
						list += tar
					} else {
						object.eSet(feat, tar)
					}
				}
			}
		]
	}
	
	
	var List<EObject> objectsAfterElementMutationList = null;
	
	protected def boolean isContainer(EObject parent, EObject child) {
		var obj = child;
		while(obj!==null) {
			if(obj===parent) return true
			obj = obj.eContainer
		}
		return false
	}
	
	protected def Pair<EObject, EReference> selectContainer(EObject object) {
		val cls = object.eClass
		val containerFeatures = containingFeatureMap.get(cls);
		if(containerFeatures===null) return null
		
		var retry = 0;
		while(retry<10) {
			val ref = random.selectOne(containerFeatures)
			val containerClass = ref.EContainingClass
			val candContainerClasses = subclassMap.get(containerClass)
			val candContainerClass = random.selectOne(candContainerClasses);
			if(candContainerClass!==null) {
				val candContainers = typeIndex.get(candContainerClass)
				if(candContainers!==null) {
					val container = random.selectOne(candContainers.filter[!object.isContainer(it)])
					if(container!==null) {
						if(ref.many) return container->ref
						else if(container.eGet(ref)===null) return container->ref
					}
				}
			}
			retry ++
		}
		
		return null
	}
	
	protected def getObjectsAfterElementMutation() {
		if(objectsAfterElementMutationList===null) {
			val Set<EObject> all = new HashSet
			all.addAll(allObjects);
			all.removeAll(objectsToBeDeleted)
			all.addAll(objectsToBeCreated)
			objectsAfterElementMutationList = all.toList
		}
		
		return objectsAfterElementMutationList
	}
	
	private def EObject selectObjectOfType(EClass clazz) {
		val subclasses = subclassMap.getOrDefault(clazz, Collections.emptySet)
		val cands = subclasses.map[typeIndex.getOrDefault(it, Collections.emptySet)]
		val totalSize = cands.map[it.size].fold(0,[l,r|l+r]);
		
		var poss = random.nextInt(totalSize)
		var acc = 0;
		
		for(candSet : cands) {
			acc += candSet.size
			if(poss < acc) {
				random.selectOne(candSet)
			}
		}
		
		null
	}
	
	protected def void determineFeaturesToBeSet() {
		// candidate elements: created + allObjects - deleted
		val num = Math.max(minFeatureSet, Math.round(sFeatureSetRate * allExistingSFeatures.size) as int)
		val cand = objectsAfterElementMutation
		
		var oldSize = 0;
		var retry = 0;
		
		while(featuresToBeSet.size < num) {
			oldSize = featuresToBeSet.size;
			
			val src = random.selectOne(cand)
			val features = featureMap.get(src.eClass).filter[it.many===false && (!(it instanceof EReference) || (it as EReference).containment===false)]
			val feature = random.selectOne(features);
			if(feature!==null) {
				val value = if(feature instanceof EReference) {
					selectObjectOfType(feature.EReferenceType)
				} else {
					if(feature.EType.instanceClass===String) {
						val old = src.eGet(feature) as String
						random.randomEdit(old)
					} else
						random.randomValue(feature.EType as EDataType)
				}
				if(value!==null) {					
					val ft = new FeatureTuple(src, feature, value);
					featuresToBeSet += ft
				}
			}
			
			if(featuresToBeSet.size === oldSize) retry++
			else retry = 0;
			
			if(retry > 5) return;
		} 
	}
	
	protected def void determineFeaturesToBeAdded() {
		// candidate elements: created + allObjects - deleted
		val num = Math.max(minFeatureAddition, Math.round(mFeatureAddRate * allExistingMFeatures.size) as int)
		val cand = objectsAfterElementMutation
		
		var oldSize = 0;
		var retry = 0;
		
		while(featuresToBeAdded.size < num) {
			oldSize = featuresToBeAdded.size;
			
			val src = random.selectOne(cand)
			val features = featureMap.get(src.eClass).filter[it.many && (!(it instanceof EReference) || (it as EReference).containment===false)]
			val feature = random.selectOne(features);
			if(feature!==null) {
				val value = if(feature instanceof EReference) {
					selectObjectOfType(feature.EReferenceType)
				} else {
					random.randomValue(feature.EType as EDataType)
				}
				
				if(value!==null) {					
					val ft = new FeatureTuple(src, feature, value);
					featuresToBeAdded += ft
				}
			}
			
			if(featuresToBeAdded.size === oldSize) retry++
			else retry = 0;
			
			if(retry > 5) return;
		} 
	}
	
	protected def void determineFeaturesToBeDeleted() {
		// compute deleted features
		// compute extra features to be deleted
		var totalSize = 0;
		val allFeatures = (allExistingSFeatures.filter[!(it.feature.many===false && it.feature instanceof EAttribute)] + allExistingMFeatures)
		
		val remainingFeatures = new ArrayList<FeatureTuple>(allExistingSFeatures.size + allExistingMFeatures.size)
		val deletedFeatures = new HashSet<FeatureTuple>
		
		for(it : allFeatures) {
			totalSize = totalSize + 1;
			if(objectsToBeDeleted.contains(it.host) || objectsToBeDeleted.contains(it.value)) {
				deletedFeatures += it
			} else remainingFeatures += it
		}
		
		val num = Math.max(minFeatureDeletion, Math.round(mFeatureDeletionRate * totalSize) as int)
		
		val extra = num - deletedFeatures.size
		
		featuresToBeDeleted += deletedFeatures
		
		if(extra>0) {
			val sel = random.select(remainingFeatures, extra)
			featuresToBeDeleted += sel
		}
	}
	
	protected def void determineFeaturesToBeReordered() {
		val num = Math.max(minFeatureReordering, Math.round(mFeatureReorderRate * allExistingMFeatures.size) as int)
		val list = random.shuffle(allExistingMFeatures.toList)
		
		for(ft : list) {
			if(featuresToBeDeleted.contains(ft)===false) {
				featuresToBeReordered += ft
				if(featuresToBeReordered.size >= num) return;
			}
		}
	}
	
	protected def void plan() {
		determineObjectsToBeDeleted
		determineObjectsToBeCreated
		determineObjectsToBeMoved
		
		determineFeaturesToBeSet
		determineFeaturesToBeAdded
		determineFeaturesToBeDeleted
		determineFeaturesToBeReordered
	}
	
	protected def int apply() {
		// delete elements
		val edits = new AtomicInteger(0)
		
		objectsToBeDeleted.forEach[o|
			removeIndex(o)
			EcoreUtil.delete(o)
			edits.incrementAndGet
		]
		
		// insert new elements
		(objectsToBeCreated+objectsToBeMoved).forEach[o|
			val pair = selectContainer(o)
			if(pair!==null) {
				if(pair.value.isMany) {
					val list = pair.key.eGet(pair.value) as List<Object>
					list.add(o)
				} else {
					pair.key.eSet(pair.value, o)
				}
				edits.incrementAndGet
			}
		]
		
		featuresToBeSet.forEach[tuple|
			tuple.host.eSet(tuple.feature, tuple.value)
			edits.incrementAndGet
		]
		
		// delete features
		featuresToBeDeleted.forEach[tuple|
			EcoreUtil.remove(tuple.host, tuple.feature, tuple.value)
			edits.incrementAndGet
		]
		
		// add features
		featuresToBeAdded.forEach[tuple|
			if(tuple.feature.many) {
				val list = tuple.host.eGet(tuple.feature) as List<Object>
				try{
					list += tuple.value
					edits.incrementAndGet
				} catch(Exception e) {
				}
			} else {
				tuple.host.eSet(tuple.feature, tuple.value)
				edits.incrementAndGet
			}
		]
		
		// move features
		featuresToBeReordered.forEach[tuple|
			val list = tuple.host.eGet(tuple.feature) as List<Object>
			val id = list.indexOf(tuple.value)
			if(id===-1) {
				return
			}
			val newID = random.nextInt(list.size)
			val other = list.get(newID)
			
			if(newID > id) {
				list.remove(newID)
				list.set(id, other)
				list.add(newID, tuple.value)
				edits.incrementAndGet
			} else if(id > newID) {
				list.remove(id)
				list.set(newID, tuple.value)
				list.add(id, other)
				edits.incrementAndGet
			}
		]
		
		edits.acquire
	}
	
	protected def void buildResource(Resource result) {
		getObjectsAfterElementMutation.forEach[o|
			if(o.eContainer===null) result.contents += o
		]
	}
	
	def mutateModel(EObject rootObject, Resource result) {
		init(rootObject)
		plan()
		apply()
		buildResource(result)
	}
	
	def mutateResource(Resource resource, Resource result) {
		init(resource)
		plan()
		val edits = apply()
		buildResource(result)
		return edits
	}
	
	static def void saveAs(EObject rootObject, Resource result) {
		val copied = EcoreUtil.copy(rootObject)
		result.contents.add(copied)
	}
	
	static def void saveAsIdBasedResource(Resource resource, XMIResourceImpl result) {
		val copied = EcoreUtil.copyAll(resource.contents)
		result.contents.addAll(copied)
		result.allContents.forEach[e|
			val id = result.getID(e)
			if(id===null) {
				result.setID(e, EcoreUtil.generateUUID)
			}
		]
	}
	
	static def void saveAsIdBasedResource(EObject rootObject, XMIResourceImpl result) {
		val copied = EcoreUtil.copy(rootObject)
		result.contents.add(copied)
		result.allContents.forEach[e|
			val id = result.getID(e)
			if(id===null) {
				result.setID(e, EcoreUtil.generateUUID)
			}
		]
	}
}

class FeatureTuple {
	public val EObject host;
	public val EStructuralFeature feature;
	public val Object value;
	
	new(EObject host, EStructuralFeature feature, Object value) {
		this.host = host;
		this.feature = feature;
		this.value = value;
	}
	
	override hashCode() {
		host.hashCode * 31 + feature.hashCode
	}
	
	override equals(Object obj) {
		if(obj === null || !(obj instanceof FeatureTuple)) return false
		else {
			val right = obj as FeatureTuple
			if(host!==right.host || feature!==right.feature) return false
			else {
				if(feature.many) return value==right.value
				else true
			}
		}
	}
	
	override toString() {
		host+"\t"+feature.name+"\t"+value+"\n"
	}
	
}