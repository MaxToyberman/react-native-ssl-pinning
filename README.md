
# react-native-ssl-pinning

React-Native Ssl pinning using OkHttp 3 in Android, and AFNetworking on iOS. 
## Getting started

`$ npm install react-native-ssl-pinning --save`

### Mostly automatic installation

`$ react-native link react-native-ssl-pinning`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-ssl-pinning` and add `RNSslPinning.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNSslPinning.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.toyberman.RNSslPinningPackage;` to the imports at the top of the file
  - Add `new RNSslPinningPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-ssl-pinning'
  	project(':react-native-ssl-pinning').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-ssl-pinning/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-ssl-pinning')
  	```


## Usage

#### Create the certificates:
1. openssl s_client -showcerts -google.com:443 (replace google with your domain)

2. Copy the certificate (Usally the first one in the chain), and paste it using nano or other editor like so , nano mycert.pem
3. convert it to .cer with this command
openssl x509 -in mycert.pem -outform der -out mycert.cer 

#### iOS
 - drag mycert.cer to Xcode project, mark your target and 'Copy items if needed'

#### Android
 -  Place your .cer files under src/main/assets/.
```javascript
import {fetch} from 'react-native-ssl-pinning';

fetch(url, {
	method: "POST" ,
	timeoutInterval: communication_timeout,
	body: body,
	headers: {
		Accept: "application/json; charset=utf-8", "Access-Control-Allow-Origin": "*", "e_platform": "mobile",
	}
}).then(response => {
	console.log(`response received ${response}`)
})
.catch(err => {
	console.log(`error: ${err}`)
})
```
  