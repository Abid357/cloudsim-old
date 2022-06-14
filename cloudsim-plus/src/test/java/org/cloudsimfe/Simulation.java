import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Simulation {

    private static List<Task> tasks;
    private static int NUM_OF_CLBS = 500;
    private static int NUM_OF_TASKS = 500;
    private static int MEAN_OF_CLBS = 5;
    private static int MEAN_OF_EXEC_TIME = 5;

    private static long END_TIME = 0;
    private static long START_TIME = 0;
    private static long CLOCK_TIME = 0;
    private static int T_MAX = 0;
    private static int ITERATION_THRESHOLD = 16;
    private static int ITERATION_COUNT = 0;
    private static int STABLE_AFTER_ITERATION = 0;
    private static int STABLE_ITERATION_COUNT = 0;

    private static int[] bestSolution;
    private static int[] initialSolution;

    // set initial temperature
    private static double TEMP = 10000;

    // set cooling rate
    private static double COOLING_RATE = 0.001;

    public Simulation(String filename) {
        System.out.println("Welcome to A&M Simulator");

        NUM_OF_CLBS = 500;
        NUM_OF_TASKS = 1000;
        tasks = TaskGenerator.generate(NUM_OF_CLBS, NUM_OF_TASKS, 5, 10);


        System.out.println("FCFS:");
        runFCFS();
        printSummary();
        System.out.println("SDF:");
        runSDF();
        printSummary();
        System.out.println("SA:");
        run();
        printSummary();
    }

    public Simulation(double temp, double rate, String filename, int maxIterationCounts[]) {
        System.out.println("Welcome to A&M Simulator");

        tasks = readFromFile(filename + ".dat");
        if (tasks == null || tasks.isEmpty()) {
            System.out.println("\nError: task list could not be ready.");
            System.exit(2);
        } else {
            System.out.println("\nTask list ready.\nStarting parameter tuning...\n");
        }

        TEMP = temp;
        COOLING_RATE = rate;

        int count = 1;
        int completionTimes[] = new int[maxIterationCounts.length];
        long simulationDurations[] = new long[maxIterationCounts.length];
//        long stabilityIterations[] = new long[maxIterationCounts.length];
        for (int i = 0; i < maxIterationCounts.length; i++) {
            ITERATION_THRESHOLD = maxIterationCounts[i];
            run();
            completionTimes[count - 1] = getCompletionTime(bestSolution);
            simulationDurations[count - 1] = TimeUnit.NANOSECONDS.toMillis(END_TIME - START_TIME - CLOCK_TIME);
//            stabilityIterations[count - 1] = STABLE_AFTER_ITERATION;
            System.out.println("Simulation #" + count++ + " complete.");
            printSummary();
            System.out.println("Stable after iterations: " + STABLE_AFTER_ITERATION);
            System.out.println();
            ITERATION_COUNT = 0;
            STABLE_ITERATION_COUNT = 0;
        }

        System.out.print("Simulation durations: ");
        for (int i = 0; i < simulationDurations.length; i++) {
            System.out.print(simulationDurations[i] + " ");
        }

        System.out.print("\nCompletion times: ");
        for (int i = 0; i < completionTimes.length; i++) {
            System.out.print(completionTimes[i] + " ");
        }
    }

    public Simulation(double temps[], double rates[], String filename, int maxIterationCount) {
        System.out.println("Welcome to A&M Simulator");

        ITERATION_THRESHOLD = maxIterationCount;

        tasks = readFromFile(filename + ".dat");
        if (tasks == null || tasks.isEmpty()) {
            System.out.println("\nError: task list could not be ready.");
            System.exit(2);
        } else {
            System.out.println("\nTask list ready.\nStarting parameter tuning...\n");
        }

        int count = 1;
        int completionTimes[] = new int[temps.length * rates.length];
        long simulationDurations[] = new long[temps.length * rates.length];
        for (int i = 0; i < temps.length; i++) {
            TEMP = temps[i];
            for (int j = 0; j < rates.length; j++) {
                COOLING_RATE = rates[j];
                run();
                completionTimes[count - 1] = getCompletionTime(bestSolution);
                simulationDurations[count - 1] = TimeUnit.NANOSECONDS.toMillis(END_TIME - START_TIME);
                System.out.println("Simulation #" + count++ + " complete.");
                printSummary();
                System.out.println("Initial temperature: " + temps[i]);
                System.out.println("Cooling rate: " + rates[j]);
                System.out.println();
            }
        }

        System.out.print("Simulation durations: ");
        for (int i = 0; i < simulationDurations.length; i++) {
            System.out.print(simulationDurations[i] + " ");
        }

        System.out.print("\nCompletion times: ");
        for (int i = 0; i < completionTimes.length; i++) {
            System.out.print(completionTimes[i] + " ");
        }
    }

    public Simulation() {
        int option = -1;
        boolean doneStep1 = false;
        boolean doneStep2 = false;
        Scanner reader = new Scanner(System.in);
        System.out.println("Welcome to A&M Simulator");
        do {
            if (!doneStep1) {
                System.out.println("\nStep 1 - Please select an option:");
                System.out.println("\t1. Generate task list (user input)");
                System.out.println("\t2. Generate task list (default)");
                System.out.println("\t3. Read task list from file");
                System.out.println("\tPress -1 to exit");
                System.out.print("Option: ");
                option = reader.nextInt();
                reader.nextLine(); // skip a line, fix for reading a string immediately after int
                while ((option < 1 || option > 3) && option != -1) {
                    System.out.println("Invalid option!");
                    System.out.print("Option: ");
                    option = reader.nextInt();
                }

                if (option == 1) {
                    TaskGenerator generator = new TaskGenerator(reader);
                    tasks = generator.getTasks();
                    NUM_OF_TASKS = tasks.size();
                    NUM_OF_CLBS = generator.getNumOfClbs();
                } else if (option == 2)
                    tasks = TaskGenerator.generate(NUM_OF_CLBS, NUM_OF_TASKS, MEAN_OF_CLBS, MEAN_OF_EXEC_TIME);
                else if (option == 3) {
                    System.out.println("Existing text files: ");
                    printListOfTextFiles(new File(System.getProperty("user.dir")));
                    System.out.print("\nEnter file name: ");
                    String filename = reader.nextLine();
                    tasks = readFromFile(filename + ".dat");
                } else // option == -1
                    break;
                if (tasks == null || tasks.isEmpty()) {
                    System.out.println("Error: task list could not be ready.");
                    continue;
                } else {
                    System.out.println("Task list ready.");
                    doneStep1 = true;
                }
            }

            if (!doneStep2) {
                System.out.println("\nStep 2 - Please select an option:");
                System.out.println("\t0. Go back");
                System.out.println("\t1. Simulate");
                System.out.println("\t2. Save task list to file");
                System.out.println("\tPress -1 to exit");
                System.out.print("Option: ");
                option = reader.nextInt();
                reader.nextLine(); // skip a line, fix for reading a string immediately after int
                while (option < -1 || option > 2) {
                    System.out.println("Invalid option!");
                    System.out.print("Option: ");
                    option = reader.nextInt();
                }

                if (option == 0) {
                    doneStep1 = false;
                    continue;
                } else if (option == 1) {
                    run();
                    System.out.println("Simulation complete.");
                    Toolkit.getDefaultToolkit().beep();
                    doneStep2 = true;
                } else if (option == 2) {
                    System.out.print("Enter file name: ");
                    String filename = reader.nextLine();
                    printToFile(filename + ".dat");
                } else // option == -1
                    break;
            }

            if (doneStep2) {
                // STEP 3
                System.out.println("\nStep 3 - Please select an option:");
                System.out.println("\t0. Go back");
                System.out.println("\t1. View solution summary");
                System.out.println("\t2. View initial solution in table");
                System.out.println("\t3. View initial solution in array");
                System.out.println("\t4. View best solution in table");
                System.out.println("\t5. View best solution in array");
                System.out.println("\t6. Save task list to file");
                System.out.println("\tPress -1 to exit");
                System.out.print("Option: ");
                option = reader.nextInt();
                reader.nextLine(); // skip a line, fix for reading a string immediately after int
                while (option < -1 || option > 6) {
                    System.out.println("Invalid option!");
                    System.out.print("Option: ");
                    option = reader.nextInt();
                }

                if (option == 0) {
                    doneStep2 = false;
                    continue;
                } else if (option == 1)
                    printSummary();
                else if (option == 2)
                    printTable(initialSolution);
                else if (option == 3)
                    printArray(initialSolution);
                else if (option == 4)
                    printTable(bestSolution);
                else if (option == 5)
                    printArray(bestSolution);
                else if (option == 6) {
                    System.out.print("Enter file name: ");
                    String filename = reader.nextLine();
                    printToFile(filename + ".dat");
                } else // option == -1
                    break;
            }

        } while (option != -1);

        reader.close();
        System.out.println("\nThank you.");
    }

    private static boolean canAllocateTemporal(int x[], int requiredTime, int currentTime, int currentIndex, int pos) {
        for (int t = 0; t < requiredTime
                && currentTime <= T_MAX; currentTime++, t++, currentIndex = (currentTime - 1) * NUM_OF_CLBS + pos) {
            if (x[currentIndex] != 0)
                return false;
        }
        if (currentTime > T_MAX)
            return false;
        return true;
    }

    public static void main(String[] args) {
        /*
         * Normal simulation with user interface Call the Simulation() constructor
         * WITHOUT parameters
         */
//        new Simulation();

        /*
         * Parameter tuning: Call the Simulation() constructor WITH parameters Function
         * parameters: temperature values, initial rates, name of generated tasks file, max iteration count
         */
//		 String filename = "taskset_tsk=100_mclb=10000_mexec=10";
//		 double[] temps = { 10000 };
//		 double[] rates = { 0.001 };
//		 new Simulation(temps, rates, filename, 5);

        /*
         * Parameter tuning: Call the Simulation() constructor WITH parameters Function
         * parameters: temperature value, initial rate, name of generated tasks file, max iteration counts
         */
        String filename = "taskset_large_mclb=100_mexec=15";
        double temp = 10000;
        double rate = 0.001;
        int[] iterationCounts = new int[7];
        for (int i = 0; i < iterationCounts.length; i++)
            iterationCounts[i] = (int) Math.pow(2, 4);
//        for (int i = 0; i < 10; i++)
//            iterationCounts[i + 10] = (i + 1) * 1000;
//        for (int i = 0; i < 10; i++)
//            iterationCounts[i + 20] = (i + 1) * 1000;
//        new Simulation(temp, rate, filename, iterationCounts);
//        new Simulation(temp, rate, filename, iterationCounts);
//        new Simulation(temp, rate, filename, iterationCounts);

        new Simulation();
    }

    // calculate the acceptance probability
    private double acceptanceProbability(int energy, int newEnergy) {
        // if the new solution is better, accept it
        if (newEnergy < energy) {
            return 1.0;
        }
        // if the new solution is worse, calculate an acceptance probability
        return Math.exp((energy - newEnergy) / TEMP);
    }

    private int getTMax() {
        if (!tasks.isEmpty())
            return tasks.stream().mapToInt(Task::getExecTime).sum();
        else
            return -1;
    }

    private void printArray(int[] solution) {
        int x[] = solution;
        System.out.println("---------\n| ARRAY |\n---------");
        System.out.print("|");
        for (int i = 0; i < x.length; i++) {
            System.out.print(x[i] + "|");
        }
        System.out.println();
    }

    private void printTable(int[] solution) {
        int x[] = solution;
        int time = getCompletionTime(x);
        System.out.println("---------\n| TABLE |\n---------");
        for (int b = NUM_OF_CLBS; b > 0; b--) {
            System.out.printf("b =%3d|", b);
            for (int t = 1; t <= time; t++) {
                int index = ((t - 1) * NUM_OF_CLBS) + (b - 1);
                System.out.printf("%5d", x[index]);
            }
            System.out.println();
        }

        char sep = '_';
        System.out.print("      |");
        for (int t = 1; t <= time; t++)
            System.out.printf("%5s", sep);
        System.out.print("\n  T   |");
        for (int t = 1; t <= time; t++)
            System.out.printf("%5d", t);
        System.out.println();
    }

    private boolean canAllocateSpatial(int x[], int requiredClbs, int currentIndex, int endIndex) {
        for (int c = 0; c < requiredClbs && currentIndex <= endIndex; currentIndex++, c++) {
            if (x[currentIndex] != 0)
                c = -1;
        }
        if (currentIndex > endIndex)
            return false;
        return true;
    }

    private int getCompletionTime(int x[]) {
        if (x == null)
            return -1;
        for (int i = x.length - 1; i >= 0; --i) {
            if (x[i] != 0) {
                return (i / NUM_OF_CLBS) + 1;
            }
        }
        return 0;
    }

    private int[] allocate(List<Task> tasks, boolean initialFlag) {
        int[] x = new int[T_MAX * NUM_OF_CLBS];
        for (int k = 0; k < tasks.size(); k++) {
            int id = tasks.get(k).getId();
            int requiredTime = tasks.get(k).getExecTime();
            int requiredClbs = tasks.get(k).getClb();
            int currentTime = 1;
            int pos = -1;
            boolean success = false;

            // for every task k, check through each time unit
            for (; !success && currentTime <= T_MAX; currentTime++) {
                if (!initialFlag && (currentTime + tasks.get(k).getExecTime() - 1) > tasks.get(k).getDeadline())
//                    return new int[T_MAX * NUM_OF_CLBS];
                    return null;
                int startIndex = (currentTime - 1) * NUM_OF_CLBS;
                int endIndex = (currentTime - 1) * NUM_OF_CLBS + NUM_OF_CLBS;

                // for every time unit, check through each block and validate spatially and
                // temporally
                for (int b = startIndex; b <= endIndex && !success; b++) {
                    pos = b % NUM_OF_CLBS;
                    if (!canAllocateTemporal(x, requiredTime, currentTime, b, pos))
                        continue;
                    if (!canAllocateSpatial(x, requiredClbs, b, endIndex))
                        break;
                    success = true;
                    currentTime--; // compensate by one decrement not to proceed to next time unit if successful
                }
            }

            if (success) {
                for (int allocatedClbs = 0; allocatedClbs < requiredClbs; allocatedClbs++, pos++)
                    for (int t = currentTime, allocatedTime = 0; allocatedTime < requiredTime; t++, allocatedTime++) {
                        int index = (t - 1) * NUM_OF_CLBS + pos;
                        x[index] = id;
                    }
            }
        }
        return x;
    }

    public void printToFile(String filename) {
        File file = new File(filename);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            writer.print("");
            writer.println("numOfBlocks = " + NUM_OF_CLBS + ";");
            writer.println("tasks = {");

            // print until second last task with commas
            for (int i = 0; i < tasks.size() - 1; ++i) {
                writer.println(tasks.get(i) + ",");
            }

            // print last task without comma
            if (!tasks.isEmpty()) {
                writer.println(tasks.get(tasks.size() - 1));
            }
            writer.println("};");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }
    }

    public List<Task> readFromFile(String filename) {
        File file = new File(filename);
        List<Task> tasks = new ArrayList<Task>();

        if (!file.exists()) {
            return null;
        } else {
            try {
                Scanner reader = new Scanner(file);
                String line = reader.nextLine();
                int startIndex = line.indexOf("=");
                int endIndex = line.indexOf(";");
                NUM_OF_CLBS = Integer.parseInt(line.substring(startIndex + 2, endIndex));

                reader.nextLine(); // skip one line

                while (reader.hasNext()) {
                    line = reader.nextLine();
                    if (line.equals("};")) // end of file reached
                        break;
                    startIndex = line.indexOf("<");
                    endIndex = line.indexOf(">");
                    String csvLine = line.substring(startIndex + 1, endIndex);
                    String[] tokens = csvLine.split(", ");

                    int id = Integer.parseInt(tokens[0]);
                    int clb = Integer.parseInt(tokens[1]);
                    int execTime = Integer.parseInt(tokens[2]);
                    int deadline = Integer.parseInt(tokens[3]);
                    tasks.add(new Task(id, clb, execTime, deadline));
                }

                reader.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
            }
            NUM_OF_TASKS = tasks.size();
            return tasks;
        }
    }

    public void runFCFS(){
        // sort task based on earliest deadline first algorithm
//        Collections.sort(tasks, Comparator.comparingInt(Task::getDeadline));


        T_MAX = getTMax();

        START_TIME = System.nanoTime();
        END_TIME = System.nanoTime();
        CLOCK_TIME = END_TIME - START_TIME;
        START_TIME = System.nanoTime();

        int[] currentSolution = allocate(tasks, true);
        bestSolution = currentSolution;
        initialSolution = currentSolution;
    }

    public void runSDF(){
        // sort task based on earliest deadline first algorithm
        Collections.sort(tasks, Comparator.comparingInt(Task::getDeadline));


        T_MAX = getTMax();

        START_TIME = System.nanoTime();
        END_TIME = System.nanoTime();
        CLOCK_TIME = END_TIME - START_TIME;
        START_TIME = System.nanoTime();

        int[] currentSolution = allocate(tasks, true);
        bestSolution = currentSolution;
        initialSolution = currentSolution;
    }

    public void run() {
        // sort task based on earliest deadline first algorithm
        Collections.sort(tasks, new Comparator<Task>() {
            @Override
            public int compare(Task lhs, Task rhs) {
                return lhs.getDeadline() - rhs.getDeadline();
            }
        });

        T_MAX = getTMax();

        START_TIME = System.nanoTime();
        END_TIME = System.nanoTime();
        CLOCK_TIME = END_TIME - START_TIME;
        START_TIME = System.nanoTime();

        int currentEnergy = -1;
        int[] currentSolution = allocate(tasks, true);
        bestSolution = currentSolution;
        int bestEnergy = getCompletionTime(bestSolution);
        initialSolution = currentSolution;

        List<Task> newTasks = new ArrayList<Task>(tasks);
        List<Task> prevTasks = new ArrayList<Task>(newTasks);

        double temp = TEMP;
        double rate = COOLING_RATE;
        System.out.println("Initial objective: " + getCompletionTime(bestSolution) + ", Iterations passed: " + ITERATION_COUNT);
        // loop until system has cooled
        while (temp > 1 && ITERATION_COUNT != ITERATION_THRESHOLD) {

            boolean isBestFound = false;
            for (int i = 0; i < newTasks.size() / 3; i++) {
                // get a random positions
                int pos1 = (int) (newTasks.size() * Math.random());
                int pos2 = (int) (newTasks.size() * Math.random());

                // swap tasks based on positions
                Task tempTask = newTasks.get(pos1);
                newTasks.set(pos1, newTasks.get(pos2));
                newTasks.set(pos2, tempTask);

                // get energy of solutions
                currentEnergy = getCompletionTime(currentSolution);
                int[] newSolution = allocate(newTasks, false);
                int neighbourEnergy = getCompletionTime(newSolution);

                // if violates deadline constraint, do not consider neighbor
                if (neighbourEnergy == -1) {
                    newTasks = new ArrayList<Task>(prevTasks);
                    continue;
                }
                prevTasks = new ArrayList<Task>(newTasks);

                // decide if we should accept the neighbor
                if (acceptanceProbability(currentEnergy, neighbourEnergy) > Math.random()) {
                    currentSolution = newSolution;
                    currentEnergy = neighbourEnergy;
                }

                if (currentEnergy < bestEnergy) {
                    bestSolution = currentSolution;
                    bestEnergy = getCompletionTime(bestSolution);
//                STABLE_AFTER_ITERATION = ITERATION_COUNT;
                    isBestFound = true;
                }
            }

            // keep track of the best solution found

            if (isBestFound) {
                System.out.println("New best found: " + getCompletionTime(bestSolution) + ", Iterations passed: " + ITERATION_COUNT);
                ITERATION_COUNT = 0;
            } else
                ++ITERATION_COUNT;

            // cool system
            temp *= 1 - rate;

            // iteration condition
//            if (ITERATION_COUNT == ITERATION_THRESHOLD)
//                break;
        }
        END_TIME = System.nanoTime();
        System.out.println("Final objective: " + getCompletionTime(bestSolution) + ", Iterations passed: " + ITERATION_COUNT + ", Simulation time: " + TimeUnit.NANOSECONDS.toMillis(END_TIME - START_TIME - CLOCK_TIME) + " ms\n");
    }

    public void printListOfTextFiles(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                printListOfTextFiles(fileEntry);
            } else if (fileEntry.getName().endsWith(".dat")) {
                String name = fileEntry.getName();
                name = name.substring(0, name.indexOf(".dat"));
                System.out.println(name);
            }
        }
    }

    public void printSummary() {
        int x[] = bestSolution;
        int completionTime = getCompletionTime(x);
        long duration = END_TIME - START_TIME - CLOCK_TIME;
        duration = TimeUnit.NANOSECONDS.toMillis(duration);
        System.out.println("-----------\n| SUMMARY |\n-----------");
        if (duration > 0)
            System.out.println("Simulation duration: " + duration + " ms");
        else
            System.out.println("Simulation duration: < 1 ms");
        System.out.println("Number of CLBs: " + NUM_OF_CLBS);
        System.out.println("Number of tasks: " + NUM_OF_TASKS);
        System.out.println("Max time (tmax): " + T_MAX + " time units");
        System.out.println("Completion time (initial solution): " + getCompletionTime(initialSolution) + " time units");
        System.out.println("Completion time (best solution): " + completionTime + " time units");
        System.out.println("Iteration count: " + ITERATION_COUNT);
    }
}
