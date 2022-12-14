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
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.emf.ecore.EPackage
import org.eclipse.uml2.uml.UMLPackage
import java.util.Set
import edu.ustb.sei.mde.mohash.TypeMap
import edu.ustb.sei.mde.mohash.onehot.EObjectOneHotHasher
import java.util.function.Consumer

/**
 * RQ what is the best threshold for the similarity that can determine whether two eobjects are dissimilar.
 */
@Deprecated class EvaluateSimThreshold {
	val ElementMutator mutator
	val URIComputer uriComputer = new URIComputer
	val DistanceFunction distance
	val EObjectSimHasher hasher
	val EObjectOneHotHasher onehotHasher;
	val mutationCount = 20
	
	new(EClass type, DistanceFunction distance, EObjectSimHasher hasher) {
		mutator = new ElementMutator(type)
		this.distance = distance
		this.hasher = hasher
		this.onehotHasher = new EObjectOneHotHasher
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
	

	
	def estimateOne(EObject original, PrintStream out) {
		hasher.reset
		onehotHasher.reset();
		mutator.doMutation(original)
		
		
		val mutant = mutator.getMutant(original)
		val originalParent = original.eContainer
		val mutantParent = mutant.eContainer
		
		
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
//			val hamsim = Hash64.hammingSimilarity(originalHashValue, mutantHashValue)

			val ohOriginalValue = onehotHasher.hash(original)
			val ohMutantValue = onehotHasher.hash(mutant)
			val ohsim = EObjectOneHotHasher.onehotSim(ohOriginalValue, ohMutantValue);
			
			val tuple = new EstimationTuple(dist, cossim, jacsim, ohsim)
			estimationTuples += tuple
			
//			if(out!==null) {
//				if(dist<dist_thresh && cossim < 0.5) {					
//					out.print(dist>dist_thresh ? "N/A" : dist)
//					out.print("\t")
//					out.print(dist_thresh)
//					out.print("\t")
//					out.print(cossim)
//					out.print("\t")
//					out.print(jacsim)
//					out.print("\t")
//					out.print(original.eObjectPrinter)
//					out.print("\t")
//					out.print(mutant.eObjectPrinter)
//					out.println
//				}
//				
//			}
			
			return dist>dist_thresh
		} catch (Exception exception) {
			System.err.println("error occurring")
			exception.printStackTrace
			
			return true
		}
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
	
	def estimateAll(PrintStream out) {
		printHeader(out)
		val allObjects = mutator.selectAll
		var over = 0
		for (object : allObjects) {
			for(var i =0; i< mutationCount;i++) {
				if(estimateOne(object, out)) over++
			}
			if(out!==null) out.println
		}
		if (over > mutationCount * allObjects.size * 0.5) {
			println("["+mutator.type.name+"] Terminate at " + mutator.featureChangeRate)
			return false
		} else return true
	}
	def evaluateThreshold(double cosThreshold, double jacThreshold, PrintStream out) {
		var double reducedCosRateSum = 0
		var double reducedJacRateSum = 0
		var int reducedCase = 0
		var double uselessCosRateSum = 0
		var double uselessJacRateSum = 0
		var int uselessCase = 0
		
		for(tuple : evaluationTuples) {
			var cSimRate = tuple.otherCosSims / (mutator.selectAll.size as double)
			var jSimRate = tuple.otherJacSims / (mutator.selectAll.size as double)
			if(tuple.dist < Double.MAX_VALUE) {
				reducedCase++
				reducedCosRateSum += (1 - cSimRate)
				reducedJacRateSum += (1 - jSimRate)
			} else {
				uselessCase++
				uselessCosRateSum += cSimRate
				uselessJacRateSum += jSimRate
			}
		}
		
		val double avgReducedCosRate = reducedCosRateSum / reducedCase
		val double avgReducedJacRate = reducedJacRateSum / reducedCase
		val double avgUselessCosRate = uselessCosRateSum / uselessCase
		val double avgUselessJacRate = uselessJacRateSum / uselessCase
		
		out.print("[cos] threshold:\t") out.println(cosThreshold)
		out.print("avgReducedCosRate:") out.print(avgReducedCosRate) out.println
		out.print("avgUselessCosRate:") out.print(avgUselessCosRate) out.println
		
		out.print("[jac] threshold:\t") out.println(jacThreshold)
		out.print("avgReducedJacRate:") out.print(avgReducedJacRate) out.println
		out.print("avgUselessJacRate:") out.print(avgUselessJacRate) out.println
		
		out.println
	}
	
	def evaluateAll(double cosThreshold, double jacThreshold) {
		val allObjects = mutator.selectAll
		for (object : allObjects) {
			for(var i =0; i< mutationCount;i++) {
				evaluateOne(object, cosThreshold, jacThreshold)
			}
		}
	}
	def evaluateOne(EObject original, double cosThreshold, double jacThreshold) {
		hasher.reset
		mutator.doMutation(original)
		
		val mutant = mutator.getMutant(original)
		val originalParent = original.eContainer
		val mutantParent = mutant.eContainer
		
		// when sim(mutant, original) > threshold, we count 1 - |sim(mutant, other)>threshold|/|allObjects| => reduced size
		// when sim(mutant, original) < threshold, we count |sim(mutant, other)>threshold|/|allObjects| => useless size
		
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
			
			val tuple = new EvaluationTuple(dist, cossim, jacsim)
			for(object : mutator.selectAll) {
				if(object!==original) {
					val objectHash = hasher.hash(object)
					val objectHashValue = new HashValue64(objectHash)
					val ocossim = Hash64.cosineSimilarity(objectHashValue, mutantHashValue)
					val ojacsim = Hash64.jaccardSimilarity(objectHashValue, mutantHashValue)
					if(ocossim>=cosThreshold) tuple.otherCosSims ++
					if(ojacsim>=jacThreshold) tuple.otherJacSims ++
				}
			}
			evaluationTuples += tuple
		} catch(Exception e){}
	}
	
