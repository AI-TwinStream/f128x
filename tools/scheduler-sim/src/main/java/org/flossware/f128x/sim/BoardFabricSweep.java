package org.flossware.f128x.sim;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.SplittableRandom;

/**
 * Board/fabric experiment for modular f128x multiprocessor systems.
 *
 * Each board owns independent execution clusters and local memory service.
 * Workspaces are homed to boards; threads stay on their home board in this
 * first model. Remote memory operations traverse one shared, bandwidth-limited
 * fabric. This is a comparative queueing model, not RTL.
 */
public final class BoardFabricSweep {
    private record Wakeup(long cycle, int thread, boolean memoryRequest) {}

    private record Result(int boards, int clustersPerBoard, int issueWidthPerCluster,
            long instructions, long memoryRequests, long remoteRequests,
            long fabricBytes, long queueFullAttempts, long idleClusterCycles,
            long measuredCycles, long maxOutstanding, long[] boardIssues) {
        double ipc() {
            return (double) instructions / measuredCycles;
        }

        double utilization() {
            return (double) instructions
                    / (measuredCycles * boards * clustersPerBoard * issueWidthPerCluster);
        }
    }

    private BoardFabricSweep() {}

    public static void main(String[] args) {
        int contexts = Integer.getInteger("f128x.contexts", 1024);
        int workspaces = Integer.getInteger("f128x.workspaces", 64);
        int threadsPerWorkspace = Integer.getInteger("f128x.threadsPerWorkspace", 16);
        int cycles = Integer.getInteger("f128x.cycles", 30_000);
        int warmup = Integer.getInteger("f128x.warmup", 3_000);
        long seed = Long.getLong("f128x.seed", 42L);
        int localMemoryLatency = Integer.getInteger("f128x.localMemoryLatency", 80);
        int remoteMemoryLatency = Integer.getInteger("f128x.remoteMemoryLatency", 80);
        int fabricLatency = Integer.getInteger("f128x.fabricLatency", 20);
        int fabricBytesPerCycle = Integer.getInteger("f128x.fabricBytesPerCycle", 128);
        int localMemoryBytesPerCycle = Integer.getInteger(
                "f128x.localMemoryBytesPerCycle", 64);
        int fabricHopLatency = Integer.getInteger("f128x.fabricHopLatency", 2);
        int requestBytes = Integer.getInteger("f128x.requestBytes", 64);
        int queueCapacity = Integer.getInteger("f128x.queueCapacity", 4096);
        double memoryFraction = Double.parseDouble(
                System.getProperty("f128x.memoryFraction", "0.30"));
        double dependencyFraction = Double.parseDouble(
                System.getProperty("f128x.dependencyFraction", "0.15"));
        int dependencyLatency = Integer.getInteger("f128x.dependencyLatency", 12);

        if (contexts < 1 || workspaces < 1 || threadsPerWorkspace < 1
                || contexts > workspaces * threadsPerWorkspace || cycles < 2
                || warmup < 0 || warmup >= cycles || localMemoryLatency < 1
                || remoteMemoryLatency < 1 || fabricLatency < 0
                 || fabricBytesPerCycle < 1 || localMemoryBytesPerCycle < 1 || fabricHopLatency < 0\n                || requestBytes < 1 || queueCapacity < 1
                || !fraction(memoryFraction) || !fraction(dependencyFraction)
                || dependencyLatency < 1) {
            throw new IllegalArgumentException("Invalid board/fabric simulation parameter");
        }

        System.out.printf("board-fabric-sim=0.1 contexts=%d workspaces=%d cycles=%d warmup=%d "
                + "memoryFraction=%.3f dependencyFraction=%.3f localMemoryLatency=%d "
                + "remoteMemoryLatency=%d fabricLatency=%d fabricBytesPerCycle=%d "
                + "localMemoryBytesPerCycle=%d fabricHopLatency=%d requestBytes=%d queueCapacity=%d seed=%d%n",
                contexts, workspaces, cycles, warmup, memoryFraction, dependencyFraction,
                localMemoryLatency, remoteMemoryLatency, fabricLatency,
                fabricBytesPerCycle, localMemoryBytesPerCycle, fabricHopLatency, requestBytes, queueCapacity, seed);

        int[][] topologies = {{1, 8, 1}, {2, 4, 1}, {4, 2, 1}, {8, 1, 1}, {4, 4, 1}};
        double[] remoteFractions = {0.10, 0.50, 0.90};
        for (double remoteFraction : remoteFractions) {
            System.out.printf("%nworkload=remote-mix remoteFraction=%.2f%n", remoteFraction);
            for (int[] topology : topologies) {
                Result result = simulate(contexts, workspaces, threadsPerWorkspace,
                        cycles, warmup, seed, topology[0], topology[1], topology[2],
                        remoteFraction, memoryFraction, dependencyFraction,
                        dependencyLatency, localMemoryLatency, remoteMemoryLatency,
                        fabricLatency, fabricBytesPerCycle, localMemoryBytesPerCycle, fabricHopLatency,\n                        requestBytes, queueCapacity);
                System.out.printf("boards=%d clustersPerBoard=%d issueWidthPerCluster=%d "
                        + "aggregateIssueWidth=%d ipc=%.4f utilization=%.4f "
                        + "memoryRequests=%d remoteRequests=%d fabricBytes=%d "
                        + "queueFullAttempts=%d idleClusterCycles=%d maxOutstanding=%d%n",
                        result.boards(), result.clustersPerBoard(),
                        result.issueWidthPerCluster(),
                        result.boards() * result.clustersPerBoard()
                                * result.issueWidthPerCluster(),
                        result.ipc(), result.utilization(), result.memoryRequests(),
                        result.remoteRequests(), result.fabricBytes(),
                        result.queueFullAttempts(), result.idleClusterCycles(),
                        result.maxOutstanding());
                System.out.print("boardIssues=");
                for (int board = 0; board < result.boardIssues().length; board++) {
                    System.out.printf("%s%d:%d", board == 0 ? "" : ",",
                            board, result.boardIssues()[board]);
                }
                System.out.println();
            }
        }
    }

