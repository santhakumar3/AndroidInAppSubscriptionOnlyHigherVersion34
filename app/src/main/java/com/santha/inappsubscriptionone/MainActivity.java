package com.santha.inappsubscriptionone;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private BillingClient billingClient;
    boolean isPremium = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        billingClient = BillingClient.newBuilder(this)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();
        query_purchase();

    }

    private PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
            // To be implemented in a later section.
        }
    };

    public void btnClick(View view) {

        Intent i = new Intent(MainActivity.this, Subs.class);
        startActivity(i);

    }

    private void query_purchase(){
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {

                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                    executorService.execute(() -> {
                        try{
                            billingClient.queryPurchasesAsync(
                                    QueryPurchasesParams.newBuilder()
                                            .setProductType(BillingClient.ProductType.SUBS)
                                            .build(),
                                    (billingResult1, purchaseList) -> {
                                        // check billingResult
                                        for(Purchase purchase : purchaseList){
                                            if(purchase != null && purchase.isAcknowledged()){
                                                isPremium = true;
                                            }
                                        }
                                    }
                            );
                        }catch (Exception ex){
                            isPremium = false;
                        }

                        runOnUiThread(() -> {
                            try{
                                Thread.sleep(1000);
                            }catch (InterruptedException e){
                                e.printStackTrace();
                            }

                            if(isPremium){
                                ConnectionClass.premium = true;
                                ConnectionClass.locked = false;
                            }else{
                                ConnectionClass.premium = false;
                            }

                        });

                    });


                }
            }
            @Override
            public void onBillingServiceDisconnected() {

            }
        });
    }
}