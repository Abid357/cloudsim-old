import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class TaskGenerator {

	private List<Task> tasks;
	private int numOfClbs;
	private int numOfTasks;
	private int meanOfClbs;
	private int meanOfExecTime;
	private static int count;
	private static int clbs;
	private static int execTime;
	private static int deadline;
	private static int slack;
	private static Double d;

	private static int getNextId(List<Task> tasks) {
		if (tasks.isEmpty())
			return 1;
		return Collections.max(tasks, Comparator.comparingInt(Task::getId)).getId() + 1;
	}

	private static int getCummulativeExecTime(List<Task> tasks) {
		if (!tasks.isEmpty())
			return tasks.stream().mapToInt(Task::getExecTime).sum();
		else
			return 0;
	}

	private static int getPoissonRandom(double val) {
		Random r = new Random();
		double mean = 100.0;
		double L = Math.exp(-mean);
		double factor = val / mean;
		int k = 0;
		double p = 1.0;
		do {
			p = p * r.nextDouble();
			k++;
		} while (p > L);
		return (int) Math.floor((k - 1) * factor);
	}
	
	public static List<Task> generate(int numOfClbs, int numOfTasks, int meanOfClbs, int meanOfExecTime){
		List<Task> tasks = new ArrayList<Task>();
		if (meanOfClbs >= numOfClbs) {
			System.out.println("Incorrect mean! It must be less than " + numOfClbs);
			return null;
		}
		for (count = 0; count < numOfTasks; count++) {
			do {
				clbs = getPoissonRandom(meanOfClbs) + 1;
			} while (clbs > numOfClbs);
			execTime = getPoissonRandom(meanOfExecTime) + 1;
			d = Math.random() * 100;
			slack = d.intValue();
			deadline = getCummulativeExecTime(tasks) + execTime + slack % 6;
			tasks.add(new Task(getNextId(tasks), clbs, execTime, deadline));
		}
		return tasks;
	}
	
	public List<Task> getTasks() {
		return tasks;
	}
	
	public int getNumOfClbs() {
		return numOfClbs;
	}

	public TaskGenerator(Scanner reader) {
		tasks = new ArrayList<Task>();
		
		System.out.print("Enter Number of CLBs: ");
		numOfClbs = reader.nextInt();

		System.out.print("Enter Number of Tasks: ");
		numOfTasks = reader.nextInt();

		System.out.print("Enter Mean of CLB Distribution: ");
		meanOfClbs = reader.nextInt();
		while (meanOfClbs >= numOfClbs) {
			System.out.println("Incorrect mean! It must be less than " + numOfClbs);
			System.out.print("Enter Mean of CLB Distribution: ");
			meanOfClbs = reader.nextInt();
		}

		System.out.print("Enter Mean of Execution Time Distribution: ");
		meanOfExecTime = reader.nextInt();

		for (count = 0; count < numOfTasks; count++) {
			do {
				clbs = getPoissonRandom(meanOfClbs) + 1;
			} while (clbs > numOfClbs);
			execTime = getPoissonRandom(meanOfExecTime) + 1;
			d = Math.random() * 100;
			slack = d.intValue();
			deadline = getCummulativeExecTime(tasks) + execTime + slack % 6;
			tasks.add(new Task(getNextId(tasks), clbs, execTime, deadline));
		}
	}

	public static void main(String[] args){
		double poisson = 1000;
		List<Integer> vals = new ArrayList<Integer>(100);
		int sum = 0;
		for (int i = 0; i < 100; ++i) {
			int val = getPoissonRandom(poisson);
			vals.add(val);
			sum += val;
		}
	}
}