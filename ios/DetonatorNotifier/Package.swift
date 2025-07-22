// swift-tools-version: 5.10
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "DetonatorNotifier",
    platforms: [
        .iOS(.v13),
    ],
    products: [
        // Products define the executables and libraries a package produces, making them visible to other packages.
        .library(
            name: "DetonatorNotifier",
            targets: ["DetonatorNotifier"]),
    ],
    dependencies: [
        .package(name: "Detonator", path: "../../../../../node_modules/detonator/ios/Detonator"),
        .package(url: "https://github.com/firebase/firebase-ios-sdk.git", from: "11.15.0")
    ],
    targets: [
        // Targets are the basic building blocks of a package, defining a module or a test suite.
        // Targets can depend on other targets in this package and products from dependencies.
        .target(
            name: "DetonatorNotifier",
            dependencies: [
                "Detonator",
               .product(name: "FirebaseMessaging", package: "firebase-ios-sdk"),
               .product(name: "FirebaseCore", package: "firebase-ios-sdk"),
               .product(name: "FirebaseInstallations", package: "firebase-ios-sdk")
            ]),
        .testTarget(
            name: "DetonatorNotifierTests",
            dependencies: ["DetonatorNotifier"]),
    ]
)
