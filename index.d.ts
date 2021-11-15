export namespace ReactNativeSSLPinning {
    interface Cookies {
        [cookieName: string]: string;
    }

    interface Header {
        [headerName: string]: string;
    }

    interface Options {
        body?: string | object,
        responseType?: 'text' | 'base64',
        credentials?: string,
        headers?: Header;
        method?: 'DELETE' | 'GET' | 'POST' | 'PUT',
        pkPinning?: boolean,
        sslPinning: {
            certs: string[]
        },
        timeoutInterval?: number,
        disableAllSecurity?: boolean,
        caseSensitiveHeaders?: boolean = false
    }

    interface Response {
        bodyString?: string;
        data?: string;
        headers: Header;
        status: number;
        url: string;
        json: () => Promise<{ [key: string]: any}>;
        text: () => Promise<string>;
    }
}

export declare function fetch(url: string, options: ReactNativeSSLPinning.Options): Promise<ReactNativeSSLPinning.Response>;
export declare function getCookies(domain: string): Promise<ReactNativeSSLPinning.Cookies>;
export declare function removeCookieByName(cookieName: string): Promise<void>;
