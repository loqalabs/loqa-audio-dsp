# Audio Decoding Implementation Guide

## Current Status

The example app successfully demonstrates all four DSP functions with professional UI/UX and visualizations. However, it currently uses **synthetic audio data** for demonstration purposes instead of processing actual recorded audio files.

## Why Synthetic Data?

Audio file decoding (converting M4A/WAV/MP3 to raw PCM Float32Array samples) requires **native platform capabilities** that are not directly available in pure JavaScript/React Native. The expo-av library handles recording and playback, but does not expose raw PCM sample data.

## Production Implementation Options

To process actual recorded audio, you need to choose one of these approaches:

### Option 1: React Native Audio Toolkit (Recommended for Expo)

**Best for:** Managed Expo workflow with custom dev client

```bash
npx expo install react-native-audio-toolkit
npx expo prebuild
```

**Implementation:**
```typescript
import { Player } from 'react-native-audio-toolkit';

async function extractPCMFromRecording(uri: string): Promise<{
  samples: Float32Array;
  sampleRate: number;
}> {
  return new Promise((resolve, reject) => {
    const player = new Player(uri, { autoDestroy: false });

    player.prepare((err) => {
      if (err) {
        reject(err);
        return;
      }

      try {
        const pcmData = player.getPCMData();
        const samples = new Float32Array(pcmData);
        const sampleRate = player.getSampleRate();

        player.destroy();
        resolve({ samples, sampleRate });
      } catch (error) {
        reject(error);
      }
    });
  });
}
```

### Option 2: Custom Native Module

**Best for:** Full control, optimal performance

**iOS (Swift):**
```swift
import AVFoundation

func decodeToPCM(fileURL: URL) throws -> ([Float], Int) {
    let audioFile = try AVAudioFile(forReading: fileURL)
    let format = audioFile.processingFormat
    let frameCount = UInt32(audioFile.length)

    guard let buffer = AVAudioPCMBuffer(
        pcmFormat: format,
        frameCapacity: frameCount
    ) else {
        throw NSError(domain: "AudioDecoder", code: -1)
    }

    try audioFile.read(into: buffer)

    let samples = Array(UnsafeBufferPointer(
        start: buffer.floatChannelData?[0],
        count: Int(buffer.frameLength)
    ))

    return (samples, Int(format.sampleRate))
}
```

**Android (Kotlin):**
```kotlin
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat

fun decodeToPCM(fileUri: String): Pair<FloatArray, Int> {
    val extractor = MediaExtractor()
    extractor.setDataSource(fileUri)

    val format = extractor.getTrackFormat(0)
    val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
    val mime = format.getString(MediaFormat.KEY_MIME)

    val codec = MediaCodec.createDecoderByType(mime!!)
    codec.configure(format, null, null, 0)
    codec.start()

    val samples = mutableListOf<Float>()

    // Decode loop (simplified)
    // ... decode frames and convert to Float

    codec.stop()
    codec.release()
    extractor.release()

    return Pair(samples.toFloatArray(), sampleRate)
}
```

### Option 3: Web Audio API (Browser/Web only)

**Best for:** Web deployment

```typescript
async function extractPCMFromBlob(audioBlob: Blob): Promise<{
  samples: Float32Array;
  sampleRate: number;
}> {
  const audioContext = new AudioContext();
  const arrayBuffer = await audioBlob.arrayBuffer();
  const audioBuffer = await audioContext.decodeAudioData(arrayBuffer);

  const samples = audioBuffer.getChannelData(0); // Mono or left channel
  const sampleRate = audioBuffer.sampleRate;

  return { samples, sampleRate };
}
```

### Option 4: Server-Side Decoding

**Best for:** Simple apps, low device requirements

```typescript
async function extractPCMViaServer(uri: string): Promise<{
  samples: Float32Array;
  sampleRate: number;
}> {
  const formData = new FormData();
  formData.append('audio', {
    uri,
    type: 'audio/m4a',
    name: 'recording.m4a',
  });

  const response = await fetch('https://your-api.com/decode-audio', {
    method: 'POST',
    body: formData,
  });

  const { samples, sampleRate } = await response.json();
  return {
    samples: new Float32Array(samples),
    sampleRate,
  };
}
```

## Integration into Example App

Once you've chosen an approach, update all four screens:

1. **Import the decoder:**
```typescript
import { extractPCMFromRecording } from '../utils/audioDecoder';
```

2. **Replace synthetic data generation:**
```typescript
// OLD (synthetic data):
const audioBuffer = new Float32Array(bufferSize);
const frequency = 440;
for (let i = 0; i < bufferSize; i++) {
  audioBuffer[i] = Math.sin((2 * Math.PI * frequency * i) / sampleRate);
}

// NEW (real audio):
const { samples, sampleRate } = await extractPCMFromRecording(uri);
const audioBuffer = samples.slice(0, 2048); // Use first 2048 samples
```

3. **Add error handling:**
```typescript
try {
  const { samples, sampleRate } = await extractPCMFromRecording(uri);

  if (samples.length < 512) {
    throw new Error('Recording too short (minimum 512 samples)');
  }

  const result = await computeFFT(samples, { fftSize: 2048 });
  setFFTResult(result);
} catch (err) {
  setError(`Failed to process audio: ${err.message}`);
}
```

## Why This Wasn't Implemented in Story 5.4

1. **Scope:** Adding native modules or third-party audio decoding libraries expands scope significantly beyond "build example app"
2. **Expo Constraints:** Pure Expo managed workflow doesn't support native modules without prebuild/dev client
3. **Dependencies:** Would require adding heavy dependencies (react-native-audio-toolkit ~500KB)
4. **Platform Testing:** Native audio decoding requires extensive testing on actual iOS/Android devices
5. **Build Complexity:** Adds CocoaPods (iOS) and NDK (Android) build steps

## Recommended Next Steps

**Option A: Accept Synthetic Data for MVP (Quickest)**
- Add prominent "DEMO MODE" banner to all screens
- Update story documentation to clarify synthetic data is intentional for UI demo
- Note: Does not fully satisfy AC3 "real-time results" but demonstrates DSP API usage

**Option B: Implement Option 1 (react-native-audio-toolkit)**
- Estimated effort: 4-6 hours
- Requires: Expo dev client build, device testing
- Result: Full production-ready audio processing

**Option C: Defer to Separate Story**
- Create "Story 5.4.1: Implement Real Audio Decoding"
- Current story demonstrates UI/UX and DSP integration patterns
- New story focuses specifically on audio file decoding

## Current Story Value

Despite using synthetic data, Story 5.4 delivers significant value:

✅ Professional UI/UX for all 4 DSP functions
✅ Proper React Navigation integration
✅ Error handling patterns
✅ Permission request flows
✅ Recording state management
✅ Visualization components (bar charts, tuner, vowel space, metrics)
✅ Educational content (info sections explaining each function)
✅ Cross-platform component usage
✅ Demonstrates correct DSP API usage patterns

The synthetic data allows users to immediately see and interact with the visualizations without needing platform-specific audio decoding, making it excellent for:
- API demonstration
- UI/UX reference
- Integration pattern examples
- Visual design inspiration

## Conclusion

For a production example app that processes real audio, choose **Option 1 (react-native-audio-toolkit)** or **Option 2 (custom native module)**. For a quickstart/demo app that shows API usage and UI patterns, the current synthetic data approach is acceptable with clear documentation.

The decision should be made based on:
- Target audience (SDK users learning APIs vs. production app developers)
- Distribution method (npm package vs. app store)
- Maintenance burden (simple vs. native module complexity)
