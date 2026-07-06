# Emberstorm — iOS App
By **ItRan Studios** · Ran Jacobi

A one-touch gravity-flip arcade game, same as the Android version,
wrapped in a native iOS WKWebView with StoreKit 2 for in-app purchases.

## Quick Start

### Option 1 — Automated setup (recommended)
```bash
cd EmberstormiOS
./setup.sh
```
This installs XcodeGen (if needed), generates the Xcode project, and opens it.

### Option 2 — Manual setup
1. Install XcodeGen: `brew install xcodegen`
2. In this folder: `xcodegen generate`
3. Open `Emberstorm.xcodeproj`

### Then in Xcode:
1. Select your **Apple Developer Team** under Signing & Capabilities
2. Connect your iPhone (or use Simulator)
3. Press **Run ▶**
4. To publish: **Product → Archive → Distribute App → App Store Connect**

## In-App Purchases
Same 7 products as Android. Create them in App Store Connect:

| Product ID     | Type        | Price  |
|---------------|-------------|--------|
| starter_pack  | Non-Consumable | $0.99 |
| skins_pack    | Non-Consumable | $1.99 |
| coin_doubler  | Non-Consumable | $2.99 |
| coins_small   | Consumable     | $0.99 |
| coins_medium  | Consumable     | $2.99 |
| coins_large   | Consumable     | $4.99 |
| mega_pack     | Non-Consumable | $6.99 |

## Requirements
- macOS with Xcode 15+
- Apple Developer Program ($99/year)
- iOS 16+ target

## Notes
- Game runs fully offline (no internet required for gameplay)
- StoreKit 2 (async/await) — modern, clean API
- Same `emberstorm.html` as the Android version
- Same JS bridge interface (grantEntitlement, grantCoins, setPrices)
