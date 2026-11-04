package org.flossware.f128x.sim;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            long measuredCycles, long maxOutstanding, long[] boardIssues,
            long[] boardRemoteRequests) {
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
        FabricTopologySweep.verifyDeterministicSmoke();
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
        int localMemoryLanesPerBoard = Integer.getInteger(
                "f128x.localMemoryLanesPerBoard", 1);
        String remotePattern = System.getProperty("f128x.remotePattern", "uniform");
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
                || fabricBytesPerCycle < 1 || localMemoryBytesPerCycle < 1
                || localMemoryLanesPerBoard < 1
                || !(remotePattern.equals("uniform") || remotePattern.equals("hotspot")
                        || remotePattern.equals("paired"))
                || fabricHopLatency < 0 || requestBytes < 1 || queueCapacity < 1
                || !fraction(memoryFraction) || !fraction(dependencyFraction)
                || dependencyLatency < 1) {
            throw new IllegalArgumentException("Invalid board/fabric simulation parameter");
        }

        System.out.printf("board-fabric-sim=0.1 contexts=%d workspaces=%d cycles=%d warmup=%d "
                + "memoryFraction=%.3f dependencyFraction=%.3f localMemoryLatency=%d "
                + "remoteMemoryLatency=%d fabricLatency=%d fabricBytesPerCycle=%d "
                + "localMemoryBytesPerCycle=%d localMemoryLanesPerBoard=%d "
                + "remotePattern=%s fabricHopLatency=%d requestBytes=%d "
                + "queueCapacity=%d seed=%d%n",
                contexts, workspaces, cycles, warmup, memoryFraction, dependencyFraction,
                localMemoryLatency, remoteMemoryLatency, fabricLatency,
                fabricBytesPerCycle, localMemoryBytesPerCycle,
                localMemoryLanesPerBoard, remotePattern, fabricHopLatency,
                requestBytes, queueCapacity, seed);

        int[][] topologies = {{1, 8, 1}, {2, 4, 1}, {4, 2, 1}, {8, 1, 1}, {4, 4, 1}};
        double[] remoteFractions = {0.10, 0.50, 0.90};
        for (double remoteFraction : remoteFractions) {
            System.out.printf("%nworkload=remote-mix remoteFraction=%.2f%n", remoteFraction);
            for (int[] topology : topologies) {
                Result result = simulate(contexts, workspaces, threadsPerWorkspace,
                        cycles, warmup, seed, topology[0], topology[1], topology[2],
                        remoteFraction, memoryFraction, dependencyFraction,
                        dependencyLatency, localMemoryLatency, remoteMemoryLatency,
                        fabricLatency, fabricBytesPerCycle, localMemoryBytesPerCycle,
                        localMemoryLanesPerBoard, remotePattern, fabricHopLatency,
                        requestBytes, queueCapacity);
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
                System.out.print("boardRemoteRequests=");
                for (int board = 0; board < result.boardRemoteRequests().length; board++) {
                    System.out.printf("%s%d:%d", board == 0 ? "" : ",",
                            board, result.boardRemoteRequests()[board]);
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
            int localMemoryBytesPerCycle, int localMemoryLanesPerBoard,
            String remotePattern, int fabricHopLatency,
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
        long requestFabricAvailableCycle = 0;
        long responseFabricAvailableCycle = 0;
        long[][] boardMemoryAvailableCycle =
                new long[boards][localMemoryLanesPerBoard];
        long[] boardIssues = new long[boards];
        long[] boardRemoteRequests = new long[boards];
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
                            int targetBoard = selectRemoteBoard(
                                    board, boards, remotePattern, random);
                            long requestCycles = Math.max(1,
                                    (requestBytes + fabricBytesPerCycle - 1L)
                                            / fabricBytesPerCycle);
                            long requestStart = Math.max(cycle, requestFabricAvailableCycle);
                            long requestEnd = requestStart + requestCycles;
                            long memoryCycles = Math.max(1,
                                    (requestBytes + localMemoryBytesPerCycle - 1L)
                                            / localMemoryBytesPerCycle);
                            int memoryLane = selectEarliestLane(
                                    boardMemoryAvailableCycle[targetBoard]);
                            long memoryStart = Math.max(requestEnd,
                                    boardMemoryAvailableCycle[targetBoard][memoryLane]);
                            long memoryEnd = memoryStart + memoryCycles;
                            boardMemoryAvailableCycle[targetBoard][memoryLane] = memoryEnd;
                            long responseStart = Math.max(
                                    memoryEnd + remoteMemoryLatency, responseFabricAvailableCycle);
                            long responseEnd = responseStart + requestCycles;
                            long hops = 32 - Integer.numberOfLeadingZeros(boards - 1);
                            long roundTripFabricLatency = 2L
                                    * (fabricLatency + hops * fabricHopLatency);
                            completionCycle = responseEnd + roundTripFabricLatency;
                            requestFabricAvailableCycle = requestEnd;
                            responseFabricAvailableCycle = responseEnd;
                            remoteRequests++;
                            boardRemoteRequests[targetBoard]++;
                            fabricBytes += 2L * requestBytes;
                        } else {
                            long memoryCycles = Math.max(1,
                                    (requestBytes + localMemoryBytesPerCycle - 1L)
                                            / localMemoryBytesPerCycle);
                            int memoryLane = selectEarliestLane(
                                    boardMemoryAvailableCycle[board]);
                            long memoryStart = Math.max(cycle,
                                    boardMemoryAvailableCycle[board][memoryLane]);
                            completionCycle = memoryStart + memoryCycles + localMemoryLatency;
                            boardMemoryAvailableCycle[board][memoryLane] =
                                    memoryStart + memoryCycles;
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
                maxOutstanding, boardIssues, boardRemoteRequests);
    }

    private static int selectEarliestLane(long[] availableCycles) {
        int selected = 0;
        for (int lane = 1; lane < availableCycles.length; lane++) {
            if (availableCycles[lane] < availableCycles[selected]) {
                selected = lane;
            }
        }
        return selected;
    }

    private static int selectRemoteBoard(int sourceBoard, int boards,
            String pattern, SplittableRandom random) {
        return switch (pattern) {
            case "uniform" -> {
                int target = random.nextInt(boards - 1);
                yield target >= sourceBoard ? target + 1 : target;
            }
            case "hotspot" -> sourceBoard == 0 ? 1 : 0;
            case "paired" -> {
                int paired = sourceBoard ^ 1;
                yield paired < boards ? paired : (sourceBoard + 1) % boards;
            }
            default -> throw new IllegalArgumentException(
                    "Unknown remote access pattern: " + pattern);
        };
    }

    private static boolean fraction(double value) {
        return Double.isFinite(value) && value >= 0.0 && value <= 1.0;
    }
}


