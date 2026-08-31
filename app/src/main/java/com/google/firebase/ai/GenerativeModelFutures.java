package com.google.firebase.ai;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;

/** Compatibility facade that delegates to the official Firebase AI Logic Java API. */
public final class GenerativeModelFutures {
    private final com.google.firebase.ai.java.GenerativeModelFutures delegate;

    private GenerativeModelFutures(com.google.firebase.ai.java.GenerativeModelFutures delegate) {
        this.delegate = delegate;
    }

    public static GenerativeModelFutures from(GenerativeModel model) {
        return new GenerativeModelFutures(
                com.google.firebase.ai.java.GenerativeModelFutures.from(model));
    }

    public ListenableFuture<GenerateContentResponse> generateContent(Content prompt) {
        return delegate.generateContent(prompt);
    }
}
