# fNx Conformance and Reference-Model Strategy 0.1

**Date:** 2026-11-06  
**Status:** Proposed verification architecture; no conformance level is ratified

## 1. Purpose

The fNx standard needs executable evidence that independently developed implementations preserve the same architectural and platform semantics.

Conformance must be defined at multiple layers. A CPU can implement the ISA correctly while a platform fails the fabric contract; a fabric can be correct while an OS ABI is incompatible. One undifferentiated "fNx compatible" label would conceal these differences.

This proposal defines conformance domains, evidence requirements, test organization, and the relationship between normative specifications and executable models.

## 2. Conformance domains

### 2.1 ISA conformance

Covers:
- Instruction decoding and boundaries.
- Opcode identity and semantics.
- Operand interpretation and native-width behavior.
- Arithmetic, flags, and sub-width operations.
- Control flow and privilege checks.
- Synchronous exceptions and precise state.
- Architectural state visibility and reset behavior.
- Memory operations, atomics, and ordering.

ISA conformance is specific to a declared fNx member width N, ISA revision, and implemented capability set.

### 2.2 Execution-context conformance

Covers:
- Thread-private architectural state.
- Workspace-shared state.
- Identity and stale-reference prevention.
- Lifecycle transitions.
- Context save/restore and residency equivalence.
- Migration, if supported.
- Interrupt delivery to blocked and nonresident contexts.
- Workspace isolation and debug authority.

A profile may expose fewer contexts or register resources than another implementation, but must report limits and preserve all semantics for admitted resources.

### 2.3 Memory-system conformance

Covers:
- N-bit virtual-address behavior.
- Translation and physical-address limits.
- Protection and access faults.
- Address-space isolation and invalidation.
- Alignment and split-access behavior.
- Memory attributes.
- Atomic operations and ordering.
- DMA and device visibility.
- Coherence or explicit ownership rules.

### 2.4 Fabric/platform conformance

Covers:
- Endpoint identity and discovery.
- Width-neutral transactions.
- Request acceptance and completion semantics.
- Ordering across links and bridges.
- Atomic capability negotiation.
- Flow control and deadlock requirements.
- Error reporting, integrity, and retry safety.
- Hierarchical routing and fault containment.
- FCP control-plane authority and management behavior.

### 2.5 Software-interface conformance

Covers:
- ABI and calling convention.
- Object format and relocations.
- System-call/trap ABI.
- P-Code semantics and versioning.
- .fnx package validation and metadata.
- Capability discovery and installer/AOT behavior.
- Runtime and native-code cache compatibility.

These domains may be certified independently. A claim of full platform conformance must identify all required domains and versions.

## 3. Declared implementation profile

Every conformance report must identify:
- fNx member and native width N.
- ISA revision.
- Supported opcode and capability set.
- Physical-address width and memory capacity.
- Register classes and counts.
- Workspace/thread limits.
- Shared-register operations and atomicity guarantees.
- Translation and memory-model features.
- Exception, interrupt, and reset behavior.
- Fabric protocol revision and endpoint classes.
- Optional FCP and management capabilities.
- ABI/P-Code/package revisions, if claimed.
- Known limitations and unsupported features.

The profile must distinguish:
- **Architectural namespace:** identifiers and capacities defined by the standard.
- **Implemented capability:** operations and limits the implementation actually supports.
- **Current resource availability:** resources available at runtime.

These are different facts. A temporary shortage of resident contexts must not be reported as an ISA capability change.

## 4. Normative source and test traceability

Every normative requirement should have:

The working cross-document mapping is maintained in [M1 requirements traceability matrix](m1-requirements-traceability-matrix-0.1.md). Its IDs are provisional traceability identifiers, not a claim that the linked proposal text has been ratified.
1. A stable requirement identifier.
2. A specification section and revision.
3. A classification: mandatory, optional capability, or implementation-defined with required discovery.
4. One or more positive tests.
5. Negative tests for prohibited behavior.
6. A defined expected result or permitted outcome set.
7. A test status and implementation evidence reference.

A test must not become the only definition of architectural behavior. The normative text is authoritative; executable tests demonstrate conformance to it.

Tests should be tagged by requirement ID, member width, capability, privilege level, and relevant platform profile.

## 5. Reference artifacts

### 5.1 ISA semantic model

A small executable model that:
- Decodes the selected instruction format.
- Executes specified architectural operations.
- Models register and control state.
- Produces defined exceptions and state transitions.
- Serves as a golden oracle for implementations.

The model must not encode implementation-specific scheduler or datapath assumptions.

### 5.2 Opcode registry

A machine-readable registry with:
- Stable numeric identifier.
- Canonical mnemonic.
- Semantic specification reference.
- Operand/result schema.
- Required capabilities and privilege.
- Exception and side-effect references.
- Memory-ordering classification.
- Minimum N for direct representation, if wider-only identifiers are permitted.
- Portability class.
- Lifecycle state and ISA revision.
- Exact expansion reference, where one exists.

Registry validation must reject duplicate identifiers, duplicate canonical names where prohibited, missing semantic references, invalid lifecycle transitions, and inconsistent width/capability metadata.

No opcode values should be allocated until the registry schema and allocation policy are ratified.

### 5.3 Assembler/disassembler

The assembler and disassembler must share the registry and encoding definition. They must reject:
- Out-of-range opcode identifiers for the selected N.
- Unsupported operands or malformed encodings.
- Reserved fields with prohibited values.
- Invalid instruction boundaries.
- Width-dependent opcodes not representable by the selected member.

