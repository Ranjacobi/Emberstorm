package com.itranstudios.emberstorm;

import android.app.Activity;
import android.content.Context;
import com.android.billingclient.api.*;
import org.json.JSONObject;
import java.util.*;

public class BillingManager implements PurchasesUpdatedListener {
    public interface Callback {
        void onGrant(String sku);
        void onCoins(int coins);
        void onPrices(String json);
        void onFailed();
    }

    static final String[] NON_CONSUMABLE = { "starter_pack", "skins_pack", "coin_doubler", "mega_pack" };
    static final String[] CONSUMABLE = { "coins_small", "coins_medium", "coins_large" };
    static final Map<String, Integer> COIN_VALUES = new HashMap<String, Integer>() {{
        put("coins_small", 500);
        put("coins_medium", 1800);
        put("coins_large", 4000);
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
                    queryProducts(); restorePurchases();
                }
            }
            @Override public void onBillingServiceDisconnected() { }
        });
    }

    private QueryProductDetailsParams.Product product(String id) {
        return QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id).setProductType(BillingClient.ProductType.INAPP).build();
    }

    private void queryProducts() {
        List<QueryProductDetailsParams.Product> list = new ArrayList<>();
        for (String id : NON_CONSUMABLE) list.add(product(id));
        for (String id : CONSUMABLE) list.add(product(id));
        client.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(list).build(),
            (result, productDetailsList) -> {
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
        pl.add(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd).build());
        client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(pl).build());
    }

    @Override
    public void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase p : purchases) handlePurchase(p);
        } else { cb.onFailed(); }
    }

    private void restorePurchases() {
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
            (result, purchases) -> { for (Purchase p : purchases) handlePurchase(p); });
    }

    private void handlePurchase(Purchase p) {
        if (p.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return;
        for (String id : p.getProducts()) {
            if (COIN_VALUES.containsKey(id)) {
                cb.onCoins(COIN_VALUES.get(id));
                client.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(p.getPurchaseToken()).build(), (r, token) -> {});
            } else {
                cb.onGrant(id);
                if (!p.isAcknowledged()) {
                    client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.getPurchaseToken()).build(), r -> {});
                }
            }
        }
    }
}
