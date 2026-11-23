import { NEVER, Subject } from 'rxjs';

import { WebsocketService } from './websocket.service';
import { WpmService } from './wpm.service';

describe('WpmService', () => {
  let service: WpmService;

  beforeEach(() => {
    const wsService = {
      connect: () => ({ open: NEVER, messageObservable: NEVER, messageSender: new Subject() })
    } as unknown as WebsocketService;
    service = new WpmService(wsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('versionCompare', () => {
    it('compares plain versions numerically', () => {
      expect(service.versionCompare('2026.9.1', '2026.9.1')).toBe(0);
      expect(service.versionCompare('2026.9.2', '2026.9.1')).toBeGreaterThan(0);
      expect(service.versionCompare('2026.9.1', '2026.12.0')).toBeLessThan(0);
      expect(service.versionCompare('2026.9', '2026.9.0')).toBe(0);
    });

    it('ranks a -pre version below the same final version', () => {
      expect(service.versionCompare('2026.9.1-pre', '2026.9.1')).toBeLessThan(0);
      expect(service.versionCompare('2026.9.1', '2026.9.1-pre')).toBeGreaterThan(0);
    });

    it('ranks a -pre version above the previous release and below the next one', () => {
      expect(service.versionCompare('2026.9.1-pre', '2026.9.0')).toBeGreaterThan(0);
      expect(service.versionCompare('2026.9.1-pre', '2026.9.2')).toBeLessThan(0);
      expect(service.versionCompare('2026.9.2-pre', '2026.9.1')).toBeGreaterThan(0);
    });

    it('compares qualifiers when both versions have one', () => {
      expect(service.versionCompare('1.0.0-alpha', '1.0.0-beta')).toBeLessThan(0);
      expect(service.versionCompare('1.0.0-pre', '1.0.0-pre')).toBe(0);
    });
  });
});
