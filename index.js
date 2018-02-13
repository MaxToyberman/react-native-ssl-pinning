
import { NativeModules } from 'react-native';

const { RNSslPinning } = NativeModules;

module.exports = {
    fetch: RNSslPinning.fetch
}