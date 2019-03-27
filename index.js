import { NativeModules, Platform } from 'react-native';

const { RNSslPinning } = NativeModules;
let Q = require('q');

module.exports = {
    getCookies : RNSslPinning.getCookies,
    removeCookieByName: RNSslPinning.removeCookieByName,
    fetch: function (url, obj, callback) {
        let deferred = Q.defer();
        RNSslPinning.fetch(url, obj, (err, res) => {
            if (err && typeof err != 'object') {
                deferred.reject(err);
            }

            let data = err || res;

            data.json = function() {
                return Q.fcall(function() {
                    return JSON.parse(data.bodyString);
                });
            };

            data.text = function() {
                return Q.fcall(function() {
                    return data.bodyString;
                });
            };

            data.url = url;

            if (err) {
                deferred.reject(data);
            } else {
                deferred.resolve(data);
            }

            deferred.promise.nodeify(callback);
        });

        return deferred.promise;
    }
}
