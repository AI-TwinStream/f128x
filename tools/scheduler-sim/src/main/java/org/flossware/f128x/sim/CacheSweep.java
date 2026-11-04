package org.flossware.f128x.sim;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;

/**
 * Synthetic two-level cache experiment with private per-thread L1 caches and
 * one shared L2 cache. This is a locality sensitivity model, not a coherence,
 * replacement-policy, or physical cache implementation.
 */
public final class CacheSweep {
    enum Result {
        L1_HIT,
        L2_HIT,
        DRAM_MISS
    }

    static final class SetAssociativeCache {
        private final int lineBytes;
        private final int setCount;
        private final int ways;
        private final List<ArrayDeque<Long>> sets;

        SetAssociativeCache(int capacityBytes, int lineBytes, int ways) {
            if (capacityBytes < 1 || lineBytes < 1 || ways < 1
                    || capacityBytes % lineBytes != 0
                    || (capacityBytes / lineBytes) % ways != 0) {
                throw new IllegalArgumentException(
                        "Cache capacity must contain a positive integral number of sets");
            }
            this.lineBytes = lineBytes;
            this.ways = ways;
            setCount = capacityBytes / lineBytes / ways;
            sets = new ArrayList<>(setCount);
            for (int i = 0; i < setCount; i++) {
                sets.add(new ArrayDeque<>(ways));
            }
        }

        boolean access(long address) {
            if (contains(address)) {
                return true;
            }
            insert(address);
            return false;
        }

        boolean contains(long address) {
            long line = Long.divideUnsigned(address, lineBytes);
            int setIndex = (int) Long.remainderUnsigned(line, setCount);
            long tag = Long.divideUnsigned(line, setCount);
            ArrayDeque<Long> set = sets.get(setIndex);
            if (!set.remove(tag)) {
                return false;
            }
            set.addLast(tag);
            return true;
        }

        void insert(long address) {
            long line = Long.divideUnsigned(address, lineBytes);
            int setIndex = (int) Long.remainderUnsigned(line, setCount);
            long tag = Long.divideUnsigned(line, setCount);
            ArrayDeque<Long> set = sets.get(setIndex);
            if (set.remove(tag)) {
                set.addLast(tag);
                return;
            }
            if (set.size() == ways) {
                set.removeFirst();
            }
            set.addLast(tag);
        }
    }

    static final class Hierarchy {
        private final SetAssociativeCache[] l1ByThread;
        private final SetAssociativeCache l2;

        Hierarchy(int contexts, int l1Bytes, int l1Ways,
                int l2Bytes, int l2Ways, int lineBytes) {
            if (contexts < 1) {
                throw new IllegalArgumentException("Contexts must be positive");
            }
            l1ByThread = new SetAssociativeCache[contexts];
            for (int thread = 0; thread < contexts; thread++) {
                l1ByThread[thread] = new SetAssociativeCache(
                        l1Bytes, lineBytes, l1Ways);
            }
            l2 = new SetAssociativeCache(l2Bytes, lineBytes, l2Ways);
        }

        Result access(int thread, long address) {
            Result result = probe(thread, address);
            fill(thread, address, result);
            return result;
        }

        Result probe(int thread, long address) {
            checkThread(thread);
            if (l1ByThread[thread].contains(address)) {
                return Result.L1_HIT;
            }
            if (l2.contains(address)) {
                return Result.L2_HIT;
            }
            return Result.DRAM_MISS;
        }

        void fill(int thread, long address, Result result) {
            checkThread(thread);
            if (result == Result.L2_HIT) {
                l1ByThread[thread].insert(address);
            } else if (result == Result.DRAM_MISS) {
                l2.insert(address);
                l1ByThread[thread].insert(address);
            }
        }

        private void checkThread(int thread) {
            if (thread < 0 || thread >= l1ByThread.length) {
                throw new IllegalArgumentException("Thread index out of range");
            }
        }
    }

