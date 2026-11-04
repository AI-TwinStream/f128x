# f128x Roadmap

The current architectural direction is a workspace-oriented, massively multithreaded 128-bit general-purpose processor. The roadmap separates architectural specification, executable models, implementation, platform bring-up, and operating-system work.

See:
- [Architecture overview 0.1](architecture/overview-0.1.md)
- [Scheduling model research 0.1](architecture/scheduling-model-0.1.md)
- [Historical architecture research 0.1](../research/historical-architectures-0.1.md)
- [Operating-system strategy 0.1](../platforms/os-strategy-0.1.md)

## 0. Architecture definition

- [x] Establish the 128-bit general-purpose register direction.
- [x] Establish the workspace/thread capacity target: 4,096 workspaces × 16 threads.
- [x] Establish private and shared register direction: 128 private registers per thread and 128 shared registers per workspace.
- [ ] Specify precise workspace lifecycle, ownership, and protection semantics.
- [ ] Specify thread lifecycle and complete architectural context.
- [ ] Specify shared-register visibility, atomicity, and synchronization.
- [ ] Specify shared-memory and inter-workspace communication models.
- [ ] Specify 128-bit integer semantics, flags, carry/borrow, overflow, shifts, and rotates.
- [ ] Choose instruction encoding and define reserved/extension space.
- [ ] Choose byte order, byte addressing, load/store widths, and alignment rules.
- [ ] Choose virtual and physical address widths independently of GPR width.
- [ ] Specify exceptions, interrupts, reset state, and privilege model.
- [ ] Define ABI, process/workspace/thread mapping, stack convention, and calling convention.
- [ ] Publish ISA specification and conformance-test format.

## 1. Historical architecture research

- [ ] Study TMS9900 workspace-pointer and context behavior.
- [ ] Study Denelcor HEP scheduling and full-empty-reserve synchronization.
- [ ] Study Tera/Cray MTA latency hiding and hardware-thread scheduling.
- [ ] Study CDC 6600 scoreboarding and functional-unit reservations.
- [ ] Study Transputer communication channels.
- [ ] Study XMOS XCore hardware-thread implementation.
- [ ] Study Cray vector chaining and register organization.
- [ ] Record explicit adopt/adapt/reject decisions for each mechanism.

## 2. Scheduler and execution architecture

- [ ] Compare centralized, hierarchical, and distributed ready-thread selection.
- [ ] Define the distinction between resident, ready, selected, and executing contexts.
- [ ] Define issue width and execution-unit abstraction.
- [ ] Define dependency tracking and wakeup behavior.
- [ ] Define scheduler fairness and progress guarantees.
- [ ] Model shared-register banking and conflicts.
- [ ] Evaluate interrupt and exception delivery across many resident contexts.
- [ ] Build scheduler simulations before committing to RTL structure.

## 3. Software reference model

- [ ] Implement an executable architectural reference model.
- [ ] Add assembler and disassembler.
- [ ] Add instruction-level tests and golden vectors.
- [ ] Add tests for all private and shared registers.
- [ ] Add workspace/thread lifecycle and synchronization tests.
- [ ] Add small bare-metal examples and a test runner.

## 4. FPGA and microarchitecture

- [ ] Select HDL and FPGA development board after resource studies.
- [ ] Synthesize candidate private and shared register organizations.
- [ ] Evaluate register-file banking, ports, bypassing, and timing.
- [ ] Evaluate scheduler structures at multiple context capacities.
- [ ] Implement the scalar execution pipeline and control flow.
- [ ] Implement memory, atomic operations, and exception behavior.
- [ ] Integrate block RAM and debug/serial interfaces.
- [ ] Run simulation and formal checks where practical.
- [ ] Run the same conformance suite against simulator and RTL.

## 5. Platform and operating system

- [ ] Define reset, boot, timer, interrupt, and memory platform contracts.
- [ ] Build a freestanding runtime and bring-up harness.
- [ ] Evaluate TI-99/4A compatibility as a separate platform/bridge effort.
- [ ] Evaluate NetBSD as the first full OS-porting candidate.
- [ ] Study DragonFly BSD scheduler and SMP design.
- [ ] Evaluate FreeBSD as an alternative.
- [ ] Define process-to-workspace and OS-thread-to-hardware-thread mappings.
- [ ] Bring up a minimal kernel before a full userland.

## First milestone

**M0: Architectural specification 0.1**

A coherent definition of workspaces, hardware threads, private/shared register state, execution model, and open decisions. This milestone does not freeze the ISA or microarchitecture.

## Second milestone

**M1: Scheduler and register access model**

A testable model of ready-thread selection, issue eligibility, dependency tracking, shared-register access, and synchronization.

## Non-goals for the first milestones

- Freezing a physical scheduler topology before simulation.
- Requiring every implementation to instantiate 65,536 physical contexts.
- Assuming all 128-bit registers imply 128-bit addresses or buses.
- Claiming TMS9900 binary compatibility.
- Selecting a final OS before the ISA, MMU, interrupt, and ABI contracts are defined.
