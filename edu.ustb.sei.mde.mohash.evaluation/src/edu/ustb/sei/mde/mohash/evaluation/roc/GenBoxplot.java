package edu.ustb.sei.mde.mohash.evaluation.roc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EClass;

/*
 * for each log file in the given folder
 * 	extract the summary part, put the best configuration for each type into a multimap
 * for each type in the map
 * 	sort the configurations by the threshold, and compute the five values
 *  
 *
 */
public class GenBoxplot {

	public static void main(String[] args) {
		GenBoxplot gen = new GenBoxplot();
//		gen.extractFromFolder(new File("C:/JavaProjects/git/mohash/edu.ustb.sei.mde.mohash.evaluation/model/ecore/est"), Set.of("EAnnotation", "EGenericType", "EStringToStringMapEntry"));
		
		// "Abstraction",Activity,"ActivityFinalNode",Actor,Association,"BehaviorExecutionSpecification",Class,"Collaboration","Comment",Component,ControlFlow,"DataType",DecisionNode,"Dependency",Enumeration,"EnumerationLiteral","ExecutionOccurrenceSpecification","Extend","ExtensionPoint","FlowFinalNode","ForkNode","GeneralOrdering","Generalization","Include","InitialNode","InstanceSpecification",Interaction,Interface,"InterfaceRealization","JoinNode",Lifeline,"LiteralInteger","LiteralString","LiteralUnlimitedNatural","MergeNode","Message","MessageOccurrenceSpecification","Model","OccurrenceSpecification","OpaqueAction","Operation",Package,"PackageImport","Parameter","Property",Realization,"Usage",UseCase
		
		gen.extractFromFolder(new File("C:/JavaProjects/git/mohash/edu.ustb.sei.mde.mohash.evaluation/model/uml/est"),  Set.of());//Set.of("Package","Abstraction","ActivityFinalNode","Association","BehaviorExecutionSpecification","Collaboration","Comment","DataType","Dependency","EnumerationLiteral","ExecutionOccurrenceSpecification","Extend","ExtensionPoint","FlowFinalNode","ForkNode","GeneralOrdering","Generalization","Include","InitialNode","InstanceSpecification","InterfaceRealization","JoinNode","LiteralInteger","LiteralString","LiteralUnlimitedNatural","MergeNode","Message","MessageOccurrenceSpecification","Model","OccurrenceSpecification","OpaqueAction","Operation","PackageImport","Parameter","Property","Usage"));
	}
	
	private Pattern dataPattern = Pattern.compile("\\[([^\\]]+)\\]    \\[([\\w]+)\\_([\\w]+)\\]    threshold=(\\d+\\.\\d+)    TPR=(\\d+\\.\\d+)    FPR=(\\d+\\.\\d+)    Prec=(\\d+\\.\\d+)    Recall=(\\d+\\.\\d+)    Acc=(\\d+\\.\\d+)    F2=(\\d+\\.\\d+)");
	private Pattern dataPattern2 = Pattern.compile("\\[([^\\]]+)\\]    \\[OneHot\\]    threshold=(\\d+\\.\\d+)    TPR=(\\d+\\.\\d+)    FPR=(\\d+\\.\\d+)    Prec=(\\d+\\.\\d+)    Recall=(\\d+\\.\\d+)    Acc=(\\d+\\.\\d+)    F2=(\\d+\\.\\d+)");
	
	public void extractFromFolder(File folder, Set<String> ignoredTypes) {
		File[] logs = folder.listFiles((d,n)->n.endsWith("log"));
		for(File file : logs) {
			try {
				extractFromFile(file);
			} catch (IOException e) {
				System.out.println("error at "+file.getName());
			}
		}
		
		List<String> list = this.typenames.stream().collect(Collectors.toList());
		list.removeAll(ignoredTypes);
		Collections.sort(list);
		
		f2Data.printPlots(System.out, list);
		System.out.println();
		System.out.println();
		thData.printTables(System.out, list, f2Data);
	}
	
