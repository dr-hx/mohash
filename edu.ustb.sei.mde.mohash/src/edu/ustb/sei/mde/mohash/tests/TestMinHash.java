package edu.ustb.sei.mde.mohash.tests;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.uml2.uml.UMLPackage;

import com.google.common.collect.Streams;

import edu.ustb.sei.mde.mohash.minhash.EObjectMinHasher;

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
		
		EObjectMinHasher hasher = new EObjectMinHasher();
		
		
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
		
		AtomicInteger count = new AtomicInteger();
		UMLPackage.eINSTANCE.eAllContents().forEachRemaining(left->{
			if(left instanceof EClass) {
				hasher.print(left, UMLPackage.eINSTANCE.eAllContents());
				count.incrementAndGet();
			}
		});
		System.out.println("total="+count.get());
	}

}
