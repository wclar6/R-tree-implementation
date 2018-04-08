package rtree;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Rtree {
	static int n = 0;
	static ArrayList<Integer> id = new ArrayList<Integer>(); 
	static ArrayList<Integer> x = new ArrayList<Integer>(); 
	static ArrayList<Integer> y = new ArrayList<Integer>(); 
	private int M;
	private int L;
	
	
	public static void main(String[] args) {
		loadDataset("C:/Users/wclar/Desktop/dataset.txt");

	}
	
	/**
	 * Create a 2D Rtree
	 * @param M max number of entries per node
	 * @param L minimum number of entries per node
	 */
	public Rtree(int M) {
		this.M = M;
		this.L = (int) Math.ceil(0.4 * M);
	}
	
	
	
	
	
	public static void loadDataset(String filePath) {
		long startTime = System.nanoTime();
		try {
		File file = new File(filePath);
		Scanner scanner = new Scanner(file);
		n = scanner.nextInt();
		for (int i = 0; i < n-1; i++) {
			id.add(scanner.nextInt());
			x.add(scanner.nextInt());
			y.add(scanner.nextInt());
		}
		scanner.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		long endTime = System.nanoTime();
		//Run time in miliseconds
		long duration = (endTime - startTime)/1000000;
		System.out.println("Runtime is " + duration + " Milliseconds");
	}
}
