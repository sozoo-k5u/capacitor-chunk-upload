#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

// Define the plugin using the CAP_PLUGIN Macro, and
// each method the plugin supports using the CAP_PLUGIN_METHOD macro.
CAP_PLUGIN(ChunkUploadPlugin, "ChunkUpload",
          CAP_PLUGIN_METHOD(echo, CAPPluginReturnPromise);
          CAP_PLUGIN_METHOD(removeAllListeners, CAPPluginReturnPromise);
          CAP_PLUGIN_METHOD(addListener, CAPPluginReturnPromise);
          CAP_PLUGIN_METHOD(uploadFile, CAPPluginReturnPromise);
)