/**
 * Independent switched-fabric sensitivity experiment. This class deliberately
 * shares the source file so the existing Java 25 CI compile covers it.
 */
final class FabricTopologySweep {
    private record Transaction(long injectionCycle, int source, int target) {}
    private record Result(String topology, int boards, String pattern, int links,
            int bisectionEdges, int perLinkBytesPerCycle, int bisectionBytesPerCycle,
            int injectionInterval, int memoryLanes, String arbitration, int operations,
            double averageLatency, double p95Latency, double completionCycle,
            double operationsPerCycle, double minSourceMeanLatency,
            double maxSourceMeanLatency, double latencyFairnessJain) {}

    private FabricTopologySweep() {}

    static void verifyDeterministicSmoke() {
        int cases = 0;
        for (int boards : new int[] {4, 8, 16}) {
            for (String pattern : new String[] {"uniform", "hotspot", "paired"}) {
                for (String topology : new String[] {"shared", "ring", "mesh"}) {
                    for (int links : new int[] {1, 2, 4}) {
                        Result first = simulate(boards, pattern, topology, links, 64, 42L, 2, 1);
                        Result second = simulate(boards, pattern, topology, links, 64, 42L, 2, 1);
                        if (!first.equals(second) || first.operations() != boards * 64
                                || first.averageLatency() <= 0.0 || first.p95Latency() <= 0) {
                            throw new IllegalStateException("Fabric topology deterministic smoke failed: "
                                    + topology + "/" + boards + "/" + pattern + "/" + links);
                        }
                        cases++;
                    }
                }
            }
        }
        System.out.printf("fabricTopologySmoke=passed cases=%d%n", cases);
    }

