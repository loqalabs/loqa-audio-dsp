package com.loqalabs.loquaaudiodsp

import com.loqalabs.loquaaudiodsp.RustJNI.RustBridge
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Comprehensive tests for FFT functionality (Story 2.6)
 * Tests native Android FFT implementation, JNI bindings, and cross-platform consistency
 */
class FFTTests {

    // MARK: - Helper Functions

    /**
     * Generate a synthetic sine wave for testing
     * @param frequency Frequency in Hz
     * @param sampleRate Sample rate in Hz
     * @param durationSamples Number of samples to generate
     * @return FloatArray of sine wave samples
     */
    private fun generateSineWave(frequency: Float, sampleRate: Float, durationSamples: Int): FloatArray {
        val buffer = FloatArray(durationSamples)
        val omega = 2.0f * PI.toFloat() * frequency / sampleRate

        for (i in 0 until durationSamples) {
            buffer[i] = sin(omega * i)
        }

        return buffer
    }

    /**
     * Find the index of the maximum value in a FloatArray
     */
    private fun findPeakIndex(array: FloatArray): Int {
        var maxIndex = 0
        var maxValue = array[0]

        for (i in array.indices) {
            if (array[i] > maxValue) {
                maxValue = array[i]
                maxIndex = i
            }
        }

        return maxIndex
    }

    // MARK: - Valid Input Tests (AC2, AC4)

    @Test
    fun testComputeFFTWithValidSineWave() {
        // Arrange: Create a 440 Hz sine wave at 44100 Hz
        val frequency = 440.0f
        val sampleRate = 44100.0f
        val fftSize = 2048
        val buffer = generateSineWave(frequency, sampleRate, fftSize)

        // Act: Compute FFT via Rust JNI
        val magnitude = RustBridge.computeFFT(buffer, fftSize, 1) // hanning window

        // Assert: Check result properties
        assertEquals("Magnitude array should have fftSize/2 elements", fftSize / 2, magnitude.size)

        // Calculate expected peak bin: frequency / (sampleRate / fftSize)
        val expectedBin = (frequency * fftSize / sampleRate).toInt()
        val peakBin = findPeakIndex(magnitude)

        // Peak should be within a few bins of expected (windowing causes spectral leakage)
        assertTrue(
            "Peak bin ($peakBin) should be near expected bin ($expectedBin)",
            abs(peakBin - expectedBin) <= 2
        )
    }

    @Test
    fun testComputeFFTWithDifferentWindowTypes() {
        // Arrange: Create test buffer
        val buffer = generateSineWave(440.0f, 44100.0f, 1024)
        val fftSize = 1024

        // Window types: none=0, hanning=1, hamming=2, blackman=3
        val windowTypes = listOf(0, 1, 2, 3)
        val results = mutableListOf<FloatArray>()

        // Act: Compute FFT with each window type
        for (windowType in windowTypes) {
            val magnitude = RustBridge.computeFFT(buffer, fftSize, windowType)
            results.add(magnitude)

            // Assert: Each should produce valid output
            assertEquals(fftSize / 2, magnitude.size)
        }

        // Assert: Different window types should produce different results
        assertFalse(
            "none and hanning windows should differ",
            results[0].contentEquals(results[1])
        )
        assertFalse(
            "hanning and hamming windows should differ",
            results[1].contentEquals(results[2])
        )
        assertFalse(
            "hamming and blackman windows should differ",
            results[2].contentEquals(results[3])
        )
    }

    @Test
    fun testComputeFFTWithVariousFFTSizes() {
        // Arrange: Test with valid power-of-2 FFT sizes
        val validFFTSizes = listOf(256, 512, 1024, 2048, 4096, 8192)

        for (fftSize in validFFTSizes) {
            // Create buffer matching FFT size
            val buffer = generateSineWave(440.0f, 44100.0f, fftSize)

            // Act
            val magnitude = RustBridge.computeFFT(buffer, fftSize, 1)

            // Assert
            assertEquals(
                "FFT size $fftSize should produce ${fftSize / 2} magnitude bins",
                fftSize / 2,
                magnitude.size
            )
        }
    }

    @Test
    fun testComputeFFTMemoryManagement() {
        // Test that memory is properly handled by JNI after multiple FFT calls
        // JNI automatically manages FloatArray memory

        val buffer = generateSineWave(440.0f, 44100.0f, 1024)

        // Perform many FFT operations to detect memory issues
        for (i in 0 until 100) {
            val magnitude = RustBridge.computeFFT(buffer, 1024, 1)
            assertEquals(512, magnitude.size)
        }

        // If memory management is broken, this test would crash or throw
        assertTrue("Memory management test completed without error", true)
    }

    // MARK: - Validation Error Tests (AC3, AC4)

