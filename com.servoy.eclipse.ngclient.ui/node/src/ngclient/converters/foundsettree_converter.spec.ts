import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { ConverterService } from '../../sablo/converter.service';
import { SabloService } from '../../sablo/sablo.service';
import { LoggerFactory, SessionStorageService, WindowRefService } from '@servoy/public';
import { ServicesService } from '../../sablo/services.service';
import { SabloDeferHelper } from '../../sablo/defer.service';
import { LoadingIndicatorService } from '../../sablo/util/loading-indicator/loading-indicator.service';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { TypesRegistry, IPropertyContext } from '../../sablo/types_registry';
import { FoundsetTreeType, FoundsetTree } from './foundsettree_converter';

describe('FoundsetTreeConverter', () => {
  let converterService: ConverterService<FoundsetTree>;
  let typesRegistry: TypesRegistry;
  let sabloService: SabloService;
  let sabloDeferHelper: SabloDeferHelper;
  let foundsetTreeType: FoundsetTreeType;
  const propertyContext = undefined as unknown as IPropertyContext;

  let changeNotified: boolean;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ServoyTestingModule],
      providers: [ConverterService, LoggerFactory, WindowRefService, ServicesService, SessionStorageService, LoadingIndicatorService],
    });

    sabloService = TestBed.inject(SabloService);
    sabloService.connect({}, {}, '');
    sabloDeferHelper = TestBed.inject(SabloDeferHelper);
    converterService = TestBed.inject(ConverterService) as ConverterService<FoundsetTree>;
    typesRegistry = TestBed.inject(TypesRegistry);

    foundsetTreeType = new FoundsetTreeType(sabloDeferHelper);
    typesRegistry.registerGlobalType(FoundsetTreeType.TYPE_NAME, foundsetTreeType);
    changeNotified = false;
  });

  const convertToClient = (serverValue: any, current?: FoundsetTree): FoundsetTree =>
    converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetTreeType.TYPE_NAME), current as any, undefined as any, undefined as any, propertyContext);

  const convertToServer = (value: FoundsetTree): any =>
    converterService.convertFromClientToServer(value, typesRegistry.getAlreadyRegisteredType(FoundsetTreeType.TYPE_NAME), value, propertyContext)[0];

  const listen = (value: FoundsetTree) => {
    value.getInternalState().setChangeListener(() => {
      changeNotified = true;
    });
  };

  describe('fromServerToClient', () => {
    it('should create a new FoundsetTree instance from the initial server value', () => {
      const tree = convertToClient([{ id: 'root', children: [] }]);
      expect(tree).toBeInstanceOf(FoundsetTree);
      expect(tree.length).toBe(1);
      expect(tree[0]).toEqual({ id: 'root', children: [] });
    });

    it('should set newChildren when getChildren without handledID is sent', () => {
      const tree = convertToClient([{ id: 'root' }]);
      const kids = { parentID: 'root', children: ['a', 'b'] } as any;
      const updated = convertToClient({ getChildren: kids }, tree);
      expect(updated).toBe(tree);
      expect(tree.getAndResetNewChildren()).toBe(kids);
      // second call should be null after reset
      expect(tree.getAndResetNewChildren()).toBeNull();
    });

    it('should set newCheckedValues when they come from the server', () => {
      const tree = convertToClient([{ id: 'root' }]);
      const checked = { 'node-1': true } as any;
      const updated = convertToClient({ newCheckedValues: checked }, tree);
      expect(updated).toBe(tree);
      expect(tree.getAndResetUpdatedCheckboxValues()).toBe(checked);
      expect(tree.getAndResetUpdatedCheckboxValues()).toBeNull();
    });

    it('should reuse the deferred state when a new full value replaces an existing one', () => {
      const tree = convertToClient([{ id: 'root' }]);
      const replaced = convertToClient([{ id: 'root2' }], tree);
      expect(replaced).toBeInstanceOf(FoundsetTree);
      expect(replaced).not.toBe(tree);
      expect(replaced[0]).toEqual({ id: 'root2' });
    });

    it('should resolve the pending getChildren deferred when the server answers with handledID', () => {
      const tree = convertToClient([{ id: 'root' }]);
      listen(tree);
      tree.getChildren('root', 0);

      // the request should be exposed to the server-conversion
      const req = convertToServer(tree);
      expect(req.getChildren).toBe('root');
      expect(req.level).toBe(0);
      const reqId = req.id;

      const resolveSpy = vi.spyOn(sabloDeferHelper, 'resolveDeferedEvent');
      const childrenPayload = { 'root': ['c1', 'c2'] } as any;
      convertToClient({ getChildren: childrenPayload, handledID: { id: reqId, value: true } }, tree);

      expect(resolveSpy).toHaveBeenCalledWith(reqId, tree.getInternalState(), childrenPayload, true);
    });

    it('should pass a rejection message to the deferred when the server reports a failed handledID', () => {
      const tree = convertToClient([{ id: 'root' }]);
      listen(tree);
      tree.getChildren('root', 0);
      const reqId = convertToServer(tree).id;

      const resolveSpy = vi.spyOn(sabloDeferHelper, 'resolveDeferedEvent');
      convertToClient({ getChildren: { 'root': [] }, handledID: { id: reqId, value: false } }, tree);

      expect(resolveSpy).toHaveBeenCalledWith(reqId, tree.getInternalState(), 'No value for ' + reqId, false);
    });

    it('should null out the client value and cancel pending requests on a null server value', () => {
      const tree = convertToClient([{ id: 'root' }]);
      listen(tree);
      tree.getChildren('root', 0);

      const result = convertToClient(null, tree);
      expect(result).toBeNull();
    });
  });

  describe('fromClientToServer', () => {
    it('should send a getChildren request and clear it afterwards', () => {
      const tree = convertToClient([{ id: 'root' }]);
      listen(tree);
      tree.getChildren('root', 2);

      const first = convertToServer(tree);
      expect(first.getChildren).toBe('root');
      expect(first.level).toBe(2);

      // after being sent, there are no more changes
      expect(tree.getInternalState().hasChanges()).toBe(false);
      const second = convertToServer(tree);
      expect(second).toBeNull();
    });

    it('should send an updateSelection request', () => {
      const tree = convertToClient([{ id: 'root' }]);
      listen(tree);
      tree.updateSelection(['n1', 'n2']);
      expect(changeNotified).toBe(true);

      const req = convertToServer(tree);
      expect(req).toEqual({ updateSelection: ['n1', 'n2'] });
      expect(convertToServer(tree)).toBeNull();
    });

    it('should send an updateCheckboxValue request', () => {
      const tree = convertToClient([{ id: 'root' }]);
      listen(tree);
      tree.updateCheckboxValue('n1', true);
      expect(changeNotified).toBe(true);

      const req = convertToServer(tree);
      expect(req).toEqual({ updateCheckboxValue: 'n1', value: true });
      expect(convertToServer(tree)).toBeNull();
    });

    it('should send an updateExpandedNodes request', () => {
      const tree = convertToClient([{ id: 'root' }]);
      listen(tree);
      tree.updateExpandedNodes(['p1', 'p2']);
      expect(changeNotified).toBe(true);

      const req = convertToServer(tree);
      expect(req).toEqual({ updateExpandedNodes: ['p1', 'p2'] });
      expect(convertToServer(tree)).toBeNull();
    });

    it('should return null when there are no pending requests', () => {
      const tree = convertToClient([{ id: 'root' }]);
      expect(convertToServer(tree)).toBeNull();
    });

    it('should return null when client data is null', () => {
      expect(foundsetTreeType.fromClientToServer(null as any, null as any, propertyContext)).toBeNull();
    });
  });

  describe('uiDestroyed', () => {
    it('should cancel pending deferred requests', () => {
      const tree = convertToClient([{ id: 'root' }]);
      listen(tree);
      tree.getChildren('root', 0);
      expect(() => tree.uiDestroyed()).not.toThrow();
    });
  });
});
