package edu.ustb.sei.mde.mohash.emfcompare;

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
import org.eclipse.emf.compare.match.eobject.WeightProvider;
import org.eclipse.emf.compare.match.eobject.WeightProviderDescriptorRegistryImpl;
import org.eclipse.emf.compare.match.impl.MatchEngineFactoryImpl;
import org.eclipse.emf.compare.match.impl.MatchEngineFactoryRegistryImpl;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.compare.utils.UseIdentifiers;

public class MoHashMatchEngineFactory implements Factory {
	/** The match engine created by this factory. */
	protected IMatchEngine matchEngine;

	/** Ranking of this match engine. */
	private int ranking;

	/** A match engine needs a WeightProvider in case of this match engine do not use identifiers. */
	private WeightProvider.Descriptor.Registry weightProviderRegistry;

	/** A match engine may need a specific equality helper extension provider. */
	private EqualityHelperExtensionProvider.Descriptor.Registry equalityHelperExtensionProviderRegistry;
	
	private boolean convolutional = false;
	
	
	
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

	@Override
	public IMatchEngine getMatchEngine() {
		if (matchEngine == null) {
			final IComparisonFactory comparisonFactory = new DefaultComparisonFactory(
					new DefaultEqualityHelperFactory());
			
			final EditionDistance editionDistance = new EditionDistance(weightProviderRegistry, equalityHelperExtensionProviderRegistry);
			final CachingDistance cachedDistance = new CachingDistance(editionDistance);
			final IEObjectMatcher matcher ;
			
			if(convolutional) matcher = new ConvolutionalHammingProximityEObjectMatcher(cachedDistance, weightProviderRegistry, thresholds);
			else matcher = new HammingProximityEObjectMatcher(cachedDistance, this.weightProviderRegistry, thresholds);
			
			matchEngine = new DefaultMatchEngine(matcher, comparisonFactory);
		}
		return matchEngine;
	}
	
	private double[] thresholds = null;

	public double[] getThresholds() {
		return thresholds;
	}

	public void setThresholds(double[] thresholds) {
		this.thresholds = thresholds;
	}

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
	
	static public IMatchEngine.Factory.Registry createFactoryRegistry(boolean convolutional, WeightProvider.Descriptor.Registry weightProviderRegistry, double[] thresholds) {
		IMatchEngine.Factory.Registry reg = MatchEngineFactoryRegistryImpl.createStandaloneInstance();
		
		if(weightProviderRegistry!=null) matchEngineFactory = new MoHashMatchEngineFactory(weightProviderRegistry);
		else matchEngineFactory = new MoHashMatchEngineFactory();
		
		matchEngineFactory.setConvolutional(convolutional);
		
		if(thresholds!=null) matchEngineFactory.setThresholds(thresholds);
		
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

}
