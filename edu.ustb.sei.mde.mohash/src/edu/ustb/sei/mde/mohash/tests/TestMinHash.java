package edu.ustb.sei.mde.mohash.tests;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.uml2.uml.UMLPackage;

import com.google.common.collect.Streams;

import edu.ustb.sei.mde.mohash.onehot.EObjectOneHotHasher;

public class TestMinHash {

	public TestMinHash() {
		// TODO Auto-generated constructor stub
	}
	
	static public void calc() {
		for(int b = 4; b <= 64; b += 2) {
			for(int r = 4; r<=32; r+=2) {
				double th = Math.pow(1.0/b, 1.0/r);
				double thu = Math.nextUp(th);
				double thd = Math.nextDown(th);
				
				double fp = 1.0 - Math.pow(1.0 - Math.pow(0.4, r), b);
				double fn = Math.pow(1.0 - Math.pow(0.6, r), b);
				
				double balance = Math.log10(fp/fn);
				
				if(fn < 0.02)
					System.out.println(String.format("#band=%d\t#row=%d\tthresh=%f\tfp=%f\tfn=%f\tbalance=%f", b, r, th, fp, fn, balance));
			}
		}
	}
	
	public static void main(String[] args) {
		
//		calc();
		
		EObjectOneHotHasher hasher = new EObjectOneHotHasher();
		
		
		for(int i=0;i<1;i++) {
		long start = System.nanoTime();
		UMLPackage.eINSTANCE.eAllContents().forEachRemaining(e->hasher.prehash(e));
		EcorePackage.eINSTANCE.eAllContents().forEachRemaining(e->hasher.prehash(e));
		long end1 = System.nanoTime();
		
		hasher.doHash();
		long end2 = System.nanoTime();
		
		System.out.println(end1-start);
		System.out.println(end2-start);
		}
		
//		hasher.print();
		
		Set<EClass> allUMLEClasses = new HashSet<>();
		Set<EClass> allEcoreEClasses = new HashSet<>();
		
		UMLPackage.eINSTANCE.eAllContents().forEachRemaining(e->{
			if(e instanceof EClass) allUMLEClasses.add((EClass) e);
		});
		
		EcorePackage.eINSTANCE.eAllContents().forEachRemaining(e->{
			if(e instanceof EClass) allEcoreEClasses.add((EClass) e);
		});
		
//		AtomicInteger count = new AtomicInteger();
//		
//		allUMLEClasses.forEach(left->{
//			if(left instanceof EClass) {
//				hasher.print(left, allEcoreEClasses);
//				count.incrementAndGet();
//			}
//		});
//		System.out.println("total="+count.get());
		
		for(int t = 0; t<1;t++) {			
			long s1 = System.nanoTime();
			Map<EObject, Collection<EObject>> r1 = hasher.testLSC(allEcoreEClasses, allUMLEClasses);
			long e1 = System.nanoTime();
			
			long s2 = System.nanoTime();
			Map<EObject, Collection<EObject>> r2 = hasher.testHWT(allEcoreEClasses, allUMLEClasses);
			long e2 = System.nanoTime();
			
			
			System.out.println("LSC:"+(e1-s1));
			System.out.println("HWT:"+(e2-s2));
			
			allEcoreEClasses.forEach(e->{
				Collection<EObject> lsc = r1.get(e);
				Collection<EObject> hwt = r2.get(e);
				if(!lsc.containsAll(hwt) || !hwt.containsAll(lsc)) {
					System.out.println(e);
					System.out.println(lsc);
					System.out.println(hwt);
				}
			});
		}
	}

}
