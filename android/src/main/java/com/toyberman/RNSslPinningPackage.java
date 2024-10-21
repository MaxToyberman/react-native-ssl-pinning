
package com.toyberman;

import androidx.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.module.model.ReactModuleInfoProvider;

import com.facebook.react.TurboReactPackage;
import java.util.HashMap;
import java.util.Map;

public class RNSslPinningPackage extends TurboReactPackage {

    @Nullable
    @Override
    public NativeModule getModule(String name, ReactApplicationContext reactContext) {
      if (name.equals(RNSslPinningImpl.NAME)) {
        return new RNSslPinningModule(reactContext);
      } else {
        return null;
      }
    }

    @Override
    public ReactModuleInfoProvider getReactModuleInfoProvider() {
              return () -> {
          final Map<String, ReactModuleInfo> moduleInfos = new HashMap<>();
          boolean isTurboModule = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED;
          moduleInfos.put(
                  RNSslPinningImpl.NAME,
                  new ReactModuleInfo(
                          RNSslPinningImpl.NAME,
                          RNSslPinningImpl.NAME,
                          false, // canOverrideExistingModule
                          false, // needsEagerInit
                          true, // hasConstants
                          false, // isCxxModule
                          isTurboModule // isTurboModule
          ));
          return moduleInfos;
      };
    }
}