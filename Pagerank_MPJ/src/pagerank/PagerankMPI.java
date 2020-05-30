package pagerank;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import mpi.MPI;

public class PagerankMPI {

	public static void main(String[] args) {

		try {
			File file = new File("");
			String parentPath = file.getAbsoluteFile().getParent();
			System.out.println(parentPath);
			String fileName = "p2p-Gnutella05-convert.txt";
			String outputFileName = "test3_MPJ.txt";
			String inputFile = parentPath + "\\InputFile\\" + fileName;
			String outputFile = outputFileName;
			int iteration = 8;
			double dumpingFactor = 0.85;
			MPI.Init(args);
			int rank = MPI.COMM_WORLD.Rank();
			long start = System.currentTimeMillis();
			if (rank == 0) {
				work0(inputFile, outputFile, iteration, dumpingFactor);

				long finish = System.currentTimeMillis();
				long time = finish - start;
				System.out.println("Finished " + time);
			} else {
				workI(iteration, dumpingFactor);
			}
			MPI.Finalize();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("Exception " + ex);
		}
	}

	public static void work0(String inputFile, String outputFile, int iteration, double dumpingFactor)
			throws FileNotFoundException {
		HashMap<Integer, ArrayList<Integer>> adjacencyMatrix = new HashMap<Integer, ArrayList<Integer>>();
		ReadFile(inputFile, adjacencyMatrix);
		int totalURL = adjacencyMatrix.keySet().size();
		int threads = MPI.COMM_WORLD.Size();
		int urlPerThread = totalURL / threads;
		System.out.println("Num of threads: "+ threads);
		System.out.println("Url per thread: "+ urlPerThread);
		if (urlPerThread > 0) {
			int url0 = urlPerThread + totalURL % threads;
			int[] numURLContainer = new int[2];
			numURLContainer[0] = urlPerThread;
			numURLContainer[1] = totalURL;
			MPI.COMM_WORLD.Bcast(numURLContainer, 0, 2, MPI.INT, 0);
			ArrayList<Integer> urlList = new ArrayList<Integer>(adjacencyMatrix.keySet());
			int[] detailNodes = new int[url0 * 2];
			int j = 0;
			HashMap<Integer, ArrayList<Integer>> adjacencyMatrix1 = new HashMap<Integer, ArrayList<Integer>>();
			for (int i = 0; i < (url0 * 2); i = i + 2) {
				detailNodes[i] = urlList.get(j);
				ArrayList<Integer> outLinks = adjacencyMatrix.get(detailNodes[i]);
				detailNodes[i + 1] = outLinks.size();
				adjacencyMatrix1.put(detailNodes[i], outLinks);
				j++;
			}
			for (int i = 1; i < threads; i++) {
				int index = (urlPerThread * i) + totalURL % threads;
				int[] detailNodes_I = new int[urlPerThread * 2];
				for (int k = 0; k < (urlPerThread * 2); k = k + 2) {
					detailNodes_I[k] = urlList.get(index);
					ArrayList<Integer> outLinks = adjacencyMatrix.get(detailNodes_I[k]);
					detailNodes_I[k + 1] = outLinks.size();
					index++;
				}
				MPI.COMM_WORLD.Send(detailNodes_I, 0, urlPerThread * 2, MPI.INT, i, 1);
				// Send out_links
				for (int k = 0; k < (urlPerThread * 2); k = k + 2) {
					int node = detailNodes_I[k];
					int outdegree = detailNodes_I[k + 1];
					int[] outlinks = new int[outdegree];
					ArrayList<Integer> outLinks = adjacencyMatrix.get(node);
					for (int idx = 0; idx < outdegree; idx++) {
						outlinks[idx] = outLinks.get(idx);
					}
					MPI.COMM_WORLD.Send(outlinks, 0, outdegree, MPI.INT, i, 1);
				}

			}
			/*
			 * double[] pagerank = PageRank(adjacencyMatrix1, totalURL, 0.85, 3);
			 * System.out.println(pagerank.length + "End"); for(int i = 0; i <
			 * pagerank.length; i++) { System.out.print(pagerank[i] + "[" + i + "]"); }
			 */
			Page[] pagerankList = PageRank(adjacencyMatrix1, totalURL, dumpingFactor, iteration);
			QuickSort(pagerankList, 0, url0 - 1);
			int height = GetHeight(threads);
			Page[] result = new Page[totalURL];
			result = MergeResult(pagerankList, height, url0, result);
			// System.out.println(result[totalURL - 1].getRank_value());
			WriteFile(result, outputFile);

		}

	}

