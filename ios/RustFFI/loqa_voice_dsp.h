// loqa_voice_dsp.h
// C header for Rust FFI functions exported by loqa-voice-dsp
// This header allows Swift to call Rust extern "C" functions with proper C ABI

#ifndef loqa_voice_dsp_h
#define loqa_voice_dsp_h

#include <stdint.h>
#include <stdbool.h>

// MARK: - Result Structs (matching Rust #[repr(C)] layout)

/// Pitch detection result
typedef struct {
    float frequency;
    float confidence;
    bool is_voiced;
} PitchResultC;

/// Formant extraction result
typedef struct {
    float f1;
    float f2;
    float f3;
    float bw1;
    float bw2;
    float bw3;
} FormantsResultC;

/// Spectrum analysis result
typedef struct {
    float centroid;
    float rolloff;
    float tilt;
} SpectrumResultC;

/// HNR (Harmonics-to-Noise Ratio) result
typedef struct {
    float hnr;
    float f0;
    bool is_voiced;
} HNRResultC;

/// H1-H2 (Harmonic Amplitude Difference) result
typedef struct {
    float h1h2;
    float h1_amplitude_db;
    float h2_amplitude_db;
    float f0;
} H1H2ResultC;

// MARK: - FFI Function Declarations

/// FFT computation with window type support
/// Returns pointer to FFT magnitudes (caller must free with free_fft_result_rust)
const float* compute_fft_rust(
    const float* buffer,
    int32_t length,
    int32_t fft_size,
    int32_t window_type
);

/// Free FFT result memory allocated by compute_fft_rust
void free_fft_result_rust(const float* ptr);

/// Pitch detection using YIN algorithm
/// Returns PitchResultC by value
PitchResultC detect_pitch_rust(
    const float* buffer,
    int32_t length,
    int32_t sample_rate
);

/// Formant extraction using LPC analysis
/// Returns FormantsResultC by value
FormantsResultC extract_formants_rust(
    const float* buffer,
    int32_t length,
    int32_t sample_rate,
    int32_t lpc_order
);

/// Spectrum analysis (centroid, rolloff, tilt)
/// Returns SpectrumResultC by value
SpectrumResultC analyze_spectrum_rust(
    const float* buffer,
    int32_t length,
    int32_t sample_rate
);

/// HNR calculation using Boersma's autocorrelation method
/// Returns HNRResultC by value
HNRResultC calculate_hnr_rust(
    const float* buffer,
    int32_t length,
    int32_t sample_rate,
    float min_freq,
    float max_freq
);

/// H1-H2 calculation for vocal weight analysis
/// Returns H1H2ResultC by value
H1H2ResultC calculate_h1h2_rust(
    const float* buffer,
    int32_t length,
    int32_t sample_rate,
    float f0
);

#endif /* loqa_voice_dsp_h */
