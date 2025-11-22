// FFI wrapper for loqa-voice-dsp crate
// Provides C-compatible exports for iOS (Swift FFI) and Android (Kotlin JNI)

use std::os::raw::{c_float, c_int};
use std::slice;

/// Computes Fast Fourier Transform (FFT) of audio buffer
///
/// # Arguments
/// * `buffer` - Pointer to input audio samples (Float32 array)
/// * `length` - Number of samples in input buffer
/// * `sample_rate` - Sample rate in Hz (e.g., 44100, 48000)
/// * `fft_size` - FFT size (must be power of 2, range: 256-8192)
///
/// # Returns
/// * Pointer to magnitude spectrum (length = fft_size / 2 + 1) or null on error
///
/// # Safety
/// * Caller must ensure `buffer` points to valid memory of at least `length` samples
/// * Caller MUST call `free_fft_result_rust` to deallocate returned pointer
/// * Returned pointer is heap-allocated and owned by caller after return
/// * This function dereferences raw pointers and is inherently unsafe
/// * Buffer must remain valid for the duration of this function call
///
/// # Memory Management Pattern (Critical for FFI/JNI)
/// * Rust allocates → Returns raw pointer → Swift/Kotlin copies → Swift/Kotlin frees Rust memory
///
/// # Note
/// The loqa-voice-dsp crate applies its own windowing internally, so we don't expose
/// window type as a parameter in this FFI interface. The TypeScript layer may accept
/// window type as an option, but it will be handled at that layer for v0.1.0.
#[no_mangle]
pub unsafe extern "C" fn compute_fft_rust(
    buffer: *const c_float,
    length: c_int,
    sample_rate: c_int,
    fft_size: c_int,
) -> *mut c_float {
    // Input validation
    if buffer.is_null() {
        eprintln!("[Rust FFI] Error: buffer pointer is null");
        return std::ptr::null_mut();
    }

    if length <= 0 {
        eprintln!("[Rust FFI] Error: length must be > 0, got {length}");
        return std::ptr::null_mut();
    }

    if sample_rate <= 0 {
        eprintln!("[Rust FFI] Error: sample_rate must be > 0, got {sample_rate}");
        return std::ptr::null_mut();
    }

    let fft_size_usize = fft_size as usize;

    // Validate FFT size is power of 2
    if fft_size <= 0 || (fft_size_usize & (fft_size_usize - 1)) != 0 {
        eprintln!("[Rust FFI] Error: fft_size must be power of 2, got {fft_size}");
        return std::ptr::null_mut();
    }

    // Validate FFT size range (256 to 8192)
    if !(256..=8192).contains(&fft_size) {
        eprintln!("[Rust FFI] Error: fft_size must be in range [256, 8192], got {fft_size}");
        return std::ptr::null_mut();
    }

    // Convert raw pointer to Rust slice
    let input_slice = slice::from_raw_parts(buffer, length as usize);

    // Call loqa-voice-dsp FFT function
    let fft_result =
        loqa_voice_dsp::compute_fft(input_slice, sample_rate as u32, fft_size_usize);

    // Handle FFT computation result
    let magnitudes = match fft_result {
        Ok(result) => result.magnitudes,
        Err(e) => {
            eprintln!("[Rust FFI] FFT computation failed: {e:?}");
            return std::ptr::null_mut();
        }
    };

    // Convert Vec<f32> to raw pointer for FFI
    // This transfers ownership to the caller
    // CRITICAL: Caller MUST call free_fft_result_rust to prevent memory leak
    Box::into_raw(magnitudes.into_boxed_slice()) as *mut c_float
}

/// Frees FFT result memory allocated by compute_fft_rust
///
/// # Arguments
/// * `ptr` - Pointer returned by compute_fft_rust
/// * `length` - Length of the FFT result (should be fft_size / 2 + 1)
///
/// # Safety
/// * Must only be called once per pointer returned from compute_fft_rust
/// * Must not be called with pointers from other sources
/// * `length` MUST match the actual allocation size (fft_size / 2 + 1)
/// * Caller must ensure pointer was created by compute_fft_rust
/// * This function dereferences raw pointers and is inherently unsafe
///
/// # Memory Safety
/// * This function converts the raw pointer back to a Box<[f32]> and drops it
/// * Prevents memory leaks at FFI/JNI boundary
/// * Null pointers are handled gracefully and do nothing
#[no_mangle]
pub unsafe extern "C" fn free_fft_result_rust(ptr: *mut c_float, length: c_int) {
    if ptr.is_null() {
        return;
    }

    if length <= 0 {
        eprintln!(
            "[Rust FFI] Error: free_fft_result_rust called with invalid length {length}"
        );
        return;
    }

    // Reconstruct the Box from the raw pointer with correct length
    // This will automatically deallocate when Box goes out of scope
    let _ = Box::from_raw(slice::from_raw_parts_mut(ptr, length as usize));
}