	public static void workI(int iteration, double dumpingFactor) {
		HashMap<Integer, ArrayList<Integer>> adjacencyMatrix = new HashMap<Integer, ArrayList<Integer>>();
		int[] numURLContainer = new int[2];
		MPI.COMM_WORLD.Bcast(numURLContainer, 0, 2, MPI.INT, 0);
		int url_I = numURLContainer[0];
		int total = numURLContainer[1];
		int[] detailNodes_I = new int[url_I * 2];
		MPI.COMM_WORLD.Recv(detailNodes_I, 0, url_I * 2, MPI.INT, 0, 1); // tag 1 la??
		// Nhan outlinks cua tung node
		for (int i = 0; i < (url_I * 2); i = i + 2) {
			int node = detailNodes_I[i];
			int outdegree = detailNodes_I[i + 1];
			int[] outlinks = new int[outdegree];
			MPI.COMM_WORLD.Recv(outlinks, 0, outdegree, MPI.INT, 0, 1);
			ArrayList<Integer> outLinks = new ArrayList<Integer>(outdegree);
			for (int k = 0; k < outdegree; k++) {
				outLinks.add(outlinks[k]);
			}
			adjacencyMatrix.put(node, outLinks);
		}
		Page[] pagerankList = PageRank(adjacencyMatrix, total, dumpingFactor, iteration);
		QuickSort(pagerankList, 0, url_I - 1);
		int numThreads = MPI.COMM_WORLD.Size();
		int height = GetHeight(numThreads);
		MergeResult(pagerankList, height, url_I, null);
	}

	public static double[][] ConstructColumnStochatic(HashMap<Integer, ArrayList<Integer>> adjacencyMatrix, int total) {
		ArrayList<Integer> urlList = new ArrayList<Integer>(adjacencyMatrix.keySet());
		int numURL = urlList.size();
		double[][] matrix = new double[total][numURL];
		for (int i = 0; i < numURL; i++) {
			int node = urlList.get(i);
			ArrayList<Integer> outLinks = adjacencyMatrix.get(node);
			int outdegree = outLinks.size();
			if (outdegree > 0) {
				for (int k = 0; k < outdegree; k++) {
					int outNode = outLinks.get(k);
					matrix[outNode][i] = 1.0 / outdegree;
				}
			} else {
				double value = 1.0 / total;
				for (int j = 0; j < total; j++) {
					matrix[j][i] = value;
				}
			}

		}
		return matrix;

	}

