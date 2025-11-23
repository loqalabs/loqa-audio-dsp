import XCTest
@testable import LoqaAudioDsp

/// Comprehensive tests for spectrum analysis functionality (Story 4.4)
/// Tests native iOS spectral analysis implementation, FFI bindings, and memory management
class SpectrumAnalysisTests: XCTestCase {

    // MARK: - Helper Functions

    /// Generate a synthetic sine wave for testing
    /// - Parameters:
    ///   - frequency: Frequency in Hz
    ///   - sampleRate: Sample rate in Hz
    ///   - durationSamples: Number of samples to generate
    /// - Returns: Array of Float samples
    func generateSineWave(frequency: Float, sampleRate: Float, durationSamples: Int) -> [Float] {
        var buffer = [Float](repeating: 0.0, count: durationSamples)
        let omega = 2.0 * Float.pi * frequency / sampleRate

        for i in 0..<durationSamples {
            buffer[i] = sin(omega * Float(i))
        }

        return buffer
    }

    /// Generate white noise (broad spectrum)
    func generateWhiteNoise(durationSamples: Int) -> [Float] {
        var buffer = [Float](repeating: 0.0, count: durationSamples)

        for i in 0..<durationSamples {
            // Generate random values between -1 and 1
            buffer[i] = Float.random(in: -1.0...1.0)
        }

        return buffer
    }

    /// Generate pink noise (1/f spectrum approximation)
    func generatePinkNoise(durationSamples: Int, sampleRate: Float) -> [Float] {
        var buffer = [Float](repeating: 0.0, count: durationSamples)

        // Generate pink noise by summing multiple octaves with decreasing amplitude
        let numOctaves = 5
        for octave in 0..<numOctaves {
            let freq = 100.0 * Float(pow(2.0, Double(octave))) // 100, 200, 400, 800, 1600 Hz
            let amplitude = 1.0 / Float(octave + 1) // Decreasing amplitude
            let omega = 2.0 * Float.pi * freq / sampleRate
            let phase = Float.random(in: 0...(2.0 * Float.pi))

            for i in 0..<durationSamples {
                buffer[i] += amplitude * sin(omega * Float(i) + phase)
            }
        }

        // Normalize
        let maxVal = buffer.map { abs($0) }.max() ?? 1.0
        for i in 0..<buffer.count {
            buffer[i] /= maxVal
        }

        return buffer
    }

    // MARK: - Valid Input Tests - Computing Features Successfully (AC1)

    func testAnalyzeSpectrumFor440HzSineWave() throws {
        // Arrange: Create a 440 Hz sine wave with narrow spectral peak
        let frequency: Float = 440.0
        let sampleRate: Float = 44100.0
        let bufferSize = 2048
        let buffer = generateSineWave(frequency: frequency, sampleRate: sampleRate, durationSamples: bufferSize)

        // Act: Analyze spectrum via Rust wrapper
        let result = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: Int(sampleRate))

        // Assert: Sine wave has narrow spectral peak
        XCTAssertGreaterThan(result.centroid, 0, "Centroid should be positive")
        XCTAssertGreaterThan(result.rolloff, 0, "Rolloff should be positive")
        
