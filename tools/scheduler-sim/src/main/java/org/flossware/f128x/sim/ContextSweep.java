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
    private record Completion(long cycle, int thread, int latencyClass,
            int channel, int bank, int requestBytes) {}

    private record DependencyWakeup(long cycle, int thread) {}

    private record MigrationWakeup(long cycle, int thread) {}

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
        int dependencyChains = Integer.getInteger("f128x.dependencyChains", 1);
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
            if (!Double.isFinite(l1Fraction) || !Double.isFinite(l2Fraction)
                    || l1Fraction < 0.0 || l2Fraction < 0.0
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

        int workspaceCount = (contexts + 15) / 16;
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

        if (contexts < 1 || issueWidth < 1 || defaultLatency < 1
                || cycles < 1 || maxOutstanding < 1
                || memoryQueueCapacity < 1 || !Double.isFinite(memoryFraction)
                || memoryFraction < 0.0 || memoryFraction > 1.0
                || !Double.isFinite(dependencyFraction) || dependencyFraction < 0.0
                || dependencyFraction > 1.0 || dependencyLatency < 1
                || dependencyChains < 1 || dependencyChains > 128
                || memoryCompletionRate < 1 || !Double.isFinite(hotWorkspaceRatio)
                || hotWorkspaceRatio < 0.0 || hotWorkspaceRatio > 1.0
                || !Double.isFinite(hotMemoryFraction) || hotMemoryFraction < 0.0
                || hotMemoryFraction > 1.0 || !Double.isFinite(coldMemoryFraction)
                || coldMemoryFraction < 0.0 || coldMemoryFraction > 1.0
                || !Double.isFinite(hotDependencyFraction)
                || hotDependencyFraction < 0.0 || hotDependencyFraction > 1.0
                || !Double.isFinite(coldDependencyFraction)
                || coldDependencyFraction < 0.0 || coldDependencyFraction > 1.0) {
            throw new IllegalArgumentException("Invalid simulation parameter");
        }
        String schedulerStrategy = System.getProperty("f128x.scheduler", "fifo");
        int defaultClusters = Math.max(1, Math.min(8, workspaceCount));
        int clusterCount = Integer.getInteger("f128x.clusters", defaultClusters);
        int clusterIssueWidth = Integer.getInteger(
                "f128x.clusterIssueWidth", issueWidth);
        int migrationLatency = Integer.getInteger(
                "f128x.migrationLatency", 0);
        int remoteMemoryLatencyPenalty = Integer.getInteger(
                "f128x.remoteMemoryLatencyPenalty", 0);
        int memoryRequestBytes = Integer.getInteger(
                "f128x.memoryRequestBytes", 64);
        int memoryRequestMinBytes = Integer.getInteger(
                "f128x.memoryRequestMinBytes", memoryRequestBytes);
        int memoryRequestMaxBytes = Integer.getInteger(
                "f128x.memoryRequestMaxBytes", memoryRequestBytes);
        int memoryChannels = Integer.getInteger("f128x.memoryChannels", 1);
        int memoryBanksPerChannel = Integer.getInteger(
                "f128x.memoryBanksPerChannel", 1);
        int memoryBandwidthBytesPerCycle = Integer.getInteger(
                "f128x.memoryBandwidthBytesPerCycle", 0);
        int bankConflictPenalty = Integer.getInteger(
                "f128x.bankConflictPenalty", 0);
        boolean memorySystemEnabled = memoryBandwidthBytesPerCycle > 0
                || memoryChannels > 1 || memoryBanksPerChannel > 1
                || bankConflictPenalty > 0;
        long aggregateMemoryBandwidthBytesPerCycle =
                (long) memoryBandwidthBytesPerCycle * memoryChannels;
        if (clusterCount < 1 || clusterIssueWidth < 1
                || migrationLatency < 0 || remoteMemoryLatencyPenalty < 0
                || memoryRequestBytes < 1 || memoryRequestMinBytes < 1
                || memoryRequestMaxBytes < memoryRequestMinBytes
                || memoryChannels < 1 || memoryBanksPerChannel < 1
                || memoryBandwidthBytesPerCycle < 0
                || bankConflictPenalty < 0) {
            throw new IllegalArgumentException(
                    "Cluster count and cluster issue width must be positive; "
                            + "migration and remote-memory penalties cannot be negative");
        }
        ReadyScheduler ready = createReadyScheduler(
                schedulerStrategy, contexts, clusterCount);
        int effectiveIssueCapacity = (int) Math.min(
                issueWidth, (long) ready.clusterCount() * clusterIssueWidth);

        for (MemoryClass memoryClass : memoryClasses) {
            if (memoryClass.latency() < 1) {
                throw new IllegalArgumentException(
                        "Memory-class latencies must be positive");
            }
        }

        var queueWaiters = new ArrayDeque<Integer>();
        var completions = new PriorityQueue<>(
                Comparator.comparingLong(Completion::cycle));
        var dependencyWakeups = new PriorityQueue<>(
                Comparator.comparingLong(DependencyWakeup::cycle));
        var migrationWakeups = new PriorityQueue<>(
                Comparator.comparingLong(MigrationWakeup::cycle));
        long[][] bankAvailableCycle = new long[memoryChannels][memoryBanksPerChannel];
        int[][] bankOutstanding = new int[memoryChannels][memoryBanksPerChannel];
        int[] outstandingByThread = new int[contexts];
        boolean[] waitingForThreadSlot = new boolean[contexts];
        boolean[] waitingForQueueSlot = new boolean[contexts];
        int[] pendingClass = new int[contexts];
        long[][] dependencyReadyCycles = new long[contexts][dependencyChains];
        int[] dependencyChainCursor = new int[contexts];
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
        long dependencyIssuesOverlapped = 0;
        long dependencyStallWakeups = 0;
        long remoteWakeups = 0;
        long remoteMemoryIssues = 0;
        long remoteMemoryLatencyCycles = 0;
        long migrationWaiterCycles = 0;
        long migrationCompletions = 0;
        long memoryBytesIssued = 0;
        long memoryBytesCompleted = 0;
        long memoryServiceCycles = 0;
        long bankConflictRequests = 0;
        long bankConflictWaitCycles = 0;
        int maxBankOutstanding = 0;
        int minObservedRequestBytes = Integer.MAX_VALUE;
        int maxObservedRequestBytes = 0;
        long maxQueueOccupancy = 0;
        int maxThreadOutstandingObserved = 0;
        long[] workspaceIssues = new long[workspaceCount];
        long[] workspaceMeasuredIssues = new long[workspaceCount];
        long[] workspaceRunnableThreadCycles = new long[workspaceCount];
        long[] workspaceRunnableWorkspaceCycles = new long[workspaceCount];
        long[] workspaceRemoteThreadCycles = new long[workspaceCount];
        long[] classIssues = new long[memoryClasses.length];
        long[] classCompletions = new long[memoryClasses.length];
        long[] clusterIssues = new long[ready.clusterCount()];
        int[] clusterIssuedThisCycle = new int[ready.clusterCount()];
        int nextCluster = 0;
        final int warmup = Math.min(10_000, cycles / 10);

        for (int cycle = 0; cycle < cycles; cycle++) {
            queueWaiterCycles += queueWaiters.size();
            dependencyWaiterCycles += dependencyWakeups.size();
            migrationWaiterCycles += migrationWakeups.size();
            if (cycle >= warmup) {
                ready.accumulateRemoteThreadCycles(workspaceRemoteThreadCycles);
            }

            while (!migrationWakeups.isEmpty()
                    && migrationWakeups.peek().cycle() <= cycle) {
                MigrationWakeup wakeup = migrationWakeups.remove();
                ready.addLast(wakeup.thread());
                migrationCompletions++;
            }

            while (!dependencyWakeups.isEmpty()
                    && dependencyWakeups.peek().cycle() <= cycle) {
                DependencyWakeup wakeup = dependencyWakeups.remove();
                if (ready.isRemote(wakeup.thread())) {
                    remoteWakeups++;
                }
                ready.addLast(wakeup.thread());
                dependencyStallWakeups++;
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
                memoryBytesCompleted += completion.requestBytes();
                if (memorySystemEnabled) {
                    int channel = completion.channel();
                    int bank = completion.bank();
                    bankOutstanding[channel][bank]--;
                }
                completedRequests++;
                classCompletions[latencyClass]++;

                if (waitingForThreadSlot[thread]
                        && outstandingByThread[thread] < maxOutstanding) {
                    waitingForThreadSlot[thread] = false;
                    if (!waitingForQueueSlot[thread]) {
                        if (ready.isRemote(thread)) {
                            remoteWakeups++;
                        }
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
                if (ready.isRemote(awakened[i])) {
                    remoteWakeups++;
                }
                ready.addFirst(awakened[i]);
            }

            if (cycle >= warmup) {
                ready.accumulateRunnableCycles(
                        workspaceRunnableThreadCycles, workspaceRunnableWorkspaceCycles);
            }

            int candidates = ready.size();
            int issuedThisCycle = 0;
            int[] requeue = new int[candidates];
            int requeueCount = 0;
            java.util.Arrays.fill(clusterIssuedThisCycle, 0);
            int unavailableClusters = 0;

            // Visit each ready thread at most once, respecting both the global
            // issue budget and each cluster's independent local budget.
            while (candidates > 0 && issuedThisCycle < issueWidth
                    && unavailableClusters < ready.clusterCount()) {
                int cluster = nextCluster;
                nextCluster = (nextCluster + 1) % ready.clusterCount();
                if (clusterIssuedThisCycle[cluster] >= clusterIssueWidth) {
                    unavailableClusters++;
                    continue;
                }
                int thread = ready.poll(cluster);
                if (thread < 0) {
                    unavailableClusters++;
                    continue;
                }
                candidates--;
                unavailableClusters = 0;
                if (ready.lastPollMigrated() && migrationLatency > 0) {
                    migrationWakeups.add(new MigrationWakeup(
                            (long) cycle + migrationLatency, thread));
                    continue;
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
                    boolean remote = ready.isRemote(thread);
                    int localityPenalty = remote ? remoteMemoryLatencyPenalty : 0;
                    int requestBytes = memoryRequestMinBytes
                            == memoryRequestMaxBytes ? memoryRequestMinBytes
                            : (int) random.nextLong(
                                    memoryRequestMinBytes,
                                    (long) memoryRequestMaxBytes + 1);
                    minObservedRequestBytes = Math.min(
                            minObservedRequestBytes, requestBytes);
                    maxObservedRequestBytes = Math.max(
                            maxObservedRequestBytes, requestBytes);
                    long nominalReadyCycle = (long) cycle
                            + memoryClass.latency() + localityPenalty;
                    int channel = 0;
                    int bank = 0;
                    long completionCycle = nominalReadyCycle;
                    if (memorySystemEnabled) {
                        long syntheticAddress = random.nextLong();
                        channel = (int) Long.remainderUnsigned(
                                syntheticAddress, memoryChannels);
                        bank = (int) Long.remainderUnsigned(
                                Long.divideUnsigned(syntheticAddress, memoryChannels),
                                memoryBanksPerChannel);
                        long bankReadyCycle = bankAvailableCycle[channel][bank];
                        long bankWait = Math.max(0L,
                                bankReadyCycle - nominalReadyCycle);
                        if (bankWait > 0) {
                            bankConflictRequests++;
                            bankConflictWaitCycles += bankWait;
                        }
                        long transferCycles = memoryBandwidthBytesPerCycle > 0
                                ? Math.max(1L,
                                        ((long) requestBytes
                                                * memoryBanksPerChannel
                                                + memoryBandwidthBytesPerCycle - 1)
                                                / memoryBandwidthBytesPerCycle)
                                : 1L;
                        long serviceStart = Math.max(
                                nominalReadyCycle, bankReadyCycle);
                        completionCycle = serviceStart + transferCycles;
                        bankAvailableCycle[channel][bank] =
                                completionCycle + bankConflictPenalty;
                        memoryServiceCycles += transferCycles;
                        bankOutstanding[channel][bank]++;
                        maxBankOutstanding = Math.max(maxBankOutstanding,
                                bankOutstanding[channel][bank]);
                    }
                    completions.add(new Completion(
                            completionCycle, thread, selectedClass,
                            channel, bank, requestBytes));
                    memoryBytesIssued += requestBytes;
                    if (remote) {
                        remoteMemoryIssues++;
                        remoteMemoryLatencyCycles += localityPenalty;
                    }
                    pendingClass[thread] = -1;
                    memoryIssues++;
                    classIssues[selectedClass]++;
                    workspaceIssues[thread / 16]++;
                    if (cycle >= warmup) {
                        measuredIssued++;
                        workspaceMeasuredIssues[thread / 16]++;
                    }
                    issuedThisCycle++;
                    clusterIssuedThisCycle[cluster]++;
                    clusterIssues[cluster]++;

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
                    clusterIssuedThisCycle[cluster]++;
                    clusterIssues[cluster]++;
                    workspaceIssues[thread / 16]++;
                    if (cycle >= warmup) {
                        measuredIssued++;
                        workspaceMeasuredIssues[thread / 16]++;
                    }
                    if (random.nextDouble() < threadDependencyFraction) {
                        int chain = selectReadyDependencyChain(
                                dependencyReadyCycles[thread],
                                dependencyChainCursor[thread], cycle);
                        if (chain < 0) {
                            throw new IllegalStateException(
                                    "Scheduled thread has no ready dependency chain");
                        }
                        dependencyIssues++;
                        dependencyReadyCycles[thread][chain] =
                                (long) cycle + dependencyLatency;
                        dependencyChainCursor[thread] = (chain + 1) % dependencyChains;

                        if (hasOtherReadyDependencyChain(
                                dependencyReadyCycles[thread], chain,
                                (long) cycle + 1)) {
                            dependencyIssuesOverlapped++;
                            requeue[requeueCount++] = thread;
                        } else {
                            long nextReadyCycle = earliestDependencyReadyCycle(
                                    dependencyReadyCycles[thread]);
                            dependencyWakeups.add(new DependencyWakeup(
                                    nextReadyCycle, thread));
                        }
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
        double clusterBudgetUtilization = ipc / effectiveIssueCapacity;
        if (minObservedRequestBytes == Integer.MAX_VALUE) {
            minObservedRequestBytes = 0;
        }
        long totalRemoteThreadCycles = 0;
        for (long count : workspaceRemoteThreadCycles) {
            totalRemoteThreadCycles += count;
        }
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
        double workspaceIssueJainFairness = totalWorkspaceIssues == 0 ? 1.0
                : (double) totalWorkspaceIssues * totalWorkspaceIssues
                        / (workspaceIssues.length * sumWorkspaceSquares);
        long totalRunnableThreadCycles = 0;
        double threadOpportunityRateSum = 0.0;
        double threadOpportunityRateSquares = 0.0;
        double minThreadOpportunityRate = Double.POSITIVE_INFINITY;
        double maxThreadOpportunityRate = 0.0;
        double workspaceOpportunityRateSum = 0.0;
        double workspaceOpportunityRateSquares = 0.0;
        double minWorkspaceOpportunityRate = Double.POSITIVE_INFINITY;
        double maxWorkspaceOpportunityRate = 0.0;
        int workspacesWithRunnableOpportunity = 0;
        int workspacesWithRunnableWorkspaceCycles = 0;
        long totalRunnableWorkspaceCycles = 0;
        for (int workspace = 0; workspace < workspaceCount; workspace++) {
            long runnableThreadCycles = workspaceRunnableThreadCycles[workspace];
            long runnableWorkspaceCycles = workspaceRunnableWorkspaceCycles[workspace];
            totalRunnableThreadCycles += runnableThreadCycles;
            totalRunnableWorkspaceCycles += runnableWorkspaceCycles;
            if (runnableThreadCycles > 0) {
                double rate = (double) workspaceMeasuredIssues[workspace]
                        / runnableThreadCycles;
                threadOpportunityRateSum += rate;
                threadOpportunityRateSquares += rate * rate;
                minThreadOpportunityRate = Math.min(minThreadOpportunityRate, rate);
                maxThreadOpportunityRate = Math.max(maxThreadOpportunityRate, rate);
                workspacesWithRunnableOpportunity++;
            }
            if (runnableWorkspaceCycles > 0) {
                double rate = (double) workspaceMeasuredIssues[workspace]
                        / runnableWorkspaceCycles;
                workspaceOpportunityRateSum += rate;
                workspaceOpportunityRateSquares += rate * rate;
                minWorkspaceOpportunityRate = Math.min(
                        minWorkspaceOpportunityRate, rate);
                maxWorkspaceOpportunityRate = Math.max(
                        maxWorkspaceOpportunityRate, rate);
                workspacesWithRunnableWorkspaceCycles++;
            }
        }
        double workspaceIssuePerRunnableThreadCycleJainFairness =
                workspacesWithRunnableOpportunity == 0 ? 1.0
                : threadOpportunityRateSum * threadOpportunityRateSum
                        / (workspacesWithRunnableOpportunity
                                * threadOpportunityRateSquares);
        double workspaceIssuePerRunnableWorkspaceCycleJainFairness =
                workspacesWithRunnableWorkspaceCycles == 0 ? 1.0
                : workspaceOpportunityRateSum * workspaceOpportunityRateSum
                        / (workspacesWithRunnableWorkspaceCycles
                                * workspaceOpportunityRateSquares);
        if (workspacesWithRunnableOpportunity == 0) {
            minThreadOpportunityRate = 0.0;
        }
        if (workspacesWithRunnableWorkspaceCycles == 0) {
            minWorkspaceOpportunityRate = 0.0;
        }

        System.out.printf(
                "scheduler=%s clusters=%d schedulerSteals=%d schedulerSelections=%d "
                        + "contexts=%d workspaces=%d hotWorkspaceCount=%d "
                        + "hotWorkspaceRatio=%.3f hotMemoryFraction=%.3f "
                        + "coldMemoryFraction=%.3f hotDependencyFraction=%.3f "
                        + "coldDependencyFraction=%.3f issueWidth=%d memoryFraction=%.3f "
                        + "dependencyFraction=%.3f dependencyLatency=%d "
                        + "dependencyChains=%d "
                        + "clusterIssueWidth=%d effectiveIssueCapacity=%d "
                        + "clusterBudgetUtilization=%.4f "
                        + "migrationLatency=%d remoteMemoryLatencyPenalty=%d "
                        + "schedulerMigrations=%d remoteWakeups=%d "
                        + "remoteThreadCycles=%d remoteMemoryIssues=%d "
                        + "remoteMemoryLatencyCycles=%d migrationWaiterCycles=%d "
                        + "migrationCompletions=%d "
                        + "memorySystemEnabled=%s memoryRequestBytes=%d "
                        + "memoryRequestBytesMin=%d memoryRequestBytesMax=%d "
                        + "memoryRequestBytesObservedMin=%d "
                        + "memoryRequestBytesObservedMax=%d "
                        + "memoryChannels=%d memoryBanksPerChannel=%d "
                        + "memoryBandwidthBytesPerCycle=%d "
                        + "aggregateMemoryBandwidthBytesPerCycle=%d "
                        + "bankConflictPenalty=%d memoryBytesIssued=%d "
                        + "memoryBytesCompleted=%d memoryServiceCycles=%d "
                        + "bankConflictRequests=%d bankConflictWaitCycles=%d "
                        + "maxBankOutstanding=%d "
                        + "maxOutstanding=%d memoryQueueCapacity=%d "
                        + "memoryCompletionRate=%d cycles=%d warmup=%d measuredIPC=%.4f "
                        + "issueUtilization=%.4f memoryIssues=%d "
                        + "completedRequests=%d maxThreadOutstanding=%d "
                        + "maxQueueOccupancy=%d queueFullAttempts=%d queueWaiterCycles=%d "
                        + "dependencyWaiterCycles=%d serviceBacklogCycles=%d "
                        + "idleCycles=%d readyAtEnd=%d "
                        + "waitingAtThreadLimitAtEnd=%d "
                        + "waitingAtQueueLimitAtEnd=%d outstandingAtEnd=%d "
                        + "dependencyIssues=%d dependencyIssuesOverlapped=%d "
                        + "dependencyStallWakeups=%d threadsWaitingOnDependenciesAtEnd=%d "
                        + "workspaceIssuesMin=%d workspaceIssuesMax=%d "
                        + "workspaceIssueJainFairness=%.6f "
                        + "workspaceRunnableThreadCycles=%d "
                        + "workspaceRunnableWorkspaceCycles=%d "
                        + "workspacesWithRunnableOpportunity=%d "
                        + "workspaceIssuePerRunnableThreadCycleMin=%.8f "
                        + "workspaceIssuePerRunnableThreadCycleMax=%.8f "
                        + "workspaceIssuePerRunnableThreadCycleJainFairness=%.6f "
                        + "workspacesWithRunnableWorkspaceCycles=%d "
                        + "workspaceIssuePerRunnableWorkspaceCycleMin=%.8f "
                        + "workspaceIssuePerRunnableWorkspaceCycleMax=%.8f "
                        + "workspaceIssuePerRunnableWorkspaceCycleJainFairness=%.6f seed=%d%n",
                ready.name(), ready.clusterCount(), ready.steals(),
                ready.selectionProbes(), contexts, workspaceCount,
                hotWorkspaceCount, hotWorkspaceRatio, hotMemoryFraction,
                coldMemoryFraction, hotDependencyFraction, coldDependencyFraction,
                issueWidth, memoryFraction, dependencyFraction,
                dependencyLatency, dependencyChains, clusterIssueWidth,
                effectiveIssueCapacity,
                clusterBudgetUtilization, migrationLatency,
                remoteMemoryLatencyPenalty, ready.migrations(), remoteWakeups,
                totalRemoteThreadCycles, remoteMemoryIssues,
                remoteMemoryLatencyCycles, migrationWaiterCycles,
                migrationCompletions, memorySystemEnabled,
                memoryRequestBytes, memoryRequestMinBytes,
                memoryRequestMaxBytes, minObservedRequestBytes,
                maxObservedRequestBytes, memoryChannels, memoryBanksPerChannel,
                memoryBandwidthBytesPerCycle,
                aggregateMemoryBandwidthBytesPerCycle, bankConflictPenalty,
                memoryBytesIssued, memoryBytesCompleted, memoryServiceCycles,
                bankConflictRequests, bankConflictWaitCycles, maxBankOutstanding,
                maxOutstanding, memoryQueueCapacity, memoryCompletionRate, cycles, warmup, ipc, utilization,
                memoryIssues, completedRequests, maxThreadOutstandingObserved,
                maxQueueOccupancy, queueFullAttempts, queueWaiterCycles,
                dependencyWaiterCycles, serviceBacklogCycles, idleCycles, ready.size(),
                waitingAtThreadLimit, waitingAtQueueLimit, outstandingAtEnd,
                dependencyIssues, dependencyIssuesOverlapped,
                dependencyStallWakeups, dependencyWakeups.size(),
                minWorkspaceIssues, maxWorkspaceIssues, workspaceIssueJainFairness,
                totalRunnableThreadCycles, totalRunnableWorkspaceCycles,
                workspacesWithRunnableOpportunity,
                minThreadOpportunityRate, maxThreadOpportunityRate,
                workspaceIssuePerRunnableThreadCycleJainFairness,
                workspacesWithRunnableWorkspaceCycles,
                minWorkspaceOpportunityRate, maxWorkspaceOpportunityRate,
                workspaceIssuePerRunnableWorkspaceCycleJainFairness, seed);

        for (int i = 0; i < memoryClasses.length; i++) {
            System.out.printf("memoryClass=%s latency=%d issues=%d completions=%d%n",
                    memoryClasses[i].name(), memoryClasses[i].latency(),
                    classIssues[i], classCompletions[i]);
        }
        for (int cluster = 0; cluster < clusterIssues.length; cluster++) {
            System.out.printf("clusterIssueCount=%d issues=%d%n",
                    cluster, clusterIssues[cluster]);
        }
    }

    private interface ReadyScheduler {
        void addFirst(int thread);
        void addLast(int thread);
        int poll(int cluster);
        int size();
        String name();
        int clusterCount();
        long steals();
        long selectionProbes();
        long migrations();
        boolean isRemote(int thread);
        boolean lastPollMigrated();
        void accumulateRemoteThreadCycles(long[] totals);
        void accumulateRunnableCycles(
                long[] runnableThreadTotals, long[] runnableWorkspaceTotals);
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
        private final int[] workspaceReadyCounts;
        private long selectionProbes;

        private FifoReadyScheduler(int capacity) {
            queue = new ArrayDeque<>(capacity);
            workspaceReadyCounts = new int[(capacity + 15) / 16];
        }

        @Override
        public void addFirst(int thread) {
            queue.addFirst(thread);
            workspaceReadyCounts[thread / 16]++;
        }

        @Override
        public void addLast(int thread) {
            queue.addLast(thread);
            workspaceReadyCounts[thread / 16]++;
        }

        @Override
        public int poll(int cluster) {
            if (cluster != 0) {
                return -1;
            }
            selectionProbes++;
            if (queue.isEmpty()) {
                return -1;
            }
            int thread = queue.removeFirst();
            workspaceReadyCounts[thread / 16]--;
            return thread;
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

        @Override
        public long migrations() {
            return 0;
        }

        @Override
        public boolean isRemote(int thread) {
            return false;
        }

        @Override
        public boolean lastPollMigrated() {
            return false;
        }

        @Override
        public void accumulateRemoteThreadCycles(long[] totals) {
            // The flat FIFO policy has no remote cluster state.
        }

        @Override
        public void accumulateRunnableCycles(
                long[] runnableThreadTotals, long[] runnableWorkspaceTotals) {
            for (int workspace = 0; workspace < workspaceReadyCounts.length; workspace++) {
                int readyCount = workspaceReadyCounts[workspace];
                runnableThreadTotals[workspace] += readyCount;
                if (readyCount > 0) {
                    runnableWorkspaceTotals[workspace]++;
                }
            }
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
        public int poll(int cluster) {
            if (cluster != 0 || size == 0) {
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

        @Override
        public long migrations() {
            return 0;
        }

        @Override
        public boolean isRemote(int thread) {
            return false;
        }

        @Override
        public boolean lastPollMigrated() {
            return false;
        }

        @Override
        public void accumulateRemoteThreadCycles(long[] totals) {
            // The hierarchical policy uses one logical cluster.
        }

        @Override
        public void accumulateRunnableCycles(
                long[] runnableThreadTotals, long[] runnableWorkspaceTotals) {
            for (int workspace = 0; workspace < workspaceQueues.size(); workspace++) {
                int readyCount = workspaceQueues.get(workspace).size();
                runnableThreadTotals[workspace] += readyCount;
                if (readyCount > 0) {
                    runnableWorkspaceTotals[workspace]++;
                }
            }
        }
    }

    private static final class DistributedReadyScheduler
            implements ReadyScheduler {
        private final List<ArrayDeque<Integer>> clusterQueues;
        private final int[] homeCluster;
        private final int[] ownerCluster;
        private final int[] workspaceReadyCounts;
        private final int[] remoteThreadsByWorkspace;
        private int size;
        private long steals;
        private long migrations;
        private boolean lastPollMigrated;
        private long selectionProbes;

        private DistributedReadyScheduler(int contexts, int clusters) {
            if (clusters < 1) {
                throw new IllegalArgumentException("Cluster count must be positive");
            }
            clusterQueues = new ArrayList<>(clusters);
            for (int i = 0; i < clusters; i++) {
                clusterQueues.add(new ArrayDeque<>());
            }
            homeCluster = new int[contexts];
            ownerCluster = new int[contexts];
            workspaceReadyCounts = new int[(contexts + 15) / 16];
            remoteThreadsByWorkspace = new int[workspaceReadyCounts.length];
            for (int thread = 0; thread < contexts; thread++) {
                homeCluster[thread] = (thread / 16) % clusters;
                ownerCluster[thread] = homeCluster[thread];
            }
        }

        @Override
        public void addFirst(int thread) {
            clusterQueues.get(ownerCluster[thread]).addFirst(thread);
            workspaceReadyCounts[thread / 16]++;
            size++;
        }

        @Override
        public void addLast(int thread) {
            clusterQueues.get(ownerCluster[thread]).addLast(thread);
            workspaceReadyCounts[thread / 16]++;
            size++;
        }

        @Override
        public int poll(int targetCluster) {
            lastPollMigrated = false;
            if (size == 0) {
                return -1;
            }
            selectionProbes++;
            int source = targetCluster;
            if (clusterQueues.get(source).isEmpty()) {
                source = -1;
                for (int offset = 1; offset < clusterQueues.size(); offset++) {
                    int candidate = (targetCluster + offset) % clusterQueues.size();
                    if (!clusterQueues.get(candidate).isEmpty()) {
                        source = candidate;
                        break;
                    }
                }
            }
            if (source < 0) {
                return -1;
            }

            int thread = clusterQueues.get(source).removeFirst();
            workspaceReadyCounts[thread / 16]--;
            if (source != targetCluster) {
                steals++;
                migrations++;
                int workspace = thread / 16;
                if (ownerCluster[thread] == homeCluster[thread]) {
                    remoteThreadsByWorkspace[workspace]++;
                } else if (targetCluster == homeCluster[thread]) {
                    remoteThreadsByWorkspace[workspace]--;
                }
                ownerCluster[thread] = targetCluster;
                lastPollMigrated = true;
            }
            size--;
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

        @Override
        public long migrations() {
            return migrations;
        }

        @Override
        public boolean isRemote(int thread) {
            return ownerCluster[thread] != homeCluster[thread];
        }

        @Override
        public boolean lastPollMigrated() {
            return lastPollMigrated;
        }

        @Override
        public void accumulateRemoteThreadCycles(long[] totals) {
            for (int workspace = 0; workspace < remoteThreadsByWorkspace.length; workspace++) {
                totals[workspace] += remoteThreadsByWorkspace[workspace];
            }
        }

        @Override
        public void accumulateRunnableCycles(
                long[] runnableThreadTotals, long[] runnableWorkspaceTotals) {
            for (int workspace = 0; workspace < workspaceReadyCounts.length; workspace++) {
                int readyCount = workspaceReadyCounts[workspace];
                runnableThreadTotals[workspace] += readyCount;
                if (readyCount > 0) {
                    runnableWorkspaceTotals[workspace]++;
                }
            }
        }
    }

    private static int selectReadyDependencyChain(
            long[] readyCycles, int start, long cycle) {
        for (int offset = 0; offset < readyCycles.length; offset++) {
            int chain = (start + offset) % readyCycles.length;
            if (readyCycles[chain] <= cycle) {
                return chain;
            }
        }
        return -1;
    }

    private static boolean hasOtherReadyDependencyChain(
            long[] readyCycles, int excludedChain, long cycle) {
        for (int chain = 0; chain < readyCycles.length; chain++) {
            if (chain != excludedChain && readyCycles[chain] <= cycle) {
                return true;
            }
        }
        return false;
    }

    private static long earliestDependencyReadyCycle(long[] readyCycles) {
        long earliest = Long.MAX_VALUE;
        for (long readyCycle : readyCycles) {
            earliest = Math.min(earliest, readyCycle);
        }
        return earliest;
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
