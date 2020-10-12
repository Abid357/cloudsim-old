package org.cloudsimfe;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RegionSchedulerMSA implements RegionScheduler {

    public static final int PRINT_OPTION_SUMMARY = 0;
    public static final int PRINT_OPTION_TABLE = 1;
    public static final int PRINT_OPTION_ARRAY = 2;
    public static final int PRINT_DATA_BEST_SOLUTION = 3;
    public static final int PRINT_DATA_INITIAL_SOLUTION = 4;

    // user-defined parameters
    public static int ITERATION_THRESHOLD = 6;
    public static double TEMP = 10000;
    public static double COOLING_RATE = 0.001;
    private static List<ConfigurationTask> tasks;
    private static int NUM_OF_REGIONS;
    private static int NUM_OF_FPGAS;
    private static int NUM_OF_TASKS;

    private static long END_TIME;
    private static long START_TIME;
    private static long CLOCK_TIME;
    private static int T_MAX;
    private static int ITERATION_COUNT = 0;

    private static int[] bestSolution;
    private static int[] initialSolution;

    private UnifiedManager manager;
    private PrintStream writer;

    public RegionSchedulerMSA(UnifiedManager manager) {
        this.manager = manager;
    }

    private static boolean canAllocateTemporal(int x[], int requiredTime, int currentTime, int currentIndex, int pos) {
        for (int t = 0; t < requiredTime
                && currentTime <= T_MAX; currentTime++, t++, currentIndex = (currentTime - 1) * NUM_OF_REGIONS + pos) {
            if (x[currentIndex] != 0)
                return false;
        }
        if (currentTime > T_MAX)
            return false;
        return true;
    }

    @Override
    public boolean schedule(int fpgaCount, int regionCount, Object tasks) {
        NUM_OF_FPGAS = fpgaCount;
        NUM_OF_REGIONS = regionCount;

        this.tasks = (List<ConfigurationTask>) tasks;
        NUM_OF_TASKS = RegionSchedulerMSA.tasks.size();

        simulate();

        manager.updateHypervisors(getCompletionTime(bestSolution), bestSolution, this.tasks);

        return false;
    }

    @Override
    public void print(Object... options) {
        if (options.length < 1)
            return;

        if (options.length == 1 && (int) options[0] == PRINT_OPTION_SUMMARY) {
            printSummary();
            return;
        }

        int[] solution = null;
        for (Object option : options) {
            if (option instanceof PrintStream)
                setWriter((PrintStream) option);
            else {
                int optionValue = (int) option;

                switch (optionValue) {
                    case PRINT_DATA_BEST_SOLUTION:
                        solution = bestSolution;
                        break;
                    case PRINT_DATA_INITIAL_SOLUTION:
                        solution = initialSolution;
                        break;
                }

                if (solution == null || writer == null)
                    continue;
                else {
                    switch (optionValue) {
                        case PRINT_OPTION_SUMMARY:
                            printSummary();
                            break;
                        case PRINT_OPTION_TABLE:
                            printTable(solution);
                            break;
                        case PRINT_OPTION_ARRAY:
                            printArray(solution);
                            break;
                    }
                }
            }
        }
    }

    private void simulate() {
        // sort task based on earliest deadline first algorithm
        Collections.sort(tasks, new Comparator<ConfigurationTask>() {
            @Override
            public int compare(ConfigurationTask lhs, ConfigurationTask rhs) {
                return lhs.getDeadline() - rhs.getDeadline();
            }
        });

        T_MAX = getTMax() + 1;

        START_TIME = System.nanoTime();
        END_TIME = System.nanoTime();
        CLOCK_TIME = END_TIME - START_TIME;
        START_TIME = System.nanoTime();

        int currentEnergy = -1;
        int[] currentSolution = allocate(tasks, true);
        bestSolution = currentSolution;
        int bestEnergy = getCompletionTime(bestSolution);
        initialSolution = currentSolution;

        List<ConfigurationTask> newTasks = new ArrayList<ConfigurationTask>(tasks);
        List<ConfigurationTask> prevTasks = new ArrayList<ConfigurationTask>(newTasks);

        double temp = TEMP;
        double rate = COOLING_RATE;

        // loop until system has cooled
        while (temp > 1 && ITERATION_COUNT != ITERATION_THRESHOLD) {

            boolean isBestFound = false;
            for (int i = 0; i < newTasks.size() / 3; i++) {
                // get a random positions
                int pos1 = (int) (newTasks.size() * Math.random());
                int pos2 = (int) (newTasks.size() * Math.random());

                // swap tasks based on positions
                ConfigurationTask tempTask = newTasks.get(pos1);
                newTasks.set(pos1, newTasks.get(pos2));
                newTasks.set(pos2, tempTask);

                // get energy of solutions
                currentEnergy = getCompletionTime(currentSolution);
                int[] newSolution = allocate(newTasks, false);
                int neighbourEnergy = getCompletionTime(newSolution);

                // if violates deadline constraint, do not consider neighbor
                if (neighbourEnergy == -1) {
                    newTasks = new ArrayList<ConfigurationTask>(prevTasks);
                    continue;
                }
                prevTasks = new ArrayList<ConfigurationTask>(newTasks);

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

            if (isBestFound)
                ITERATION_COUNT = 0;
            else
                ++ITERATION_COUNT;

            // cool system
            temp *= 1 - rate;

            // iteration condition
//            if (ITERATION_COUNT == ITERATION_THRESHOLD)
//                break;
        }
        END_TIME = System.nanoTime();
    }

    private int getTMax() {
        if (!tasks.isEmpty())
            return tasks.stream().mapToInt(ConfigurationTask::getExecutionTime).sum();
        else
            return -1;
    }

    public int getCompletionTime(int x[]) {
        if (x == null)
            return -1;
        for (int i = x.length - 1; i >= 0; --i) {
            if (x[i] != 0) {
                return (i / NUM_OF_REGIONS) + 1;
            }
        }
        return 0;
    }

    private int[] allocate(List<ConfigurationTask> tasks, boolean initialFlag) {
        int[] x = new int[T_MAX * NUM_OF_REGIONS];
        for (int k = 0; k < tasks.size(); k++) {
            int id = tasks.get(k).getId();
            int requiredTime = tasks.get(k).getExecutionTime();
            int requiredTiles = tasks.get(k).getTile();
            int currentTime = 1;
            int pos = -1;
            boolean success = false;

            // for every task k, check through each time unit
            for (; !success && currentTime <= T_MAX; currentTime++) {
                if (!initialFlag && (currentTime + tasks.get(k).getExecutionTime() - 1) > tasks.get(k).getDeadline())
                    return null;
                int startIndex = (currentTime - 1) * NUM_OF_REGIONS;
                int endIndex = (currentTime - 1) * NUM_OF_REGIONS + NUM_OF_REGIONS;

                // for every time unit, check through each block and validate spatially and
                // temporally
                for (int b = startIndex; b <= endIndex && !success; b++) {
                    pos = b % NUM_OF_REGIONS;
                    if (!canAllocateTemporal(x, requiredTime, currentTime, b, pos))
                        continue;
                    if (!canAllocateSpatial(x, requiredTiles, b, endIndex))
                        break;
                    success = true;
                    currentTime--; // compensate by one decrement not to proceed to next time unit if successful
                }
            }

            if (success) {
                for (int allocatedTiles = 0; allocatedTiles < requiredTiles; allocatedTiles++, pos++)
                    for (int t = currentTime, allocatedTime = 0; allocatedTime < requiredTime; t++, allocatedTime++) {
                        int index = (t - 1) * NUM_OF_REGIONS + pos;
                        x[index] = id;
                    }
            }
        }
        return x;
    }

    @Override
    public double getSchedulingDuration() {
        return END_TIME - START_TIME - CLOCK_TIME;
    }

    private boolean canAllocateSpatial(int x[], int requiredTiles, int currentIndex, int endIndex) {
        for (int c = 0; c < requiredTiles && currentIndex <= endIndex; currentIndex++, c++) {
            if (x[currentIndex] != 0)
                c = -1;
        }
        if (currentIndex > endIndex)
            return false;
        return true;
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


    private void printSummary() {
        int x[] = bestSolution;
        int completionTime = getCompletionTime(x);
        long duration = END_TIME - START_TIME - CLOCK_TIME;
        duration = TimeUnit.NANOSECONDS.toMillis(duration);
        System.out.println("-----------\n| SUMMARY |\n-----------");
        if (duration > 0)
            System.out.println("Simulation duration: " + duration + " ms");
        else
            System.out.println("Simulation duration: < 1 ms");
        System.out.println("Number of FPGAs: " + NUM_OF_FPGAS);
        System.out.println("Number of regions: " + NUM_OF_REGIONS);
        System.out.println("Number of VFPGAs: " + NUM_OF_TASKS);
        System.out.println("Max time (tmax): " + T_MAX + " time units");
        System.out.println("Completion time (initial solution): " + getCompletionTime(initialSolution) + " time units");
        System.out.println("Completion time (best solution): " + completionTime + " time units");
        System.out.println("Iteration count: " + ITERATION_COUNT);
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
        for (int b = NUM_OF_REGIONS; b > 0; b--) {
            System.out.printf("b =%3d|", b);
            for (int t = 1; t <= time; t++) {
                int index = ((t - 1) * NUM_OF_REGIONS) + (b - 1);
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

    public PrintStream getWriter() {
        return writer;
    }

    public void setWriter(PrintStream writer) {
        this.writer = writer;
    }

}
