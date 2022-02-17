package edu.ustb.sei.mde.mohash.minhash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;

import edu.ustb.sei.mde.mohash.functions.URIComputer;

/**
 * The basic idea of MinHasher is as follows.
 * <ol>
 * <li>Build the word table.</li>
 * <li>Construct a word bag of each EObject.</li>
 * <li>Construct perturbation hash functions.</li>
 * <li>Compute min hash of each object.</li>
 * </ol>
 * @author hexiao
 *
 */
public class EObjectMinHasher {
	// #band=12	#row=2	thresh=0.288675	fp=0.876590	fn=0.004722	balance=2.268637 for thu=0.6, thd=0.4
	//#band=32	#row=4	thresh=0.420448	fp=0.563893	fn=0.011776	balance=1.680193
	protected int numberOfMinHashFunctions = 32;
	protected int row = 4;
	protected int worldSize = 1024;
	
	protected URIComputer uriEncoder = new URIComputer();
	protected NGramSplitter stringSplitter = new NGramSplitter();
	

	public EObjectMinHasher() {
		words = new HashSet<>(1000);
		wordBagMap = new HashMap<>(1000);

		wordTable = new HashMap<>(1000);
		functions = new PerturbationFunction[numberOfMinHashFunctions];
		
		minhashVectorMap = new ConcurrentHashMap<>(1000);
		
		vectorMap = new HashMap<>();
	}
	
	protected Set<Object> words;
	protected Map<Object, Integer> wordTable;
	protected Map<EObject, Set<Object>> wordBagMap;
	protected Map<EObject, Set<Integer>> vectorMap;
	protected Map<EObject, int[]> minhashVectorMap;
	
	private interface PerturbationFunction extends Function<Integer, Integer> {}
	private PerturbationFunction[] functions;
	
	public void prehash(EObject data) {
		Set<Object> bow = extractWordBag(data);
		words.addAll(bow);
		wordBagMap.put(data, bow);
	}
	
	public int[] getHash(EObject object) {
		return minhashVectorMap.get(object);
	}
	
	public void doHash() {
		// build functions
		// build word table
		buildWordTable();
		buildFunctions();
		buildMinHashVectors();
		
	}
	
	private void buildMinHashVectors() {
		wordBagMap.entrySet().parallelStream().forEach(e->{
			int[] hash = minhash(e.getValue());
			minhashVectorMap.put(e.getKey(), hash);
		});
		
		wordBagMap.entrySet().forEach(e->{
			Set<Integer> vec = new HashSet<>();
			e.getValue().forEach(o->{
				vec.add(wordTable.get(o));
			});
			vectorMap.put(e.getKey(), vec);
		});
	}

	static final int[] primes = new int[] {3,5,7,11,13,17,19,23,29,31,37,41,43,47,53,59,61,67,71,73,79,83,89,97,101,103,107,109,113,127,131,137,139,149,151,157,163,167,173,179,181,191,193,197,199,211,223,227,229,233,239,241,251,257,263,269,271,277,281,283,293,307,311,313,317,331,337,347,349,353,359,367,373,379,383,389,397,401,409,419,421,431,433,439,443,449,457,461,463,467,479,487,491,499,503,509,521,523,541,547,557,563,569,571,577,587,593,599,601,607,613,617,619,631,641,643,647,653,659,661,673,677,683,691,701,709,719,727,733,739,743,751,757,761,769,773,787,797,809,811,821,823,827,829,839,853,857,859,863,877,881,883,887,907,911,919,929,937,941,947,953,967,971,977,983,991,997};
	
	private void buildFunctions() {
		int totalWords = words.size();
		for(int i=0,j=0;i<numberOfMinHashFunctions; i++,j++) {
			
			int oldJ = j;
			while(j<primes.length && (totalWords!=0 && (totalWords/primes[j])*primes[j]==totalWords)) {
				j ++;
			}
			
			final int p;
			final int q;
			
			if(j>=primes.length) {
				p = primes[oldJ];
				q = primes[oldJ+1]; // although there is a risk of out of bound, it is unlikely to happen
			} else {
				p = primes[j];
				q = 1;
			}
			
			functions[i] = (x) -> {return Math.abs(x * p + q) % (worldSize-1) + 1;};
		}
	}

	private void buildWordTable() {
		ArrayList<Object> list = new ArrayList<>(words);
		Collections.shuffle(list);
		for(int i = 0;i<list.size(); i++) {
			wordTable.put(list.get(i), i % worldSize);
		}
	}

	@SuppressWarnings("unchecked")
	protected Set<Object> extractWordBag(EObject object) {
//		return Collections.emptySet();
		
		Set<Object> bag = new HashSet<>();
		EClass clazz = object.eClass();
		for(EStructuralFeature feature : getHashableFeatures(clazz)) {
			Object raw = object.eGet(feature);
			if(raw!=null) {
				if(feature.isMany()) {
					if(feature instanceof EReference) {
						List<EObject> objs = (List<EObject>) raw;
						for(EObject o : objs) extractURI(o, bag);
					} else {
						List<Object> objs = (List<Object>) raw;
						for(Object o : objs) extractValue(o, bag);
					}
				} else {
					if(feature instanceof EReference) {
						extractURI((EObject)raw, bag);
					} else {
						extractValue(raw, bag);
					}
				}
			}
		}
		
		return bag;
	}