    public static void main(String[] args) {
        int operationsPerBoard = Integer.getInteger("f128x.fabric.operationsPerBoard", 2048);
        long seed = Long.getLong("f128x.fabric.seed", 42L);
        if (operationsPerBoard < 1) {
            throw new IllegalArgumentException("operationsPerBoard must be positive");
        }
        int injectionInterval = Integer.getInteger("f128x.fabric.injectionInterval", 2);
        int memoryLanes = Integer.getInteger("f128x.fabric.memoryLanesPerBoard", 1);
        if (memoryLanes < 1) {
            throw new IllegalArgumentException("memoryLanesPerBoard must be positive");
        }
        String arbitration = System.getProperty("f128x.fabric.arbitration", "round_robin");
        if (!(arbitration.equals("fixed") || arbitration.equals("round_robin"))) {
            throw new IllegalArgumentException("arbitration must be fixed or round_robin");
        }
        if (injectionInterval < 1) {
            throw new IllegalArgumentException("injectionInterval must be positive");
        }
        System.out.printf("fabric-topology-sim=0.1 operationsPerBoard=%d seed=%d injectionInterval=%d memoryLanesPerBoard=%d arbitration=%s%n",
                operationsPerBoard, seed, injectionInterval, memoryLanes, arbitration);
        for (int boards : new int[] {4, 8, 16}) {
            for (String pattern : new String[] {"uniform", "hotspot", "paired"}) {
                for (String topology : new String[] {"shared", "ring", "mesh"}) {
                    for (int links : new int[] {1, 2, 4}) {
                        Result result = simulate(boards, pattern, topology,
                                links, operationsPerBoard, seed, injectionInterval, memoryLanes, arbitration);
                        System.out.printf("topology=%s boards=%d pattern=%s linksPerNeighbor=%d "
                                        + "bisectionEdges=%d perLinkBytesPerCycle=%d "
                                        + "bisectionBandwidthBytesPerCycle=%d injectionInterval=%d memoryLanesPerBoard=%d "
                                        + "arbitration=%s remoteOperations=%d averageLatency=%.2f "
                                        + "p95Latency=%.2f completionCycle=%.2f operationsPerCycle=%.5f "
                                        + "minSourceMeanLatency=%.2f maxSourceMeanLatency=%.2f "
                                        + "latencyFairnessJain=%.5f%n",
                                result.topology(), result.boards(), result.pattern(),
                                result.links(), result.bisectionEdges(), result.perLinkBytesPerCycle(),
                                result.bisectionBytesPerCycle(), result.injectionInterval(), result.memoryLanes(),
                                result.arbitration(), result.operations(), result.averageLatency(),
                                result.p95Latency(), result.completionCycle(),
                                result.operationsPerCycle(), result.minSourceMeanLatency(),
                                result.maxSourceMeanLatency(), result.latencyFairnessJain());
                    }
                }
            }
        }
    }

