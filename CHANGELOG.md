# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2025-11-21

### Added

- **Core DSP Functions**: Production-grade audio DSP analysis capabilities

  - `computeFFT()`: Fast Fourier Transform with configurable window functions (Hanning, Hamming, Blackman)
  - `detectPitch()`: YIN algorithm-based pitch detection with confidence scoring
  - `extractFormants()`: LPC-based formant extraction (F1, F2, F3 with bandwidths)
  - `analyzeSpectrum()`: Spectral analysis (centroid, rolloff, tilt)

- **Cross-Platform Support**: Native iOS and Android implementations

  - iOS 15.1+ with Swift FFI bindings to Rust DSP core
  - Android API 24+ with Kotlin JNI bindings to Rust DSP core
  - Identical API and behavior across platforms

- **TypeScript API**: Fully typed TypeScript interface

  - Complete type definitions (.d.ts) for all functions and types
  - Input validation with clear error messages
  - Promise-based async API for non-blocking operation

- **Performance**: Optimized for real-time audio processing

  - Sub-5ms processing latency for 2048-sample buffers
  - Battle-tested Rust loqa-voice-dsp crate for core algorithms
  - Memory-safe FFI/JNI boundaries

- **Example Application**: Comprehensive demo app showcasing all features

  - Real-time FFT visualization
  - Pitch detection tuner interface
  - Formant analysis with vowel chart
  - Spectral analysis visualization
  - Performance benchmarking tools

- **Documentation**: Complete developer resources

  - README.md with quick start guide
  - API.md with detailed function reference
  - INTEGRATION_GUIDE.md with common patterns and best practices
  - Example code for all DSP functions

- **Build & CI/CD**: Automated quality assurance
  - GitHub Actions CI pipeline (lint, typecheck, test, audit)
  - Automated npm publishing on version tags
  - TypeScript strict mode compilation
  - Jest unit tests for TypeScript layer
  - XCTest (iOS) and JUnit (Android) tests for native code

### Technical Details

- Package name: `@loqalabs/loqa-audio-dsp`
- Expo SDK: 54.0+
- React Native: 0.76+
- License: MIT
- Repository: https://github.com/loqalabs/loqa-audio-dsp

---

[0.1.0]: https://github.com/loqalabs/loqa-audio-dsp/releases/tag/v0.1.0