	public void extractFromFile(File file) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line = null;
		boolean inSummary = false;
		while((line=in.readLine())!=null) {
			if(line.contains("=====================[SUMMARY]======================")) {
				inSummary = true;
			} else if(inSummary) {
				if(line.contains("=====================[CONFIG]======================")) {
					break;
				} else if(line.contains("====================================================")) {
					continue;
				} else {
					Matcher matcher = dataPattern.matcher(line);
					if(matcher.matches()) {
						String type = matcher.group(1);
						String hashMet = matcher.group(2);
						String simMet = matcher.group(3);
						String th = matcher.group(4);
						String tpr = matcher.group(5);
						String fpr = matcher.group(6);
						String pr = matcher.group(7);
						String recall = matcher.group(8);
						String Acc = matcher.group(9);
						String F2 = matcher.group(10);
						
						f2Data.put(hashMet, simMet, type, Double.parseDouble(F2));
						thData.put(hashMet, simMet, type, Double.parseDouble(th), Double.parseDouble(tpr), Double.parseDouble(fpr), Double.parseDouble(pr), Double.parseDouble(recall),Double.parseDouble(Acc));
						typenames.add(type);
					} else {
						Matcher matcher2 = dataPattern2.matcher(line);
						if(matcher2.matches()) {
							String type = matcher2.group(1);
							String hashMet = "OHR";
							String simMet = "Jac";
							String th = matcher2.group(2);
							String tpr = matcher2.group(3);
							String fpr = matcher2.group(4);
							String pr = matcher2.group(5);
							String recall = matcher2.group(6);
							String Acc = matcher2.group(7);
							String F2 = matcher2.group(8);
							
							f2Data.put(hashMet, simMet, type, Double.parseDouble(F2));
							thData.put(hashMet, simMet, type, Double.parseDouble(th), Double.parseDouble(tpr), Double.parseDouble(fpr), Double.parseDouble(pr), Double.parseDouble(recall),Double.parseDouble(Acc));
							typenames.add(type);
						}
					}
				}
			}
		}
		in.close();
	}
	
	private ConfigurationMap f2Data = new ConfigurationMap();
	private ConfigurationDataMap thData = new ConfigurationDataMap();
	private Set<String> typenames = new HashSet<>();
}

class ConfigurationData {
	private int count = 0;
	private double sumTh = 0;
	private double sumTPR = 0;
	private double sumFPR = 0;
	private double sumPr = 0;
	private double sumRecall = 0;
	private double sumAcc = 0;
	
	public void add(double th, double tpr, double fpr, double pr, double recall, double acc) {
		count ++;
		sumTh += th;
		sumTPR += tpr;
		sumFPR += fpr;
		sumPr += pr;
		sumRecall += recall;
		sumAcc += acc;
//		count = 1;
//		if(sumTh==0 || sumTh > th) {
//			sumTh = th;
//			sumTPR = tpr;
//			sumFPR = fpr;
//			sumPr = pr;
//			sumRecall = recall;
//			sumAcc = acc;
//		}
	}
	
	public void printRow(PrintStream out) {
		out.print(String.format("%.2f & %.2f & %.2f & %.2f", sumTh / count, sumTPR / count, sumPr / count, sumAcc / count));
	}
}

class ConfigurationDataMap {
	private Map<String, Map<String, Map<String, ConfigurationData>>> map = new HashMap<>();
	public void put(String hashMethod, String simMethod, String type, double th, double tpr, double fpr, double pr, double recall, double acc) {
		Map<String, Map<String, ConfigurationData>> simMap = map.computeIfAbsent(hashMethod, (x)->new HashMap<>());
		Map<String, ConfigurationData> typeMap = simMap.computeIfAbsent(simMethod, (x)->new HashMap<>());
		ConfigurationData list = typeMap.computeIfAbsent(type, (t)->new ConfigurationData());
		list.add(th, tpr, fpr, pr, recall, acc);
	}
	
	public void printTables(PrintStream out, List<String> typenames, ConfigurationMap f2Data) {
		Function<String, String> hash = (x)->{
			if("LSH".equals(x)) return "\\mohn";
			else return "OHR";
		};
		
		map.forEach((hashMet, simMap)->{
			simMap.forEach((simMet, typeMap)->{
				out.println("% "+hash.apply(hashMet)+", "+simMet.toLowerCase());
				out.println("\\begin{tabular}{c|c|c|c|c|c}");
				out.println("\\hline");
				out.println("Type & {F2-scores} & Avg. $\\mathcal{T}_H$ & Avg. TPR & Avg. Pr & Avg. Ac \\\\");
				out.println("\\hline");
				
				for(int i=0;i<typenames.size();i++) {
					ConfigurationData rawResult = typeMap.get(typenames.get(i));
					out.print(typenames.get(i));
					out.print(" & ");
					out.print(f2Data.getF2Range(hashMet, simMet, typenames.get(i)));
					out.print(" & ");
					rawResult.printRow(out);
					out.println("\\\\");
				}
				
				out.println("\\hline");
				out.println("\\end{tabular}");
			});
		});
	}
}

class ConfigurationMap {
	private Map<String, Map<String, Map<String, java.util.List<Double>>>> map = new HashMap<>();
	private Map<String, Map<String, Map<String, BoxplotData>>> plotData = new HashMap<>();
	
	public void put(String hashMethod, String simMethod, String type, Double best) {
		Map<String, Map<String, java.util.List<Double>>> simMap = map.computeIfAbsent(hashMethod, (x)->new HashMap<>());
		Map<String, java.util.List<Double>> typeMap = simMap.computeIfAbsent(simMethod, (x)->new HashMap<>());
		List<Double> list = typeMap.computeIfAbsent(type, (t)->new ArrayList<>());
		list.add(best);
	}
	
