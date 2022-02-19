package edu.ustb.sei.mde.mohash.indexstructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Hamming Weighted Tree
 * @author hexiao
 *
 */
public class HWTree<H,D> {
	protected BiFunction<H,H, Integer> hammingDistanceFunction;
	public HWTree(BiFunction<H, H, Integer> hammingDistanceFunction,
			BiFunction<H, Integer, CodePattern> particularPatternComputer) {
		rootNode = new HWTreeRootNode<H,D>();
		this.hammingDistanceFunction = hammingDistanceFunction;
		this.particularPatternComputer = particularPatternComputer;
	}

	protected BiFunction<H,Integer,CodePattern> particularPatternComputer;
	
	static public int integerSetHamDistance(Set<Integer> left, Set<Integer> right) {
		int union = 0;
		for(Integer r : right) {
			if(left.contains(r)) union++;
		}
		int total = left.size() + right.size();
		return total - union * 2;
	}
	
	static public CodePattern integerSetCodePattern(Set<Integer> left, int depth, int worldSize) {
		int fragments = 1 << depth;
		short[] weight = new short[fragments];
		
		int[] bound = new int[fragments];
		
		integerSetBound(bound, 0, fragments, 0, worldSize);
		
		for(Integer i : left) {
			int pos = Arrays.binarySearch(bound, i);
			if(pos < 0) pos = -(pos + 1);
			weight[pos] ++;
		}
		
		CodePattern pattern = new CodePattern();
		pattern.weight = weight;
		
		return pattern;
	}
	
	static private void integerSetBound(int[] bound,int bstart, int bend, int worldLow, int worldHigh) {
		if(bend-bstart==1) {
			bound[bstart] = worldHigh;
		} else {
			int bmid = (bstart+bend)/2;
			int worldMid = (worldLow+worldHigh)/2;
			
			integerSetBound(bound, bstart, bmid, worldLow, worldMid);
			integerSetBound(bound, bmid, bend, worldMid, worldHigh);
		}
	}
	
	static public int intArrayHamDistance(int[] left, int[] right) {
		int diff = 0;
		for(int i=0;i<left.length;i++) {
			diff += Math.abs(left[i] - right[i]);
		}
		return diff;
	}
	
	static public CodePattern intArrayCodePattern(int[] array, int depth) {
		int fragments = 1 << depth;
		short[] weight = new short[fragments];
		
		intArrayCodePattern(array, 0, array.length, weight, 0, weight.length);
		
		CodePattern pattern = new CodePattern();
		pattern.weight = weight;
		
		return pattern;
	}
	
	static private void intArrayCodePattern(int[] array, int start, int end, short[] weight, int wstart, int wend) {
		if(wend==wstart) return;
		if(wend-wstart==1) {
			int sum = 0;
			for(int i=start;i<end;i++) {
				sum += array[i];
			}
			weight[wstart] = (short) sum;
		} else {
			int mid = (start+end)/2;
			int wmid = (wstart+wend)/2;
			intArrayCodePattern(array, start, mid, weight, wstart, wmid);
			intArrayCodePattern(array, mid, end, weight, wmid, wend);
		}
	}
	
	static public int longHashDistance(long left, long right) {
		long xor = left ^ right;
		return Long.bitCount(xor);
	}
	
	static public CodePattern longCodePattern(long code, int depth) {
		int fragments = 1 << depth;
		short[] weight = new short[fragments];
		
		int64CodePattern(code, weight, 0, weight.length);
		CodePattern pattern = new CodePattern();
		pattern.weight = weight;
		
		return pattern;
	}
	
	static private void int64CodePattern(long num, short[] weight, int wstart, int wend) {
		if(wend-wstart==1) {
			int count = Long.bitCount(num);
			weight[wstart] = (short) count;
		} else {
			long left = num >>> 32;
			long right = num &0x00000000FFFFFFFF;
			int mid = (wstart+wend)/2;
			
			int32CodePattern((int) left, weight, wstart, mid);
			int32CodePattern((int) right, weight, mid, wend);
		}
	}
	
	static private void int32CodePattern(int num, short[] weight, int wstart, int wend) {
		if(wend-wstart==1) {
			int count = Integer.bitCount(num);
			weight[wstart] = (short) count;
		} else {
			int left = num >>> 16;
			int right = num &0x0000FFFF;
			int mid = (wstart+wend)/2;
			
			int16CodePattern(left, weight, wstart, mid);
			int16CodePattern(right, weight, mid, wend);
		}
	}
	
	static private void int16CodePattern(int num, short[] weight, int wstart, int wend) {
		if(wend-wstart==1) {
			int count = Integer.bitCount(num);
			weight[wstart] = (short) count;
		} else {
			int left = num >>> 8;
			int right = num &0x00FF;
			int mid = (wstart+wend)/2;
			int8CodePattern(left, weight, wstart, mid);
			int8CodePattern(right, weight, mid, wend);
		}
	}
	
