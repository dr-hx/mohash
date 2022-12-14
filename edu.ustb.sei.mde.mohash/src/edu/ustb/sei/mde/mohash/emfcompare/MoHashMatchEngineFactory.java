package edu.ustb.sei.mde.mohash.emfcompare;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.emf.compare.match.DefaultComparisonFactory;
import org.eclipse.emf.compare.match.DefaultEqualityHelperFactory;
import org.eclipse.emf.compare.match.DefaultMatchEngine;
import org.eclipse.emf.compare.match.IComparisonFactory;
import org.eclipse.emf.compare.match.IMatchEngine;
import org.eclipse.emf.compare.match.IMatchEngine.Factory;
import org.eclipse.emf.compare.match.eobject.CachingDistance;
import org.eclipse.emf.compare.match.eobject.EditionDistance;
import org.eclipse.emf.compare.match.eobject.EqualityHelperExtensionProvider;
import org.eclipse.emf.compare.match.eobject.EqualityHelperExtensionProviderDescriptorRegistryImpl;
import org.eclipse.emf.compare.match.eobject.IEObjectMatcher;
import org.eclipse.emf.compare.match.eobject.ProximityEObjectMatcher.DistanceFunction;
import org.eclipse.emf.compare.match.eobject.WeightProvider;
import org.eclipse.emf.compare.match.eobject.WeightProviderDescriptorRegistryImpl;
import org.eclipse.emf.compare.match.impl.MatchEngineFactoryImpl;
import org.eclipse.emf.compare.match.impl.MatchEngineFactoryRegistryImpl;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.compare.utils.UseIdentifiers;
import org.eclipse.emf.ecore.EClass;

import edu.ustb.sei.mde.mohash.EObjectSimHasher;
import edu.ustb.sei.mde.mohash.HWTreeBasedIndex;
import edu.ustb.sei.mde.mohash.HammingIndex;
import edu.ustb.sei.mde.mohash.ObjectIndex;
import edu.ustb.sei.mde.mohash.TypeMap;

public class MoHashMatchEngineFactory implements Factory {
	/** The match engine created by this factory. */
	protected IMatchEngine matchEngine;

	/** Ranking of this match engine. */
	private int ranking;

	/** A match engine needs a WeightProvider in case of this match engine do not use identifiers. */
	private WeightProvider.Descriptor.Registry weightProviderRegistry;

	public WeightProvider.Descriptor.Registry getWeightProviderRegistry() {
		return weightProviderRegistry;
	}

	/** A match engine may need a specific equality helper extension provider. */
	private EqualityHelperExtensionProvider.Descriptor.Registry equalityHelperExtensionProviderRegistry;
	
	private boolean convolutional = false;
	
	private Function<EClass, ObjectIndex> objectIndexBuilder = (t) -> new HammingIndex();
	
	public void setObjectIndexBuilder(Function<EClass, ObjectIndex> objectIndexBuilder) {
		this.objectIndexBuilder = objectIndexBuilder;
	}

	public void setConvolutional(boolean convolutional) {
		this.convolutional = convolutional;
	}

	public MoHashMatchEngineFactory() {
		this(WeightProviderDescriptorRegistryImpl.createStandaloneInstance(),
				EqualityHelperExtensionProviderDescriptorRegistryImpl.createStandaloneInstance());
	}
	
	public MoHashMatchEngineFactory(WeightProvider.Descriptor.Registry weightProviderRegistry) {
		this(weightProviderRegistry,
				EqualityHelperExtensionProviderDescriptorRegistryImpl.createStandaloneInstance());
	}
	
	public MoHashMatchEngineFactory(WeightProvider.Descriptor.Registry weightProviderRegistry,
			EqualityHelperExtensionProvider.Descriptor.Registry equalityHelperExtensionProviderRegistry) {
		this.weightProviderRegistry = weightProviderRegistry;
		this.equalityHelperExtensionProviderRegistry = equalityHelperExtensionProviderRegistry;
	}
	
	private TypeMap<Double> thresholdMap = new TypeMap<>(0.5);
	
	private Set<EClass> ignoredClasses = Collections.emptySet();
	
	public void setIgnoredClasses(Set<EClass> ignoredClasses) {
		this.ignoredClasses = ignoredClasses;
	}

	public void setThresholdMap(TypeMap<Double> thresholdMap) {
		this.thresholdMap = thresholdMap;
	}
	
	public void setThreshold(double defaultThreshold) {
		this.thresholdMap = new TypeMap<Double>(defaultThreshold);
	}

	private DistanceFunction distance;
	private SimHashProximityEObjectMatcher matcher ;

