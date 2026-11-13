import { describe, it, expect, beforeEach } from 'vitest';
import { RecordRefType, RecordRefForServer } from './record_ref_converter';
import { RowValue, FoundsetValue } from './foundset_converter';

describe('RecordRefConverter', () => {
  let recordRefType: RecordRefType;

  beforeEach(() => {
    recordRefType = new RecordRefType();
  });

  describe('generateRecordRef', () => {
    it('should build a RecordRefForServer with the given rowId and foundsetId', () => {
      const ref = RecordRefType.generateRecordRef('row-1', 42);
      expect(ref).toBeInstanceOf(RecordRefForServer);
      expect(ref._svyRowId).toBe('row-1');
      expect(ref.foundsetId).toBe(42);
    });
  });

  describe('fromServerToClient', () => {
    it('should return the server value unchanged', () => {
      const serverValue = { recordhash: 'h', foundsetId: 1, _svyRowId: 'r' };
      expect(recordRefType.fromServerToClient(serverValue, undefined, undefined as any)).toBe(serverValue);
      expect(recordRefType.fromServerToClient(null, undefined, undefined as any)).toBeNull();
    });
  });

  describe('fromClientToServer', () => {
    it('should return null when no client data is given', () => {
      expect(recordRefType.fromClientToServer(null as any, undefined, undefined as any)).toBeNull();
    });

    it('should pass through a ServerSentRecordRef (has recordhash)', () => {
      const serverSent: any = { recordhash: 'h', foundsetId: 1, _svyRowId: 'r' };
      const result = recordRefType.fromClientToServer(serverSent, undefined, undefined as any);
      expect(result).toEqual([serverSent, serverSent]);
    });

    it('should pass through a RecordRefForServer (has foundsetId and _svyRowId)', () => {
      const ref = RecordRefType.generateRecordRef('row-2', 7);
      const result = recordRefType.fromClientToServer(ref, undefined, undefined as any);
      expect(result).toEqual([ref, ref]);
    });

    it('should generate a record ref from a RowValue', () => {
      const foundset = { foundsetId: 99, getId: () => 99 } as unknown as FoundsetValue;
      const row = new RowValue(foundset);
      row._svyRowId = 'the-row-id';

      const result = recordRefType.fromClientToServer(row, undefined, undefined as any);
      const generated = result[0] as RecordRefForServer;
      expect(generated._svyRowId).toBe('the-row-id');
      expect(generated.foundsetId).toBe(99);
      expect(result[1]).toBe(row);
    });
  });
});
