package edu.ustb.sei.mde.mohash.cli;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.match.IMatchEngine;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.compare.utils.EMFComparePrettyPrinter;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.XMI2UMLResource;

import edu.ustb.sei.mde.mohash.TypeMap;
import edu.ustb.sei.mde.mohash.emfcompare.MoHashMatchEngineFactory;

/**
 * <ul>
 * <li>-type=[uml|ecore]</li>
 * <li>-left=[uri]</li>
 * <li>-right=[uri]</li>
 * <li>-base=[uri]</li>
 * <li>-thresholds=[uri]</li>
 * <li>-output=[uri]</li>
 * </ul>
 * @author dr-he
 * 
 */
public class MoHashCLI {
	public static final String KEY_THRESHOLDS = "thresholds";

	public static final String KEY_BASE = "base";

	public static final String KEY_RIGHT = "right";

	public static final String KEY_LEFT = "left";

	public static final String KEY_TYPE = "type";
	
	public static final String KEY_OUTPUT = "output";

	public static final String TYPE_UML = "uml";

	public static final String TYPE_ECORE = "ecore";

	static protected Pattern singleParameterPattern = Pattern.compile("-(?<key>[a-zA-Z0-9]+)=(?<value>.+)");
	
	static protected Pattern weightFormat = Pattern.compile("\\s+(?<class>[a-zA-Z0-9]+)\\s+=\\s+(?<value>[0-9]+\\.[0-9]+)\\s+");
	
	
	static protected Map<String, String> parseParameters(String[] args) {
		Map<String, String> map = new HashMap<>();
		for(String arg : args) {
			Matcher matcher = singleParameterPattern.matcher(arg);
			if(matcher.matches()) {
				String key = matcher.group("key");
				String value = matcher.group("value");
				map.put(key, value);
			}
		}
		
		return map;
	}
	
	static protected ResourceSet createEcoreResourceSet() {
		ResourceSet resourceSet;
		resourceSet = new ResourceSetImpl();
		// Register the default resource factory -- only needed for stand-alone!
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
			Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		return resourceSet; 
	}
	
	static protected ResourceSet createUMLResourceSet() {
		ResourceSet resourceSet;
		resourceSet = new ResourceSetImpl();
		resourceSet.getPackageRegistry().put(UMLPackage.eINSTANCE.getNsURI(), UMLPackage.eINSTANCE);
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(XMI2UMLResource.FILE_EXTENSION, XMI2UMLResource.Factory.INSTANCE);
		return resourceSet;
	}
	