    private static Result simulate(int boards, String pattern, String topology,
            int links, int operationsPerBoard, long seed, int injectionInterval,
            int memoryLanes, String arbitration) {
        if (boards < 2 || links < 1 || operationsPerBoard < 1
                || !(pattern.equals("uniform") || pattern.equals("hotspot")
                        || pattern.equals("paired"))
                || !(topology.equals("shared") || topology.equals("ring")
                        || topology.equals("mesh"))
                || !(arbitration.equals("fixed") || arbitration.equals("round_robin"))) {
            throw new IllegalArgumentException("Invalid fabric topology parameter");
        }
        final int requestBytes = 64;
        final int targetBisectionBytesPerCycle = 192;
        final int bisectionEdges = bisectionEdges(boards, topology);
        final int fabricBytesPerCycle = targetBisectionBytesPerCycle / bisectionEdges;
        final int memoryBytesPerCycle = 64;
        final int memoryLatency = 80;
        final int fabricBaseLatency = 20;
        final int hopLatency = 2;
        if (injectionInterval < 1) {
            throw new IllegalArgumentException("injectionInterval must be positive");
        }
        if (memoryLanes < 1) {
            throw new IllegalArgumentException("memoryLanesPerBoard must be positive");
        }

        Network network = new Network(boards, topology, links, fabricBytesPerCycle);
        double[][] memoryAvailable = new double[boards][memoryLanes];
        List<Transaction> transactions = new ArrayList<>(boards * operationsPerBoard);
        for (int source = 0; source < boards; source++) {
            SplittableRandom random = new SplittableRandom(seed + source * 0x9E3779B97F4A7C15L);
            for (int operation = 0; operation < operationsPerBoard; operation++) {
                int target = switch (pattern) {
                    case "uniform" -> {
                        int selected = random.nextInt(boards - 1);
                        yield selected >= source ? selected + 1 : selected;
                    }
                    case "hotspot" -> source == 0 ? 1 : 0;
                    case "paired" -> (source ^ 1) < boards ? (source ^ 1) : (source + 1) % boards;
                    default -> throw new IllegalArgumentException("Unknown traffic pattern");
                };
                transactions.add(new Transaction((long) operation * injectionInterval,
                        source, target));
            }
        }
        transactions.sort(Comparator.comparingLong(Transaction::injectionCycle)
                .thenComparingInt(transaction -> arbitration.equals("fixed")
                        ? transaction.source()
                        : Math.floorMod(transaction.source()
                                - (int) ((transaction.injectionCycle() / injectionInterval) % boards),
                                boards)));

        double[] latencies = new double[transactions.size()];
        double[] sourceLatencyTotals = new double[boards];
        int[] sourceOperations = new int[boards];
        double lastCompletion = 0.0;
        int index = 0;
        for (Transaction transaction : transactions) {
            double requestEnd = network.transfer(transaction.source(), transaction.target(),
                    transaction.injectionCycle(), requestBytes, false)
                    + fabricBaseLatency;
            int lane = earliestLane(memoryAvailable[transaction.target()]);
            double memoryStart = Math.max(requestEnd, memoryAvailable[transaction.target()][lane]);
            double memoryEnd = memoryStart + requestBytes / (double) memoryBytesPerCycle;
            memoryAvailable[transaction.target()][lane] = memoryEnd;
            double responseEnd = network.transfer(transaction.target(), transaction.source(),
                    memoryEnd + memoryLatency, requestBytes, true) + fabricBaseLatency;
            int hops = network.hops(transaction.source(), transaction.target());
            double completion = responseEnd + 2.0 * hops * hopLatency;
            double latency = completion - transaction.injectionCycle();
            latencies[index++] = latency;
            sourceLatencyTotals[transaction.source()] += latency;
            sourceOperations[transaction.source()]++;
            lastCompletion = Math.max(lastCompletion, completion);
        }
        Arrays.sort(latencies);
        double total = 0.0;
        for (double latency : latencies) {
            total += latency;
        }
        double average = total / latencies.length;
        double p95 = latencies[Math.min(latencies.length - 1,
                (int) Math.ceil(latencies.length * 0.95) - 1)];
        double minSourceMeanLatency = Double.POSITIVE_INFINITY;
        double maxSourceMeanLatency = 0.0;
        double reciprocalLatencyTotal = 0.0;
        double reciprocalLatencySquares = 0.0;
        for (int source = 0; source < boards; source++) {
            if (sourceOperations[source] == 0) {
                throw new IllegalStateException("A source received no arbitration service");
            }
            double sourceMean = sourceLatencyTotals[source] / sourceOperations[source];
            minSourceMeanLatency = Math.min(minSourceMeanLatency, sourceMean);
            maxSourceMeanLatency = Math.max(maxSourceMeanLatency, sourceMean);
            double reciprocal = 1.0 / sourceMean;
            reciprocalLatencyTotal += reciprocal;
            reciprocalLatencySquares += reciprocal * reciprocal;
        }
        double latencyFairnessJain = reciprocalLatencyTotal * reciprocalLatencyTotal
                / (boards * reciprocalLatencySquares);
        return new Result(topology, boards, pattern, links, bisectionEdges,
                fabricBytesPerCycle, fabricBytesPerCycle * bisectionEdges * links,
                injectionInterval, memoryLanes, arbitration, latencies.length, average, p95,
                lastCompletion, (double) latencies.length / lastCompletion,
                minSourceMeanLatency, maxSourceMeanLatency, latencyFairnessJain);
    }

    private static int bisectionEdges(int boards, String topology) {
        if (topology.equals("shared")) {
            return 1;
        }
        boolean[][] edge = new boolean[boards][boards];
        if (topology.equals("ring")) {
            for (int board = 0; board < boards; board++) {
                edge[board][(board + 1) % boards] = true;
                edge[board][(board + boards - 1) % boards] = true;
            }
        } else if (topology.equals("mesh")) {
            int columns = (int) Math.ceil(Math.sqrt(boards));
            for (int board = 0; board < boards; board++) {
                int row = board / columns;
                int column = board % columns;
                addMeshEdge(edge, board, row, column - 1, columns, boards);
                addMeshEdge(edge, board, row, column + 1, columns, boards);
                addMeshEdge(edge, board, row - 1, column, columns, boards);
                addMeshEdge(edge, board, row + 1, column, columns, boards);
            }
        } else {
            throw new IllegalArgumentException("Unknown topology: " + topology);
        }
        int selectedSize = boards / 2;
        int minimum = Integer.MAX_VALUE;
        long limit = 1L << boards;
        for (long mask = 1; mask < limit; mask++) {
            if ((mask & 1L) == 0 || Long.bitCount(mask) != selectedSize) {
                continue;
            }
            int crossing = 0;
            for (int left = 0; left < boards; left++) {
                if ((mask & (1L << left)) == 0) continue;
                for (int right = 0; right < boards; right++) {
                    if ((mask & (1L << right)) == 0 && edge[left][right]) crossing++;
                }
            }
            minimum = Math.min(minimum, crossing);
        }
        if (minimum == Integer.MAX_VALUE || minimum < 1) {
            throw new IllegalStateException("Could not determine topology bisection");
        }
        return minimum;
    }

