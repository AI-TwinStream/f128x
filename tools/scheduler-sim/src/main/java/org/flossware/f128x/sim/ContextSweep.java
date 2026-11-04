package org.flossware.f128x.sim;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.SplittableRandom;

/**
 * Cycle-level experiment for context scheduling and memory latency hiding.
 *
 * A thread issues at most one instruction per cycle. Memory instructions are
 * assigned to latency classes and consume a slot in a finite shared set of
 * outstanding memory transactions. If no slot is available, the selected
 * memory instruction waits for queue credit without being issued.
 *
 * Non-memory instructions may optionally incur a serialized dependency stall.
 * This is an exploratory model, not a realistic cache, DRAM, or core model.
 */
public final class ContextSweep {
    private record Completion(long cycle, int thread, int latencyClass) {}

    private record DependencyWakeup(long cycle, int thread) {}

    private record MemoryClass(String name, double cumulativeFraction,
            int latency) {}

    private ContextSweep() {}

    public static void main(String[] args) {
        if (args.length != 6 && args.length != 7
                && args.length != 8 && args.length != 13 && args.length != 14
                && args.length != 15 && args.length != 16) {
            usage();
            System.exit(2);
        }

        int contexts = Integer.parseInt(args[0]);
        int issueWidth = Integer.parseInt(args[1]);
        int defaultLatency = Integer.parseInt(args[2]);
        double memoryFraction = Double.parseDouble(args[3]);
        int cycles = Integer.parseInt(args[4]);
        long seed = Long.parseLong(args[5]);
        int maxOutstanding = args.length >= 7
                ? Integer.parseInt(args[6]) : 1;
        int memoryQueueCapacity = args.length >= 8
                ? Integer.parseInt(args[7]) : Integer.MAX_VALUE;
        double dependencyFraction = args.length >= 15
                ? Double.parseDouble(args[13]) : 0.0;
        int dependencyLatency = args.length >= 15
                ? Integer.parseInt(args[14]) : 1;
        int memoryCompletionRate = args.length == 14
                ? Integer.parseInt(args[13])
                : args.length == 16 ? Integer.parseInt(args[15])
                : Integer.MAX_VALUE;

        MemoryClass[] memoryClasses;
        if (args.length >= 13) {
            double l1Fraction = Double.parseDouble(args[8]);
            int l1Latency = Integer.parseInt(args[9]);
            double l2Fraction = Double.parseDouble(args[10]);
            int l2Latency = Integer.parseInt(args[11]);
            int dramLatency = Integer.parseInt(args[12]);
            if (l1Fraction < 0.0 || l2Fraction < 0.0
                    || l1Fraction + l2Fraction > 1.0) {
                throw new IllegalArgumentException(
                        "Memory-class fractions must be non-negative and sum to at most 1");
            }
            memoryClasses = new MemoryClass[] {
                    new MemoryClass("L1", l1Fraction, l1Latency),
                    new MemoryClass("L2", l1Fraction + l2Fraction, l2Latency),
                    new MemoryClass("DRAM", 1.0, dramLatency)
            };
        } else {
            memoryClasses = new MemoryClass[] {
                    new MemoryClass("MEM", 1.0, defaultLatency)
            };
        }

        if (contexts < 1 || issueWidth < 1 || defaultLatency < 1
                || cycles < 1 || maxOutstanding < 1
                || memoryQueueCapacity < 1 || memoryFraction < 0.0
                || memoryFraction > 1.0 || dependencyFraction < 0.0
                || dependencyFraction > 1.0 || dependencyLatency < 1
                || memoryCompletionRate < 1 || hotWorkspaceRatio < 0.0
                || hotWorkspaceRatio > 1.0 || hotMemoryFraction < 0.0
                || hotMemoryFraction > 1.0 || coldMemoryFraction < 0.0
                || coldMemoryFraction > 1.0 || hotDependencyFraction < 0.0
                || hotDependencyFraction > 1.0 || coldDependencyFraction < 0.0
                || coldDependencyFraction > 1.0) {
            throw new IllegalArgumentException("Invalid simulation parameter");
        }
        for (MemoryClass memoryClass : memoryClasses) {
            if (memoryClass.latency() < 1) {
                throw new IllegalArgumentException(
                        "Memory-class latencies must be positive");
            }
        }

        String schedulerStrategy = System.getProperty("f128x.scheduler", "fifo");
        int workspaceCount = (contexts + 15) / 16;
        int defaultClusters = Math.max(1, Math.min(8, workspaceCount));
        int clusterCount = Integer.getInteger("f128x.clusters", defaultClusters);
        double hotWorkspaceRatio = Double.parseDouble(
                System.getProperty("f128x.hotWorkspaceRatio", "0.25"));
        double hotMemoryFraction = Double.parseDouble(System.getProperty(
                "f128x.hotMemoryFraction", Double.toString(memoryFraction)));
        double coldMemoryFraction = Double.parseDouble(System.getProperty(
                "f128x.coldMemoryFraction", Double.toString(memoryFraction)));
        double hotDependencyFraction = Double.parseDouble(System.getProperty(
                "f128x.hotDependencyFraction", Double.toString(dependencyFraction)));
        double coldDependencyFraction = Double.parseDouble(System.getProperty(
                "f128x.coldDependencyFraction", Double.toString(dependencyFraction)));
        int hotWorkspaceCount = (int) Math.ceil(workspaceCount * hotWorkspaceRatio);
        ReadyScheduler ready = createReadyScheduler(
                schedulerStrategy, contexts, clusterCount);
        var queueWaiters = new ArrayDeque<Integer>();
        var completions = new PriorityQueue<>(
                Comparator.comparingLong(Completion::cycle));
        var dependencyWakeups = new PriorityQueue<>(
                Comparator.comparingLong(DependencyWakeup::cycle));
        int[] outstandingByThread = new int[contexts];
        boolean[] waitingForThreadSlot = new boolean[contexts];
        boolean[] waitingForQueueSlot = new boolean[contexts];
        int[] pendingClass = new int[contexts];
        java.util.Arrays.fill(pendingClass, -1);

        for (int thread = 0; thread < contexts; thread++) {
            ready.addLast(thread);
        }

        var random = new SplittableRandom(seed);
        long measuredIssued = 0;
        long memoryIssues = 0;
        long completedRequests = 0;
        long idleCycles = 0;
        long queueFullAttempts = 0;
        long queueWaiterCycles = 0;
        long dependencyWaiterCycles = 0;
        long serviceBacklogCycles = 0;
        long dependencyIssues = 0;
        long completedDependencies = 0;
        long maxQueueOccupancy = 0;
        int maxThreadOutstandingObserved = 0;
        long[] workspaceIssues = new long[workspaceCount];
        long[] classIssues = new long[memoryClasses.length];
        long[] classCompletions = new long[memoryClasses.length];
        final int warmup = Math.min(10_000, cycles / 10);

        for (int cycle = 0; cycle < cycles; cycle++) {
            queueWaiterCycles += queueWaiters.size();
            dependencyWaiterCycles += dependencyWakeups.size();

            while (!dependencyWakeups.isEmpty()
                    && dependencyWakeups.peek().cycle() <= cycle) {
                DependencyWakeup wakeup = dependencyWakeups.remove();
                ready.addLast(wakeup.thread());
                completedDependencies++;
            }

            int completionsThisCycle = 0;
            while (!completions.isEmpty()
                    && completions.peek().cycle() <= cycle
                    && completionsThisCycle < memoryCompletionRate) {
                completionsThisCycle++;
                Completion completion = completions.remove();
                int thread = completion.thread();
                int latencyClass = completion.latencyClass();
                outstandingByThread[thread]--;
                completedRequests++;
                classCompletions[latencyClass]++;

                if (waitingForThreadSlot[thread]
                        && outstandingByThread[thread] < maxOutstanding) {
                    waitingForThreadSlot[thread] = false;
                    if (!waitingForQueueSlot[thread]) {
                        ready.addLast(thread);
                    }
                }
            }

            if (!completions.isEmpty()
                    && completions.peek().cycle() <= cycle) {
                serviceBacklogCycles++;
            }

            // Wake only as many FIFO queue waiters as there are returned credits.
            int credits = Math.max(0,
                    memoryQueueCapacity - completions.size());
            int wakeCount = Math.min(credits, queueWaiters.size());
            int[] awakened = new int[wakeCount];
            for (int i = 0; i < wakeCount; i++) {
                int thread = queueWaiters.removeFirst();
                waitingForQueueSlot[thread] = false;
                awakened[i] = thread;
            }
            // Give returned-credit waiters first access to the credits.
            for (int i = wakeCount - 1; i >= 0; i--) {
                ready.addFirst(awakened[i]);
            }

            int candidates = ready.size();
            int issuedThisCycle = 0;
            int[] requeue = new int[candidates];
            int requeueCount = 0;

            // Visit each thread that was ready at cycle start at most once.
            while (candidates-- > 0 && issuedThisCycle < issueWidth) {
                int thread = ready.poll();
                if (thread < 0) {
                    break;
                }
                int workspace = thread / 16;
                double threadMemoryFraction = workspace < hotWorkspaceCount
                        ? hotMemoryFraction : coldMemoryFraction;
                double threadDependencyFraction = workspace < hotWorkspaceCount
                        ? hotDependencyFraction : coldDependencyFraction;
                int selectedClass = pendingClass[thread];

                if (selectedClass < 0
                        && random.nextDouble() < threadMemoryFraction) {
                    selectedClass = selectMemoryClass(memoryClasses, random);
                    pendingClass[thread] = selectedClass;
                }

                if (selectedClass >= 0) {
                    if (completions.size() >= memoryQueueCapacity) {
                        queueFullAttempts++;
                        waitingForQueueSlot[thread] = true;
                        queueWaiters.addLast(thread);
                        continue;
                    }

                    MemoryClass memoryClass = memoryClasses[selectedClass];
                    completions.add(new Completion(
                            (long) cycle + memoryClass.latency(),
                            thread, selectedClass));
                    pendingClass[thread] = -1;
                    memoryIssues++;
                    classIssues[selectedClass]++;
                    workspaceIssues[thread / 16]++;
                    issuedThisCycle++;
                    if (cycle >= warmup) {
                        measuredIssued++;
                    }

                    outstandingByThread[thread]++;
                    maxThreadOutstandingObserved = Math.max(
                            maxThreadOutstandingObserved,
                            outstandingByThread[thread]);
                    maxQueueOccupancy = Math.max(
                            maxQueueOccupancy, completions.size());

                    if (outstandingByThread[thread] >= maxOutstanding) {
                        waitingForThreadSlot[thread] = true;
                    } else {
                        requeue[requeueCount++] = thread;
                    }
                } else {
                    issuedThisCycle++;
                    workspaceIssues[thread / 16]++;
                    if (cycle >= warmup) {
                        measuredIssued++;
                    }
                    if (random.nextDouble() < threadDependencyFraction) {
                        dependencyIssues++;
                        dependencyWakeups.add(new DependencyWakeup(
                                (long) cycle + dependencyLatency, thread));
                    } else {
                        requeue[requeueCount++] = thread;
                    }
                }
            }

            for (int i = 0; i < requeueCount; i++) {
                ready.addLast(requeue[i]);
            }
            if (issuedThisCycle == 0) {
                idleCycles++;
            }
        }

        int measuredCycles = Math.max(1, cycles - warmup);
        double ipc = (double) measuredIssued / measuredCycles;
        double utilization = ipc / issueWidth;
        int waitingAtThreadLimit = 0;
        int waitingAtQueueLimit = 0;
        long outstandingAtEnd = 0;
        for (int thread = 0; thread < contexts; thread++) {
            if (waitingForThreadSlot[thread]) {
                waitingAtThreadLimit++;
            }
            if (waitingForQueueSlot[thread]) {
                waitingAtQueueLimit++;
            }
            outstandingAtEnd += outstandingByThread[thread];
        }

        long totalWorkspaceIssues = 0;
        long minWorkspaceIssues = Long.MAX_VALUE;
        long maxWorkspaceIssues = 0;
        double sumWorkspaceSquares = 0.0;
        for (long count : workspaceIssues) {
            totalWorkspaceIssues += count;
            minWorkspaceIssues = Math.min(minWorkspaceIssues, count);
            maxWorkspaceIssues = Math.max(maxWorkspaceIssues, count);
            sumWorkspaceSquares += (double) count * count;
        }
        double workspaceJainFairness = totalWorkspaceIssues == 0 ? 1.0
                : (double) totalWorkspaceIssues * totalWorkspaceIssues
                        / (workspaceIssues.length * sumWorkspaceSquares);

        System.out.printf(
                "scheduler=%s clusters=%d schedulerSteals=%d schedulerSelections=%d "
                        + "contexts=%d workspaces=%d hotWorkspaceCount=%d "
                        + "hotWorkspaceRatio=%.3f hotMemoryFraction=%.3f "
                        + "coldMemoryFraction=%.3f hotDependencyFraction=%.3f "
                        + "coldDependencyFraction=%.3f issueWidth=%d memoryFraction=%.3f "
                        + "dependencyFraction=%.3f dependencyLatency=%d "
                        + "maxOutstanding=%d memoryQueueCapacity=%d "
                        + "memoryCompletionRate=%d cycles=%d warmup=%d measuredIPC=%.4f "
                        + "issueUtilization=%.4f memoryIssues=%d "
                        + "completedRequests=%d maxThreadOutstanding=%d "
                        + "maxQueueOccupancy=%d queueFullAttempts=%d queueWaiterCycles=%d "
                        + "dependencyWaiterCycles=%d serviceBacklogCycles=%d "
                        + "idleCycles=%d readyAtEnd=%d "
                        + "waitingAtThreadLimitAtEnd=%d "
                        + "waitingAtQueueLimitAtEnd=%d outstandingAtEnd=%d "
                        + "dependencyIssues=%d completedDependencies=%d "
                        + "dependenciesInFlightAtEnd=%d "
                        + "workspaceIssuesMin=%d workspaceIssuesMax=%d "
                        + "workspaceJainFairness=%.6f seed=%d%n",
                ready.name(), ready.clusterCount(), ready.steals(),
                ready.selectionProbes(), contexts, workspaceCount,
                hotWorkspaceCount, hotWorkspaceRatio, hotMemoryFraction,
                coldMemoryFraction, hotDependencyFraction, coldDependencyFraction,
                issueWidth, memoryFraction, dependencyFraction,
                dependencyLatency, maxOutstanding,
                memoryQueueCapacity, memoryCompletionRate, cycles, warmup, ipc, utilization,
                memoryIssues, completedRequests, maxThreadOutstandingObserved,
                maxQueueOccupancy, queueFullAttempts, queueWaiterCycles,
                dependencyWaiterCycles, serviceBacklogCycles, idleCycles, ready.size(),
                waitingAtThreadLimit, waitingAtQueueLimit, outstandingAtEnd,
                dependencyIssues, completedDependencies, dependencyWakeups.size(),
                minWorkspaceIssues, maxWorkspaceIssues, workspaceJainFairness, seed);

        for (int i = 0; i < memoryClasses.length; i++) {
            System.out.printf("memoryClass=%s latency=%d issues=%d completions=%d%n",
                    memoryClasses[i].name(), memoryClasses[i].latency(),
                    classIssues[i], classCompletions[i]);
        }
    }

