package edu.ustb.sei.mde.mohash.emfcompare;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.Monitor;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.match.eobject.EObjectIndex.Side;
import org.eclipse.emf.compare.match.eobject.ProximityEObjectMatcher.DistanceFunction;
import org.eclipse.emf.compare.match.eobject.WeightProvider.Descriptor.Registry;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;

import edu.ustb.sei.mde.mohash.EObjectHasher;
import edu.ustb.sei.mde.mohash.HashAdapter;

public class ConvolutionalHammingProximityEObjectMatcher extends AdapterEnhancedHammingProximityEObjectMatcher {

	public ConvolutionalHammingProximityEObjectMatcher(DistanceFunction meter) {
		super(meter);
	}

	public ConvolutionalHammingProximityEObjectMatcher(DistanceFunction meter, Registry weightProviderRegistry) {
		super(meter, weightProviderRegistry);
	}

	public ConvolutionalHammingProximityEObjectMatcher(DistanceFunction meter, Registry weightProviderRegistry,
			double[] thresholds) {
		super(meter, weightProviderRegistry, thresholds);
	}
	
	@Override
	protected void doIndexing(Comparison comparison, Iterator<? extends EObject> leftEObjects,
			Iterator<? extends EObject> rightEObjects, Iterator<? extends EObject> originEObjects, Monitor monitor) {
		AdapterEnhancedHammingEObjectIndex index = (AdapterEnhancedHammingEObjectIndex) this.index;
		EObjectHasher hasher = index.getEObjectHasher();
		
		// first round
		firstRoundHashing(leftEObjects, hasher, Side.LEFT);
		firstRoundHashing(rightEObjects, hasher, Side.RIGHT);
		firstRoundHashing(originEObjects, hasher, Side.ORIGIN);
		
		// second round and indexing
		this.eObjectsToSide.forEach((o,side)->{
			long finalHash = convolutionIndex(o);
			index.index(o, finalHash, side);
		});
	}

	protected void firstRoundHashing(Iterator<? extends EObject> eObjects, EObjectHasher hasher, Side side) {
		while(eObjects.hasNext()) {
			EObject next = eObjects.next();
			long hash = hasher.hash(next);
			HashAdapter.make(next, hash);
			eObjectsToSide.put(next, side);
		}
	}
	
	static protected int[] buffer = new int[64]; 
	
	@SuppressWarnings("unchecked")
	protected long convolutionIndex(EObject object) {
		AdapterEnhancedHammingEObjectIndex index = (AdapterEnhancedHammingEObjectIndex) this.index;
		EObjectHasher hasher = index.getEObjectHasher();
		
		HashAdapter adapter = (HashAdapter) EcoreUtil.getExistingAdapter(object, HashAdapter.class);
		
		long localHash = adapter.getLocalHashCode();
		long finalHash = localHash;
		
		
		long accHash = 0;
		Arrays.fill(buffer, 0);
		
		for(EObject child : object.eContents()) {
			long childHash = HashAdapter.getLocalHash(child);
			if(childHash!=0) EObjectHasher.mergeHash(buffer, childHash, 10, 5);
		}
		
		for(int i=0;i<64;i++) {
			if(buffer[i]>0) accHash |= EObjectHasher.bitmasks[i];
		}
		
		finalHash |= accHash;
		
		accHash = 0;
		Arrays.fill(buffer, 0);
		Stream<EReference> references = object.eClass().getEAllReferences().stream().filter(r->!hasher.shouldSkip(r) && !r.isContainment());
		
		references.forEach(r->{
			if(r.isMany()) {
				List<EObject> relateds = (List<EObject>) object.eGet(r);
				relateds.forEach(related->{
					long relatedHash = HashAdapter.getLocalHash(related);
					if(relatedHash!=0) EObjectHasher.mergeHash(buffer, relatedHash, 5, 2);
				});
			} else {
				EObject related = (EObject) object.eGet(r);
				if(related!=null) {
					long relatedHash = HashAdapter.getLocalHash(related);
					if(relatedHash!=0) EObjectHasher.mergeHash(buffer, relatedHash, 5, 2);
				}
			}
		});
		
		for(int i=0;i<64;i++) {
			if(buffer[i]>0) accHash |= EObjectHasher.bitmasks[i];
		}
		
		finalHash |= accHash;
		
		adapter.setFinalHashCode(finalHash);
		
		return finalHash;
	}
	
	@Override
	protected Match areMatching(Comparison comparison, EObject left, EObject right, EObject origin) {
		return super.areMatching(comparison, left, right, origin);
	}

}
