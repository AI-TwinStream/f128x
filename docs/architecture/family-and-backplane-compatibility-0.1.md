# f128x Architecture Family and Mixed-Width Backplane Compatibility 0.1

**Date:** 2026-11-04  
**Status:** Architectural direction for specification; electrical and protocol details remain open

## 1. Purpose

Define how the f128x family grows across power-of-two datapath profiles while retaining one coherent architecture, and establish the requirement that unlike-width processors can coexist in one backplane system.

The family is intended to scale without creating a new, unrelated ISA or interconnect for each width.

## 2. Architecture family

The family names are:

| Profile | Native architectural register width |
|---|---:|
| `f128x` | 128 bits |
| `f256x` | 256 bits |
| `f512x` | 512 bits |
| `f1024x` | 1,024 bits |

These are width profiles of one architecture family. They are not four independently designed ISAs.

### 2.1 One ISA across profiles

The family should retain:

- One opcode namespace and instruction model.
- The same instruction meanings and control-flow semantics.
- The same architectural register naming model.
- The same workspace, thread, privilege, and protection concepts.
- The same memory and backplane programming model.
- A common ABI foundation and explicit profile rules.

An operation such as `ADD R1, R2, R3` has the same architectural meaning in every profile. Its native operand width is determined by the selected profile. The profile changes the width of the architectural datapath/register values, not the conceptual operation.

Profile-specific implementation capabilities may differ, but they must not silently redefine an instruction. Optional capabilities must be discoverable and architecturally specified.

### 2.2 Register count is independent of register width

Do not conflate:
1. Register width: the number of bits stored in each register.
2. Register count: the number of architecturally selectable registers.
3. Selector width: the number of bits required to identify one register.

For a register bank containing N entries, the minimum selector width is:

`selector_bits = ceil(log2(N))`

| Register entries | Selector bits |
|---:|---:|
| 128 | 7 |
| 256 | 8 |
| 512 | 9 |
| 1,024 | 10 |

Doubling the register count adds one selector bit. It does not double the selector width. A 7-bit selector identifies 128 entries; a 14-bit selector identifies 16,384 entries.

If the family retains 128 private registers per thread and 128 shared registers per workspace, their selectors remain 7 bits in every width profile. Increasing each register from 128 to 256, 512, or 1,024 bits does not change the selector width.

The register-count policy is separate from the width-profile policy and must be stated explicitly in the ISA specification. This document does not require register counts to grow with datapath width.

### 2.3 Sub-width operations

The ISA should support operations on smaller portions of a native register, including 8-, 16-, 32-, and 64-bit values, while preserving consistent semantics across profiles.

The specification must define:
- Lane and subregister selection.
- Signed and unsigned interpretation.
- Sign/zero extension and upper-bit behavior.
- Scalar versus packed/vector operations.
- Predicate and mask behavior.
- Carry, overflow, saturation, and exception semantics.

These are semantic rules, not reasons to create separate ISAs.

### 2.4 Pointer and address model

The current architectural direction uses 128-bit pointers across the family. Register width must not automatically change pointer representation or virtual-address width.

The ISA and ABI must define pointer representation, virtual and physical address widths, translation, canonicality if applicable, protection, and load/store behavior independently of datapath width.

## 3. Mixed-width systems are a backplane requirement

A system backplane must permit processors from different f128x width profiles to coexist.

Examples include:
- An `f128x` processor board operating alongside an `f256x` board.
- An `f128x` processor board operating alongside an `f512x` board.
- Any supported combination of `f128x`, `f256x`, `f512x`, and `f1024x` endpoints.

The backplane must not be defined as a direct extension of one processor's native register or execution width. Otherwise, every width increase would force a new backplane generation and undermine family interoperability.

### 3.1 Width-neutral interconnect contract

The backplane protocol should define transfers in terms of a stable, width-neutral contract rather than requiring every endpoint to share the same native datapath width.

Candidate protocol concepts:
- Byte-addressed transactions.
- Explicit transfer length and transaction type.
- A stable minimum transfer unit.
- Negotiated maximum payload or burst size per endpoint/link.
- Request and response semantics independent of endpoint datapath width.
- Explicit ordering, completion, and error reporting.
- Flow control and backpressure.
- Capability discovery and link training.
- Address-space and access-protection attributes.
- Defined atomic-operation capabilities and alignment rules.