Round-trip tests should verify decode/encode stability for all standardized instructions.

### 5.4 Golden vectors and litmus tests

Golden vectors cover deterministic instruction semantics and exception outcomes.

Memory-model litmus tests cover permitted and forbidden observations across threads, workspaces, CPU units, and DMA agents.

Fabric tests cover request/response ordering, atomicity, error, retry, and hierarchy properties.

### 5.5 Platform reference model

A platform model should represent:
- Multiple CPU units with different N values.
- Memory and I/O endpoints.
- Address translation and protection.
- Width-neutral fabric transactions.
- Endpoint discovery and capability negotiation.
- Interrupt and fault delivery.
- Reset and partial hierarchy failure.

The model should be parameterized by explicit implementation profiles rather than embedding the 128-bit research profile as a universal assumption.

## 6. Test classes

| Test class | Purpose |
|---|---|
| Positive functional | Required behavior succeeds |
| Negative functional | Invalid/unsupported behavior fails as specified |
| Boundary | Minimum/maximum widths, lengths, identifiers, and resource limits |
| State-transition | Lifecycle, reset, exception, and migration transitions |
| Concurrency | Races, atomicity, ordering, and visibility |
| Fault injection | Link loss, endpoint failure, corruption, timeout, and reset |
| Differential | Implementation result compared with reference model |
| Property-based | Broad generated input/state coverage |
| Formal property | Invariants such as no stale-ID aliasing or duplicate side effects |
| Performance characterization | Measures behavior but does not substitute for functional conformance |

Performance tests must not be used to weaken functional semantics. Performance portability is reported separately.

## 7. Width and profile matrix

The test strategy must cover all eventually supported N values, with emphasis on boundary and scale effects.

Before the supported N range is selected, the research suite should use representative values such as:
- A candidate smallest member.
- f16x and f32x.
- f64x.
- The 128-bit research member.
- f256x and f512x.
- f1024x as a large-width stress case.

These are test points, not a declaration that all are supported members.

Cross-width tests must include at least one pair with different native widths and must verify identical abstract fabric semantics, shared object representation, and explicit capability negotiation.

## 8. Security and isolation verification

Security-relevant conformance must test:
- Cross-workspace register and memory isolation.
- Forged/stale workspace and thread identities.
- Unauthorized debug inspection or modification.
- Privilege escalation through traps or interrupt routing.
- Address-space identifier reuse.
- DMA access outside authorized domains.
- Fabric management-command forgery.
- FCP/host authority conflicts.
- Teardown and migration races.
- Information leakage through fault and capability reporting where the standard defines constraints.

Security tests should include negative tests and adversarial scheduling/interleaving, not only normal execution paths.

## 9. Release and compatibility claims

A conformance report must state the exact revisions and domains tested. Claims should use precise labels, for example:
- ISA-conformant for member N and ISA revision R.
- Execution-context-conformant for profile P.
- Memory-model-conformant for model M.
- Fabric-conformant for protocol revision F.
- P-Code runtime-conformant for P-Code revision V.
- Full platform conformance for a specified combination of all required domains.

A newer ISA revision may preserve compatibility while adding optional instructions. The implementation must report its supported capability set; it must not claim support for an operation merely because its opcode identifier is representable.

## 10. CI and evidence policy

The project CI should:
- Validate Markdown links and specification structure.
- Validate machine-readable registry schema and uniqueness when introduced.
- Build and run the reference model.
- Run deterministic golden vectors.
- Run memory-model litmus tests.
- Run fabric protocol tests.
- Run generated/property tests with reproducible seeds.
- Publish test results and artifacts by revision.
- Track unimplemented requirements explicitly.

Tests that require unavailable hardware should be labeled as hardware tests and must not be silently counted as passed by a simulation-only run.

A failed or skipped test must remain visible in the conformance report.

## 11. Decision status

| Topic | Status |
|---|---|
| Layered conformance domains | Proposed |
| Member/profile declaration | Required working direction |
| Requirement-to-test traceability | Required |
| Machine-readable opcode registry | Required; schema not selected |
| ISA semantic reference model | Required; not implemented |
| Golden vectors and memory litmus tests | Required; not implemented |
| Fabric/platform reference model | Required; existing research simulation is not yet a conformance model |
| Cross-width conformance | Required |
| Security/isolation test domain | Required |
| Formal verification | Recommended for identity, transaction, ordering, and safety invariants |
| Conformance labels and release policy | Open |

## 12. Related specifications

- [fNx family and architectural contract](fnx-family-and-architectural-contract-0.1.md)
- [fNx opcode namespace and allocation proposal](opcode-namespace-and-allocation-proposal-0.1.md)
- [fNx instruction encoding feasibility study](instruction-encoding-feasibility-0.1.md)
- [fNx address-space and memory-model proposal](address-space-and-memory-model-proposal-0.1.md)
- [fNx exception and interrupt model proposal](exception-and-interrupt-model-proposal-0.1.md)
- [fNx workspace and thread lifecycle contract proposal](workspace-and-thread-lifecycle-contract-proposal-0.1.md)
- [fNx width-neutral fabric and backplane contract proposal](width-neutral-fabric-and-backplane-contract-proposal-0.1.md)
- [M1 requirements traceability matrix](m1-requirements-traceability-matrix-0.1.md)
- [fNx roadmap](../roadmap.md)
