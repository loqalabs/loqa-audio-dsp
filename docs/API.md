# API Reference

**@loqalabs/loqa-expo-dsp**
Version: 0.1.0

This document provides complete API reference for all functions, types, and options in the @loqalabs/loqa-expo-dsp module.

---

## Table of Contents

- [Functions](#functions)
  - [computeFFT](#computefft)
  - [detectPitch](#detectpitch)
  - [extractFormants](#extractformants)
  - [analyzeSpectrum](#analyzespectrum)
- [Types](#types)
  - [FFTOptions](#fftoptions)
  - [FFTResult](#fftresult)
  - [PitchDetectionOptions](#pitchdetectionoptions)
  - [PitchResult](#pitchresult)
  - [FormantExtractionOptions](#formantextractionoptions)
  - [FormantsResult](#formantsresult)
  - [SpectrumAnalysisOptions](#spectrumanalysisoptions)
  - [SpectrumResult](#spectrumresult)
- [Error Handling](#error-handling)
  - [LoqaExpoDspError](#loqaexpodsperror)
  - [ValidationError](#validationerror)
  - [NativeModuleError](#nativemoduleerror)
- [Validation Rules](#validation-rules)

---

## Functions

### computeFFT

Computes Fast Fourier Transform (FFT) of audio buffer.

This function performs frequency analysis on audio data using the FFT algorithm. It accepts audio buffers as Float32Array or number[], validates the input, and returns magnitude and frequency information.

```typescript
async function computeFFT(
  audioBuffer: Float32Array | number[],
  options?: FFTOptions
): Promise<FFTResult>;
```

#### Parameters

| Parameter     | Type                       | Required | Description                                                                                                                         |
| ------------- | -------------------------- | -------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| `audioBuffer` | `Float32Array \| number[]` | Yes      | Audio samples to analyze. Must be non-empty and contain no more than 16384 samples. All values must be finite (no NaN or Infinity). |
| `options`     | `FFTOptions`               | No       | Configuration options for FFT computation. See [FFTOptions](#fftoptions).                                                           |

#### Returns

`Promise<FFTResult>` - Resolves to an [FFTResult](#fftresult) containing magnitude spectrum, frequency bins, and optional phase information.

#### Throws

- `ValidationError` - If buffer is null, empty, too large, contains invalid values, or if FFT size is invalid
- `NativeModuleError` - If native computation fails

#### Example

```typescript
import { computeFFT } from '@loqalabs/loqa-expo-dsp';

// Basic usage with defaults
const audioData = new Float32Array(1024);
// ... fill with audio samples ...

const result = await computeFFT(audioData);
console.log('Magnitude:', result.magnitude);
console.log('Frequencies:', result.frequencies);

// Advanced usage with options
const result2 = await computeFFT(audioData, {
  fftSize: 2048, // Use 2048-point FFT
  windowType: 'hanning', // Apply Hanning window
  includePhase: false, // Omit phase for performance
});

// Find peak frequency
const peakIndex = result2.magnitude.indexOf(Math.max(...result2.magnitude));
const peakFrequency = result2.frequencies[peakIndex];
console.log(`Peak at ${peakFrequency} Hz`);
```

---

### detectPitch

Detects pitch using YIN algorithm.

This function performs fundamental frequency (F0) detection on audio data using the YIN algorithm, which is optimized for voice and monophonic instruments. It accepts audio buffers as Float32Array or number[], validates the input, and returns pitch information with confidence scores.

```typescript
async function detectPitch(
  audioBuffer: Float32Array | number[],
  sampleRate: number,
  options?: Partial<PitchDetectionOptions>
): Promise<PitchResult>;
```

#### Parameters

| Parameter     | Type                             | Required | Description                                                                                                    |
| ------------- | -------------------------------- | -------- | -------------------------------------------------------------------------------------------------------------- |
| `audioBuffer` | `Float32Array \| number[]`       | Yes      | Audio samples to analyze. Must be non-empty and contain no more than 16384 samples. All values must be finite. |
| `sampleRate`  | `number`                         | Yes      | Sample rate in Hz. Must be an integer between 8000 and 48000.                                                  |
| `options`     | `Partial<PitchDetectionOptions>` | No       | Configuration options for pitch detection. See [PitchDetectionOptions](#pitchdetectionoptions).                |

#### Returns

`Promise<PitchResult>` - Resolves to a [PitchResult](#pitchresult) containing detected pitch frequency, confidence score, and voicing information.

#### Throws

- `ValidationError` - If buffer or sample rate are invalid
- `NativeModuleError` - If native computation fails or frequency range is invalid

#### Example

```typescript
import { detectPitch } from '@loqalabs/loqa-expo-dsp';

// Basic usage with defaults (human voice range: 80-400 Hz)
const audioData = new Float32Array(2048);
// ... fill with audio samples ...

const result = await detectPitch(audioData, 44100);

if (result.isVoiced) {
  console.log(`Detected pitch: ${result.frequency} Hz`);
  console.log(`Confidence: ${result.confidence}`);
} else {
  console.log('No pitch detected (unvoiced segment)');
}

// Custom frequency range for bass instruments
const bassResult = await detectPitch(audioData, 44100, {
  minFrequency: 40, // Lower bound for bass
  maxFrequency: 250, // Upper bound for bass
});
```

---

### extractFormants

Extracts formants (F1, F2, F3) using LPC analysis.

This function performs Linear Predictive Coding (LPC) analysis to extract the first three formant frequencies from audio data. Formants are resonant frequencies of the vocal tract and are essential for vowel identification and speech analysis.

```typescript
async function extractFormants(
  audioBuffer: Float32Array | number[],
  sampleRate: number,
  options?: Partial<FormantExtractionOptions>
): Promise<FormantsResult>;
```

#### Parameters

| Parameter     | Type                                | Required | Description                                                                                                                        |
| ------------- | ----------------------------------- | -------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `audioBuffer` | `Float32Array \| number[]`          | Yes      | Audio samples to analyze. Must be non-empty and contain no more than 16384 samples. Should contain voiced speech for best results. |
| `sampleRate`  | `number`                            | Yes      | Sample rate in Hz. Must be an integer between 8000 and 48000.                                                                      |
| `options`     | `Partial<FormantExtractionOptions>` | No       | Configuration options for formant extraction. See [FormantExtractionOptions](#formantextractionoptions).                           |

#### Returns

`Promise<FormantsResult>` - Resolves to a [FormantsResult](#formantsresult) containing F1, F2, F3 frequencies and their bandwidths.

#### Throws

- `ValidationError` - If buffer or sample rate are invalid
- `NativeModuleError` - If native computation fails or LPC order is invalid

#### Example

```typescript
import { extractFormants } from '@loqalabs/loqa-expo-dsp';

// Basic usage with automatic LPC order
const audioData = new Float32Array(2048);
// ... fill with voiced audio samples ...

const result = await extractFormants(audioData, 44100);

console.log(`F1: ${result.f1} Hz`);
console.log(`F2: ${result.f2} Hz`);
console.log(`F3: ${result.f3} Hz`);
console.log(`Bandwidths:`, result.bandwidths);

// Custom LPC order for high sample rates
const result2 = await extractFormants(audioData, 48000, {
  lpcOrder: 16, // Higher order for 48kHz audio
});

// Vowel identification example
if (result.f1 < 400 && result.f2 > 2000) {
  console.log('Likely vowel: /i/ (as in "beat")');
} else if (result.f1 > 700 && result.f2 < 1200) {
  console.log('Likely vowel: /ɑ/ (as in "father")');
}
```

---

### analyzeSpectrum

Analyzes spectral features (centroid, rolloff, tilt).

This function computes spectral characteristics of audio data, including:

- **Spectral centroid**: "center of mass" of the spectrum (brightness measure)
- **Spectral rolloff**: frequency below which 95% of energy is concentrated
- **Spectral tilt**: overall slope of the spectrum (timbre indicator)

All features are computed in a single call for efficiency.

```typescript
async function analyzeSpectrum(
  audioBuffer: Float32Array | number[],
  sampleRate: number,
  options?: Partial<SpectrumAnalysisOptions>
): Promise<SpectrumResult>;
```

#### Parameters

| Parameter     | Type                               | Required | Description                                                                                                                                                |
| ------------- | ---------------------------------- | -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `audioBuffer` | `Float32Array \| number[]`         | Yes      | Audio samples to analyze. Must be non-empty and contain no more than 16384 samples. All values must be finite.                                             |
| `sampleRate`  | `number`                           | Yes      | Sample rate in Hz. Must be an integer between 8000 and 48000.                                                                                              |
| `options`     | `Partial<SpectrumAnalysisOptions>` | No       | Configuration options for spectrum analysis. See [SpectrumAnalysisOptions](#spectrumanalysisoptions). Currently unused but reserved for future extensions. |

#### Returns

`Promise<SpectrumResult>` - Resolves to a [SpectrumResult](#spectrumresult) containing spectral centroid, rolloff, and tilt values.

#### Throws

- `ValidationError` - If buffer or sample rate are invalid
- `NativeModuleError` - If native computation fails

#### Example

```typescript
import { analyzeSpectrum } from '@loqalabs/loqa-expo-dsp';

// Basic usage
const audioData = new Float32Array(2048);
// ... fill with audio samples ...

const result = await analyzeSpectrum(audioData, 44100);

console.log(`Spectral centroid: ${result.centroid} Hz`);
console.log(`Spectral rolloff: ${result.rolloff} Hz`);
console.log(`Spectral tilt: ${result.tilt}`);

// Brightness classification
if (result.centroid > 4000) {
  console.log('Bright sound (high-frequency dominant)');
} else if (result.centroid < 1500) {
  console.log('Dark sound (low-frequency dominant)');
} else {
  console.log('Balanced sound');
}

// Timbre analysis
if (result.tilt > 0) {
  console.log('More energy in high frequencies');
} else {
  console.log('More energy in low frequencies');
}
```

---

## Types

### FFTOptions

Configuration options for FFT computation.

```typescript
interface FFTOptions {
  fftSize?: number;
  windowType?: 'hanning' | 'hamming' | 'blackman' | 'none';
  includePhase?: boolean;
}
```

#### Properties

| Property       | Type                                             | Default              | Description                                                                                                                                                                                                                                                                           |
| -------------- | ------------------------------------------------ | -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `fftSize`      | `number`                                         | `audioBuffer.length` | FFT size. Must be a power of 2 between 256 and 8192. Larger sizes provide better frequency resolution but lower time resolution.                                                                                                                                                      |
| `windowType`   | `'hanning' \| 'hamming' \| 'blackman' \| 'none'` | `'hanning'`          | Window function type. **'hanning'**: Good general-purpose window (default). **'hamming'**: Similar to Hanning, slightly different sidelobe behavior. **'blackman'**: Better frequency resolution, more attenuation. **'none'**: Rectangular window (use only for perfect sine waves). |
| `includePhase` | `boolean`                                        | `false`              | Whether to return phase information. Set to `true` only if you need phase data, as it increases computation time.                                                                                                                                                                     |

#### Validation Rules

- `fftSize` must be an integer
- `fftSize` must be a power of 2
- `fftSize` must be between 256 and 8192

---

### FFTResult

Result of FFT computation.

Contains frequency-domain representation of the input audio signal.

```typescript
interface FFTResult {
  magnitude: Float32Array;
  phase?: Float32Array;
  frequencies: Float32Array;
}
```

#### Properties

| Property      | Type                      | Description                                                                                                                                                                                          |
| ------------- | ------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `magnitude`   | `Float32Array`            | Magnitude spectrum (length = `fftSize / 2`). Each element represents the amplitude at the corresponding frequency. Higher values indicate stronger presence of that frequency component.             |
| `phase`       | `Float32Array` (optional) | Phase spectrum (only present if `includePhase: true`). Each element represents the phase angle in radians at the corresponding frequency. Useful for signal reconstruction and phase-based analysis. |
| `frequencies` | `Float32Array`            | Frequency bin centers in Hz (length = `fftSize / 2`). Each element corresponds to the center frequency of each magnitude/phase bin. Use this array to map magnitude values to their frequencies.     |

---

### PitchDetectionOptions

Configuration options for pitch detection.

```typescript
interface PitchDetectionOptions {
  sampleRate: number;
  minFrequency?: number;
  maxFrequency?: number;
}
```

#### Properties

| Property       | Type     | Default    | Description                                                                                                                                                    |
| -------------- | -------- | ---------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `sampleRate`   | `number` | (required) | Sample rate in Hz. Must be an integer between 8000 and 48000.                                                                                                  |
| `minFrequency` | `number` | `80`       | Minimum detectable frequency in Hz. Must be positive. Default value is optimized for human voice (80 Hz ≈ low male voice).                                     |
| `maxFrequency` | `number` | `400`      | Maximum detectable frequency in Hz. Must be positive and greater than `minFrequency`. Default value is optimized for human voice (400 Hz ≈ high female voice). |

#### Validation Rules

- `sampleRate` must be an integer between 8000 and 48000
- `minFrequency` and `maxFrequency` must be positive
- `minFrequency` must be less than `maxFrequency`

---

### PitchResult

Result of pitch detection.

```typescript
interface PitchResult {
  frequency: number | null;
  confidence: number;
  isVoiced: boolean;
}
```

#### Properties

| Property     | Type             | Description                                                                                                                                                                            |
| ------------ | ---------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `frequency`  | `number \| null` | Detected pitch in Hz. `null` if no pitch was detected (unvoiced segment or below confidence threshold).                                                                                |
| `confidence` | `number`         | Confidence score (0-1). Values closer to 1 indicate higher confidence in the detected pitch. Values below ~0.5 typically indicate unvoiced segments or unreliable pitch.               |
| `isVoiced`   | `boolean`        | Whether the audio segment is voiced. `true` indicates periodic signal (likely speech or musical note), `false` indicates non-periodic signal (silence, noise, or unvoiced consonants). |

---

### FormantExtractionOptions

Configuration options for formant extraction.

```typescript
interface FormantExtractionOptions {
  sampleRate: number;
  lpcOrder?: number;
}
```

#### Properties

| Property     | Type     | Default                             | Description                                                                                                                                                                                                                |
| ------------ | -------- | ----------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `sampleRate` | `number` | (required)                          | Sample rate in Hz. Must be an integer between 8000 and 48000.                                                                                                                                                              |
| `lpcOrder`   | `number` | `Math.floor(sampleRate / 1000) + 2` | LPC (Linear Predictive Coding) order. Higher values provide better formant resolution but increase computation time. The default formula provides appropriate resolution for most speech analysis tasks. Must be positive. |

#### Default LPC Order Values

| Sample Rate | Default LPC Order |
| ----------- | ----------------- |
| 8000 Hz     | 10                |
| 16000 Hz    | 18                |
| 22050 Hz    | 24                |
| 44100 Hz    | 46                |
| 48000 Hz    | 50                |

#### Validation Rules

- `sampleRate` must be an integer between 8000 and 48000
- `lpcOrder` must be positive

---

### FormantsResult

Result of formant extraction.

```typescript
interface FormantsResult {
  f1: number;
  f2: number;
  f3: number;
  bandwidths: {
    f1: number;
    f2: number;
    f3: number;
  };
}
```

#### Properties

| Property        | Type     | Description                                                                                                                                                      |
| --------------- | -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `f1`            | `number` | First formant (F1) in Hz. Typically 200-1000 Hz for speech. Correlates with vowel height (low F1 = high vowels like /i/, high F1 = low vowels like /a/).         |
| `f2`            | `number` | Second formant (F2) in Hz. Typically 600-3000 Hz for speech. Correlates with vowel frontness (low F2 = back vowels like /u/, high F2 = front vowels like /i/).   |
| `f3`            | `number` | Third formant (F3) in Hz. Typically 1500-4000 Hz for speech. Less variable than F1/F2 but useful for distinguishing certain consonants (especially /r/ and /l/). |
| `bandwidths`    | `object` | Formant bandwidths in Hz. Indicates the resonance width of each formant. Narrower bandwidths indicate sharper resonances.                                        |
| `bandwidths.f1` | `number` | Bandwidth of F1 in Hz.                                                                                                                                           |
| `bandwidths.f2` | `number` | Bandwidth of F2 in Hz.                                                                                                                                           |
| `bandwidths.f3` | `number` | Bandwidth of F3 in Hz.                                                                                                                                           |

---

### SpectrumAnalysisOptions

Configuration options for spectrum analysis.

```typescript
interface SpectrumAnalysisOptions {
  sampleRate: number;
}
```

#### Properties

| Property     | Type     | Description                                                   |
| ------------ | -------- | ------------------------------------------------------------- |
| `sampleRate` | `number` | Sample rate in Hz. Must be an integer between 8000 and 48000. |

#### Validation Rules

- `sampleRate` must be an integer between 8000 and 48000

---

### SpectrumResult

Result of spectrum analysis.

```typescript
interface SpectrumResult {
  centroid: number;
  rolloff: number;
  tilt: number;
}
```

#### Properties

| Property   | Type     | Description                                                                                                                                                                                                                                                                                                                                        |
| ---------- | -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `centroid` | `number` | Spectral centroid in Hz (brightness measure). Represents the "center of mass" of the spectrum. Higher values indicate brighter sounds (more high-frequency content), lower values indicate darker sounds (more low-frequency content). Typical ranges: Speech ~1500-3000 Hz, Bright instruments (cymbals) ~6000+ Hz, Bass instruments ~200-500 Hz. |
| `rolloff`  | `number` | Spectral rolloff in Hz (95% energy threshold). Frequency below which 95% of the signal's energy is concentrated. Useful for distinguishing harmonic content from noise. Lower values indicate most energy is in low frequencies.                                                                                                                   |
| `tilt`     | `number` | Spectral tilt (slope of spectrum). Measures the overall slope of the magnitude spectrum. **Positive values**: More energy in high frequencies (bright timbre). **Negative values**: More energy in low frequencies (dark timbre). **Near zero**: Balanced spectrum (white noise-like).                                                             |

---

## Error Handling

All functions in @loqalabs/loqa-expo-dsp throw typed errors that extend the base `LoqaExpoDspError` class. Errors include detailed context information to help diagnose issues.

### LoqaExpoDspError

Base error class for all LoqaExpoDsp errors.

```typescript
class LoqaExpoDspError extends Error {
  constructor(message: string, public code: string, public details?: Record<string, unknown>);
}
```

#### Properties

| Property  | Type                      | Description                                |
| --------- | ------------------------- | ------------------------------------------ |
| `message` | `string`                  | Human-readable error message               |
| `code`    | `string`                  | Error code for programmatic error handling |
| `details` | `Record<string, unknown>` | Additional error details for debugging     |
| `name`    | `string`                  | Error class name (`'LoqaExpoDspError'`)    |

---

### ValidationError

Error thrown when input validation fails.

This error indicates that the provided input parameters did not meet the required constraints (e.g., buffer size, sample rate range, FFT size).

```typescript
class ValidationError extends LoqaExpoDspError {
  constructor(message: string, details?: Record<string, unknown>);
}
```

#### Properties

| Property  | Type                      | Description                                                |
| --------- | ------------------------- | ---------------------------------------------------------- |
| `code`    | `string`                  | Always `'VALIDATION_ERROR'`                                |
| `name`    | `string`                  | Always `'ValidationError'`                                 |
| `message` | `string`                  | Description of the validation failure                      |
| `details` | `Record<string, unknown>` | Additional context (e.g., invalid values, expected ranges) |

#### Common Validation Errors

| Error Message                                     | Cause                              | Solution                                                     |
| ------------------------------------------------- | ---------------------------------- | ------------------------------------------------------------ |
| `'Audio buffer cannot be null or undefined'`      | Buffer parameter is null/undefined | Provide a valid Float32Array or number[]                     |
| `'Audio buffer cannot be empty'`                  | Buffer has length 0                | Ensure buffer contains audio samples                         |
| `'Buffer too large (max 16384 samples)'`          | Buffer exceeds maximum size        | Split buffer into chunks ≤16384 samples                      |
| `'Buffer contains NaN or Infinity values'`        | Invalid numeric values in buffer   | Check audio source for corruption                            |
| `'Sample rate must be an integer'`                | Non-integer sample rate            | Use integer sample rate (e.g., 44100, not 44100.5)           |
| `'Sample rate must be between 8000 and 48000 Hz'` | Sample rate out of range           | Use standard sample rates (8000, 16000, 22050, 44100, 48000) |
| `'FFT size must be a power of 2'`                 | Invalid FFT size                   | Use powers of 2 (256, 512, 1024, 2048, 4096, 8192)           |
| `'FFT size must be between 256 and 8192'`         | FFT size out of range              | Use FFT size between 256 and 8192                            |

#### Example

```typescript
import { computeFFT, ValidationError } from '@loqalabs/loqa-expo-dsp';

try {
  const result = await computeFFT(audioBuffer, { fftSize: 1000 });
} catch (error) {
  if (error instanceof ValidationError) {
    console.error('Validation failed:', error.message);
    console.error('Error code:', error.code); // 'VALIDATION_ERROR'
    console.error('Details:', error.details); // { fftSize: 1000 }
  }
}
```

---

### NativeModuleError

Error thrown when native module operations fail.

This error wraps errors that occur in the native iOS or Android code, providing context about the failure and suggestions for resolution.

```typescript
class NativeModuleError extends LoqaExpoDspError {
  constructor(message: string, details?: Record<string, unknown>);
}
```

#### Properties

| Property  | Type                      | Description                                                  |
| --------- | ------------------------- | ------------------------------------------------------------ |
| `code`    | `string`                  | Always `'NATIVE_MODULE_ERROR'`                               |
| `name`    | `string`                  | Always `'NativeModuleError'`                                 |
| `message` | `string`                  | Description of the native module failure                     |
| `details` | `Record<string, unknown>` | Additional context (e.g., original native error, parameters) |

#### Common Native Module Errors

| Error Prefix                   | Cause                             | Solution                                                        |
| ------------------------------ | --------------------------------- | --------------------------------------------------------------- |
| `'FFT computation failed:'`    | Native FFT computation error      | Check buffer data validity, ensure module is properly installed |
| `'Pitch detection failed:'`    | Native pitch detection error      | Ensure audio contains periodic signal, check frequency range    |
| `'Formant extraction failed:'` | Native LPC analysis error         | Ensure audio contains voiced speech, check LPC order            |
| `'Spectrum analysis failed:'`  | Native spectrum computation error | Check buffer data validity                                      |

#### Example

```typescript
import { computeFFT, NativeModuleError } from '@loqalabs/loqa-expo-dsp';

try {
  const result = await computeFFT(audioBuffer);
} catch (error) {
  if (error instanceof NativeModuleError) {
    console.error('Native module failed:', error.message);
    console.error('Error code:', error.code); // 'NATIVE_MODULE_ERROR'
    console.error('Details:', error.details);
    // Details include: originalError, fftSize, windowType, bufferLength, etc.
  }
}
```

---

## Validation Rules

### Audio Buffer Validation

All audio buffers passed to API functions must meet these requirements:

| Rule              | Constraint                                      | Validation                                   |
| ----------------- | ----------------------------------------------- | -------------------------------------------- |
| **Non-null**      | Buffer cannot be null or undefined              | `if (!buffer)`                               |
| **Non-empty**     | Buffer must contain at least 1 sample           | `buffer.length > 0`                          |
| **Maximum size**  | Buffer cannot exceed 16384 samples              | `buffer.length <= 16384`                     |
| **Finite values** | All samples must be finite (no NaN or Infinity) | `Array.from(buffer).every(v => isFinite(v))` |
| **Type**          | Must be Float32Array or number[]                | Accepted by all functions                    |

### Sample Rate Validation

Sample rates must meet these requirements:

| Rule        | Constraint                | Common Values                    |
| ----------- | ------------------------- | -------------------------------- |
| **Integer** | Must be a whole number    | Use `Math.round()` if needed     |
| **Range**   | Between 8000 and 48000 Hz | 8000, 16000, 22050, 44100, 48000 |

### FFT Size Validation

FFT sizes must meet these requirements:

| Rule           | Constraint                     | Valid Values                                    |
| -------------- | ------------------------------ | ----------------------------------------------- |
| **Integer**    | Must be a whole number         | 256, 512, 1024, 2048, 4096, 8192                |
| **Power of 2** | Must be 2^n where n is integer | Use formula: `2 ** Math.round(Math.log2(size))` |
| **Range**      | Between 256 and 8192           | Common: 512, 1024, 2048, 4096                   |

### Frequency Range Validation (Pitch Detection)

Frequency ranges must meet these requirements:

| Rule         | Constraint                   | Typical Ranges                                                                           |
| ------------ | ---------------------------- | ---------------------------------------------------------------------------------------- |
| **Positive** | Both min and max must be > 0 | Human voice: 80-400 Hz<br>Bass instruments: 40-250 Hz<br>Musical instruments: 27-4186 Hz |
| **Ordered**  | minFrequency < maxFrequency  | Ensure min is always less than max                                                       |

### LPC Order Validation (Formant Extraction)

LPC order must meet these requirements:

| Rule         | Constraint  | Recommended Values                                                                      |
| ------------ | ----------- | --------------------------------------------------------------------------------------- |
| **Positive** | Must be > 0 | Default: `Math.floor(sampleRate / 1000) + 2`<br>8 kHz: 10<br>16 kHz: 18<br>44.1 kHz: 46 |

---

## Performance Considerations

### Processing Latency

All functions are optimized for sub-5ms processing latency on typical mobile devices:

| Function          | Typical Latency (2048 samples) | Notes                                        |
| ----------------- | ------------------------------ | -------------------------------------------- |
| `computeFFT`      | 1-3 ms                         | Depends on FFT size and includePhase option  |
| `detectPitch`     | 2-4 ms                         | YIN algorithm is optimized for real-time use |
| `extractFormants` | 2-5 ms                         | LPC analysis with default order              |
| `analyzeSpectrum` | 1-3 ms                         | Efficient spectral feature extraction        |

### Memory Usage

Memory usage is proportional to buffer size and options:

| Function          | Memory Usage (2048 samples) | Scalability     |
| ----------------- | --------------------------- | --------------- |
| `computeFFT`      | ~8-16 KB                    | O(fftSize)      |
| `detectPitch`     | ~4-8 KB                     | O(bufferLength) |
| `extractFormants` | ~4-8 KB                     | O(lpcOrder)     |
| `analyzeSpectrum` | ~4-8 KB                     | O(bufferLength) |

### Best Practices

1. **Buffer Size**: Use buffers between 1024-2048 samples for real-time analysis
2. **Reuse Buffers**: Reuse Float32Array instances to minimize allocations
3. **Batch Processing**: Process multiple buffers in parallel for offline analysis
4. **Error Handling**: Always catch errors to prevent crashes
5. **Validation**: Validate inputs early to fail fast with clear messages

---

## Version History

- **0.1.0** (2025-11-20): Initial release with computeFFT, detectPitch, extractFormants, analyzeSpectrum

---

For integration patterns and usage examples, see [INTEGRATION_GUIDE.md](./INTEGRATION_GUIDE.md).
For general information and quick start, see [README.md](../README.md).