    private interface ReadyScheduler {
        void addFirst(int thread);
        void addLast(int thread);
        int poll();
        int size();
        String name();
        int clusterCount();
        long steals();
        long selectionProbes();
    }

    private static ReadyScheduler createReadyScheduler(
            String strategy, int contexts, int clusters) {
        return switch (strategy.toLowerCase(java.util.Locale.ROOT)) {
            case "fifo" -> new FifoReadyScheduler(contexts);
            case "hierarchical" -> new HierarchicalReadyScheduler(contexts);
            case "distributed" -> new DistributedReadyScheduler(contexts, clusters);
            default -> throw new IllegalArgumentException(
                    "Unknown scheduler strategy: " + strategy);
        };
    }

    private static final class FifoReadyScheduler implements ReadyScheduler {
        private final ArrayDeque<Integer> queue;
        private long selectionProbes;

        private FifoReadyScheduler(int capacity) {
            queue = new ArrayDeque<>(capacity);
        }

        @Override
        public void addFirst(int thread) {
            queue.addFirst(thread);
        }

        @Override
        public void addLast(int thread) {
            queue.addLast(thread);
        }

        @Override
        public int poll() {
            selectionProbes++;
            return queue.isEmpty() ? -1 : queue.removeFirst();
        }

        @Override
        public int size() {
            return queue.size();
        }