    private static void addMeshEdge(boolean[][] edge, int source, int row,
            int column, int columns, int boards) {
        if (row < 0 || column < 0) return;
        int target = row * columns + column;
        if (target < boards) edge[source][target] = true;
    }

    private static int earliestLane(double[] available) {
        int selected = 0;
        for (int lane = 1; lane < available.length; lane++) {
            if (available[lane] < available[selected]) {
                selected = lane;
            }
        }
        return selected;
    }

    private static final class Network {
        private final int boards;
        private final String topology;
        private final int links;
        private final int bytesPerCycle;
        private final List<List<Integer>> neighbors;
        private final Map<Long, Integer> directedEdges = new HashMap<>();
        private final double[][][] available;

        Network(int boards, String topology, int links, int bytesPerCycle) {
            this.boards = boards;
            this.topology = topology;
            this.links = links;
            this.bytesPerCycle = bytesPerCycle;
            this.neighbors = new ArrayList<>(boards);
            for (int board = 0; board < boards; board++) {
                neighbors.add(new ArrayList<>());
            }
            if (topology.equals("ring")) {
                for (int board = 0; board < boards; board++) {
                    addNeighbor(board, (board + 1) % boards);
                    addNeighbor(board, (board + boards - 1) % boards);
                }
            } else if (topology.equals("mesh")) {
                int columns = (int) Math.ceil(Math.sqrt(boards));
                for (int board = 0; board < boards; board++) {
                    int row = board / columns;
                    int column = board % columns;
                    addIfPresent(board, row, column - 1, columns);
                    addIfPresent(board, row, column + 1, columns);
                    addIfPresent(board, row - 1, column, columns);
                    addIfPresent(board, row + 1, column, columns);
                }
            }
            if (topology.equals("shared")) {
                directedEdges.put(0L, 0);
            } else {
                for (int source = 0; source < boards; source++) {
                    for (int target : neighbors.get(source)) {
                        directedEdges.put(edgeKey(source, target), directedEdges.size());
                    }
                }
            }
            available = new double[2][directedEdges.size()][links];
        }

        double transfer(int source, int target, double earliest, int bytes, boolean response) {
            List<Integer> route = route(source, target);
            double cursor = earliest;
            int direction = response ? 1 : 0;
            double serviceCycles = bytes / (double) bytesPerCycle;
            for (int edge : route) {
                int selected = 0;
                for (int lane = 1; lane < links; lane++) {
                    if (available[direction][edge][lane]
                            < available[direction][edge][selected]) {
                        selected = lane;
                    }
                }
                double start = Math.max(cursor, available[direction][edge][selected]);
                cursor = start + serviceCycles;
                available[direction][edge][selected] = cursor;
            }
            return cursor;
        }

        int hops(int source, int target) {
            return route(source, target).size();
        }

        private List<Integer> route(int source, int target) {
            if (topology.equals("shared")) {
                return List.of(0);
            }
            int[] previous = new int[boards];
            Arrays.fill(previous, -1);
            int[] previousEdge = new int[boards];
            Arrays.fill(previousEdge, -1);
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(source);
            previous[source] = source;
            while (!queue.isEmpty() && previous[target] == -1) {
                int current = queue.removeFirst();
                for (int next : neighbors.get(current)) {
                    if (previous[next] == -1) {
                        previous[next] = current;
                        previousEdge[next] = directedEdges.get(edgeKey(current, next));
                        queue.addLast(next);
                    }
                }
            }
            if (previous[target] == -1) {
                throw new IllegalStateException("Fabric topology is disconnected");
            }
            ArrayDeque<Integer> path = new ArrayDeque<>();
            for (int current = target; current != source; current = previous[current]) {
                path.addFirst(previousEdge[current]);
            }
            return new ArrayList<>(path);
        }

        private void addIfPresent(int source, int row, int column, int columns) {
            if (row < 0 || column < 0) {
                return;
            }
            int target = row * columns + column;
            if (target < boards) {
                addNeighbor(source, target);
            }
        }

        private void addNeighbor(int source, int target) {
            if (!neighbors.get(source).contains(target)) {
                neighbors.get(source).add(target);
            }
        }

        private static long edgeKey(int source, int target) {
            return ((long) source << 32) | (target & 0xffffffffL);
        }
    }
}