	protected val estimationTuples = new ArrayList<EstimationTuple>(8192);
	protected val estimations = new ArrayList<Estimation>(256)
	protected val evaluationTuples = new ArrayList<EvaluationTuple>(256)
	
	def void estimateThreshold(double lb, double ub, double step, Consumer<PrintStream> prefix, PrintStream out, boolean printBestOnly) {
		var num = Math.round((ub - lb) / step) as int
		var thlist = newDoubleArrayOfSize(num+1)
		var th = lb
		for(var i = 0; i<=num; i++) {
			thlist.set(i,th)
			th += step
		}
		
		estimateThreshold(thlist, prefix, out, printBestOnly)
	}
	
	def void estimateThreshold(double[] thlist, Consumer<PrintStream> prefix, PrintStream out, boolean printBestOnly) {
		for(double th : thlist) {
			estimations += new Estimation(estimationTuples.size, th)
		}
		
		estimationTuples.forEach[tuple|
			val simByEditDist = tuple.dist !== Double.MAX_VALUE
			for(var i=0; i<thlist.length; i++) {
				val th = thlist.get(i)
				val estimation = estimations.get(i)
				
				val simByCosHash = tuple.cosSim >= th
				val simByJacHash = tuple.jacSim >= th
				val simByOHHash = tuple.ohSim >= th
				
				if(simByEditDist) {
					if(simByCosHash) estimation.cosTruePostive ++
					else estimation.cosFalseNegative ++
					
					if(simByJacHash) estimation.jacTruePostive ++
					else estimation.jacFalseNegative ++

					if(simByOHHash) estimation.ohTruePostive ++
					else estimation.ohFalseNegative ++
					
				} else {
					if(simByCosHash) estimation.cosFalsePostive ++
					else estimation.cosTrueNegative ++
					
					if(simByJacHash) estimation.jacFalsePostive ++
					else estimation.jacTrueNegative ++
					
					if(simByOHHash) estimation.ohFalsePostive ++
					else estimation.ohTrueNegative ++
				}
			}
			
		]
		if(printBestOnly) {
			var double bestCosF1 = - Double.MIN_VALUE
			var double bestJacF1 = - Double.MIN_VALUE
			var double bestOHF1 = - Double.MIN_VALUE
			var double cosRecall = 0
			var double jacRecall = 0
			var double ohRecall = 0
			var Estimation bestCosEst = null
			var Estimation bestJacEst = null
			var Estimation bestOHEst = null
			
			for(estimation : estimations) {
				{
					val cosPrec = (estimation.cosTruePostive)/ ((estimation.cosTruePostive + estimation.cosFalsePostive) as double)
					val cosRec = (estimation.cosTruePostive)/ ((estimation.cosTruePostive + estimation.cosFalseNegative) as double)
					val F1 = 2*cosPrec*cosRec/(cosPrec + cosRec)
					if(bestCosF1 <= F1) {
						bestCosF1 = F1
						cosRecall = cosRec
						bestCosEst = estimation
					}
				}
				
				{
					val jacPrec = (estimation.jacTruePostive)/ ((estimation.jacTruePostive + estimation.jacFalsePostive) as double)
					val jacRec = (estimation.jacTruePostive)/ ((estimation.jacTruePostive + estimation.jacFalseNegative) as double)
					val F1 = 2*jacPrec*jacRec/(jacPrec + jacRec)
					if(bestJacF1 <= F1) {
						bestJacF1 = F1
						jacRecall = jacRec
						bestJacEst = estimation
					}
				}
				
				{
					val ohPrec = (estimation.ohTruePostive)/ ((estimation.ohTruePostive + estimation.ohFalsePostive) as double)
					val ohRec = (estimation.ohTruePostive)/ ((estimation.ohTruePostive + estimation.ohFalseNegative) as double)
					val F1 = 2*ohPrec*ohRec/(ohPrec + ohRec)
					if(bestOHF1 <= F1) {
						bestOHF1 = F1
						ohRecall = ohRec
						bestOHEst = estimation
					}
				}
			}
			
			if (bestCosEst === null) {
				prefix.accept(out) out.println("\tCos: N/A")
			} else {
				prefix.accept(out) out.print("\tCos: Best F1score=" + bestCosF1 + " with recall=" + cosRecall)
				out.print("\t")
				printEstimation(bestCosEst, out, 1)
			}
			
			if (bestJacEst === null) {
				prefix.accept(out) out.println("\tJac: N/A")
			} else {
				prefix.accept(out) out.print("\tJac: Best F1score=" + bestJacF1 + " with recall=" + jacRecall)
				out.print("\t")
				printEstimation(bestJacEst, out, 2)
			}
			
			if (bestOHEst === null) {
				prefix.accept(out) out.println("\tOneHot: N/A")
			} else {
				prefix.accept(out) out.print("\tOneHot: Best F1score=" + bestOHF1 + " with recall=" + ohRecall)
				out.print("\t")
				printEstimation(bestOHEst, out, 4)
			}
		} else {			
			for(estimation : estimations) {
				prefix.accept(out) printEstimation(estimation, out, 7)
			}
		}
	}
	
