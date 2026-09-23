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
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class PremiumPlansActivity extends Activity {
    private LinearLayout root;
    private BillingClient billing;
    private final Map<String, String> azurePlanBasePlans = new HashMap<>();
    private final Map<String, ProductDetails> products = new HashMap<>();
    private TextView status;

    private void add(String text, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(bold ? Premium2030Ui.TEXT : Premium2030Ui.MUTED);
        t.setPadding(8,10,8,10);
        if (bold) t.setTypeface(null, 1);
        root.addView(t);
    }

    private Button button(String text) {
        boolean buy=text.contains("Buy");
        Button b = buy ? Premium2030Ui.primary(this,text) : Premium2030Ui.secondary(this,text);
        Premium2030Ui.addButton(root,b);
        return b;
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setBackgroundColor(Premium2030Ui.CREAM);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Premium2030Ui.dp(this,18),Premium2030Ui.dp(this,20),Premium2030Ui.dp(this,18),Premium2030Ui.dp(this,30));
        scroll.addView(root);
        setContentView(scroll);

        root.addView(Premium2030Ui.title(this,"Go Premium"));
        root.addView(Premium2030Ui.subtitle(this,"Unlock more opportunities for your halal journey."));
        root.addView(Premium2030Ui.heroLine(this,"20 • 40 • 60 SAR — Real Google Play Billing"));
        add("Real Google Play purchases only. Premium access is granted only after verified Play purchase confirmation.", 14, false);
        add("The final price and payment screen are supplied by Google Play for your country.", 14, false);
        status = new TextView(this);
        status.setTextSize(15);
        status.setPadding(8,12,8,18);
        root.addView(status);

        Button p20 = button("20 SAR Premium — Buy");
        Button p40 = button("40 SAR Premium — Buy");
        Button p60 = button("60 SAR Premium — Buy");
        Button refresh = button("Refresh Premium Status");
        Button back = button("Back");
        p20.setOnClickListener(v -> buy("premium_basic_20"));
        p40.setOnClickListener(v -> buy("premium_plus_40"));
        p60.setOnClickListener(v -> buy("premium_vip_60"));
        refresh.setOnClickListener(v -> { loadAzurePlans(); loadAzureEntitlement(); });
        back.setOnClickListener(v -> finish());

        status.setText("Connecting to Google Play…");
        connectBilling();
        loadAzurePlans();
        loadAzureEntitlement();
    }

    private void connectBilling() {
        billing = BillingClient.newBuilder(this)
                .setListener(this::onPurchasesUpdated)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
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
        for (String id : new String[]{"premium_basic_20","premium_plus_40","premium_vip_60"}) {
            list.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.SUBS)
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
            for (ProductDetails d : details.getProductDetailsList()) products.put(d.getProductId(), d);
            if (products.size() == 3) {
                status.setText("All 3 real Google Play plans are available. Prices are supplied by Google Play.");
            } else {
                status.setText("Some plans are not active in Google Play yet. Activate the missing products in Play Console before selling.");
            }
        });
    }

    /** Primary production server path: Azure External ID -> Azure Functions -> PostgreSQL. */
    private void loadAzurePlans() {
        if (!AzureAuthManager.hasAccount(this)) { status.setText("Please sign in with Azure."); return; }
        AzureApiClient.get("/premium/plans", new AzureApiClient.Callback() {
            @Override public void ok(int code, String body) {
                try {
                    JSONArray plans = new JSONObject(body).getJSONArray("plans");
                    azurePlanBasePlans.clear();
                    for (int i=0;i<plans.length();i++) {
                        JSONObject p=plans.getJSONObject(i);
                        azurePlanBasePlans.put(p.getString("productId"), p.optString("basePlanId",""));
                    }
                    runOnUiThread(() -> status.setText("Premium catalog verified by Azure. Google Play supplies the live price."));
                } catch(Exception e) { runOnUiThread(() -> status.setText("Azure premium catalog response is invalid.")); }
            }
            @Override public void err(String message) { runOnUiThread(() -> status.setText("Azure premium catalog unavailable: "+message)); }
        });
    }

    private void loadAzureEntitlement() {
        if (!AzureAuthManager.hasAccount(this)) return;
        AzureApiClient.get("/premium/entitlement", new AzureApiClient.Callback() {
            @Override public void ok(int code,String body) {
                try {
                    JSONObject o=new JSONObject(body);
                    boolean active=o.optBoolean("active",false);
                    JSONObject e=o.optJSONObject("entitlement");
                    String plan=e==null?"":e.optString("plan_key","");
                    String expires=e==null?"":e.optString("expires_at","");
                    runOnUiThread(() -> status.setText(active
                        ? "Azure Premium active: "+plan+" • expires "+expires
                        : "Azure Premium: not active"));
                } catch(Exception e) { runOnUiThread(() -> status.setText("Azure premium status is invalid.")); }
            }
            @Override public void err(String message) { runOnUiThread(() -> status.setText("Azure premium status unavailable: "+message)); }
        });
    }

    private void verifyOnAzure(String productId, String purchaseToken) {
        status.setText("Purchase received. Azure is verifying it with Google Play…");
        try {
            JSONObject data=new JSONObject();
            data.put("productId",productId);
            data.put("purchaseToken",purchaseToken);
            AzureApiClient.post("/premium/purchases/verify",data.toString(),new AzureApiClient.Callback(){
                @Override public void ok(int code,String body){runOnUiThread(()->{status.setText("Google Play purchase verified by Azure. Premium activated.");loadAzureEntitlement();});}
                @Override public void err(String message){runOnUiThread(()->status.setText("Azure purchase verification failed. Premium is not granted: "+message));}
            });
        } catch(Exception e) { status.setText("Could not send purchase to Azure."); }
    }

    private void buy(String productId) {
        ProductDetails details = products.get(productId);
        if (details == null) {
            new AlertDialog.Builder(this).setTitle("Plan unavailable")
                    .setMessage("This product is not currently available from Google Play. Make sure the product is active in Play Console and the app is installed from a test/release track.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        List<ProductDetails.SubscriptionOfferDetails> offers = details.getSubscriptionOfferDetails();
        if (offers == null || offers.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Plan unavailable")
                    .setMessage("Google Play did not return a valid subscription offer for this product.")
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
            for (String productId : purchase.getProducts()) verifyOnAzure(productId, purchase.getPurchaseToken());
        }
    }


    @Override protected void onDestroy() {
        if (billing != null) billing.endConnection();
        super.onDestroy();
    }
}
