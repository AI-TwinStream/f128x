# Register File and Workspace Model

## 1. Decision

The working architectural baseline for f128x is:

- 128 general-purpose registers.
- Each GPR is 128 bits wide.
- Total architectural GPR capacity is 16,384 bits, or 2,048 bytes (2 KiB).
- PC, status, and privileged control state are separate from the GPR file.
- The implementation is expected to use a hardware register file, not memory-mapped workspace registers as the original TMS9900 did.

This is an architectural direction for exploration. It does not prescribe FPGA block structure, port count, banking, or pipeline organization.

## 2. Why 128 registers?

The project is intended to be an ambitious successor, not a width-only port of the TMS9900. A larger register file can reduce spills and memory traffic, support more live values, and give compilers more allocation freedom. The register count is intentionally generous.

The trade-off is real: 128 × 128-bit registers represent 2 KiB of state before ECC/parity, ports, bypass networks, and implementation overhead. Multiported register files can be expensive in FPGA fabrics. Synthesis and timing experiments must inform microarchitecture without silently changing the ISA-visible register count.

## 3. Architectural naming

The simplest initial model is a flat register namespace:

- R0 through R127.
- Each register is exactly 128 bits.
- Instructions identify registers by architectural index.
- No GPR is implicitly reserved as zero, stack pointer, frame pointer, or link register unless a later ABI or ISA decision explicitly establishes that convention.

A flat namespace makes the 128-register commitment clear and gives assemblers, compilers, debuggers, and reference models a simple foundation.

## 4. TMS9900 workspace heritage

The original TMS9900 keeps its sixteen 16-bit general-purpose registers in memory. The workspace pointer (WP) identifies the active register workspace. This made context changes distinctive and efficient, but it also means ordinary register accesses are tied to the memory system.

f128x should preserve the idea of a workspace as an organized execution context while avoiding the requirement that every GPR access be a memory transaction.

The leading design direction is:

1. Keep the 128 GPRs in a hardware register file.
2. Retain a dedicated workspace/context control concept inspired by WP.
3. Investigate register windows or banked contexts as a way to make calls, interrupts, and task switches efficient.
4. Define whether a workspace is a view onto a subset of the 128 GPRs, a saved context in memory, or a combination.
5. Ensure all 128 GPRs remain addressable through a well-defined architectural mechanism, even if a windowed fast path is adopted.

No exact window size, overlap policy, save/restore behavior, or workspace instruction is frozen yet.

## 5. Candidate workspace models

| Model | Description | Advantages | Costs / questions |
|---|---|---|---|
| Flat file + software save | All 128 GPRs are always directly addressable; software saves contexts to memory | Simple ISA and compiler model | Context switches may move substantial state |
| Flat file + banked control contexts | GPR file is flat, with selected banks reserved or switched for privileged/interrupt contexts | Fast interrupt entry; clear general register model | Bank count and visibility rules add complexity |
| Register windows | A workspace pointer selects a window into a larger physical register file | Preserves strong TMS9900 lineage; fast calls/context changes | Window overlap, spill/fill, and debugging semantics need careful design |
| Memory-backed workspace | GPRs remain memory-resident as on TMS9900, but widened and expanded | Maximum conceptual continuity | Register accesses inherit memory latency and bandwidth costs; poor fit for the ambitious successor |

The current preference is a hardware register file with a modernized workspace/window mechanism. The final choice requires simulation and FPGA cost measurements.

## 6. Context and exception requirements

Whichever model is selected, the architecture must specify:

- Which GPRs are caller-saved and callee-saved at the ABI level.
- Whether interrupts receive a dedicated bank/window or save state through a defined mechanism.
- How nested exceptions work.
- How workspace/window state is saved and restored.
- How debuggers enumerate and modify all 128 GPRs.
- How context state is represented in core dumps and task structures.
- Whether register windows can be disabled or bypassed by privileged software.
- What happens on window exhaustion or invalid workspace state.

The ISA must define deterministic architectural behavior. Microarchitectural optimizations must not make context state unpredictable.

## 7. Register-file implementation experiments

Before selecting an FPGA, compare at least:

- Distributed-flop implementation.
- Replicated or banked memories to provide multiple read ports.
- Time-multiplexed register-file access where pipeline timing permits.
- Hybrid implementation with a fast working set and backing state, if it remains architecturally transparent.

Measure resource utilization, maximum clock, read/write latency, bypass complexity, and behavior under interrupt/context pressure. Include reset and debug access in the design.

## 8. Open decisions

- Flat versus windowed architectural visibility.
- Workspace/window register format and width.
- Window size, overlap, and allocation policy.
- Number of privileged/interrupt banks, if any.
- Context-switch and exception entry/return semantics.
- Whether multi-register load/store or context-save instructions are needed.
- ABI register roles and calling convention.
- Whether any registers have special architectural semantics.

These decisions should be resolved in the ISA v0.1 design review, not guessed during RTL implementation.
