# fNx Baseline Review Synthesis 0.2

**Date:** 2026-11-04  
**Status:** Review synthesis; recommendations are not frozen architectural decisions

## 1. Scope

This document synthesizes the independent reviews of the architecture research baseline in PR #1:
- Claude reviewed the PR description, README, and `overview-0.1.md`. Its review explicitly did not cover the scheduling, historical research, OS strategy, or roadmap documents because its fetch was truncated.
- Grok reviewed the broader PR documentation, including scheduling, historical research, OS strategy, and roadmap.

The reviewers agree that the 0.1 baseline is coherent and appropriately provisional. The aggregate register arithmetic and TID arithmetic are correct.

## 2. Confirmed arithmetic

- One 128-bit register = 16 bytes.
- Private state per thread = 128 × 16 = 2,048 bytes (2 KiB).
- Aggregate private state = 65,536 × 2,048 = 134,217,728 bytes (128 MiB).
- Shared state per workspace = 128 × 16 = 2,048 bytes (2 KiB).
- Aggregate shared state = 4,096 × 2,048 = 8,388,608 bytes (8 MiB).
- Aggregate architectural register capacity = 136 MiB.
- TID = 12-bit workspace identifier concatenated with a 4-bit thread slot.

The 136 MiB figure describes architectural state capacity, not a physically multiported register file or a requirement that all state be resident in one implementation.

## 3. Findings and disposition

| Finding | Source | Disposition |
|---|---|---|
| Define nonresident context state, backing, spill/fill, and observability | Claude | Accepted as a foundational specification requirement; mechanism remains open |
| Clarify that f128x workspace differs from the TMS9900 memory-resident register workspace | Claude | Accepted; preserve historical inspiration without claiming semantic equivalence |
| Define shared-register atomicity, ordering, hazards, and synchronization | Both | Accepted; evaluate atomic operations and HEP-style full/empty mechanisms without prematurely selecting one |
| Define workspace protection and OS virtualization | Both | Accepted; workspace is not automatically a process or a permanent trust-domain count |
| Move instruction encoding earlier | Claude | Accepted; encoding feasibility becomes a parallel early workstream |
| Justify 65,536 contexts with a latency/throughput model | Both | Accepted; preserve as a provisional architectural exploration target pending quantitative justification |
| State hierarchical/distributed scheduler direction | Both | Accepted as a candidate, not a frozen topology |
| Define fairness and progress guarantees | Grok | Accepted for investigation; distinguish architectural guarantees from policy |
| Define interrupt and exception routing | Both | Accepted as an early architecture contract |
| Define memory ordering across memory/register domains | Both | Accepted; compare candidate consistency models |
| Formalize scaled implementation profiles and resource estimates | Grok | Accepted; profiles are implementation configurations, not incompatible ISAs |
| Add compiler, ABI, assembler, debugger milestones | Claude | Accepted |
| Separate address width from GPR width | Both | Accepted; no width selected yet |
| Clarify OS CPU mapping | Claude | Accepted; do not expose 65,536 contexts as 65,536 OS CPUs by default |
| Move superseded README links under historical documentation | Claude | Accepted |

## 4. Important non-decisions

This review does **not** decide:
- Whether the architectural term remains “workspace.”
- Whether contexts are hardware-resident, cached, or transparently backed in any particular implementation.
- Whether context spill/fill is hardware- or software-managed.
- Whether shared registers use locks, atomic instructions, full/empty state, versioning, or another protocol.
- Whether memory consistency is sequentially consistent, release/acquire, or relaxed by default.
- Whether the target has 48-bit, 64-bit, or another virtual address width.
- Whether the full 4,096 × 16 capacity is retained after performance modelling.
- Whether the scheduler is centralized, hierarchical, clustered, or distributed.
- Which OS is ultimately ported first.

These are explicit design decisions requiring analysis and, where practical, executable models.

## 5. Priority order

1. Architectural context state, residency, and observability.
2. Workspace identity, ownership, protection, and virtualization.
3. Shared-register semantics and synchronization.
4. Memory model and communication domains.
5. Scheduling, fairness, progress, interrupts, and exceptions.
6. Addressing, instruction encoding, and ABI/toolchain.
7. Physical implementation profiles and resource/banking estimates.
8. OS integration.

Some workstreams proceed in parallel; this ordering identifies dependencies, not a strictly serial schedule.

## 6. Review process

Each proposed decision should have:
- Problem statement and constraints.
- Alternatives considered.
- Consequences and tradeoffs.
- Evidence, calculations, simulations, or source references.
- Decision status: proposed, accepted, rejected, or deferred.
- Explicit consequences for ISA, ABI, microarchitecture, and OS interfaces.

Reviewer suggestions are inputs, not decisions. No proposal becomes normative merely because multiple reviewers independently suggested it.