	static private void int8CodePattern(int num, short[] weight, int wstart, int wend) {
		if(wend-wstart==1) {
			int count = Integer.bitCount(num);
			weight[wstart] = (short) count;
		} else {
			int left = num >>> 4;
			int right = num &0x0F;
			int mid = (wstart+wend)/2;
			int4CodePattern(left, weight, wstart, mid);
			int4CodePattern(right, weight, mid, wend);
		}
	}
	
	static private void int4CodePattern(int num, short[] weight, int wstart, int wend) {
		if(wend-wstart==1) {
			int count = Integer.bitCount(num);
			weight[wstart] = (short) count;
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	protected int maxDepth = -1;
	
	protected HWTreeInnerNode<H,D> rootNode;
	
	
	public CodePattern[] computePattern(H code, int depth) {
		CodePattern[] patterns = new CodePattern[depth + 1];
		patterns[depth] = particularPatternComputer.apply(code, depth);
		
		for(int i=depth; i>0; i--) {
			int toCompute = i - 1;
			CodePattern prev = patterns[i];
			CodePattern pat = new CodePattern();
			pat.doAbstract(prev);
			patterns[toCompute] = pat;
		}
		
		return patterns;
	}
	
	public Collection<D> search(H code) {
		return search(code, 0);
	}
	
	public List<D> search(H code, int diffRange) {
		List<D> results = new ArrayList<>(128);
		CodePattern[] patterns = computePattern(code, maxDepth);
		search(rootNode, code, 0, diffRange, diffRange, patterns, results);
		return results;
	}
	
	public List<D> searchKNearest(H code, int k, int maxDiff) {
		List<D> results = new ArrayList<>(128);
		CodePattern[] patterns = computePattern(code, maxDepth);
		
		for(int diff = 0; diff <= maxDiff; diff ++) {
			search(rootNode, code, diff, diff, maxDiff, patterns, results);
			if(results.size() >= k && diff > 4) break;
		}
		
		return results;
	}
	
	public void insert(H code, D data) {
		CodePattern[] patterns = computePattern(code, maxDepth + 1);
		HWTreeNode<H,D> node = findContainerNode(rootNode, code, patterns);
		
		if(node instanceof HWTreeLeaf) {
			((HWTreeLeaf<H,D>) node).insert(code, data);
			
			if(((HWTreeLeaf<H, D>) node).shouldSplit()) {
				int newDep = ((HWTreeLeaf<H, D>) node).split(particularPatternComputer);
				if(maxDepth < newDep) maxDepth = newDep;
			}
		} else {
			if(node!=null) {
				HWTreeInnerNode<H,D> parent = (HWTreeInnerNode<H, D>) node;
				insertUnder(parent, patterns[parent.depth + 1], code, data);
			}
		}
	}
	
	public void remove(H code, D data) {
		CodePattern[] patterns = computePattern(code, maxDepth);
		HWTreeNode<H,D> node = findContainerNode(rootNode, code, patterns);
		if(node instanceof HWTreeLeaf) {
			((HWTreeLeaf<H,D>) node).remove(code, data);
			if(((HWTreeLeaf<H,D>) node).shouldBeRemoved()) node.removeFromParent();
		}
	}
	
	private void search(HWTreeNode<H,D> parent, H code, int lowerBoundOfPatternDiff, int upperBoundOfPatternDiff, int maxDiffOfHashing, CodePattern[] patterns, Collection<D> results) {
		if(lowerBoundOfPatternDiff > upperBoundOfPatternDiff) {
			System.err.println("The diffRange should be larger!");
			return;
		}
		
		if(parent instanceof HWTreeInnerNode) {
			for(HWTreeNode<H,D> child : ((HWTreeInnerNode<H,D>) parent).children) {
				int childDepth = child.depth;
				CodePattern pattern = patterns[childDepth];
				if(child.codePattern.match(pattern, lowerBoundOfPatternDiff, upperBoundOfPatternDiff)) {
					search(child, code, lowerBoundOfPatternDiff, upperBoundOfPatternDiff, maxDiffOfHashing, patterns, results);
				}
			}
		} else if(parent instanceof HWTreeLeaf) {
			for(Entry<D,H> e : ((HWTreeLeaf<H,D>) parent).getStoredData()) {
				int diff = hammingDistanceFunction.apply(code, e.getValue());
				if(diff <= maxDiffOfHashing) {
					results.add(e.getKey());
				}
			}
		}
	}
	
	private HWTreeNode<H,D> findContainerNode(HWTreeNode<H,D> parent, H code, CodePattern[] patterns) {
		if(parent instanceof HWTreeInnerNode) {
			for(HWTreeNode<H,D> child : ((HWTreeInnerNode<H,D>) parent).children) {
				int childDepth = child.depth;
				CodePattern pattern = patterns[childDepth];
				if(child.codePattern.match(pattern)) {
					return findContainerNode(child, code, patterns);
				}
			}
		}
		
		return parent;
	}
	
	private void insertUnder(HWTreeInnerNode<H,D> parent, CodePattern pattern, H code, D data) {
		HWTreeLeaf<H,D> newLeaf = new HWTreeLeaf<>(pattern);
		newLeaf.insert(code, data);
		
		parent.addChildren(newLeaf);
		if(newLeaf.depth>maxDepth) 
			maxDepth = newLeaf.depth;
	}
	
}

abstract class HWTreeNode<H,D> {
	final public CodePattern codePattern;

	public void removeFromParent() {
		HWTreeInnerNode<H,D> parent = (HWTreeInnerNode<H,D>) this.parent;
		parent.remove(this);
		if(parent.shouldBeRemoved()) {
			parent.removeFromParent();
		}
	}
	
	public HWTreeNode(CodePattern codePattern) {
		super();
		this.codePattern = codePattern;
	}
	/**
	 * The depth of this node. The root node has a depth of -1.
	 */
	public int depth;	
	protected HWTreeNode<H,D> parent;
	protected int positionInParent;
}

class HWTreeInnerNode<H,D> extends HWTreeNode<H,D> {
	public HWTreeInnerNode(CodePattern codePattern) {
		super(codePattern);
		children = new ArrayList<>(128);
	}

	protected List<HWTreeNode<H,D>> children;
	
	public List<HWTreeNode<H,D>> getChildren() {
		return children;
	}
	
	public void addChildren(HWTreeNode<H,D> child) {
		child.depth = depth + 1;
		child.parent = this;
		int position = children.size();
		child.positionInParent = position;
		children.add(child);
	}
	
	public void replace(HWTreeNode<H,D> child, HWTreeNode<H,D> newChild) {
		if(child.parent!=this) return;
		
		newChild.parent = child.parent;
		newChild.depth = child.depth;
		newChild.positionInParent = child.positionInParent;
		
		children.set(child.positionInParent, newChild);
	}
	
	public void remove(HWTreeNode<H,D> child) {
		if(child.parent==this) {
			int last = this.children.size() - 1;
			if(child.positionInParent==last) {				
				this.children.remove(last);
			} else {
				HWTreeNode<H,D> lastNode = this.children.get(last);
				this.children.set(child.positionInParent, lastNode);
				this.children.remove(last);
				lastNode.positionInParent = child.positionInParent;
			}
		}
	}
	
	public boolean shouldBeRemoved() {
		return this.children.isEmpty();
	}
}

final class HWTreeRootNode<H,D> extends HWTreeInnerNode<H,D> {

	public HWTreeRootNode() {
		super(null);
		this.depth = -1;
	}
	
	@Override
	public boolean shouldBeRemoved() {
		return false;
	}
	
	@Override
	public void removeFromParent() {
	}
	
}

class HWTreeLeaf<H,D> extends HWTreeNode<H,D> {
	public static final int MAX_TREE_DEPTH = 3;
	public static final int BUCKET_SIZE_THRESHOLD = 128;

	public HWTreeLeaf(CodePattern codePattern) {
		super(codePattern);
		storedData = new LinkedHashMap<>();
	}

	protected Map<D, H> storedData;
	
	public Collection<Entry<D,H>> getStoredData() {
		return storedData.entrySet();
	}
	
	public void insert(H code, D data) {
		this.storedData.put(data, code);
	}
	
	public void remove(H code, D data) {
		storedData.remove(data);
	}
	
	public boolean shouldSplit() {
		return (storedData.size() > BUCKET_SIZE_THRESHOLD && depth < MAX_TREE_DEPTH);
	}
	
	public boolean shouldBeRemoved() {
		return this.storedData.isEmpty();
	}
	
	public int split(BiFunction<H,Integer,CodePattern> particularPatternComputer) {
		int newDepth = depth + 1;
		
		Map<CodePattern, HWTreeLeaf<H,D>> newSplits = new HashMap<>();
		for(Entry<D,H> e : getStoredData()) {
			CodePattern pat = particularPatternComputer.apply(e.getValue(), newDepth);
			HWTreeLeaf<H,D> newLeaf = newSplits.computeIfAbsent(pat, (p)->{
				HWTreeLeaf<H,D> leaf = new HWTreeLeaf<H,D>(pat);
				return leaf;
			});
			newLeaf.insert(e.getValue(), e.getKey());
		}
		
		HWTreeInnerNode<H,D> newContainer = new HWTreeInnerNode<H,D>(this.codePattern);
		((HWTreeInnerNode<H,D>)parent).replace(this, newContainer);
		
		for(Entry<CodePattern,HWTreeLeaf<H,D>> e : newSplits.entrySet()) {
			newContainer.addChildren(e.getValue());
		}
		
		return newDepth;
	}
}