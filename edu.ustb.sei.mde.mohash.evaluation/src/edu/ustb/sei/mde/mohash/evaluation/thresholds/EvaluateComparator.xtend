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
import java.util.Set
import java.util.HashSet
import edu.ustb.sei.mde.mohash.EObjectSimHasher
import java.io.PrintStream
import edu.ustb.sei.mde.mohash.functions.Hash64
import edu.ustb.sei.mde.mohash.ObjectIndex
import edu.ustb.sei.mde.mohash.HashValue64
import edu.ustb.sei.mde.mohash.functions.StringSimHash64
import edu.ustb.sei.mde.mohash.emfcompare.HashBasedEObjectIndex

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
		val thresholds = newDoubleArrayOfSize(1)
		val invThreshold = 0.5
		thresholds. set(0, invThreshold)
		factory.setThresholds(thresholds)
		
		val evaluator = new EvaluateComparator(UMLPackage.eINSTANCE,  factory)
		val hasher = factory.hasher
		for(var i=0; i<5; i++) {
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
				val sim = ObjectIndex.similarity(new HashValue64(h0), new HashValue64(h2))
				if(sim < threshold || !differences.contains(o.eContainer)) {
					critical+=o
				}
			} else if(r1!==null && r2===null) {
				if(!differences.contains(o.eContainer)) {
					critical+=o
				}
			} else {
				val sim = ObjectIndex.similarity(new HashValue64(h0), new HashValue64(h2))
				if(sim <= threshold && !differences.contains(o.eContainer)) {
					critical+=o
				}
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
			out.print("Common left\t") out.print(o) out.print("\t") out.println(o.eContainer)
			out.print("Mohash.right\t") out.println(r1)
			out.print("EMFcom.right\t") out.println(r2)
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