	public static Page[] PageRank(HashMap<Integer, ArrayList<Integer>> adjacencyMatrix, int total, double dumpingFactor,
			int iteration) {
		int numURL_I = adjacencyMatrix.keySet().size();
		ArrayList<Integer> urlList = new ArrayList<Integer>(adjacencyMatrix.keySet());
		double[][] columnStochatic = ConstructColumnStochatic(adjacencyMatrix, total);
		for (int i = 0; i < total; i++) {
			for (int j = 0; j < numURL_I; j++) {
				columnStochatic[i][j] = dumpingFactor * columnStochatic[i][j] + (1 - dumpingFactor) * (1.0) / total;
			}
		}
		double[] x = new double[total]; // xac suat cua node
		double[] global_X = new double[total];
		Arrays.fill(global_X, 1.0); // Khởi tạo vecto x
		int count = 0;
		do {
			for (int i = 0; i < total; i++) {
				double temp = 0;
				for (int j = 0; j < numURL_I; j++) {
					int node = urlList.get(j);
					temp = temp + global_X[node] * columnStochatic[i][j];
				}
				x[i] = temp;
			}

			MPI.COMM_WORLD.Allreduce(x, 0, global_X, 0, total, MPI.DOUBLE, MPI.SUM);
			count++;
		} while (count < (iteration + 1));
		double sum = 0;
		for (int i = 0; i < total; i++) {
			sum += global_X[i];
		}
		Page[] urlList_I = new Page[numURL_I];
		for (int i = 0; i < numURL_I; i++) {
			int node = urlList.get(i);
			double rankValue = global_X[node] / sum;
			Page temp = new Page(node, rankValue);
			urlList_I[i] = temp;
		}
		return urlList_I;
		// return global_X;

	}

	public static void QuickSort(Page[] urlList, int left, int right) {
		if (left < right) {
			int pivot = Partition(urlList, left, right);
			if (left < pivot) {
				QuickSort(urlList, left, pivot - 1);
			}
			if (right > pivot) {
				QuickSort(urlList, pivot + 1, right);
			}
		}
	}

	public static int Partition(Page[] urlList, int left, int right) {
		double pivot = urlList[left].getRank_value();
		int start = left;
		int end = right + 1;
		while (start < end) {
			start += 1;
			while (start < right && urlList[start].getRank_value() <= pivot)
				start++;
			end--;
			while (end > left && urlList[end].getRank_value() > pivot)
				end--;
			Swap(urlList, start, end);
		}
		Swap(urlList, start, end);
		Swap(urlList, end, left);
		return end;
	}

	public static void Swap(Page[] urlList, int a, int b) {
		Page pageA = urlList[a];
		Page pageB = urlList[b];
		urlList[a] = pageB;
		urlList[b] = pageA;
	}

	public static Page[] MergeResult(Page[] urlList_I, int height, int numURL_I, Page[] totalURLs) {
		int i = 0;
		int rank = MPI.COMM_WORLD.Rank();
		int numThreads = MPI.COMM_WORLD.Size();
		Page[] currentNode = urlList_I;
		while (i < height) {
			int parent = (rank & (~1 << i));
			if (parent == rank) {
				int sibling = rank | 1 << i;
				if (sibling < numThreads) {
					int[] receivedSiblingURLs = new int[1];
					MPI.COMM_WORLD.Recv(receivedSiblingURLs, 0, 1, MPI.INT, sibling, 1);
					int numSiblingURLs = receivedSiblingURLs[0];
					Page[] siblingURLs = new Page[numSiblingURLs];
					MPI.COMM_WORLD.Recv(siblingURLs, 0, numSiblingURLs, MPI.OBJECT, sibling, 1);
					// merge
					Page[] parentURLs = new Page[numURL_I + numSiblingURLs];
					parentURLs = Merge(currentNode, siblingURLs, parentURLs, numURL_I + numSiblingURLs);
					currentNode = parentURLs;
					numURL_I = numURL_I + numSiblingURLs;
				}
				i++;
			} else {
				int[] numURLsContainer = new int[1];
				numURLsContainer[0] = numURL_I;
				MPI.COMM_WORLD.Send(numURLsContainer, 0, 1, MPI.INT, parent, 1);
				MPI.COMM_WORLD.Send(currentNode, 0, numURL_I, MPI.OBJECT, parent, 1);
				i = height;
			}
		}
		if (rank == 0) {
			totalURLs = currentNode;
		}
		return totalURLs;
	}

