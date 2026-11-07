# fNx Opcode Namespace and Allocation Proposal 0.1

**Date:** 2026-11-06  
**Status:** Design proposal; no opcode values or instruction encodings are ratified

## 1. Purpose

This proposal defines the questions and constraints for one stable opcode namespace shared by every fNx member. It deliberately does not assign operation numbers or select a complete instruction encoding.

The goals are:

- One numeric opcode identifies one operation throughout the fNx family.
- Existing standardized opcode assignments and semantics are immutable.
- New operations are added without changing the meaning of existing programs.
- An implementation can report which standardized operations it supports.
- An unsupported operation cannot silently execute as a different operation.
- Opcode allocation remains independent of instruction length and operand encoding.

## 2. Existing working direction

The family contract currently favors an opcode field whose width equals the member's native architectural width N. It also calls for one finite global registry, with the same numeric assignment used by every member.

These are the same rule, applied consistently: the opcode field is N bits. The minimum and maximum supported N values remain open, but no member receives a special fixed-width opcode representation merely to enlarge its namespace.

## 3. The minimum-width constraint

If the opcode field is exactly N bits, the representable numeric range is:

| Member width | Direct opcode values |
|---:|---:|
| 8 | 0–255 |
| 16 | 0–65,535 |
| 32 | 0–4,294,967,295 |
| 64 | 0–18,446,744,073,709,551,615 |

A single global registry does not require every member to implement or directly encode every operation. Each member's N-bit opcode field can represent identifiers in the range shown above. Consequently, a member with a smaller N has a smaller directly encodable portion of the global namespace. For example, f8x can directly encode identifiers 0–255; f16x can directly encode identifiers 0–65,535. Wider members can directly encode larger identifiers.

This is an intentional consequence of the N-bit rule, not a reason to introduce a fixed-width opcode field or an escape mechanism. The registry remains global: a numeric identifier has one meaning wherever it is representable. Operations outside a member's representable range are unavailable for direct native encoding on that member. Portable software may use an exact compiler/runtime expansion when one exists. The ISA capability model must make these limits discoverable.

The architecture must define how allocation accounts for the representable ranges of supported members. It must not imply that every member can directly encode every registry entry.

## 4. Namespace model

The registry should be treated as a single, centrally maintained numeric namespace, not as separate per-width opcode tables.

Each standardized entry must have at least:

- A stable numeric opcode identifier.
- A canonical mnemonic.
- A concise operation name and semantic definition reference.
- A lifecycle state: proposed, reserved, standardized, or withdrawn.
- The ISA revision in which the assignment became standardized.
- Operand and result class references, once those classes are defined.
- Required architectural capabilities, if any.
- Exception, privilege, memory-ordering, and side-effect references, as applicable.
- Compatibility classification: expandable, conditionally expandable, or non-expandable.
- A canonical exact expansion reference when expansion is possible.

Numeric values must be globally unique. Mnemonics should also be unique unless an explicit alias relationship is documented. Aliases must never create a second numeric identity for the same standardized operation.

## 5. Allocation and immutability rules

The following rules are proposed:

1. A numeric value may be assigned to at most one standardized operation.
2. Once standardized, the numeric value and operation semantics are immutable.
3. A standardized value is never recycled, including after an operation is deprecated or withdrawn.
4. Proposed and reserved values are not executable instructions.
5. A proposal does not acquire an opcode merely because it appears in a draft or implementation.
6. Allocation must be reviewed against the machine-readable registry before acceptance.
7. Registry changes must include semantic documentation and conformance evidence, not just a mnemonic and number.
8. Width-specific implementation support does not create a width-specific meaning or alternate numeric assignment.
9. Vendor or experimental extensions must not collide with standardized assignments. Their allocation and discoverability mechanism remains an open decision.
10. An implementation must identify its supported ISA revision and operation capabilities through a defined discovery contract.

The registry should distinguish an unassigned value from an assigned operation that is unsupported by a particular implementation. Both must fail safely, but they are different architectural conditions.

## 6. Decode and unsupported-operation behavior

The architecture must distinguish at least:

- **Unassigned opcode:** no standardized operation is associated with the decoded value.
- **Assigned but unsupported operation:** the value is standardized, but the executing implementation does not provide the required capability.
- **Malformed instruction:** the opcode may be known, but the complete instruction encoding violates the selected format.
- **Privilege violation:** the operation exists but is not permitted in the current execution context.