        @Override
        public String name() {
            return "fifo";
        }

        @Override
        public int clusterCount() {
            return 1;
        }

        @Override
        public long steals() {
            return 0;
        }

        @Override
        public long selectionProbes() {
            return selectionProbes;
        }
    }

    private static final class HierarchicalReadyScheduler
            implements ReadyScheduler {
        private final List<ArrayDeque<Integer>> workspaceQueues;
        private final BitSet activeWorkspaces;
        private int cursor;
        private int size;
        private long selectionProbes;

        private HierarchicalReadyScheduler(int contexts) {
            int workspaces = (contexts + 15) / 16;
            workspaceQueues = new ArrayList<>(workspaces);
            for (int i = 0; i < workspaces; i++) {
                workspaceQueues.add(new ArrayDeque<>(16));
            }
            activeWorkspaces = new BitSet(workspaces);
        }

        @Override
        public void addFirst(int thread) {
            int workspace = thread / 16;
            workspaceQueues.get(workspace).addFirst(thread);
            activeWorkspaces.set(workspace);
            size++;
        }

        @Override
        public void addLast(int thread) {
            int workspace = thread / 16;
            workspaceQueues.get(workspace).addLast(thread);
            activeWorkspaces.set(workspace);
            size++;
        }

        @Override
        public int poll() {
            if (size == 0) {
                return -1;
            }
            selectionProbes++;
            int workspace = activeWorkspaces.nextSetBit(cursor);
            if (workspace < 0) {
                workspace = activeWorkspaces.nextSetBit(0);
            }
            int thread = workspaceQueues.get(workspace).removeFirst();
            size--;
            if (workspaceQueues.get(workspace).isEmpty()) {
                activeWorkspaces.clear(workspace);
            }
            cursor = (workspace + 1) % workspaceQueues.size();
            return thread;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public String name() {
            return "hierarchical";
        }

        @Override
        public int clusterCount() {
            return 1;
        }

        @Override
        public long steals() {
            return 0;
        }

        @Override
        public long selectionProbes() {
            return selectionProbes;
        }
    }

    private static final class DistributedReadyScheduler
            implements ReadyScheduler {
        private final List<ArrayDeque<Integer>> clusterQueues;
        private final int[] ownerCluster;
        private int cursor;
        private int size;
        private long steals;
        private long selectionProbes;

        private DistributedReadyScheduler(int contexts, int clusters) {
            if (clusters < 1) {
                throw new IllegalArgumentException("Cluster count must be positive");
            }
            clusterQueues = new ArrayList<>(clusters);
            for (int i = 0; i < clusters; i++) {
                clusterQueues.add(new ArrayDeque<>());
            }
            ownerCluster = new int[contexts];
            for (int thread = 0; thread < contexts; thread++) {
                ownerCluster[thread] = (thread / 16) % clusters;
            }
        }

        @Override
        public void addFirst(int thread) {
            clusterQueues.get(ownerCluster[thread]).addFirst(thread);
            size++;
        }

        @Override
        public void addLast(int thread) {
            clusterQueues.get(ownerCluster[thread]).addLast(thread);
            size++;
        }

        @Override
        public int poll() {
            if (size == 0) {
                return -1;
            }
            selectionProbes++;
            int source = -1;
            for (int offset = 0; offset < clusterQueues.size(); offset++) {
                int candidate = (cursor + offset) % clusterQueues.size();
                if (!clusterQueues.get(candidate).isEmpty()) {
                    source = candidate;
                    break;
                }
            }
            if (source < 0) {
                return -1;
            }

            int thread = clusterQueues.get(source).removeFirst();
            if (source != cursor) {
                steals++;
                ownerCluster[thread] = cursor;
            }
            size--;
            cursor = (cursor + 1) % clusterQueues.size();
            return thread;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public String name() {
            return "distributed";
        }

        @Override
        public int clusterCount() {
            return clusterQueues.size();
        }

        @Override
        public long steals() {
            return steals;
        }

        @Override
        public long selectionProbes() {
            return selectionProbes;
        }
    }

    private static int selectMemoryClass(
            MemoryClass[] memoryClasses, SplittableRandom random) {
        double selection = random.nextDouble();
        for (int i = 0; i < memoryClasses.length; i++) {
            if (selection < memoryClasses[i].cumulativeFraction()) {
                return i;
            }
        }
        return memoryClasses.length - 1;
    }

    private static void usage() {
        System.err.println("""
                Usage:
                  ContextSweep <contexts> <issue-width> <latency-cycles>
                    <memory-fraction> <cycles> <seed>
                    [max-outstanding [memory-queue-capacity
                    l1-fraction l1-latency l2-fraction l2-latency dram-latency
                    [dependency-fraction dependency-latency
                    [max-memory-completions-per-cycle]]]

                Use all five memory-class arguments together. The L1 and L2
                fractions are conditional probabilities among memory
                instructions; DRAM receives the remaining probability.
                The optional completion-rate argument caps completed memory
                requests per cycle; it is not a byte-bandwidth model.

                Example:
                  ContextSweep 4096 4 300 0.25 200000 42 4 128
                    0.70 4 0.20 20 200
                """);
    }
}
