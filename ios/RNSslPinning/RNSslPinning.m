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


-(void)performRequest:(AFURLSessionManager*)manager request:(NSMutableURLRequest*) request callback:(RCTResponseSenderBlock) callback  {
    
    [[manager dataTaskWithRequest:request completionHandler:^(NSURLResponse * _Nonnull response, id  _Nullable responseObject, NSError * _Nullable error) {
        
        NSHTTPURLResponse *httpResp = (NSHTTPURLResponse*) response;
        NSString *bodyString = [[NSString alloc] initWithData: responseObject encoding:NSUTF8StringEncoding];
        NSInteger statusCode = httpResp.statusCode;

        if (!error) {
            callback(@[[NSNull null], @{
                @"status": @(statusCode),
                @"headers": httpResp.allHeaderFields,
                @"bodyString": bodyString
            }]);
        } else if (error && error.userInfo[AFNetworkingOperationFailingURLResponseDataErrorKey]) {
            dispatch_async(dispatch_get_main_queue(), ^{
                callback(@[@{
                    @"status": @(statusCode),
                    @"headers": httpResp.allHeaderFields,
                    @"bodyString": bodyString
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
    
    
    request = [[AFHTTPRequestSerializer serializer] multipartFormRequestWithMethod:@"POST" URLString:url parameters:nil constructingBodyWithBlock:^(id<AFMultipartFormData> _formData) {
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
                                        NSString * data = [value objectForKey:@"data"];
                                        NSData *nsdataFromBase64String = [[NSData alloc] initWithBase64EncodedString:data options:0];

                                        [_formData appendPartWithFileData:nsdataFromBase64String name:@"file" fileName:fileName mimeType:mimeType];
                                        //[_formData appendPartWithFileURL:[NSURL fileURLWithPath:path] name:name fileName:fileName mimeType:mimeType error:&error1];
                                        //NSLog(@"%@",error1);

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
    

    
    NSURLSessionUploadTask *uploadTask;
    uploadTask = [manager
                  uploadTaskWithStreamedRequest:request
                  progress:^(NSProgress * _Nonnull uploadProgress) {
                      
                  }
                  completionHandler:^(NSURLResponse * _Nonnull response, id  _Nullable responseObject, NSError * _Nullable error) {
                      if (error) {
                          NSLog(@"Error: %@", error);
                      } else {
                          NSLog(@"%@ %@", response, responseObject);
                          
                          NSHTTPURLResponse *httpResp = (NSHTTPURLResponse*) response;

                          NSString *bodyString = [[NSString alloc] initWithData: responseObject encoding:NSUTF8StringEncoding];
                          NSInteger statusCode = httpResp.statusCode;
                          
                          NSDictionary *res = @{
                                                @"status": @(statusCode),
                                                @"headers": httpResp.allHeaderFields,
                                                @"bodyString": bodyString
                                                };
                          callback(@[[NSNull null], res]);
                          
                      }
                  }];
    
    [uploadTask resume];
}

RCT_EXPORT_METHOD(fetch:(NSString *)url obj:(NSDictionary *)obj callback:(RCTResponseSenderBlock)callback) {
    NSURL *u = [NSURL URLWithString:url];
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] initWithURL:u];
    
    // set policy (ssl pinning)
//    AFSecurityPolicy *policy = [AFSecurityPolicy policyWithPinningMode:AFSSLPinningModeNone];
//
//    policy.validatesDomainName = false;
//    policy.allowInvalidCertificates = true;
//    NSURLSessionConfiguration *configuration = [NSURLSessionConfiguration defaultSessionConfiguration];
//    configuration.requestCachePolicy = NSURLRequestReloadIgnoringLocalCacheData;
//
//    AFURLSessionManager *manager = [[AFURLSessionManager alloc] initWithSessionConfiguration:configuration];
//
//    manager.securityPolicy = policy;
//
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
            NSDictionary * body = obj[@"body"];
            
            // this is a multipart form data request
            if([body isKindOfClass:[NSDictionary class]] && [body objectForKey:@"formData"]){
                // post multipart
                NSDictionary * formData = body[@"formData"];
                [self performMultipartRequest:manager obj:obj url:url request:request callback:callback formData:formData];
            }
            else {
                
                // post a string
                NSData *data = [obj[@"body"] dataUsingEncoding:NSUTF8StringEncoding];
                [request setHTTPBody:data];
                [self performRequest:manager request:request callback:callback ];
                //TODO: if no body
            }
            
        }
        else {
            [self performRequest:manager request:request callback:callback ];
        }
    }
    else {
        
    }
    
}

+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

@end
