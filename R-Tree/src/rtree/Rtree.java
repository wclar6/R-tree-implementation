package rtree;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Scanner;

public final class Rtree {
	static int n = 0;
	static ArrayList<Integer> id = new ArrayList<Integer>(); 
	static ArrayList<Integer> x = new ArrayList<Integer>(); 
	static ArrayList<Integer> y = new ArrayList<Integer>(); 
	static ArrayList<int[]> rangeQueries = new ArrayList<int[]>();
	static ArrayList<Integer> results = new ArrayList<Integer>();
	private static int M;
	private static Node root;
	private static int result;
	
	public static void main(String[] args) {
		if (args.length != 4) {
			System.out.println("Usage: datasetPath queryPath savePath m");
		}
		String datasetLocation = args[0];
		String queryLocation = args[1];
		String saveLocation = args[2];
		//Max number of points per node
		int m = Integer.parseInt(args[3]);
		
		loadDataset(datasetLocation);
		loadQueries(queryLocation);
		Rtree r = new Rtree(m);
		ArrayList<Integer> storeResults = new ArrayList<Integer>();
		long startTime = System.nanoTime();
		for (int i =0; i<id.size(); i++){
			r.insert(root, id.get(i));
		}
		long endTime = System.nanoTime();
		long duration = (endTime - startTime);
		System.out.println("Runtime to build tree " + duration + " Nanoseconds");
		
		long startTime1 = System.nanoTime();
		for (int i=0; i<rangeQueries.size(); i++) {
			int[] range = rangeQueries.get(i);
			int res = Rtree.range_query(root, range);
			storeResults.add(res);
			result = 0;
		}
		long endTime1 = System.nanoTime();
		long duration1 = (endTime1 - startTime1);
		System.out.println("Runtime for range query is " + duration1 + " Nanoseconds");
		saveResults(saveLocation, storeResults, duration1);
	}
	
	/**
	 * Create a 2D Rtree
	 * @param M max number of entries per node
	 * @param L minimum number of entries per node
	 */
	public Rtree(int B) {
		M = B;
		Rtree.root = createRoot(true);
	}

	
	private static Node createRoot(final boolean asleaf) {
		return new Node(asleaf);
	}
	
	/**
	 * Should be invoked as insert(root, x, y)
	 * @param u
	 * @param x
	 * @param y
	 */
	public void insert(Node u, int id){
		Entry e = new Entry(id);
		if (u.leaf) {
			//System.out.println("Inserting at leaf");
			u.addLeafNode(e);
			e.parent = u;
			//System.out.println("Root MBR: " +"("+root.MBR[0] + "," + root.MBR[1]+ ")" + "("+root.MBR[2] + "," + root.MBR[3]+ ")");
			if (u.leafNode.size() > M) {
				handle_overflow(u);
			}
		} else {
			//System.out.println("Choosing SUb tree");
			Node v = chooseSubTree(u, id);
			insert(v, id);
		}
	}
	
	private static int range_query(Node u, int [] range) {
		if (u.leaf){
			//System.out.println("Range Searching Leaf");
			for (int i = 0; i < u.leafNode.size(); i++) {
				int id = u.leafNode.get(i).id;
				//System.out.println("Leaf Visited: "+id);
				int [] point = {x.get(id-1), y.get(id-1)};
				if (isCovered(range, point)){
					//System.out.println("ID: " + id);
					result++;
				}
				
			}
		} else {
			//System.out.println("Range Searching Internal");
			for (Node n : u.children){
				if (intersects(range, n.MBR)){
					range_query(n,range);
				}
			}
		}
		return result;
	}
	//Checks if a point is inside the rectange range
	private static boolean isCovered(int [] range, int [] point) {
		if (point[0] >= range[0] && point[0] <= range[2] && point[1] >= range[1] && point[1] <= range[3]){
			return true;
		} else {
			return false;
		}
	}
	//Checks if an MBR intersects the rectangle range
	private static boolean intersects(int[] range, int[] MBR){
		//rect[0] = lx, rect[1] = ly, rect[2] = ux, rect[3] = uy
		int mlx = MBR[0];
		int mly = MBR[1];
		int mux = MBR[2];
		int muy = MBR[3];
		int rlx = MBR[0];
		int rly = MBR[1];
		int rux = MBR[2];
		int ruy = MBR[3];
		//LowerRange is between LowerMBR and UpperMBR
		if (mlx <= rlx && mux >= rlx && mly <= rly && muy >= rly) return true;
		//mlx < rlx < mux && mly < ruy < muy
		if (mlx <= rlx && rlx < mux && mly < ruy && ruy < muy) return true;
		// mlx <= rux && rux < mux && mly <= rly && rly < mux
		if (mlx <= rux && rux < mux && mly <= rly && rly < mux) return true;
		// mlx <= rux && rux < mux && mly <= ruy && ruy < muy
		if (mlx <= rux && rux < mux && mly <= ruy && ruy < muy) return true;
		//mlx >= rlx && mlx <= rux && mly > rly && mly < ruy
		if (mlx >= rlx && mlx <= rux && mly >= rly && mly <= ruy) return true;
		//mlx >= rlx && mlx <= rux && mly < rly && rly < muy
		if (mlx >= rlx && mlx <= rux && mly < rly && rly < muy) return true;
		// muy > rly && muy < ruy && mlx >= rlx && mlx <= rux
		if (muy > rly && muy < ruy && mlx >= rlx && mlx <= rux) return true;
		return false;
	} 
	
