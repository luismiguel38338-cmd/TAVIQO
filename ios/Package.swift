// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "Taviqo",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "Taviqo", targets: ["Taviqo"])
    ],
    dependencies: [
        .package(url: "https://github.com/firebase/firebase-ios-sdk.git", from: "11.0.0")
    ],
    targets: [
        .target(
            name: "Taviqo",
            dependencies: [
                .product(name: "FirebaseAuth", package: "firebase-ios-sdk"),
                .product(name: "FirebaseFirestore", package: "firebase-ios-sdk"),
                .product(name: "FirebaseAppCheck", package: "firebase-ios-sdk")
            ]
        )
    ]
)
