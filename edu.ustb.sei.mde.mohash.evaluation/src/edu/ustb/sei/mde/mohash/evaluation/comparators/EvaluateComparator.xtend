package edu.ustb.sei.mde.mohash.evaluation.comparators

import edu.ustb.sei.mde.mohash.EObjectSimHasher
import edu.ustb.sei.mde.mohash.HashValue64
import edu.ustb.sei.mde.mohash.ObjectIndex
import edu.ustb.sei.mde.mohash.TypeMap
import edu.ustb.sei.mde.mohash.emfcompare.HashBasedEObjectIndex
import edu.ustb.sei.mde.mohash.emfcompare.MoHashMatchEngineFactory
import edu.ustb.sei.mde.mohash.functions.Hash64
import edu.ustb.sei.mde.mumodel.ModelMutator
import edu.ustb.sei.mde.mumodel.ResourceSetExtension
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.util.Collections
import java.util.HashSet
import java.util.List
import java.util.Set
import org.eclipse.emf.common.util.BasicMonitor
import org.eclipse.emf.compare.Comparison
import org.eclipse.emf.compare.match.IMatchEngine
import org.eclipse.emf.compare.match.eobject.EcoreWeightProvider
import org.eclipse.emf.compare.match.eobject.WeightProvider
import org.eclipse.emf.compare.scope.DefaultComparisonScope
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.util.EcoreUtil
//import org.eclipse.emf.ecore.xcore.XcorePackage
import org.eclipse.emf.mapping.ecore2xml.Ecore2XMLPackage
import org.eclipse.uml2.uml.UMLPackage
import org.eclipse.xtext.XtextPackage
//import org.eclipse.xtext.xbase.XbasePackage

