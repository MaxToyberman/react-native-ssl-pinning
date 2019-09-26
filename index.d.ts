export namespace ReactNativeSSLPinning {
    interface Cookies {
        [cookieName: string]: string;
    }

    interface Header {
        [headerName: string]: string;
    }

    interface Options {
        body?: string | object,
        credentials?: string,
        headers?: Header;
        method?: 'DELETE' | 'GET' | 'POST' | 'PUT',
        disableAllSecurity: boolean,
        sslPinning: {
            certs: string[]
        },
        timeoutInterval?: number,
    }

    interface Response {
        bodyString: string;
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
