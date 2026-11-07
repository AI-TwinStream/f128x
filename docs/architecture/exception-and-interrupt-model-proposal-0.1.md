# fNx Exception, Trap, and Interrupt Model Proposal 0.1

**Date:** 2026-11-06  
**Status:** Architectural requirements and candidate model; delivery semantics are not ratified

## 1. Purpose

This proposal defines the minimum architectural distinctions and questions for synchronous exceptions, software traps, asynchronous interrupts, and reset events across fNx members.

The goal is a common observable contract across native widths N. It does not select a physical interrupt controller, vector-table layout, priority encoder, or scheduler topology.

## 2. Event classes

The architecture must distinguish at least these event classes:

| Event class | Origin | Architectural meaning |
|---|---|---|
| Synchronous exception | Current instruction or its execution | The instruction cannot complete normally or reports a defined exceptional condition |
| Software trap | Explicit instruction | Intentional transfer to a privileged or defined service path |
| Asynchronous interrupt | External device, timer, platform, or inter-processor source | Event independent of the currently executing instruction |
| Non-maskable event | Platform-defined critical source | Event that cannot be suppressed by ordinary interrupt masking |
| Reset | CPU unit, workspace domain, or platform control | Defined transition to an initialization state |
| Debug event | Debug facility or configured execution condition | Controlled entry to a debug state |

These classes may share hardware entry machinery, but their restart, priority, masking, and privilege rules must remain explicit.

## 3. Synchronous exceptions

### 3.1 Required fault attribution

A synchronous exception must identify:
- The faulting instruction address.
- The instruction identity or enough information to identify the faulting encoding.
- The exception class and cause.
- The effective virtual address, when applicable.
- The access type, when applicable.
- The privilege and address-space context.
- Whether any architectural state was committed before the exception.
- Whether retry, restart, emulation, or termination is permitted.

### 3.2 Precise state

**Recommended baseline:** Synchronous architectural exceptions are precise at the instruction boundary.

A precise exception means that:
- All architecturally earlier instructions have committed.
- The faulting instruction has not committed effects that the architecture defines as all-or-nothing.
- No later instruction has committed visible architectural effects.
- The saved state is sufficient to resume, emulate, or terminate according to the exception class.

Some operations may be architecturally nonrestartable or may report partial completion, but such behavior must be explicitly specified per operation. It must not arise from an undocumented implementation detail.

### 3.3 Exception taxonomy

At minimum, the ISA needs defined classes for:
- Unassigned opcode.
- Assigned but unsupported operation.
- Malformed or truncated instruction.
- Privilege violation.
- Illegal control-state transition.
- Arithmetic exception, where enabled by the operation's semantics.
- Instruction-fetch translation/protection fault.
- Data translation/protection fault.
- Alignment or access-size fault.
- Atomic-operation or memory-system failure.
- External/fabric access error.
- Debug exception.

The standard may subdivide these classes, but software-visible distinctions must be stable.

## 4. Software traps and system calls

A software trap is an explicit architectural instruction or operation that requests a defined service transition.

The contract must specify:
- Calling privilege and permitted targets.
- Saved instruction address and return behavior.
- Argument and result state.
- Whether the transition changes address space or workspace.
- How service identity is encoded and validated.
- Error return conventions.
- Reentrancy and nested-trap behavior.
- Interaction with interrupts during entry and return.

The ISA trap mechanism should not hard-code one operating system's syscall ABI.

## 5. Asynchronous interrupts

### 5.1 Delivery contract

The architecture must define:
- Interrupt source identity and priority.
- Target selection: thread, workspace, CPU unit, or platform service context.
- Masking and enable state.
- Pending-state retention and acknowledgement.
- Edge versus level semantics, or an abstracted equivalent.
- Nesting and preemption rules.
- Affinity and migration behavior.
- Delivery to blocked or nonresident contexts.
- Ordering relative to outstanding memory and fabric operations.
- Interrupt return and state restoration.

An interrupt must not be lost merely because its target context is temporarily nonresident. The platform must define how pending events are retained and made visible.