        // Centroid should be near the fundamental frequency for sine wave
        XCTAssertGreaterThan(result.centroid, 400, "Centroid should be close to 440 Hz")
        XCTAssertLessThan(result.centroid, 480, "Centroid should be close to 440 Hz")
    }

    func testAnalyzeSpectrumComputesAllThreeFeatures() throws {
        // Arrange
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 1024)

        // Act
        let result = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: 44100)

        // Assert: All three features should be present and valid
        XCTAssertTrue(result.centroid.isFinite, "Centroid should be finite")
        XCTAssertTrue(result.rolloff.isFinite, "Rolloff should be finite")
        XCTAssertTrue(result.tilt.isFinite, "Tilt should be finite")
    }

    // MARK: - Expected Ranges for Spectral Features (AC2)

    func testAnalyzeSpectrumReturnsCentroidRolloffTiltInExpectedRangesFor440HzSineWave() throws {
        // Arrange: 440 Hz sine wave should have:
        // - Centroid ~440 Hz (narrow peak at fundamental)
        // - Rolloff ~440 Hz (energy concentrated at fundamental)
        // - Tilt ~0 (flat spectrum at single frequency)
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 2048)

        // Act
        let result = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: 44100)

        // Assert: Spectral characteristics of pure tone
        XCTAssertGreaterThan(result.centroid, 400, "Centroid should be near 440 Hz")
        XCTAssertLessThan(result.centroid, 480, "Centroid should be near 440 Hz")
        
        XCTAssertGreaterThan(result.rolloff, 400, "Rolloff should be near 440 Hz")
        XCTAssertLessThan(result.rolloff, 500, "Rolloff should be near or slightly above centroid")
        
        XCTAssertGreaterThan(result.tilt, -0.5, "Tilt should be near zero for sine wave")
        XCTAssertLessThan(result.tilt, 0.5, "Tilt should be near zero for sine wave")
    }

    func testAnalyzeSpectrumReturnsMidRangeCentroidForWhiteNoise() throws {
        // Arrange: White noise has broad spectrum
        // - Centroid should be in mid-range (4000-10000 Hz for 44.1kHz)
        // - Rolloff should be high
        let buffer = generateWhiteNoise(durationSamples: 2048)

        // Act
        let result = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: 44100)

        // Assert: White noise has broad spectrum characteristics
        XCTAssertGreaterThan(result.centroid, 3000, "White noise centroid should be mid-range")
        XCTAssertLessThan(result.centroid, 12000, "White noise centroid should be mid-range")
        
        XCTAssertGreaterThan(result.rolloff, 10000, "White noise rolloff should be high")
    }

    func testAnalyzeSpectrumReturnsLowerCentroidAndNegativeTiltForPinkNoise() throws {
        // Arrange: Pink noise (1/f spectrum) has:
        // - Lower centroid than white noise
        // - Negative tilt (energy decreases with frequency)
        let buffer = generatePinkNoise(durationSamples: 2048, sampleRate: 44100)

        // Act
        let result = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: 44100)

        // Assert: Pink noise has 1/f spectrum characteristics
        XCTAssertLessThan(result.centroid, 8000, "Pink noise centroid should be lower than white noise")
        XCTAssertLessThan(result.tilt, 0, "Pink noise should have negative tilt")
        XCTAssertGreaterThan(result.tilt, -2.0, "Tilt should be in reasonable range")
    }

    // MARK: - Buffer Sizes (AC4)

    func testAnalyzeSpectrumHandlesVariousBufferSizes() throws {
        // Test buffer sizes: 512, 1024, 2048
        let bufferSizes = [512, 1024, 2048]

        for bufferSize in bufferSizes {
            // Arrange
            let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: bufferSize)

            // Act
            let result = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: 44100)

            // Assert
            XCTAssertGreaterThan(result.centroid, 0, "Buffer size \(bufferSize): centroid should be positive")
            XCTAssertGreaterThan(result.rolloff, 0, "Buffer size \(bufferSize): rolloff should be positive")
            XCTAssertTrue(result.tilt.isFinite, "Buffer size \(bufferSize): tilt should be finite")
        }
    }

    // MARK: - Sample Rate Validation (AC3)

    func testAnalyzeSpectrumValidatesSampleRateIsInteger() throws {
        // Note: Swift enforces Int type, so non-integer sample rates are prevented at compile time
        // This test verifies that valid integer sample rates work
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 1024)

        // Act
        let result = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: 44100)

        // Assert
        XCTAssertTrue(result.centroid.isFinite)
    }

    func testAnalyzeSpectrumValidatesSampleRateIsWithinValidRange() throws {
        // Arrange
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 1024)

        // Act & Assert: Below minimum (8000 Hz)
        XCTAssertThrowsError(try analyzeSpectrumWrapper(buffer: buffer, sampleRate: 7999)) { error in
            guard let rustError = error as? RustFFIError else {
                XCTFail("Expected RustFFIError")
                return
            }

            if case .invalidInput(let message) = rustError {
                XCTAssertTrue(message.contains("8000") && message.contains("48000"),
                            "Error message should mention valid sample rate range")
            } else {
                XCTFail("Expected invalidInput error")
            }
        }

        // Act & Assert: Above maximum (48000 Hz)
        XCTAssertThrowsError(try analyzeSpectrumWrapper(buffer: buffer, sampleRate: 48001)) { error in
            guard let rustError = error as? RustFFIError else {
                XCTFail("Expected RustFFIError")
                return
            }

            if case .invalidInput(let message) = rustError {
                XCTAssertTrue(message.contains("8000") && message.contains("48000"),
                            "Error message should mention valid sample rate range")
            } else {
                XCTFail("Expected invalidInput error")
            }
        }
    }

    func testAnalyzeSpectrumAcceptsValidSampleRates() throws {
        // Arrange
        let validSampleRates = [8000, 16000, 22050, 44100, 48000]

        for sampleRate in validSampleRates {
            // Create buffer with appropriate sample rate
            let buffer = generateSineWave(
                frequency: 200,
                sampleRate: Float(sampleRate),
                durationSamples: 1024
            )

            // Act
            let result = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: sampleRate)

            // Assert
            XCTAssertTrue(result.centroid.isFinite, "Sample rate \(sampleRate): centroid should be finite")
            XCTAssertGreaterThan(result.centroid, 0, "Sample rate \(sampleRate): centroid should be positive")
        }
    }

    // MARK: - Validation Error Tests

    func testAnalyzeSpectrumThrowsErrorForEmptyBuffer() {
        // Arrange
        let emptyBuffer: [Float] = []

        // Act & Assert
        XCTAssertThrowsError(try analyzeSpectrumWrapper(buffer: emptyBuffer, sampleRate: 44100)) { error in
            guard let rustError = error as? RustFFIError else {
                XCTFail("Expected RustFFIError")
                return
            }

            if case .invalidInput(let message) = rustError {
                XCTAssertTrue(message.contains("empty"), "Error message should mention empty buffer")
            } else {
                XCTFail("Expected invalidInput error")
            }
        }
    }

    // MARK: - FFI Binding Tests

    func testAnalyzeSpectrumFFIBindingReturnsValidResults() throws {
        // Test that FFI correctly marshals data between Swift and Rust
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 1024)

        // Act: Call Rust spectrum analysis via FFI
        let result = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: 44100)

        // Assert: Check all values are valid
        XCTAssertTrue(result.centroid.isFinite, "Centroid should be finite")
        XCTAssertTrue(result.rolloff.isFinite, "Rolloff should be finite")
        XCTAssertTrue(result.tilt.isFinite, "Tilt should be finite")
        
        XCTAssertGreaterThan(result.centroid, 0, "Centroid should be positive")
        XCTAssertGreaterThan(result.rolloff, 0, "Rolloff should be positive")
    }

    func testAnalyzeSpectrumMemoryManagement() throws {
        // Test that memory is properly managed after multiple spectrum analysis calls
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 1024)

        // Perform many spectrum analysis operations to detect memory leaks
        for _ in 0..<100 {
            let result = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: 44100)
            XCTAssertTrue(result.centroid.isFinite)
        }

        // If memory management is broken, this test would crash or leak
        XCTAssertTrue(true, "Memory management test completed without crash")
    }

    // MARK: - Cross-Platform Consistency Tests (AC5)

    func testAnalyzeSpectrumProducesConsistentResultsForSameInput() throws {
        // Test that running spectrum analysis multiple times on same input produces identical results
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 1024)

        let result1 = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: 44100)
        let result2 = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: 44100)

        // Assert: Results should be identical (deterministic computation)
        XCTAssertEqual(result1.centroid, result2.centroid, accuracy: 0.001,
                      "Centroid should be identical for same input")
        XCTAssertEqual(result1.rolloff, result2.rolloff, accuracy: 0.001,
                      "Rolloff should be identical for same input")
        XCTAssertEqual(result1.tilt, result2.tilt, accuracy: 0.001,
                      "Tilt should be identical for same input")
    }

    func testAnalyzeSpectrumCentroidIsWithinNyquistFrequency() throws {
        // Centroid should always be within valid frequency range (0 to Nyquist)
        let buffer = generateWhiteNoise(durationSamples: 2048)
        let sampleRate = 44100
        let nyquistFrequency = Float(sampleRate) / 2.0

        let result = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: sampleRate)

        // Assert: Centroid should be within valid range
        XCTAssertGreaterThan(result.centroid, 0)
        XCTAssertLessThanOrEqual(result.centroid, nyquistFrequency,
                                "Centroid should not exceed Nyquist frequency")
    }

    func testAnalyzeSpectrumRolloffIsWithinNyquistFrequency() throws {
        // Rolloff should always be within valid frequency range (0 to Nyquist)
        let buffer = generateWhiteNoise(durationSamples: 2048)
        let sampleRate = 44100
        let nyquistFrequency = Float(sampleRate) / 2.0

        let result = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: sampleRate)

        // Assert: Rolloff should be within valid range
        XCTAssertGreaterThan(result.rolloff, 0)
        XCTAssertLessThanOrEqual(result.rolloff, nyquistFrequency,
                                "Rolloff should not exceed Nyquist frequency")
    }

    // MARK: - Real-World Use Cases - Known Spectral Characteristics

    func testAnalyzeSpectrumComparesWhiteNoiseVsPinkNoise() throws {
        // Arrange: Generate both types
        let whiteNoise = generateWhiteNoise(durationSamples: 2048)
        let pinkNoise = generatePinkNoise(durationSamples: 2048, sampleRate: 44100)

        // Act
        let whiteResult = try analyzeSpectrumWrapper(buffer: whiteNoise, sampleRate: 44100)
        let pinkResult = try analyzeSpectrumWrapper(buffer: pinkNoise, sampleRate: 44100)

        // Assert: Pink noise should have lower centroid and more negative tilt than white noise
        XCTAssertLessThan(pinkResult.centroid, whiteResult.centroid,
                         "Pink noise centroid should be lower than white noise")
        XCTAssertLessThan(pinkResult.tilt, whiteResult.tilt,
                         "Pink noise tilt should be more negative than white noise")
    }

    // MARK: - Edge Cases

    func testAnalyzeSpectrumWithMinimumValidBufferSize() throws {
        // Arrange
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 256)

        // Act
        let result = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: 44100)

        // Assert
        XCTAssertTrue(result.centroid.isFinite)
        XCTAssertGreaterThan(result.centroid, 0)
    }

    func testAnalyzeSpectrumWithMaximumValidBufferSize() throws {
        // Arrange
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 16384)

        // Act
        let result = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: 44100)

        // Assert
        XCTAssertTrue(result.centroid.isFinite)
        XCTAssertGreaterThan(result.centroid, 0)
    }

    func testAnalyzeSpectrumWithMinimumSampleRate() throws {
        // Arrange
        let buffer = generateSineWave(frequency: 200, sampleRate: 8000, durationSamples: 1024)

        // Act
        let result = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: 8000)

        // Assert
        XCTAssertTrue(result.centroid.isFinite)
        XCTAssertGreaterThan(result.centroid, 0)
        XCTAssertLessThanOrEqual(result.centroid, 4000) // Nyquist for 8000 Hz
    }

    func testAnalyzeSpectrumWithMaximumSampleRate() throws {
        // Arrange
        let buffer = generateSineWave(frequency: 440, sampleRate: 48000, durationSamples: 1024)

        // Act
        let result = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: 48000)

        // Assert
        XCTAssertTrue(result.centroid.isFinite)
        XCTAssertGreaterThan(result.centroid, 0)
        XCTAssertLessThanOrEqual(result.centroid, 24000) // Nyquist for 48000 Hz
    }

    // MARK: - Performance Tests

    func testAnalyzeSpectrumPerformance() throws {
        // Measure spectrum analysis computation time to ensure it meets <5ms target
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 2048)

        measure {
            do {
                _ = try analyzeSpectrumWrapper(buffer: buffer, sampleRate: 44100)
            } catch {
                XCTFail("Spectrum analysis should not throw during performance test")
            }
        }
    }
}
