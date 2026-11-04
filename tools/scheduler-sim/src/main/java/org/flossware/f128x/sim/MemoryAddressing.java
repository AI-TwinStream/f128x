package org.flossware.f128x.sim;

import java.util.Locale;

/**
 * Deterministic synthetic address generation and channel/bank mapping.
 *
 * This deliberately models address placement only. It is not a cache,
 * coherence, DRAM-row, or physical-address translation model.
 */
final class MemoryAddressing {
    enum Pattern {
        RANDOM,
        SEQUENTIAL,
        STRIDED,
        SHARED_REGION;

        static Pattern parse(String value) {
            try {
                return valueOf(value.replace('-', '_').toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Unknown address pattern '" + value
                                + "'. Expected random, sequential, strided, or shared_region",
                        exception);
            }
        }
    }

    enum Mapping {
        LOW_ORDER,
        XOR;

        static Mapping parse(String value) {
            try {
                return valueOf(value.replace('-', '_').toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Unknown address mapping '" + value
                                + "'. Expected low_order or xor",
                        exception);
            }
        }
    }

    record Location(int channel, int bank) {}

    private MemoryAddressing() {}

    static long address(Pattern pattern, int thread, long sequence,
            long randomAddress, long stride, long regionBytes) {
        if (thread < 0 || sequence < 0 || stride < 1 || regionBytes < 1) {
            throw new IllegalArgumentException("Invalid synthetic address parameters");
        }
        long threadRegion = (long) thread * regionBytes;
        return switch (pattern) {
            case RANDOM -> randomAddress;
            case SEQUENTIAL -> threadRegion + (sequence * 64L) % regionBytes;
            case STRIDED -> threadRegion + (sequence * stride) % regionBytes;
            case SHARED_REGION -> (sequence * stride) % regionBytes;
        };
    }

    static Location map(long address, int channels, int banksPerChannel,
            Mapping mapping) {
        if (channels < 1 || banksPerChannel < 1) {
            throw new IllegalArgumentException("Channel and bank counts must be positive");
        }
        long cacheLine = address >>> 6;
        long channelBits = cacheLine;
        long bankBits = cacheLine / channels;
        if (mapping == Mapping.XOR) {
            channelBits ^= cacheLine >>> 7;
            channelBits ^= cacheLine >>> 17;
            bankBits ^= cacheLine >>> 11;
            bankBits ^= cacheLine >>> 23;
        }
        int channel = (int) Long.remainderUnsigned(channelBits, channels);
        int bank = (int) Long.remainderUnsigned(bankBits, banksPerChannel);
        return new Location(channel, bank);
    }
}
