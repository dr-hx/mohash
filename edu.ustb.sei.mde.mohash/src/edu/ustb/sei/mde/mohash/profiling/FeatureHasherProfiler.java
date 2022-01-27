package edu.ustb.sei.mde.mohash.profiling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import edu.ustb.sei.mde.mohash.EHasherTable;
import edu.ustb.sei.mde.mohash.functions.Hash64;

/**
 * The basic idea is to determine the weights of the feature hashers.
 * We adopt two kinds of weights, i.e., positive weights and negative weights.
 * The positive weights denote how important the features are for identify an object. 
 * If a feature hasher produces unique bits, then the feature is important.
 * So, the occurrence of the feature bits may cause differences.
 * The negative weights denote how wide the features of an object are.
 * If the feature hasher produces many bits, then the feature is wide. 
 * If a feature is narrow (not wide), then a large negative weight may cancel other feature bits.
 * So the missing of the feature bits may cause differences.  
 * The uniqueness of a bit is measured by log(|O|/|B|), 
 * where |O| means the number of the objects that own the feature and |B| means the number of the objects that produce the bit.
 * The wideness of a feature is measured by avg(|W|)/64, where |W| is the number of bits of a hash code
 * 
 * @author hexiao
 *
 */
public class FeatureHasherProfiler {
	static final public int bandwidth = 64;
	
	private Map<EClass, Integer> objectCounts = new HashMap<>();
	private Map<EClass, Map<EStructuralFeature, int[]>> occurrenceMap = new HashMap<>();
	private Map<EClass, Map<EStructuralFeature, List<Integer>>> widthMap = new HashMap<>();
	
	public void addObjectCount(EClass clazz) {
		objectCounts.compute(clazz, (c, i)->{
			if(i==null) return 1;
			else return i+1;
		});
	}
	
	public void addOccurrence(EClass clazz, EStructuralFeature feature, long code) {
		occurrenceMap.compute(clazz, (c,m)->{
			if(m==null) {
				m = new HashMap<>();
			}
			
			m.compute(feature, (f,is)->{
				if(is==null) is = new int[bandwidth];
				
				
				for(int i=0;i<bandwidth; i++) {
					if((code & Hash64.bitmasks[i])!=0) {
						is[i] ++;
					}
				}
				
				return is;
			});
			
			return m;
		});
	}
	
	public void addWidth(EClass clazz, EStructuralFeature feature, long code) {
		int nb = Long.bitCount(code);
		widthMap.compute(clazz, (c,m)->{
			if(m==null) {
				m = new HashMap<>();
			}
			
			m.compute(feature, (f,list)->{
				if(list==null) list = new ArrayList<>(); 
				list.add(nb);
				return list;
			});
			
			return m;
		});
	}
	
	public void profile(EObject root, EHasherTable table) {
		EObjectHasherWithProfiling eObjectHasher = new EObjectHasherWithProfiling(this, table);
		
		eObjectHasher.hash(root);
		
		root.eAllContents().forEachRemaining(e->{
			eObjectHasher.hash(e);
		});
	}
	
	public void getResult() {
		System.out.println("Total objects");
		objectCounts.forEach((c,i)->{
			System.out.println(c.getClass().getName()+"\t"+i);
		});
		
		System.out.println();
		
		System.out.println("Bit occurrences");
		occurrenceMap.forEach((c,m)->{
			Integer nb = objectCounts.get(c);
			
			m.forEach((f, oc)->{
				System.out.print(c.getName()+"."+f.getName());
				double sum = 0.0;
				int total = 0;
				for(Integer i : oc) {
					System.out.print("\t"+i);
					if(i==0) System.out.print(" (N/A)");
					else {
						double weight = 0.0;
						weight = Math.log(((double)nb)/ i);
						System.out.print(String.format(" (%.2f)", weight));
						sum += weight;
						total ++;
					}
				}
				System.out.print("\t\tsum="+sum/total);
				System.out.println();
			});
		});
		System.out.println();
		
		System.out.println("Bit widths");
		widthMap.forEach((c,m)->{
			m.forEach((f, list)->{
				System.out.print(c.getName()+"."+f.getName());
				int sum = list.stream().reduce(0, (l,r)->l+r);
				double width = ((double)sum)/list.size()/64.0;
				System.out.print("\t"+width);
				System.out.println();
			});
		});
	}
}
