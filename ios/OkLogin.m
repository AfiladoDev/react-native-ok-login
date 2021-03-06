#import "OkLogin.h"
#import "OKSDK.h"
#import <React/RCTUtils.h>

#ifdef DEBUG
#define DMLog(...) NSLog(@"[OKLogin] %s %@", __PRETTY_FUNCTION__, [NSString stringWithFormat:__VA_ARGS__])
#else
#define DMLog(...) do { } while (0)
#endif

NSString *const E_LOGIN_ERROR = @"E_LOGIN_ERROR";
NSString *const E_GET_USER_FAILED = @"E_GET_USER_FAILED";
NSString *const E_REQUEST_FAILED = @"E_REQUEST_FAILED";

@implementation OkLogin {
  RCTPromiseResolveBlock loginResolver;
  RCTPromiseRejectBlock loginRejector;
  RCTPromiseResolveBlock requestResolver;
  RCTPromiseRejectBlock requestRejector;
}

RCT_EXPORT_MODULE()

- (dispatch_queue_t)methodQueue {
  return dispatch_get_main_queue();
}

RCT_EXPORT_METHOD(initialize: (NSString *) appId withKey: (NSString *) appKey ) {
  DMLog(@"Initialize app id %@", appId);
  UIViewController *root = [[[[UIApplication sharedApplication] delegate] window] rootViewController];
  OKSDKInitSettings *settings = [OKSDKInitSettings new];
  settings.appId = appId;
  settings.appKey = appKey;
  settings.controllerHandler = ^{
    return root;
  };
  [OKSDK initWithSettings: settings];
}

RCT_EXPORT_METHOD(request: (NSString *) method arguments:(NSDictionary *) params  resolver: (RCTPromiseResolveBlock) resolve rejecter: (RCTPromiseRejectBlock) reject) {
    self->requestResolver = resolve;
    self->requestRejector = reject;
    [OKSDK invokeMethod:method arguments:params success:^(NSDictionary* data) {
      DMLog(@"Successfully request");
      self->requestResolver([self getResponse:data]);
    } error:^(NSError *error) {
      DMLog(@"Error in request: %@", [error localizedDescription]);
      self->requestRejector(RCTErrorUnspecified, E_REQUEST_FAILED, RCTErrorWithMessage([error localizedDescription]));
    }];
}

RCT_EXPORT_METHOD(login: (NSArray *) scope resolver: (RCTPromiseResolveBlock) resolve rejecter: (RCTPromiseRejectBlock) reject) {
  DMLog(@"Login with scope %@", scope);
  self->loginResolver = resolve;
  self->loginRejector = reject;

  [OKSDK authorizeWithPermissions:scope success:^(id data) {
    [OKSDK invokeMethod:@"users.getCurrentUser" arguments:@{} success:^(NSDictionary* data) {
      DMLog(@"Successfully obtained current user");
      self->loginResolver([self getResponse:data]);
    } error:^(NSError *error) {
      DMLog(@"Error in users.getCurrentUser: %@", [error localizedDescription]);
      self->loginRejector(RCTErrorUnspecified, E_GET_USER_FAILED, RCTErrorWithMessage([error localizedDescription]));
    }];
  } error:^(NSError *error) {
    DMLog(@"Error during auth: %@", [error localizedDescription]);
    self->loginRejector(RCTErrorUnspecified, E_LOGIN_ERROR, RCTErrorWithMessage([error localizedDescription]));
  }];
};

RCT_REMAP_METHOD(logout, resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
  DMLog(@"Logout");
  [OKSDK clearAuth];
  resolve(nil);
};

- (NSDictionary *)getResponse:(NSDictionary *)user {
  return @{
    @"access_token" : [OKSDK currentAccessToken],
    @"session_secret_key" : [OKSDK currentAccessTokenSecretKey],
    @"user": user
  };
}

@end
