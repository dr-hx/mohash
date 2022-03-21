package edu.ustb.sei.mde.mohash.evaluation.roc

import edu.ustb.sei.mde.mohash.EObjectSimHasher
import edu.ustb.sei.mde.mohash.HashValue64
import edu.ustb.sei.mde.mohash.TypeMap
import edu.ustb.sei.mde.mohash.emfcompare.MoHashMatchEngineFactory
import edu.ustb.sei.mde.mohash.functions.Hash64
import edu.ustb.sei.mde.mohash.functions.URIComputer
import edu.ustb.sei.mde.mohash.onehot.EObjectOneHotHasher
import edu.ustb.sei.mde.mumodel.ElementMutator
import java.io.FileOutputStream
import java.io.PrintStream
import java.util.ArrayList
import java.util.Collections
import java.util.List
import java.util.Set
import java.util.function.Consumer
import org.eclipse.emf.compare.ComparePackage
import org.eclipse.emf.compare.Comparison
import org.eclipse.emf.compare.Match
import org.eclipse.emf.compare.match.eobject.EditionDistance
import org.eclipse.emf.compare.match.eobject.ProximityEObjectMatcher.DistanceFunction
import org.eclipse.emf.compare.match.eobject.WeightProvider
import org.eclipse.emf.compare.match.eobject.WeightProviderDescriptorRegistryImpl
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EReference
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.uml2.uml.UMLPackage

/*
 * The code is incomplete because we should choose F1 rather than AUC
 * 
 * auc希望训练一个尽量不误报的模型，也就是知识外推的时候倾向保守估计，而f1希望训练一个不放过任何可能的模型，即知识外推的时候倾向激进，这就是这两个指标的核心区别。
 * 所以在实际中，选择这两个指标中的哪一个，取决于一个trade-off。如果我们犯检验误报错误的成本很高，那么我们选择auc是更合适的指标。如果我们犯有漏网之鱼错误的成本很高，那么我们倾向于选择f1score。
 */

/**
 * RQ what is the best threshold for the similarity that can determine whether two eobjects are dissimilar.
 */
class CalcROC {
	val ElementMutator mutator
	val DistanceFunction distance
	val EObjectSimHasher hasher
	val EObjectOneHotHasher onehotHasher;
	val mutationCount = 20
	val WeightProvider weight
	
	new(EClass type, DistanceFunction distance, EObjectSimHasher hasher) {
		mutator = new ElementMutator(type)
		this.distance = distance
		this.hasher = hasher
		this.onehotHasher = new EObjectOneHotHasher
		weight = WeightProviderDescriptorRegistryImpl.createStandaloneInstance().getHighestRankingWeightProvider(type.EPackage)
	}
	
	
	def prepare(EObject root) {
		prepare(Collections.singletonList(root))
	}
	def prepare(List<EObject> contents) {
		mutator.prepare(contents)
	}

	def estimateOne(EObject original, int catID) {
		hasher.reset
		onehotHasher.reset();
		mutator.doMutation(original)
		
		val mutant = mutator.getMutant(original)
		val allObjects = mutator.selectAll
		
		try {
			
			val m_belong = checkBelong(original, mutant)
			val simVector = new SimVector(original, mutant, catID)
			val mutantHash = hasher.hash(mutant)
			val mutantHashValue = new HashValue64(mutantHash)
			val ohMutantValue = onehotHasher.hash(mutant)
			
			for(var ci = 0; ci < allObjects.size; ci++) {
				val target = allObjects.get(ci)
				
				val belong = checkBelong(target, mutant)

				val targetHash = hasher.hash(target)
				val targetHashValue = new HashValue64(targetHash)
				
				val cossim = Hash64.cosineSimilarity(targetHashValue, mutantHashValue)
				val jacsim = Hash64.jaccardSimilarity(targetHashValue, mutantHashValue)
	
				val ohTargetValue = onehotHasher.hash(target)
				val ohsim = EObjectOneHotHasher.onehotSim(ohTargetValue, ohMutantValue);
				
				val tuple = new SimTuple(belong, cossim, jacsim, ohsim)
				simVector.sims += tuple
			}
			this.samples += simVector
			return !m_belong
		} catch (Exception exception) {
			System.err.println("error occurring")
			exception.printStackTrace
			return true
		}
	}
	
