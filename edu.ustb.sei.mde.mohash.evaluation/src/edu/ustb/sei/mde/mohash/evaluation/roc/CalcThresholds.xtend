package edu.ustb.sei.mde.mohash.evaluation.roc

import edu.ustb.sei.mde.mohash.EObjectSimHasher
import edu.ustb.sei.mde.mohash.HashValue64
import edu.ustb.sei.mde.mohash.TypeMap
import edu.ustb.sei.mde.mohash.emfcompare.MoHashMatchEngineFactory
import edu.ustb.sei.mde.mohash.functions.Hash64
import edu.ustb.sei.mde.mohash.functions.URIComputer
import edu.ustb.sei.mde.mohash.onehot.EObjectOneHotHasher
import edu.ustb.sei.mde.mumodel.ElementMutator
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.util.ArrayList
import java.util.Collections
import java.util.List
import java.util.Set
import java.util.function.Consumer
import org.eclipse.emf.common.util.URI
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
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl
import org.eclipse.uml2.uml.UMLPackage
import java.util.HashMap
//import org.eclipse.xtext.xbase.XbasePackage
import org.eclipse.emf.mapping.ecore2xml.Ecore2XMLPackage
import org.eclipse.uml2.uml.internal.resource.UML212UMLResourceFactoryImpl
import org.eclipse.uml2.uml.internal.resource.UMLResourceFactoryImpl
import org.eclipse.uml2.uml.resource.UMLResource
import org.eclipse.uml2.uml.resource.XMI2UMLResource

/*
 * 
 * auc希望训练一个尽量不误报的模型，也就是知识外推的时候倾向保守估计，而f1希望训练一个不放过任何可能的模型，即知识外推的时候倾向激进，这就是这两个指标的核心区别。
 * 所以在实际中，选择这两个指标中的哪一个，取决于一个trade-off。如果我们犯检验误报错误的成本很高，那么我们选择auc是更合适的指标。如果我们犯有漏网之鱼错误的成本很高，那么我们倾向于选择f1score。
 */

/**
 * RQ what is the best threshold for the similarity that can determine whether two eobjects are dissimilar.
 */
class CalcThresholds {
	val ElementMutator mutator
	val DistanceFunction distance
	val EObjectSimHasher hasher
	val EObjectOneHotHasher onehotHasher;
	val mutationCount = 50
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

