//
//  RNNativeFetch.m
//  medipass
//
//  Created by Paul Wong on 13/10/16.
//  Copyright © 2016 Localz. All rights reserved.
//

#import "RNSslPinning.h"
#import "AFNetworking.h"

@interface RNSslPinning()

@property (nonatomic, strong) NSURLSessionConfiguration *sessionConfig;

@end

@implementation RNSslPinning
RCT_EXPORT_MODULE();

- (instancetype)init
{
    self = [super init];
    if (self) {
        self.sessionConfig = [NSURLSessionConfiguration ephemeralSessionConfiguration];
        self.sessionConfig.HTTPCookieStorage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    }
    return self;
}

RCT_REMAP_METHOD(getCookies, findEventsWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    
    NSHTTPCookie *cookie;
    NSHTTPCookieStorage* cookieJar  =  NSHTTPCookieStorage.sharedHTTPCookieStorage;
    
    NSMutableDictionary* dictionary = @{}.mutableCopy;
    
    for (cookie in [cookieJar cookies]) {
        [dictionary setObject:cookie.value forKey:cookie.name];
    }
    
    if ([dictionary count] > 0){
        resolve(dictionary);
    }
    else{
        NSError *error = nil;
        reject(@"no_cookies", @"There were no cookies", error);
    }
}

RCT_EXPORT_METHOD(fetch:(NSString *)url obj:(NSDictionary *)obj callback:(RCTResponseSenderBlock)callback) {
    NSURL *u = [NSURL URLWithString:url];
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] initWithURL:u];
    
    if (obj) {
        if (obj[@"method"]) {
            [request setHTTPMethod:obj[@"method"]];
        }
        if (obj[@"timeoutInterval"]) {
            [request setTimeoutInterval:[obj[@"timeoutInterval"] doubleValue] / 1000];
        }
        if (obj[@"headers"] && [obj[@"headers"] isKindOfClass:[NSDictionary class]]) {
            NSMutableDictionary *m = [obj[@"headers"] mutableCopy];
            for (NSString *key in [m allKeys]) {
                if (![m[key] isKindOfClass:[NSString class]]) {
                    m[key] = [m[key] stringValue];
                }
            }
            [request setAllHTTPHeaderFields:m];
        }
        if (obj[@"body"]) {
            NSData *data = [obj[@"body"] dataUsingEncoding:NSUTF8StringEncoding];
            [request setHTTPBody:data];
        }
    }

    AFSecurityPolicy *policy = [AFSecurityPolicy policyWithPinningMode:AFSSLPinningModePublicKey];
    AFURLSessionManager *manager = [[AFURLSessionManager alloc] initWithSessionConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration]];
    manager.securityPolicy = policy;
    

    manager.responseSerializer = [AFHTTPResponseSerializer serializer];
    
    [[manager dataTaskWithRequest:request completionHandler:^(NSURLResponse * _Nonnull response, id  _Nullable responseObject, NSError * _Nullable error) {
        
        NSHTTPURLResponse *httpResp = (NSHTTPURLResponse*) response;
        
        if (!error) {
            NSLog(@"Reply JSON: %@", responseObject);
            NSString *bodyString = [[NSString alloc] initWithData: responseObject encoding:NSUTF8StringEncoding];
            NSInteger statusCode = httpResp.statusCode;
            
            NSDictionary *res = @{
                                  @"status": @(statusCode),
                                  @"headers": httpResp.allHeaderFields,
                                  @"bodyString": bodyString
                                  };
            callback(@[[NSNull null], res]);
            
            
        } else {
            dispatch_async(dispatch_get_main_queue(), ^{
                NSInteger errorCode = error.code;
                callback(@[@{@"message":error.localizedDescription}, [NSNull null]]);
            });
        }
    }] resume];
    
}

@end

