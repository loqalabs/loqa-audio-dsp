// computeFFT - FFT computation API
import LoqaAudioDspModule from './LoqaAudioDspModule';
import { NativeModuleError } from './errors';
import type { FFTOptions, FFTResult } from './types';
import { logDebug } from './utils';
import { validateAudioBuffer, validateFFTSize } from './validation';

/**
 * Computes Fast Fourier Transform (FFT) of audio buffer
 *
 * This function performs frequency analysis on audio data using the FFT algorithm.
 * It accepts audio buffers as Float32Array or number[], validates the input,
 * and returns magnitude and frequency information.
 *
 * @param audioBuffer - Audio samples (Float32Array or number[])
 * @param options - FFT configuration options
 * @returns Promise resolving to FFT result with magnitude, frequencies, and optional phase
 * @throws ValidationError if buffer or options are invalid
 * @throws NativeModuleError if native computation fails
 *
 * @example
 * ```typescript
 * const audioData = new Float32Array(1024);
 * // ... fill with audio samples ...
 *
 * const result = await computeFFT(audioData, {
 *   fftSize: 2048,
 *   windowType: 'hanning',
 *   includePhase: false
 * });
 *
 * console.log('Magnitude:', result.magnitude);
 * console.log('Frequencies:', result.frequencies);
 * ```
 */
export async function computeFFT(
  audioBuffer: Float32Array | number[],
  options?: FFTOptions
): Promise<FFTResult> {
  // Step 1: Validate audio buffer
  logDebug('computeFFT called', {
    bufferLength: audioBuffer.length,
    bufferType: audioBuffer instanceof Float32Array ? 'Float32Array' : 'number[]',
    options,
  });

  validateAudioBuffer(audioBuffer);

  // Step 2: Extract and validate options with defaults
  const fftSize = options?.fftSize || audioBuffer.length;
  const windowType = options?.windowType || 'hanning';
  const includePhase = options?.includePhase || false;

  // Validate FFT size
  validateFFTSize(fftSize);

  // Step 3: Convert to number[] for React Native bridge
  // React Native bridge requires plain arrays, not typed arrays
  const bufferArray: number[] =
    audioBuffer instanceof Float32Array ? Array.from(audioBuffer) : audioBuffer;

  logDebug('Calling native module', {
    fftSize,
    windowType,
    includePhase,
  });

  try {
    // Step 4: Call native module
    // Default sample rate to 44100 Hz if not provided (used for frequency calculation)
    const sampleRate = 44100; // Default sample rate for frequency bin calculation

    const nativeResult = await LoqaAudioDspModule.computeFFT(bufferArray, {
      fftSize,
      windowType,
      includePhase,
      sampleRate,
    });

    logDebug('Native module returned', {
      magnitudeLength: nativeResult.magnitude?.length,
      hasPhase: !!nativeResult.phase,
      frequenciesLength: nativeResult.frequencies?.length,
    });

    // Step 5: Convert result to FFTResult with Float32Array
    // The native module returns plain arrays, but our API contract uses Float32Array
    const result: FFTResult = {
      magnitude: new Float32Array(nativeResult.magnitude),
      phase: nativeResult.phase ? new Float32Array(nativeResult.phase) : undefined,
      frequencies: new Float32Array(nativeResult.frequencies),
    };

    logDebug('computeFFT completed successfully', {
      magnitudeLength: result.magnitude.length,
      hasPhase: !!result.phase,
    });

    return result;
  } catch (error: unknown) {
    // Step 6: Wrap native errors in NativeModuleError with context
    const errorMessage = error instanceof Error ? error.message : String(error);

    logDebug('computeFFT failed', {
      error: errorMessage,
      fftSize,
      bufferLength: audioBuffer.length,
    });

    throw new NativeModuleError(`FFT computation failed: ${errorMessage}`, {
      originalError: error,
      fftSize,
      windowType,
      bufferLength: audioBuffer.length,
    });
  }
}
