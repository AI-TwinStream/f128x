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
 * memoryFraction. Such a thread is blocked for latencyCycles, then becomes
 * ready again. Other instructions make the thread ready in the next cycle.
 *
 * This intentionally simple model isolates context count, issue width,
 * blocking latency, and memory-operation fraction. It is not an ISA model,
 * cache model, out-of-order core, or realistic memory hierarchy.
 */
public final class ContextSweep {
    private record Wakeup(long cycle, int thread) {}

    private ContextSweep() {}

    public static void main(String[] args) {
        if (args.length != 6) {
            System.err.println("""
                    Usage: ContextSweep <contexts> <issue-width> <latency-cycles>
                                         <memory-fraction> <cycles> <seed>
                    Example: ContextSweep 4096 4 300 0.25 200000 42
                    """);
            System.exit(2);
        }

        int contexts = Integer.parseInt(args[0]);
        int issueWidth = Integer.parseInt(args[1]);
        int latency = Integer.parseInt(args[2]);
        double memoryFraction = Double.parseDouble(args[3]);
        int cycles = Integer.parseInt(args[4]);
        long seed = Long.parseLong(args[5]);

        if (contexts < 1 || issueWidth < 1 || latency < 1 || cycles < 1
                || memoryFraction < 0.0 || memoryFraction > 1.0) {
            throw new IllegalArgumentException("Invalid simulation parameter");
        }

        var ready = new ArrayDeque<Integer>(contexts);
        var wakeups = new PriorityQueue<>(
                Comparator.comparingLong(Wakeup::cycle));
        for (int thread = 0; thread < contexts; thread++) {
            ready.addLast(thread);
        }

        var random = new SplittableRandom(seed);
        long issued = 0;
        long blockedIssues = 0;
        long idleCycles = 0;
        long measuredIssued = 0;
        final int warmup = Math.min(10_000, cycles / 10);

        for (int cycle = 0; cycle < cycles; cycle++) {
            while (!wakeups.isEmpty() && wakeups.peek().cycle() <= cycle) {
                ready.addLast(wakeups.remove().thread());
            }

            int slots = Math.min(issueWidth, ready.size());
            if (slots == 0) {
                idleCycles++;
                continue;
            }

            int[] nonBlocking = new int[slots];
            int nonBlockingCount = 0;
            for (int slot = 0; slot < slots; slot++) {
                int thread = ready.removeFirst();
                issued++;
                if (cycle >= warmup) {
                    measuredIssued++;
                }

                if (random.nextDouble() < memoryFraction) {
                    blockedIssues++;
                    wakeups.add(new Wakeup((long) cycle + latency, thread));
                } else {
                    nonBlocking[nonBlockingCount++] = thread;
                }
            }

            // Requeue only after selecting this cycle's issue bundle.
            for (int i = 0; i < nonBlockingCount; i++) {
                ready.addLast(nonBlocking[i]);
            }
        }

        int measuredCycles = Math.max(1, cycles - warmup);
        double ipc = (double) measuredIssued / measuredCycles;
        double utilization = ipc / issueWidth;
        System.out.printf(
                "contexts=%d issueWidth=%d latency=%d memoryFraction=%.3f "
                        + "cycles=%d warmup=%d measuredIPC=%.4f "
                        + "issueUtilization=%.4f blockedIssues=%d "
                        + "idleCycles=%d readyAtEnd=%d blockedAtEnd=%d seed=%d%n",
                contexts, issueWidth, latency, memoryFraction, cycles, warmup,
                ipc, utilization, blockedIssues, idleCycles, ready.size(),
                wakeups.size(), seed);
    }
}
