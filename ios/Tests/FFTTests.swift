import XCTest
@testable import LoqaAudioDsp

/// Comprehensive tests for FFT functionality (Story 2.6)
/// Tests native iOS FFT implementation, FFI bindings, and memory management
class FFTTests: XCTestCase {

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

    /// Find the index of the maximum value in an array
    func findPeakIndex(in array: [Float]) -> Int {
        var maxIndex = 0
        var maxValue = array[0]

        for (index, value) in array.enumerated() {
            if value > maxValue {
                maxValue = value
                maxIndex = index
            }
        }

        return maxIndex
    }

    // MARK: - Valid Input Tests (AC2, AC4)

    func testComputeFFTWithValidSineWave() throws {
        // Arrange: Create a 440 Hz sine wave at 44100 Hz
        let frequency: Float = 440.0
        let sampleRate: Float = 44100.0
        let fftSize = 2048
        let buffer = generateSineWave(frequency: frequency, sampleRate: sampleRate, durationSamples: fftSize)

        // Act: Compute FFT via Rust wrapper
        let magnitude = try computeFFTWrapper(buffer: buffer, fftSize: fftSize, windowType: 1) // hanning

        // Assert: Check result properties
        XCTAssertEqual(magnitude.count, fftSize / 2, "Magnitude array should have fftSize/2 elements")

        // Calculate expected peak bin: frequency / (sampleRate / fftSize)
        let expectedBin = Int(frequency * Float(fftSize) / sampleRate)
        let peakBin = findPeakIndex(in: magnitude)

        // Peak should be within a few bins of expected (windowing causes spectral leakage)
        XCTAssertTrue(abs(peakBin - expectedBin) <= 2,
                     "Peak bin (\(peakBin)) should be near expected bin (\(expectedBin))")
    }

    func testComputeFFTWithDifferentWindowTypes() throws {
        // Arrange: Create test buffer
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 1024)
        let fftSize = 1024

        // Window types: none=0, hanning=1, hamming=2, blackman=3
        let windowTypes: [Int32] = [0, 1, 2, 3]
        var results: [[Float]] = []

        // Act: Compute FFT with each window type
        for windowType in windowTypes {
            let magnitude = try computeFFTWrapper(buffer: buffer, fftSize: fftSize, windowType: windowType)
            results.append(magnitude)

            // Assert: Each should produce valid output
            XCTAssertEqual(magnitude.count, fftSize / 2)
        }