	private void putPlot(String hashMethod, String simMethod, String type, BoxplotData data) {
		Map<String, Map<String, BoxplotData>> simMap = plotData.computeIfAbsent(hashMethod, (x)->new HashMap<>());
		Map<String, BoxplotData> typeMap = simMap.computeIfAbsent(simMethod, (x)->new HashMap<>());
		typeMap.put(type, data);
	}
	
	public String getF2Range(String hashMethod, String simMethod, String type) {
		Map<String, Map<String, BoxplotData>> simMap = plotData.computeIfAbsent(hashMethod, (x)->new HashMap<>());
		Map<String, BoxplotData> typeMap = simMap.computeIfAbsent(simMethod, (x)->new HashMap<>());
		BoxplotData data = typeMap.get(type);
		return data.getF2Range();
	}
	
	public void printPlots(PrintStream out, List<String> typenames) {
		Function<String, String> hash = (x)->{
			if("LSH".equals(x)) return "\\mohn";
			else return "OHR";
		};
		
		map.forEach((hashMet, simMap)->{
			simMap.forEach((simMet, typeMap)->{
				out.println("\\subfloat[$"+hash.apply(hashMet)+"+"+simMet.toLowerCase()+"$]{");
				out.println("\\thboxplot{4cm}{5cm}{");
				for(int i=0;i<typenames.size();i++) {
					out.print(i+1);
					if(i!=typenames.size()-1) out.print(",");
				}
				out.println("}{");
				out.print(typenames.stream().reduce((l,r)->l+","+r).get());
				out.println("}{");
				out.println("% "+hashMet+", "+simMet);
				for(int i=0;i<typenames.size();i++) {
					List<Double> rawResult = typeMap.getOrDefault(typenames.get(i), Collections.emptyList());
					BoxplotData data = BoxplotData.create(rawResult);
					out.print(data.toString());
					out.println("% "+typenames.get(i));
					
					putPlot(hashMet, simMet, typenames.get(i), data);
				}
				out.println("}}");
			});
		});
	}
}

class BoxplotData {
	private double lowerWhisker;
	private double lowerQuartile;
	private double median;
	private double upperQuartile;
	private double upperWhisker;
	
	private BoxplotData() {}
	
	public String getF2Range() {
		return String.format("%.2f--%.2f", lowerWhisker, upperWhisker);
	}

	static public BoxplotData create(List<Double> list) {
		BoxplotData data = new BoxplotData();
		
		Collections.sort(list);
		
		double Q1; //= (list.size() + 1)/4.0;
		double Q2; //= (list.size() + 1)/2.0;
		double Q3; //= 3*(list.size() + 1)/4.0;
		
		if(list.size()==1) {
			Q1 = Q2 = Q3 = 0;
		} else if(list.size()==2) {
			Q1 = 0.25;
			Q2 = 0.5;
			Q3 = 0.75;
		} else if(list.size()==3) {
			Q1 = 0.5;
			Q2 = 1.0;
			Q3 = 1.5;
		} else {
			Q1 = (list.size() + 1)/4.0 - 1;
			Q2 = (list.size() + 1)/2.0 - 1;
			Q3 = 3*(list.size() + 1)/4.0 - 1;
		}
		
		data.lowerQuartile = compute(list, Q1);
		data.median = compute(list, Q2);
		data.upperQuartile = compute(list, Q3);

		double IQR = (data.upperQuartile-data.lowerQuartile)*1.5;
		double LB = data.lowerQuartile - 1.5*IQR;
		double UB = data.upperQuartile + 1.5*IQR;
		
		int plb = computeLB(list, LB);
		data.lowerWhisker = list.get(plb);
		
		int pub = computeUB(list, UB);
		data.upperWhisker = list.get(list.size()-pub-1);
		
		return data;
	}
	
	static public int computeLB(List<Double> list, double lb) {
		int i=0;
		for(;i<list.size();i++) {
			if(list.get(i) < lb) continue;
			else return i;
		}
		return -1;
	}
	
	static public int computeUB(List<Double> list, double ub) {
		int i=0;
		for(;i<list.size();i++) {
			if(list.get(list.size() - i - 1) > ub) continue;
			else return i;
		}
		return -1;
	}
	
	static public double compute(List<Double> list, double position) {
		if(position < 0) return list.get(0);
		if(position >= list.size()) return list.get(list.size() - 1);
		
		
		double lb = Math.floor(position);
		double ub = Math.ceil(position);
		
		
		double ra = 1.0 - (position - lb);
		double rb = (position - lb);
		
		
		double rawL = list.get((int) lb);
		double rawU = list.get((int) ub);
		
		return rawL * ra + rawU * rb;
	}
	
	public String toString() {
		return String.format("\\thplot{%.2f}{%.2f}{%.2f}{%.2f}{%.2f}", lowerWhisker, lowerQuartile, median, upperQuartile, upperWhisker);
	}
}