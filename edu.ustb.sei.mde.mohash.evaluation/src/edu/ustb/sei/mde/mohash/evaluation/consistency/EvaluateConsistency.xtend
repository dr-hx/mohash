package edu.ustb.sei.mde.mohash.evaluation.consistency

import org.eclipse.emf.compare.match.eobject.ProximityEObjectMatcher.DistanceFunction
import edu.ustb.sei.mde.mohash.EObjectSimHasher
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EObject
import java.util.List
import java.util.Collections
import java.util.ArrayList
import java.io.PrintStream
import edu.ustb.sei.mde.mohash.HashValue64
import org.eclipse.emf.compare.Comparison
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.emf.compare.ComparePackage
import org.eclipse.emf.compare.Match
import edu.ustb.sei.mde.mohash.functions.Hash64
import org.eclipse.emf.ecore.EPackage
import edu.ustb.sei.mde.mohash.emfcompare.MoHashMatchEngineFactory
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.uml2.uml.UMLPackage
import java.util.concurrent.atomic.AtomicInteger

/**
 * RQ d(e1,e2) > d(e1,e3) => sim(e1,e2) <= sim(e1,e3)
 */
class EvaluateConsistency {
	val DistanceFunction distance
	val EObjectSimHasher hasher
	val objectsOfType = new ArrayList<EObject>
	val EClass focusedType
	
	new(EClass type, DistanceFunction distance, EObjectSimHasher hasher) {
		this.focusedType = type
		this.distance = distance
		this.hasher = hasher
	}
	
	def prepare(EObject root) {
		prepare(Collections.singletonList(root))
	}
	def prepare(List<EObject> contents) {
		init(contents)
	}
	
	protected def void init(List<EObject> contents) {
		for(r : contents) {
			if(focusedType === r.eClass) {
				objectsOfType.add(r)
			}
			r.eAllContents.forEachRemaining[c|
				if(focusedType===c.eClass) {
					objectsOfType.add(c)
				}
			]
		}
	}
	
	def void evaluateAll(PrintStream out) {
		val totalTriples = new AtomicInteger(0)
		val totalInconsistencies = new AtomicInteger(0)
		val equalInconsistencies = new AtomicInteger(0)
		
		val comparison = EcoreUtil.create(ComparePackage.eINSTANCE.comparison) as Comparison
		
		objectsOfType.parallelStream.forEach[e1|
			var HashValue64 h1 = null
			
			synchronized(hasher) {
				h1 = new HashValue64(hasher.hash(e1))
			}
			for(e2: objectsOfType) {
				if(e2!==e1) {
					var HashValue64 h2 = null
					synchronized(hasher) {
						h2 = new HashValue64(hasher.hash(e2))
					}
					val sim_1_2 = Hash64.cosineSimilarity(h1, h2)
					var double d_1_2;
					val match_par_1_2 = EcoreUtil.create(ComparePackage.eINSTANCE.match) as Match
					match_par_1_2.left = e1.eContainer
					match_par_1_2.right = e2.eContainer

					synchronized(distance) {
						comparison.matches += match_par_1_2
						d_1_2 = distance.distance(comparison, e1, e2)	
						comparison.matches.remove(match_par_1_2)
					}

					for(e3 : objectsOfType) {
						if(e3!==e2 && e3!==e1) {
							var HashValue64 h3 = null
							synchronized(hasher) {
								h3 = new HashValue64(hasher.hash(e3))
							}
							val sim_1_3 = Hash64.cosineSimilarity(h1, h3)
							val match_par_1_3 = EcoreUtil.create(ComparePackage.eINSTANCE.match) as Match
							match_par_1_3.left = e1.eContainer
							match_par_1_3.right = e3.eContainer
							var double d_1_3 = 0 
							synchronized(distance) {
								comparison.matches += match_par_1_3
								d_1_3 = distance.distance(comparison, e1, e3)	
								comparison.matches.remove(match_par_1_3)
							}
							
							totalTriples.incrementAndGet
							
							if(d_1_2 < d_1_3) {
								if(sim_1_2 < sim_1_3) totalInconsistencies.incrementAndGet
							} else if(d_1_3 < d_1_2) {
								if(sim_1_3 < sim_1_2) totalInconsistencies.incrementAndGet
							} else { // ==
								if(sim_1_2!==Double.MAX_VALUE) {									
									if(Math.max(sim_1_2, sim_1_3)>0.5 && Math.min(sim_1_2, sim_1_3)<0.5) equalInconsistencies.incrementAndGet
								}
							}
						}
					}
				}
			}
		]
		
		synchronized(out) {			
			out.println("For "+focusedType.name)
			out.println("\tTotal: "+totalTriples+", Incons: "+totalInconsistencies.get+" ("+((totalInconsistencies.get as double)/totalTriples.get)+")"+", Eqcons: "+equalInconsistencies.get+" ("+((equalInconsistencies.get as double)/totalTriples.get)+")")
		}
	}
	
	def static void evaluate(EPackage metamodel, EObject root, PrintStream out) {
			
		val list = metamodel.EClassifiers.filter[it instanceof EClass].map[it as EClass].filter[it.abstract===false].toList
		list.parallelStream.forEach[c|
			out.println(c.name+" begin...")
			val factory = new MoHashMatchEngineFactory()
			factory.matchEngine
			val e = new EvaluateConsistency(c, factory.distance, factory.hasher)
			e.prepare(root)
			e.evaluateAll(out)
			out.println(c.name+" end")
		]
	}
	
	def static void main(String[] args) {
		evaluate(EcorePackage.eINSTANCE, UMLPackage.eINSTANCE, System.out)
	}
}