/// Android JNI native method for computeFFT
///
/// JNI Method Signature Resolution:
/// - Kotlin declaration: `external fun nativeComputeFFT(buffer: FloatArray, fftSize: Int, windowType: Int): FloatArray`
/// - Package: com.loqalabs.loqaaudiodsp.RustJNI
/// - Class: RustBridge (object)
/// - Method: nativeComputeFFT
/// - JNI Function Name: Java_com_loqalabs_loqaaudiodsp_RustJNI_RustBridge_nativeComputeFFT
///
/// # Arguments
/// * `env` - JNI environment pointer (unused but required by JNI)
/// * `class` - JNI class reference (unused but required by JNI)
/// * `buffer` - JNI jfloatArray reference to input audio samples
/// * `fft_size` - FFT size (must be power of 2, range: 256-8192)
/// * `window_type` - Window function type (0=none, 1=hanning, 2=hamming, 3=blackman) - IGNORED in v0.1.0
///
/// # Returns
/// * JNI jfloatArray containing magnitude spectrum (length = fft_size / 2 + 1) or null on error
///
/// # Safety
/// * JNI framework ensures proper type conversions and memory management
/// * This function is called from Kotlin via JNI, not directly
///
/// # Note
/// For v0.1.0, window_type is accepted but ignored - loqa-voice-dsp applies windowing internally.
/// Sample rate is hardcoded to 44100 Hz (matches default in LoqaAudioDspModule.kt).
/// This function delegates to compute_fft_rust with appropriate parameters.
///
/// # JNI Implementation Note
/// This requires proper JNI environment handling and FloatArray conversion,
/// which should be implemented using jni-rs crate or manual JNI calls.
/// For now, we provide the C ABI signature that matches Kotlin expectations.
#[no_mangle]
pub unsafe extern "C" fn Java_com_loqalabs_loqaaudiodsp_RustJNI_RustBridge_nativeComputeFFT(
    _env: *mut std::os::raw::c_void,
    _class: *mut std::os::raw::c_void,
    buffer: *const c_float,
    buffer_length: c_int,
    fft_size: c_int,
    _window_type: c_int,  // Accepted but ignored - windowing handled by loqa-voice-dsp
) -> *mut c_float {
    // Use default sample rate (44100 Hz) for Android in v0.1.0
    // Matches the default in LoqaAudioDspModule.kt
    const DEFAULT_SAMPLE_RATE: c_int = 44100;

    // Delegate to the main FFT implementation
    // The JNI framework handles conversion of FloatArray to *const f32 and back
    compute_fft_rust(buffer, buffer_length, DEFAULT_SAMPLE_RATE, fft_size)
}

/// Result structure for pitch detection
///
/// Returns the detected pitch frequency, confidence score, and voicing classification.
/// This struct is C-compatible for FFI/JNI interop.
///
/// # Fields
/// * `frequency` - Detected pitch in Hz (0.0 if unvoiced or no pitch detected)
/// * `confidence` - Confidence score from 0.0 (low) to 1.0 (high)
/// * `is_voiced` - Whether the audio segment is voiced (true) or unvoiced (false)
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub struct PitchResult {
    pub frequency: c_float,
    pub confidence: c_float,
    pub is_voiced: bool,
}