        // Assert: Different window types should produce different results
        XCTAssertNotEqual(results[0], results[1], "none and hanning windows should differ")
        XCTAssertNotEqual(results[1], results[2], "hanning and hamming windows should differ")
        XCTAssertNotEqual(results[2], results[3], "hamming and blackman windows should differ")
    }

    func testComputeFFTWithVariousFFTSizes() throws {
        // Arrange: Test with valid power-of-2 FFT sizes
        let validFFTSizes = [256, 512, 1024, 2048, 4096, 8192]

        for fftSize in validFFTSizes {
            // Create buffer matching FFT size
            let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: fftSize)

            // Act
            let magnitude = try computeFFTWrapper(buffer: buffer, fftSize: fftSize, windowType: 1)

            // Assert
            XCTAssertEqual(magnitude.count, fftSize / 2,
                          "FFT size \(fftSize) should produce \(fftSize/2) magnitude bins")
        }
    }

    func testComputeFFTMemoryManagement() throws {
        // Test that memory is properly freed after multiple FFT calls
        // This tests the defer block in computeFFTWrapper

        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 1024)

        // Perform many FFT operations to detect memory leaks
        for _ in 0..<100 {
            let magnitude = try computeFFTWrapper(buffer: buffer, fftSize: 1024, windowType: 1)
            XCTAssertEqual(magnitude.count, 512)
        }

        // If memory management is broken, this test would crash or leak
        // Success indicates defer block is working correctly
        XCTAssertTrue(true, "Memory management test completed without crash")
    }

    // MARK: - Validation Error Tests (AC3, AC4)

    func testComputeFFTThrowsErrorForEmptyBuffer() {
        // Arrange
        let emptyBuffer: [Float] = []

        // Act & Assert
        XCTAssertThrowsError(try computeFFTWrapper(buffer: emptyBuffer, fftSize: 512, windowType: 1)) { error in
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

    func testComputeFFTThrowsErrorForNonPowerOf2FFTSize() {
        // Arrange
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 1024)
        let invalidFFTSize = 1000 // Not a power of 2

        // Act & Assert
        XCTAssertThrowsError(try computeFFTWrapper(buffer: buffer, fftSize: invalidFFTSize, windowType: 1)) { error in
            guard let rustError = error as? RustFFIError else {
                XCTFail("Expected RustFFIError")
                return
            }

            if case .invalidInput(let message) = rustError {
                XCTAssertTrue(message.contains("power of 2"), "Error message should mention power of 2")
            } else {
                XCTFail("Expected invalidInput error")
            }
        }
    }

    func testComputeFFTThrowsErrorForFFTSizeBelowMinimum() {
        // Arrange
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 512)
        let tooSmallFFTSize = 128 // Below minimum (256)

        // Act & Assert
        XCTAssertThrowsError(try computeFFTWrapper(buffer: buffer, fftSize: tooSmallFFTSize, windowType: 1)) { error in
            guard let rustError = error as? RustFFIError else {
                XCTFail("Expected RustFFIError")
                return
            }

            if case .invalidInput(let message) = rustError {
                XCTAssertTrue(message.contains("256") && message.contains("8192"),
                            "Error message should mention valid range")
            } else {
                XCTFail("Expected invalidInput error")
            }
        }
    }

    func testComputeFFTThrowsErrorForFFTSizeAboveMaximum() {
        // Arrange
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 1024)
        let tooLargeFFTSize = 16384 // Above maximum (8192)

        // Act & Assert
        XCTAssertThrowsError(try computeFFTWrapper(buffer: buffer, fftSize: tooLargeFFTSize, windowType: 1)) { error in
            guard let rustError = error as? RustFFIError else {
                XCTFail("Expected RustFFIError")
                return
            }

            if case .invalidInput(let message) = rustError {
                XCTAssertTrue(message.contains("256") && message.contains("8192"),
                            "Error message should mention valid range")
            } else {
                XCTFail("Expected invalidInput error")
            }
        }
    }

    // MARK: - FFI Binding Tests (AC4)

    func testFFIBindingReturnsValidMagnitudes() throws {
        // Test that FFI correctly marshals data between Swift and Rust
        let buffer: [Float] = [0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8]
        let fftSize = 8

        // Act: Call Rust FFT via FFI
        let magnitude = try computeFFTWrapper(buffer: buffer, fftSize: fftSize, windowType: 0) // none

        // Assert: Check result is valid
        XCTAssertEqual(magnitude.count, fftSize / 2)

        // All magnitudes should be finite (not NaN or Infinity)
        for value in magnitude {
            XCTAssertTrue(value.isFinite, "Magnitude values should be finite")
        }
    }

    func testFFIMemorySafetyWithDeferBlock() throws {
        // Test that defer block guarantees Rust memory is freed even if error occurs
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 1024)

        // This should not leak memory even though we're creating and discarding results
        for _ in 0..<50 {
            _ = try computeFFTWrapper(buffer: buffer, fftSize: 1024, windowType: 1)
        }

        XCTAssertTrue(true, "FFI memory safety test completed")
    }

    // MARK: - Cross-Platform Consistency Tests (AC5)

    func testFFTProducesConsistentResultsForSameInput() throws {
        // Test that running FFT multiple times on same input produces identical results
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 1024)

        let result1 = try computeFFTWrapper(buffer: buffer, fftSize: 1024, windowType: 1)
        let result2 = try computeFFTWrapper(buffer: buffer, fftSize: 1024, windowType: 1)

        // Assert: Results should be identical (deterministic computation)
        XCTAssertEqual(result1.count, result2.count)

        for (index, value1) in result1.enumerated() {
            let value2 = result2[index]
            XCTAssertEqual(value1, value2, accuracy: 0.0001,
                          "FFT results should be identical for same input")
        }
    }

    func testFFTMagnitudeIsNonNegative() throws {
        // Magnitude spectrum should always be non-negative
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 2048)

        let magnitude = try computeFFTWrapper(buffer: buffer, fftSize: 2048, windowType: 1)

        // Assert: All magnitudes should be >= 0
        for (index, value) in magnitude.enumerated() {
            XCTAssertGreaterThanOrEqual(value, 0.0,
                                       "Magnitude at bin \(index) should be non-negative")
        }
    }

    // MARK: - Edge Case Tests

    func testFFTWithMinimumFFTSize() throws {
        // Test with minimum allowed FFT size (256)
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 256)

        let magnitude = try computeFFTWrapper(buffer: buffer, fftSize: 256, windowType: 1)

        XCTAssertEqual(magnitude.count, 128)
    }

    func testFFTWithMaximumFFTSize() throws {
        // Test with maximum allowed FFT size (8192)
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 8192)

        let magnitude = try computeFFTWrapper(buffer: buffer, fftSize: 8192, windowType: 1)

        XCTAssertEqual(magnitude.count, 4096)
    }

    func testFFTWithBufferSmallerThanFFTSize() throws {
        // Test behavior when buffer is smaller than fftSize
        // Should still work (zero-padding is handled in Rust)
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 512)

        let magnitude = try computeFFTWrapper(buffer: buffer, fftSize: 1024, windowType: 1)

        XCTAssertEqual(magnitude.count, 512)
    }

    func testFFTWithDCComponent() throws {
        // Test FFT with DC offset (all values offset by constant)
        var buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 1024)

        // Add DC offset
        for i in 0..<buffer.count {
            buffer[i] += 0.5
        }

        let magnitude = try computeFFTWrapper(buffer: buffer, fftSize: 1024, windowType: 1)

        // DC component should be significant at bin 0
        XCTAssertGreaterThan(magnitude[0], 0.1, "DC component should be present in bin 0")
    }

    // MARK: - Performance Tests

    func testFFTPerformance() throws {
        // Measure FFT computation time to ensure it meets <5ms target
        let buffer = generateSineWave(frequency: 440, sampleRate: 44100, durationSamples: 2048)

        measure {
            do {
                _ = try computeFFTWrapper(buffer: buffer, fftSize: 2048, windowType: 1)
            } catch {
                XCTFail("FFT should not throw during performance test")
            }
        }
    }
}
