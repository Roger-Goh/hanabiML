package machineLearning;


public class Classifier {
	
	//Fields
	private int features[][]; //nested array [[140,1], [130,1], [150,0]]
	private int labels[]; // [0,0,1,1]
	
	//Constructor
	public Classifier() {}
	
	//Fits our training data for machine learning
	public void fit(int[][] features, int[] labels) {
		this.features = features;
		this.labels = labels;
	}
	
	//Predicts the label for an unknown object with known features
	public int predict(int[] testData) {
		int prediction = closest(testData);
		return prediction;
	}
	
	//Calculates how similar unknown object is to past observations/training data
	public int closest(int[] testData) {
		float best_dist = euc(testData,this.features[0]);
		int best_index = 0;
		for(int i=0; i<this.features.length;i++) {
			float dist = euc(testData, this.features[i]);
					if(dist<best_dist){	//if new calculated distance is shorter it means the data sets are closer
						best_dist = dist;
						best_index = i;
					}
		}
		return this.labels[best_index];
	}
	
	//Returns euclidean distance between two points in nth dimension
	public float euc(int[] a, int[] b) {
		float total = 0, diff;
		for(int i=0; i<a.length; i++) {
			diff = b[i] - a[i];
			total += diff * diff;
		}
		return (float) Math.sqrt(total);
	}
	
	
	//Get methods
	public int[][] getFeatures() {
		return features;
	}
	
	public int[] getLabels() {
		return labels;
	}
	
}
