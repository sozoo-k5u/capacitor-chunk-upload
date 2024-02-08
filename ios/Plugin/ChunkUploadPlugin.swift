import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(ChunkUploadPlugin)
public class ChunkUploadPlugin: CAPPlugin , CAPBridgedPlugin {
    public var identifier: String = "ChunkUpload"
    
    public var jsName: String = "ChunkUpload"
    
    public let pluginMethods: [CAPPluginMethod] = [
            CAPPluginMethod(name: "upload", returnType: CAPPluginReturnPromise),
        ]
    
    public let errorUnknown = "An unknown error occurred."
    public let errorUrlMissing = "Url must be provided."
    public let errorPathMissing = "File path must be provided."
    
    private let implementation = ChunkUpload()

    override public func load() {
    }

    @objc func echo(_ call: CAPPluginCall) {
        let value = call.getString("value") ?? ""
        call.resolve([
            "value": implementation.echo(value)
        ])
    }
    @objc func uploadFile(_ call: CAPPluginCall) {
        guard let url = call.getString("url") else {
            call.reject(errorUrlMissing)
            return
        }
        guard let path = call.getString("path") else {
            call.reject(errorPathMissing)
            return
        }
        let headers = call.getObject("headers") as? NSDictionary ?? [:];
        notifyListeners("uploadStarted",data: [:]);
        implementation.uploadFile(
            path: path,
            url: url,
            headers: headers,
            onProgress: { percentage in
                self.notifyListeners("uploadProgressChanged",data: ["percentage": percentage]);
            },
            onComplete: { errorMessage in
                if let errorMessage = errorMessage {
                    call.reject(errorMessage)
                    return
                }
                call.resolve([:]);
            }
        );
        
    }
}
