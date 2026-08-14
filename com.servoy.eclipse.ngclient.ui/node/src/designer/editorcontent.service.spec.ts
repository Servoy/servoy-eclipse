import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { EditorContentService } from './editorcontent.service';
import { FormService } from '../ngclient/form.service';
import { ConverterService } from '../sablo/converter.service';
import { TypesRegistry } from '../sablo/types_registry';
import { StructureCache, FormCache } from '../ngclient/types';
import { IDesignFormComponent } from './servoydesigner.component';
import { LoggerFactory } from '@servoy/public';

describe('EditorContentService', () => {
  let service: EditorContentService;
  let formServiceMock: any;
  let converterServiceMock: any;
  let typesRegistryMock: any;
  let loggerFactoryMock: any;
  let designFormCallbackMock: any;

  beforeEach(() => {
    formServiceMock = { getFormCacheByName: vi.fn() } as any;
    converterServiceMock = { convertFromServerToClient: vi.fn() } as any;
    typesRegistryMock = { getComponentSpecification: vi.fn() } as any;
    loggerFactoryMock = { getLogger: vi.fn() } as any;
    loggerFactoryMock.getLogger.mockReturnValue({ error: vi.fn(), buildMessage: vi.fn() } as any);

    designFormCallbackMock = { getFormName: vi.fn(), refresh: vi.fn(), renderGhosts: vi.fn(), updateForm: vi.fn(), redrawDecorators: vi.fn(), contentRefresh: vi.fn() } as any;
    designFormCallbackMock.getFormName.mockReturnValue('testForm');

    service = TestBed.configureTestingModule({
      providers: [
        EditorContentService,
        { provide: FormService, useValue: formServiceMock },
        { provide: ConverterService, useValue: converterServiceMock },
        { provide: TypesRegistry, useValue: typesRegistryMock },
        { provide: LoggerFactory, useValue: loggerFactoryMock },
      ],
    }).inject(EditorContentService);
    service.setDesignFormComponent(designFormCallbackMock);
  });

  describe('updateFormData - null parent guard in reorderLayoutContainers (SVY-21255)', () => {
    it('should not crash when parent container is null for an existing layout container', () => {
      const existingContainer = new StructureCache('div', ['col-md-6'], { 'svy-id': 'container-1', 'svy-priority': '1' }, [], 'container-1', false, {});
      existingContainer['parent'] = new StructureCache('div', ['row'], { 'svy-id': 'old-parent' }, [existingContainer], 'old-parent', false, {});

      const formCacheMock = {
        getLayoutContainer: vi.fn(),
        getFormComponent: vi.fn(),
        addLayoutContainer: vi.fn(),
        getComponent: vi.fn(),
        removeComponent: vi.fn(),
        removeFormComponent: vi.fn(),
        getFormCacheByName: vi.fn(),
        getPart: vi.fn(),
      } as any;
      formCacheMock.absolute = false;
      formCacheMock.mainStructure = new StructureCache(null as any, null as any);
      formCacheMock.layoutContainersCache = new Map();
      formCacheMock.partComponentsCache = [];

      formCacheMock.getLayoutContainer.mockImplementation((id: string) => {
        if (id === 'container-1') return existingContainer;
        return null;
      });
      formCacheMock.getFormComponent.mockReturnValue(null);
      formCacheMock.getComponent.mockReturnValue(null);
      formCacheMock.getPart.mockReturnValue(null);
      formServiceMock.getFormCacheByName.mockReturnValue(formCacheMock);

      const updates = JSON.stringify({
        ng2containers: [
          {
            tagname: 'div',
            styleclass: ['col-md-8'],
            attributes: { 'svy-id': 'container-1', 'svy-priority': '1' },
            position: {},
            cssPositionContainer: false,
          },
        ],
        childParentMap: {
          'container-1': { uuid: 'non-existent-parent-uuid' },
        },
      });

      expect(() => service.updateFormData(updates)).not.toThrow();
      expect(existingContainer.classes).toEqual(['col-md-8']);
    });

    it('should not push null to reorderLayoutContainers when newParent is null', () => {
      const existingContainer = new StructureCache('div', ['col-md-6'], { 'svy-id': 'container-1', 'svy-priority': '1' }, [], 'container-1', false, {});
      const oldParent = new StructureCache('div', ['row'], { 'svy-id': 'old-parent', 'svy-priority': '0' }, [existingContainer], 'old-parent', false, {});
      existingContainer['parent'] = oldParent;

      const formCacheMock = {
        getLayoutContainer: vi.fn(),
        getFormComponent: vi.fn(),
        addLayoutContainer: vi.fn(),
        getComponent: vi.fn(),
        removeComponent: vi.fn(),
        removeFormComponent: vi.fn(),
        getPart: vi.fn(),
      } as any;
      formCacheMock.absolute = false;
      formCacheMock.mainStructure = new StructureCache(null as any, null as any);
      formCacheMock.layoutContainersCache = new Map();
      formCacheMock.partComponentsCache = [];

      formCacheMock.getLayoutContainer.mockImplementation((id: string) => {
        if (id === 'container-1') return existingContainer;
        return null;
      });
      formCacheMock.getFormComponent.mockReturnValue(null);
      formCacheMock.getComponent.mockReturnValue(null);
      formCacheMock.getPart.mockReturnValue(null);
      formServiceMock.getFormCacheByName.mockReturnValue(formCacheMock);

      const updates = JSON.stringify({
        ng2containers: [
          {
            tagname: 'div',
            styleclass: ['col-md-8'],
            attributes: { 'svy-id': 'container-1', 'svy-priority': '1' },
            position: {},
            cssPositionContainer: false,
          },
        ],
        childParentMap: {
          'container-1': { uuid: 'missing-parent-uuid' },
        },
      });

      expect(() => service.updateFormData(updates)).not.toThrow();
      expect(existingContainer.classes).toEqual(['col-md-8']);
    });
  });

  describe('updateFormData - sortChildren null container guard (SVY-21255)', () => {
    it('should not throw TypeError when reorderLayoutContainers contains null entries', () => {
      const formCacheMock = {
        getLayoutContainer: vi.fn(),
        getFormComponent: vi.fn(),
        addLayoutContainer: vi.fn(),
        getComponent: vi.fn(),
        removeComponent: vi.fn(),
        removeFormComponent: vi.fn(),
        getPart: vi.fn(),
      } as any;
      formCacheMock.absolute = false;
      formCacheMock.mainStructure = new StructureCache(null as any, null as any);
      formCacheMock.layoutContainersCache = new Map();
      formCacheMock.partComponentsCache = [];
      formCacheMock.getLayoutContainer.mockReturnValue(null);
      formCacheMock.getFormComponent.mockReturnValue(null);
      formCacheMock.getComponent.mockReturnValue(null);
      formCacheMock.getPart.mockReturnValue(null);
      formServiceMock.getFormCacheByName.mockReturnValue(formCacheMock);

      const updates = JSON.stringify({});

      expect(() => service.updateFormData(updates)).not.toThrow();
    });
  });
});
