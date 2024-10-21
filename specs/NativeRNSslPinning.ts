import type { TurboModule } from "react-native";
import { TurboModuleRegistry } from "react-native";
import { Int32 } from 'react-native/Libraries/Types/CodegenTypes';

import type {
    ReactNativeSSLPinning,
  } from '../index.d.ts';

  enum ResponseType {
    Text = 'text',
    Base64 = 'base64'
  }

  enum Method {
    DELETE = 'DELETE',
    GET = 'GET',
    POST = 'POST',
    PUT = 'PUT'
  }

  interface Options {
    body?: string,
    responseType?: ResponseType,
    credentials?: string,
    headers?: {
        [key: string]: string;
    };
    method?: Method,
    pkPinning?: boolean,
    sslPinning: {
        certs: string[]
    },
    timeoutInterval?: Int32,
    disableAllSecurity?: boolean,
    caseSensitiveHeaders?: boolean
}

interface FetchResponse {
    bodyString?: string;
    data?: string;
    headers: {
        [key: string]: string;
    };
    status: number;
    url: string;
}

export interface Spec extends TurboModule {
    fetch(
        url: string,
        options: Options,
        callback: (response: FetchResponse) => void
    ): void;
    
    getCookies(
        domain: string
    ): Promise<ReactNativeSSLPinning.Cookies>;

    removeCookieByName(
        cookieName: string
    ): Promise<void>;
}

export default TurboModuleRegistry.getEnforcing<Spec>("RNSslPinning"); 