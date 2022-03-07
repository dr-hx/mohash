package edu.ustb.sei.mde.mohash.evaluation.thresholds

import edu.ustb.sei.mde.mohash.EObjectSimHasher
import edu.ustb.sei.mde.mohash.HashValue64
import edu.ustb.sei.mde.mohash.emfcompare.MoHashMatchEngineFactory
import edu.ustb.sei.mde.mohash.functions.Hash64
import edu.ustb.sei.mde.mohash.functions.URIComputer
import edu.ustb.sei.mde.mumodel.ElementMutator
import java.io.FileOutputStream
import java.io.PrintStream
import java.util.ArrayList
import java.util.Collections
import java.util.List
import org.eclipse.emf.compare.ComparePackage
import org.eclipse.emf.compare.Comparison
import org.eclipse.emf.compare.Match
import org.eclipse.emf.compare.match.eobject.EditionDistance
import org.eclipse.emf.compare.match.eobject.ProximityEObjectMatcher.DistanceFunction
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EReference
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.emf.ecore.util.EcoreUtil

/**
 * RQ what is the best threshold for the similarity that can determine whether two eobjects are dissimilar.
 */
class EvaluateSimThreshold {
	val ElementMutator mutator
	val URIComputer uriComputer = new URIComputer
	val DistanceFunction distance
	val EObjectSimHasher hasher
	
	new(EClass type, DistanceFunction distance, EObjectSimHasher hasher) {
		mutator = new ElementMutator(type)
		this.distance = distance
		this.hasher = hasher
	}
	def prepare(EObject root) {
		prepare(Collections.singletonList(root))
	}
	def prepare(List<EObject> contents) {
		mutator.prepare(contents)
	}
	
	def eObjectPrinter(EObject object) {
		val builder = new StringBuilder
		builder.append(object.eClass.name)
		builder.append(Integer.toHexString(object.hashCode))
		builder.append("{")
		val features = object.eClass.EAllStructuralFeatures
		features.forEach[f, fi|
			if(fi>0) builder.append(", ")
			builder.append(f.name)
			builder.append(":")
			if(f instanceof EReference) {
				if(f.many) {
					builder.append("[")
					val list = object.eGet(f) as List<EObject>
					list.forEach[o, i|
						if(i>0) builder.append(", ")
						builder.append(uriComputer.getOrComputeLocation(o).toList)
					]
					builder.append("]")
				} else {
					val o = object.eGet(f) as EObject
					builder.append(o===null ? "" : uriComputer.getOrComputeLocation(o).toList)
				}
			} else {
				builder.append(object.eGet(f))
			}
		]
		builder.append("}")
		builder.toString
	}
	
	val mutationCount = 10
	
	def check(EObject mutant, PrintStream out) {
		hasher.reset
		
		val original = mutator.getOriginal(mutant)
		
		val mutantParent = mutant.eContainer
		val originalParent = original.eContainer
		
		mutator.doMutation(mutant)
		
		try {
			val comparison = EcoreUtil.create(ComparePackage.eINSTANCE.comparison) as Comparison
			if(originalParent!==null) {
				val match = EcoreUtil.create(ComparePackage.eINSTANCE.match) as Match
				match.left = originalParent
				match.right = mutantParent
				comparison.matches.add(match)
			}
			
			val dist_thresh = Math.max((distance as EditionDistance).getThresholdAmount(original), (distance as EditionDistance).getThresholdAmount(mutant));
			val dist = distance.distance(comparison, original, mutant)
			
			val originalHash = hasher.hash(original)
			val mutantHash = hasher.hash(mutant)
			
			val originalHashValue = new HashValue64(originalHash)
			val mutantHashValue = new HashValue64(mutantHash)
			
			val cossim = Hash64.cosineSimilarity(originalHashValue, mutantHashValue)
			val jacsim = Hash64.jaccardSimilarity(originalHashValue, mutantHashValue)
			val hamsim = Hash64.hammingSimilarity(originalHashValue, mutantHashValue)
			
			if(dist < Double.MAX_VALUE && cossim < 0.5) {
				val t = distance.distance(comparison, original, mutant)
			}
			
			val tuple = new CheckTuple(dist, cossim, jacsim, hamsim)
			checkTuples += tuple
			
			if(out!==null) {
				if(dist<dist_thresh && cossim < 0.5) {					
					out.print(dist>dist_thresh ? "N/A" : dist)
					out.print("\t")
					out.print(dist_thresh)
					out.print("\t")
					out.print(cossim)
					out.print("\t")
					out.print(jacsim)
					out.print("\t")
					out.print(original.eObjectPrinter)
					out.print("\t")
					out.print(mutant.eObjectPrinter)
					out.println
				}
				
			}
		} catch (Exception exception) {
			System.err.println("error occurring")
		}
		
		
		mutator.restore(mutant)
	}
	
	protected def void printHeader(PrintStream out) {
		if(out===null) return;
		out.print("Dist")
		out.print("\t")
		out.print("Dist_Thres")
		out.print("\t")
		out.print("CosSim")
		out.print("\t")
		out.print("JacSim")
		out.print("\t")
		out.print("Original")
		out.print("\t")
		out.print("Mutant")
		out.println
	}
	
	def checkAll(PrintStream out) {
		printHeader(out)
		val allObjects = mutator.selectAll
		for (object : allObjects) {
			for(var i =0; i< mutationCount;i++) {
				check(object, out)
			}
			if(out!==null) out.println
		}
	}
	