Each condition requires a specified architectural exception or trap outcome, including restart behavior and fault attribution. The exact exception model is not selected here.

No condition may silently alias to another operation, become a no-op, or be approximated by hardware without an explicit architectural rule.

Portable-code compilation may replace an operation with an exact older-member sequence when the semantics are fully reproducible. That compiler/runtime compatibility path does not change the opcode assignment.

## 7. Opcode identity is not instruction encoding

This proposal does not define:

- Instruction length or fetch alignment.
- Whether the opcode occupies the first bits/bytes of an instruction.
- Register selector widths or register numbering.
- Operand ordering or operand count.
- Immediate and displacement representation.
- Extension words, prefixes, or escape encodings.
- Variable-length versus fixed-length instruction formats.
- Endianness of instruction storage.
- Reserved bit behavior within an instruction.
- Decoder behavior for truncated or cross-boundary instructions.

These decisions must be made together in the encoding feasibility study. In particular, the N-bit opcode-field direction must not be interpreted as a requirement that every instruction or operand be N bits long.

## 8. ISA revision and capability discovery

A numeric opcode assignment is permanent, but an implementation may support only a subset of the standardized registry.

The ISA revision/capability contract must answer:

- How a platform reports its supported base ISA revision.
- How optional or later-added operations are enumerated.
- Whether capabilities are reported per CPU unit, per workspace, or per execution context.
- How software handles heterogeneous CPU units with different operation sets.
- How the installer/compiler selects exact expansions.
- How a future incompatible architectural change would be represented without reinterpreting an existing opcode.

A single monotonically increasing revision number may be insufficient to describe independently optional capabilities. A capability bitmap or structured capability set should be evaluated, but is not selected here.

## 9. Required decision record

Before the registry is populated, the project should decide:

1. Supported minimum and maximum N values.
2. Whether every supported member follows the N-bit opcode-field rule (the current direction).
3. Allocation policy for identifiers that are representable only by wider members.
4. Reserved ranges, if any, and who may allocate them.
5. Experimental/vendor extension policy.
6. Unsupported-operation and malformed-instruction trap semantics, including out-of-range identifiers.
7. ISA revision and capability discovery model.
8. Whether standardized entries may ever be marked withdrawn, and the exact meaning of withdrawal.
9. Required review and conformance evidence before an allocation becomes standardized.

## 10. Recommended next work

Do not assign real opcode numbers yet.

The next design artifact should compare representative encoding candidates across the supported N range, including the smallest proposed member and the 128-bit member. The study must account for the smaller directly representable opcode namespace at smaller N values. The comparison should measure:

- Decode unambiguity and instruction-boundary recovery.
- Common operation encoding cost.
- Register selector and operand-field cost.
- Immediate and displacement reach.
- Extension/prefix overhead.
- Code density for representative instruction mixes.
- Fetch and decode complexity.
- Exact compatibility and disassembly behavior.

After the minimum-width and encoding direction are selected, establish a machine-readable registry and validate uniqueness, lifecycle state, and revision metadata automatically.

## 11. Decision status

| Topic | Proposal status |
|---|---|
| One global numeric namespace | Existing working direction; reaffirmed here |
| Stable numeric identity across members | Required by the family model |
| Immutable standardized assignments and semantics | Proposed allocation rule |
| No value recycling | Proposed allocation rule |
| N-bit opcode field | Selected working architectural direction |
| Minimum and maximum supported N | Open |
| Smaller-member opcode range | 0 through 2^N−1; intentional consequence of N |
| Escape/prefix mechanism | Not part of the current direction |
| Fixed-width opcode representation | Not part of the current direction |
| Instruction length and operand formats | Open |
| Reserved/vendor ranges | Open |
| Trap semantics | Open |
| ISA revision/capability discovery | Open |
| Numeric opcode assignments | None made by this proposal |

## 12. Related specifications

- [fNx family and architectural contract](fnx-family-and-architectural-contract-0.1.md)
- [fNx ISA, ABI, and toolchain plan](isa-abi-and-toolchain-plan-0.2.md)
- [fNx architecture overview](overview-0.1.md)
- [fNx roadmap](../roadmap.md)
