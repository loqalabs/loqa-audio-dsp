package com.loqalabs.loqaaudiodsp.RustJNI

/**
 * PitchResult data class matching Rust #[repr(C)] PitchResult struct.
 * Returned by value from nativeDetectPitch JNI function.
 */
data class PitchResult(
    val frequency: Float,
    val confidence: Float,
    val isVoiced: Boolean
)

/**
 * FormantsResult data class matching Rust #[repr(C)] FormantsResult struct.
 * Returned by value from nativeExtractFormants JNI function.
 */
data class FormantsResult(
    val f1: Float,
    val f2: Float,
    val f3: Float,
    val bw1: Float,
    val bw2: Float,
    val bw3: Float
)

/**
 * RustBridge provides JNI bindings to the Rust loqa-voice-dsp library.
 *
 * This class handles:
 * - Loading the native Rust library (libloqa_voice_dsp.so)
 * - Declaring external JNI function signatures for Rust DSP functions
 * - Providing Kotlin wrapper functions with error handling
 *
 * Memory Management Notes:
 * - JNI automatically handles FloatArray marshalling for primitive arrays
 * - Structs (PitchResult, FormantsResult) are returned by value (no heap allocation)
 * - No manual memory management needed (simpler than iOS FFI)
 * - All native calls are wrapped in try-catch for error handling
 *
 * Implementation Status:
 * - Story 1.4: Placeholder JNI function declarations
 * - Story 2.3: Real FFT implementation
 * - Story 3.3: Pitch and formant implementations (this story)
 * - Story 4.2: Spectrum analysis implementation
 */
object RustBridge {
    init {
        try {
            System.loadLibrary("loqa_voice_dsp")
        } catch (e: UnsatisfiedLinkError) {
            throw RuntimeException(
                "Failed to load native library 'loqa_voice_dsp'. " +
                "Ensure the .so files are included in jniLibs directory.",
                e
            )
        }
    }

    // ============================================================================
    // External JNI Function Declarations (C ABI from Rust)
    // ============================================================================

    /**
     * JNI native function for FFT computation.
     *
     * Implemented in Story 2.3. Maps to Rust function:
     * Java_com_loqalabs_loqaaudiodsp_RustJNI_RustBridge_nativeComputeFFT
     *
     * This external function is resolved by JNI to the Rust implementation in lib.rs.
     * The Rust function delegates to compute_fft_rust with a default sample rate of 44100 Hz.
     *
     * @param buffer Input audio samples as FloatArray (JNI auto-converts to *const f32)
     * @param fftSize FFT size (must be power of 2, range 256-8192)
     * @param windowType Window function type (0=none, 1=hanning, 2=hamming, 3=blackman) - ignored in v0.1.0
     * @return Magnitude spectrum as FloatArray (length = fftSize / 2 + 1)
     */
    external fun nativeComputeFFT(
        buffer: FloatArray,
        fftSize: Int,
        windowType: Int
    ): FloatArray

    /**
     * JNI native function for pitch detection.
     *
     * Implemented in Story 3.1/3.3. Maps to Rust function:
     * Java_com_loqalabs_loqaaudiodsp_RustJNI_RustBridge_nativeDetectPitch
     *
     * This external function is resolved by JNI to the Rust implementation in lib.rs.
     * The Rust function uses YIN algorithm for pitch detection.
     *
     * @param buffer Input audio samples as FloatArray (JNI auto-converts to *const f32)
     * @param sampleRate Sample rate in Hz (8000-48000)
     * @return PitchResult struct with frequency, confidence, and isVoiced
     */
    external fun nativeDetectPitch(
        buffer: FloatArray,
        sampleRate: Int
    ): PitchResult

    /**
     * JNI native function for formant extraction.
     *
     * Implemented in Story 3.2/3.3. Maps to Rust function:
     * Java_com_loqalabs_loqaaudiodsp_RustJNI_RustBridge_nativeExtractFormants
     *
     * This external function is resolved by JNI to the Rust implementation in lib.rs.
     * The Rust function uses LPC analysis for formant extraction.
     *
     * @param buffer Input audio samples as FloatArray (JNI auto-converts to *const f32)
     * @param sampleRate Sample rate in Hz (8000-48000)
     * @param lpcOrder LPC order (0 for default: sampleRate / 1000 + 2)
     * @return FormantsResult struct with f1, f2, f3 and bandwidths
     */
    external fun nativeExtractFormants(
        buffer: FloatArray,
        sampleRate: Int,
        lpcOrder: Int
    ): FormantsResult

