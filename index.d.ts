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
        json:() => Promise<object>;
        text: () => Promise<string>;
    }
}

export declare function fetch(url: string, options: ReactNativeSSLPinning.Options): Promise<ReactNativeSSLPinning.Response>;
export declare function getCookies(name: string): Promise<ReactNativeSSLPinning.Cookies>;
export declare function removeCookieByName(name: string): Promise<void>;
