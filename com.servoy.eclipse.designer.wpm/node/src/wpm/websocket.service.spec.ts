import { describe, it, expect } from 'vitest';

import { WebsocketService } from './websocket.service';

describe('WebsocketService', () => {
  it('should be instantiable', () => {
    expect(WebsocketService).toBeDefined();
  });
});