	public DistanceFunction getDistance() {
		if(distance==null) {
			distance = new EditionDistance(weightProviderRegistry, equalityHelperExtensionProviderRegistry);
		}
		return distance;
	}
	
	public EObjectSimHasher getHasher() {
		if(matcher==null) return null;
		else return matcher.getIndex().getEObjectHasher();
	}

	@Override
	public IMatchEngine getMatchEngine() {
		if (matchEngine == null) {
			final IComparisonFactory comparisonFactory = new DefaultComparisonFactory(
					new DefaultEqualityHelperFactory());
			
			final ReasonableCachingDistance cachedDistance = new ReasonableCachingDistance(getDistance());
			
			if(convolutional) matcher = new ConvolutionalSimHashProximityEObjectMatcher(cachedDistance, weightProviderRegistry, thresholdMap);
			else matcher = new SimHashProximityEObjectMatcher(cachedDistance, this.weightProviderRegistry, thresholdMap, objectIndexBuilder);
			
			matcher.setIgnoredClasses(ignoredClasses);
			
			matchEngine = new DefaultMatchEngine(matcher, comparisonFactory);
		}
		return matchEngine;
	}
	
	public void reset() {
		matchEngine = null;
	}
	
//	private double[] thresholds = null;
//
//	public double[] getThresholds() {
//		return thresholds;
//	}
//
//	public void setThresholds(double[] thresholds) {
//		this.thresholds = thresholds;
//	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.match.IMatchEngine.Factory#getRanking()
	 */
	public int getRanking() {
		return ranking;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.match.IMatchEngine.Factory#setRanking(int)
	 */
	public void setRanking(int r) {
		ranking = r;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.match.IMatchEngine.Factory#isMatchEngineFactoryFor(org.eclipse.emf.compare.scope.IComparisonScope)
	 */
	public boolean isMatchEngineFactoryFor(IComparisonScope scope) {
		return true;
	}

	/**
	 * The match engine needs a WeightProvider in case of this match engine do not use identifiers.
	 * 
	 * @param registry
	 *            the registry to associate with the match engine.
	 */
	void setWeightProviderRegistry(WeightProvider.Descriptor.Registry registry) {
		this.weightProviderRegistry = registry;
	}

	/**
	 * The match engine may need a Equality Helper Extension.
	 * 
	 * @param equalityHelperExtensionProviderRegistry
	 *            the registry to associate with the match engine.
	 */
	public void setEqualityHelperExtensionProviderRegistry(
			EqualityHelperExtensionProvider.Descriptor.Registry equalityHelperExtensionProviderRegistry) {
		this.equalityHelperExtensionProviderRegistry = equalityHelperExtensionProviderRegistry;
	}
	
	static public MoHashMatchEngineFactory matchEngineFactory;
	
	static public IMatchEngine.Factory.Registry createFactoryRegistry(boolean convolutional, WeightProvider.Descriptor.Registry weightProviderRegistry, TypeMap<Double> thresholdMap) {
		IMatchEngine.Factory.Registry reg = MatchEngineFactoryRegistryImpl.createStandaloneInstance();
		
		if(weightProviderRegistry!=null) matchEngineFactory = new MoHashMatchEngineFactory(weightProviderRegistry);
		else matchEngineFactory = new MoHashMatchEngineFactory();
		
		matchEngineFactory.setConvolutional(convolutional);
		
		if(thresholdMap!=null) matchEngineFactory.setThresholdMap(thresholdMap);
		
		matchEngineFactory.setRanking(20);
		reg.add(matchEngineFactory);
		return reg;
	}
	
	static public IMatchEngine.Factory.Registry createFactoryRegistry() {
		return createFactoryRegistry(false, null, null);
	}
	
	static public IMatchEngine.Factory.Registry createEMFCompareFactoryRegistry() {
		final IMatchEngine.Factory.Registry registry = new MatchEngineFactoryRegistryImpl();
		org.eclipse.emf.compare.match.eobject.WeightProvider.Descriptor.Registry weightInstance = WeightProviderDescriptorRegistryImpl.createStandaloneInstance();
		final MatchEngineFactoryImpl matchEngineFactory = new MatchEngineFactoryImpl(UseIdentifiers.NEVER, weightInstance);
		matchEngineFactory.setRanking(10);
		registry.add(matchEngineFactory);
		return registry;
	}
	
	static public IMatchEngine.Factory createEMFCompareFactory() {
		org.eclipse.emf.compare.match.eobject.WeightProvider.Descriptor.Registry weightInstance = WeightProviderDescriptorRegistryImpl.createStandaloneInstance();
		final MatchEngineFactoryImpl matchEngineFactory = new MatchEngineFactoryImpl(UseIdentifiers.NEVER, weightInstance);
		return matchEngineFactory;
	}

}
