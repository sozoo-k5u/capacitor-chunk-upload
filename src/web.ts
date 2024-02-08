import { WebPlugin } from '@capacitor/core';

import type { ChunkUploadPlugin, UploadOptions } from './definitions';

export class ChunkUploadWeb extends WebPlugin implements ChunkUploadPlugin {
  public static readonly ERROR_FILE_MISSING = 'blob must be provided.';

  public async uploadFile(options: UploadOptions): Promise<void> {
    if (!options.blob) {
      throw new Error(ChunkUploadWeb.ERROR_FILE_MISSING);
    }
    //upload here
    this.notifyListeners('uploadStarted', {});
    //define block Ids and chunk size
    const blockIdPrefix = 'block-';
    const blockIds: string[] = [];
    const chunkSize = 1024 * 1024 * 10; // 10 Megabytes
    const totalSize = options.blob.size || 0;
    const chunks: { blockId: string; blob: Blob }[] = [];
    //slice to chunks
    let start = 0;
    let end = Math.min(chunkSize, totalSize);
    while (start < totalSize) {
      const blob = options.blob.slice(start, end);
      const blockId = btoa(blockIdPrefix + this.pad(blockIds.length, 6));
      blockIds.push(blockId);
      chunks.push({ blockId, blob });
      start = end;
      end = Math.min(start + chunkSize, totalSize);
    }
    for (const chunk of chunks) {
      const { blockId, blob } = chunk;
      await this.uploadFileChunk(options.url, blockId, blob, options.headers);
      //calculate how many chunk has been processed then notify
      const processedChunks = chunks.indexOf(chunk) + 1;
      const percentage = (processedChunks / chunks.length) * 100;
      this.notifyListeners('uploadProgressChanged', {
        progress: { percentage },
      });
    }
    await this.commitFileChunks(options.url, blockIds, options.headers);
  }

  /**
   *
   * @param url
   * @param blockId
   * @param chunk
   */
  private async uploadFileChunk(
    url: string,
    blockId: string,
    chunk: Blob,
    headers?: { [key: string]: string },
  ): Promise<Response> {
    const uri = `${url}&comp=block&blockid=${blockId}`;
    const requestData = new Uint8Array(await chunk.arrayBuffer());
    const baseHeaders = {
      'Content-Type': 'application/json',
      'Content-Length': requestData.length.toString(),
      //'x-ms-blob-type': 'BlockBlob',
    };
    //merge baseHeaders with headers
    const mergedHeaders = { ...baseHeaders, ...headers };
    return fetch(uri, {
      method: 'PUT',
      body: chunk,
      headers: mergedHeaders,
    });
  }
  /**
   *
   * @param url
   * @param blockId
   * @param chunk
   */
  private async commitFileChunks(
    url: string,
    blockIds: string[],
    headers?: { [key: string]: string },
  ): Promise<Response> {
    const uri = `${url}&comp=blocklist`;
    const requestBody = JSON.stringify(blockIds);
    const baseHeaders = {
      'Content-Type': 'application/json',
      'Content-Length': requestBody.length.toString(),
    };
    //merge baseHeaders with headers
    const mergedHeaders = { ...baseHeaders, ...headers };
    return fetch(uri, {
      method: 'PUT',
      headers: mergedHeaders,
      body: requestBody,
    });
  }
  /**
   *
   * @param number
   * @param length
   * @returns
   */
  private pad(number: number, length: number): string {
    let str = '' + number;
    while (str.length < length) {
      str = '0' + str;
    }
    return str;
  }
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
