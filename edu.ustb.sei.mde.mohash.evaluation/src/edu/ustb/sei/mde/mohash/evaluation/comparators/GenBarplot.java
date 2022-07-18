package edu.ustb.sei.mde.mohash.evaluation.comparators;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenBarplot {

	public static void main(String[] args) {
		GenBarplot gen = new GenBarplot();
		gen.generate(new File("C:\\JavaProjects\\git\\mohash\\edu.ustb.sei.mde.mohash.evaluation\\output\\uml_top"), System.out);
	}
	
	private List<ModelData> modelDataList = new ArrayList<>();
	private Pattern pattern1 = Pattern.compile("Time:    Avg\\(MoHash\\)=([\\.0-9]+)    Avg\\(EMFComp\\)=([\\.0-9]+)");
	private Pattern pattern2 = Pattern.compile("Diff Rate:    Total=([\\.0-9]+)%    Critical=([\\.0-9]+)%");
	
	
	public void generate(File folder, PrintStream out) {
		try {
			extractFromFolder(folder);
			
			printBarplot(out);
			
			printSummary(out);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String wrapModelName(String str) {
		return str.replaceAll("_", "\\\\_").replaceAll("@", "\\\\@");
	}
	
	private void printSummary(PrintStream out) {
		double timeReduce = modelDataList.stream().map(d->d.timeReductionRate).reduce(0.0, (l,r)->l+r) / modelDataList.size();
		double disRate = modelDataList.stream().map(d->d.disagreementRate).reduce(0.0, (l,r)->l+r) / modelDataList.size();
		out.println("AvgTimeReduce="+timeReduce);
		out.println("AvgDARate="+disRate);
	}

	private void printBarplot(PrintStream out) {
		out.println("%% Barplot data starts here");
		out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		out.println();
		
		out.print("\\timebarplot{4cm}{6cm}{");
		out.print(modelDataList.stream().map(d->wrapModelName(d.modelName)).reduce((l,r)->l+","+r).orElse(""));
		out.println("}{");
		out.print("\\timebars{");
		out.print(modelDataList.stream().map(d->"("+wrapModelName(d.modelName)+","+d.emfcTime+")").reduce((l,r)->l+" "+r).orElse(""));
		out.println("} % EMF-C time");
		out.print("\\timebars{");
		out.print(modelDataList.stream().map(d->"("+wrapModelName(d.modelName)+","+d.oursTime+")").reduce((l,r)->l+" "+r).orElse(""));
		out.println("} % Ours time");
		out.println("}");
		
		out.println();
		out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		out.println("%% Barplot data ends here");
		out.println();
		out.println();
	}

	protected void extractFromFolder(File folder) throws IOException {
		for(File file : folder.listFiles((p,n)->{
			return n.endsWith(".log");
		})) {
			extractFromLog(file);
		}
	}
	
	protected void extractFromLog(File logFile) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(logFile));
		String line = null;
		boolean inSummary =false;
		
		double ours = 0;
		double emfc = 0;
		double daRate = 0;
		
		String modelName = logFile.getName().substring(0, logFile.getName().indexOf('.'));
		
		while((line=in.readLine())!=null) {
			if("====================[SUMMARY]=====================".equals(line)) {
				inSummary = true;
			} else if(inSummary) {
				Matcher matcher1 = pattern1.matcher(line);
				if(matcher1.matches()) {
					ours = Double.parseDouble(matcher1.group(1));
					emfc = Double.parseDouble(matcher1.group(2));
				} else {
					Matcher matcher2 = pattern2.matcher(line);
					if(matcher2.matches()) {
						daRate = Double.parseDouble(matcher2.group(1));
					}
				}
			}
		}
		
		ModelData data = new ModelData(modelName, emfc, ours, daRate);
		modelDataList.add(data);
		
		in.close();
	}
}


class ModelData {
	public ModelData(String modelName, double emfcTime, double oursTime, double disagreementRate) {
		super();
		this.modelName = modelName;
		this.emfcTime = emfcTime;
		this.oursTime = oursTime;
		this.timeReductionRate = (oursTime - emfcTime)/emfcTime * 100.0;
		this.disagreementRate = disagreementRate;
	}

	public String modelName;
	public double emfcTime;
	public double oursTime;
	public double timeReductionRate;
	
	public double disagreementRate;
}