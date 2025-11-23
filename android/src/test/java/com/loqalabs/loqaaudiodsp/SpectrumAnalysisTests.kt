package com.loqalabs.loqaaudiodsp

import com.loqalabs.loqaaudiodsp.RustJNI.RustBridge
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Comprehensive tests for spectrum analysis functionality (Story 4.4)
 * Tests native Android spectral analysis implementation, JNI bindings, and cross-platform consistency
 */
class SpectrumAnalysisTests {

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
     * Generate white noise (broad spectrum)
     * @param durationSamples Number of samples to generate
     * @return FloatArray of white noise
     */
    private fun generateWhiteNoise(durationSamples: Int): FloatArray {
        val buffer = FloatArray(durationSamples)

        for (i in 0 until durationSamples) {
            // Generate random values between -1 and 1
            buffer[i] = Random.nextFloat() * 2.0f - 1.0f
        }

        return buffer
    }

    /**
     * Generate pink noise (1/f spectrum approximation)
     * @param durationSamples Number of samples to generate
     * @param sampleRate Sample rate in Hz
     * @return FloatArray of pink noise
     */
    private fun generatePinkNoise(durationSamples: Int, sampleRate: Float): FloatArray {
        val buffer = FloatArray(durationSamples)

        // Generate pink noise by summing multiple octaves with decreasing amplitude
        val numOctaves = 5
        for (octave in 0 until numOctaves) {
            val freq = 100.0f * kotlin.math.pow(2.0f, octave.toFloat()) // 100, 200, 400, 800, 1600 Hz
            val amplitude = 1.0f / (octave + 1) // Decreasing amplitude
            val omega = 2.0f * PI.toFloat() * freq / sampleRate
            val phase = Random.nextFloat() * 2.0f * PI.toFloat()

            for (i in 0 until durationSamples) {
                buffer[i] += amplitude * sin(omega * i + phase)
            }
        }

        // Normalize
        val maxVal = buffer.maxOrNull() ?: 1.0f
        for (i in buffer.indices) {
            buffer[i] /= maxVal
        }

        return buffer
    }

    // MARK: - Valid Input Tests - Computing Features Successfully (AC1)

    @Test
    fun testAnalyzeSpectrumFor440HzSineWave() {
        // Arrange: Create a 440 Hz sine wave with narrow spectral peak
        val frequency = 440.0f
        val sampleRate = 44100
        val bufferSize = 2048
        val buffer = generateSineWave(frequency, sampleRate.toFloat(), bufferSize)

        // Act: Analyze spectrum via Rust JNI
        val result = RustBridge.analyzeSpectrum(buffer, sampleRate)

        // Assert: Sine wave has narrow spectral peak
        assertTrue("Centroid should be positive", result.centroid > 0)
        assertTrue("Rolloff should be positive", result.rolloff > 0)

        // Centroid should be near the fundamental frequency for sine wave
        assertTrue("Centroid should be close to 440 Hz", result.centroid > 400 && result.centroid < 480)
    }

    @Test
    fun testAnalyzeSpectrumComputesAllThreeFeatures() {
        // Arrange
        val buffer = generateSineWave(440.0f, 44100.0f, 1024)

        // Act
        val result = RustBridge.analyzeSpectrum(buffer, 44100)

        // Assert: All three features should be present and valid
        assertTrue("Centroid should be finite", result.centroid.isFinite())
        assertTrue("Rolloff should be finite", result.rolloff.isFinite())
        assertTrue("Tilt should be finite", result.tilt.isFinite())
    }

    // MARK: - Expected Ranges for Spectral Features (AC2)

    @Test
    fun testAnalyzeSpectrumReturnsCentroidRolloffTiltInExpectedRangesFor440HzSineWave() {
        // Arrange: 440 Hz sine wave should have:
        // - Centroid ~440 Hz (narrow peak at fundamental)
        // - Rolloff ~440 Hz (energy concentrated at fundamental)
        // - Tilt ~0 (flat spectrum at single frequency)
        val buffer = generateSineWave(440.0f, 44100.0f, 2048)

        // Act
        val result = RustBridge.analyzeSpectrum(buffer, 44100)

        // Assert: Spectral characteristics of pure tone
        assertTrue("Centroid should be near 440 Hz", result.centroid > 400 && result.centroid < 480)
        assertTrue("Rolloff should be near 440 Hz", result.rolloff > 400 && result.rolloff < 500)
        assertTrue("Tilt should be near zero for sine wave",
            result.tilt > -0.5f && result.tilt < 0.5f)
    }