	protected def boolean checkBelong(EObject original, EObject mutant) {
		val mutantParent = mutant.eContainer
		val comparison = EcoreUtil.create(ComparePackage.eINSTANCE.comparison) as Comparison
		val originalParent = original.eContainer
		if(originalParent!==null) {
			val match = EcoreUtil.create(ComparePackage.eINSTANCE.match) as Match
			match.left = originalParent
			match.right = mutantParent
			comparison.matches.add(match)
		}
		
		val dist_thresh = Math.max((distance as EditionDistance).getThresholdAmount(original), (distance as EditionDistance).getThresholdAmount(mutant));
		val dist = distance.distance(comparison, original, mutant) // + (if(mutantParent!==mutator.getMutant(original)) weight.getParentWeight(mutant) else 0)
		val belong = dist<dist_thresh
		belong
	}
	
	def estimateAll() {
		val allObjects = mutator.selectAll
		var over = 0
		for (var o = 0; o < allObjects.size; o++ ) {
			val object = allObjects.get(o);
			for(var i =0; i< mutationCount;i++) {
				if(estimateOne(object, o)) over++
			}
		}
		if (over > mutationCount * allObjects.size * 0.5) {
			println("["+mutator.type.name+"] Terminate at " + mutator.featureChangeRate)
			return false
		} else return true
	}
	
	protected val samples = new ArrayList<SimVector>(8192);
	
	
	def estimateThreshold(double lb, double ub, double step) {
		var num = Math.round((ub - lb) / step) as int
		var thlist = newDoubleArrayOfSize(num+1)
		var th = lb
		for(var i = 0; i<=num; i++) {
			thlist.set(i,th)
			th += step
		}
		
		estimateClass(thlist)
	}
	
	def estimateClass(double[] thresholds) {
		val EstimationForClass result = new EstimationForClass(this.mutator.type)
		val catSize = mutator.selectAll.size
		
		result.methodResults += new EstimationForMethod('LSH_Cos', thresholds.length, catSize)
		result.methodResults += new EstimationForMethod('LSH_Jac', thresholds.length, catSize)
		result.methodResults += new EstimationForMethod('OneHot', thresholds.length, catSize)
		
		val selector1 = [SimTuple t| t.cosSim]
		val selector2 = [SimTuple t| t.jacSim]
		val selector3 = [SimTuple t| t.ohSim]
		val selectors = #[selector1, selector2, selector3]
		
		
		this.samples.forEach[s|
			// given a sample and a cat, if sample in cat and sample.sim > threshold => TP
			val actualCat = s.catID;
			
			result.methodResults.forEach[method, mid|
				s.sims.forEach[tuple, catID|
					val simSel = selectors.get(mid)
					val catSample = s.sims.get(catID)
					
					val sim = simSel.apply(catSample)
					
					for(var thid = 0; thid<thresholds.length; thid++) {
						val thResult = method.thResults.get(thid)
						val threshold = thresholds.get(thid)
						val cat = thResult.categories.get(catID)
						if(tuple.belongTo) {
							if(sim>=threshold) {
								cat.TP ++
							} else {
								cat.FN ++
							}
						} else {
							if(sim>=threshold) {
								cat.FP ++
							} else {
								cat.TN ++
							}
						}
					}
					
				]
			]
		]
		
		return result
	}
	