	private static void handle_overflow(Node in) {
		boolean leaf;
		Node u;
		Node udash;
		//System.out.println("Handle Overflow");
		ArrayList<Node> split = new ArrayList<Node>();
		leaf = in.leaf;
		split = splitNode(in);
		if (leaf){
			//System.out.println("Overflow of leaf");
			u = new Node(true);
			for (Entry e : split.get(0).leafNode){
				u.addLeafNode(e);
			}
			udash = new Node(true);
			for (Entry e : split.get(1).leafNode){
				udash.addLeafNode(e);
			}
		} else {
			//System.out.println("Overflow of Internal");
			u = new Node(false);
			for (Node e : split.get(0).children){
				u.addNode(e);
			}
			udash = new Node(false);
			for (Node e : split.get(1).children){
				udash.addNode(e);
			}
		}
		if (in == root) {
			//System.out.println("Overflow at root");
			root = createRoot(false);
			root.addNode(u);
			root.addNode(udash);
		} else {
			//System.out.println("Overflow elsewhere");
			Node w = new Node(false);
			u.parent = w;
			udash.parent = w;
			w.addNode(u);
			w.addNode(udash);
			in.parent.addNode(w);
			if (in.parent.children.size() > M){
				//System.out.println("Created new overflow");
				handle_overflow(in.parent);
			}
		}
	}