	def estimateOne(EObject original) {
		hasher.reset
		onehotHasher.reset();
		mutator.doMutation(original)
		
		val mutant = mutator.getMutant(original)
		val allObjects = mutator.selectAll
		
		try {
			
			val m_belong = checkBelong(original, mutant)
			val simVector = new SimVector(original, mutant)
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
				val sorsim = Hash64.sorensenSimilarity(targetHashValue, mutantHashValue)
	
				val ohTargetValue = onehotHasher.hash(target)
				val ohsim = EObjectOneHotHasher.onehotSim(ohTargetValue, ohMutantValue);
				
				val tuple = new SimTuple(belong, cossim, jacsim, sorsim, ohsim)
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
	
	def estimateAll(boolean some) {
		val allObjects = if(!some) mutator.selectAll else (if(mutator.selectAll.size>50) mutator.select(mutator.selectAll.size / 2) else mutator.selectAll)
		
		var over = 0
		for (object :allObjects) {
			for(var i =0; i< mutationCount;i++) {
				if(estimateOne(object)) over++
			}
		}
		if (over > mutationCount * allObjects.size * 0.75) {
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
		
		result.methodResults += new EstimationForMethod('LSH_Cos', thresholds, catSize)
		result.methodResults += new EstimationForMethod('LSH_Jac', thresholds, catSize)
		result.methodResults += new EstimationForMethod('LSH_Sor', thresholds, catSize)
		result.methodResults += new EstimationForMethod('OneHot', thresholds, catSize)
		
		val selector1 = [SimTuple t| t.cosSim]
		val selector2 = [SimTuple t| t.jacSim]
		val selector3 = [SimTuple t| t.sorSim]
		val selector4 = [SimTuple t| t.ohSim]
		val selectors = #[selector1, selector2, selector3, selector4]
		
		
		this.samples.forEach[s|
			// given a sample and a cat, if sample in cat and sample.sim > threshold => TP
//			val actualCat = s.catID;
			
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
//		val uris = #[
//			URI.createPlatformPluginURI('/org.eclipse.emf.ecore.xcore/model/Xcore.ecore', true),
//			URI.createPlatformPluginURI('/org.eclipse.emf.ecore/model/Ecore.ecore', true),
//			URI.createPlatformPluginURI('/org.eclipse.xtext.xbase/model/Xbase.ecore', true)
//		]
//		estimate(uris, EcorePackage.eINSTANCE, "ecore", new File('/Users/hexiao/Projects/Java/git/mohash/edu.ustb.sei.mde.mohash.evaluation/output/roc'), 
//			#{EcorePackage.eINSTANCE.EStringToStringMapEntry, EcorePackage.eINSTANCE.EObject, EcorePackage.eINSTANCE.EFactory, EcorePackage.eINSTANCE.EAnnotation, EcorePackage.eINSTANCE.EGenericType}
//		)
		val uris = new File('/Users/hexiao/Projects/Java/git/mohash/edu.ustb.sei.mde.mohash.evaluation/modeldata/uml/data_small_30/').listFiles.filter[it.name.endsWith('.xmi')].map[URI.createFileURI(it.absolutePath)].toList

//		val uri = URI.createFileURI('/Users/hexiao/Projects/Java/git/mohash/edu.ustb.sei.mde.mohash.evaluation/modeldata/uml/data_big_30/10005_kwfbUD9QEemphNojEI-2Ug.xmi')
		
		estimate(uris, UMLPackage.eINSTANCE, "xmi", new File('/Users/hexiao/Projects/Java/git/mohash/edu.ustb.sei.mde.mohash.evaluation/modeldata/uml'), #{}, true)
//		estimate(System.out, EcorePackage.eINSTANCE, EcorePackage.eINSTANCE, #{EcorePackage.eINSTANCE.EStringToStringMapEntry, EcorePackage.eINSTANCE.EObject, EcorePackage.eINSTANCE.EFactory, EcorePackage.eINSTANCE.EAnnotation, EcorePackage.eINSTANCE.EGenericType})
	}
	
	protected def static void estimate(List<URI> uris, EPackage metamodel, String extFile, File outputFolder , Set<EClass> ignored, boolean uml) {
		val resourceSet = new ResourceSetImpl();
		if(uml) {
			resourceSet.packageRegistry.put(UMLPackage.eINSTANCE.nsURI, UMLPackage.eINSTANCE)
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(XMI2UMLResource.FILE_EXTENSION, XMI2UMLResource.Factory.INSTANCE) 
		} else {
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(extFile, new XMIResourceFactoryImpl());
			resourceSet.packageRegistry.put(metamodel.nsURI, metamodel)
		}
		
		uris.forEach[uri|
			val resource = resourceSet.getResource(uri, true)
			val filename = uri.trimFileExtension.lastSegment + ".log"
			val outfile = new File(outputFolder, filename)
			
			val out = new PrintStream(new BufferedOutputStream(new FileOutputStream(outfile)))
			estimate(out, metamodel, resource.contents, ignored)
			out.flush
			out.close
		]
	}
	
	protected def static void estimate(File inputFolder, EPackage metamodel, String extFile, File outputFolder , Set<EClass> ignored, boolean uml) {
		val uris = inputFolder.listFiles.filter[it.isFile && it.absolutePath.endsWith(extFile)].map[URI.createFileURI(it.absolutePath)].toList
		estimate(uris, metamodel, extFile, outputFolder, ignored, uml)
	}
	
	protected def static void estimate(PrintStream out, EPackage metamodel, EObject model, Set<EClass> ignored) {
		estimate(out,metamodel, Collections.singletonList(model), ignored)
	}
	
	protected def static void estimate(PrintStream out, EPackage metamodel, List<EObject> model, Set<EClass> ignored) {
		val clazzList = metamodel.EClassifiers.filter[it instanceof EClass].map[it as EClass].filter[!it.abstract && !ignored.contains(it)].toList
		val resultMap = new HashMap<EClass, EstimationForClass>
		clazzList.parallelStream.forEach[
			val factory = new MoHashMatchEngineFactory()
			factory.matchEngine
			synchronized(out) {				
				println("Test "+it.name)
			}
			
			val e = new  CalcThresholds(it, factory.distance, factory.hasher)
			e.prepare(model)
			if(!e.mutator.selectAll.isEmpty) {
				for(var mr = 0.1; mr > 0 && mr < 1; mr += 0.1) {
					e.mutator.featureChangeRate = mr
					if(!e.estimateAll(false)) mr = -1
				}
				
				val result = e.estimateThreshold(0.1, 0.8, 0.01)
				resultMap.put(it, result)
				
				synchronized(out) {				
					out.print(result)
					out.println("====================================================")
					out.println
					out.println
				}
			}
		]
		
		
		out.println("====================================================")
		out.println("=====================[SUMMARY]======================")
		out.println("====================================================")
		clazzList.forEach[clazz|
			val result = resultMap.get(clazz)
			if(result!==null) {
				result.methodResults.forEach[m|
					out.println('''[«clazz.name»]    [«m.method»]    «m.best»''')
				]
			}
		]
		out.println
		out.println
		out.println("====================================================")
		out.println("======================[CONFIG]======================")
		out.println("====================================================")
		clazzList.forEach[clazz|
			val result = resultMap.get(clazz)
			if(result!==null) {
				result.methodResults.filter[it.method=='LSH_Cos'].forEach[m|
					out.println('''thresholds.put(«clazz.EPackage.name.toFirstUpper»Package.eINSTANCE.get«clazz.name»(), «if(m.best===null) -1 else m.best.threshold»);''')
				]
			}
		]
	}
}

class SimVector {
	public val EObject original;
	public val EObject mutant;
//	public val int catID;
	public val List<SimTuple> sims = new ArrayList
	
	new(EObject original, EObject mutant) {
		this.original = original
		this.mutant = mutant
//		this.catID = catID
	}
}

class SimTuple {
	public val double cosSim
	public val double jacSim
	public val double sorSim
	public val double ohSim
	public val boolean belongTo
	
	new(boolean belongTo, double cosSim, double jacSim, double sorSim, double ohSim) {
		this.belongTo = belongTo
		this.cosSim = cosSim
		this.jacSim = jacSim
		this.sorSim = sorSim
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
	
	new(String method, double[] thlist, int catSize) {
		this.method = method;
		thResults = new ArrayList
		for(var i=0;i<thlist.length;i++) {
			thResults += new EstimationForTH(thlist.get(i), catSize)
		}
	}
	
	def getString(String prefix) {
		'''
		«FOR r : thResults»
		[«prefix»]    [«method»]    «r.toString»
		«ENDFOR»
		'''
	}
	
	def getBest() {
		var f = -1.0
		var EstimationForTH th = null
		for(t : thResults) {
			val f2 = t.Fscore(2)
			if(f2 > f) {
				f = f2
				th = t
			}
		}
		
		return th
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
	
	var double cachedTPR = -1
	var double cachedFPR = -1
	var double cachedPrecision = -1
	var double cachedAcc = -1
	
	def double getTPR() {
		if(cachedTPR===-1) {
			cachedTPR = {
				val sum = categories.map[it.TPR].reduce[p1, p2|p1+p2] ?: 0.0
				sum / categories.size
			}
		}
		return cachedTPR
	}
	
	def double getFPR() {
		if(cachedFPR===-1) {
			cachedFPR = {
				val sum = categories.map[it.FPR].reduce[p1, p2|p1+p2] ?: 0.0
				sum / categories.size
			}
		}
		return cachedFPR
	}
	
	def double getPrecision() {
		if(cachedPrecision===-1) {
			cachedPrecision = {
				val sum = categories.map[it.precision].reduce[p1, p2|p1+p2] ?: 0.0
				sum / categories.size
			}
		}
		return cachedPrecision
	}
	
	def double getRecall() {
		TPR
	}
	
	def double Fscore(double beta) {
		(1+beta*beta) * precision * recall / (beta * beta * precision + recall)
	}
	
	def double getAcc() {
		if(cachedAcc===-1) {
			cachedAcc = {
				val sum = categories.map[it.accuracy].reduce[p1, p2|p1+p2] ?: 0.0
				sum / categories.size
			}
		}
		return cachedAcc
	}
	
	override toString() {
		val tpr = TPR
		val fpr = FPR
		val prec = precision
		val recall = recall
		val acc = acc
		val f2 = Fscore(2)
		
		return String.format('threshold=%.4f    TPR=%.4f    FPR=%.4f    Prec=%.4f    Recall=%.4f    Acc=%.4f    F2=%.4f', threshold, tpr, fpr, prec, recall, acc, f2)
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
		val r = (TP as double) / (TP + FN)
		if(Double.isNaN(r)) 0
		else r
	}
	
	def double getFPR() {
		val r = (FP as double) / (TN + FP)
		if(Double.isNaN(r)) 0
		else r
	}
	
	def double getPrecision() {
		val r = (TP as double) / (TP + FP)
		if(Double.isNaN(r)) 0
		else r
	}
	
	def double getRecall() {
		TPR
	}
	
	def double getAccuracy() {
		(TP+TN) as double  / (TP+TN+FP+FN)
	}
	
	
}