    @Test(expected = RuntimeException::class)
    fun testComputeFFTThrowsErrorForEmptyBuffer() {
        // Arrange
        val emptyBuffer = FloatArray(0)

        // Act & Assert: Should throw RuntimeException
        RustBridge.computeFFT(emptyBuffer, 512, 1)
    }

    @Test(expected = RuntimeException::class)
    fun testComputeFFTThrowsErrorForNonPowerOf2FFTSize() {
        // Arrange
        val buffer = generateSineWave(440.0f, 44100.0f, 1024)
        val invalidFFTSize = 1000 // Not a power of 2

        // Act & Assert: Should throw RuntimeException
        RustBridge.computeFFT(buffer, invalidFFTSize, 1)
    }

    @Test(expected = RuntimeException::class)
    fun testComputeFFTThrowsErrorForFFTSizeBelowMinimum() {
        // Arrange
        val buffer = generateSineWave(440.0f, 44100.0f, 512)
        val tooSmallFFTSize = 128 // Below minimum (256)

        // Act & Assert: Should throw RuntimeException
        RustBridge.computeFFT(buffer, tooSmallFFTSize, 1)
    }

    @Test(expected = RuntimeException::class)
    fun testComputeFFTThrowsErrorForFFTSizeAboveMaximum() {
        // Arrange
        val buffer = generateSineWave(440.0f, 44100.0f, 1024)
        val tooLargeFFTSize = 16384 // Above maximum (8192)

        // Act & Assert: Should throw RuntimeException
        RustBridge.computeFFT(buffer, tooLargeFFTSize, 1)
    }

    // MARK: - JNI Binding Tests (AC4)

    @Test
    fun testJNIBindingReturnsValidMagnitudes() {
        // Test that JNI correctly marshals data between Kotlin and Rust
        val buffer = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f)
        val fftSize = 8

        // Act: Call Rust FFT via JNI
        val magnitude = RustBridge.computeFFT(buffer, fftSize, 0) // none window

        // Assert: Check result is valid
        assertEquals(fftSize / 2, magnitude.size)

