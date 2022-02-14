package edu.ustb.sei.mde.mohash.tests;

import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.uml2.uml.UMLPackage;

import edu.ustb.sei.mde.mohash.EHasherTable;
import edu.ustb.sei.mde.mohash.EObjectHasher;
import edu.ustb.sei.mde.mohash.HashValue64;
import edu.ustb.sei.mde.mohash.ObjectIndex;
import edu.ustb.sei.mde.mohash.emfcompare.MoHashMatchEngineFactory;
import edu.ustb.sei.mde.mohash.emfcompare.MohashEMFCompareBuilder;
import edu.ustb.sei.mde.mohash.functions.Hash64;

public class BencharmkingEMFCompare {
	public boolean checkComparision(Iterator<EObject> iterator, Comparison a, Comparison b, Function<EObject, Long> hasher) {
//		EHasherTable table = new EHasherTable();
//		EObjectHasher privateHasher = new EObjectHasher(table);
//		
		boolean flag = true;
		for(EObject e = null; iterator.hasNext();) {
			e = iterator.next();
			Match ma = a.getMatch(e);
			Match mb = b.getMatch(e);
			if((ma==null && mb!=null)||(ma!=null && mb==null)) 
				return false;
			if(ma!=mb) {
				if(!(ma.getLeft()==mb.getLeft() && ma.getRight()==mb.getRight() && ma.getOrigin()==mb.getOrigin())) {
					flag = false;
					System.out.println(ma);
					printMatchHash("A", hasher, ma);
					System.out.println(mb);
					printMatchHash("B", hasher, mb);
					System.out.println();
				}
			}
		}
		return flag;
	}

	protected void printMatchHash(String side, Function<EObject, Long> privateHasher, Match ma) {
		long hashA = ma.getLeft()==null ? 0 : privateHasher.apply(ma.getLeft());
		long hashB = ma.getRight()==null ? 0 : privateHasher.apply(ma.getRight());
		double sim = ObjectIndex.similarity(new HashValue64(hashA), new HashValue64(hashB));
		double sim2 = ObjectIndex.similarity(new HashValue64(hashA), new HashValue64(hashB));
		System.out.println(String.format("Match %s: leftHash=%s, rightHash=%s, j_sim=%f, c_sim=%f", side, Hash64.toString(hashA), Hash64.toString(hashB), sim, sim2));
	}
	
//	public void checkCorrectness(Resource left, Resource right) {
//			Comparison a = benchmarkingMohashBasedEMFCompare(left, right);
//			Comparison b = benchmarkingDefaultEMFCompare(left, right);
//			
//			if(checkComparision(left.getAllContents(), a, b)) {
//				System.out.println("equal");
//			} else {
//				System.out.println("inequal");
//			}
//	}
	
//	public void checkCorrectness(EObject left, EObject right) {
////		Comparison a = benchmarkingMohashBasedEMFCompare(left, right);
//		Comparison b = benchmarkingDefaultEMFCompare(left, right);
//		
//		if(checkComparision(left.eAllContents(), a, b, )) {
//			System.out.println("equal");
//		} else {
//			System.out.println("inequal");
//		}
//}
	
	public void benchmarking(Notifier left, Notifier right, int round) {
		for(int i=0;i<round;i++) {
			System.out.println("Round "+(i+1));
			benchmarkingMohashBasedEMFCompare(left, right);
			benchmarkingDefaultEMFCompare(left, right);
			System.out.println("======================");
		}
	}
	
	/*
	 * both resource and eobject are acceptable
	 */
	public Comparison benchmarkingMohashBasedEMFCompare(Notifier left, Notifier right) {
		EMFCompare compare = MohashEMFCompareBuilder.build();
		DefaultComparisonScope cScope = new DefaultComparisonScope(left, right, null);
		
		long start = System.nanoTime();
		Comparison result = compare.compare(cScope);
		long end = System.nanoTime();
		
		System.out.println(String.format("Mohash-based EMFCompare consumed %d nano sec.", (end-start)));
		
		return result;
	}
	
	/*
	 * both resource and eobject are acceptable
	 */
	public Comparison benchmarkingDefaultEMFCompare(Notifier left, Notifier right) {
		// We ensure that identifiers are not used
		EMFCompare compare = EMFCompare.builder().setMatchEngineFactoryRegistry(MoHashMatchEngineFactory.createEMFCompareFactoryRegistry()).build();
		DefaultComparisonScope cScope = new DefaultComparisonScope(UMLPackage.eINSTANCE, EcorePackage.eINSTANCE, null);
		
		long start = System.nanoTime();
		Comparison result = compare.compare(cScope);
		long end = System.nanoTime();
		
		System.out.println(String.format("Default EMFCompare consumed %d nano sec.", (end-start)));
		
		return result;
	}
}
