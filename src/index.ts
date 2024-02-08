import { registerPlugin } from '@capacitor/core';

import type { ChunkUploadPlugin } from './definitions';

const ChunkUpload = registerPlugin<ChunkUploadPlugin>('ChunkUpload', {
  web: () => import('./web').then(m => new m.ChunkUploadWeb()),
});

export * from './definitions';
export { ChunkUpload };