### 5.2 Latency and fairness

The architecture should define bounded delivery guarantees only where the platform can support them. Any bound must state assumptions about masking, higher-priority events, non-preemptible regions, power state, and fabric availability.

A generic claim of "low latency" is not a conformance requirement.

## 6. Non-maskable and platform events

The standard must define which events qualify as non-maskable and what state is guaranteed to be available during delivery.

The FCP may report and coordinate platform faults, but a CPU's architectural exception/interrupt behavior must not depend on the FCP being in the normal instruction path.

Fatal machine checks, power-failure notification, thermal shutdown, watchdog expiry, and fabric-fatal events need explicit classification and recovery expectations.

## 7. Context, workspace, and privilege interaction

The architecture must define how events are routed when:
- The target thread is blocked or suspended.
- The target thread is nonresident.
- A workspace is being destroyed or migrated.
- A thread is in a privileged service.
- The CPU unit is entering or leaving a low-power state.
- An interrupt arrives during exception entry or return.
- A workspace has no runnable thread.

Event delivery must preserve workspace isolation. An unprivileged thread must not redirect an interrupt into another workspace or gain authority by forging a context identifier.

## 8. Reset model

Reset must be specified separately for:
- One hardware thread/context.
- One workspace.
- One CPU unit.
- One board or backplane.
- The complete platform.

For each reset scope, define:
- Which architectural state is cleared, retained, or undefined.
- How outstanding memory/fabric transactions are handled.
- How pending interrupts are handled.
- How privilege and translation state are initialized.
- How reset cause is reported.
- Whether recovery is restartable.
- How other CPU units observe partial reset or failure.

A platform reset must not imply that unrelated backplanes or CPU units are reset unless the hierarchy contract says so.

## 9. Privilege and protection prerequisites

The exception model depends on a privilege model that defines:
- Architectural privilege levels or modes.
- Entry and return authority.
- Protected control registers.
- Translation and memory-protection authority.
- Interrupt-controller configuration authority.
- Debug access and authorization.
- FCP/host management authority boundaries.

The number and encoding of privilege levels remain open. The architecture should specify authority and transitions before assigning numeric mode encodings.

## 10. Conformance requirements

A reference implementation and conformance suite must test:
1. Precise synchronous exception state.
2. Faulting instruction and address attribution.
3. Restart and nonrestart behavior for each exception class.
4. Unassigned, unsupported, and malformed instruction distinctions.
5. Privilege checks and illegal transitions.
6. Software trap entry and return.
7. Interrupt masking, pending retention, priority, and nesting.
8. Delivery to blocked and nonresident contexts.
9. Interrupt interaction with outstanding memory/fabric requests.
10. Reset at each supported scope.
11. Workspace isolation during event routing.
12. Debug event authorization and state consistency.

## 11. Decision status

| Topic | Status |
|---|---|
| Synchronous versus asynchronous event distinction | Required |
| Precise synchronous exception baseline | Recommended; not ratified |
| Exception taxonomy | Required; exact codes and state open |
| Software trap ABI | Open |
| Interrupt target and priority model | Open |
| Masking, nesting, and pending semantics | Open |
| Non-maskable event set | Open |
| Reset scopes and state | Open |
| Privilege levels and authority model | Open |
| FCP interaction | Control-plane coordination only; detailed authority open |
| Conformance tests | Required; not yet implemented |

## 12. Related specifications

- [fNx family and architectural contract](fnx-family-and-architectural-contract-0.1.md)
- [fNx context state and residency](context-state-and-residency-0.2.md)
- [fNx scheduling progress and interrupts](scheduling-progress-and-interrupts-0.2.md)
- [fNx workspace protection and shared state](workspace-protection-and-shared-state-0.2.md)
- [fNx address-space and memory-model proposal](address-space-and-memory-model-proposal-0.1.md)
- [fNx control processor and management plane](fnx-control-and-management-plane-0.1.md)
- [fNx roadmap](../roadmap.md)
