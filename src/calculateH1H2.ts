// calculateH1H2 - H1-H2 amplitude difference API for vocal weight analysis
import LoqaExpoDspModule from './LoqaExpoDspModule';
import { NativeModuleError } from './errors';
import type { H1H2Options, H1H2Result } from './types';
import { logDebug } from './utils';
import { validateAudioBuffer, validateSampleRate } from './validation';

/**
 * Calculates H1-H2 amplitude difference for vocal weight analysis
 *
 * H1-H2 measures the difference in amplitude between the first harmonic (fundamental)
 * and second harmonic. It's a key acoustic correlate of vocal weight:
 * - Higher H1-H2 (>5 dB): Lighter, breathier vocal quality
 * - Lower H1-H2 (<0 dB): Fuller, heavier vocal quality
 * - Moderate H1-H2 (0-5 dB): Balanced vocal weight
 *
 * The function uses parabolic interpolation around the harmonic peaks for
 * accurate sub-bin amplitude estimation.
 *
 * @param audioBuffer - Audio samples (Float32Array or number[])
 * @param options - H1-H2 calculation options including sampleRate and optional pre-calculated f0
 * @returns Promise resolving to H1H2Result with h1h2, h1AmplitudeDb, h2AmplitudeDb, and f0
 * @throws ValidationError if buffer or sample rate are invalid
 * @throws NativeModuleError if native computation fails (e.g., unvoiced signal without f0)
 *
 * @example
 * ```typescript
 * const audioData = new Float32Array(4096);
 * // ... fill with audio samples ...
 *
 * // Option 1: Auto-detect F0
 * const result = await calculateH1H2(audioData, { sampleRate: 44100 });
 *
 * // Option 2: Provide pre-calculated F0 (more efficient if you already have it)
 * const pitch = await detectPitch(audioData, 44100);
 * const result2 = await calculateH1H2(audioData, {
 *   sampleRate: 44100,
 *   f0: pitch.frequency ?? undefined
 * });
 *
 * console.log(`H1-H2: ${result.h1h2} dB`);
 * console.log(`H1 amplitude: ${result.h1AmplitudeDb} dB`);
 * console.log(`H2 amplitude: ${result.h2AmplitudeDb} dB`);
 *
 * if (result.h1h2 > 5) {
 *   console.log('Lighter voice quality');
 * } else if (result.h1h2 < 0) {
 *   console.log('Fuller voice quality');
 * }
 * ```
 */
export async function calculateH1H2(
  audioBuffer: Float32Array | number[],
  options: H1H2Options
): Promise<H1H2Result> {
  // Step 1: Validate audio buffer and sample rate
  logDebug('calculateH1H2 called', {
    bufferLength: audioBuffer.length,
    bufferType: audioBuffer instanceof Float32Array ? 'Float32Array' : 'number[]',
    sampleRate: options.sampleRate,
    f0: options.f0,
  });

  validateAudioBuffer(audioBuffer);
  validateSampleRate(options.sampleRate);

  // Step 2: Convert to number[] for React Native bridge
  // React Native bridge requires plain arrays, not typed arrays
  const bufferArray: number[] =
    audioBuffer instanceof Float32Array ? Array.from(audioBuffer) : audioBuffer;

  // Build native options
  const nativeOptions: Record<string, unknown> = {};
  if (options.f0 !== undefined && options.f0 > 0) {
    nativeOptions.f0 = options.f0;
  }

  logDebug('Calling native module for H1-H2 calculation', {
    sampleRate: options.sampleRate,
    bufferLength: bufferArray.length,
    nativeOptions,
  });

  try {
    // Step 3: Call native module
    const nativeResult = await LoqaExpoDspModule.calculateH1H2(
      bufferArray,
      options.sampleRate,
      nativeOptions
    );

    logDebug('Native module returned H1-H2 result', {
      h1h2: nativeResult.h1h2,
      h1AmplitudeDb: nativeResult.h1AmplitudeDb,
      h2AmplitudeDb: nativeResult.h2AmplitudeDb,
      f0: nativeResult.f0,
    });

    // Step 4: Convert result to H1H2Result type
    const result: H1H2Result = {
      h1h2: nativeResult.h1h2,
      h1AmplitudeDb: nativeResult.h1AmplitudeDb,
      h2AmplitudeDb: nativeResult.h2AmplitudeDb,
      f0: nativeResult.f0,
    };

    logDebug('calculateH1H2 completed successfully', result);

    return result;
  } catch (error: unknown) {
    // Step 5: Wrap native errors in NativeModuleError with context
    const errorMessage = error instanceof Error ? error.message : String(error);

    logDebug('calculateH1H2 failed', {
      error: errorMessage,
      sampleRate: options.sampleRate,
      bufferLength: audioBuffer.length,
    });

    throw new NativeModuleError(`H1-H2 calculation failed: ${errorMessage}`, {
      originalError: error,
      sampleRate: options.sampleRate,
      bufferLength: audioBuffer.length,
    });
  }
}