    @Test
    fun testAnalyzeSpectrumReturnsMidRangeCentroidForWhiteNoise() {
        // Arrange: White noise has broad spectrum
        // - Centroid should be in mid-range (4000-10000 Hz for 44.1kHz)
        // - Rolloff should be high
        val buffer = generateWhiteNoise(2048)

        // Act
        val result = RustBridge.analyzeSpectrum(buffer, 44100)

        // Assert: White noise has broad spectrum characteristics
        assertTrue("White noise centroid should be mid-range",
            result.centroid > 3000 && result.centroid < 12000)
        assertTrue("White noise rolloff should be high", result.rolloff > 10000)
    }

    @Test
    fun testAnalyzeSpectrumReturnsLowerCentroidAndNegativeTiltForPinkNoise() {
        // Arrange: Pink noise (1/f spectrum) has:
        // - Lower centroid than white noise
        // - Negative tilt (energy decreases with frequency)
        val buffer = generatePinkNoise(2048, 44100.0f)

        // Act
        val result = RustBridge.analyzeSpectrum(buffer, 44100)

        // Assert: Pink noise has 1/f spectrum characteristics
        assertTrue("Pink noise centroid should be lower than white noise",
            result.centroid < 8000)
        assertTrue("Pink noise should have negative tilt", result.tilt < 0)
        assertTrue("Tilt should be in reasonable range", result.tilt > -2.0f)
    }

    // MARK: - Buffer Sizes (AC4)

    @Test
    fun testAnalyzeSpectrumHandlesVariousBufferSizes() {
        // Test buffer sizes: 512, 1024, 2048
        val bufferSizes = listOf(512, 1024, 2048)

        for (bufferSize in bufferSizes) {
            // Arrange
            val buffer = generateSineWave(440.0f, 44100.0f, bufferSize)

            // Act
            val result = RustBridge.analyzeSpectrum(buffer, 44100)

            // Assert
            assertTrue("Buffer size $bufferSize: centroid should be positive",
                result.centroid > 0)
            assertTrue("Buffer size $bufferSize: rolloff should be positive",
                result.rolloff > 0)
            assertTrue("Buffer size $bufferSize: tilt should be finite",
                result.tilt.isFinite())
        }
    }

    // MARK: - Sample Rate Validation (AC3)

    @Test
    fun testAnalyzeSpectrumValidatesSampleRateIsInteger() {
        // Note: Kotlin enforces Int type, so non-integer sample rates are prevented at compile time
        // This test verifies that valid integer sample rates work
        val buffer = generateSineWave(440.0f, 44100.0f, 1024)

        // Act
        val result = RustBridge.analyzeSpectrum(buffer, 44100)

        // Assert
        assertTrue(result.centroid.isFinite())
    }

    @Test
    fun testAnalyzeSpectrumValidatesSampleRateIsWithinValidRange() {
        // Arrange
        val buffer = generateSineWave(440.0f, 44100.0f, 1024)

        // Act & Assert: Below minimum (8000 Hz)
        try {
            RustBridge.analyzeSpectrum(buffer, 7999)
            fail("Should throw exception for sample rate below 8000 Hz")
        } catch (e: Exception) {
            assertTrue("Error message should mention valid sample rate range",
                e.message?.contains("8000") == true && e.message?.contains("48000") == true)
        }

        // Act & Assert: Above maximum (48000 Hz)
        try {
            RustBridge.analyzeSpectrum(buffer, 48001)
            fail("Should throw exception for sample rate above 48000 Hz")
        } catch (e: Exception) {
            assertTrue("Error message should mention valid sample rate range",
                e.message?.contains("8000") == true && e.message?.contains("48000") == true)
        }
    }

