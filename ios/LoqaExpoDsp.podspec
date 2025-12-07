require 'json'

package = JSON.parse(File.read(File.join(__dir__, '../package.json')))

Pod::Spec.new do |s|
  s.name           = 'LoqaExpoDsp'
  s.version        = package['version']
  s.summary        = package['description']
  s.description    = package['description']
  s.license        = package['license']
  s.author         = package['author']
  s.homepage       = package['homepage']
  s.platforms      = {
    :ios => '15.1',
    :tvos => '15.1'
  }
  s.swift_version  = '5.5'
  s.source         = { git: 'https://github.com/loqalabs/loqa-expo-dsp' }
  s.static_framework = true

  s.dependency 'ExpoModulesCore'

  # Swift and Objective-C source files (exclude Tests directory)
  s.source_files = "*.{h,m,mm,swift,hpp,cpp}", "RustFFI/**/*.{h,hpp,cpp,swift}"
  s.exclude_files = "Tests/**/*"

  # C header for Rust FFI (required for proper C ABI on ARM64)
  s.public_header_files = "RustFFI/*.h"
  s.preserve_paths = "RustFFI/module.modulemap", "RustFFI/loqa_voice_dsp.h"

  # Configure module map for C interop
  s.pod_target_xcconfig = {
    'SWIFT_INCLUDE_PATHS' => '$(PODS_TARGET_SRCROOT)/RustFFI',
    'HEADER_SEARCH_PATHS' => '$(PODS_TARGET_SRCROOT)/RustFFI'
  }
  s.user_target_xcconfig = {
    'HEADER_SEARCH_PATHS' => '$(PODS_ROOT)/LoqaExpoDsp/RustFFI'
  }

  # Rust XCFramework (supports device + simulator)
  s.vendored_frameworks = "RustFFI/LoqaVoiceDSP.xcframework"

  # Fallback: Static library (device only, for legacy support)
  # s.vendored_libraries = "RustFFI/libloqa_voice_dsp.a"
  # s.preserve_paths = "RustFFI/libloqa_voice_dsp.a"

  # Link required system frameworks and libraries
  s.frameworks = 'Foundation'
  s.libraries = 'c++', 'resolv'
end
