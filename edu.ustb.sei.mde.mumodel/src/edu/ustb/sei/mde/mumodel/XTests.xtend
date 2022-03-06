package edu.ustb.sei.mde.mumodel

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.emf.ecore.xmi.XMIResource
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl

class XTests {
	extension ResourceSetExtension = new ResourceSetExtension
	
	@Test
	def void testGenID() {
		val result = createResource('/Users/hexiao/Projects/Java/mohash/edu.ustb.sei.mde.mumodel/model/output.xmi')
		ModelMutator.saveAsIdBasedResource(EcorePackage.eINSTANCE, result as XMIResourceImpl)
		result.save(System.out, null)
	}
	
	@Test
	def void testModelCopy() {
		val result = createResource('/Users/hexiao/Projects/Java/mohash/edu.ustb.sei.mde.mumodel/model/copy.xmi')
		ModelMutator.saveAs(EcorePackage.eINSTANCE, result)
		result.save(null)
	}
	
	@Test
	def void testMutation() {
//		for(var i=0;i<100;i++) {			
			val mutator = new ModelMutator()
			mutator.ignoredClasses = #[EcorePackage.eINSTANCE.EAnnotation, EcorePackage.eINSTANCE.EGenericType, EcorePackage.eINSTANCE.EFactory, EcorePackage.eINSTANCE.EStringToStringMapEntry]
			val result = createResource('/Users/hexiao/Projects/Java/mohash/edu.ustb.sei.mde.mumodel/model/output.xmi')
			mutator.mutateModel(EcorePackage.eINSTANCE, result)
			result.save(null)
//		}
	}
}