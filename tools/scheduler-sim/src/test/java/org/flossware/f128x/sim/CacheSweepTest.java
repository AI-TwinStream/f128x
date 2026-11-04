package org.flossware.f128x.sim;

/** Dependency-free deterministic tests for the synthetic cache hierarchy. */
public final class CacheSweepTest {
    private CacheSweepTest() {}

    public static void main(String[] args) {
        CacheSweep.Hierarchy hierarchy = new CacheSweep.Hierarchy(
                2, 256, 2, 1024, 4, 64);
        check(hierarchy.access(0, 0) == CacheSweep.Result.DRAM_MISS,
                "first access must miss both levels");
        check(hierarchy.access(0, 0) == CacheSweep.Result.L1_HIT,
                "repeated same-thread access must hit private L1");
        check(hierarchy.access(1, 0) == CacheSweep.Result.L2_HIT,
                "another thread must see the shared L2 copy");
        check(hierarchy.access(1, 0) == CacheSweep.Result.L1_HIT,
                "second-thread reuse must populate its private L1");

        CacheSweep.SetAssociativeCache directMapped =
                new CacheSweep.SetAssociativeCache(64, 64, 1);
        check(!directMapped.access(0), "first line must miss");
        check(directMapped.access(0), "same line must hit");
        check(!directMapped.access(64), "conflicting line must evict");
        check(!directMapped.access(0), "evicted line must miss");

        System.out.println("CacheSweepTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
