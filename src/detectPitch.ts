// detectPitch - Pitch detection API using pYIN algorithm (loqa-voice-dsp v0.4.0)
import LoqaExpoDspModule from './LoqaExpoDspModule';
import { NativeModuleError } from './errors';
import type { PitchDetectionOptions, PitchResult } from './types';
import { logDebug } from './utils';
import { validateAudioBuffer, validateSampleRate } from './validation';

/**
 * Detects pitch using pYIN algorithm
 *
 * This function performs fundamental frequency (F0) detection on audio data using
 * the pYIN (probabilistic YIN) algorithm, which is optimized for voice and provides
 * improved accuracy for breathy or noisy signals compared to standard YIN.
 *
 * As of loqa-voice-dsp v0.4.0, this now uses a custom pYIN implementation with:
 * - Beta distribution threshold sampling for probabilistic candidate generation
 * - HMM with Viterbi decoding for smooth pitch tracks
 * - Voice-specific optimizations (80-400 Hz range, ±20% transition constraints)
 *
 * @param audioBuffer - Audio samples (Float32Array or number[])
 * @param sampleRate - Sample rate in Hz (8000-48000)
 * @param options - Pitch detection options (minFrequency, maxFrequency)
 * @returns Promise resolving to pitch result with frequency, confidence, voicing, and voicedProbability
 * @throws ValidationError if buffer or sample rate are invalid
 * @throws NativeModuleError if native computation fails
 *
 * @example
 * ```typescript
 * const audioData = new Float32Array(2048);
 * // ... fill with audio samples ...
 *
 * const result = await detectPitch(audioData, 44100, {
 *   minFrequency: 80,   // Minimum detectable pitch (Hz)
 *   maxFrequency: 400   // Maximum detectable pitch (Hz)
 * });
 *
 * if (result.isVoiced) {
 *   console.log(`Detected pitch: ${result.frequency} Hz`);
 *   console.log(`Confidence: ${result.confidence}`);
 *   console.log(`Voiced probability: ${result.voicedProbability}`);
 * } else {
 *   console.log('No pitch detected (unvoiced segment)');
 * }
 * ```
 */
export async function detectPitch(
  audioBuffer: Float32Array | number[],
  sampleRate: number,
  options?: Partial<PitchDetectionOptions>
): Promise<PitchResult> {
  // Step 1: Validate audio buffer and sample rate
  logDebug('detectPitch called', {
    bufferLength: audioBuffer.length,
    bufferType: audioBuffer instanceof Float32Array ? 'Float32Array' : 'number[]',
    sampleRate,
    options,
  });

  validateAudioBuffer(audioBuffer);
  validateSampleRate(sampleRate);

  // Step 2: Extract and set defaults for optional parameters
  // Default to human voice range: 80-400 Hz
  const minFrequency = options?.minFrequency ?? 80;
  const maxFrequency = options?.maxFrequency ?? 400;

  // Validate frequency range
  if (minFrequency <= 0 || maxFrequency <= 0) {
    throw new NativeModuleError('Frequency range must be positive', {
      minFrequency,
      maxFrequency,
    });
  }

  if (minFrequency >= maxFrequency) {
    throw new NativeModuleError('minFrequency must be less than maxFrequency', {
      minFrequency,
      maxFrequency,
    });
  }

  // Step 3: Convert to number[] for React Native bridge
  // React Native bridge requires plain arrays, not typed arrays
  const bufferArray: number[] =
    audioBuffer instanceof Float32Array ? Array.from(audioBuffer) : audioBuffer;

  logDebug('Calling native module for pitch detection', {
    sampleRate,
    minFrequency,
    maxFrequency,
  });

  try {
    // Step 4: Call native module
    const nativeResult = await LoqaExpoDspModule.detectPitch(bufferArray, sampleRate, {
      minFrequency,
      maxFrequency,
    });

    logDebug('Native module returned pitch result', {
      frequency: nativeResult.frequency,
      confidence: nativeResult.confidence,
      isVoiced: nativeResult.isVoiced,
      voicedProbability: nativeResult.voicedProbability,
    });

    // Step 5: Convert result to PitchResult type
    // Native module returns dictionary/map, convert to proper TypeScript type
    const result: PitchResult = {
      frequency: nativeResult.frequency !== null ? nativeResult.frequency : null,
      confidence: nativeResult.confidence,
      isVoiced: nativeResult.isVoiced,
      voicedProbability: nativeResult.voicedProbability ?? 0,
    };

    logDebug('detectPitch completed successfully', {
      frequency: result.frequency,
      confidence: result.confidence,
      isVoiced: result.isVoiced,
      voicedProbability: result.voicedProbability,
    });

    return result;
  } catch (error: unknown) {
    // Step 6: Wrap native errors in NativeModuleError with context
    const errorMessage = error instanceof Error ? error.message : String(error);

    logDebug('detectPitch failed', {
      error: errorMessage,
      sampleRate,
      bufferLength: audioBuffer.length,
    });

    throw new NativeModuleError(`Pitch detection failed: ${errorMessage}`, {
      originalError: error,
      sampleRate,
      minFrequency,
      maxFrequency,
      bufferLength: audioBuffer.length,
    });
  }
}
