// extractFormants - Formant extraction API using LPC analysis
import LoqaExpoDspModule from './LoqaExpoDspModule';
import { NativeModuleError } from './errors';
import { logDebug } from './utils';
import { validateAudioBuffer, validateSampleRate } from './validation';
/**
 * Extracts formants (F1, F2, F3) using LPC analysis
 *
 * This function performs Linear Predictive Coding (LPC) analysis to extract
 * the first three formant frequencies from audio data. Formants are resonant
 * frequencies of the vocal tract and are essential for vowel identification
 * and speech analysis.
 *
 * @param audioBuffer - Audio samples (Float32Array or number[])
 * @param sampleRate - Sample rate in Hz (8000-48000)
 * @param options - Formant extraction options (lpcOrder)
 * @returns Promise resolving to formants result with F1, F2, F3 and bandwidths
 * @throws ValidationError if buffer or sample rate are invalid
 * @throws NativeModuleError if native computation fails
 *
 * @example
 * ```typescript
 * const audioData = new Float32Array(2048);
 * // ... fill with voiced audio samples ...
 *
 * const result = await extractFormants(audioData, 44100, {
 *   lpcOrder: 24  // Optional: defaults to min(24, sampleRate/1000 + 2)
 * });
 *
 * console.log(`F1: ${result.f1} Hz`);
 * console.log(`F2: ${result.f2} Hz`);
 * console.log(`F3: ${result.f3} Hz`);
 * console.log(`Bandwidths:`, result.bandwidths);
 * ```
 */
export async function extractFormants(audioBuffer, sampleRate, options) {
    // Step 1: Validate audio buffer and sample rate
    logDebug('extractFormants called', {
        bufferLength: audioBuffer.length,
        bufferType: audioBuffer instanceof Float32Array ? 'Float32Array' : 'number[]',
        sampleRate,
        options,
    });
    validateAudioBuffer(audioBuffer);
    validateSampleRate(sampleRate);
    // Step 2: Calculate LPC order
    // LPC order should be high enough to capture formants but not so high it causes
    // numerical instability. Typical range: 10-24 for voice analysis.
    // For high sample rates, cap at 24 to avoid LPC algorithm issues.
    const MIN_LPC_ORDER = 10;
    const MAX_LPC_ORDER = 24; // Cap to prevent numerical instability in LPC
    const defaultLpcOrder = Math.min(MAX_LPC_ORDER, Math.floor(sampleRate / 1000) + 2);
    const lpcOrder = Math.max(MIN_LPC_ORDER, Math.min(MAX_LPC_ORDER, options?.lpcOrder ?? defaultLpcOrder));
    // Step 3: Convert to number[] for React Native bridge
    // React Native bridge requires plain arrays, not typed arrays
    const bufferArray = audioBuffer instanceof Float32Array ? Array.from(audioBuffer) : audioBuffer;
    logDebug('Calling native module for formant extraction', {
        sampleRate,
        lpcOrder,
    });
    try {
        // Step 4: Call native module
        const nativeResult = await LoqaExpoDspModule.extractFormants(bufferArray, sampleRate, {
            lpcOrder,
        });
        logDebug('Native module returned formants result', {
            f1: nativeResult.f1,
            f2: nativeResult.f2,
            f3: nativeResult.f3,
            hasBandwidths: !!nativeResult.bandwidths,
        });
        // Step 5: Convert result to FormantsResult type
        // Native module returns dictionary/map, convert to proper TypeScript type
        const result = {
            f1: nativeResult.f1,
            f2: nativeResult.f2,
            f3: nativeResult.f3,
            bandwidths: {
                f1: nativeResult.bandwidths.f1,
                f2: nativeResult.bandwidths.f2,
                f3: nativeResult.bandwidths.f3,
            },
        };
        logDebug('extractFormants completed successfully', {
            f1: result.f1,
            f2: result.f2,
            f3: result.f3,
        });
        return result;
    }
    catch (error) {
        // Step 6: Wrap native errors in NativeModuleError with context
        const errorMessage = error instanceof Error ? error.message : String(error);
        logDebug('extractFormants failed', {
            error: errorMessage,
            sampleRate,
            lpcOrder,
            bufferLength: audioBuffer.length,
        });
        throw new NativeModuleError(`Formant extraction failed: ${errorMessage}`, {
            originalError: error,
            sampleRate,
            lpcOrder,
            bufferLength: audioBuffer.length,
        });
    }
}
//# sourceMappingURL=extractFormants.js.map