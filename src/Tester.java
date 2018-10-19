package machineLearning;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.*;

public class Tester {
	public static void main(String [] args) 
	{
		Classifier classifier = new Classifier();
		
		//Training data for machine learning
		//a) Simple apple/orange example
//		int[][] trainingX= {{140,1},{130,1},{150,0},{170,0}}; //weight of fruit, bumpy-0/smooth-1
//		int[] trainingY= {0,0,1,1}; 						  //name of fruit: apple-0/orange-1
		
		//b) Hanabi, 61 games of 17+ score ... index1,players3
		int[][] trainingX= loadFeatures();
		int[] trainingY= loadLabels();
		
//		//Console tests
//		System.out.println("Features: "+Arrays.deepToString(loadFeatures()));
//		System.out.println("Labels: "+Arrays.toString(loadLabels()));
//		System.out.println("Features Size: "+loadFeatures().length+", Labels Size: "+loadLabels().length);
		
		classifier.fit(trainingX, trainingY);	//insert training data
		
		//Print fitted data
//		System.out.println("Features:");
//		for(int[] feature:classifier.getFeatures()) {
//			System.out.println("\tWeight: "+feature[0]+", Texture: "+feature[1]);
//		}
//		System.out.println("Features:");
//		for(int[] feature:classifier.getFeatures()) {
//			System.out.println("\tTurn: "+feature[0]+", Score: "+feature[1]);
//		}
//		System.out.println("Labels: "+Arrays.toString(classifier.getLabels())); 
		
		//Predict unknown object and print results
//		int[] testData = {50,1}; //unknown fruit with weight: 150, texture: 0 (lumpy)
		int[] testData = {1, 0, 3, 7, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 4, 3, 3, 1, 5, 1, 1, 4, 2, 4, 2, 1, 2, 1, 5, 3, 5, 1, 3, 1};
		int result=classifier.predict(testData); //make a prediction what this unknown fruit is
		System.out.println(result);
		
//		System.out.println("Unknown fruit\n\tWeight: "+testData[0]+", Texture: "+testData[1]);
//		if(result==1) {
//			System.out.println("\tPrediction: Orange");
//		} else {
//			System.out.println("\tPrediction: Apple");
//		}
	}
	
	public static int[][] loadFeatures(){
	    try {
	    	BufferedReader br = new BufferedReader(new FileReader("./dataset.txt"));
		    String line;
		    //get features array
		    List<int[]> features = new ArrayList<int[]>();		//{1, 2, 3}, {1, 2, 3}, {1, 2, 3}
		    line = br.readLine();
		    line=line.replaceAll("\\s+","");					//{1,2,3},{1,2,3},{1,2,3}
		    line=line.substring(1, line.length()-1);			//1,2,3}.{1,2,3}.{1,2,3}
		    String delimiter = "\\}\\,\\{"; 					//1,2,3   1,2,3   1,2,3
		    String[] featureStr = line.split(delimiter);		//	0		1		2	//featureStr array
		    for(String str:featureStr) {
		    	List<Integer> objList = new ArrayList<Integer>(); 
		    	String[] objFeatures = str.split("\\,");		//	'1'	'2'	'3'	//objFeatures String array
		    	for(String s:objFeatures) {						
		    		int n = Integer.valueOf(s);					//turns into actual ints
		    		objList.add(n);
		    	}
		    	int[] newFeatureObj = objList.stream().mapToInt(i->i).toArray();
		    	features.add(newFeatureObj);
		    }
		    //change features to int[][]
		    int[][] finalFeatures = new int[features.size()][];
		    for(int i=0; i<features.size();i++) {
		    	int[] row = features.get(i);
		    	finalFeatures[i] = row;
		    }
//		    //get labels array
//		    line = br.readLine();
//		    System.out.println("Labels: "+line.replaceAll("\\s+",""));
		    br.close();
		    return finalFeatures;
	    } 
	    catch (IOException e){
	    	e.printStackTrace();
	    }
	    return null;
    }
	
	public static int[] loadLabels(){
	    try {
	    	BufferedReader br = new BufferedReader(new FileReader("./dataset.txt"));
		    String line;
		    line = br.readLine();
		    line = br.readLine();					//	1, 2, 3
		    line = line.replaceAll("\\s+","");		//	1,2,3
		    String[] strArr = line.split("\\,");	//	'1'	'2'	'3'
		    int[] labels = new int[strArr.length];
		    for(int i=0;i<strArr.length;i++) {
		    	labels[i]=Integer.valueOf(strArr[i]);
		    }
		    br.close();
		    return labels;
	    } 
	    catch (IOException e){
	    	e.printStackTrace();
	    }
	    return null;
    }
	
}
