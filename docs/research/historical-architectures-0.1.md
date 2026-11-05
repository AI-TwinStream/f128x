# fNx Historical Architecture Research 0.1

**Date:** 2026-11-04  
**Status:** Research plan and initial findings

## 1. Objective

Study historical processor and computer architectures for mechanisms relevant to the fNx family. The objective is to understand the original design, not to collect features by analogy.

Each study should document the original problem, actual mechanisms, performance motivations, tradeoffs, limitations, and whether fNx should adopt, adapt, or reject the idea.

A historical feature is not an fNx requirement merely because it is interesting.

## 2. Priority research subjects

### 2.1 Texas Instruments TMS9900

**Focus:** Workspace pointer and register organization.

The TMS9900 uses a workspace pointer to identify a register workspace in memory. Its sixteen general registers are addressed through that workspace.

Investigate:
- Workspace layout and alignment.
- Context-switch and interrupt behavior.
- Memory traffic and latency associated with workspace access.
- Register visibility and addressing.
- Limits of scaling a memory-resident workspace to thousands of contexts.

Potential lesson: separate execution-context identity from the physical location and organization of its register state.

### 2.2 Denelcor HEP

**Priority: Critical**  
**Focus:** Fine-grained multithreading and hardware synchronization.

HEP research describes a multiprocessor organized around hierarchies of parallel processes. Its synchronization model included hardware full-empty-reserve mechanisms and semaphores.

Investigate:
- Process and thread hierarchy.
- Ready-process selection.
- Pipeline occupancy and latency tolerance.
- Full-empty-reserve synchronization.
- Memory and FIFO organization.
- Fairness, starvation, and synchronization overhead.

Potential lessons:
- Hardware-visible synchronization state can simplify producer/consumer coordination.
- Thread readiness should be explicit.
- Scheduling must be designed around latency tolerance, not merely context switching.

Primary source:
- NASA NTRS, “HEP - A semaphore-synchronized multiprocessor with central control” (1976): https://ntrs.nasa.gov/citations/19770041725

### 2.3 Tera / Cray Multi-Threaded Architecture (MTA)

**Priority: Critical**  
**Focus:** Massive multithreading and memory-latency tolerance.

The Tera MTA supported 128 hardware threads per processor. Its design used rapid switching among hardware thread contexts to mask memory latency. An evaluation describes a uniform-access shared-memory model without a data cache in the evaluated design.

Investigate:
- Thread issue cadence and eligibility.
- Context-state implementation.
- Memory latency and thread switching.
- Register organization.
- Synchronization and memory consistency.
- Compiler support and parallelization.
- Performance limits when thread-level parallelism is insufficient.

Potential lessons:
- Many resident contexts are useful only when the scheduler can find ready work.
- Memory-system design and scheduling must be co-designed.
- A cacheless latency-tolerant design is a comparison point, not an automatic fNx choice.

Primary sources:
- Sandia National Laboratories, “Final report for the Tera Computer TTI CRADA” (1997): https://www.sandia.gov/research/publications/details/final-report-for-the-tera-computer-tti-crada-1997-01-01/
- CaltechAUTHORS, “An Initial Evaluation of the Tera Multithreaded Architecture and Programming System” (1998): https://authors.library.caltech.edu/records/b3wkr-b7p27

### 2.4 CDC 6600

**Priority: Critical**  
**Focus:** Scoreboarding and dynamic functional-unit scheduling.

The CDC 6600 used multiple functional units and a scoreboard to track unit reservations and register dependencies. Instructions were issued from an instruction stack, with execution units operating independently when resources and operands were available.

Investigate:
- Functional-unit reservation.
- Register-result reservation.
- Operand availability.
- Structural and data hazards.
- Issue ordering and completion behavior.
- How scoreboarding changes with multiple independent threads.

Potential lessons:
- Execution resources need explicit reservation and dependency tracking.
- Thread selection and execution-unit scheduling are distinct responsibilities.
- A CDC-style scoreboard is a useful baseline, but a 65,536-context design may require hierarchical or distributed tracking.

Primary references:
- CDC 6600 Reference Manual: https://ed-thelen.org/comp-hist/CDC-6600RefMan.pdf
- “Design of a Computer: The Control Data 6600”: https://ed-thelen.org/comp-hist/DesignOfAComputer_CDC6600.pdf

### 2.5 INMOS Transputer

**Priority: Critical**  
**Focus:** Explicit communication and message-oriented parallelism.

Investigate:
- Hardware communication channels.
- Blocking and rendezvous semantics.
- Message transfer costs.
- Processor topology.
- Deadlock and flow-control behavior.
- Programming-language and compiler support.

Potential lessons:
- Shared registers are not the only useful communication mechanism.
- Explicit message passing may complement workspace-shared memory.
- Inter-workspace communication must define ownership, backpressure, ordering, and deadlock behavior.

### 2.6 XMOS XCore

**Priority: High**  
**Focus:** Practical hardware-thread implementation and deterministic scheduling.

Investigate:
- Dedicated thread state.
- Thread scheduling and issue behavior.
- Synchronization primitives.
- Timing predictability.
- Resource sharing and contention.

Potential lessons:
- Examine the physical cost of architectural thread state.
- Study deterministic scheduling as an optional execution policy.
- Understand how hardware thread count interacts with available execution resources.

### 2.7 SPARC

**Priority: Medium**  
**Focus:** Register windows and procedure-call behavior.

Investigate overlapping register windows, window overflow/underflow, ABI implications, and interrupt behavior.

Potential lesson: evaluate register windows against the fNx family's already-large private register set. Do not assume they are beneficial.

### 2.8 Cray-1 and CDC STAR-100

**Priority: High**  
**Focus:** Vector execution and memory throughput.

Investigate vector register organization, vector length and chaining, functional-unit pipelines, memory bandwidth and bank conflicts, and scalar/vector interaction.

Potential lesson: 128-bit general-purpose registers do not replace a well-designed vector execution model.

## 3. Research mapping

| 128-bit architecture member subsystem | Primary historical references |
|---|---|
| Workspace organization | TMS9900 |
| Massive thread scheduling | HEP, Tera MTA |
| Dynamic execution scheduling | CDC 6600 |
| Inter-workspace communication | Transputer |
| Hardware-thread implementation | XMOS XCore |
| Procedure-call register organization | SPARC |
| Vector execution | Cray-1, CDC STAR-100 |
| Protection and system architecture | Burroughs B5000, IBM System/360 |

## 4. Initial conclusions

The strongest direct research inputs are:
1. HEP and Tera MTA for latency-tolerant multithreading.
2. CDC 6600 for dependency tracking and functional-unit reservation.
3. TMS9900 for workspace-based register organization.
4. Transputer for explicit communication between independent execution domains.

These mechanisms solve different problems and should not be collapsed into one scheduler or one communication primitive.

## 5. Next research deliverable

Produce a detailed HEP/MTA comparison covering context state, ready-thread selection, issue cadence, pipeline occupancy, memory latency handling, synchronization, fairness, and physical resource costs.

Then compare candidate scheduling mechanisms against the fNx family's target of 4,096 workspaces and 16 threads per workspace.
