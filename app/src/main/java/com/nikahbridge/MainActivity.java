package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerativeBackend;

import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Best Nikah Bridge - production Android entry point.
 * Real Firebase data only. No demo members, fake scores, fake verification,
 * local-only connections, or hard-coded match records.
 */
public class MainActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseFunctions functions;
    private LinearLayout root;
    private ExecutorService aiExecutor;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(95,108,103), red=Color.rgb(165,50,50), light=Color.rgb(247,250,249);

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        auth=FirebaseAuth.getInstance();
        db=FirebaseFirestore.getInstance();
        functions=FirebaseFunctions.getInstance();
        aiExecutor=Executors.newSingleThreadExecutor();
        route();
    }

    @Override protected void onDestroy(){
        if(aiExecutor!=null) aiExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override public void onBackPressed(){ if(auth.getCurrentUser()==null) authScreen(); else home(); }

    private void route(){
        FirebaseUser u=auth.getCurrentUser();