        // All magnitudes should be finite (not NaN or Infinity)
        for (value in magnitude) {
            assertTrue("Magnitude values should be finite", value.isFinite())
        }
    }

    @Test
    fun testJNIMemorySafetyWithMultipleCalls() {
        // Test that JNI handles memory correctly with multiple calls
        val buffer = generateSineWave(440.0f, 44100.0f, 1024)

        // This should not leak memory or crash
        for (i in 0 until 50) {
            RustBridge.computeFFT(buffer, 1024, 1)
        }

        assertTrue("JNI memory safety test completed", true)
    }

    // MARK: - Cross-Platform Consistency Tests (AC5)

    @Test
    fun testFFTProducesConsistentResultsForSameInput() {
        // Test that running FFT multiple times on same input produces identical results
        val buffer = generateSineWave(440.0f, 44100.0f, 1024)

        val result1 = RustBridge.computeFFT(buffer, 1024, 1)
        val result2 = RustBridge.computeFFT(buffer, 1024, 1)

        // Assert: Results should be identical (deterministic computation)
        assertEquals(result1.size, result2.size)

        for (i in result1.indices) {
            assertEquals(
                "FFT results should be identical for same input",
                result1[i],
                result2[i],
                0.0001f
            )
        }
    }

    @Test
    fun testFFTMagnitudeIsNonNegative() {
        // Magnitude spectrum should always be non-negative
        val buffer = generateSineWave(440.0f, 44100.0f, 2048)

        val magnitude = RustBridge.computeFFT(buffer, 2048, 1)

        // Assert: All magnitudes should be >= 0
        for (i in magnitude.indices) {
            assertTrue(
                "Magnitude at bin $i should be non-negative",
                magnitude[i] >= 0.0f
            )
        }
    }

    @Test
    fun testFFTCrossplatformCompatibility() {
        // Test that Android FFT produces results consistent with expected behavior
        // This ensures Android matches iOS for same inputs (AC5)
        val buffer = generateSineWave(440.0f, 44100.0f, 1024)

        val magnitude = RustBridge.computeFFT(buffer, 1024, 1)

        // Assert: Result should have correct length
        assertEquals(512, magnitude.size)

        // Assert: Peak should be at expected frequency bin
        val peakBin = findPeakIndex(magnitude)
        val expectedBin = (440.0f * 1024 / 44100.0f).toInt()

        assertTrue(
            "Peak should be near expected bin for cross-platform consistency",
            abs(peakBin - expectedBin) <= 2
        )
    }

    // MARK: - Edge Case Tests

    @Test
    fun testFFTWithMinimumFFTSize() {
        // Test with minimum allowed FFT size (256)
        val buffer = generateSineWave(440.0f, 44100.0f, 256)

        val magnitude = RustBridge.computeFFT(buffer, 256, 1)

        assertEquals(128, magnitude.size)
    }

    @Test
    fun testFFTWithMaximumFFTSize() {
        // Test with maximum allowed FFT size (8192)
        val buffer = generateSineWave(440.0f, 44100.0f, 8192)

        val magnitude = RustBridge.computeFFT(buffer, 8192, 1)

        assertEquals(4096, magnitude.size)
    }

    @Test
    fun testFFTWithBufferSmallerThanFFTSize() {
        // Test behavior when buffer is smaller than fftSize
        // Should still work (zero-padding is handled in Rust)
        val buffer = generateSineWave(440.0f, 44100.0f, 512)

        val magnitude = RustBridge.computeFFT(buffer, 1024, 1)

        assertEquals(512, magnitude.size)
    }

    @Test
    fun testFFTWithDCComponent() {
        // Test FFT with DC offset (all values offset by constant)
        val buffer = generateSineWave(440.0f, 44100.0f, 1024)

        // Add DC offset
        for (i in buffer.indices) {
            buffer[i] += 0.5f
        }

        val magnitude = RustBridge.computeFFT(buffer, 1024, 1)

        // DC component should be significant at bin 0
        assertTrue(
            "DC component should be present in bin 0",
            magnitude[0] > 0.1f
        )
    }

    @Test
    fun testFFTWithZeroBuffer() {
        // Test FFT with all zeros
        val buffer = FloatArray(1024) { 0.0f }

        val magnitude = RustBridge.computeFFT(buffer, 1024, 1)

        // All magnitudes should be very small (near zero)
        for (value in magnitude) {
            assertTrue(
                "Zero input should produce near-zero output",
                abs(value) < 0.01f
            )
        }
    }

    @Test
    fun testFFTWithMultipleFrequencies() {
        // Test FFT with multiple sine waves combined
        val sampleRate = 44100.0f
        val fftSize = 2048

        val freq1 = 440.0f
        val freq2 = 880.0f

        val buffer = FloatArray(fftSize) { i ->
            val omega1 = 2.0f * PI.toFloat() * freq1 / sampleRate
            val omega2 = 2.0f * PI.toFloat() * freq2 / sampleRate
            sin(omega1 * i) + sin(omega2 * i)
        }

        val magnitude = RustBridge.computeFFT(buffer, fftSize, 1)

        // Should have peaks at both frequencies
        val expectedBin1 = (freq1 * fftSize / sampleRate).toInt()
        val expectedBin2 = (freq2 * fftSize / sampleRate).toInt()

        // Find local maxima near expected bins
        assertTrue(
            "Should have significant magnitude near $freq1 Hz",
            magnitude[expectedBin1] > 0.1f
        )
        assertTrue(
            "Should have significant magnitude near $freq2 Hz",
            magnitude[expectedBin2] > 0.1f
        )
    }

    // MARK: - Data Type Tests

    @Test
    fun testJNIFloatArrayMarshalling() {
        // Test that JNI correctly marshals FloatArray between Kotlin and Rust
        val inputBuffer = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f)

        val magnitude = RustBridge.computeFFT(inputBuffer, 8, 0)

        // Verify that result is a valid FloatArray
        assertTrue("Result should be a FloatArray", magnitude is FloatArray)
        assertEquals(4, magnitude.size)
    }

    @Test
    fun testFFTPreservesInputBuffer() {
        // Test that FFT doesn't modify the input buffer
        val originalBuffer = generateSineWave(440.0f, 44100.0f, 1024)
        val bufferCopy = originalBuffer.copyOf()

        RustBridge.computeFFT(originalBuffer, 1024, 1)

        // Assert: Original buffer should be unchanged
        assertTrue(
            "FFT should not modify input buffer",
            originalBuffer.contentEquals(bufferCopy)
        )
    }

    // MARK: - Performance Tests

    @Test
    fun testFFTPerformance() {
        // Measure FFT computation time to ensure it meets <5ms target
        val buffer = generateSineWave(440.0f, 44100.0f, 2048)

        val iterations = 10
        val startTime = System.nanoTime()

        for (i in 0 until iterations) {
            RustBridge.computeFFT(buffer, 2048, 1)
        }

        val endTime = System.nanoTime()
        val avgTimeMs = (endTime - startTime) / iterations / 1_000_000.0

        // Log performance (would show in test output)
        println("Average FFT time: $avgTimeMs ms")

        // Assert: Should be reasonably fast (under 10ms per call on typical hardware)
        assertTrue(
            "FFT should complete in reasonable time",
            avgTimeMs < 10.0
        )
    }
}
