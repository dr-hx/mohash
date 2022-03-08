package edu.ustb.sei.mde.mohash.evaluation.thresholds

import org.eclipse.emf.compare.Comparison
import org.eclipse.emf.ecore.EObject
import java.util.List
import java.util.ArrayList
import org.eclipse.emf.ecore.util.EcoreUtil
import edu.ustb.sei.mde.mohash.emfcompare.MoHashMatchEngineFactory
import org.eclipse.emf.compare.match.IMatchEngine
import org.eclipse.emf.ecore.resource.Resource
import edu.ustb.sei.mde.mumodel.ResourceSetExtension
import java.util.Collections
import edu.ustb.sei.mde.mumodel.ModelMutator
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.compare.scope.DefaultComparisonScope
import java.util.Arrays
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.swt.widgets.Monitor
import org.eclipse.emf.common.util.BasicMonitor
import org.eclipse.uml2.uml.UMLPackage
import edu.ustb.sei.mde.mohash.HWTreeBasedIndex

class EvaluateComparator {
	private def int checkMatch(EObject object, Comparison mohash, Comparison emfc) {
		val m1 = mohash.getMatch(object)
		val m2 = emfc.getMatch(object)
		
		if(m1===null && m2===null) return 0
		else if(m1===null || m2===null) return 1
		else return if(m1.left===m2.left && m1.right===m2.right) {
			0
		} else 1
	}
	def void checkComparisonResults(Resource original, ComparisonResult result) {
		var total = 0
		var diffs = 0
		
		val iter = original.allContents
		while (iter.hasNext) {
			val next = iter.next
			total++
			diffs += checkMatch(next, result.mohash, result.emfcomp)
		}
		
		result.total = total
		result.diffs = diffs
	}
	
	extension ResourceSetExtension = new ResourceSetExtension
	
	val Resource original
	val Resource mutant
	
	val IMatchEngine mohash
	val IMatchEngine emfcomp
	
	new(EObject root, MoHashMatchEngineFactory factory) {
		this(Collections.singletonList(root), factory)
	}
	
	new(Resource model, MoHashMatchEngineFactory factory) {
		this(model.contents, factory)
	}
	
	new(List<EObject> roots, MoHashMatchEngineFactory factory) {
		factory.reset
		mohash = factory.matchEngine
		emfcomp = MoHashMatchEngineFactory.createEMFCompareFactory.matchEngine
		
		original = createResource('/Mohash/pseudo_original.xmi')
		mutant = createResource('/Mohash/pseudo_mutant.xmi')
		
		original.contents += EcoreUtil.copyAll(roots)
	}
	
	def ComparisonResult evaluate(List<EClass> ignoredClasses, List<EStructuralFeature> ignoredFeatures) {
		val mutator = new ModelMutator
		mutator.ignoredClasses = ignoredClasses
		mutator.ignoredFeatures = ignoredFeatures
		
		mutant.contents.clear
		val result = new ComparisonResult
		
		result.numOfEdits = mutator.mutateResource(original, mutant)
		
		val mohashScope = new DefaultComparisonScope(original, mutant, null)
		val mohashStart = System.currentTimeMillis
		result.mohash = mohash.match(mohashScope, new BasicMonitor)
		val mohashEnd = System.currentTimeMillis
		result.mohashTime = (mohashEnd - mohashStart)
		
		val emfcScope = new DefaultComparisonScope(original, mutant, null)
		val emfcStart = System.currentTimeMillis
		result.emfcomp = emfcomp.match(emfcScope, new BasicMonitor)
		val emfcEnd = System.currentTimeMillis
		result.emfcompTime = (emfcEnd - emfcStart)
		
		checkComparisonResults(original, result)
		
		result
	}
	
	def static void main(String[] args) {
		val factory = new MoHashMatchEngineFactory
//		factory.objectIndexBuilder = [t| new HWTreeBasedIndex(100, 32)]
		val thresholds = newDoubleArrayOfSize(1)
		thresholds. set(0, 0.5)
		factory.setThresholds(thresholds)
		
		val evaluator = new EvaluateComparator(UMLPackage.eINSTANCE,  factory)
		
		for(var i=0; i<10; i++) {
			val result = evaluator.evaluate(#[EcorePackage.eINSTANCE.EAnnotation, EcorePackage.eINSTANCE.EGenericType, EcorePackage.eINSTANCE.EFactory, EcorePackage.eINSTANCE.EStringToStringMapEntry], #[])
			println(result)
		}
	}
}

class ComparisonResult {
	public var Comparison mohash
	public var Comparison emfcomp
	
	public var long mohashTime
	public var long emfcompTime
	
	public var int diffs
	public var int total
	public var int numOfEdits
	
	override toString() {
		return '''ComparisonResult [numOfEdits=«numOfEdits», mohashTime=«mohashTime», emfcompTime=«emfcompTime», diffs=«diffs», total=«total»]''' 
	}
}