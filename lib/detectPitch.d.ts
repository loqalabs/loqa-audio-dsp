import type { PitchDetectionOptions, PitchResult } from './types';
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
export declare function detectPitch(audioBuffer: Float32Array | number[], sampleRate: number, options?: Partial<PitchDetectionOptions>): Promise<PitchResult>;
//# sourceMappingURL=detectPitch.d.ts.map