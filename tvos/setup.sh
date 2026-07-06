#!/bin/bash
set -e
echo "🔥 Emberstorm tvOS — Project Setup"
if ! command -v xcodebuild &>/dev/null; then
    echo "❌ Xcode not found. Install from the App Store."
    exit 1
fi
echo "✅ Xcode found"
if ! command -v xcodegen &>/dev/null; then
    echo "📦 Installing XcodeGen..."
    brew install xcodegen
else
    echo "✅ XcodeGen found"
fi
echo "🔨 Generating Xcode project..."
cd "$(dirname "$0")"
xcodegen generate
echo ""
echo "✅ Done! Opening in Xcode..."
open Emberstorm.xcodeproj
echo ""
echo "Next steps:"
echo "  1. Select your Apple Developer team in Signing & Capabilities"
echo "  2. Pick an Apple TV simulator from the dropdown"
echo "  3. Press Run ▶"
