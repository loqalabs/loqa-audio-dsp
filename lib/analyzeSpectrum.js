// analyzeSpectrum - Spectral analysis API
import LoqaAudioDspModule from './LoqaAudioDspModule';
import { NativeModuleError } from './errors';
import { logDebug } from './utils';
import { validateAudioBuffer, validateSampleRate } from './validation';
/**
 * Analyzes spectral features (centroid, rolloff, tilt)
 *
 * This function computes spectral characteristics of audio data, including:
 * - Spectral centroid: "center of mass" of the spectrum (brightness measure)
 * - Spectral rolloff: frequency below which 95% of energy is concentrated
 * - Spectral tilt: overall slope of the spectrum (timbre indicator)
 *
 * All features are computed in a single call for efficiency.
 *
 * @param audioBuffer - Audio samples (Float32Array or number[])
 * @param sampleRate - Sample rate in Hz (8000-48000)
 * @param options - Spectrum analysis options
 * @returns Promise resolving to spectrum result with centroid, rolloff, and tilt
 * @throws ValidationError if buffer or sample rate are invalid
 * @throws NativeModuleError if native computation fails
 *
 * @example
 * ```typescript
 * const audioData = new Float32Array(2048);
 * // ... fill with audio samples ...
 *
 * const result = await analyzeSpectrum(audioData, 44100);
 *
 * console.log(`Spectral centroid: ${result.centroid} Hz`);
 * console.log(`Spectral rolloff: ${result.rolloff} Hz`);
 * console.log(`Spectral tilt: ${result.tilt}`);
 * ```
 */
export async function analyzeSpectrum(audioBuffer, sampleRate, options) {
    // Step 1: Validate audio buffer and sample rate
    logDebug('analyzeSpectrum called', {
        bufferLength: audioBuffer.length,
        bufferType: audioBuffer instanceof Float32Array ? 'Float32Array' : 'number[]',
        sampleRate,
        options,
    });
    validateAudioBuffer(audioBuffer);
    validateSampleRate(sampleRate);
    // Step 2: Convert to number[] for React Native bridge
    // React Native bridge requires plain arrays, not typed arrays
    const bufferArray = audioBuffer instanceof Float32Array ? Array.from(audioBuffer) : audioBuffer;
    logDebug('Calling native module for spectrum analysis', {
        sampleRate,
        bufferLength: bufferArray.length,
    });
    try {
        // Step 3: Call native module
        const nativeResult = await LoqaAudioDspModule.analyzeSpectrum(bufferArray, sampleRate, options || {});
        logDebug('Native module returned spectrum result', {
            centroid: nativeResult.centroid,
            rolloff: nativeResult.rolloff,
            tilt: nativeResult.tilt,
        });
        // Step 4: Convert result to SpectrumResult type
        // Native module returns dictionary/map, convert to proper TypeScript type
        const result = {
            centroid: nativeResult.centroid,
            rolloff: nativeResult.rolloff,
            tilt: nativeResult.tilt,
        };
        logDebug('analyzeSpectrum completed successfully', {
            centroid: result.centroid,
            rolloff: result.rolloff,
            tilt: result.tilt,
        });
        return result;
    }
    catch (error) {
        // Step 5: Wrap native errors in NativeModuleError with context
        const errorMessage = error instanceof Error ? error.message : String(error);
        logDebug('analyzeSpectrum failed', {
            error: errorMessage,
            sampleRate,
            bufferLength: audioBuffer.length,
        });
        throw new NativeModuleError(`Spectrum analysis failed: ${errorMessage}`, {
            originalError: error,
            sampleRate,
            bufferLength: audioBuffer.length,
        });
    }
}
//# sourceMappingURL=analyzeSpectrum.js.map