    private static Result simulate(int contexts, int workspaces, int threadsPerWorkspace,
            int cycles, int warmup, long seed, int boards, int clustersPerBoard,
            int issueWidthPerCluster, double remoteFraction, double memoryFraction,
            double dependencyFraction, int dependencyLatency, int localMemoryLatency,
            int remoteMemoryLatency, int fabricLatency, int fabricBytesPerCycle,
            int localMemoryBytesPerCycle, int fabricHopLatency,
            int requestBytes, int queueCapacity) {
        int clusterCount = boards * clustersPerBoard;
        List<ArrayDeque<Integer>> ready = new ArrayList<>(clusterCount);
        for (int i = 0; i < clusterCount; i++) {
            ready.add(new ArrayDeque<>());
        }
        int[] homeCluster = new int[contexts];
        for (int thread = 0; thread < contexts; thread++) {
            int workspace = thread / threadsPerWorkspace;
            int board = workspace % boards;
            int cluster = board * clustersPerBoard
                    + ((workspace / boards) % clustersPerBoard);
            homeCluster[thread] = cluster;
            ready.get(cluster).addLast(thread);
        }

        var completions = new PriorityQueue<Wakeup>(Comparator.comparingLong(Wakeup::cycle));
        var random = new SplittableRandom(seed);
        long instructions = 0;
        long memoryRequests = 0;
        long remoteRequests = 0;
        long fabricBytes = 0;
        long queueFullAttempts = 0;
        long idleClusterCycles = 0;
        long maxOutstanding = 0;
        long outstandingCount = 0;
        long fabricAvailableCycle = 0;
        long[] boardMemoryAvailableCycle = new long[boards];
        long[] boardIssues = new long[boards];
        int measuredCycles = cycles - warmup;

        for (int cycle = 0; cycle < cycles; cycle++) {
            while (!completions.isEmpty() && completions.peek().cycle() <= cycle) {
                Wakeup wakeup = completions.remove();
                ready.get(homeCluster[wakeup.thread()]).addLast(wakeup.thread());
                if (wakeup.memoryRequest()) {
                    outstandingCount--;
                }
            }

            for (int cluster = 0; cluster < clusterCount; cluster++) {
                int board = cluster / clustersPerBoard;
                int issuedOnCluster = 0;
                int candidates = ready.get(cluster).size();
                while (candidates-- > 0 && issuedOnCluster < issueWidthPerCluster) {
                    int thread = ready.get(cluster).removeFirst();
                    double draw = random.nextDouble();
                    if (draw < memoryFraction) {
                        if (outstandingCount >= queueCapacity) {
                            queueFullAttempts++;
                            ready.get(cluster).addLast(thread);
                            continue;
                        }
                        boolean remote = boards > 1
                                && random.nextDouble() < remoteFraction;
                        long completionCycle;
                        if (remote) {
                            int targetBoard = random.nextInt(boards - 1);
                            if (targetBoard >= board) {
                                targetBoard++;
                            }
                            long requestCycles = Math.max(1,
                                    (requestBytes + fabricBytesPerCycle - 1L)
                                            / fabricBytesPerCycle);
                            long requestStart = Math.max(cycle, fabricAvailableCycle);
                            long requestEnd = requestStart + requestCycles;
                            long memoryCycles = Math.max(1,
                                    (requestBytes + localMemoryBytesPerCycle - 1L)
                                            / localMemoryBytesPerCycle);
                            long memoryStart = Math.max(requestEnd,
                                    boardMemoryAvailableCycle[targetBoard]);
                            long memoryEnd = memoryStart + memoryCycles;
                            boardMemoryAvailableCycle[targetBoard] = memoryEnd;
                            long responseStart = Math.max(
                                    memoryEnd + remoteMemoryLatency, requestEnd);
                            long responseEnd = responseStart + requestCycles;
                            long hops = 32 - Integer.numberOfLeadingZeros(boards - 1);
                            long roundTripFabricLatency = 2L * (fabricLatency + hops * fabricHopLatency);
                            completionCycle = responseEnd + roundTripFabricLatency;
                            fabricAvailableCycle = responseEnd;
                            remoteRequests++;
                            fabricBytes += 2L * requestBytes;
                        } else {
                            long memoryCycles = Math.max(1,
                                    (requestBytes + localMemoryBytesPerCycle - 1L)
                                            / localMemoryBytesPerCycle);
                            long memoryStart = Math.max(cycle,
                                    boardMemoryAvailableCycle[board]);
                            completionCycle = memoryStart + memoryCycles + localMemoryLatency;
                            boardMemoryAvailableCycle[board] = memoryStart + memoryCycles;
                        }
                        memoryRequests++;
                        outstandingCount++;
                        maxOutstanding = Math.max(maxOutstanding, outstandingCount);
                        completions.add(new Wakeup(completionCycle, thread, true));
                    } else if (random.nextDouble() < dependencyFraction) {
                        completions.add(new Wakeup(
                                (long) cycle + dependencyLatency, thread, false));
                    } else {
                        ready.get(cluster).addLast(thread);
                    }

                    issuedOnCluster++;
                    if (cycle >= warmup) {
                        instructions++;
                        boardIssues[board]++;
                    }
                }
                idleClusterCycles += issueWidthPerCluster - issuedOnCluster;
            }
        }

        return new Result(boards, clustersPerBoard, issueWidthPerCluster,
                instructions, memoryRequests, remoteRequests, fabricBytes,
                queueFullAttempts, idleClusterCycles, measuredCycles,
                maxOutstanding, boardIssues);
    }

    private static boolean fraction(double value) {
        return Double.isFinite(value) && value >= 0.0 && value <= 1.0;
    }
}
