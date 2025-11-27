import type { HNROptions, HNRResult } from './types';
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
export declare function calculateHNR(audioBuffer: Float32Array | number[], options: HNROptions): Promise<HNRResult>;
//# sourceMappingURL=calculateHNR.d.ts.map