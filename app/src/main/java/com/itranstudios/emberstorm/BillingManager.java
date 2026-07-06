package com.itranstudios.emberstorm;

import android.app.Activity;
import android.content.Context;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around Google Play Billing (library 7.x).
 * Grants/consumes are reported back through Callback, which the Activity
 * forwards into the web layer (window.grantEntitlement / grantCoins / setPrices).
 *
 * NOTE: These product IDs must be created in the Google Play Console
 * (Monetize > Products > In-app products) with matching prices.
 */
public class BillingManager implements PurchasesUpdatedListener {

    public interface Callback {
        void onGrant(String sku);
        void onCoins(int coins);
        void onPrices(String json);
        void onFailed();
    }

    static final String[] NON_CONSUMABLE = { "skins_pack", "coin_doubler", "mega_pack" };
    static final String[] CONSUMABLE = { "coins_large" };
    static final Map<String, Integer> COIN_VALUES = new HashMap<String, Integer>() {{
        put("coins_large", 2000);
    }};

    private final BillingClient client;
    private final Callback cb;
    private final Map<String, ProductDetails> details = new HashMap<>();

    BillingManager(Context ctx, Callback cb) {
        this.cb = cb;
        client = BillingClient.newBuilder(ctx)
                .setListener(this)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();
    }

    void start() {
        client.startConnection(new BillingClientStateListener() {
            @Override public void onBillingSetupFinished(BillingResult r) {
                if (r.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProducts();
                    restorePurchases();
                }
            }
            @Override public void onBillingServiceDisconnected() { }
        });
    }

    private QueryProductDetailsParams.Product product(String id) {
        return QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build();
    }

    private void queryProducts() {
        List<QueryProductDetailsParams.Product> list = new ArrayList<>();
        for (String id : NON_CONSUMABLE) list.add(product(id));
        for (String id : CONSUMABLE) list.add(product(id));

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(list).build();

        client.queryProductDetailsAsync(params, (result, productDetailsList) -> {
            JSONObject prices = new JSONObject();
            for (ProductDetails pd : productDetailsList) {
                details.put(pd.getProductId(), pd);
                ProductDetails.OneTimePurchaseOfferDetails offer = pd.getOneTimePurchaseOfferDetails();
                if (offer != null) {
                    try { prices.put(pd.getProductId(), offer.getFormattedPrice()); } catch (Exception ignored) {}
                }
            }
            if (prices.length() > 0) cb.onPrices(prices.toString());
        });
    }

    void launchPurchase(Activity activity, String sku) {
        ProductDetails pd = details.get(sku);
        if (pd == null) { cb.onFailed(); return; }
        List<BillingFlowParams.ProductDetailsParams> pl = new ArrayList<>();
        pl.add(BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(pd).build());
        BillingFlowParams flow = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(pl).build();
        client.launchBillingFlow(activity, flow);
    }

    @Override
    public void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase p : purchases) handlePurchase(p);
        } else {
            cb.onFailed();
        }
    }

    private void restorePurchases() {
        client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP).build(),
                (result, purchases) -> { for (Purchase p : purchases) handlePurchase(p); });
    }

    private void handlePurchase(Purchase p) {
        if (p.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return;
        for (String id : p.getProducts()) {
            if (COIN_VALUES.containsKey(id)) {
                cb.onCoins(COIN_VALUES.get(id));
                client.consumeAsync(
                        ConsumeParams.newBuilder().setPurchaseToken(p.getPurchaseToken()).build(),
                        (r, token) -> {});
            } else {
                cb.onGrant(id);
                if (!p.isAcknowledged()) {
                    client.acknowledgePurchase(
                            AcknowledgePurchaseParams.newBuilder()
                                    .setPurchaseToken(p.getPurchaseToken()).build(),
                            r -> {});
                }
            }
        }
    }
}