	def static void main(String[] args) {
		
		estimate(EcorePackage.eINSTANCE, EcorePackage.eINSTANCE, #{EcorePackage.eINSTANCE.EStringToStringMapEntry, EcorePackage.eINSTANCE.EObject, EcorePackage.eINSTANCE.EFactory, EcorePackage.eINSTANCE.EAnnotation, EcorePackage.eINSTANCE.EGenericType})
	}
	
	
	protected def static void estimate(EPackage metamodel, EObject model, Set<EClass> ignored) {
		val out = System.out; //new PrintStream(new FileOutputStream('/Users/hexiao/Projects/Java/git/mohash/edu.ustb.sei.mde.mohash.evaluation/output/sim.txt'))
		metamodel.EClassifiers.filter[it instanceof EClass].map[it as EClass].filter[!it.abstract && !ignored.contains(it)].toList.parallelStream.forEach[
			val factory = new MoHashMatchEngineFactory()
			factory.matchEngine
			synchronized(out) {				
				out.println("Test "+it.name)
			}
			
			val e = new  CalcROC(it, factory.distance, factory.hasher)
			e.prepare(model)
			if(!e.mutator.selectAll.isEmpty) {
				for(var mr = 0.1; mr > 0 && mr < 1; mr += 0.1) {
					e.mutator.featureChangeRate = mr
					if(!e.estimateAll()) mr = -1
				}
				
				synchronized(out) {				
					out.print(e.estimateThreshold(0.1, 0.8, 0.01))
					out.println
					out.println
				}
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

class SimVector {
	public val EObject original;
	public val EObject mutant;
	public val int catID;
	public val List<SimTuple> sims = new ArrayList
	
	new(EObject original, EObject mutant, int catID) {
		this.original = original
		this.mutant = mutant
		this.catID = catID
	}
}

class SimTuple {
	public val double cosSim
	public val double jacSim
	public val double ohSim
	public val boolean belongTo
	
	new(boolean belongTo, double cosSim, double jacSim, double ohSim) {
		this.belongTo = belongTo
		this.cosSim = cosSim
		this.jacSim = jacSim
		this.ohSim = ohSim
	}
}

class EstimationForClass {
	public val EClass clazz;
	public val List<EstimationForMethod> methodResults = new ArrayList
	
	new(EClass clazz) {
		this.clazz = clazz;
	}
	
	override toString() {
		'''
		«FOR m : methodResults»
		«m.getString(clazz.name)»
		«ENDFOR»
		'''
	}
	
}

class EstimationForMethod {
	public val String method;
	public val List<EstimationForTH> thResults
	
	new(String method, int thSize, int catSize) {
		this.method = method;
		thResults = new ArrayList
		for(var i=0;i<thSize;i++) {
			thResults += new EstimationForTH(i, catSize)
		}
	}
	
	def getString(String prefix) {
		'''
		«FOR r : thResults»
		[«prefix»]    [«method»]    «r.toString»
		«ENDFOR»
		'''
	}
	
}

class EstimationForTH {
	public val double threshold;
	new(double threshold, int catSize) {
		this.threshold = threshold;
		categories = new ArrayList
		for(var i=0;i<catSize;i++) {
			categories += new EstimationForCat(i)
		}
	}
	public val List<EstimationForCat> categories
	
	def double getTPR() {
		val sum = categories.map[it.TPR].reduce[p1, p2|p1+p2] ?: 0.0
		return sum / categories.size
	}
	
	def double getFPR() {
		val sum = categories.map[it.FPR].reduce[p1, p2|p1+p2] ?: 0.0
		return sum / categories.size
	}
	
	def double getPrecision() {
		val sum = categories.map[it.precision].reduce[p1, p2|p1+p2] ?: 0.0
		return sum / categories.size
	}
	
	def double getRecall() {
		TPR
	}
	
	override toString() {
		val tpr = TPR
		val fpr = FPR
		val prec = precision
		val recall = recall
		val f2 = 5 * prec * recall / (4*prec + recall)
		
		return String.format('threshold=%.4f    TPR=%.4f    FPR=%.4f    Prec=%.4f    Recall=%.4f    F1=%.4f', threshold, tpr, fpr, prec, recall, f2)
	}
}

class EstimationForCat {
	public val int id;
	
	new(int id) {
		this.id = id
	}
	
	public var int TP = 0;
	public var int FP = 0;
	public var int TN = 0;
	public var int FN = 0;
	
	def double getTPR() {
		(TP as double) / (TP + FN)
	}
	
	def double getFPR() {
		(FP as double) / (TN + FP)
	}
	
	def double getPrecision() {
		(TP as double) / (TP + FP)
	}
	
	def double getRecall() {
		TPR
	}
	
	
}