	public static Page[] Merge(Page[] leftChild, Page[] rightChild, Page[] parent, int total) {
		int leftSize = leftChild.length;
		int rightSize = rightChild.length;
		int i = 0;
		int j = 0;
		int index = 0;
		while ((i < leftSize) && (j < rightSize)) {
			if (leftChild[i].getRank_value() <= rightChild[j].getRank_value()) {
				parent[index] = new Page();
				parent[index] = leftChild[i];
				i++;
			} else {
				parent[index] = new Page();
				parent[index] = rightChild[j];
				j++;
			}
			index++;
		}
		while (i < leftSize) {
			parent[index] = new Page();
			parent[index] = leftChild[i];
			index++;
			i++;
		}
		while (j < rightSize) {
			parent[index] = new Page();
			parent[index] = rightChild[j];
			index++;
			j++;
		}
		return parent;
	}

	public static int Log2(int x) {
		if (x <= 0)
			return -1;
		else if (x >= 2)
			return (1 + Log2(x / 2));
		else
			return 0;
	}

	public static int GetHeight(int total) {
		int x = Log2(total);
		if (1 << x == total)
			return x;
		else
			return (x + 1);
	}

	public static void ReadFile(String fileName, HashMap<Integer, ArrayList<Integer>> adjacencyMatrix)
			throws FileNotFoundException {
		FileInputStream input = new FileInputStream(fileName);
		HashMap<Integer, Integer> intputHashMap = null;
		try {
			DataInputStream datastr = new DataInputStream(input);
			BufferedReader reader = new BufferedReader(new InputStreamReader(datastr));
			String line;
			while ((line = reader.readLine()) != null) {
				ArrayList<Integer> outLinks = new ArrayList<Integer>();
				line = line.trim();
				line = line.replaceAll(" ", "\t");
				String[] nodeList = line.split("\t");
				int node = Integer.valueOf(nodeList[0]);
				for (int i = 1; i < nodeList.length; i++) {
					int value = Integer.valueOf(nodeList[i]);
					outLinks.add(value);
				}
				adjacencyMatrix.put(node, outLinks);
			}
			datastr.close();
			input.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/*
	 * public static HashMap<Integer, Integer> mapIndexToNodeId(String fileName)
	 * throws FileNotFoundException { // ArrayList<Integer> mapIndexToNodeId = new
	 * ArrayList(); FileInputStream input = new FileInputStream(fileName);
	 * HashMap<Integer, Integer> mapIndexToNodeId = null; try { DataInputStream
	 * datastr = new DataInputStream(input); BufferedReader reader = new
	 * BufferedReader(new InputStreamReader(datastr)); String line;
	 * 
	 * while ((line = reader.readLine()) != null) { ArrayList<Integer> outLinks =
	 * new ArrayList<Integer>(); line = line.trim(); String[] arr =
	 * line.split("\t");
	 * mapIndexToNodeId.put(Integer.valueOf(arr[1]),Integer.valueOf(arr[0])); }
	 * datastr.close(); input.close(); } catch (IOException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); } return mapIndexToNodeId; }
	 */

	public static void WriteFile(Page[] result, String fileName) throws FileNotFoundException {
//		HashMap<Integer, Integer> mapIndexToNodeId = mapIndexToNodeId("C:\\Users\\Asus\\Desktop\\distributed_cumputing\\project\\Pagerank_MPJ\\InputFile\\"+"mapIndexToNodeId.txt");
		try {
			FileWriter output = new FileWriter(
					"C:\\Users\\Asus\\Desktop\\distributed_cumputing\\project\\Pagerank_MPJ\\OutputFile\\output.txt");
			BufferedWriter buffWriter = new BufferedWriter(output);
			String line = "Rank\t\t\tNode_ID\t\t\tValue\n";
			buffWriter.write(line);
			int j = 1;
			DecimalFormat df = new DecimalFormat("#.#####");
			df.setRoundingMode(RoundingMode.CEILING);
			for (int i = result.length - 1; i >= 0; i--) {
				double value = result[i].getRank_value();
				/* System.out.println(df.format(value)); */
				line = j + "\t\t\t\t" + result[i].getNode_id() + "\t\t\t\t" + df.format(value) + "\n";
				buffWriter.write(line);
				j++;
			}
			buffWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