	private void extractValue(Object value, Set<Object> bag) {
		if(value instanceof String) {
			bag.addAll(stringSplitter.split((String) value));
		} else {
			bag.add(value);
		}
	}

	private void extractURI(EObject value, Set<Object> bag) {
		for(String str : uriEncoder.getOrComputeLocation(value)) {
			bag.add(str);
		}
	}

	protected int[] minhash(Set<Object> bag) {
		// get the bag of words of data
		// compute sig of data using one-hot rep
		// use minhash functions h1,...,hn to compute a hash vector  (numberOfFunctions integers)
		
//		Set<Object> bag = wordBagMap.get(data);
		int[] signature = new int[bag.size()];
		
		int i = 0;
		for(Object o : bag) {
			signature[i] = wordTable.get(o);
			i++;
		}
		
		int[] minhashVectors = new int[numberOfMinHashFunctions];
		
		IntStream.range(0, numberOfMinHashFunctions).parallel().forEach(idx->{
			minhashVectors[idx] = minhash(functions[idx], signature);
		});
		
//		for(i=0;i<numberOfMinHashFunctions;i++) {
//			minhashVectors[i] = minhash(functions[i], signature);
//		}
		
		return minhashVectors;
	}
	
	private int minhash(PerturbationFunction func, int[] integers) {
//		return Arrays.stream(integers).parallel().map(i->func.apply(i)).min().orElse(0);
		int min = 0;
		for(int i : integers) {
			Integer newIndex = func.apply(i);
			if(min==0 || newIndex < min) min = newIndex;
		}
		return min;
	}
	
	
	public List<EStructuralFeature> getHashableFeatures(EClass clazz) {
		List<EStructuralFeature> it = classFeatureHasherMap.get(clazz);
		if(it==null) {
			Iterable<EStructuralFeature> features = Iterables.filter(clazz.getEAllStructuralFeatures(), f->!shouldSkip(f));
			List<EStructuralFeature> list = new LinkedList<>();
			features.forEach(f->{
				list.add(f);
			});
			it = list;
			classFeatureHasherMap.put(clazz, it);
		}
		
		return it;
	}
	
	private Map<EClass, List<EStructuralFeature>> classFeatureHasherMap = new HashMap<>();
	
	public boolean shouldSkip(EStructuralFeature feature) {
		if(feature.isDerived() || feature.isTransient()) return true;
		if(feature == EcorePackage.Literals.EMODEL_ELEMENT__EANNOTATIONS) return true;
		if(feature instanceof EReference) return ((EReference) feature).isContainer() || ((EReference) feature).isContainment();
		return false;
	}

	
	public void print() {
		System.out.println("total words:"+words.size());
		vectorMap.forEach((e,v)->{
			System.out.print(e);
			System.out.print("\t");
			System.out.print(wordBagMap.get(e).size());
			System.out.print("\t");
			System.out.print("{");
			for(int vv: v) System.out.print(vv+",");
			System.out.println("}");
		});
	}
	
	public void print(EObject left, Iterator<EObject> right) {
		Set<Integer> lefthash = vectorMap.get(left);
		Set<Object> leftbag = wordBagMap.get(left);
		
		right.forEachRemaining(r->{
			if(r.eClass()==left.eClass()) {				
				Set<Integer> righthash = vectorMap.get(r);
				Set<Object> rightbag = wordBagMap.get(r);
				
//				boolean rowEqual = false;
//				for(int i=0 ; i<lefthash.length/row;i++) {
//					rowEqual = true;
//					for(int j=0;j<row;j++) {						
//						if(lefthash[i*row + j]!=righthash[i*row + j]) rowEqual = false;
//					}
//					if(rowEqual==true) break;
//				}
				
				double fullHamSim, fullJacSim;
				double minHamSim, minJacSim;
				int fullHamDiff,  minHamDiff;
				{
					Set<Object> union = new HashSet<>();
					Set<Object> intersect = new HashSet<>();
					Set<Object> diff = new HashSet<>();
					
					union.addAll(lefthash);
					union.addAll(righthash);
					
					intersect.addAll(lefthash);
					intersect.retainAll(righthash);
					
					diff.addAll(union);
					diff.removeAll(intersect);
					
					minHamSim = 1.0 - (((double)diff.size())/numberOfMinHashFunctions);
					
					minJacSim = ((double)intersect.size())/union.size();
					
					minHamDiff = diff.size();
				}
				
				{
					Set<Object> union = new HashSet<>();
					Set<Object> intersect = new HashSet<>();
					Set<Object> diff = new HashSet<>();
					
					union.addAll(leftbag);
					union.addAll(rightbag);
					
					intersect.addAll(leftbag);
					intersect.retainAll(rightbag);
					
					diff.addAll(union);
					diff.removeAll(intersect);
					
					fullHamSim = 1.0 - (((double)diff.size())/worldSize);
					
					fullJacSim = ((double)intersect.size())/union.size();
					
					fullHamDiff = diff.size();
				}

				
				
				if(fullHamDiff <= 25) {
					System.out.println("FullJacSim="+fullJacSim+"\tminJacSim="+minJacSim+"\tFullHamSim="+fullHamSim+"\tminHamSim="+minHamSim+"\tfullHDiff="+fullHamDiff+"\tminHDiff="+minHamDiff+"\t"+left+"\t"+r);
				}
			}
		});
	}
}
