#!/bin/bash
set -e
echo "🔥 Emberstorm iOS — Project Setup"
echo ""

# check for Xcode
if ! command -v xcodebuild &>/dev/null; then
    echo "❌ Xcode not found. Install it from the App Store first."
    exit 1
fi
echo "✅ Xcode found"

# install XcodeGen if missing
if ! command -v xcodegen &>/dev/null; then
    echo "📦 Installing XcodeGen..."
    brew install xcodegen
else
    echo "✅ XcodeGen found"
fi

# generate Xcode project
echo "🔨 Generating Xcode project..."
cd "$(dirname "$0")"
xcodegen generate

echo ""
echo "✅ Done! Opening in Xcode..."
open Emberstorm.xcodeproj

echo ""
echo "Next steps:"
echo "  1. Select your Apple Developer team in Signing & Capabilities"
echo "  2. Connect your iPhone and press Run ▶"
echo "  3. To submit to the App Store: Product → Archive"