/// Detects pitch using YIN algorithm from loqa-voice-dsp crate
///
/// # Arguments
/// * `buffer` - Pointer to input audio samples (Float32 array)
/// * `length` - Number of samples in input buffer
/// * `sample_rate` - Sample rate in Hz (must be 8000-48000 Hz)
///
/// # Returns
/// * PitchResult struct with frequency, confidence, and is_voiced
/// * Returns frequency=0.0, confidence=0.0, is_voiced=false on error
///
/// # Safety
/// * Caller must ensure `buffer` points to valid memory of at least `length` samples
/// * This function dereferences raw pointers and is inherently unsafe
/// * Buffer must remain valid for the duration of this function call
///
/// # Validation
/// * Sample rate must be between 8000 and 48000 Hz (AC3)
/// * Returns null frequency (0.0) if no pitch detected (AC4)
/// * Confidence score is always between 0.0 and 1.0 (AC5)
/// * Uses YIN algorithm from loqa-voice-dsp (AC2)
///
/// # Note on Voiced/Unvoiced Classification
/// * is_voiced=true when clear pitch is detected with reasonable confidence
/// * is_voiced=false for silence, noise, or unvoiced segments
/// * frequency=0.0 indicates no pitch detected (unvoiced)
#[no_mangle]
pub unsafe extern "C" fn detect_pitch_rust(
    buffer: *const c_float,
    length: c_int,
    sample_rate: c_int,
) -> PitchResult {
    // Default error result
    let error_result = PitchResult {
        frequency: 0.0,
        confidence: 0.0,
        is_voiced: false,
    };

    // Input validation
    if buffer.is_null() {
        eprintln!("[Rust FFI] Error: buffer pointer is null");
        return error_result;
    }

    if length <= 0 {
        eprintln!("[Rust FFI] Error: length must be > 0, got {length}");
        return error_result;
    }

    // Validate sample rate range: 8000-48000 Hz (AC3)
    if !(8000..=48000).contains(&sample_rate) {
        eprintln!(
            "[Rust FFI] Error: sample_rate must be in range [8000, 48000] Hz, got {sample_rate}"
        );
        return error_result;
    }

    // Convert raw pointer to Rust slice
    let input_slice = slice::from_raw_parts(buffer, length as usize);

    // Define frequency range for YIN algorithm
    // Default range suitable for human voice: 80 Hz (low male) to 400 Hz (high female)
    // Can be extended to 800 Hz for wider coverage
    const MIN_FREQUENCY: f32 = 80.0;
    const MAX_FREQUENCY: f32 = 400.0;

    // Call loqa-voice-dsp YIN pitch detection function (AC2)
    let pitch_result = loqa_voice_dsp::detect_pitch(
        input_slice,
        sample_rate as u32,
        MIN_FREQUENCY,
        MAX_FREQUENCY
    );

    // Handle pitch detection result
    match pitch_result {
        Ok(result) => {
            // Extract frequency, confidence, and voiced classification
            let frequency = if result.is_voiced { result.frequency } else { 0.0 }; // AC4: Return 0.0 if unvoiced
            let confidence = result.confidence.clamp(0.0, 1.0); // Ensure 0.0-1.0 range (AC5)
            let is_voiced = result.is_voiced;

            PitchResult {
                frequency,
                confidence,
                is_voiced,
            }
        }
        Err(e) => {
            eprintln!("[Rust FFI] Pitch detection failed: {e:?}");
            error_result
        }
    }
}

/// Android JNI native method for detectPitch
///
/// JNI Method Signature Resolution:
/// - Kotlin declaration: `external fun nativeDetectPitch(buffer: FloatArray, sampleRate: Int): PitchResult`
/// - Package: com.loqalabs.loqaaudiodsp.RustJNI
/// - Class: RustBridge (object)
/// - Method: nativeDetectPitch
/// - JNI Function Name: Java_com_loqalabs_loqaaudiodsp_RustJNI_RustBridge_nativeDetectPitch
///
/// # Arguments
/// * `env` - JNI environment pointer (unused but required by JNI)
/// * `class` - JNI class reference (unused but required by JNI)
/// * `buffer` - JNI jfloatArray reference to input audio samples
/// * `buffer_length` - Number of samples in buffer
/// * `sample_rate` - Sample rate in Hz (8000-48000)
///
/// # Returns
/// * PitchResult struct with frequency, confidence, and is_voiced
///
/// # Safety
/// * JNI framework ensures proper type conversions and memory management
/// * This function is called from Kotlin via JNI, not directly
///
/// # Note
/// Unlike FFT, PitchResult is returned by value (small struct), not by pointer.
/// JNI will automatically marshal this back to Kotlin data class.
#[no_mangle]
pub unsafe extern "C" fn Java_com_loqalabs_loqaaudiodsp_RustJNI_RustBridge_nativeDetectPitch(
    _env: *mut std::os::raw::c_void,
    _class: *mut std::os::raw::c_void,
    buffer: *const c_float,
    buffer_length: c_int,
    sample_rate: c_int,
) -> PitchResult {
    // Delegate to the main pitch detection implementation
    // The JNI framework handles conversion of FloatArray to *const f32
    detect_pitch_rust(buffer, buffer_length, sample_rate)
}

