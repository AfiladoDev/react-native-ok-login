import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-ok-login' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const OkLogin = NativeModules.OkLogin
  ? NativeModules.OkLogin
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export function initialize(appId: string, appKey: string): Promise<number> {
  return OkLogin.initialize(appId, appKey);
}

export function login(scope: string[]): Promise<number> {
  return OkLogin.login(scope);
}

export function request(method: string, params: any): Promise<number> {
  return OkLogin.request(method, params);
}

export function logout(): Promise<number> {
  return OkLogin.logout();
}