	protected def void printEstimation(Estimation estimation, PrintStream out, int flag) {
		out.print("("+estimation.threshold+")")
		if((flag.bitwiseAnd(1))!=0) {
			out.print("\tTP%=")
			out.print(estimation.cosTruePostive/ (estimation.total as double))
			out.print("\tTN%=")
			out.print(estimation.cosTrueNegative/ (estimation.total as double))
			out.print("\tFP%=")
			out.print(estimation.cosFalsePostive/ (estimation.total as double))
			out.print("\tFN%=")
			out.print(estimation.cosFalseNegative/ (estimation.total as double))
			out.print("\tPrec=")
			val cosPrec = (estimation.cosTruePostive)/ ((estimation.cosTruePostive + estimation.cosFalsePostive) as double)
			out.print(cosPrec)
			out.print("\tRecall(TPR)=")
			val cosRec = (estimation.cosTruePostive)/ ((estimation.cosTruePostive + estimation.cosFalseNegative) as double)
			out.print(cosRec)
			out.print("\tF1=")
			out.print(2*cosPrec*cosRec/(cosPrec + cosRec))
			out.print("\tFPR(1-Spec)=")// FPR = FP / (FP+TN), 1-specificity
			val ohFPR = (estimation.cosFalsePostive)/ ((estimation.cosFalsePostive + estimation.cosTrueNegative) as double)
			out.print(ohFPR)
		}
		
		if(flag.bitwiseAnd(2)!=0) {
			out.print("\tTP%=")
			out.print(estimation.jacTruePostive/ (estimation.total as double))
			out.print("\tTN%=")
			out.print(estimation.jacTrueNegative/ (estimation.total as double))
			out.print("\tFP%=")
			out.print(estimation.jacFalsePostive/ (estimation.total as double))
			out.print("\tFN%=")
			out.print(estimation.jacFalseNegative/ (estimation.total as double))
			out.print("\tPrec=")
			val jacPrec = (estimation.jacTruePostive)/ ((estimation.jacTruePostive + estimation.jacFalsePostive) as double)
			out.print(jacPrec)
			out.print("\tRecall(TPR)=")
			val jacRec = (estimation.jacTruePostive)/ ((estimation.jacTruePostive + estimation.jacFalseNegative) as double)
			out.print(jacRec)
			out.print("\tF1=")
			out.print(2*jacPrec*jacRec/(jacPrec + jacRec))
			out.print("\tFPR(1-Spec)=")// FPR = FP / (FP+TN)
			val ohFPR = (estimation.jacFalsePostive)/ ((estimation.jacFalsePostive + estimation.jacTrueNegative) as double)
			out.print(ohFPR)
		}
		
		if(flag.bitwiseAnd(4)!=0) {
			out.print("\tTP%=")
			out.print(estimation.ohTruePostive/ (estimation.total as double))
			out.print("\tTN%=")
			out.print(estimation.ohTrueNegative/ (estimation.total as double))
			out.print("\tFP%=")
			out.print(estimation.ohFalsePostive/ (estimation.total as double))
			out.print("\tFN%=")
			out.print(estimation.ohFalseNegative/ (estimation.total as double))
			out.print("\tPrec=")
			val ohPrec = (estimation.ohTruePostive)/ ((estimation.ohTruePostive + estimation.ohFalsePostive) as double)
			out.print(ohPrec)
			out.print("\tRecall(TPR)=")// TPR
			val ohRec = (estimation.ohTruePostive)/ ((estimation.ohTruePostive + estimation.ohFalseNegative) as double)
			out.print(ohRec)
			out.print("\tF1=")
			out.print(2*ohPrec*ohRec/(ohPrec + ohRec))
			out.print("\tFPR(1-Spec)=")// FPR = FP / (FP+TN)
			val ohFPR = (estimation.ohFalsePostive)/ ((estimation.ohFalsePostive + estimation.ohTrueNegative) as double)
			out.print(ohFPR)
		}
		out.println
	}
	
	
	def static void main(String[] args) {
		
		estimate(EcorePackage.eINSTANCE, UMLPackage.eINSTANCE, #{EcorePackage.eINSTANCE.EStringToStringMapEntry, EcorePackage.eINSTANCE.EAnnotation, EcorePackage.eINSTANCE.EGenericType})
		
//		val TypeMap<Pair<Double,Double>> pairs = new TypeMap(null)
//		pairs.put(EcorePackage.eINSTANCE.EPackage, 0.44->0.4)
//		pairs.put(EcorePackage.eINSTANCE.EDataType, 0.48->0.4)
//		pairs.put(EcorePackage.eINSTANCE.EEnum, 0.54->0.4)
//		pairs.put(EcorePackage.eINSTANCE.EAttribute, 0.74->0.58)
//		pairs.put(EcorePackage.eINSTANCE.EEnumLiteral, 0.50->0.4)
//		pairs.put(EcorePackage.eINSTANCE.EClass, 0.68->0.50)
//		pairs.put(EcorePackage.eINSTANCE.EReference, 0.74->0.58)
//		pairs.put(EcorePackage.eINSTANCE.EOperation, 0.66->0.48)
//		pairs.put(EcorePackage.eINSTANCE.EParameter, 0.7->0.51)
//		evaluate(EcorePackage.eINSTANCE, UMLPackage.eINSTANCE, pairs)
	}
	
		
	def static void evaluate(EPackage metamodel, EObject root, TypeMap<Pair<Double,Double>> thresholdMap) {
		val list = metamodel.EClassifiers.filter[it instanceof EClass].map[it as EClass].filter[it.abstract===false].toList
		list.forEach[t|
			val pair = thresholdMap.get(t);
			if(pair!==null) {
				evaluate(t, root, pair.key, pair.value);
			}
		]
	}
	
	protected def static void evaluate(EClass clazz, EObject root, double cosThreshold, double jacThreshold) {
		val factory = new MoHashMatchEngineFactory()
		factory.matchEngine
		
		val e = new  EvaluateSimThreshold(clazz, factory.distance, factory.hasher)
		e.prepare(root)
		
		val out = System.out;
		e.evaluateAll(cosThreshold, jacThreshold)
		synchronized(out) {
			out.println('''[??clazz.name??]''')
			e.evaluateThreshold(cosThreshold, jacThreshold, out)			
		}
//		out.close
	}
	
	
	protected def static void estimate(EPackage metamodel, EObject model, Set<EClass> ignored) {
		val out = System.out; //new PrintStream(new FileOutputStream('/Users/hexiao/Projects/Java/git/mohash/edu.ustb.sei.mde.mohash.evaluation/output/sim.txt'))
		metamodel.EClassifiers.filter[it instanceof EClass].map[it as EClass].filter[!it.abstract && !ignored.contains(it)].toList.parallelStream.forEach[
			val factory = new MoHashMatchEngineFactory()
			factory.matchEngine
			synchronized(out) {				
				out.println("Test "+it.name)
			}
			
			val e = new  EvaluateSimThreshold(it, factory.distance, factory.hasher)
			e.prepare(model)
			
			for(var mr = 0.1; mr > 0 && mr < 1; mr += 0.1) {
				e.mutator.featureChangeRate = mr
				if(!e.estimateAll(null)) mr = -1
			}
			synchronized(out) {				
				e.estimateThreshold(0.1, 0.8, 0.01, [o|o.print("["+it.name+"] ")], out, true)
			}
		]
//		out.close
	}
//	protected def static void estimate(EClass clazz, EObject model, boolean best) {
//		val factory = new MoHashMatchEngineFactory()
//		factory.matchEngine
//		
//		val e = new  EvaluateSimThreshold(clazz, factory.distance, factory.hasher)
//		e.prepare(model)
//		
//		val out = System.out; //new PrintStream(new FileOutputStream('/Users/hexiao/Projects/Java/git/mohash/edu.ustb.sei.mde.mohash.evaluation/output/sim.txt'))
//		for(var mr = 0.05; mr < 0.5; mr += 0.05) {
//			e.mutator.featureChangeRate = mr
//			e.estimateAll(null)
//		}
//		e.estimateThreshold(0.4, 0.8, 0.01, out, best)
////		out.close
//	}
}

class EvaluationTuple {
	public val double dist
	public val double cosSim
	public val double jacSim
//	public val double ohSim
	new(double dist, double cosSim, double jacSim) {
		this.dist = dist
		this.cosSim = cosSim
		this.jacSim = jacSim
//		this.ohSim = ohSim
//		this.hamSim = hamSim
	}
	public var otherCosSims = 0
	public var otherJacSims = 0
//	public var otherOHSims = 0
}

class EstimationTuple {
	public val double dist
	public val double cosSim
	public val double jacSim
	public val double ohSim
//	public val double hamSim
	
	new(double dist, double cosSim, double jacSim, double ohSim) {
		this.dist = dist
		this.cosSim = cosSim
		this.jacSim = jacSim
		this.ohSim = ohSim
//		this.hamSim = hamSim
	}
}

class Evaluation {
	public val double threshold
	public val int total
	new(int total, double threshold) {
		this.total = total
		this.threshold = threshold
	}
	public var double reducedRate = 0.0
	public var double uselessRate = 0.0
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
	
	public var ohTruePostive = 0
	public var ohTrueNegative = 0
	public var ohFalsePostive = 0
	public var ohFalseNegative = 0
}