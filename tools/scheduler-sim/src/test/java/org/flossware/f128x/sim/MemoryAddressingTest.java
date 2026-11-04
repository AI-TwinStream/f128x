package org.flossware.f128x.sim;

/** Dependency-free deterministic tests for synthetic address generation/mapping. */
public final class MemoryAddressingTest {
    private MemoryAddressingTest() {}

    public static void main(String[] args) {
        addressPatternsAreDeterministicAndDistinct();
        mappingsExposeExpectedInterleaving();
        invalidConfigurationsAreRejected();
        System.out.println("MemoryAddressingTest: PASS");
    }

    private static void addressPatternsAreDeterministicAndDistinct() {
        long sequential = MemoryAddressing.address(
                MemoryAddressing.Pattern.SEQUENTIAL, 1, 2, 99, 256, 4096);
        long strided = MemoryAddressing.address(
                MemoryAddressing.Pattern.STRIDED, 1, 2, 99, 256, 4096);
        long sharedA = MemoryAddressing.address(
                MemoryAddressing.Pattern.SHARED_REGION, 0, 3, 99, 128, 4096);
        long sharedB = MemoryAddressing.address(
                MemoryAddressing.Pattern.SHARED_REGION, 7, 3, 11, 128, 4096);
        long random = MemoryAddressing.address(
                MemoryAddressing.Pattern.RANDOM, 7, 3, 0x1234, 128, 4096);

        require(sequential == 4096 + 2 * 64, "sequential address");
        require(strided == 4096 + 2 * 256, "strided address");
        require(sharedA == sharedB, "shared-region addresses overlap across threads");
        require(random == 0x1234, "random pattern preserves supplied entropy");
        require(sequential != strided, "sequential and strided patterns differ");
    }

    private static void mappingsExposeExpectedInterleaving() {
        MemoryAddressing.Location line0 = MemoryAddressing.map(
                0, 4, 4, MemoryAddressing.Mapping.LOW_ORDER);
        MemoryAddressing.Location line1 = MemoryAddressing.map(
                64, 4, 4, MemoryAddressing.Mapping.LOW_ORDER);
        MemoryAddressing.Location line4 = MemoryAddressing.map(
                256, 4, 4, MemoryAddressing.Mapping.LOW_ORDER);
        require(line0.equals(new MemoryAddressing.Location(0, 0)), "first low-order line");
        require(line1.equals(new MemoryAddressing.Location(1, 0)), "channel interleave");
        require(line4.equals(new MemoryAddressing.Location(0, 1)), "bank interleave");

        MemoryAddressing.Location lowOrder = MemoryAddressing.map(
                64L * 128, 4, 8, MemoryAddressing.Mapping.LOW_ORDER);
        MemoryAddressing.Location xor = MemoryAddressing.map(
                64L * 128, 4, 8, MemoryAddressing.Mapping.XOR);
        require(lowOrder.channel() == 0, "low-order conflict case");
        require(xor.channel() != lowOrder.channel(), "XOR disperses structured conflict");
    }

    private static void invalidConfigurationsAreRejected() {
        expectFailure(() -> MemoryAddressing.Pattern.parse("unknown"));
        expectFailure(() -> MemoryAddressing.Mapping.parse("unknown"));
        expectFailure(() -> MemoryAddressing.map(0, 0, 1, MemoryAddressing.Mapping.LOW_ORDER));
        expectFailure(() -> MemoryAddressing.address(
                MemoryAddressing.Pattern.STRIDED, 0, 0, 0, 0, 1024));
    }

    private static void expectFailure(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected invalid configuration to fail");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
