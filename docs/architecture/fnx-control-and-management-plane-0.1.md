# fNx Control Processor and Management Plane 0.1

**Date:** 2026-11-05  
**Status:** Proposed platform architecture; hardware and protocol details not frozen

## 1. Purpose

An fNx platform needs a management authority that can discover, initialize, monitor, and recover processor units, boards, backplanes, memory endpoints, and I/O devices.

The proposed **fNx Control Processor (FCP)** is an auxiliary processor that provides this capability. It is a platform control-plane component, not a mandatory serial execution stage for application instructions.

## 2. ISA relationship

The FCP should use the fNx ISA family and instruction semantics rather than introducing an unrelated proprietary instruction language.

The FCP may be a smaller, low-power fNx member than the main processor unit. A main CPU unit has one native width; the FCP is a separate CPU unit with its own single native width. The family minimum width remains an open decision, with f16x currently favored provisionally.

A controller of one width can manage a processor or backplane of another width because management messages and platform contracts are width-neutral. It does not execute instructions on behalf of the managed processor and does not reinterpret the managed processor's native opcodes.

## 3. Separation of planes

The platform should separate three logical planes:

### Data plane
- Main CPU instruction execution.
- Loads, stores, atomics, and normal application traffic.
- Direct execution on the main CPU's native datapath.
- No routine FCP hop for each instruction.

### Control plane
- Processor and endpoint discovery.
- Capability and ISA-revision inventory.
- Reset, start, stop, and recovery coordination.
- Clock and power sequencing.
- Board and fabric configuration.
- Fault isolation and recovery orchestration.
- Firmware lifecycle and boot coordination.

### Management/telemetry plane
- Health and status reporting.
- Temperature, voltage, power, and thermal monitoring.
- Error counters and fault logs.
- Inventory and topology reporting.
- Event notification and audit information.

These planes may share physical links or hardware blocks, but their interfaces and failure behavior must be specified independently.

## 4. Independent operating capability

The FCP should have sufficiently independent power, clock, reset, and connectivity to manage a board when its main CPUs are stopped, failed, or not yet initialized.

The platform contract must define:
- FCP boot and firmware trust.
- Independent reset and watchdog behavior.
- Management access when main CPU power is absent or unstable.
- Secure update and rollback.
- Redundant FCP behavior, if required.
- Failure reporting when the FCP itself is unavailable.
- Arbitration and ownership when both host software and FCP request a control action.

The exact electrical implementation is open. Independence is a functional requirement, not a commitment to a separate power supply in every product.

## 5. Capability discovery

The FCP should maintain or expose a structured inventory of:
- fNx member and native width.
- Supported ISA revision and opcode/capability set.
- Optional execution features.
- Physical-address and memory-capacity limits.
- Memory and I/O endpoints.
- Fabric links, topology, and negotiated transfer capabilities.
- Atomic, ordering, and coherence capabilities.
- Firmware and platform-contract versions.
- Health, fault, and recovery state.

The operating system, installer, runtime, and system software consume this information through stable interfaces. The FCP reports capabilities; it does not decide that absent hardware features magically exist.

## 6. Compatibility responsibilities

Application compatibility should primarily be handled by the P-Code compiler, installer/AOT compiler, runtime, and OS.

The FCP may assist by reporting exact capabilities and exposing platform services. It is not the application compiler and should not perform instruction-by-instruction translation for normal execution.

Legacy device, fabric, memory-controller, and management-protocol adaptation may be implemented by dedicated platform adapters or firmware services. Such adaptation must preserve defined ordering, atomicity, privilege, and error semantics.

An older processor can support a newer application only when required instruction behavior can be expanded and all non-ISA capabilities are available. The FCP cannot create unavailable physical resources or faithfully emulate inherently non-expandable operations.

## 7. Hierarchical backplanes

The platform should support backplanes connected to other backplanes, forming a hierarchy.

Each level needs:
- Stable endpoint identity and discovery.
- Width-neutral management and data contracts.
- Address and routing domains.
- Capability aggregation and propagation.
- Fault containment and isolation.
- Reset and power-domain boundaries.
- Link health and error reporting.
- Defined behavior during partial hierarchy failure.

A parent backplane or management controller should not need to share the native width of every subordinate CPU unit. It interacts through the common contract.

## 8. Security and authority

Management operations can affect the entire platform. The standard must define authenticated authority, privilege separation, firmware trust, auditability, and recovery from compromised or unavailable components.

The FCP must not become an undocumented bypass around CPU privilege, memory protection, or device access controls. Host OS management interfaces and FCP firmware interfaces need a clearly specified authority model.

## 9. Open decisions

- FCP native width and minimum family member.
- FCP count, redundancy, and placement.
- Management transport and message formats.
- Boot, firmware, secure update, and recovery model.
- Host/FCP command ownership and arbitration.
- Telemetry schema and fault taxonomy.
- Hierarchical addressing and routing.
- Trust roots and authenticated management operations.
- Which functions require hardware versus firmware/software.

## 10. Related specifications

- [fNx family and architectural contract](fnx-family-and-architectural-contract-0.1.md)
- [fNx P-Code and application compatibility](fnx-pcode-and-compatibility-0.1.md)
- [Physical system block diagram](physical-system-block-diagram-0.1.md)