import static edu.ustb.sei.mde.mohash.emfcompare.HashBasedEObjectIndex.*
import java.util.regex.Pattern
import org.eclipse.uml2.uml.resource.UMLResource
import org.eclipse.emf.common.util.URI

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
	
	static val needDump = false
	static val dumpDir = 'C:/JavaProjects/git/mohash/edu.ustb.sei.mde.mohash.evaluation/output/dump';
	static var dumpcount = 0;
	
	private def void dump(ComparisonResult result) {
		val dir = new File(dumpDir);
		dir.mkdirs
		
		val oRes = createResource(dumpDir+'/original_'+dumpcount+'.xmi')
		oRes.contents+=original.contents
		
		val mRes = createResource(dumpDir+'/mutant_'+dumpcount+'.xmi')
		mRes.contents+=mutant.contents
		
		val emfcRes = createResource(dumpDir+'/emfc_'+dumpcount+'.xmi')
		emfcRes.contents += result.emfcomp
		
		val oursRes = createResource(dumpDir+'/ours_'+dumpcount+'.xmi')
		oursRes.contents += result.mohash
		
		try {			
			oRes.save(null);
			mRes.save(null);
			emfcRes.save(null);
			oursRes.save(null);
		} catch(Exception e) {}
		
		dumpcount ++;
	}
	
	def void checkComparisonResults(Resource original, ComparisonResult result, Set<EClass> neglectedTypes) {
		var total = 0
		var diffs = 0
		
		val iter = original.allContents
		while (iter.hasNext) {
			val next = iter.next
			if(neglectedTypes.contains(next.eClass)===false) {
				total++
				diffs += checkMatch(next, result)
			}
		}
		
		result.total = total
		result.diffs = diffs
		
		if(diffs!==0 && needDump) dump(result)
	}
	
	static extension ResourceSetExtension = new ResourceSetExtension
	
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
	
	def long usedMemory() {
//		try { Thread.sleep(1000) } catch (Exception e) {}
		val r = Runtime.runtime;
		return r.totalMemory - r.freeMemory;
	}
	
	def ComparisonResult evaluate(List<EClass> ignoredClasses, List<EStructuralFeature> ignoredFeatures, Set<EClass> neglectedTypes) {
		val mutator = new ModelMutator
		mutator.ignoredClasses = ignoredClasses
		mutator.ignoredFeatures = ignoredFeatures
		
		mutant.contents.clear
		val result = new ComparisonResult
		
		
		result.numOfEdits = mutator.mutateResource(original, mutant)
		
		evaluateEMFC(result)
		
		evaluateMohash(result)
		
		checkComparisonResults(original, result, neglectedTypes)
		
		result
	}
	
	protected def Long evaluateMohash(ComparisonResult result) {
//		try {
//			System.gc();
////			Thread.sleep(3000)
//		} catch(Exception e) {}
		val mohashScope = new DefaultComparisonScope(original, mutant, null)
		val mohashMemB = usedMemory
		val mohashStart = System.nanoTime
		result.mohash = mohash.match(mohashScope, new BasicMonitor)
		val mohashEnd = System.nanoTime
		val mohashMemE = usedMemory
		result.mohashTime = (mohashEnd - mohashStart)
		result.mohashMem = (mohashMemE - mohashMemB)
	}
	
	protected def Long evaluateEMFC(ComparisonResult result) {
//		try {
//			System.gc();
////			Thread.sleep(3000)
//		} catch(Exception e) {}
		val emfcScope = new DefaultComparisonScope(original, mutant, null)
		val emfcMemB = usedMemory
		val emfcStart = System.nanoTime
		result.emfcomp = emfcomp.match(emfcScope, new BasicMonitor)
		val emfcEnd = System.nanoTime
		val emfcMemE = usedMemory
		result.emfcompTime = (emfcEnd - emfcStart)
		result.emfcompMem = (emfcMemE - emfcMemB)
	}
	
	def static void main(String[] args) {
		val modelFolder = new File('C:/JavaProjects/git/mohash/edu.ustb.sei.mde.mohash.evaluation/model/uml/data_big_30')
		val models = modelFolder.listFiles.filter[it.name.endsWith(".xmi")].map[
			// for ecore
//			it.absolutePath.loadResource.contents.get(0) as EPackage
			// for uml
			it.absolutePath.loadUMLResource
		].toList
		
		
		
//		evaluateEcoreModels(models, 10, new File('C:/JavaProjects/git/mohash/edu.ustb.sei.mde.mohash.evaluation/output/big'))
		evaluateUMLModels(models, 10, new File('C:/JavaProjects/git/mohash/edu.ustb.sei.mde.mohash.evaluation/output/big'))
	}
	
	
	protected def static void evaluateEcoreModels(List<EPackage> models, int count, File outputFolder) {
		outputFolder.mkdirs
		models.forEach[model|
			System.gc
			model.evaluateEcoreModel(count, outputFolder)
		]
	}
	protected def static void evaluateUMLModels(List<Resource> models, int count, File outputFolder) {
		outputFolder.mkdirs
		models.forEach[model|
			System.gc
			model.evaluateUMLModel(count, outputFolder)
		]
	}
	
	protected def static getLogFileName(Object object) {
		val resource = if(object instanceof Resource) object as Resource else (object as EObject).eResource;
		val name = if(resource!==null) {
			val pattern = Pattern.compile('([0-9]+).+')
			val matcher = pattern.matcher(resource.URI.lastSegment)
			if(matcher.matches) {
				(if(object instanceof Resource) 'UML@' else 'Ecore@') + matcher.group(1)
			} else {
				resource.URI.trimFileExtension.lastSegment
			}
		} else {
			(object as EPackage).name
		}
		
		name
	}
	
	protected def static void evaluateEcoreModel(EPackage model, int count, File outputFolder) {
		val file = new File(outputFolder, model.getLogFileName+'.log')
		val out = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)))
		try { evaluateEcoreModel(model, count, out) } catch(Exception e) {
			e.printStackTrace
		}
		out.flush
		out.close
	}
	
	protected def static void evaluateUMLModel(Resource umlModel, int count, File outputFolder) {
//		val modelName = umlModel.URI.trimFileExtension.lastSegment
		val file = new File(outputFolder, umlModel.getLogFileName+'.log')
		val out = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)))
		try { evaluateUMLModel(umlModel, count, out) } catch(Exception e) {
			e.printStackTrace
		}
		out.flush
		out.close
	}
	
	protected def static void evaluateUMLModel(Resource umlModel, int count, PrintStream out) {
		val factory = new MoHashMatchEngineFactory
		val weight = factory.weightProviderRegistry
		
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
		
		// for UML ...
//		thresholds.put(UMLPackage.eINSTANCE.activity, 0.60);
//		thresholds.put(UMLPackage.eINSTANCE.association, 0.62);
//		thresholds.put(UMLPackage.eINSTANCE.actor, 0.55);
//		thresholds.put(UMLPackage.eINSTANCE.behaviorExecutionSpecification, 0.65);
//		thresholds.put(UMLPackage.eINSTANCE.class_, 0.60);
//		thresholds.put(UMLPackage.eINSTANCE.collaboration, 0.60);
//		thresholds.put(UMLPackage.eINSTANCE.component, 0.50);
//		thresholds.put(UMLPackage.eINSTANCE.dataType, 0.55);
//		thresholds.put(UMLPackage.eINSTANCE.dependency, 0.60);
//		thresholds.put(UMLPackage.eINSTANCE.elementImport, 0.65);
//		thresholds.put(UMLPackage.eINSTANCE.enumeration, 0.55);
//		thresholds.put(UMLPackage.eINSTANCE.enumerationLiteral, 0.35);
//		thresholds.put(UMLPackage.eINSTANCE.executionOccurrenceSpecification, 0.65);
//		thresholds.put(UMLPackage.eINSTANCE.generalOrdering, 0.68);
//		thresholds.put(UMLPackage.eINSTANCE.interaction, 0.60);
//		thresholds.put(UMLPackage.eINSTANCE.instanceSpecification, 0.35);
//		thresholds.put(UMLPackage.eINSTANCE.lifeline, 0.45);
//		thresholds.put(UMLPackage.eINSTANCE.literalInteger, 0.50);
//		thresholds.put(UMLPackage.eINSTANCE.message, 0.55);
//		thresholds.put(UMLPackage.eINSTANCE.messageOccurrenceSpecification, 0.70);
//		thresholds.put(UMLPackage.eINSTANCE.model, 0.40);
//		thresholds.put(UMLPackage.eINSTANCE.occurrenceSpecification, 0.70);
//		thresholds.put(UMLPackage.eINSTANCE.operation, 0.55);
//		thresholds.put(UMLPackage.eINSTANCE.package, 0.50);
//		thresholds.put(UMLPackage.eINSTANCE.parameter, 0.65);
//		thresholds.put(UMLPackage.eINSTANCE.property, 0.65);
//		thresholds.put(UMLPackage.eINSTANCE.stateMachine, 0.55);
//		thresholds.put(UMLPackage.eINSTANCE.useCase, 0.60);
//		thresholds.put(UMLPackage.eINSTANCE.usage, 0.45);
//		thresholds.put(UMLPackage.eINSTANCE.literalUnlimitedNatural, 0.45);
		
		thresholds.put(UMLPackage.eINSTANCE.activity, 0.73);
		thresholds.put(UMLPackage.eINSTANCE.association, 0.78);
		thresholds.put(UMLPackage.eINSTANCE.actor, 0.65);
		thresholds.put(UMLPackage.eINSTANCE.behaviorExecutionSpecification, 0.76);
		thresholds.put(UMLPackage.eINSTANCE.class_, 0.67);
		thresholds.put(UMLPackage.eINSTANCE.collaboration, 0.67);
		thresholds.put(UMLPackage.eINSTANCE.component, 0.69);
		thresholds.put(UMLPackage.eINSTANCE.dataType, 0.58);
		thresholds.put(UMLPackage.eINSTANCE.dependency, 0.58);
		thresholds.put(UMLPackage.eINSTANCE.elementImport, 0.65);
		thresholds.put(UMLPackage.eINSTANCE.enumeration, 0.59);
		thresholds.put(UMLPackage.eINSTANCE.enumerationLiteral, 0.51);
		thresholds.put(UMLPackage.eINSTANCE.executionOccurrenceSpecification, 0.73);
		thresholds.put(UMLPackage.eINSTANCE.generalOrdering, 0.78);
		thresholds.put(UMLPackage.eINSTANCE.interaction, 0.71);
		thresholds.put(UMLPackage.eINSTANCE.instanceSpecification, 0.5);
		thresholds.put(UMLPackage.eINSTANCE.lifeline, 0.63);
		thresholds.put(UMLPackage.eINSTANCE.message, 0.64);
		thresholds.put(UMLPackage.eINSTANCE.messageOccurrenceSpecification, 0.73);
		thresholds.put(UMLPackage.eINSTANCE.model, 0.46);
		thresholds.put(UMLPackage.eINSTANCE.occurrenceSpecification, 0.72);
		thresholds.put(UMLPackage.eINSTANCE.operation, 0.68);
		thresholds.put(UMLPackage.eINSTANCE.package, 0.42);
		thresholds.put(UMLPackage.eINSTANCE.parameter, 0.65);
		thresholds.put(UMLPackage.eINSTANCE.property, 0.77);
		thresholds.put(UMLPackage.eINSTANCE.stateMachine, 0.55);
		thresholds.put(UMLPackage.eINSTANCE.useCase, 0.72);
		thresholds.put(UMLPackage.eINSTANCE.usage, 0.61);
		thresholds.put(UMLPackage.eINSTANCE.abstraction, 0.5);
		thresholds.put(UMLPackage.eINSTANCE.activityFinalNode, 0.69);
		thresholds.put(UMLPackage.eINSTANCE.decisionNode, 0.77);
		thresholds.put(UMLPackage.eINSTANCE.flowFinalNode, 0.67);
		thresholds.put(UMLPackage.eINSTANCE.forkNode, 0.69);
		thresholds.put(UMLPackage.eINSTANCE.joinNode, 0.72);
		thresholds.put(UMLPackage.eINSTANCE.include, 0.74);
		thresholds.put(UMLPackage.eINSTANCE.initialNode, 0.65);
		thresholds.put(UMLPackage.eINSTANCE.interface, 0.62);
		
		factory.setThresholdMap(thresholds)
		
		val nohashTypes = #{
			EcorePackage.eINSTANCE.EAnnotation, EcorePackage.eINSTANCE.EGenericType, EcorePackage.eINSTANCE.EFactory, 
			EcorePackage.eINSTANCE.EStringToStringMapEntry,
			UMLPackage.eINSTANCE.packageImport, UMLPackage.eINSTANCE.literalInteger, UMLPackage.eINSTANCE.literalBoolean, UMLPackage.eINSTANCE.literalUnlimitedNatural
		}
		
		factory.ignoredClasses = nohashTypes
		
		var mohashTotal = 0L;
		var emfcomTotal = 0L;
		var critialMissTotal = 0L;
		var missTotal = 0L;
		var allTotal = 0L
		
		for(var i=0; i<2; i++) {
			val evaluator = new EvaluateComparator(umlModel.contents,  factory)
			evaluator.evaluate(#[EcorePackage.eINSTANCE.EAnnotation, EcorePackage.eINSTANCE.EGenericType, EcorePackage.eINSTANCE.EFactory, EcorePackage.eINSTANCE.EStringToStringMapEntry,
				UMLPackage.eINSTANCE.literalInteger, UMLPackage.eINSTANCE.literalBoolean, UMLPackage.eINSTANCE.literalUnlimitedNatural
			], #[], #{})
		}
		
		val modelName = umlModel.URI.trimFileExtension.lastSegment
		
		for(var i=0; i<count; i++) {
			val evaluator = new EvaluateComparator(umlModel.contents,  factory)
			val result = evaluator.evaluate(#[EcorePackage.eINSTANCE.EAnnotation, EcorePackage.eINSTANCE.EGenericType, EcorePackage.eINSTANCE.EFactory, EcorePackage.eINSTANCE.EStringToStringMapEntry,
				UMLPackage.eINSTANCE.literalInteger, UMLPackage.eINSTANCE.literalBoolean, UMLPackage.eINSTANCE.literalUnlimitedNatural
			], #[], #{EcorePackage.eINSTANCE.EGenericType, EcorePackage.eINSTANCE.EFactory, EcorePackage.eINSTANCE.EStringToStringMapEntry,
				UMLPackage.eINSTANCE.literalInteger, UMLPackage.eINSTANCE.literalBoolean, UMLPackage.eINSTANCE.literalUnlimitedNatural})
			val hasher = factory.hasher
			result.print(modelName, out, hasher, weight)
			mohashTotal += result.mohashTime
			emfcomTotal += result.emfcompTime
			critialMissTotal += result.critialMisses
			missTotal += result.diffs
			allTotal += result.total
		}
		
		out.println("==================================================")
		out.println("====================[SUMMARY]=====================")
		out.println("==================================================")
		out.println('''Time:    Avg(MoHash)=«(mohashTotal as double)/count/1000000.0»    Avg(EMFComp)=«(emfcomTotal as double)/count/1000000.0»''')
		out.println(String.format('Diff Rate:    Total=%.4f%%    Critical=%.4f%%', (missTotal*100 as double)/allTotal, (critialMissTotal*100 as double)/allTotal))
	}
	
	protected def static void evaluateEcoreModel(EPackage model, int count, PrintStream out) {
		val factory = new MoHashMatchEngineFactory
		val weight = factory.weightProviderRegistry
		
		val thresholds = new TypeMap<Double>(0.5)
//		thresholds.put(EcorePackage.eINSTANCE.EPackage, 0.15)
//		thresholds.put(EcorePackage.eINSTANCE.EClass, 0.55)
//		thresholds.put(EcorePackage.eINSTANCE.EReference, 0.65)
//		thresholds.put(EcorePackage.eINSTANCE.EOperation, 0.55)
//		thresholds.put(EcorePackage.eINSTANCE.EAttribute, 0.68)
//		thresholds.put(EcorePackage.eINSTANCE.EStringToStringMapEntry, 0.4)
//		thresholds.put(EcorePackage.eINSTANCE.EEnum, 0.5)
//		thresholds.put(EcorePackage.eINSTANCE.EEnumLiteral, 0.45)
//		thresholds.put(EcorePackage.eINSTANCE.EParameter, 0.5)

		thresholds.put(EcorePackage.eINSTANCE.EPackage, 0.42)
		thresholds.put(EcorePackage.eINSTANCE.EClass, 0.6)
		thresholds.put(EcorePackage.eINSTANCE.EReference, 0.75)
		thresholds.put(EcorePackage.eINSTANCE.EOperation, 0.61)
		thresholds.put(EcorePackage.eINSTANCE.EAttribute, 0.72)
		thresholds.put(EcorePackage.eINSTANCE.EStringToStringMapEntry, 0.4)
		thresholds.put(EcorePackage.eINSTANCE.EEnum, 0.5)
		thresholds.put(EcorePackage.eINSTANCE.EEnumLiteral, 0.54)
		thresholds.put(EcorePackage.eINSTANCE.EParameter, 0.66)
		
		factory.setThresholdMap(thresholds)
		
		val nohashTypes = newHashSet(EcorePackage.eINSTANCE.EAnnotation, EcorePackage.eINSTANCE.EGenericType, EcorePackage.eINSTANCE.EFactory, EcorePackage.eINSTANCE.EStringToStringMapEntry);
		
		factory.ignoredClasses = nohashTypes
		
		var mohashTotal = 0L;
		var emfcomTotal = 0L;
		var critialMissTotal = 0L;
		var missTotal = 0L;
		var allTotal = 0L
		
		for(var i=0; i<2; i++) {
			val evaluator = new EvaluateComparator(model,  factory)
			evaluator.evaluate(#[EcorePackage.eINSTANCE.EAnnotation, EcorePackage.eINSTANCE.EGenericType, EcorePackage.eINSTANCE.EFactory, EcorePackage.eINSTANCE.EStringToStringMapEntry], #[], #{})
		}
		
		for(var i=0; i<count; i++) {
			val evaluator = new EvaluateComparator(model,  factory)
			val result = evaluator.evaluate(#[EcorePackage.eINSTANCE.EAnnotation, EcorePackage.eINSTANCE.EGenericType, EcorePackage.eINSTANCE.EFactory, EcorePackage.eINSTANCE.EStringToStringMapEntry], #[], #{EcorePackage.eINSTANCE.EGenericType, EcorePackage.eINSTANCE.EFactory, EcorePackage.eINSTANCE.EStringToStringMapEntry})
			val hasher = factory.hasher
			result.print(model.name, out, hasher, weight)
			mohashTotal += result.mohashTime
			emfcomTotal += result.emfcompTime
			critialMissTotal += result.critialMisses
			missTotal += result.diffs
			allTotal += result.total
		}
		
		out.println("==================================================")
		out.println("====================[SUMMARY]=====================")
		out.println("==================================================")
		out.println('''Time:    Avg(MoHash)=«(mohashTotal as double)/count/1000000.0»    Avg(EMFComp)=«(emfcomTotal as double)/count/1000000.0»''')
		out.println(String.format('Diff Rate:    Total=%.4f%%    Critical=%.4f%%', (missTotal*100 as double)/allTotal, (critialMissTotal*100 as double)/allTotal))
	}
}

class ComparisonResult {
	public var Comparison mohash
	public var Comparison emfcomp
	
	public var long mohashMem;
	public var long emfcompMem; 
	
	public var long mohashTime
	public var long emfcompTime
	
	public var int diffs
	public var int total
	public var int numOfEdits
	
	public var int critialMisses = 0
	
	override toString() {
		return '''
		ComparisonResult [numOfEdits=«numOfEdits», mohashTime=«mohashTime», mohashMem=«mohashMem», emfcompTime=«emfcompTime», emfcompMem=«emfcompMem», diffs=«diffs», total=«total»]
		''' 
	}
	
	
	val Set<EObject> differences = new HashSet
	
	def void recordDiff(EObject source) {
		differences += source
	}
	
	def void print(String header, PrintStream out, EObjectSimHasher hasher, WeightProvider.Descriptor.Registry weight) {
		out.print('''«header» [numOfEdits=«numOfEdits», mohashTime=«mohashTime», mohashMem=«mohashMem», emfcompTime=«emfcompTime», emfcompMem=«emfcompMem», diffs=«diffs», total=«total», ''')
		
		val critical = new HashSet<EObject>()
		for(o : differences) {
//			val m1 = mohash.getMatch(o)
//			val m2 = emfcomp.getMatch(o)
//			
//			val r1 = m1.right
//			val r2 = m2.right
//			val h0 = hasher.hash(o)
//			val h1 = r1!==null ? hasher.hash(r1) : 0
//			val h2 = r2!==null ? hasher.hash(r2) : 0
			
			val pw = weight.getHighestRankingWeightProvider(o.eClass.EPackage).getParentWeight(o);
			
			if(pw<=EcoreWeightProvider.SIGNIFICANT) {
				critical += o
			} else {
				if(!differences.contains(o.eContainer)) {
					critical += o
				}
			}
		}
		
		critialMisses = critical.size
		
		out.println('''criticalMiss=«critical.size»]''')
		
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