	private static ArrayList<Node> splitNode(Node in) {
		ArrayList<int[]> s1 = new ArrayList<int[]>();
		ArrayList<int[]> s2 = new ArrayList<int[]>();
		int bestPerim = 90000000;
		ArrayList<Node> bestSplit = new ArrayList<Node>();
		//Allows us to sort points by x or y
		class sortPoints implements Comparator<int []> {
	    	private int compareBy = 0;
	    	 public sortPoints(int compareBy) {
	    	        this.compareBy = compareBy;
	    	    }

	        public int compare(int [] a, int [] b) {
	        	if (compareBy == 0){
	        		return a[0] - b[0];
	        	} else if (compareBy == 0){
	        		return a[1] - b[1];
	        	} else
	        	return a[0] - b[0];
	        }
	    }
		//Allows us to sort rectangles lx or ux or ly or uy
	    class sortRectangles implements Comparator<int []> {
	    	private int compareBy = 0;
	    	 public sortRectangles(int compareBy) {
	    	        this.compareBy = compareBy;
	    	    }

	        public int compare(int [] a, int [] b) {
	        	if (compareBy == 0){
	        		return a[0] - b[0];
	        	} else if (compareBy == 0){
	        		return a[2] - b[2];
	        	} else if (compareBy == 1){
	        		return a[1] - b[1];
	        	} else if (compareBy == 2) {
	        		return a[3] - b[3];
	        	}
	        	return a[0] - b[0];
	        }
	    }
	
		if (in.leaf == false) {
			//System.out.println("Spliting internal Node");
			ArrayList<int[]> splitNodes = new ArrayList<int[]>();
			int m = in.children.size();
			ArrayList<int[]> recList = new ArrayList<int[]>();
		for (int i =0; i<m;i++){
			int[] rec = {in.children.get(i).MBR[0], in.children.get(i).MBR[1], in.children.get(i).MBR[2],in.children.get(i).MBR[3], i};
			recList.add(rec);
		}

		for (int k =0; k<4;k++) {
			
			Collections.sort(recList, new sortRectangles(k));
			for (int i=(int) Math.ceil(0.4*M); i<=m-Math.ceil(0.4*M); i++){
				for (int j = 0; j<i; j++) {
					s1.add(recList.get(j));
				}
				for (int j = i; j<recList.size(); j++){
					s2.add(recList.get(j));
				}
				int perims1 = perimSum(getMBR(s1));
				int perims2 = perimSum(getMBR(s2));
				int totalPerim = perims1 + perims2;
				if (totalPerim < bestPerim){
					splitNodes.clear();
					bestPerim = totalPerim;
					int[] u = new int[s1.size()];
					int[] udash = new int[s2.size()];
					int l=0;
					for(int n =0; n<i;n++){
						u[l] = n;
						l++;
					}
					l=0;
					for(int n =i; n<recList.size();n++){
						udash[l] = n;
						l++;
					}
					splitNodes.add(u);
					splitNodes.add(udash);
				}
				s1.clear();
				s2.clear();
			}
		}
		Node u = new Node(true);
		Node udash = new Node(true);
		for (int i =0; i<splitNodes.get(0).length; i++) {
			u.addNode(in.children.get(splitNodes.get(0)[i]));
		}
		for (int i =0; i<splitNodes.get(1).length; i++) {
			udash.addNode(in.children.get(splitNodes.get(1)[i]));
		}
		in.children.clear();
		bestSplit.add(u);
		bestSplit.add(udash);
		return bestSplit;
		} else {
			ArrayList<int[]> splitNodes = new ArrayList<int[]>();
			//System.out.println("Splitting Leaf");
			int m = in.leafNode.size();
			ArrayList<int[]> coord = new ArrayList<int []>();
			for (int i =0; i<m;i++){
				int id = in.leafNode.get(i).id;
				int [] c = {x.get(id-1), y.get(id-1), id};
				coord.add(c);
			}
			
			for (int k = 0; k<2;k++){
				Collections.sort(coord, new sortPoints(k));	
				for (int i=(int) Math.ceil(0.4*M); i<=(int)(m-Math.ceil(0.4*M)); i++){
					
					for (int j = 0; j<i; j++) {
						s1.add(coord.get(j));
					}
					for (int j = i; j<coord.size(); j++){
						s2.add(coord.get(j));
					}
					int perims1 = perimSum(getMBRLeaf(s1));
					int perims2 = perimSum(getMBRLeaf(s2));
					int totalPerim = perims1 + perims2;
					if (totalPerim < bestPerim){
						splitNodes.clear();
						bestPerim = totalPerim;
						int[] u = new int[s1.size()];
						int[] udash = new int[s2.size()];
						int l=0;
						for(int n =0; n<i;n++){
							u[l] = n;
							l++;
						}
						l=0;
						for(int n =i; n<coord.size();n++){
							udash[l] = n;
							l++;
						}
						splitNodes.add(u);
						splitNodes.add(udash);
					}
					s1.clear();
					s2.clear();
				}
				
			}
			Node u = new Node(true);
			Node udash = new Node(true);
			for (int i =0; i<splitNodes.get(0).length; i++) {
				u.addLeafNode(in.leafNode.get(splitNodes.get(0)[i]));
			}
			for (int i =0; i<splitNodes.get(1).length; i++) {
				udash.addLeafNode(in.leafNode.get(splitNodes.get(1)[i]));
			}
			in.leafNode.clear();
			bestSplit.add(u);
			bestSplit.add(udash);
			return bestSplit;
		}
	}

	
	private Node chooseSubTree(Node u, int id) {
		//System.out.print("Is Leaf: " + u.leaf + "\n");
		if (u.leaf) {
			return u;
		}
		Node next = null;
		int minInc = 9000000;
		for (Node c: u.children) {
			int inc = perimeterInc(c, id);
			//System.out.println("Min Increase :" + minInc);
			//System.out.println("New Increase :" + inc);
			if (inc < minInc){
				minInc = inc;
				next = c;
			} else if (inc == minInc){
				//System.out.println("TIE BREAKER!");
				int nextPerim = perimSum(next.MBR);
				int cPerim = perimSum(c.MBR);
				//System.out.println("C PERIM :" + cPerim);
				//System.out.println("nextPerim :" + nextPerim);
				if (cPerim < nextPerim){
					next = c;
				} else {
					next = c;
				}
			}
		}
		return chooseSubTree(next, id);
	}

	private int perimeterInc(Node n, int id){
		int lx = n.MBR[0];
		int ly = n.MBR[1];
		int ux = n.MBR[2];
		int uy = n.MBR[3];
		int xValue = x.get(id-1);
		int yValue = x.get(id-1);
		int perim = 2*(ux-lx) + 2*(uy-ly);
		if (xValue < lx) {
			lx = xValue;	
		}else if (xValue > ux) {
			ux = xValue;	
		}
		if (yValue < ly) {
			ly = yValue;	
		}else if (yValue > uy) {
			uy = yValue;	
		}
		int newPerim = 2*(ux-lx) + 2*(uy-ly);
		return newPerim-perim;
	}
	
