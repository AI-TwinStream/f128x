# f128x

**f128x** is a 128-bit processor architecture and implementation project. Its first hardware exploration target is a full CPU replacement for the Texas Instruments TI-99/4A, preserving the machine's useful peripherals and software experience while replacing the original 16-bit execution core.

## Project direction

The goal is not to attach a 128-bit arithmetic coprocessor to the original CPU. The goal is to develop a native 128-bit processor: 128-bit general-purpose registers, native 128-bit integer operations, a defined instruction set, and a deliberately specified memory/addressing model.

The TI-99/4A is the initial platform target. The design should retain the original video, sound, keyboard, and peripheral subsystems through explicit compatibility interfaces rather than pretending those devices are natively 128-bit.

## Architecture principles

- Define register width, integer operation width, address width, and external bus width independently.
- Make 128-bit general-purpose registers and scalar integer operations architectural fundamentals.
- Define a clean, extensible ISA rather than attempting binary compatibility with TMS9900 machine code.
- Provide a compatibility boundary for existing TI-99/4A peripherals and, where practical, legacy software services.
- Prototype the CPU and platform interfaces in FPGA before committing to custom silicon or a replacement motherboard.
- Keep the architecture portable beyond the TI-99/4A.

## Status

Early architecture definition. No ISA, ABI, memory model, FPGA target, or board-level interface is frozen yet.

## Documents

- [Architecture overview](docs/architecture/overview.md)
- [TI-99/4A platform strategy](docs/platforms/ti-99-4a.md)
- [Initial roadmap](docs/roadmap.md)
