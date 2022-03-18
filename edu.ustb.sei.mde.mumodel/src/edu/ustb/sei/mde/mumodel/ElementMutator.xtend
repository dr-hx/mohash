package edu.ustb.sei.mde.mumodel

import com.google.common.collect.Iterators
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Map
import java.util.Set
import java.util.function.Function
import java.util.function.Supplier
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EDataType
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EReference
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl

class ElementMutator {
	public val EClass type;
	public var featureChangeRate = 0.1
	public var featureValueChangeRate = 0.4
	
	public var minChangedFeatures = 1
	var minChangedFeatureValues = 1
	var possOfElementRemoval = 0.4
	var possOfElementInsertion = 0.5
	var possOfElementReorder = 0.1 
	var possOfValueSet = 0.9
	var possOfValueUnset = 0.1
	var possOfCharRemoval = 0.2
	var possOfCharAlter = 0.2
	var possOfCharInsert = 0.7 
	
	val random = new RandomUtils
	
	val Map<EObject, EObject> mapping  = new HashMap
	
	val objectsOfType = new ArrayList<EObject>
	val List<EStructuralFeature> features
	val Set<EClass> focusedTypes;
	val Map<EClass, List<EObject>> focusedObjects;
	var List<EObject> original;
	
	new(EClass type) {
		this.type = type
		this.features = type.EAllStructuralFeatures.filter[!(
			it.derived || it.transient || it.volatile || it.changeable===false
			|| (it instanceof EReference && ((it as EReference).isContainer))
		)].toList
		
		focusedTypes = features.filter[it instanceof EReference].map[it as EReference].map[it.EReferenceType].toSet
		focusedObjects = new HashMap
	}
	
	protected def void focusIfNeeded(EObject o) {
		focusedTypes.forEach[t|
			if(t.isSuperTypeOf(o.eClass)) {
				val list = focusedObjects.computeIfAbsent(t, [new ArrayList])
				list += o
			}
		]
	}
	
	def void prepare(List<EObject> contents) {
		original = EcoreUtil.copyAll(contents) as List<EObject>
		init(original)
	}
	
	protected def void buildMapping(List<EObject> original, List<EObject> copy) {
		if(original.size!==copy.size) throw new RuntimeException
		val oit = original.iterator
		val cit = copy.iterator
		while(oit.hasNext) {
			val o = oit.next
			val c = cit.next
			mapping.put(o, c)
			buildMapping(o.eContents, c.eContents)
		}
	}
	
	protected def void init(List<EObject> contents) {
		for(r : contents) {
			if(this.type.isSuperTypeOf(r.eClass)) {
				objectsOfType.add(r)
			}
			r.focusIfNeeded
			r.eAllContents.forEachRemaining[c|
				if(this.type.isSuperTypeOf(c.eClass)) {
					objectsOfType.add(c)
				}
				c.focusIfNeeded
			]
		}
	}
	
	def void push() {
		val copiedForMutation = EcoreUtil.copyAll(original) as List<EObject>
		mapping.clear
		buildMapping(original, copiedForMutation)
	}
	
	protected def void randomEditList(List<Object> list, Supplier<?> randomValue) {
		val size = list.size
		var changes = Math.max(Math.round(featureValueChangeRate * size) as int, minChangedFeatureValues)
		val actionRates = #[possOfElementRemoval, possOfElementInsertion, possOfElementReorder]
		for(var i = 0; i< list.size && changes > 0; i++) {
			if(random.shouldHappen(featureValueChangeRate)) {
				val action = random.select(actionRates);
				switch(action) {
					case 0: {
						list.remove(i)
						i--
					}
					case 1: {
						val v = randomValue.get
						try {					
							if(v!==null) {
								list.add(i, v)
								i++
							}
						} catch(Exception e) {}
					}
					case 2: {
						val id = random.nextInt(list.size)
						try {
						if(id!==i) {							
							if(id > i) {
								val tar = list.remove(id)
								val src = list.remove(i)
								list.add(i, tar)
								list.add(id, src)
							} else { // id < i
								val src = list.remove(i)
								val tar = list.remove(id)
								list.add(id, src)
								list.add(i, tar)
							}
						}
						} catch(Exception e){
//							e.printStackTrace
							changes ++;
						}
					}
				}
				changes--
			}
		}
		
		while(changes > 0) {
			val value = randomValue.get
			if(value!==null) list.add(value)
			changes --
		}
	}
	
	protected def randomEdit(Object oldValue, Function<Object, Object> randomValue) {
		if(oldValue===null) randomValue.apply(null)
		else {			
			val actionRates = #[possOfValueSet, possOfValueUnset]
			val action = random.select(actionRates);
			if(action === 0) {
				randomValue.apply(oldValue)
			} else {
				null
			}
		}
	}
	