	//Return Perimeter Sum of the MBR
	public static int perimSum(int[] mbr){
		int lx = mbr[0];
		int ly = mbr[1];
		int ux = mbr[2];
		int uy = mbr[3];
		return 2*(ux-lx) + 2*(uy-ly);
	}
	//Get MBR for internal Node
	public static int [] getMBR(ArrayList<int []> recList){
		ArrayList<Integer> lx = new ArrayList<Integer>();
		ArrayList<Integer> ly = new ArrayList<Integer>();
		ArrayList<Integer> ux = new ArrayList<Integer>();
		ArrayList<Integer> uy = new ArrayList<Integer>();
		for (int i =0; i<recList.size(); i++){
			lx.add(recList.get(i)[0]);
			ly.add(recList.get(i)[1]);
			ux.add(recList.get(i)[2]);
			uy.add(recList.get(i)[3]);
		}
		int [] rec = {Collections.min(lx), Collections.min(ly), Collections.max(ux), Collections.max(uy)};
		return rec;
	}
	
	public static int[] getMBRLeaf(ArrayList<int[]> coords){
		ArrayList<Integer> x = new ArrayList<Integer>();
		ArrayList<Integer> y = new ArrayList<Integer>();
		for (int i=0; i<coords.size(); i++){
			x.add(coords.get(i)[0]);
			y.add(coords.get(i)[1]);
		}
		int [] rec ={Collections.min(x), Collections.min(y), Collections.max(x), Collections.max(y)};
		return rec;
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
		long duration = (endTime - startTime);
		System.out.println("Runtime to scan file " + duration + " Nanoseconds");
	}
	
	public static void loadQueries(String filePath) {
		try {
		int lx;
		int ly;
		int ux;
		int uy;
		File file = new File(filePath);
		Scanner scanner = new Scanner(file);
		while(scanner.hasNextInt()) {
			lx = (scanner.nextInt());
			ux = (scanner.nextInt());
			ly = (scanner.nextInt());
			uy = (scanner.nextInt());
			int[] query = {lx, ly, ux, uy}; 
			rangeQueries.add(query);
		}
		scanner.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void saveResults(String filePath, ArrayList<Integer> results, long duration){
		try (PrintWriter out = new PrintWriter(filePath)) {
			for (int i=0; i<results.size();i++){
		    out.println(results.get(i));
			}
			out.println("Total Runtime: " + duration + " Nanoseconds");
			out.println("Average Runtime: " + duration/rangeQueries.size() + " Nanoseconds");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	 
	
	private static class Node {
	        final LinkedList<Node> children;
	        final LinkedList<Entry> leafNode;
	        final boolean leaf;
	        Node parent;
	        int[] MBR;

	        private Node(boolean leaf) {
	        	//MBR[1],MBR[2] == l.x,l.y MBR[3], MBR[4] == u.x,u.y
	            this.leaf = leaf;
	            children = new LinkedList<Node>();
	            leafNode = new LinkedList<Entry>();
	            this.MBR = new int[4];
	        }
	        //Add points to the leaf
	        //Update MBR
	        private void addLeafNode(Entry e) {
	        	ArrayList<Integer> xlist = new ArrayList<Integer>();
	        	ArrayList<Integer> ylist = new ArrayList<Integer>();
	        	this.leafNode.add(e);
	        	e.parent = this;
	        	for (Entry i : this.leafNode) {
	        		int id = i.id;
	        		xlist.add(x.get(id-1));
	        		ylist.add(y.get(id-1));
	        	}
	        	this.MBR[0] = Collections.min(xlist);
	        	this.MBR[1] = Collections.min(ylist);
	        	this.MBR[2] = Collections.max(xlist);
	        	this.MBR[3] = Collections.max(ylist);
	        }
	        private void addNode(Node n) {
	        	ArrayList<Integer> xlist = new ArrayList<Integer>();
	        	ArrayList<Integer> ylist = new ArrayList<Integer>();
	        	this.children.add(n);
	        	n.parent = this;
	        	for (Node e : this.children) {
	        		xlist.add(e.MBR[0]);
	        		xlist.add(e.MBR[2]);
	        		ylist.add(e.MBR[1]);
	        		ylist.add(e.MBR[3]);
	        	}
	        	this.MBR[0] = Collections.min(xlist);
	        	this.MBR[1] = Collections.min(ylist);
	        	this.MBR[2] = Collections.max(xlist);
	        	this.MBR[3] = Collections.max(ylist);
	        }
	    }

	    private static class Entry extends Node {

	        
	        final int id;

			public Entry(int id) {
	            // an entry isn't actually a leaf (its parent is a leaf)
	            // but all the algorithms should stop at the first leaf they encounter,
	            // so this little hack shouldn't be a problem.
	            super(true);
	            this.id = id;
	        }

	        @Override
	        public String toString() {
	            return "Entry: " + x + ',' + y + ' ';
	        }
	    }
}
