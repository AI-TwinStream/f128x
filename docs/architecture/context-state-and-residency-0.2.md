# fNx Context State and Residency 0.3

**Date:** 2026-11-05  
**Status:** Architectural investigation; no physical residency mechanism selected

## 1. Scope

This document applies to the fNx family. For an fNx member, N is the native architectural width in bits. Architectural GPRs are N bits. The number of registers, contexts, workspaces, and physical residency capacity are separate design parameters.

The current 128-workspace × 128-thread × 128-private-register/128-shared-register configuration is an f128x research profile, not a family-wide invariant. At f128x width it accounts for 32 MiB of private register state plus 256 KiB shared state. Do not extrapolate this storage total unchanged to other N values.

## 2. Architectural state versus physical residency

The specification must distinguish:
- **Architectural context:** complete state associated with a thread identity, including GPRs, instruction address, execution status, privilege/control state, and visible metadata.
- **Workspace state:** shared architectural state and control associated with a workspace.
- **Resident state:** state held in fast physical structures and directly available to execution.
- **Nonresident state:** architectural state retained elsewhere.
- **In-flight state:** transient state required for precise execution while instructions are outstanding.

An implementation may support fewer simultaneously resident contexts than the architectural namespace, but limits and behavior must be discoverable. A reduced physical implementation must preserve the defined semantics of the resources it supports.

## 3. Required observability contract

For a context, privileged software must be able to observe architecturally defined state consistently whether resident or nonresident.

Specify:
- State ownership at each lifecycle stage.
- Inspection and modification of suspended contexts.
- Debug access to nonresident state.
- Interrupt/exception behavior during eviction.
- Identity across eviction and restoration.
- Creation, suspension, migration, and destruction.
- Stale-reference prevention when a slot is reused.
- Precise architectural updates and exception behavior.

## 4. Candidate implementation approaches

| Approach | Advantages | Costs and risks |
|---|---|---|
| Fully resident register state | Low and predictable access latency | Large storage, ports, wiring, and power cost |
| Hardware-managed context cache | Fast active contexts; transparent backing possible | Tags, replacement, dirty tracking, save/restore bandwidth |
| Software-managed save/restore | Simpler hardware; explicit OS control | Context-switch latency and software complexity |
| Hybrid residency | Hardware fast paths with explicit lifecycle control | More complex ownership transitions |

These remain candidates, not decisions.

## 5. Register-file organization questions

A resource study must include:
- N-bit register storage and width-dependent area.
- Read/write port requirements.
- Execution-cluster count.
- Banking and bank conflicts.
- Shared-state replication and consistency.
- Bypass and forwarding networks.
- Wakeup/select logic.
- Context save/restore bandwidth.
- SRAM/BRAM/URAM mapping and timing.
- Area, power, and frequency across several N and context profiles.

A register namespace of 128 entries does not imply that every thread can access all entries in parallel. Architectural capacity is not a physical multiported-register-file requirement.

## 6. Experimental profiles

Existing scheduler simulations include small context profiles and an f128x-oriented 128/256-bit register-width parameter sweep. Those are experimental configurations, not frozen architecture members. Any future f16x/f32x/f64x/f256x study must explicitly account for the selected N, state size, execution width, and physical implementation assumptions.

## 7. Required experiments

1. Estimate register storage and port costs across several N values.
2. Compare banked, replicated, and time-multiplexed access structures.
3. Model contention under representative thread mixes.
4. Estimate save/restore traffic.
5. Evaluate shared-register hotspots and synchronization.
6. Identify state required for precise execution.
7. Define privileged inspection and debug interfaces.
8. Compare physical residency limits and discoverability.

## 8. Open decisions

Complete context format, workspace control state, residency/replacement, hardware/software save/restore, migration, debug semantics, and resource discovery remain open. No spill/fill mechanism is selected.