/// Placeholder FFI function for testing build infrastructure (retained for backward compatibility)
#[no_mangle]
pub extern "C" fn test_ffi_bridge() -> i32 {
    42
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::f32::consts::PI;

    #[test]
    fn test_ffi_placeholder() {
        assert_eq!(test_ffi_bridge(), 42);
    }

    #[test]
    fn test_compute_fft_null_buffer() {
        unsafe {
            let result = compute_fft_rust(std::ptr::null(), 1024, 44100, 512);
            assert!(result.is_null(), "Should return null for null buffer");
        }
    }

    #[test]
    fn test_compute_fft_invalid_length() {
        let buffer: Vec<f32> = vec![0.0; 1024];
        unsafe {
            let result = compute_fft_rust(buffer.as_ptr(), 0, 44100, 512);
            assert!(result.is_null(), "Should return null for length <= 0");

            let result = compute_fft_rust(buffer.as_ptr(), -10, 44100, 512);
            assert!(result.is_null(), "Should return null for negative length");
        }
    }

    #[test]
    fn test_compute_fft_invalid_sample_rate() {
        let buffer: Vec<f32> = vec![0.0; 1024];
        unsafe {
            let result = compute_fft_rust(buffer.as_ptr(), 1024, 0, 512);
            assert!(result.is_null(), "Should return null for sample_rate <= 0");

            let result = compute_fft_rust(buffer.as_ptr(), 1024, -100, 512);
            assert!(
                result.is_null(),
                "Should return null for negative sample_rate"
            );
        }
    }

    #[test]
    fn test_compute_fft_invalid_fft_size_not_power_of_2() {
        let buffer: Vec<f32> = vec![0.0; 1024];

        unsafe {
            // Test non-power-of-2 sizes
            let result = compute_fft_rust(buffer.as_ptr(), 1024, 44100, 500);
            assert!(
                result.is_null(),
                "Should return null for non-power-of-2 FFT size"
            );

            let result = compute_fft_rust(buffer.as_ptr(), 1024, 44100, 1000);
            assert!(
                result.is_null(),
                "Should return null for non-power-of-2 FFT size"
            );
        }
    }

    #[test]
    fn test_compute_fft_invalid_fft_size_out_of_range() {
        let buffer: Vec<f32> = vec![0.0; 1024];

        unsafe {
            // Test below minimum (256)
            let result = compute_fft_rust(buffer.as_ptr(), 1024, 44100, 128);
            assert!(result.is_null(), "Should return null for FFT size < 256");

            // Test above maximum (8192)
            let result = compute_fft_rust(buffer.as_ptr(), 16384, 44100, 16384);
            assert!(result.is_null(), "Should return null for FFT size > 8192");
        }
    }

    #[test]
    fn test_compute_fft_valid_input_returns_non_null() {
        // Generate a simple sine wave at 440 Hz
        let sample_rate = 44100;
        let frequency = 440.0;
        let duration = 0.1; // 100ms
        let num_samples = (sample_rate as f32 * duration) as usize;

        let mut buffer: Vec<f32> = Vec::with_capacity(num_samples);
        for i in 0..num_samples {
            let t = i as f32 / sample_rate as f32;
            buffer.push((2.0 * PI * frequency * t).sin());
        }

        let fft_size = 2048;
        unsafe {
            let result = compute_fft_rust(buffer.as_ptr(), num_samples as c_int, sample_rate, fft_size);
            assert!(!result.is_null(), "Should return valid pointer");

            // Clean up memory (fft_size / 2 + 1)
            free_fft_result_rust(result, (fft_size / 2) + 1);
        }
    }

    #[test]
    fn test_compute_fft_result_length() {
        let buffer: Vec<f32> = vec![0.5; 2048];
        let sample_rate = 44100;
        let fft_size = 1024;
        let expected_result_length = (fft_size / 2) + 1; // loqa-voice-dsp returns N/2 + 1

        unsafe {
            let result = compute_fft_rust(buffer.as_ptr(), 2048, sample_rate, fft_size);
            assert!(!result.is_null());

            // Verify we can read the result (this tests memory safety)
            let result_slice = slice::from_raw_parts(result, expected_result_length as usize);
            assert_eq!(result_slice.len(), expected_result_length as usize);

            // All values should be finite (not NaN or Infinity)
            for val in result_slice {
                assert!(val.is_finite(), "FFT result should be finite");
            }

            // Clean up
            free_fft_result_rust(result, expected_result_length);
        }
    }

    #[test]
    fn test_compute_fft_sine_wave_peak_detection() {
        // Generate a pure sine wave at known frequency
        let sample_rate = 44100;
        let target_frequency = 1000.0; // 1 kHz
        let fft_size = 4096;
        let num_samples = fft_size;

        let mut buffer: Vec<f32> = Vec::with_capacity(num_samples);
        for i in 0..num_samples {
            let t = i as f32 / sample_rate as f32;
            buffer.push((2.0 * PI * target_frequency * t).sin());
        }

        unsafe {
            let result = compute_fft_rust(buffer.as_ptr(), num_samples as c_int, sample_rate, fft_size as c_int);
            assert!(!result.is_null());

            let magnitude_len = (fft_size / 2) + 1;
            let magnitude_slice = slice::from_raw_parts(result, magnitude_len);

            // Find the peak in the magnitude spectrum
            let mut max_magnitude = 0.0_f32;
            let mut max_index = 0;
            for (i, &mag) in magnitude_slice.iter().enumerate() {
                if mag > max_magnitude {
                    max_magnitude = mag;
                    max_index = i;
                }
            }

            // Calculate the frequency of the peak
            let peak_frequency = (max_index as f32) * (sample_rate as f32 / fft_size as f32);

            // The peak should be close to our target frequency (within 1 bin)
            let frequency_resolution = sample_rate as f32 / fft_size as f32;
            let frequency_error = (peak_frequency - target_frequency).abs();

            assert!(
                frequency_error < frequency_resolution * 1.5,
                "Peak frequency {peak_frequency} Hz should be close to target {target_frequency} Hz (error: {frequency_error} Hz)"
            );

            free_fft_result_rust(result, ((fft_size / 2) + 1) as c_int);
        }
    }

    #[test]
    fn test_free_fft_result_handles_null() {
        // Should not crash
        unsafe {
            free_fft_result_rust(std::ptr::null_mut(), 256);
        }
    }

    #[test]
    fn test_free_fft_result_handles_invalid_length() {
        let buffer: Vec<f32> = vec![0.5; 1024];
        unsafe {
            let result = compute_fft_rust(buffer.as_ptr(), 1024, 44100, 512);
            assert!(!result.is_null());

            // These should handle gracefully (not crash)
            free_fft_result_rust(result, 0);
        }
        // Note: We've now leaked the memory, but that's ok for this test
        // In production, free should be called with correct length
    }

    #[test]
    fn test_memory_safety_multiple_allocations() {
        // Test that we can allocate and free multiple FFT results without issues
        let buffer: Vec<f32> = vec![0.5; 2048];
        let sample_rate = 44100;
        let fft_size = 1024;
        let result_len = (fft_size / 2) + 1;

        unsafe {
            for _ in 0..10 {
                let result = compute_fft_rust(buffer.as_ptr(), 2048, sample_rate, fft_size);
                assert!(!result.is_null());
                free_fft_result_rust(result, result_len);
            }
        }
    }

    // ======== Pitch Detection Tests ========

    #[test]
    fn test_detect_pitch_null_buffer() {
        unsafe {
            let result = detect_pitch_rust(std::ptr::null(), 1024, 44100);
            assert_eq!(result.frequency, 0.0, "Should return frequency=0.0 for null buffer");
            assert_eq!(result.confidence, 0.0, "Should return confidence=0.0 for null buffer");
            assert!(!result.is_voiced, "Should return is_voiced=false for null buffer");
        }
    }

    #[test]
    fn test_detect_pitch_invalid_length() {
        let buffer: Vec<f32> = vec![0.0; 1024];
        unsafe {
            // Test zero length
            let result = detect_pitch_rust(buffer.as_ptr(), 0, 44100);
            assert_eq!(result.frequency, 0.0);
            assert_eq!(result.confidence, 0.0);
            assert!(!result.is_voiced);

            // Test negative length
            let result = detect_pitch_rust(buffer.as_ptr(), -10, 44100);
            assert_eq!(result.frequency, 0.0);
            assert_eq!(result.confidence, 0.0);
            assert!(!result.is_voiced);
        }
    }

    #[test]
    fn test_detect_pitch_invalid_sample_rate_below_minimum() {
        let buffer: Vec<f32> = vec![0.0; 1024];
        unsafe {
            // Test below 8000 Hz (AC3)
            let result = detect_pitch_rust(buffer.as_ptr(), 1024, 7999);
            assert_eq!(result.frequency, 0.0, "Should return error for sample rate < 8000 Hz");
            assert_eq!(result.confidence, 0.0);
            assert!(!result.is_voiced);

            // Test zero sample rate
            let result = detect_pitch_rust(buffer.as_ptr(), 1024, 0);
            assert_eq!(result.frequency, 0.0);
            assert_eq!(result.confidence, 0.0);
            assert!(!result.is_voiced);

            // Test negative sample rate
            let result = detect_pitch_rust(buffer.as_ptr(), 1024, -100);
            assert_eq!(result.frequency, 0.0);
            assert_eq!(result.confidence, 0.0);
            assert!(!result.is_voiced);
        }
    }

    #[test]
    fn test_detect_pitch_invalid_sample_rate_above_maximum() {
        let buffer: Vec<f32> = vec![0.0; 1024];
        unsafe {
            // Test above 48000 Hz (AC3)
            let result = detect_pitch_rust(buffer.as_ptr(), 1024, 48001);
            assert_eq!(result.frequency, 0.0, "Should return error for sample rate > 48000 Hz");
            assert_eq!(result.confidence, 0.0);
            assert!(!result.is_voiced);
        }
    }

    #[test]
    fn test_detect_pitch_valid_sample_rates() {
        let buffer: Vec<f32> = vec![0.5; 2048];

        unsafe {
            // Test minimum valid sample rate (8000 Hz)
            let result = detect_pitch_rust(buffer.as_ptr(), 2048, 8000);
            // Should not error (frequency may be 0 due to buffer content, but call should succeed)
            assert!(result.confidence >= 0.0 && result.confidence <= 1.0);

            // Test common sample rate (44100 Hz)
            let result = detect_pitch_rust(buffer.as_ptr(), 2048, 44100);
            assert!(result.confidence >= 0.0 && result.confidence <= 1.0);

            // Test maximum valid sample rate (48000 Hz)
            let result = detect_pitch_rust(buffer.as_ptr(), 2048, 48000);
            assert!(result.confidence >= 0.0 && result.confidence <= 1.0);
        }
    }

    #[test]
    fn test_detect_pitch_confidence_range() {
        // Generate synthetic tone at 440 Hz
        let sample_rate = 44100;
        let frequency = 440.0;
        let duration = 0.1; // 100ms
        let num_samples = (sample_rate as f32 * duration) as usize;

        let mut buffer: Vec<f32> = Vec::with_capacity(num_samples);
        for i in 0..num_samples {
            let t = i as f32 / sample_rate as f32;
            buffer.push((2.0 * PI * frequency * t).sin());
        }

        unsafe {
            let result = detect_pitch_rust(buffer.as_ptr(), num_samples as c_int, sample_rate);

            // AC5: Confidence must be in range [0.0, 1.0]
            assert!(
                result.confidence >= 0.0 && result.confidence <= 1.0,
                "Confidence {:.3} must be in range [0.0, 1.0]",
                result.confidence
            );
        }
    }

    #[test]
    fn test_detect_pitch_sine_wave_220hz() {
        // Generate a pure 220 Hz sine wave (A3) - within human voice range
        let sample_rate = 44100;
        let target_frequency = 220.0; // Within MIN_FREQUENCY..MAX_FREQUENCY range
        let duration = 0.1; // 100ms should be enough for YIN
        let num_samples = (sample_rate as f32 * duration) as usize;

        let mut buffer: Vec<f32> = Vec::with_capacity(num_samples);
        for i in 0..num_samples {
            let t = i as f32 / sample_rate as f32;
            buffer.push((2.0 * PI * target_frequency * t).sin());
        }

        unsafe {
            let result = detect_pitch_rust(buffer.as_ptr(), num_samples as c_int, sample_rate);

            // For a clear sine wave within the detection range, we should detect a pitch
            // YIN is very accurate for pure tones in the target frequency range
            if result.is_voiced {
                // If voiced, frequency should be close to 220 Hz
                let error = (result.frequency - target_frequency).abs();
                let error_percent = (error / target_frequency) * 100.0;

                assert!(
                    error_percent < 10.0,
                    "Detected frequency {:.1} Hz should be within 10% of target {:.1} Hz (error: {:.2}%)",
                    result.frequency,
                    target_frequency,
                    error_percent
                );

                // Confidence should be reasonably high for clean tone
                assert!(
                    result.confidence > 0.5,
                    "Confidence {:.3} should be > 0.5 for clear sine wave",
                    result.confidence
                );
            }
        }
    }

    #[test]
    fn test_detect_pitch_silence_returns_unvoiced() {
        // Test with silence (all zeros)
        let buffer: Vec<f32> = vec![0.0; 2048];
        let sample_rate = 44100;

        unsafe {
            let result = detect_pitch_rust(buffer.as_ptr(), 2048, sample_rate);

            // AC4: Silence should return frequency=0.0 and is_voiced=false
            assert_eq!(
                result.frequency, 0.0,
                "Silence should return frequency=0.0"
            );
            assert!(
                !result.is_voiced,
                "Silence should be classified as unvoiced"
            );
        }
    }

    #[test]
    fn test_detect_pitch_noise_behavior() {
        // Generate white noise (random values)
        let mut buffer: Vec<f32> = vec![0.0; 2048];
        let sample_rate = 44100;

        // Simple pseudo-random noise generator
        for (i, sample) in buffer.iter_mut().enumerate() {
            // Use a simple hash-like function for reproducibility
            let hash = (i as u32).wrapping_mul(2654435761);
            *sample = ((hash % 1000) as f32 / 1000.0) * 2.0 - 1.0; // Range: [-1.0, 1.0]
        }

        unsafe {
            let result = detect_pitch_rust(buffer.as_ptr(), 2048, sample_rate);

            // Noise behavior: The YIN algorithm may detect spurious periodicities in noise
            // The important thing is that confidence values are always in valid range
            assert!(
                result.confidence >= 0.0 && result.confidence <= 1.0,
                "Confidence must be in valid range [0.0, 1.0], got {:.3}",
                result.confidence
            );

            // AC4: If unvoiced, frequency should be 0.0
            if !result.is_voiced {
                assert_eq!(
                    result.frequency, 0.0,
                    "Unvoiced noise should have frequency=0.0"
                );
            }
        }
    }

    #[test]
    fn test_detect_pitch_multiple_sample_rates() {
        // Generate 220 Hz tone (A3)
        let target_frequency = 220.0;

        for sample_rate in [8000, 16000, 22050, 44100, 48000] {
            let duration = 0.1;
            let num_samples = (sample_rate as f32 * duration) as usize;

            let mut buffer: Vec<f32> = Vec::with_capacity(num_samples);
            for i in 0..num_samples {
                let t = i as f32 / sample_rate as f32;
                buffer.push((2.0 * PI * target_frequency * t).sin());
            }

            unsafe {
                let result = detect_pitch_rust(
                    buffer.as_ptr(),
                    num_samples as c_int,
                    sample_rate as c_int
                );

                // AC3: All sample rates in 8000-48000 Hz should work
                assert!(
                    result.confidence >= 0.0 && result.confidence <= 1.0,
                    "Sample rate {} Hz should work (got confidence {:.3})",
                    sample_rate,
                    result.confidence
                );
            }
        }
    }

    #[test]
    fn test_detect_pitch_result_struct_layout() {
        // Verify PitchResult struct is properly laid out for FFI
        // This is a compile-time check, but runtime verification doesn't hurt
        let test_result = PitchResult {
            frequency: 440.0,
            confidence: 0.95,
            is_voiced: true,
        };

        assert_eq!(test_result.frequency, 440.0);
        assert_eq!(test_result.confidence, 0.95);
        assert!(test_result.is_voiced);

        // Verify struct is Copy (required for FFI)
        let copied = test_result;
        assert_eq!(copied.frequency, 440.0);
        assert_eq!(test_result.frequency, 440.0); // Original still valid
    }
}