	protected val checkTuples = new ArrayList<CheckTuple>(8192);
	protected val estimations = new ArrayList<Estimation>(256)
	
	def void estimateThreshold(double lb, double ub, double step, PrintStream out) {
		var num = Math.round((ub - lb) / step) as int
		var thlist = newDoubleArrayOfSize(num+1)
		var th = lb
		for(var i = 0; i<=num; i++) {
			thlist.set(i,th)
			th += step
		}
		
		estimateThreshold(thlist, out)
	}
	
	def void estimateThreshold(double[] thlist, PrintStream out) {
		for(double th : thlist) {
			estimations += new Estimation(checkTuples.size, th)
		}
		
		checkTuples.forEach[tuple|
			val simByEditDist = tuple.dist !== Double.MAX_VALUE
			for(var i=0; i<thlist.length; i++) {
				val th = thlist.get(i)
				val estimation = estimations.get(i)
				
				val simByCosHash = tuple.cosSim >= th
				val simByJacHash = tuple.jacSim >= th
				val simByHamHash = tuple.hamSim >= th
				
				if(simByEditDist) {
					if(simByCosHash) estimation.cosTruePostive ++
					else estimation.cosFalseNegative ++
					
					if(simByJacHash) estimation.jacTruePostive ++
					else estimation.jacFalseNegative ++

					if(simByHamHash) estimation.hamTruePostive ++
					else estimation.hamFalseNegative ++
					
				} else {
					if(simByCosHash) estimation.cosFalsePostive ++
					else estimation.cosTrueNegative ++
					
					if(simByJacHash) estimation.jacFalsePostive ++
					else estimation.jacTrueNegative ++
					
					if(simByHamHash) estimation.hamFalsePostive ++
					else estimation.hamTrueNegative ++
				}
			}
			
		]
		
		for(estimation : estimations) {
			out.print(estimation.threshold)
			out.print("\t")
			out.print(estimation.cosTruePostive/ (estimation.total as double))
			out.print("\t")
			out.print(estimation.cosTrueNegative/ (estimation.total as double))
			out.print("\t")
			out.print(estimation.cosFalsePostive/ (estimation.total as double))
			out.print("\t")
			out.print(estimation.cosFalseNegative/ (estimation.total as double))
			out.print("\t")
			val cosPrec = (estimation.cosTruePostive)/ ((estimation.cosTruePostive + estimation.cosFalsePostive) as double)
			out.print(cosPrec)
			out.print("\t")
			val cosRec = (estimation.cosTruePostive)/ ((estimation.cosTruePostive + estimation.cosFalseNegative) as double)
			out.print(cosRec)
			out.print("\t")
			out.print(2*cosPrec*cosRec/(cosPrec + cosRec))
			out.print("\t")
			out.print(estimation.jacTruePostive/ (estimation.total as double))
			out.print("\t")
			out.print(estimation.jacTrueNegative/ (estimation.total as double))
			out.print("\t")
			out.print(estimation.jacFalsePostive/ (estimation.total as double))
			out.print("\t")
			out.print(estimation.jacFalseNegative/ (estimation.total as double))
			out.print("\t")
			val jacPrec = (estimation.jacTruePostive)/ ((estimation.jacTruePostive + estimation.jacFalsePostive) as double)
			out.print(jacPrec)
			out.print("\t")
			val jacRec = (estimation.jacTruePostive)/ ((estimation.jacTruePostive + estimation.jacFalseNegative) as double)
			out.print(jacRec)
			out.print("\t")
			out.print(2*jacPrec*jacRec/(jacPrec + jacRec))
			out.println
		}
	}
	
	
	def static void main(String[] args) {
		val factory = new MoHashMatchEngineFactory()
		factory.matchEngine
		
		val e = new  EvaluateSimThreshold(EcorePackage.eINSTANCE.EClass, factory.distance, factory.hasher)
		e.prepare(EcorePackage.eINSTANCE)
		
		val out = new PrintStream(new FileOutputStream('C:/JavaProjects/models/edu.ustb.sei.mde.mohash.evaluation/output/sim.txt'))
		for(var mr = 0.05; mr < 0.5; mr += 0.05) {
			e.mutator.featureChangeRate = mr
			e.checkAll(null)
		}
		e.estimateThreshold(0.4, 0.8, 0.01, out)
		out.close
	}
}

class CheckTuple {
	public val double dist
	public val double cosSim
	public val double jacSim
	public val double hamSim
	
	new(double dist, double cosSim, double jacSim, double hamSim) {
		this.dist = dist
		this.cosSim = cosSim
		this.jacSim = jacSim
		this.hamSim = hamSim
	}
}

class Estimation {
	new(int total, double threshold) {
		this.total = total
		this.threshold = threshold
	}
	public val double threshold
	public val int total
	public var cosTruePostive = 0
	public var cosTrueNegative = 0
	public var cosFalsePostive = 0
	public var cosFalseNegative = 0
	
	public var jacTruePostive = 0
	public var jacTrueNegative = 0
	public var jacFalsePostive = 0
	public var jacFalseNegative = 0
	
	public var hamTruePostive = 0
	public var hamTrueNegative = 0
	public var hamFalsePostive = 0
	public var hamFalseNegative = 0
}