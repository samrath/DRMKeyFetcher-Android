# DRMKeyFetcher-Android
DRMKeyFetcher can be used to fetch the license keys for both DASH and HLS media files in android using Exoplayer library.

DRMKeyFetcher is the main java class to use in-order to fetch the keys for both DASH and HLS media files.

Required Parameters in constructor:  

Context: context on which your code is running. It can be UI or Non-UI or application context.
UUID : A class that represents an immutable universally unique identifier (UUID). A UUID represents a 128-bit value. For more info follow this file : http://www.ietf.org/rfc/rfc4122.txt
License Server URL:  Its you proxy server url which handle all the license related logic.
File url:  URL of the file for which you want to download the license keys.
DRMDefaultDrmSessionEventListener: This is the listener available in the library which provide the keys in case of success and exception in case of any error.

Extra Parameter Functions: 

setOptionalKeyRequestParameters:  This is a public function to set the optional parameters to send to proxy server and it will be in POST call body itself.

Function to make request for keys: 

startFetchingKeys:  This will make your keys request to server and provide the keys in DRMDefaultDrmSessionEventListener


