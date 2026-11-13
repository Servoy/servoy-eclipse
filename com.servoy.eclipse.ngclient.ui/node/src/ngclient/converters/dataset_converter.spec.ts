import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { ConverterService } from '../../sablo/converter.service';
import { LoggerFactory } from '@servoy/public';
import { TypesRegistry, IPropertyContext, PushToServerEnum } from '../../sablo/types_registry';
import { DateType } from '../../sablo/converters/date_converter';
import { DatasetType } from './dataset_converter';

describe('DatasetConverter', () => {
  let converterService: ConverterService<any>;
  let typesRegistry: TypesRegistry;
  let datasetType: DatasetType;
  let propertyContext: IPropertyContext;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ConverterService, LoggerFactory],
    });

    converterService = TestBed.inject(ConverterService);
    typesRegistry = TestBed.inject(TypesRegistry);
    typesRegistry.registerGlobalType(DateType.TYPE_NAME_SVY, new DateType(), true);

    datasetType = new DatasetType(typesRegistry, converterService);

    propertyContext = {
      getProperty: () => undefined,
      getPushToServerCalculatedValue: () => PushToServerEnum.REJECT,
      isInsideModel: true,
    } as IPropertyContext;
  });

  describe('fromServerToClient', () => {
    it('should return the value unchanged when server value is null/undefined', () => {
      expect(datasetType.fromServerToClient(null, undefined, propertyContext)).toBeNull();
      expect(datasetType.fromServerToClient(undefined, undefined, propertyContext)).toBeUndefined();
    });

    it('should extract the value array and convert simple cells unchanged', () => {
      const serverValue = {
        v: [
          [1, 'a'],
          [2, 'b'],
        ],
      };
      const result = datasetType.fromServerToClient(serverValue, undefined, propertyContext);
      expect(result).toEqual([
        [1, 'a'],
        [2, 'b'],
      ]);
    });

    it('should skip the first row when it contains column names', () => {
      const serverValue = {
        i: true,
        v: [
          ['col1', 'col2'],
          [10, 20],
        ],
      };
      const result = datasetType.fromServerToClient(serverValue, undefined, propertyContext);
      expect(result[0]).toEqual(['col1', 'col2']);
      expect(result[1]).toEqual([10, 20]);
    });

    it('should apply per-column client-side types from the server type descriptor', () => {
      const dateMillis = Date.UTC(2020, 0, 2, 3, 4, 5);
      const serverValue = {
        t: { 1: 'svy_date' },
        v: [['keep', dateMillis]],
      };
      const result = datasetType.fromServerToClient(serverValue, undefined, propertyContext);
      expect(result[0][0]).toBe('keep');
      expect(result[0][1] instanceof Date).toBe(true);
      expect((result[0][1] as Date).getTime()).toBe(dateMillis);
    });
  });

  describe('fromClientToServer', () => {
    it('should always return null (not supported)', () => {
      expect(datasetType.fromClientToServer({ v: [[1]] }, undefined, propertyContext)).toBeNull();
    });
  });
});