The physical link may serialize or aggregate data as required. A 512-bit processor can issue a wide local operation while the interconnect carries one or more protocol-defined transfer units. A 128-bit endpoint need not grow its datapath merely to communicate with that processor.

No physical lane width, signaling rate, encoding, topology, or fixed bus width is selected here.

### 3.2 Endpoint adaptation

Each processor or I/O endpoint adapts its native operations to the common interconnect contract.

For example, a 512-bit local load/store path may be wider than the negotiated transfer payload supported by a particular link. The endpoint adapter can split the operation into multiple protocol transactions where the architectural memory model permits it, then reassemble the response.

Splitting must not invent atomicity. If an operation requires atomic completion, the protocol must provide a supported atomic transaction or the architecture must define a safe alternative. A multi-transaction transfer is not automatically equivalent to one atomic wide access.

### 3.3 Shared memory and data exchange

Mixed-width processors can exchange data through common memory and messaging interfaces, but interoperability requires explicit rules for:
- Byte order and byte addressing.
- Fixed-width data types and alignment.
- Atomic access sizes supported by each endpoint.
- Memory ordering and visibility.
- Cache coherence or another defined sharing mechanism.
- DMA and I/O consistency.
- Error handling for unsupported transaction sizes or capabilities.

The ABI and shared-memory formats should use explicit, profile-neutral layouts for data exchanged between unlike-width processors. Native register width must not silently alter the layout of shared structures.

The 128-bit pointer direction should remain consistent across profiles. A pointer must not become 256 or 512 bits merely because a processor has wider registers.

## 4. Compatibility and capability negotiation

At link initialization, endpoints should discover and negotiate protocol capabilities, including supported transaction sizes, atomics, ordering features, address attributes, and optional extensions.

The common protocol baseline must be implementable by every supported width profile. Wider endpoints may support additional local operations or higher throughput, but they must fall back to the common contract when communicating with narrower endpoints.

Compatibility has several layers:

1. **ISA compatibility:** the same architectural instruction model and semantics across profiles.
2. **Binary/ABI compatibility:** a defined common ABI subset and explicit rules for profile-specific code and data layouts. Identical ISA semantics do not by themselves guarantee that arbitrary binaries can be moved between profiles.
3. **Memory interoperability:** shared data layouts, address translation, ordering, and atomicity are defined.
4. **Interconnect compatibility:** all endpoints implement the common backplane protocol baseline.
5. **Performance portability:** not guaranteed. A narrower endpoint may service a wider endpoint's request in multiple transfers and at lower throughput.

## 5. Design constraints and unresolved work

The following must be specified before claiming implementation-level interoperability:

- Common backplane transaction format and versioning.
- Minimum and maximum transfer sizes.
- Burst splitting, reassembly, alignment, and partial-write behavior.
- Atomic operation negotiation and unsupported-atomic behavior.
- Ordering domains, fences, and completion rules.
- Cache coherence or explicit non-coherent memory ownership.
- Address-space discovery, routing, and protection.
- Link-level flow control, retry, CRC/ECC, and fault recovery.
- Capability negotiation and backward/forward compatibility.
- Interrupt and message delivery between unlike-width endpoints.
- Reset, hot-plug, and endpoint failure behavior.
- Performance and fairness under mixed-width traffic.

The existing backplane/fabric simulations are simplified research models. They do not establish this protocol, physical signaling, coherence, or real hardware performance.

## 6. Architectural direction

The intended design is one f128x architecture family with power-of-two width profiles and a common, width-neutral system interconnect.

The key separation is:

- **ISA:** stable across profiles.
- **Native register/datapath width:** selected by profile.
- **Register count and selector width:** independently specified.
- **Pointer representation:** 128 bits in the current direction, independent of profile width.
- **Backplane protocol:** common across profiles, independent of native datapath width.
- **Physical implementation and performance:** may vary by profile and product.

This provides a path for mixed systems such as f128x + f256x and f128x + f512x without multiplying ISA and backplane designs.
