package edu.ustb.sei.mde.mohash.evaluation.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.XMI2UMLResource;

import edu.ustb.sei.mde.mohash.evaluation.comparators.EvaluateComparator;
import edu.ustb.sei.mde.mohash.evaluation.comparators.GenBarplot;
import edu.ustb.sei.mde.mohash.evaluation.roc.CalcThresholds;
import edu.ustb.sei.mde.mohash.evaluation.roc.GenBoxplot;

public class EvaluationCLI {
	
	static public String KEY_TYPE = "type";
	static public String TYPE_ECORE = "ecore";
	static public String TYPE_UML = "uml";
	
	static public String KEY_FUNCTION = "function";
	static public String FUNCTION_COMPARE = "compare";
	static public String FUNCTION_ESTIMATE = "estimate";
	static public String FUNCTION_DRAWBOX = "boxplot";
	static public String FUNCTION_DRAWBAR = "barplot";
	
	static public String KEY_COUNT = "count";
	static public String KEY_DATASET = "dataset";
	static public String KEY_RAWRESULT = "result";
	static public String KEY_OUTPUT = "output";
	
	static protected Pattern singleParameterPattern = Pattern.compile("-(?<key>[a-zA-Z0-9]+)=(?<value>.+)");
	
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
	
	public static void main(String[] args) throws FileNotFoundException {
		Map<String,String> options = parseParameters(args);
		String func = options.get(KEY_FUNCTION);
		if(func==null) {
			error("Parameter '-function' must be specified!");
			return;
		}
		
		if(FUNCTION_ESTIMATE.equals(func)) {
			handleEstimation(options);
		} else if(FUNCTION_COMPARE.equals(func)) {
			handleComparison(options);
		} else if(FUNCTION_DRAWBOX.equals(func)) {
			handleBoxplot(options);
		} else if(FUNCTION_DRAWBAR.equals(func)) {
			handleBarplot(options);
		} else {
			error("Unknown function "+func);
		}
	}
	
	protected static void handleBoxplot(Map<String,String> options) throws FileNotFoundException {
		String result = options.get(KEY_RAWRESULT);
		File resultfolder = null;
		if(result==null) {
			error("Parameter '-result' must be specified!");
			return;
		}
		
		resultfolder = new File(result);
		if(resultfolder.exists()==false) {
			error("The result folder does not exist!");
			return;
		}
		
		String output = options.get(KEY_OUTPUT);
		PrintStream out;
		if(output==null) {
			out = System.out;
		} else {
			out = new PrintStream(output);
		}
		
		GenBoxplot gen = new GenBoxplot();
		gen.extractFromFolder(resultfolder, Set.of(), out);
		out.close();
	}
	
	protected static void handleBarplot(Map<String,String> options) throws FileNotFoundException {
		String result = options.get(KEY_RAWRESULT);
		File resultfolder = null;
		if(result==null) {
			error("Parameter '-result' must be specified!");
			return;
		}
		
		resultfolder = new File(result);
		if(resultfolder.exists()==false) {
			error("The result folder does not exist!");
			return;
		}
		
		String output = options.get(KEY_OUTPUT);
		PrintStream out;
		if(output==null) {
			out = System.out;
		} else {
			out = new PrintStream(output);
		}
		
		GenBarplot gen = new GenBarplot();
		gen.generate(resultfolder, out);
		out.close();
	}
	
	protected static void error(String msg) {
		System.err.println(msg);
	}
	
	protected static void handleComparison(Map<String,String> options) {
		String dataset = options.get(KEY_DATASET);
		if(dataset==null) {
			error("Parameter '-dataset' must be specified!");
			return;
		}
		
		File datasetfolder = new File(dataset);
		if(datasetfolder.exists()==false) {
			error("The dataset folder does not exist!");
			return;
		}
		
		String type = options.get(KEY_TYPE);
		if(type==null) {
			error("Parameter '-type' must be specified!");
			return;
		}
		ResourceSet  resourceSet;
		if(TYPE_ECORE.equals(type)) {
			resourceSet = createEcoreResourceSet();
		} else if(TYPE_UML.equals(type)) {
			resourceSet = createUMLResourceSet();
		} else {
			System.err.println("Unknow type "+type);
			return;
		}
		
		String result = options.get(KEY_RAWRESULT);
		File resultfolder = null;
		if(result==null) {
			resultfolder = makeFolder(new File(datasetfolder.getParent(),"result-cmp"));
			System.out.println("A default result folder will be used: "+resultfolder.getAbsolutePath());
		} else {
			resultfolder = makeFolder(result);
		}
		
		String count = options.getOrDefault(KEY_COUNT, "100");
		int countInt = 0;
		
		try {
			countInt = Integer.parseInt(count);
		} catch (Exception e) {
			error("count must be an integer!");
		}

		if(TYPE_ECORE.equals(type)) {		
			List<EPackage> models = Arrays.asList(datasetfolder.listFiles()).stream().filter(it->it.getName().endsWith(".ecore"))
					.map(it->{
						Resource resource = resourceSet.getResource(URI.createFileURI(it.getAbsolutePath()), true);
						return (EPackage) resource.getContents().get(0);
					}).toList();
			EvaluateComparator.evaluateEcoreModels(models, countInt, resultfolder);
		} else if(TYPE_UML.equals(type)) {
			List<Resource> models = Arrays.asList(datasetfolder.listFiles()).stream().filter(it->it.getName().endsWith(".xmi"))
					.map(it->resourceSet.getResource(URI.createFileURI(it.getAbsolutePath()), true)).toList();
			EvaluateComparator.evaluateUMLModels(models, countInt, resultfolder);
		}
	}
	
	protected static void handleEstimation(Map<String,String> options) {
		String dataset = options.get(KEY_DATASET);
		if(dataset==null) {
			error("Parameter '-dataset' must be specified!");
			return;
		}
		
		File datasetfolder = new File(dataset);
		if(datasetfolder.exists()==false) {
			error("The dataset folder does not exist!");
			return;
		}
		
		String type = options.get(KEY_TYPE);
		if(type==null) {
			error("Parameter '-type' must be specified!");
			return;
		}
		
		String result = options.get(KEY_RAWRESULT);
		File resultfolder = null;
		if(result==null) {
			resultfolder = makeFolder(new File(datasetfolder.getParent(),"result-est"));
			System.out.println("A default result folder will be used: "+resultfolder.getAbsolutePath());
		} else {
			resultfolder = makeFolder(result);
		}
		
		
		if(TYPE_ECORE.equals(type)) {		
			CalcThresholds.estimateEcore(datasetfolder.getAbsolutePath(), resultfolder.getAbsolutePath());
		} else if(TYPE_UML.equals(type)) {
			CalcThresholds.estimateUML(datasetfolder.getAbsolutePath(), resultfolder.getAbsolutePath());
		}
	}
	
	protected static File makeFolder(String path) {
		File folder = new File(path);
		return makeFolder(folder);
	}

	protected static File makeFolder(File folder) {
		if(folder.exists()==false) folder.mkdirs();
		return folder;
	}

}
