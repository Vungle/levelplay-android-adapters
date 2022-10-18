package com.ironsource.adapters.vungle;

public interface AdAdapter {

    void onInitializationSuccess();

    void onInitializationFailure(String error);

    void releaseMemory();

}
