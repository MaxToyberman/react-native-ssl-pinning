
import { NativeModules, Platform } from 'react-native';

const { RNSslPinning } = NativeModules;
var Q = require('q');

module.exports = {
    getCookies : RNSslPinning.getCookies,
    removeCookieByName: RNSslPinning.removeCookieByName,
    fetch: function (url, obj, callback) {
        var deferred = Q.defer();
        RNSslPinning.fetch(url, obj, (err, res) => {
            if (err) {
                deferred.reject(err);
            } else {
                res.json = function() {
                    return Q.fcall(function () {
                        return JSON.parse(res.bodyString);
                    });
                };
                res.text = function() {
                    return Q.fcall(function () {
                        return res.bodyString;
                    });
                };
                res.url = url;

                deferred.resolve(res);
            }

            deferred.promise.nodeify(callback);
        });
        return deferred.promise;
    }
}
