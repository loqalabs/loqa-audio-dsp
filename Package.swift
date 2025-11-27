// swift-tools-version:5.5
import PackageDescription

let package = Package(
    name: "LoqaExpoDsp",
    platforms: [
        .iOS(.v13)
    ],
    products: [
        .library(
            name: "LoqaExpoDsp",
            targets: ["LoqaExpoDsp"]),
    ],
    targets: [
        .target(
            name: "LoqaExpoDsp",
            path: "ios",
            exclude: ["LoqaExpoDsp.podspec", "Tests"],
            sources: ["LoqaExpoDspModule.swift", "RustFFI"]
        ),
        .testTarget(
            name: "LoqaExpoDspTests",
            dependencies: ["LoqaExpoDsp"],
            path: "ios/Tests"
        ),
    ]
)
