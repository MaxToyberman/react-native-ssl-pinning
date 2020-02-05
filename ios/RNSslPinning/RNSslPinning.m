//
//  RNNativeFetch.m
//  medipass
//
//  Created by Max Toyberman on 13/10/16.
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

RCT_EXPORT_METHOD(getCookies: (NSURL *)url resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    
    NSHTTPCookie *cookie;
    NSHTTPCookieStorage* cookieJar  =  NSHTTPCookieStorage.sharedHTTPCookieStorage;
    
    NSMutableDictionary* dictionary = @{}.mutableCopy;
    
    for (cookie in [cookieJar cookiesForURL:url]) {
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



RCT_EXPORT_METHOD(removeCookieByName: (NSString *)cookieName
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    
    NSHTTPCookieStorage *cookieStorage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    for (NSHTTPCookie *cookie in cookieStorage.cookies) {
        // [cookieStorage deleteCookie:each];
        NSString * name = cookie.name;
        
        if([cookieName isEqualToString:name]) {
            [cookieStorage deleteCookie:cookie];
        }
    }
    
    resolve(nil);
    
}


-(void)performRequest:(AFURLSessionManager*)manager  obj:(NSDictionary *)obj  request:(NSMutableURLRequest*) request callback:(RCTResponseSenderBlock) callback  {
    
    [[manager dataTaskWithRequest:request completionHandler:^(NSURLResponse * _Nonnull response, id  _Nullable responseObject, NSError * _Nullable error) {
        
        NSHTTPURLResponse *httpResp = (NSHTTPURLResponse*) response;
        NSString *bodyString = [[NSString alloc] initWithData: responseObject encoding:NSUTF8StringEncoding];
        NSInteger statusCode = httpResp.statusCode;
        
        if (!error) {
            // if(obj[@"responseType"]){
            NSString * responseType = obj[@"responseType"];
            
            if ([responseType isEqualToString:@"base64"]){
                NSString* base64String = [responseObject base64EncodedStringWithOptions:0];
                callback(@[[NSNull null], @{
                               @"status": @(statusCode),
                               @"headers": httpResp.allHeaderFields,
                               @"data": base64String
                }]);
            }
            else {
                callback(@[[NSNull null], @{
                               @"status": @(statusCode),
                               @"headers": httpResp.allHeaderFields,
                               @"bodyString": bodyString ? bodyString : @""
                }]);
            }
        } else if (error && error.userInfo[AFNetworkingOperationFailingURLResponseDataErrorKey]) {
            dispatch_async(dispatch_get_main_queue(), ^{
                callback(@[@{
                               @"status": @(statusCode),
                               @"headers": httpResp.allHeaderFields,
                               @"bodyString": bodyString ? bodyString : @""
                }, [NSNull null]]);
            });
        } else {
            dispatch_async(dispatch_get_main_queue(), ^{
                callback(@[error.localizedDescription, [NSNull null]]);
            });
        }
    }] resume];
    
}


-(void) setHeaders: (NSDictionary *)obj request:(NSMutableURLRequest*) request {
    
    if (obj[@"headers"] && [obj[@"headers"] isKindOfClass:[NSDictionary class]]) {
        NSMutableDictionary *m = [obj[@"headers"] mutableCopy];
        for (NSString *key in [m allKeys]) {
            if (![m[key] isKindOfClass:[NSString class]]) {
                m[key] = [m[key] stringValue];
            }
        }
        [request setAllHTTPHeaderFields:m];
    }
    
}

-(void)performMultipartRequest: (AFURLSessionManager*)manager obj:(NSDictionary *)obj url:(NSString *)url request:(NSMutableURLRequest*) request callback:(RCTResponseSenderBlock) callback formData:(NSDictionary*) formData {
    
    
    NSMutableURLRequest *formDataRequest = [[AFHTTPRequestSerializer serializer] multipartFormRequestWithMethod:@"POST" URLString:url parameters:nil constructingBodyWithBlock:^(id<AFMultipartFormData> _formData) {
        if([formData objectForKey:@"_parts"]){
            NSArray * parts = formData[@"_parts"];
            for (int i = 0; i < [parts count]; i++)
            {
                NSArray * part = parts[i];
                if([part[0] isKindOfClass:[NSString class]]) {
                    NSString * key = part[0];
                    if ([key isEqualToString:@"file"])
                    {
                        NSDictionary * value = part[1];
                        NSString * fileName = [value objectForKey:@"fileName"];
                        NSString * mimeType = [value objectForKey:@"type"];
                        NSString * path = [value objectForKey:@"path"];
                        NSError *error1;
                        [_formData appendPartWithFileURL:[NSURL URLWithString:path] name:@"file" fileName:fileName mimeType:mimeType error:&error1];
                        NSLog(@"Upload file error: %@",error1);
                    }
                    else  {
                        NSString * value = part[1];
                        NSData *data = [value dataUsingEncoding:NSUTF8StringEncoding];
                        [_formData appendPartWithFormData:data name: key];
                    }
                }
            }
        }
    } error:nil];
    
    // Migrate header fields.
    [formDataRequest setAllHTTPHeaderFields:[request allHTTPHeaderFields]];
    
    NSURLSessionUploadTask *uploadTask = [manager
                                          uploadTaskWithStreamedRequest:formDataRequest
                                          progress:^(NSProgress * _Nonnull uploadProgress) {
        NSLog(@"Upload progress %lld", uploadProgress.completedUnitCount / uploadProgress.totalUnitCount);
    }
                                          completionHandler:^(NSURLResponse * _Nonnull response, id  _Nullable responseObject, NSError * _Nullable error) {
        NSHTTPURLResponse *httpResp = (NSHTTPURLResponse*) response;
        NSString *bodyString = [[NSString alloc] initWithData: responseObject encoding:NSUTF8StringEncoding];
        NSInteger statusCode = httpResp.statusCode;
        if (!error) {
            NSLog(@"%@ %@", response, responseObject);
            
            NSHTTPURLResponse *httpResp = (NSHTTPURLResponse*) response;
            
            NSString *bodyString = [[NSString alloc] initWithData: responseObject encoding:NSUTF8StringEncoding];
            NSInteger statusCode = httpResp.statusCode;
            
            NSDictionary *res = @{
                @"status": @(statusCode),
                @"headers": httpResp.allHeaderFields,
                @"bodyString": bodyString ? bodyString : @""
            };
            callback(@[[NSNull null], res]);
        }
        else if (error && error.userInfo[AFNetworkingOperationFailingURLResponseDataErrorKey]) {
            dispatch_async(dispatch_get_main_queue(), ^{
                callback(@[@{
                               @"status": @(statusCode),
                               @"headers": httpResp.allHeaderFields,
                               @"bodyString": bodyString ? bodyString : @""
                }, [NSNull null]]);
            });
        } else {
            dispatch_async(dispatch_get_main_queue(), ^{
                callback(@[error.localizedDescription, [NSNull null]]);
            });
        }
    }];
    
    [uploadTask resume];
}

RCT_EXPORT_METHOD(fetch:(NSString *)url obj:(NSDictionary *)obj callback:(RCTResponseSenderBlock)callback) {
    NSURL *u = [NSURL URLWithString:url];
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] initWithURL:u];
    // set policy (ssl pinning)
    AFSecurityPolicy *policy = [AFSecurityPolicy policyWithPinningMode:AFSSLPinningModePublicKey];
    AFURLSessionManager *manager = [[AFURLSessionManager alloc] initWithSessionConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration]];
    manager.securityPolicy = policy;
    manager.responseSerializer = [AFHTTPResponseSerializer serializer];
    
    if (obj[@"method"]) {
        [request setHTTPMethod:obj[@"method"]];
    }
    if (obj[@"timeoutInterval"]) {
        [request setTimeoutInterval:[obj[@"timeoutInterval"] doubleValue] / 1000];
    }
    
    if(obj[@"headers"]) {
        [self setHeaders:obj request:request];
    }
    
    if (obj) {
        if ([obj objectForKey:@"body"]) {
            NSString *body = obj[@"body"];
            NSData *bodyData = [body dataUsingEncoding:NSUTF8StringEncoding];
            NSDictionary *bodyDic = [NSJSONSerialization JSONObjectWithData:bodyData options:NSJSONReadingMutableLeaves error:nil];
            
            // this is a multipart form data request
            if([bodyDic objectForKey:@"formData"]){
                // post multipart
                NSDictionary * formData = bodyDic[@"formData"];
                [self performMultipartRequest:manager obj:obj url:url request:request callback:callback formData:formData];
            }
            // Update normal request.
            else {
                NSData *jsondata = [NSJSONSerialization dataWithJSONObject:bodyDic
                                                                   options:NSJSONWritingPrettyPrinted
                                                                     error:nil];
                [request setValue:@"application/json" forHTTPHeaderField:@"Content-type"];
                [request setValue:[NSString stringWithFormat:@"%lu", (unsigned long)[jsondata length]] forHTTPHeaderField:@"Content-Length"];
                [request setHTTPBody:jsondata];
                
                [self performRequest:manager obj:obj request:request callback:callback ];
            }
        }
        else {
            [self performRequest:manager obj:obj request:request callback:callback ];
        }
    }
}

+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

@end
