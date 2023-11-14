package com.santha.inappsubscriptionone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.santha.inappsubscriptionone.databinding.ActivitySubsBinding;
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Subs extends AppCompatActivity {

    private BillingClient billingClient;
    String subsName,phases,des,dur;
    boolean isSuccess = false;
    ActivitySubsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySubsBinding.inflate(getLayoutInflater());
//        setContentView(R.layout.activity_subs);
        setContentView(binding.getRoot());
        billingClient = BillingClient.newBuilder(this)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();

        getPrices();

        if(ConnectionClass.premium){
            binding.tvSubsts.setText("Status: Already Subscribed");
            binding.btnSub.setVisibility(View.GONE);
        }else {
            binding.tvSubsts.setText("Status: Not Subscribed");
        }

    }

    private PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {

            if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null){

                for(Purchase purchase : purchases){
                    handlePurchase(purchase);
                }

            }else if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED){
                binding.tvSubsts.setText("Already Subscribed");
                isSuccess = true;
                ConnectionClass.premium = true;
                ConnectionClass.locked = false;
                binding.btnSub.setVisibility(View.GONE);
            }else if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED){
                binding.tvSubsts.setText("FEATURE_NOT_SUPPORTED");
            }else if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE){
                binding.tvSubsts.setText("BILLING_UNAVAILABLE");
            }else if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED){
                binding.tvSubsts.setText("USER_CANCELED");
            }else if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.DEVELOPER_ERROR){
                binding.tvSubsts.setText("DEVELOPER_ERROR");
            }else if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_UNAVAILABLE){
                binding.tvSubsts.setText("ITEM_UNAVAILABLE");
            }else if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.NETWORK_ERROR){
                binding.tvSubsts.setText("NETWORK_ERROR");
            }else if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED){
                binding.tvSubsts.setText("SERVICE_DISCONNECTED");
            }else{
                Toast.makeText(Subs.this, "Error "+billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
            }


        }
    };

    void handlePurchase(Purchase purchase) {

        ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();

        ConsumeResponseListener listener = (billingResult, s) -> {
            if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){

            }
        };

        billingClient.consumeAsync(consumeParams, listener);

        if(purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED){
            if(!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())){
                Toast.makeText(getApplicationContext(), "Error : invalid Purchase", Toast.LENGTH_SHORT).show();
                return;
            }

            if(!purchase.isAcknowledged()){
                AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams,acknowledgePurchaseResponseListener);
                binding.tvSubsts.setText("Subscribe");
                isSuccess = true;
            }else {
                binding.tvSubsts.setText("Already_Subscribed");
            }
            ConnectionClass.premium = true;
            ConnectionClass.locked = false;
            binding.btnSub.setVisibility(View.GONE);
        }else if(purchase.getPurchaseState() == Purchase.PurchaseState.PENDING){
            binding.tvSubsts.setText("Subscription PENDING");
        }else if(purchase.getPurchaseState() == Purchase.PurchaseState.UNSPECIFIED_STATE){
            binding.tvSubsts.setText("UNSPECIFIED_STATE");
        }

    }

    AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
        @Override
        public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
            if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                binding.tvSubsts.setText("Subscribed");
                isSuccess = true;
                ConnectionClass.premium = true;
                ConnectionClass.locked = false;
            }
        }
    };

    private boolean verifyValidSignature(String signedData, String signature) {
        try{
            String base64Key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqHJVRxrh30T+2LI7LCx7jJF3eLB3TkIZNWV3aNrKgLOJXn6j0eWXXLhNT9cACsA017Hu3T3XvTjOQMYTRSmF8horzQNTikJPYzx1XUHl0GCLR+/B8+HXQOlVEyFxHReR83ygAEj/1zJeKQpDkdGHRL7+m03CBkngeuecozk1l+CwUvkWIIRWX0Zq8RaTO45kNAxo6DZyguSyW2j2jeK86PPRiJux8roIjErz92LW3Qn6UrUmAp0OJMdnz5qqwBKkQu5azdAaF5d+3hKLlM2JDssjZo5x3NRvD0+lTcQ2F6iipJdx/1k02IbSE0w5IwTl26qvqJGM0NkRmgeu9JzGoQIDAQAB";
            return Security.verifyPurchase(base64Key, signedData, signature);
        } catch (IOException e){
            return  false;
        }
    }

    private void getPrices(){
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {

                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {


                            QueryProductDetailsParams queryProductDetailsParams =
                                    QueryProductDetailsParams.newBuilder()
                                            .setProductList(
                                                    ImmutableList.of(
                                                            QueryProductDetailsParams.Product.newBuilder()
                                                                    .setProductId("monthly_subscription")
                                                                    .setProductType(BillingClient.ProductType.SUBS)
                                                                    .build()))
                                            .build();

                            billingClient.queryProductDetailsAsync(
                                    queryProductDetailsParams,
                                    new ProductDetailsResponseListener() {
                                        public void onProductDetailsResponse(BillingResult billingResult,
                                                                             List<ProductDetails> productDetailsList) {
                                            for(ProductDetails productDetails:productDetailsList){

                                                String offerToken = productDetails.getSubscriptionOfferDetails()
                                                        .get(0).getOfferToken();

                                                ImmutableList productDetailsParamsList =
                                                        ImmutableList.of(
                                                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                                                        // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                                                                        .setProductDetails(productDetails)
                                                                        // to get an offer token, call ProductDetails.getSubscriptionOfferDetails()
                                                                        // for a list of offers that are available to the user
                                                                        .setOfferToken(offerToken)
                                                                        .build()
                                                        );
                                                subsName = productDetails.getName();
                                                des = productDetails.getDescription();
                                                String formattedPrice = productDetails.getSubscriptionOfferDetails().get(0).getPricingPhases()
                                                        .getPricingPhaseList().get(0).getFormattedPrice();
                                                String billingPeriod = productDetails.getSubscriptionOfferDetails().get(0).getPricingPhases()
                                                        .getPricingPhaseList().get(0).getBillingPeriod();
                                                int recurrenceMode = productDetails.getSubscriptionOfferDetails().get(0).getPricingPhases()
                                                        .getPricingPhaseList().get(0).getRecurrenceMode();

                                                String n,duration,bp;
                                                bp = billingPeriod;
                                                n = billingPeriod.substring(1,2);
                                                duration = billingPeriod.substring(2,3);
                                                if(recurrenceMode == 2){
                                                    if(duration.equals("M")){
                                                        dur = " For " + n +" Month";
                                                    } else if(duration.equals("Y")){
                                                        dur = " For " + n +" Year";
                                                    } else if(duration.equals("W")){
                                                        dur = " For " + n +" Week";
                                                    } else if(duration.equals("D")){
                                                        dur = " For " + n +" Days";
                                                    }
                                                }else{
                                                    if(bp.equals("P1M")){
                                                        dur = "/Monthly";
                                                    } else if (bp.equals("P6M")){
                                                        dur = "/Every 6 Month";
                                                    } else if(bp.equals("P1Y")){
                                                        dur = "/Yearly";
                                                    } else if(bp.equals("P1W")){
                                                        dur = "/Weekly";
                                                    } else if(bp.equals("P3W")){
                                                        dur = "Every /3 Week";
                                                    }
                                                }

                                                phases = formattedPrice+" "+ dur;

                                                for (int i=0; i<=(productDetails.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().size());i++){
                                                    if(i>0){
                                                        String period = productDetails.getSubscriptionOfferDetails().get(0).getPricingPhases()
                                                                .getPricingPhaseList().get(i).getBillingPeriod();
                                                        String price = productDetails.getSubscriptionOfferDetails().get(0).getPricingPhases()
                                                                .getPricingPhaseList().get(i).getFormattedPrice();

                                                        if(period.equals("P1M")){
                                                            dur ="/Monthly";
                                                        } else if(period.equals("P6M")){
                                                            dur = "/Every 6 Month";
                                                        } else if(period.equals("P1Y")){
                                                            dur = "/Yearly";
                                                        } else if(period.equals("P1W")){
                                                            dur = "/Weekly";
                                                        } else if(period.equals("P3W")){
                                                            dur = "Every /3 Week";
                                                        }

                                                        phases +="\n"+price+dur;

                                                    }
                                                }



                                            }
                                        }
                                    }
                            );


                        }
                    });
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                Thread.sleep(1000);
                            }catch (InterruptedException e){
                                e.printStackTrace();
                            }

                            binding.tvSubid.setText(subsName);
                            binding.txtprice.setText("Price: "+phases);
                            binding.tvBenifit.setText(des);

                        }
                    });

                }
            }
            @Override
            public void onBillingServiceDisconnected() {


            }
        });
    }

    public void getPrice_click(View view) {
        getPrices();
    }

    public void btn_sub_click(View view) {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {


            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {


                QueryProductDetailsParams queryProductDetailsParams =
                        QueryProductDetailsParams.newBuilder()
                                .setProductList(
                                        ImmutableList.of(
                                                QueryProductDetailsParams.Product.newBuilder()
                                                        .setProductId("monthly_subscription")
                                                        .setProductType(BillingClient.ProductType.SUBS)
                                                        .build()))
                                .build();

                billingClient.queryProductDetailsAsync(
                        queryProductDetailsParams,
                        new ProductDetailsResponseListener() {
                            public void onProductDetailsResponse(BillingResult billingResult,
                                                                 List<ProductDetails> productDetailsList) {
                                for(ProductDetails productDetails:productDetailsList){

                                    String offerToken = productDetails.getSubscriptionOfferDetails()
                                            .get(0).getOfferToken();

                                    ImmutableList productDetailsParamsList =
                                            ImmutableList.of(
                                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                                            // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                                                            .setProductDetails(productDetails)
                                                            // to get an offer token, call ProductDetails.getSubscriptionOfferDetails()
                                                            // for a list of offers that are available to the user
                                                            .setOfferToken(offerToken)
                                                            .build()
                                            );

                                    BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                            .setProductDetailsParamsList(productDetailsParamsList)
                                            .build();

                                    // Launch the billing flow
                                    billingClient.launchBillingFlow(Subs.this, billingFlowParams);



                                }
                            }
                        }
                );


            }
        });

    }

    public void quit_click(View view) {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(billingClient != null){
            billingClient.endConnection();
        }
    }
}