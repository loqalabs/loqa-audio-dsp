// swift-tools-version:5.5
import PackageDescription

let package = Package(
    name: "LoqaAudioDsp",
    platforms: [
        .iOS(.v13)
    ],
    products: [
        .library(
            name: "LoqaAudioDsp",
            targets: ["LoqaAudioDsp"]),
    ],
    targets: [
        .target(
            name: "LoqaAudioDsp",
            path: "ios",
            exclude: ["LoqaAudioDsp.podspec", "Tests"],
            sources: ["LoqaAudioDspModule.swift", "RustFFI"]
        ),
        .testTarget(
            name: "LoqaAudioDspTests",
            dependencies: ["LoqaAudioDsp"],
            path: "ios/Tests"
        ),
    ]
)