    @Test
    fun testAnalyzeSpectrumAcceptsValidSampleRates() {
        // Arrange
        val validSampleRates = listOf(8000, 16000, 22050, 44100, 48000)

        for (sampleRate in validSampleRates) {
            // Create buffer with appropriate sample rate
            val buffer = generateSineWave(200.0f, sampleRate.toFloat(), 1024)

            // Act
            val result = RustBridge.analyzeSpectrum(buffer, sampleRate)

            // Assert
            assertTrue("Sample rate $sampleRate: centroid should be finite",
                result.centroid.isFinite())
            assertTrue("Sample rate $sampleRate: centroid should be positive",
                result.centroid > 0)
        }
    }

    // MARK: - Validation Error Tests

    @Test
    fun testAnalyzeSpectrumThrowsErrorForEmptyBuffer() {
        // Arrange
        val emptyBuffer = FloatArray(0)

        // Act & Assert
        try {
            RustBridge.analyzeSpectrum(emptyBuffer, 44100)
            fail("Should throw exception for empty buffer")
        } catch (e: Exception) {
            assertTrue("Error message should mention empty buffer",
                e.message?.contains("empty") == true)
        }
    }

    // MARK: - JNI Binding Tests

    @Test
    fun testAnalyzeSpectrumJNIBindingReturnsValidResults() {
        // Test that JNI correctly marshals data between Kotlin and Rust
        val buffer = generateSineWave(440.0f, 44100.0f, 1024)

        // Act: Call Rust spectrum analysis via JNI
        val result = RustBridge.analyzeSpectrum(buffer, 44100)

        // Assert: Check all values are valid
        assertTrue("Centroid should be finite", result.centroid.isFinite())
        assertTrue("Rolloff should be finite", result.rolloff.isFinite())
        assertTrue("Tilt should be finite", result.tilt.isFinite())

        assertTrue("Centroid should be positive", result.centroid > 0)
        assertTrue("Rolloff should be positive", result.rolloff > 0)
    }

    @Test
    fun testAnalyzeSpectrumMemoryManagement() {
        // Test that memory is properly managed by JNI after multiple spectrum analysis calls
        // JNI automatically manages FloatArray memory
        val buffer = generateSineWave(440.0f, 44100.0f, 1024)

        // Perform many spectrum analysis operations to detect memory issues
        for (i in 0 until 100) {
            val result = RustBridge.analyzeSpectrum(buffer, 44100)
            assertTrue(result.centroid.isFinite())
        }

        // If memory management is broken, this test would crash or throw
        assertTrue("Memory management test completed without error", true)
    }

    // MARK: - Cross-Platform Consistency Tests (AC5)

    @Test
    fun testAnalyzeSpectrumProducesConsistentResultsForSameInput() {
        // Test that running spectrum analysis multiple times on same input produces identical results
        val buffer = generateSineWave(440.0f, 44100.0f, 1024)

        val result1 = RustBridge.analyzeSpectrum(buffer, 44100)
        val result2 = RustBridge.analyzeSpectrum(buffer, 44100)

        // Assert: Results should be identical (deterministic computation)
        assertEquals("Centroid should be identical for same input",
            result1.centroid, result2.centroid, 0.001f)
        assertEquals("Rolloff should be identical for same input",
            result1.rolloff, result2.rolloff, 0.001f)
        assertEquals("Tilt should be identical for same input",
            result1.tilt, result2.tilt, 0.001f)
    }

    @Test
    fun testAnalyzeSpectrumCentroidIsWithinNyquistFrequency() {
        // Centroid should always be within valid frequency range (0 to Nyquist)
        val buffer = generateWhiteNoise(2048)
        val sampleRate = 44100
        val nyquistFrequency = sampleRate / 2.0f

        val result = RustBridge.analyzeSpectrum(buffer, sampleRate)

        // Assert: Centroid should be within valid range
        assertTrue("Centroid should be positive", result.centroid > 0)
        assertTrue("Centroid should not exceed Nyquist frequency",
            result.centroid <= nyquistFrequency)
    }