    /**
     * Placeholder JNI function for spectral analysis.
     *
     * This will be implemented in Story 4.1/4.2 to call Rust's analyze_spectrum_rust.
     *
     * @param buffer Input audio samples as FloatArray
     * @param sampleRate Sample rate in Hz
     * @return Map with keys: "centroid" (Float), "rolloff" (Float), "tilt" (Float)
     */
    external fun nativeAnalyzeSpectrum(
        buffer: FloatArray,
        sampleRate: Int
    ): Map<String, Any>

    // ============================================================================
    // Kotlin Wrapper Functions with Error Handling
    // ============================================================================

    /**
     * Computes FFT on audio buffer with error handling.
     *
     * Implemented in Story 2.3. This wrapper calls the native JNI function and
     * provides Kotlin-friendly error handling.
     *
     * @param buffer Input audio samples
     * @param fftSize FFT size (power of 2, range 256-8192)
     * @param windowType Window function type (0=none, 1=hanning, 2=hamming, 3=blackman) - ignored in v0.1.0
     * @return Magnitude spectrum (length = fftSize / 2 + 1)
     * @throws RuntimeException if JNI call fails
     */
    fun computeFFT(buffer: FloatArray, fftSize: Int, windowType: Int): FloatArray {
        return try {
            nativeComputeFFT(buffer, fftSize, windowType)
        } catch (e: Exception) {
            throw RuntimeException("JNI call to nativeComputeFFT failed", e)
        }
    }

    /**
     * Detects pitch from audio buffer with error handling.
     *
     * Implemented in Story 3.3. This wrapper calls the native JNI function and
     * provides Kotlin-friendly error handling.
     *
     * @param buffer Input audio samples
     * @param sampleRate Sample rate in Hz (8000-48000)
     * @return PitchResult with frequency, confidence, and isVoiced
     * @throws RuntimeException if JNI call fails
     */
    fun detectPitch(
        buffer: FloatArray,
        sampleRate: Int
    ): PitchResult {
        return try {
            nativeDetectPitch(buffer, sampleRate)
        } catch (e: Exception) {
            throw RuntimeException("JNI call to nativeDetectPitch failed", e)
        }
    }

    /**
     * Extracts formants from audio buffer with error handling.
     *
     * Implemented in Story 3.3. This wrapper calls the native JNI function and
     * provides Kotlin-friendly error handling.
     *
     * @param buffer Input audio samples
     * @param sampleRate Sample rate in Hz (8000-48000)
     * @param lpcOrder LPC order (0 for default: sampleRate / 1000 + 2)
     * @return FormantsResult with f1, f2, f3 and bandwidths
     * @throws RuntimeException if JNI call fails
     */
    fun extractFormants(
        buffer: FloatArray,
        sampleRate: Int,
        lpcOrder: Int
    ): FormantsResult {
        return try {
            nativeExtractFormants(buffer, sampleRate, lpcOrder)
        } catch (e: Exception) {
            throw RuntimeException("JNI call to nativeExtractFormants failed", e)
        }
    }

    /**
     * Analyzes spectral features of audio buffer with error handling.
     *
     * Note: This is a placeholder wrapper. Full implementation in Story 4.2.
     *
     * @param buffer Input audio samples
     * @param sampleRate Sample rate in Hz
     * @return Spectral analysis result
     * @throws RuntimeException if JNI call fails
     */
    fun analyzeSpectrum(buffer: FloatArray, sampleRate: Int): Map<String, Any> {
        return try {
            nativeAnalyzeSpectrum(buffer, sampleRate)
        } catch (e: Exception) {
            throw RuntimeException("JNI call to nativeAnalyzeSpectrum failed", e)
        }
    }
}
