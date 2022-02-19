package edu.ustb.sei.mde.mohash.onehot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;

import com.google.common.collect.Iterables;

import edu.ustb.sei.mde.mohash.functions.URIComputer;
import edu.ustb.sei.mde.mohash.indexstructure.HWTree;

/**
 * The basic idea of onehot hasher is as follows.
 * <ol>
 * <li>Build the word table.</li>
 * <li>Construct a word bag of each EObject.</li>
 * </ol>
 * @author hexiao
 *
 */
public class EObjectOneHotHasher {
	protected int worldSize = 1024;
	
	protected URIComputer uriEncoder = new URIComputer();
	protected NGramSplitter stringSplitter = new NGramSplitter();
	

	public EObjectOneHotHasher() {
		words = new HashSet<>(1000);
		wordBagMap = new HashMap<>(1000);
		wordTable = new HashMap<>(1000);
		vectorMap = new HashMap<>();
	}
	
	protected Set<Object> words;
	protected Map<Object, Integer> wordTable;
	protected Map<EObject, Set<Object>> wordBagMap;
	protected Map<EObject, Set<Integer>> vectorMap;
		
	public void prehash(EObject data) {
		Set<Object> bow = extractWordBag(data);
		words.addAll(bow);
		wordBagMap.put(data, bow);
	}
	
	public void doHash() {
		buildWordTable();
		buildHashVectors();
	}
	
	private void buildHashVectors() {
		wordBagMap.entrySet().forEach(e->{
			Set<Integer> vec = new HashSet<>();
			e.getValue().forEach(o->{
				vec.add(wordTable.get(o));
			});
			vectorMap.put(e.getKey(), vec);
		});
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
	
	
	public Map<EObject, Collection<EObject>> testLSC(Iterable<? extends EObject> left, Iterable<? extends EObject> right) {
		Map<EObject, Collection<EObject>> result = new HashMap<>();
		
		int more=0, less=0;
		for(EObject l : left) {
			Set<Integer> leftHash = vectorMap.get(l);
			Set<EObject> cand = new HashSet<>();
			for(EObject r : right) {
				Set<Integer> rightHash = vectorMap.get(r);
				int diff = HWTree.integerSetHamDistance(leftHash, rightHash);
				if(diff<=50 ) {
					cand.add(r);
				}
			}
			
			result.put(l, cand);
		}
		
		System.out.println("more="+more+"\tless="+less);
		
		return result;
	}
	
	public Map<EObject, Collection<EObject>> testHWT(Iterable<? extends EObject> left, Iterable<? extends EObject> right) {
		Map<EObject, Collection<EObject>> result = new HashMap<>();
		
		
		HWTree<Set<Integer>, EObject> hwTree = new HWTree<>((l,r)->{
			return HWTree.integerSetHamDistance(l, r);
		}, (h, d)->{
			return HWTree.integerSetCodePattern(h, d, worldSize);
		});
		
		for(EObject o :right) {	
			Set<Integer> h = vectorMap.get(o);
			hwTree.insert(h, o);
		}
		
		left.forEach(l->{
			Set<Integer> leftHash = vectorMap.get(l);
			Collection<EObject> cand = hwTree.searchKNearest(leftHash, 1000, 50);
			result.put(l, cand);
		});
		
		return result;
		
	}
	
	public void print(EObject left, Iterable<? extends EObject> right) {
		Set<Integer> lefthash = vectorMap.get(left);
		Set<Object> leftbag = wordBagMap.get(left);
//		int[] leftminhash = minhashVectorMap.get(left);
		
		right.forEach(r->{
			if(r.eClass()==left.eClass()) {				
				Set<Integer> righthash = vectorMap.get(r);
				Set<Object> rightbag = wordBagMap.get(r);
//				int[] rightminhash = minhashVectorMap.get(r);
				
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
					
					minHamSim = 1.0 - (((double)diff.size())/worldSize);
					
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