    @Test
    fun testAnalyzeSpectrumRolloffIsWithinNyquistFrequency() {
        // Rolloff should always be within valid frequency range (0 to Nyquist)
        val buffer = generateWhiteNoise(2048)
        val sampleRate = 44100
        val nyquistFrequency = sampleRate / 2.0f

        val result = RustBridge.analyzeSpectrum(buffer, sampleRate)

        // Assert: Rolloff should be within valid range
        assertTrue("Rolloff should be positive", result.rolloff > 0)
        assertTrue("Rolloff should not exceed Nyquist frequency",
            result.rolloff <= nyquistFrequency)
    }

    // MARK: - Real-World Use Cases - Known Spectral Characteristics

    @Test
    fun testAnalyzeSpectrumComparesWhiteNoiseVsPinkNoise() {
        // Arrange: Generate both types
        val whiteNoise = generateWhiteNoise(2048)
        val pinkNoise = generatePinkNoise(2048, 44100.0f)

        // Act
        val whiteResult = RustBridge.analyzeSpectrum(whiteNoise, 44100)
        val pinkResult = RustBridge.analyzeSpectrum(pinkNoise, 44100)

        // Assert: Pink noise should have lower centroid and more negative tilt than white noise
        assertTrue("Pink noise centroid should be lower than white noise",
            pinkResult.centroid < whiteResult.centroid)
        assertTrue("Pink noise tilt should be more negative than white noise",
            pinkResult.tilt < whiteResult.tilt)
    }

    // MARK: - Edge Cases

    @Test
    fun testAnalyzeSpectrumWithMinimumValidBufferSize() {
        // Arrange
        val buffer = generateSineWave(440.0f, 44100.0f, 256)

        // Act
        val result = RustBridge.analyzeSpectrum(buffer, 44100)

        // Assert
        assertTrue(result.centroid.isFinite())
        assertTrue(result.centroid > 0)
    }

    @Test
    fun testAnalyzeSpectrumWithMaximumValidBufferSize() {
        // Arrange
        val buffer = generateSineWave(440.0f, 44100.0f, 16384)

        // Act
        val result = RustBridge.analyzeSpectrum(buffer, 44100)

        // Assert
        assertTrue(result.centroid.isFinite())
        assertTrue(result.centroid > 0)
    }

    @Test
    fun testAnalyzeSpectrumWithMinimumSampleRate() {
        // Arrange
        val buffer = generateSineWave(200.0f, 8000.0f, 1024)

        // Act
        val result = RustBridge.analyzeSpectrum(buffer, 8000)

        // Assert
        assertTrue(result.centroid.isFinite())
        assertTrue(result.centroid > 0)
        assertTrue("Centroid should not exceed Nyquist for 8000 Hz",
            result.centroid <= 4000) // Nyquist for 8000 Hz
    }

    @Test
    fun testAnalyzeSpectrumWithMaximumSampleRate() {
        // Arrange
        val buffer = generateSineWave(440.0f, 48000.0f, 1024)

        // Act
        val result = RustBridge.analyzeSpectrum(buffer, 48000)

        // Assert
        assertTrue(result.centroid.isFinite())
        assertTrue(result.centroid > 0)
        assertTrue("Centroid should not exceed Nyquist for 48000 Hz",
            result.centroid <= 24000) // Nyquist for 48000 Hz
    }

    // MARK: - Performance Tests (Informational)

    @Test
    fun testAnalyzeSpectrumPerformance() {
        // Measure spectrum analysis computation time
        // Note: JUnit doesn't have built-in benchmarking like XCTest
        val buffer = generateSineWave(440.0f, 44100.0f, 2048)

        val startTime = System.nanoTime()
        for (i in 0 until 100) {
            RustBridge.analyzeSpectrum(buffer, 44100)
        }
        val endTime = System.nanoTime()

        val averageTimeMs = (endTime - startTime) / 100.0 / 1_000_000.0

        // Log performance result (target: <5ms)
        println("Average spectrum analysis time: ${averageTimeMs}ms")
        
        // Informational assertion
        assertTrue("Spectrum analysis should complete in reasonable time",
            averageTimeMs < 10.0) // Allow some overhead for test environment
    }
}