	def randomEdit(Boolean b) {
		if(b===null) random.nextBoolean
		else !b
	}
	
	def randomEdit(Integer c) {
		if(c===null) random.nextInt(100)
		else {
			val range = Math.max(minChangedFeatureValues, Math.round(featureValueChangeRate * c) as int)
			val offset = random.nextInt(-range, range)
			c + offset
		}
	}
	
	def randomEdit(Double c) {
		if(c===null) random.nextDouble(100)
		else {
			val range = Math.max(minChangedFeatureValues, featureValueChangeRate * c)
			val offset = random.nextDouble(-range, range)
			c + offset
		}
	}
	
	def String randomEdit(String string) {
		if(string===null) return random.randomValue(EcorePackage.eINSTANCE.getEString()) as String
		val actionPoss = #[possOfCharRemoval, possOfCharAlter, possOfCharInsert]
		var numberOfChanges = Math.max(minChangedFeatureValues, Math.round(featureValueChangeRate * string.length()) as int)
		val builder = new StringBuilder();
		for(var i = 0;i<string.length() ;i++) {
			if(random.shouldHappen(featureValueChangeRate) && numberOfChanges>0) {
				val action = random.select(actionPoss);
				switch(action) {
				case 0: {} // skip
				case 1: { // alter
					builder.append(random.nextChar());
				}
				case 2: { // insert
					builder.append(random.nextChar());
					i--; // move back
				}
				}
				numberOfChanges--;
			} else {
				builder.append(string.charAt(i));
			}
		}
		while(numberOfChanges > 0) {
			builder.append(random.nextChar());
			numberOfChanges--;
		}
		return builder.toString();
	}
	
	def Object randomValue(EDataType type, Object oldValue) {
		var Class<?> instanceClass = type.getInstanceClass()
		if (instanceClass === int || instanceClass === Integer)
			randomEdit(oldValue as Integer)
		else if (instanceClass === boolean || instanceClass === Boolean)
			randomEdit(oldValue as Boolean)
		else if (instanceClass === String)
			randomEdit(oldValue as String)
		else if(instanceClass === double || instanceClass === double) randomEdit(oldValue as Double)
		else null
	}
	
	protected def void mutate(EObject originalObject) {
		val object = mapping.get(originalObject)
		
		val numOfChangedFeatures = Math.max(minChangedFeatures, Math.round(featureChangeRate * features.size)) as int
		val featuresToBeChanged = random.select(features, numOfChangedFeatures)
		for(feature : featuresToBeChanged) {
			val oldValue = object.eGet(feature)
			if(feature instanceof EReference) {
				val focusedObjects = focusedObjects.getOrDefault(feature.EReferenceType, Collections.emptyList)
				if(feature.many) {
					randomEditList(oldValue as List<Object>, [
						if(focusedObjects.isEmpty) return null;
						val oo = random.selectOne(focusedObjects)
						if(oo!==null && !(feature.containment && oo.isContainerOf(originalObject))) {
							mapping.get(oo) 
						}
						else null
					])
				} else {
					object.eSet(feature, randomEdit(oldValue)[
						if(focusedObjects.isEmpty) return null;
						val oo = random.selectOne(focusedObjects)
						if(oo!==null && !(feature.containment && oo.isContainerOf(originalObject))) mapping.get(oo) else null
					])
				}
			} else {
				val eAttributeType = (feature as EAttribute).EAttributeType
				if(feature.many) {
					randomEditList(oldValue as List<Object>, [random.randomValue(eAttributeType)])
				} else {
					val value = randomEdit(oldValue)[randomValue(eAttributeType, it)]
					if(value===null) {
						object.eUnset(feature)
					}
					else object.eSet(feature, value)
				}
			}
		}
	}
	
	
	def boolean isContainerOf(EObject o1, EObject o2) {
		var o = o2
		
		while(o!==null) {
			if(o===o1) return true
			o = o.eContainer
		}
		
		false
	}
	
	def List<EObject> selectAll() {
		return objectsOfType
	}
	
	def Set<EObject> select(int num) {
		val selection = new HashSet<EObject>
		var retry = 0;
		while(selection.size < num) {
			val oldSize = selection.size
			var i = random.nextInt(objectsOfType.size)
			selection.add(objectsOfType.get(i))
			if(oldSize===selection.size) {
				retry ++
				if(retry>=10) return selection
			} else  retry = 0
		}
		
		return selection
	}
	
	def getMutant(EObject object) {
		mapping.get(object)
	}
	
	def void doMutation(EObject object) {
		push()
		mutate(object)
	}
}