	static protected TypeMap<Double> parseThresholdsTable(EPackage metamodel, String file) throws Exception {
		TypeMap<Double> map = new TypeMap<Double>(0.5);
		try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			while((line=reader.readLine())!=null) {
				Matcher matcher = weightFormat.matcher(line);
				if(matcher.matches()) {
					String className = matcher.group("class");
					Double value = Double.parseDouble(matcher.group("value"));
					
					if("default".equals(className)) {
						map.setDefault(value);
					} else {
						EClass clazz = (EClass) metamodel.getEClassifier(className);
						if(clazz==null && metamodel!=UMLPackage.eINSTANCE) {
							clazz = (EClass) EcorePackage.eINSTANCE.getEClassifier(className);
						}
						
						if(clazz==null) {
							System.err.println("Cannot find EClass named "+className);
						} else {
							map.put(clazz, value);
						}
					}
				}
			}
			reader.close();
		} catch(Exception e) {
			System.err.println("Failed to parse thresholds table! Use default values.");
			map = null;
		}
		return map;
	}
	
	static protected TypeMap<Double> buildEcoreMap(Map<String, String> options) throws Exception {
		String weightFile = options.get(KEY_THRESHOLDS);
		TypeMap<Double> map = null;
		if(weightFile!=null) {
			map = parseThresholdsTable(EcorePackage.eINSTANCE, weightFile);
		}
		
		if(map==null) {
			map = new TypeMap<Double>(0.5);
			map.put(EcorePackage.eINSTANCE.getEPackage(), 0.46);
			map.put(EcorePackage.eINSTANCE.getEClass(), 0.59);
			map.put(EcorePackage.eINSTANCE.getEReference(), 0.74);
			map.put(EcorePackage.eINSTANCE.getEAttribute(), 0.72);
			map.put(EcorePackage.eINSTANCE.getEEnum(), 0.52);
			map.put(EcorePackage.eINSTANCE.getEEnumLiteral(), 0.53);
			map.put(EcorePackage.eINSTANCE.getEOperation(), 0.61);
			map.put(EcorePackage.eINSTANCE.getEParameter(), 0.66);
			map.put(EcorePackage.eINSTANCE.getEStringToStringMapEntry(), 0.4);
		}
		
		return map;
	}
	
	static protected TypeMap<Double> buildUMLMap(Map<String, String> options) throws Exception {
		String weightFile = options.get(KEY_THRESHOLDS);
		TypeMap<Double> map = null;
		if(weightFile!=null) {
			map = parseThresholdsTable(UMLPackage.eINSTANCE, weightFile);
		}
		
		if(map==null) {
			map = new TypeMap<Double>(0.5);
			map.put(EcorePackage.eINSTANCE.getEPackage(), 0.15);
			map.put(EcorePackage.eINSTANCE.getEClass(), 0.55);
			map.put(EcorePackage.eINSTANCE.getEReference(), 0.65);
			map.put(EcorePackage.eINSTANCE.getEOperation(), 0.55);
			map.put(EcorePackage.eINSTANCE.getEAttribute(), 0.68);
			map.put(EcorePackage.eINSTANCE.getEStringToStringMapEntry(), 0.4);
			map.put(EcorePackage.eINSTANCE.getEEnum(), 0.5);
			map.put(EcorePackage.eINSTANCE.getEEnumLiteral(), 0.45);
			map.put(EcorePackage.eINSTANCE.getEParameter(), 0.5);
			map.put(UMLPackage.eINSTANCE.getActivity(), 0.76);
			map.put(UMLPackage.eINSTANCE.getActor(), 0.63);
			map.put(UMLPackage.eINSTANCE.getClass_(), 0.67);
			map.put(UMLPackage.eINSTANCE.getComponent(), 0.77);
			map.put(UMLPackage.eINSTANCE.getAssociation(), 0.5);
			map.put(UMLPackage.eINSTANCE.getBehaviorExecutionSpecification(), 0.77);
			map.put(UMLPackage.eINSTANCE.getCollaboration(), 0.67);
			map.put(UMLPackage.eINSTANCE.getDataType(), 0.61);
			map.put(UMLPackage.eINSTANCE.getDependency(), 0.60);
			map.put(UMLPackage.eINSTANCE.getElementImport(), 0.65);
			map.put(UMLPackage.eINSTANCE.getEnumeration(), 0.59);
			map.put(UMLPackage.eINSTANCE.getEnumerationLiteral(), 0.51);
			map.put(UMLPackage.eINSTANCE.getExecutionOccurrenceSpecification(), 0.71);
			map.put(UMLPackage.eINSTANCE.getGeneralOrdering(), 0.78);
			map.put(UMLPackage.eINSTANCE.getInteraction(), 0.74);
			map.put(UMLPackage.eINSTANCE.getInstanceSpecification(), 0.5);
			map.put(UMLPackage.eINSTANCE.getLifeline(), 0.57);
			map.put(UMLPackage.eINSTANCE.getMessage(), 0.67);
			map.put(UMLPackage.eINSTANCE.getMessageOccurrenceSpecification(), 0.73);
			map.put(UMLPackage.eINSTANCE.getModel(), 0.44);
			map.put(UMLPackage.eINSTANCE.getOccurrenceSpecification(), 0.72);
			map.put(UMLPackage.eINSTANCE.getOperation(), 0.66);
			map.put(UMLPackage.eINSTANCE.getPackage(), 0.41);
			map.put(UMLPackage.eINSTANCE.getParameter(), 0.67);
			map.put(UMLPackage.eINSTANCE.getProperty(), 0.76);
			map.put(UMLPackage.eINSTANCE.getStateMachine(), 0.55);
			map.put(UMLPackage.eINSTANCE.getUseCase(), 0.72);
			map.put(UMLPackage.eINSTANCE.getUsage(), 0.47);
			map.put(UMLPackage.eINSTANCE.getAbstraction(), 0.67);
			map.put(UMLPackage.eINSTANCE.getActivityFinalNode(), 0.73);
			map.put(UMLPackage.eINSTANCE.getDecisionNode(), 0.68);
			map.put(UMLPackage.eINSTANCE.getFlowFinalNode(), 0.68);
			map.put(UMLPackage.eINSTANCE.getForkNode(), 0.67);
			map.put(UMLPackage.eINSTANCE.getJoinNode(), 0.72);
			map.put(UMLPackage.eINSTANCE.getInclude(), 0.74);
			map.put(UMLPackage.eINSTANCE.getInitialNode(), 0.69);
			map.put(UMLPackage.eINSTANCE.getInterface(), 0.67);
			map.put(UMLPackage.eINSTANCE.getLiteralUnlimitedNatural(), 0.50);
			map.put(UMLPackage.eINSTANCE.getLiteralInteger(), 0.50);
			map.put(UMLPackage.eINSTANCE.getLiteralString(), 0.20);
		}
		
		return map;
	}

	public static void main(String[] args) throws Exception {
		Map<String, String> options = parseParameters(args);
		
		String type = options.get(KEY_TYPE);
		String left = options.get(KEY_LEFT);
		String right = options.get(KEY_RIGHT);
		String base = options.get(KEY_BASE);
		
		if(left==null || right==null) {
			System.err.println("Both left and right files are required!");
			return;
		}
		
		if(type==null) {
			if(left.endsWith(".ecore")) type = TYPE_ECORE;
			else type = TYPE_UML;
		}
		
		TypeMap<Double> thresholds;
		MoHashMatchEngineFactory factory = new MoHashMatchEngineFactory();
		Set<EClass> nohashTypes = new HashSet<>();
		ResourceSet resourceSet;
				
		if(TYPE_ECORE.equals(type)) {
			thresholds = buildEcoreMap(options);
			nohashTypes = Set.of(
					EcorePackage.eINSTANCE.getEAnnotation(), 
					EcorePackage.eINSTANCE.getEGenericType(), 
					EcorePackage.eINSTANCE.getEFactory(), 
					EcorePackage.eINSTANCE.getEStringToStringMapEntry());
			resourceSet = createEcoreResourceSet();
		} else if(TYPE_UML.equals(type)) {
			thresholds = buildUMLMap(options);
			nohashTypes = Set.of(
					EcorePackage.eINSTANCE.getEAnnotation(), 
					EcorePackage.eINSTANCE.getEGenericType(), 
					EcorePackage.eINSTANCE.getEFactory(), 
					EcorePackage.eINSTANCE.getEStringToStringMapEntry(),
					UMLPackage.eINSTANCE.getPackageImport(), 
					UMLPackage.eINSTANCE.getLiteralBoolean());
			resourceSet = createUMLResourceSet();
		} else {
			System.err.println("Unknow type "+type);
			return;
		}
		
		factory.setThresholdMap(thresholds);
		factory.setIgnoredClasses(nohashTypes);
		IMatchEngine mohash = factory.getMatchEngine();
		
		Resource leftModel = resourceSet.getResource(URI.createFileURI(left), true);
		Resource rightModel = resourceSet.getResource(URI.createFileURI(right), true);
		Resource baseModel = null;
		if(base!=null) 
			baseModel = resourceSet.getResource(URI.createFileURI(base), true);
		
		if(leftModel==null) {
			System.err.println("Cannot load left model");
			return;
		}
		
		if(rightModel==null) {
			System.err.println("Cannot load right model");
			return;
		}
		
		if(base!=null && baseModel==null) {
			System.err.println("Cannot load base model");
			return;
		}
		
		
		IComparisonScope mohashScope = new DefaultComparisonScope(leftModel, rightModel, baseModel);
		long mohashStart = System.nanoTime();
		Comparison result = mohash.match(mohashScope, new BasicMonitor());
		long mohashEnd = System.nanoTime();
		long mohashTime = (mohashEnd - mohashStart);
		System.out.println("Time consumed: "+((double)mohashTime) / 1000000.0+" ms");
		
		String output = options.get(KEY_OUTPUT);
		
		if(output!=null && output.endsWith(".xmi")) {
			URI outURI = URI.createFileURI(output);
			Resource out = resourceSet.createResource(outURI);
			out.getContents().add(result);
			out.save(null);
		} else {			
			PrintStream out = System.out;
			if(output!=null) {
				out = new PrintStream(output);
			}
			EMFComparePrettyPrinter.printComparison(result, System.out);
			if(out!=System.out) out.close();
		}
		
	}
}
