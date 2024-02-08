import { PluginListenerHandle } from '@capacitor/core';

export interface ChunkUploadPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  /**
   * Upload a file via chunk
   */
  uploadFile(options: UploadOptions): Promise<void>;
  /**
   * Listens for event: upload started
   */
  addListener(
    eventName: 'uploadStarted',
    listenerFunc: () => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
  /**
   * Listens for event: upload progress changed
   */
  addListener(
    eventName: 'uploadProgressChanged',
    listenerFunc: (progress: { percentage: number }) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Removes all listeners
   */
  removeAllListeners(): Promise<void>;
}
/**
 * Upload option
 */
export interface UploadOptions {
  /**
   * The URL to upload to
   */
  url: string;
  /**
   * The file to upload.
   * Only available on Web.
   */
  blob?: Blob;
  /**
   * The path of the file to upload.
   * Only available on Android and iOS.
   */
  path?: string;
  /**
   * Addition headers to send with the request
   */
  headers?: { [key: string]: string };
}

/**
 * Upload result
 */
export interface UploadResult {
  /**
   * Indicate success or not
   */
  success: boolean;
  /**
   * HTTP status error if failed
   */
  errorCode?: number;
  /**
   * Error message if failed from HTTP body
   */
  errorDescription?: string;
}
