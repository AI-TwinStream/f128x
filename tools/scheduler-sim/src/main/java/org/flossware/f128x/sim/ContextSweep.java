package org.flossware.f128x.sim;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.SplittableRandom;

/**
 * Small cycle-level latency-hiding experiment.
 *
 * Each thread can issue at most one instruction per cycle. Each issued
 * instruction independently becomes a long-latency operation with probability
 * memoryFraction. A thread may continue issuing while it has fewer than
 * maxOutstanding memory operations in flight. It becomes ineligible when that
 * limit is reached and becomes ready again when a completion frees a slot.
 *
 * This intentionally simple model isolates context count, issue width, memory
 * latency, memory-operation fraction, and per-thread memory-level parallelism.
 * It is not an ISA model, cache model, out-of-order core, or realistic memory
 * hierarchy.
 */
public final class ContextSweep {
    private record Wakeup(long cycle, int thread) {}

    private ContextSweep() {}

    public static void main(String[] args) {
        if (args.length != 6 && args.length != 7) {
            System.err.println("""
                    Usage: ContextSweep <contexts> <issue-width> <latency-cycles>
                                         <memory-fraction> <cycles> <seed>
                                         [max-outstanding]
                    Example: ContextSweep 4096 4 300 0.25 200000 42 2
                    """);
            System.exit(2);
        }

        int contexts = Integer.parseInt(args[0]);
        int issueWidth = Integer.parseInt(args[1]);
        int latency = Integer.parseInt(args[2]);
        double memoryFraction = Double.parseDouble(args[3]);
        int cycles = Integer.parseInt(args[4]);
        long seed = Long.parseLong(args[5]);
        int maxOutstanding = args.length == 7 ? Integer.parseInt(args[6]) : 1;

        if (contexts < 1 || issueWidth < 1 || latency < 1 || cycles < 1
                || maxOutstanding < 1 || memoryFraction < 0.0
                || memoryFraction > 1.0) {
            throw new IllegalArgumentException("Invalid simulation parameter");
        }

        var ready = new ArrayDeque<Integer>(contexts);
        var wakeups = new PriorityQueue<>(
                Comparator.comparingLong(Wakeup::cycle));
        int[] outstanding = new int[contexts];
        boolean[] waitingForSlot = new boolean[contexts];
        for (int thread = 0; thread < contexts; thread++) {
            ready.addLast(thread);
        }

        var random = new SplittableRandom(seed);
        long issued = 0;
        long blockedIssues = 0;
        long completedRequests = 0;
        long idleCycles = 0;
        long measuredIssued = 0;
        int maxOutstandingObserved = 0;
        final int warmup = Math.min(10_000, cycles / 10);

        for (int cycle = 0; cycle < cycles; cycle++) {
            while (!wakeups.isEmpty() && wakeups.peek().cycle() <= cycle) {
                int thread = wakeups.remove().thread();
                outstanding[thread]--;
                completedRequests++;
                if (waitingForSlot[thread]
                        && outstanding[thread] < maxOutstanding) {
                    waitingForSlot[thread] = false;
                    ready.addLast(thread);
                }
            }

            int slots = Math.min(issueWidth, ready.size());
            if (slots == 0) {
                idleCycles++;
                continue;
            }

            int[] requeue = new int[slots];
            int requeueCount = 0;
            for (int slot = 0; slot < slots; slot++) {
                int thread = ready.removeFirst();
                issued++;
                if (cycle >= warmup) {
                    measuredIssued++;
                }

                if (random.nextDouble() < memoryFraction) {
                    blockedIssues++;
                    outstanding[thread]++;
                    maxOutstandingObserved = Math.max(
                            maxOutstandingObserved, outstanding[thread]);
                    wakeups.add(new Wakeup((long) cycle + latency, thread));
                    if (outstanding[thread] >= maxOutstanding) {
                        waitingForSlot[thread] = true;
                    } else {
                        requeue[requeueCount++] = thread;
                    }
                } else {
                    requeue[requeueCount++] = thread;
                }
            }

            // Requeue only after selecting this cycle's issue bundle.
            for (int i = 0; i < requeueCount; i++) {
                ready.addLast(requeue[i]);
            }
        }

        int measuredCycles = Math.max(1, cycles - warmup);
        double ipc = (double) measuredIssued / measuredCycles;
        double utilization = ipc / issueWidth;
        int waitingAtEnd = 0;
        long outstandingAtEnd = 0;
        for (int thread = 0; thread < contexts; thread++) {
            if (waitingForSlot[thread]) {
                waitingAtEnd++;
            }
            outstandingAtEnd += outstanding[thread];
        }

        System.out.printf(
                "contexts=%d issueWidth=%d latency=%d memoryFraction=%.3f "
                        + "maxOutstanding=%d cycles=%d warmup=%d "
                        + "measuredIPC=%.4f issueUtilization=%.4f "
                        + "memoryIssues=%d completedRequests=%d "
                        + "maxOutstandingObserved=%d idleCycles=%d "
                        + "readyAtEnd=%d waitingAtLimitAtEnd=%d "
                        + "outstandingAtEnd=%d seed=%d%n",
                contexts, issueWidth, latency, memoryFraction, maxOutstanding,
                cycles, warmup, ipc, utilization, blockedIssues,
                completedRequests, maxOutstandingObserved, idleCycles,
                ready.size(), waitingAtEnd, outstandingAtEnd, seed);
    }
}