    private CacheSweep() {}

    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println(
                    "Usage: CacheSweep <contexts> <accesses> <working-set-bytes> <seed>");
            System.exit(2);
        }
        int contexts = Integer.parseInt(args[0]);
        long accesses = Long.parseLong(args[1]);
        long workingSetBytes = Long.parseLong(args[2]);
        long seed = Long.parseLong(args[3]);

        int lineBytes = Integer.getInteger("f128x.cache.lineBytes", 64);
        int l1Bytes = Integer.getInteger("f128x.cache.l1Bytes", 32 * 1024);
        int l1Ways = Integer.getInteger("f128x.cache.l1Ways", 8);
        int l2Bytes = Integer.getInteger("f128x.cache.l2Bytes", 1024 * 1024);
        int l2Ways = Integer.getInteger("f128x.cache.l2Ways", 16);
        int l1Latency = Integer.getInteger("f128x.cache.l1Latency", 4);
        int l2Latency = Integer.getInteger("f128x.cache.l2Latency", 20);
        int dramLatency = Integer.getInteger("f128x.cache.dramLatency", 200);
        int warmupPercent = Integer.getInteger("f128x.cache.warmupPercent", 10);
        long stride = Long.getLong("f128x.addressStride", 4096L);
        MemoryAddressing.Pattern pattern = MemoryAddressing.Pattern.parse(
                System.getProperty("f128x.addressPattern", "sequential"));

        if (contexts < 1 || accesses < 1 || workingSetBytes < lineBytes
                || lineBytes < 1 || l1Bytes < lineBytes || l1Ways < 1
                || l2Bytes < lineBytes || l2Ways < 1
                || l1Latency < 1 || l2Latency <= l1Latency
                || dramLatency <= l2Latency || warmupPercent < 0
                || warmupPercent >= 100 || stride < 1
                || workingSetBytes % lineBytes != 0) {
            throw new IllegalArgumentException("Invalid cache simulation parameter");
        }

        Hierarchy hierarchy = new Hierarchy(
                contexts, l1Bytes, l1Ways, l2Bytes, l2Ways, lineBytes);
        long[] sequences = new long[contexts];
        SplittableRandom random = new SplittableRandom(seed);
        long warmup = accesses * warmupPercent / 100;
        long l1Hits = 0;
        long l2Hits = 0;
        long dramMisses = 0;
        long measuredL1Hits = 0;
        long measuredL2Hits = 0;
        long measuredDramMisses = 0;
        long measuredLatency = 0;

        for (long index = 0; index < accesses; index++) {
            int thread = (int) (index % contexts);
            long entropy = Long.remainderUnsigned(
                    random.nextLong(), workingSetBytes);
            long address = MemoryAddressing.address(
                    pattern, thread, sequences[thread]++, entropy,
                    stride, workingSetBytes);
            Result result = hierarchy.access(thread, address);
            switch (result) {
                case L1_HIT -> {
                    l1Hits++;
                    if (index >= warmup) {
                        measuredL1Hits++;
                        measuredLatency += l1Latency;
                    }
                }
                case L2_HIT -> {
                    l2Hits++;
                    if (index >= warmup) {
                        measuredL2Hits++;
                        measuredLatency += l2Latency;
                    }
                }
                case DRAM_MISS -> {
                    dramMisses++;
                    if (index >= warmup) {
                        measuredDramMisses++;
                        measuredLatency += dramLatency;
                    }
                }
            }
        }

        long measuredAccesses = accesses - warmup;
        double averageLatency = measuredAccesses == 0 ? 0.0
                : (double) measuredLatency / measuredAccesses;
        System.out.printf(Locale.ROOT,
                "cacheModel=private-l1-shared-l2 contexts=%d accesses=%d "
                        + "workingSetBytes=%d addressPattern=%s stride=%d "
                        + "lineBytes=%d l1BytesPerThread=%d l1Ways=%d "
                        + "l2Bytes=%d l2Ways=%d l1Latency=%d l2Latency=%d "
                        + "dramLatency=%d warmupAccesses=%d "
                        + "l1Hits=%d l2Hits=%d dramMisses=%d "
                        + "measuredL1Hits=%d measuredL2Hits=%d "
                        + "measuredDramMisses=%d averageMeasuredLatency=%.4f "
                        + "seed=%d%n",
                contexts, accesses, workingSetBytes,
                pattern.name().toLowerCase(Locale.ROOT), stride, lineBytes,
                l1Bytes, l1Ways, l2Bytes, l2Ways, l1Latency, l2Latency,
                dramLatency, warmup, l1Hits, l2Hits, dramMisses,
                measuredL1Hits, measuredL2Hits, measuredDramMisses,
                averageLatency, seed);
    }
}
