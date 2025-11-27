// calculateHNR - Harmonics-to-Noise Ratio API for breathiness analysis
import LoqaExpoDspModule from './LoqaExpoDspModule';
import { NativeModuleError } from './errors';
import type { HNROptions, HNRResult } from './types';
import { logDebug } from './utils';
import { validateAudioBuffer, validateSampleRate } from './validation';

/**
 * Calculates Harmonics-to-Noise Ratio (HNR) for breathiness analysis
 *
 * HNR measures the ratio of harmonic (periodic) to noise (aperiodic) energy
 * in voice, providing a quantitative measure of breathiness. It uses
 * Boersma's autocorrelation-based method.
 *
 * Typical HNR ranges:
 * - 18-25 dB: Clear, less breathy voice
 * - 12-18 dB: Softer, more breathy voice
 * - <10 dB: Very breathy or pathological voice
 *
 * @param audioBuffer - Audio samples (Float32Array or number[])
 * @param options - HNR calculation options including sampleRate and optional frequency range
 * @returns Promise resolving to HNRResult with hnr (dB), f0 (Hz), and isVoiced flag
 * @throws ValidationError if buffer or sample rate are invalid
 * @throws NativeModuleError if native computation fails
 *
 * @example
 * ```typescript
 * const audioData = new Float32Array(4096);
 * // ... fill with audio samples ...
 *
 * const result = await calculateHNR(audioData, { sampleRate: 44100 });
 *
 * if (result.isVoiced) {
 *   console.log(`HNR: ${result.hnr} dB`);
 *   console.log(`Detected F0: ${result.f0} Hz`);
 *
 *   if (result.hnr > 20) {
 *     console.log('Clear voice detected');
 *   } else if (result.hnr < 15) {
 *     console.log('Breathy voice detected');
 *   }
 * } else {
 *   console.log('Signal is unvoiced');
 * }
 * ```
 */
export async function calculateHNR(
  audioBuffer: Float32Array | number[],
  options: HNROptions
): Promise<HNRResult> {
  // Step 1: Validate audio buffer and sample rate
  logDebug('calculateHNR called', {
    bufferLength: audioBuffer.length,
    bufferType: audioBuffer instanceof Float32Array ? 'Float32Array' : 'number[]',
    sampleRate: options.sampleRate,
    minFreq: options.minFreq,
    maxFreq: options.maxFreq,
  });

  validateAudioBuffer(audioBuffer);
  validateSampleRate(options.sampleRate);

  // Step 2: Convert to number[] for React Native bridge
  // React Native bridge requires plain arrays, not typed arrays
  const bufferArray: number[] =
    audioBuffer instanceof Float32Array ? Array.from(audioBuffer) : audioBuffer;

  // Build native options
  const nativeOptions: Record<string, unknown> = {};
  if (options.minFreq !== undefined) {
    nativeOptions.minFreq = options.minFreq;
  }
  if (options.maxFreq !== undefined) {
    nativeOptions.maxFreq = options.maxFreq;
  }

  logDebug('Calling native module for HNR calculation', {
    sampleRate: options.sampleRate,
    bufferLength: bufferArray.length,
    nativeOptions,
  });

  try {
    // Step 3: Call native module
    const nativeResult = await LoqaExpoDspModule.calculateHNR(
      bufferArray,
      options.sampleRate,
      nativeOptions
    );

    logDebug('Native module returned HNR result', {
      hnr: nativeResult.hnr,
      f0: nativeResult.f0,
      isVoiced: nativeResult.isVoiced,
    });

    // Step 4: Convert result to HNRResult type
    const result: HNRResult = {
      hnr: nativeResult.hnr,
      f0: nativeResult.f0,
      isVoiced: nativeResult.isVoiced,
    };

    logDebug('calculateHNR completed successfully', result);

    return result;
  } catch (error: unknown) {
    // Step 5: Wrap native errors in NativeModuleError with context
    const errorMessage = error instanceof Error ? error.message : String(error);

    logDebug('calculateHNR failed', {
      error: errorMessage,
      sampleRate: options.sampleRate,
      bufferLength: audioBuffer.length,
    });

    throw new NativeModuleError(`HNR calculation failed: ${errorMessage}`, {
      originalError: error,
      sampleRate: options.sampleRate,
      bufferLength: audioBuffer.length,
    });
  }
}
