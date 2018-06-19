#import <Foundation/Foundation.h>
#if __has_include(<React/RCTBridgeModule.h>)
#import <React/RCTBridgeModule.h>
#else
#import "RCTBridgeModule.h"
#endif
#import "RCTLog.h"

@interface RNSslPinning : NSObject <RCTBridgeModule>

@end
  
