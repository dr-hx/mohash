package edu.ustb.sei.mde.mohash.evaluation.thresholds

import edu.ustb.sei.mde.mohash.EObjectSimHasher
import edu.ustb.sei.mde.mohash.HashValue64
import edu.ustb.sei.mde.mohash.ObjectIndex
import edu.ustb.sei.mde.mohash.TypeMap
import edu.ustb.sei.mde.mohash.emfcompare.HashBasedEObjectIndex
import edu.ustb.sei.mde.mohash.emfcompare.MoHashMatchEngineFactory
import edu.ustb.sei.mde.mohash.functions.Hash64
import edu.ustb.sei.mde.mumodel.ModelMutator
import edu.ustb.sei.mde.mumodel.ResourceSetExtension
import java.io.PrintStream
import java.util.Collections
import java.util.HashSet
import java.util.List
import java.util.Set
import org.eclipse.emf.common.util.BasicMonitor
import org.eclipse.emf.compare.Comparison
import org.eclipse.emf.compare.match.IMatchEngine
import org.eclipse.emf.compare.scope.DefaultComparisonScope
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.uml2.uml.UMLPackage

import static edu.ustb.sei.mde.mohash.emfcompare.HashBasedEObjectIndex.*

class EvaluateComparator {
	private def int checkMatch(EObject object, ComparisonResult result) {
		val m1 = result.mohash.getMatch(object)
		val m2 = result.emfcomp.getMatch(object)
		
		if(m1===null && m2===null) return 0
		else if(m1===null || m2===null) return {
			result.recordDiff(object)
			1
		}
		else return if(m1.left===m2.left && m1.right===m2.right) {
			0
		} else {
			result.recordDiff(object)
			1
		}
	}
	def void checkComparisonResults(Resource original, ComparisonResult result) {
		var total = 0
		var diffs = 0
		
		val iter = original.allContents
		while (iter.hasNext) {
			val next = iter.next
			total++
			diffs += checkMatch(next, result)
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
		val thresholds = new TypeMap<Double>(0.5)
		thresholds.put(EcorePackage.eINSTANCE.EPackage, 0.15)
		thresholds.put(EcorePackage.eINSTANCE.EClass, 0.55)
		thresholds.put(EcorePackage.eINSTANCE.EReference, 0.65)
		thresholds.put(EcorePackage.eINSTANCE.EOperation, 0.55)
		thresholds.put(EcorePackage.eINSTANCE.EAttribute, 0.68)
		thresholds.put(EcorePackage.eINSTANCE.EStringToStringMapEntry, 0.4)
		thresholds.put(EcorePackage.eINSTANCE.EEnum, 0.5)
		thresholds.put(EcorePackage.eINSTANCE.EEnumLiteral, 0.45)
		thresholds.put(EcorePackage.eINSTANCE.EParameter, 0.5)
		
		val invThreshold = 0.5
		factory.setThresholdMap(thresholds)
		
		val nohashTypes = newHashSet(EcorePackage.eINSTANCE.EAnnotation, EcorePackage.eINSTANCE.EGenericType, EcorePackage.eINSTANCE.EFactory, EcorePackage.eINSTANCE.EStringToStringMapEntry);
		
		factory.ignoredClasses = nohashTypes
		
		val evaluator = new EvaluateComparator(EcorePackage.eINSTANCE,  factory)
		val hasher = factory.hasher
		for(var i=0; i<10; i++) {
			val result = evaluator.evaluate(#[EcorePackage.eINSTANCE.EAnnotation, EcorePackage.eINSTANCE.EGenericType, EcorePackage.eINSTANCE.EFactory, EcorePackage.eINSTANCE.EStringToStringMapEntry], #[])
			result.print(System.out, hasher, 1 - invThreshold)
			println(HashBasedEObjectIndex.identicCount) HashBasedEObjectIndex.identicCount = 0
		}

		val hash = hasher.hash(UMLPackage.eINSTANCE.durationConstraint__ValidateFirstEventMultiplicity__DiagnosticChain_Map);
		println(Hash64.toString(hash))
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
		// critical mismatch: sim is higher than the threshold, but 
		
		return '''ComparisonResult [numOfEdits=«numOfEdits», mohashTime=«mohashTime», emfcompTime=«emfcompTime», diffs=«diffs», total=«total»]''' 
	}
	
	val Set<EObject> differences = new HashSet
	
	def void recordDiff(EObject source) {
		differences += source
	}
	
	def void print(PrintStream out, EObjectSimHasher hasher, double threshold) {
		out.println(toString)
		
		val critical = new HashSet<EObject>()
		for(o : differences) {
			val m1 = mohash.getMatch(o)
			val m2 = emfcomp.getMatch(o)
			
			val r1 = m1.right
			val r2 = m2.right
			val h0 = hasher.hash(o)
			val h1 = r1!==null ? hasher.hash(r1) : 0
			val h2 = r2!==null ? hasher.hash(r2) : 0
			
			if(r1===null && r2!==null) {
//				val sim = ObjectIndex.similarity(new HashValue64(h0), new HashValue64(h2))
//				if(sim < threshold || !differences.contains(o.eContainer)) {
//				}
				critical+=o
			} else if(r1!==null && r2===null) {
				critical+=o
			} else {
				critical+=o
			}
		}
		
		out.println("criticalMiss "+critical.size)
		critical.forEach[o|
			val m1 = mohash.getMatch(o)
			val m2 = emfcomp.getMatch(o)
			
			val r1 = m1.right
			val r2 = m2.right
			val h0 = hasher.hash(o)
			val h1 = r1!==null ? hasher.hash(r1) : 0
			val h2 = r2!==null ? hasher.hash(r2) : 0
			
			out.println("Content")
			out.print("Common left\t") out.print(o) out.print("\t") out.print(o.eContainer) out.print("\t") out.println(o.eContainer?.eContainer)
			out.print("Mohash.right\t") out.print(r1) out.print("\t") out.print(r1?.eContainer) out.print("\t") out.println(r1?.eContainer?.eContainer)
			out.print("EMFcom.right\t") out.print(r2) out.print("\t") out.print(r2?.eContainer) out.print("\t") out.println(r2?.eContainer?.eContainer)
			out.println("Hash value")
			out.print("Common left\t") 
			out.println(Hash64.toString(h0))
			out.print("Mohash.right\t") 
			out.print(Hash64.toString(h1))
			if(h1!==0) {
				out.print("\t")
				out.println(ObjectIndex.similarity(new HashValue64(h0), new HashValue64(h1)))
			}
			else out.println()
			out.print("EMFcom.right\t") 
			out.print(Hash64.toString(h2))
			if(h2!==0) {
				out.print("\t")
				out.println(ObjectIndex.similarity(new HashValue64(h0), new HashValue64(h2)))
			}
			else out.println()
			out.println
		]
//		differences.forEach[o|
//			val m1 = mohash.getMatch(o)
//			val m2 = emfcomp.getMatch(o)
//			
//			val r1 = m1.right
//			val r2 = m2.right
//			val h0 = hasher.hash(o)
//			val h1 = r1!==null ? hasher.hash(r1) : 0
//			val h2 = r2!==null ? hasher.hash(r2) : 0
//			
//			out.println("Content")
//			out.print("Common left\t") out.print(o) out.print("\t") out.println(o.eContainer)
//			out.print("Mohash.right\t") out.println(r1)
//			out.print("EMFcom.right\t") out.println(r2)
//			out.println("Hash value")
//			out.print("Common left\t") 
//			out.println(Hash64.toString(h0))
//			out.print("Mohash.right\t") 
//			out.print(Hash64.toString(h1))
//			if(h1!==0) {
//				out.print("\t")
//				out.println(ObjectIndex.similarity(new HashValue64(h0), new HashValue64(h1)))
//			}
//			else out.println()
//			out.print("EMFcom.right\t") 
//			out.print(Hash64.toString(h2))
//			if(h2!==0) {
//				out.print("\t")
//				out.println(ObjectIndex.similarity(new HashValue64(h0), new HashValue64(h2)))
//			}
//			else out.println()
//			out.println
//		]
	}
}