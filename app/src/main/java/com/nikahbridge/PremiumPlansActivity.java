package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.*;

public class PremiumPlansActivity extends Activity {
    private LinearLayout root;
    private BillingClient billing;
    private FirebaseFunctions functions;
    private final Map<String, ProductDetails> products = new HashMap<>();
    private TextView status;

    private void add(String text, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(bold ? Color.rgb(30,45,41) : Color.rgb(95,108,103));
        t.setPadding(8,10,8,10);
        if (bold) t.setTypeface(null, 1);
        root.addView(t);
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(16);
        root.addView(b, new LinearLayout.LayoutParams(-1, 64));
        return b;
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        functions = FirebaseFunctions.getInstance();
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22,24,22,30);
        scroll.addView(root);
        setContentView(scroll);

        add("Best Nikah Bridge — Premium", 27, true);
        add("Real Google Play purchases only. No demo plans, fake balances, or simulated payments.", 15, false);
        add("Choose a plan. The final price shown by Google Play is the price configured in Play Console for your country.", 14, false);
        status = new TextView(this);
        status.setTextSize(15);
        status.setPadding(8,12,8,18);
        root.addView(status);

        Button p20 = button("20 SAR Premium — Buy");
        Button p40 = button("40 SAR Premium — Buy");
        Button p60 = button("60 SAR Premium — Buy");
        Button refresh = button("Refresh Premium Status");
        Button back = button("Back");
        p20.setOnClickListener(v -> buy("bnb_plus_20"));
        p40.setOnClickListener(v -> buy("bnb_plus_40"));
        p60.setOnClickListener(v -> buy("bnb_plus_60"));
        refresh.setOnClickListener(v -> loadEntitlement());
        back.setOnClickListener(v -> finish());

        status.setText("Connecting to Google Play…");
        connectBilling();
        loadEntitlement();
    }

    private void connectBilling() {
        billing = BillingClient.newBuilder(this)
                .setListener(this::onPurchasesUpdated)
                .enablePendingPurchases()
                .build();
        billing.startConnection(new BillingClientStateListener() {
            @Override public void onBillingSetupFinished(BillingResult result) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProducts();
                } else {
                    status.setText("Google Play Billing unavailable: " + result.getDebugMessage());
                }
            }
            @Override public void onBillingServiceDisconnected() {
                status.setText("Google Play connection interrupted. Tap Refresh and try again.");
            }
        });
    }

    private void queryProducts() {
        List<QueryProductDetailsParams.Product> list = new ArrayList<>();
        for (String id : new String[]{"bnb_plus_20","bnb_plus_40","bnb_plus_60"}) {
            list.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build());
        }
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(list)
                .build();
        billing.queryProductDetailsAsync(params, (result, details) -> {
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                status.setText("Plans could not be loaded from Google Play: " + result.getDebugMessage());
                return;
            }
            products.clear();
            for (ProductDetails d : details) products.put(d.getProductId(), d);
            if (products.size() == 3) {
                status.setText("All 3 real Google Play plans are available. Prices are supplied by Google Play.");
            } else {
                status.setText("Some plans are not active in Google Play yet. Activate the missing products in Play Console before selling.");
            }
        });
    }

    private void buy(String productId) {
        ProductDetails details = products.get(productId);
        if (details == null) {
            new AlertDialog.Builder(this).setTitle("Plan unavailable")
                    .setMessage("This product is not currently available from Google Play. Make sure the product is active in Play Console and the app is installed from a test/release track.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        List<ProductDetails.OneTimePurchaseOfferDetails> offers = details.getOneTimePurchaseOfferDetailsList();
        if (offers == null || offers.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Plan unavailable")
                    .setMessage("Google Play did not return a valid one-time purchase offer for this product.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        String token = offers.get(0).getOfferToken();
        BillingFlowParams.ProductDetailsParams pd = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(token)
                .build();
        BillingFlowParams flow = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(pd))
                .build();
        BillingResult result = billing.launchBillingFlow(this, flow);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            status.setText("Google Play purchase could not start: " + result.getDebugMessage());
        }
    }

    private void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
        if (result.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            status.setText("Purchase cancelled. No charge was made by the app.");
            return;
        }
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK || purchases == null) {
            status.setText("Google Play purchase failed: " + result.getDebugMessage());
            return;
        }
        for (Purchase purchase : purchases) {
            if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) continue;
            for (String productId : purchase.getProducts()) verifyOnServer(productId, purchase.getPurchaseToken());
        }
    }

    private void verifyOnServer(String productId, String purchaseToken) {
        status.setText("Purchase received. Verifying securely with Google Play…");
        Map<String,Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("purchaseToken", purchaseToken);
        functions.getHttpsCallable("verifyPremiumPurchase").call(data)
                .addOnSuccessListener(result -> {
                    status.setText("Purchase verified. Premium access and message credits have been activated.");
                    loadEntitlement();
                })
                .addOnFailureListener(error -> status.setText("Purchase verification failed. No premium access is granted until Google Play verification succeeds."));
    }

    private void loadEntitlement() {
        functions.getHttpsCallable("getPremiumEntitlement").call(new HashMap<>())
                .addOnSuccessListener(result -> {
                    Object data = result.getData();
                    status.setText("Current premium status: " + String.valueOf(data));
                })
                .addOnFailureListener(error -> status.setText("Premium status unavailable: " + error.getMessage()));
    }

    @Override protected void onDestroy() {
        if (billing != null) billing.endConnection();
        super.onDestroy();
    }
}
