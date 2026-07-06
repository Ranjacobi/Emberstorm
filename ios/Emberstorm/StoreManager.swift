import StoreKit

/// StoreKit 2 wrapper — same 7 products as Android.
/// Handles product loading, purchasing, restoring, and reporting back to the game.
@MainActor
class StoreManager {

    enum Event {
        case granted(String)   // non-consumable purchased or restored
        case coins(Int)        // consumable coin pack
        case prices(String)    // JSON { sku: "$0.99", ... }
        case failed
    }

    // product IDs — must match App Store Connect
    static let nonConsumable = ["starter_pack", "skins_pack", "coin_doubler", "mega_pack"]
    static let consumable    = ["coins_small", "coins_medium", "coins_large"]
    static let allIDs        = Set(nonConsumable + consumable)
    static let coinValues: [String: Int] = [
        "coins_small": 500,
        "coins_medium": 1800,
        "coins_large": 4000,
    ]

    private var products: [String: Product] = [:]
    private var callback: (Event) -> Void
    private var updateTask: Task<Void, Never>?

    init(callback: @escaping (Event) -> Void) {
        self.callback = callback
        // listen for transactions (purchases, restores, renewals)
        updateTask = Task { [weak self] in
            for await result in Transaction.updates {
                guard let self else { return }
                if let tx = try? result.payloadValue {
                    await self.handle(tx)
                }
            }
        }
    }

    deinit { updateTask?.cancel() }

    // load products and send prices to the game
    func loadProducts() async {
        do {
            let storeProducts = try await Product.products(for: Self.allIDs)
            var priceMap: [String: String] = [:]
            for p in storeProducts {
                products[p.id] = p
                priceMap[p.id] = p.displayPrice
            }
            if !priceMap.isEmpty {
                let data = try JSONSerialization.data(withJSONObject: priceMap)
                if let json = String(data: data, encoding: .utf8) {
                    callback(.prices(json))
                }
            }
            // restore past purchases
            await restorePurchases()
        } catch {
            print("StoreManager: failed to load products: \(error)")
        }
    }

    func purchase(_ sku: String) async {
        guard let product = products[sku] else { callback(.failed); return }
        do {
            let result = try await product.purchase()
            switch result {
            case .success(let verification):
                if let tx = try? verification.payloadValue {
                    await handle(tx)
                }
            case .userCancelled:
                callback(.failed)
            case .pending:
                break // waiting for approval (e.g. Ask to Buy)
            @unknown default:
                callback(.failed)
            }
        } catch {
            callback(.failed)
        }
    }

    private func handle(_ tx: Transaction) async {
        let id = tx.productID
        if let coins = Self.coinValues[id] {
            callback(.coins(coins))
            await tx.finish() // consumable — finish immediately
        } else {
            callback(.granted(id))
            await tx.finish()
        }
    }

    private func restorePurchases() async {
        for await result in Transaction.currentEntitlements {
            if let tx = try? result.payloadValue {
                // only restore non-consumables
                if Self.nonConsumable.contains(tx.productID) {
                    callback(.granted(tx.productID))
                }
            }
        }
    }
}
