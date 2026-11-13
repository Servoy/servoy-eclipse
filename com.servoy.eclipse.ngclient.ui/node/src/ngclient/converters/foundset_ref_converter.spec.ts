import { describe, it, expect, beforeEach } from 'vitest';
import { FoundsetRefType } from './foundset_ref_converter';
import { FoundsetValue } from './foundset_converter';

describe('FoundsetRefConverter', () => {
  let foundsetRefType: FoundsetRefType;

  beforeEach(() => {
    foundsetRefType = new FoundsetRefType();
  });

  describe('fromServerToClient', () => {
    it('should return the server value unchanged (the foundsetId)', () => {
      expect(foundsetRefType.fromServerToClient(123, undefined as any, undefined as any)).toBe(123);
      expect(foundsetRefType.fromServerToClient(null, undefined as any, undefined as any)).toBeNull();
    });
  });

  describe('fromClientToServer', () => {
    it('should pass a number straight through as the foundsetId', () => {
      const result = foundsetRefType.fromClientToServer(456, undefined, undefined as any);
      expect(result).toEqual([456, 456]);
    });

    it('should extract foundsetId from a FoundsetValue', () => {
      const fs = { foundsetId: 789 } as FoundsetValue;
      const result = foundsetRefType.fromClientToServer(fs, undefined, undefined as any);
      expect(result).toEqual([789, fs]);
    });
  });
});
