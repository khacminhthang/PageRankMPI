package pagerank;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
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

public class Util {
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
		Arrays.fill(global_X, 1.0 / 8); // Khởi tạo vecto x
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
			for (int i = 0; i < total; i++) {
				global_X[i] = x[i];
			}

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

	/*public static ArrayList<Integer> mapIndexToNodeId(String fileName) throws FileNotFoundException {
		ArrayList<Integer> mapIndexToNodeId = new ArrayList();
		FileInputStream input = new FileInputStream(fileName);
		HashMap<Integer, Integer> intputHashMap = null;
		try {
			DataInputStream datastr = new DataInputStream(input);
			BufferedReader reader = new BufferedReader(new InputStreamReader(datastr));
			String line;

			while ((line = reader.readLine()) != null) {
				ArrayList<Integer> outLinks = new ArrayList<Integer>();
				line = line.trim();
				mapIndexToNodeId.add(Integer.valueOf(line));
			}
			datastr.close();
			input.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mapIndexToNodeId;
	}*/

	public static void WriteFile(Page[] result, String fileName) throws FileNotFoundException {
	/*	ArrayList<Integer> mapIndexToNodeId = mapIndexToNodeId(
				"C:\\Users\\Asus\\Desktop\\distributed_cumputing\\project\\Pagerank_MPJ\\InputFile\\"
						+ "mapIndexToNodeId.txt");*/
		try {
			FileWriter output = new FileWriter(fileName);
			BufferedWriter buffWriter = new BufferedWriter(output);
			String line = "Rank\t\t\tNode_ID\t\t\tValue\n";
			buffWriter.write(line);
			int j = 1;
			DecimalFormat df = new DecimalFormat("#.#####");
			df.setRoundingMode(RoundingMode.CEILING);
			System.out.println(df.format(0.0345626));
			for (int i = result.length - 1; i >= 0; i--) {
				double value = result[i].getRank_value();
				/* System.out.println(df.format(value)); */
				line = j + "\t\t\t\t" + result[i].getNode_id() + "\t\t\t\t" + df.format(value)
						+ "\n";
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
