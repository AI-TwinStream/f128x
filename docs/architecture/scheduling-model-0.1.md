> **Historical model note (2026-11-05):** This scheduling study uses the original f128x capacity assumptions (4,096 workspaces × 16 threads). It remains a research baseline only. Do not treat these counts as family invariants.

# Historical Scheduling Model Research 0.1

**Date:** 2026-11-04  
**Status:** Provisional research model; not an ISA commitment

## 1. Problem statement

f128x targets 65,536 resident hardware thread contexts:
- 4,096 workspaces.
- 16 threads per workspace.

This is a capacity target, not a promise that all contexts can issue in the same cycle.

A naive centralized scheduler that examines every context every cycle is not an acceptable default assumption. Scheduler organization must scale with the implementation.

## 2. Historical inputs

### Denelcor HEP

HEP explored a hierarchy of parallel processes and hardware-supported synchronization, including full-empty-reserve mechanisms. Its lesson for f128x is that readiness and synchronization state should be explicit architectural or microarchitectural concerns.

### Tera / Cray MTA

The Tera MTA supported 128 hardware threads per processor and used rapid switching among contexts to hide memory latency. Its lesson is to treat thread-level parallelism as a resource for covering stalls while co-designing the scheduler and memory system.

### CDC 6600

The CDC 6600 used multiple functional units and a scoreboard to track unit reservations and register dependencies. Its lesson is that selecting a thread is not the same as proving an instruction can safely issue.

## 3. Logical scheduling stages

The following is a logical decomposition, not a mandate for one centralized implementation:

1. **Context state tracking:** Maintain each thread's lifecycle and readiness.
2. **Eligibility filtering:** Exclude threads blocked by dependencies, synchronization, exceptions, or unavailable resources.
3. **Candidate selection:** Select eligible threads according to scheduling policy.
4. **Instruction validation:** Check operands, dependencies, permissions, and execution-unit availability.
5. **Issue:** Dispatch instructions to suitable execution resources.
6. **Completion and wakeup:** Release resources, publish results, and make dependent threads eligible.

Thread selection and instruction issue are separate operations.

## 4. Hierarchical scheduling hypothesis

A candidate design has two logical levels.

### Workspace-level policy
- Workspace priority and service policy.
- Resource quotas or limits.
- Workspace lifecycle and protection.
- Locality and affinity hints.
- Fairness across workspaces.

### Thread-level selection
- Independent readiness.
- Thread priority.
- Dependency and synchronization state.
- Memory-wait wakeups.
- Per-thread issue eligibility.

The workspace level must not serialize every thread decision. A workspace must not be selected as one indivisible scheduling unit before any of its threads can execute.

## 5. Candidate physical organization

Investigate a distributed or hierarchical ready structure:
- Per-workspace thread-ready state for 16 thread slots.
- A summary indicating whether a workspace has eligible threads.
- Implementation-sized groups of workspace summaries.
- Local candidate selection near execution clusters.
- Work stealing or candidate transfer when local queues are empty.
- Optional locality hints for register and memory placement.

A 16-bit ready bitmap per workspace is a natural representation for 16 thread slots. It does not by itself solve global candidate selection, priority arbitration, or wakeup propagation.

## 6. Thread states

Candidate states:
- Ready.
- Running/issued.
- Waiting for memory.
- Waiting for synchronization.
- Suspended.
- Exception handling.
- Terminated.

The design must define whether a thread can have multiple outstanding instructions and how many independent instruction streams may be in flight.

## 7. Issue width and execution resources

Issue width is an implementation parameter.

The architecture must distinguish:
- Resident contexts.
- Eligible contexts.
- Issue slots.
- Execution units and types.
- Outstanding memory operations.

Candidate execution resources include integer, branch, floating-point, vector, load/store, and atomic units.

A single global issue scheduler is not assumed. Clustered execution and distributed scheduling must be evaluated.

## 8. Register and dependency interaction

The 128 private registers per thread and 128 shared registers per workspace create substantial state and access requirements.

The scheduler and execution architecture must define:
- Operand availability.
- Register read/write bandwidth.
- Shared-register bank conflicts.
- RAW, WAR, and WAW dependency handling.
- Atomic shared-register operations.
- Wakeup after memory or synchronization waits.

The 136 MiB aggregate architectural register capacity is not the same as the cost of a physically multiported register file. Banking, replication, local access, and latency must be evaluated.

## 9. Fairness and progress

The scheduling policy must avoid:
- Starvation of low-priority work.
- Workspace monopolization.
- Priority inversion.
- Unbounded delay for synchronization participants.
- Deadlock caused by resource reservation.
- Unfairness caused by locality optimization.

Priority and fairness policies may be configurable, but the architecture must define minimum progress guarantees where required.

## 10. Research questions

1. Should selection be globally oldest-ready, round-robin, age-based, priority-based, or hybrid?
2. How many contexts should each physical scheduler cluster manage?
3. Should workspace affinity be soft or hard?
4. How are wakeups propagated across scheduler clusters?
5. How are memory dependencies tracked?
6. How many instructions may be in flight per thread?
7. How are shared-register bank conflicts handled?
8. What fairness guarantees are architectural versus OS policy?
9. How does interrupt delivery interact with thousands of resident contexts?
10. What is the minimum useful context count for small FPGA implementations?

## 11. Next step

Develop and compare at least three candidate scheduler organizations:
- Centralized ready selection.
- Hierarchical workspace/thread selection.
- Distributed scheduler clusters with local ready queues and work stealing.

Evaluate each against area, timing, power, fairness, wakeup latency, implementation complexity, and scaling from small FPGA to ASIC.

No scheduler organization is frozen by this document.
