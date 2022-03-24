package edu.ustb.sei.mde.mumodel

import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EStructuralFeature

class ElementGenerator {
	val random = new RandomUtils
	
	def createElement(EClass type) {
		val obj = EcoreUtil.create(type)
		initElement(obj)
		obj
	}
	
	protected def shouldIgnore(EStructuralFeature feature) {
		feature.derived || feature.transient || feature.volatile || feature.changeable===false
	}
	
	protected def initElement(EObject object) {
		val attrs = object.eClass.EAllAttributes.filter[!it.shouldIgnore]
		
		attrs.forEach[attr|
			if(attr.many) {
				object.eSet(attr, random.randomValueList(attr.EAttributeType, attr.lowerBound, attr.upperBound));
			} else {
				object.eSet(attr, random.randomValue(attr.EAttributeType));
			}
		]
	}
}