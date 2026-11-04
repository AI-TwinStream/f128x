# f128x Roadmap

The current architectural direction is a workspace-oriented, massively multithreaded 128-bit general-purpose processor. The 0.1 research baseline remains provisional. Independent reviews have been synthesized in [Review synthesis 0.2](reviews/review-synthesis-0.2.md).

## Current review-driven work

- [x] Capture independent Claude and Grok review findings.
- [x] Separate architectural state from physical context residency.
- [x] Add workspace protection and shared-state design questions.
- [x] Add scheduling progress, fairness, and interrupt design questions.
- [x] Add early ISA, address-width, ABI, and toolchain workstream.
- [ ] Resolve architectural context observability and residency contract.
- [ ] Define workspace ownership, isolation, virtualization, and lifecycle.
- [ ] Define shared-register atomicity and memory-ordering semantics.
- [ ] Select candidate memory consistency model(s).
- [ ] Define minimum progress/fairness guarantees.
- [ ] Define interrupt and exception routing/return semantics.
- [ ] Justify 65,536 contexts with latency/throughput workload modelling.
- [ ] Produce register banking, port, area, and timing estimates.
- [ ] Compare scheduler topologies with executable models.
- [ ] Complete instruction encoding feasibility study.
- [ ] Decide virtual and physical address widths independently.
- [ ] Prototype assembler, disassembler, interpreter, and compiler backend.
- [ ] Draft ABI and debugger support.

## 0. Architecture definition

- [x] Establish 128-bit general-purpose register direction.
- [x] Establish provisional capacity target: 4,096 workspaces × 16 threads.
- [x] Establish provisional register target: 128 private registers/thread and 128 shared registers/workspace.
- [ ] Define complete thread and workspace architectural state.
- [ ] Define context residency, backing, save/restore, and privileged observability.
- [ ] Decide whether “workspace” remains the architectural term; document TMS9900 distinction.
- [ ] Define ownership, protection, process virtualization, and stale-ID prevention.
- [ ] Define private/shared memory and inter-workspace communication.
- [ ] Define 128-bit arithmetic semantics, flags, carry/borrow, overflow, shifts, and rotates.
- [ ] Define instruction formats, extension space, and encoding.
- [ ] Define byte order, byte addressing, load/store widths, and alignment.
- [ ] Define virtual and physical address widths independently of GPR width.
- [ ] Define exceptions, interrupts, reset state, and privilege model.
- [ ] Define ABI, calling convention, stack, and process/workspace/thread mapping.
- [ ] Publish ISA specification and conformance-test format.

## 1. Historical architecture research

- [ ] Study TMS9900 workspace-pointer and context behavior.
- [ ] Study Denelcor HEP scheduling and full-empty-reserve synchronization.
- [ ] Study Tera/Cray MTA latency hiding and hardware-thread scheduling.
- [ ] Study CDC 6600 scoreboarding and functional-unit reservations.
- [ ] Study Transputer communication channels.
- [ ] Study XMOS XCore hardware-thread implementation.
- [ ] Study Cray vector chaining and register organization.
- [ ] Record explicit adopt/adapt/reject decisions with sources and quantitative evidence.

## 2. Scheduler and execution architecture

- [ ] Compare centralized, hierarchical, and distributed ready-thread selection.
- [ ] Define resident, ready, selected, issued, blocked, and executing states.
- [ ] Define issue width and execution-unit abstraction.
- [ ] Define dependency tracking and wakeup behavior.
- [ ] Define scheduler fairness and progress guarantees.
- [ ] Model shared-register banking and conflicts.
- [ ] Evaluate interrupt and exception delivery across resident contexts.
- [ ] Model context capacity against latency, throughput, and workload readiness.
- [ ] Build scheduler simulations before committing to RTL structure.

## 3. ISA, ABI, and toolchain

- [ ] Evaluate fixed, variable, and mixed-width instruction encodings.
- [ ] Evaluate register namespace and operand encoding costs.
- [ ] Decide virtual/physical address widths and load/store model.
- [ ] Implement an architectural reference interpreter.
- [ ] Implement assembler and disassembler.
- [ ] Prototype LLVM and/or GCC backend feasibility.
- [ ] Define ABI, calling convention, object format, and relocations.
- [ ] Add debugger target, debug state access, and conformance tests.

## 4. Software reference model

- [ ] Implement an executable architectural reference model.
- [ ] Add instruction-level tests and golden vectors.
- [ ] Add tests for private/shared registers and synchronization.
- [ ] Add workspace/thread lifecycle and protection tests.
- [ ] Add small bare-metal examples and a test runner.

## 5. FPGA and microarchitecture

- [ ] Select HDL and FPGA board after resource studies.
- [ ] Estimate private/shared register storage, ports, banks, replication, and bypass.
- [ ] Synthesize candidate structures for 64-, 256-, and 512-context research profiles.
- [ ] Evaluate the full 65,536-context target analytically.
- [ ] Evaluate scheduler structures at multiple context capacities.
- [ ] Implement scalar execution pipeline and control flow.
- [ ] Implement memory, atomics, exceptions, and interrupts.
- [ ] Run simulation and formal checks where practical.
- [ ] Run the same conformance suite against simulator and RTL.

## 6. Platform and operating system

- [ ] Define reset, boot, timer, interrupt, and memory platform contracts.
- [ ] Build a freestanding runtime and bring-up harness.
- [ ] Evaluate TI-99/4A compatibility as a separate platform/bridge effort.
- [ ] Evaluate NetBSD as the first full OS-porting candidate.
- [ ] Study DragonFly BSD scheduler and SMP design.
- [ ] Evaluate FreeBSD as an alternative.
- [ ] Define process-to-workspace and OS-thread-to-hardware-context mappings.
- [ ] Do not expose every hardware context as an OS CPU by default.
- [ ] Bring up a minimal kernel before a full userland.

## Implementation profiles under study

These profiles are experimental configurations, not separate architectural ISAs:
- Tiny functional model: 1 workspace × 2 threads.
- Small FPGA study: 16 workspaces × 4 threads (64 contexts).
- Medium FPGA study: 32 workspaces × 8 threads (256 contexts).
- Larger research prototype: 64 workspaces × 8 threads (512 contexts).
- Architectural capacity target: 4,096 workspaces × 16 threads (65,536 contexts).

## Milestones

**M0: Architecture research baseline 0.1**  
Completed as a draft research baseline. Not a frozen ISA.

**M1: Architectural contracts 0.2**  
Define context state and residency, workspace ownership/protection, shared-state semantics, memory ordering, scheduling progress, and interrupt contracts.

**M2: ISA and toolchain feasibility**  
Encoding, address model, interpreter, assembler/disassembler, compiler backend prototype, and ABI draft.

**M3: Executable architecture model**  
A testable reference model and scheduler/memory experiments.

**M4: FPGA feasibility prototype**  
Register banking and scheduler resource estimates followed by a scaled implementation.

**M5: Platform bring-up**  
Freestanding runtime, then OS-port feasibility when ISA/MMU/interrupt/ABI contracts stabilize.

## Non-goals for early milestones

- Freezing a physical scheduler topology before simulation.
- Requiring every implementation to instantiate 65,536 physical contexts.
- Assuming 128-bit GPRs imply 128-bit addresses, instructions, buses, or execution units.
- Claiming TMS9900 binary compatibility.
- Selecting a final OS before the ISA, MMU, interrupt, and ABI contracts exist.
