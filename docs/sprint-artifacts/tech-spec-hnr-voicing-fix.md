# Tech-Spec: HNR Voicing Detection Fix

**Created:** 2025-12-12
**Status:** Ready for Development
**GitHub Issue:** #15

## Overview

### Problem Statement

The `calculateHNR` function consistently returns `isVoiced: false` and `hnr: 0` even when analyzing audio that contains clear voiced speech. Meanwhile, `analyzeClip` (VoiceAnalyzer) correctly detects voiced frames on the **same audio buffer**.

**User Impact:** The "airiness" perceptual axis always shows maximum breathiness since we fall back to minimum HNR when `isVoiced: false`. This makes breathiness/airiness feedback useless.

### Root Cause Analysis

After investigating the codebase:

1. **The FFI layer is working correctly** - struct layouts match between C header and Rust crate
2. **The Swift/Expo bridge is correct** - `calculateHNRWrapper` properly calls `loqa_calculate_hnr`
3. **The issue is in the `loqa-voice-dsp` crate's HNR algorithm** - it uses different (stricter) voicing criteria than VoiceAnalyzer's pYIN pitch detection

Key evidence:
- `analyzeClip` detects `voicedFrameCount: 19` out of 276 frames
- `calculateHNR` returns `isVoiced: false` on the same audio
- Both use the same buffer, same sample rate

**Likely cause:** The HNR algorithm in loqa-voice-dsp uses a single-shot autocorrelation-based pitch detection that requires stronger periodicity than the frame-by-frame pYIN with HMM smoothing used by VoiceAnalyzer.

### Solution

The fix requires changes in the **loqa-voice-dsp** Rust crate (upstream), not in this repo. However, we can implement a **workaround in loqa-expo-dsp** by:

1. Using VoiceAnalyzer's pitch detection to determine if audio is voiced
2. Only calling `calculateHNR` when we have voiced frames
3. Passing the detected F0 to improve HNR calculation accuracy

**Alternative approach:** Report this bug upstream to `loqa-voice-dsp` maintainers and wait for a fix, while using a fallback strategy in the app layer.

### Scope

**In Scope:**
- Implement workaround using VoiceAnalyzer for voicing detection
- Add optional `f0` parameter to `calculateHNR` to improve accuracy
- Update TypeScript types and documentation

**Out of Scope:**
- Fixing the underlying loqa-voice-dsp crate (different repo)
- Changing the algorithm fundamentally

## Context for Development

### Codebase Patterns

1. **Async function pattern:** All native DSP functions are `AsyncFunction` in Expo module
2. **Wrapper pattern:** Swift `RustBridge.swift` wraps Rust FFI calls with Swift types
3. **Validation pattern:** Input validation in both TypeScript and Swift layers
4. **Error handling:** `RustFFIError` enum with specific error codes

### Files to Reference

| File | Purpose |
|------|---------|
| [src/calculateHNR.ts](src/calculateHNR.ts) | TypeScript wrapper - needs optional f0 param |
| [src/types.ts](src/types.ts) | Type definitions - needs HNROptions update |
| [ios/RustFFI/RustBridge.swift](ios/RustFFI/RustBridge.swift) | Swift FFI wrapper - already supports f0 (internal) |
| [ios/LoqaExpoDspModule.swift](ios/LoqaExpoDspModule.swift) | Expo module - needs f0 option extraction |
| [src/voiceAnalyzer.ts](src/voiceAnalyzer.ts) | VoiceAnalyzer - reference for voicing detection |

### Technical Decisions

1. **Don't modify the FFI layer** - the bug is upstream in loqa-voice-dsp
2. **Use existing VoiceAnalyzer** - it has reliable voicing detection
3. **Add f0 hint parameter** - allows caller to provide detected pitch for better accuracy
4. **Document the limitation** - make it clear HNR voicing detection is unreliable

## Implementation Plan

### Tasks

- [ ] Task 1: Add `f0` optional parameter to `HNROptions` type in `src/types.ts`
- [ ] Task 2: Update `calculateHNR` TypeScript wrapper to pass `f0` option
- [ ] Task 3: Update iOS `LoqaExpoDspModule.swift` to extract `f0` from options (similar to existing pattern)
- [ ] Task 4: Add documentation explaining the voicing detection limitation
- [ ] Task 5: Create helper function `calculateHNRWithVoiceDetection` that:
  - First runs VoiceAnalyzer on buffer
  - If voiced frames detected, extract median F0
  - Call `calculateHNR` with detected F0 as hint
  - Return enhanced result with voicing from VoiceAnalyzer
- [ ] Task 6: Update README with guidance on using the helper function
- [ ] Task 7: File upstream bug report on loqa-voice-dsp GitHub

### Acceptance Criteria

- [ ] AC 1: `calculateHNR` accepts optional `f0` parameter in options
- [ ] AC 2: When `f0` is provided, it's passed through to native module
- [ ] AC 3: New helper function `calculateHNRWithVoiceDetection` correctly identifies voiced speech
- [ ] AC 4: Helper function returns accurate HNR values (15-25 dB) for voiced speech
- [ ] AC 5: Documentation clearly explains the voicing detection limitation and workaround
- [ ] AC 6: Existing tests continue to pass
- [ ] AC 7: TypeScript types are updated and compile without errors

## Additional Context

### Dependencies

- `loqa-voice-dsp` v0.4.1 (Rust crate) - contains the bug
- VoiceAnalyzer API already available in this package

### Testing Strategy

1. **Unit tests:** Test new `f0` parameter flows through correctly
2. **Integration tests:** Test helper function with real audio samples
3. **Manual testing:** Verify on iOS device with voice recordings

### Notes

**Workaround complexity:** The helper function adds complexity but provides immediate value to users. The clean fix would be in the upstream crate.

**Upstream bug characteristics:**
- The `loqa_calculate_hnr` function's internal pitch detection uses stricter criteria
- Works with pure sine waves but fails on real speech
- The autocorrelation-based method may need longer windows or different thresholds for speech

**Performance consideration:** Using VoiceAnalyzer first adds overhead, but it's already needed for many use cases anyway.
