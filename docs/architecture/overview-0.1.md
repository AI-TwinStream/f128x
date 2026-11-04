# f128x Architecture Overview 0.1

**Date:** 2026-11-04  
**Status:** Initial architectural baseline; not a frozen ISA or microarchitecture

## 1. Purpose

f128x is a proposed 128-bit general-purpose processor architecture designed around workspace-oriented execution, large numbers of hardware-resident thread contexts, and shared execution resources.

The project began with the TMS9900's workspace concept. It now explores a substantially different architecture: thousands of workspaces, multiple independently scheduled threads per workspace, private thread registers, and shared workspace registers.

## 2. Design principles

1. 128-bit general-purpose architectural registers.
2. Workspace-oriented resource organization.
3. Independent architectural state for every hardware thread.
4. Separate private thread registers and shared workspace registers.
5. SMT as a first-class execution model.
6. Dynamic allocation of shared execution resources.
7. Explicit synchronization, protection, and memory-ordering semantics.
8. Scalable physical implementations that may expose fewer contexts than the full architectural target.

## 3. Workspace and thread capacity

| Property | Target |
|---|---:|
| Workspaces | 4,096 |
| Threads per workspace | 16 |
| Total thread contexts | 65,536 |
| Workspace ID width | 12 bits |
| Thread-slot width | 4 bits |
| Combined thread ID | 16 bits |

Proposed thread identifier:

`TID = (WorkspaceID << 4) | ThreadSlot`

A workspace is a logical resource-sharing and protection domain. It is not inherently a core, execution unit, or operating-system process.

Threads within a workspace have independent execution state and share workspace-level resources subject to defined access and synchronization rules.

## 4. Register architecture

### 4.1 Private thread registers

Each thread has 128 general-purpose registers, each 128 bits wide.

- 128 × 16 bytes = 2,048 bytes per thread.
- 65,536 × 2,048 bytes = 128 MiB aggregate private register storage.

### 4.2 Shared workspace registers

Each workspace has 128 general-purpose shared registers, each 128 bits wide.

- 128 × 16 bytes = 2,048 bytes per workspace.
- 4,096 × 2,048 bytes = 8 MiB aggregate shared register storage.

### 4.3 Aggregate architectural register capacity

| Resource | Aggregate storage |
|---|---:|
| Private thread registers | 128 MiB |
| Shared workspace registers | 8 MiB |
| **Total** | **136 MiB** |

These are architectural capacity calculations. They do not prescribe physical register-file ports, banking, replication, access latency, or implementation technology.

### 4.4 Register visibility

Private registers are accessible only to their owning thread.

Shared registers are accessible to threads in their owning workspace, subject to synchronization, atomicity, and protection rules.

Suggested software groupings are conventions only. No shared register is permanently reserved for a particular purpose.

## 5. Execution model

The architecture targets simultaneous multithreading across many resident contexts.

Keep these quantities distinct:
- Resident architectural contexts.
- Ready/eligible contexts.
- Instructions selected for issue.
- Instructions executing in functional units.
- Physical execution-unit count and issue width.

The target of 65,536 resident contexts does not mean 65,536 instructions execute simultaneously.

Threads should be independently schedulable. Workspace affinity may be a locality hint, not a mandatory execution restriction.

## 6. Memory and communication domains

The design anticipates:
1. Thread-private memory.
2. Workspace-shared memory.
3. Workspace-shared registers.

Explicit inter-workspace communication is a candidate capability. Arbitrary cross-workspace register access is not assumed.

## 7. Status

### Established direction
- Workspace-oriented execution.
- Independent thread architectural state.
- Separate private and shared register resources.
- SMT as a central design principle.
- Scalable physical implementations.

### Provisional targets
- 4,096 workspaces.
- 16 threads per workspace.
- 65,536 total contexts.
- 128 private 128-bit registers per thread.
- 128 shared 128-bit registers per workspace.

### Open decisions
- Instruction encoding and instruction formats.
- Virtual and physical address widths.
- Register-file banking and access ports.
- Scheduling and issue-selection architecture.
- Execution-unit organization.
- Cache hierarchy and memory consistency model.
- Atomic and synchronization instruction semantics.
- Workspace memory and inter-workspace communication.
- Privilege, protection, interrupt, and exception models.
- Context creation, termination, and OS interface.
- FPGA feasibility and ASIC implementation profiles.

## 8. Development sequence

1. Register semantics and access model.
2. Thread scheduling, SMT, and issue architecture.
3. Addressing and memory architecture.
4. Synchronization and consistency model.
5. Privilege, protection, interrupts, and exceptions.
6. Instruction set and encoding.
7. Microarchitecture and implementation profiles.
8. Reference implementation and validation.

The specification evolves through evidence and review. No implementation detail should silently become an ISA requirement.
