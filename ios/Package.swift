// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "LoboClipsAI",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "LoboClipsAI", targets: ["LoboClipsAI"])
    ],
    dependencies: [
        .package(url: "https://github.com/firebase/firebase-ios-sdk.git", from: "11.0.0")
    ],
    targets: [
        .target(
            name: "LoboClipsAI",
            dependencies: [
                .product(name: "FirebaseAuth", package: "firebase-ios-sdk"),
                .product(name: "FirebaseFirestore", package: "firebase-ios-sdk"),
                .product(name: "FirebaseAppCheck", package: "firebase-ios-sdk")
            ]
        )
    ]
)
