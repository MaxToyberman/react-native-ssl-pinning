
# react-native-ssl-pinning

React-Native ssl pinning & public key pinning using OkHttp 3 in Android, and AFNetworking on iOS. 

## NOTES:

- for RN 0.60.0 or later use `react-native-ssl-pinning@latest`


## Getting started

`$ npm install react-native-ssl-pinning --save`


### Mostly automatic installation

> If you are using `React Native 0.60.+` [the link should happen automatically](https://github.com/react-native-community/cli/blob/master/docs/autolinking.md). in iOS run pod install

`$ react-native link react-native-ssl-pinning`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-ssl-pinning` and add `RNSslPinning.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNSslPinning.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

Add maven { url "https://jitpack.io" } to project level build.gradle like this: 
```
allprojects {
    repositories {
	maven { url "https://jitpack.io" }
    }
}
```
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

1. openssl s_client -showcerts -servername google.com -connect google.com:443 </dev/null

2. Copy the certificate (Usally the first one in the chain), and paste it using nano or other editor like so , nano mycert.pem
3. convert it to .cer with this command
openssl x509 -in mycert.pem -outform der -out mycert.cer 
```
For more ways to obtain the server certificate please refer:
https://stackoverflow.com/questions/7885785/using-openssl-to-get-the-certificate-from-a-server
```
#### iOS
 - drag mycert.cer to Xcode project, mark your target and 'Copy items if needed'
 - (skip this if you are using certificate pinning) no extra step needed for public key pinning,  AFNetworking will extract the public key from the certificate. 

#### Android
 -  Only if using certificate pinning : place your .cer files under src/main/assets/

 - For public key pinning the public key should be extracted by the following options
: (replace google with your domain)
	- ```openssl s_client -servername google.com -connect google.com:443 | openssl x509 -pubkey -noout | openssl rsa -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64```
	- Turn on pinning with a broken configuration and read the expected configuration when the connection fails.
		```javascript
		fetch("https://publicobject.com", {
			method: "GET" ,
			pkPinning: true,
			sslPinning: {
				certs: ["sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="] 
			}
		})
		```
		- Now look at your logcat ,   As expected, this fails with a certificate pinning exception: <pre>javax.net.ssl.SSLPeerUnverifiedException: Certificate pinning failure!
		Peer certificate chain:
		sha256/afwiKY3RxoMmLkuRW1l7QsPZTJPwDS2pdDROQjXw8ig=: CN=publicobject.com, OU=PositiveSSL
		sha256/klO23nT2ehFDXCfx3eHTDRESMz3asj1muO+4aIdjiuY=: CN=COMODO RSA Secure Server CA
		sha256/grX4Ta9HpZx6tSHkmCrvpApTQGo67CYDnvprLg5yRME=: CN=COMODO RSA Certification Authority
		sha256/lCppFqbkrlJ3EcVFAkeip0+44VaoJUymbnOaEUk7tEU=: CN=AddTrust External CA Root
		Pinned certificates for publicobject.com:
		sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
		at okhttp3.CertificatePinner.check(CertificatePinner.java)
		at okhttp3.Connection.upgradeToTls(Connection.java)
		at okhttp3.Connection.connect(Connection.java)
		at okhttp3.Connection.connectAndSetOwner(Connection.java)
		- Follow up by pasting the public key hashes from the exception into the certificate pinner's configuration
 
 ### Certificate Pinning

```javascript
import {fetch} from 'react-native-ssl-pinning';

fetch(url, {
	method: "POST" ,
	timeoutInterval: communication_timeout, // milliseconds
	body: body,
	// your certificates array (needed only in android) ios will pick it automatically
	sslPinning: {
		certs: ["cert1","cert2"] // your certificates name (without extension), for example cert1.cer, cert2.cer
	},
	headers: {
		Accept: "application/json; charset=utf-8", "Access-Control-Allow-Origin": "*", "e_platform": "mobile",
	}
})
.then(response => {
	console.log(`response received ${response}`)
})
.catch(err => {
	console.log(`error: ${err}`)
})
```
 ### Public Key Pinning
```javascript
import {fetch} from 'react-native-ssl-pinning';

fetch("https://publicobject.com", {
      method: "GET" ,
      timeoutInterval: 10000, // milliseconds
      // your certificates array (needed only in android) ios will pick it automatically
      pkPinning: true,
      sslPinning: {
        certs: ["sha256//r8udi/Mxd6pLO7y7hZyUMWq8YnFnIWXCqeHsTDRqy8=",
        "sha256/YLh1dUR9y6Kja30RrAn7JKnbQG/uEtLMkBgFF2Fuihg=",
        "sha256/Vjs8r4z+80wjNcr1YKepWQboSIRi63WsWXhIMN+eWys="
      ]
      },
      headers: {
        Accept: "application/json; charset=utf-8", "Access-Control-Allow-Origin": "*", "e_platform": "mobile",
      }
	})
	
```
### Disable Pinning
```javascript

 fetch("https://publicobject.com", {
      method: "GET" ,
      timeoutInterval: 10000, // milliseconds
      disableAllSecurity: true,
      headers: {
        Accept: "application/json; charset=utf-8", "Access-Control-Allow-Origin": "*", "e_platform": "mobile",
      }
	})
		
```
### Case Sensitive Headers
```javascript

 fetch("https://publicobject.com", {
      method: "GET" ,
      timeoutInterval: 10000, // milliseconds
      caseSensitiveHeaders: true, //in case you want headers to be case Sensitive
      headers: {
		Accept: "application/json; charset=utf-8", "Access-Control-Allow-Origin": "*", "e_platform": "mobile",
		SOAPAction: "testAction",
      }
	})


```
 ### Cookies Handling

```javascript
import {removeCookieByName} from 'react-native-ssl-pinning';


removeCookieByName('cookieName')
.then(res =>{
	console.log('removeCookieByName');
})

getCookies('domain')
.then(cookies => {
// do what you need with your cookies
})

```
  ## Multipart request (FormData)

```javascript
let formData = new FormData()

#You could add a key/value pair to this using #FormData.append:

formData.append('username', 'Chris');

# Adding a file to the request
formData.append('file', {
		name: encodeURIComponent(response.fileName),
		fileName: encodeURIComponent(response.fileName),
		type: this._extractFileType(response.fileName),
		uri: response.uri
})

fetch(url, {
	method: "POST" ,
	timeoutInterval: communication_timeout, // milliseconds
	body: {
		formData: request,
	},
	sslPinning: {
		certs: ["cert1","cert2"]
	},
	headers: {
		accept: 'application/json, text/plain, /',
	}
})

don't add 'content-type': 'multipart/form-data; charset=UTF-8',
Setting the Content-Type header manually means it's missing the boundary parameter. Remove that header and allow fetch to generate the full content type.
```

## License
This project is licensed under the terms of the MIT license.
