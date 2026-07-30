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
    let formServiceMock: jasmine.SpyObj<FormService>;
    let converterServiceMock: jasmine.SpyObj<ConverterService<unknown>>;
    let typesRegistryMock: jasmine.SpyObj<TypesRegistry>;
    let loggerFactoryMock: jasmine.SpyObj<LoggerFactory>;
    let designFormCallbackMock: jasmine.SpyObj<IDesignFormComponent>;

    beforeEach(() => {
        formServiceMock = jasmine.createSpyObj('FormService', ['getFormCacheByName']);
        converterServiceMock = jasmine.createSpyObj('ConverterService', ['convertFromServerToClient']);
        typesRegistryMock = jasmine.createSpyObj('TypesRegistry', ['getComponentSpecification']);
        loggerFactoryMock = jasmine.createSpyObj('LoggerFactory', ['getLogger']);
        loggerFactoryMock.getLogger.and.returnValue(jasmine.createSpyObj('LoggerService', ['error', 'buildMessage']));

        designFormCallbackMock = jasmine.createSpyObj('IDesignFormComponent', [
            'getFormName', 'refresh', 'renderGhosts', 'updateForm', 'redrawDecorators', 'contentRefresh'
        ]);
        designFormCallbackMock.getFormName.and.returnValue('testForm');

        service = new EditorContentService(formServiceMock, converterServiceMock, typesRegistryMock, loggerFactoryMock);
        service.setDesignFormComponent(designFormCallbackMock);
    });

    describe('updateFormData - null parent guard in reorderLayoutContainers (SVY-21255)', () => {

        it('should not crash when parent container is null for an existing layout container', () => {
            const existingContainer = new StructureCache('div', ['col-md-6'], { 'svy-id': 'container-1', 'svy-priority': '1' }, [], 'container-1', false, {});
            existingContainer['parent'] = new StructureCache('div', ['row'], { 'svy-id': 'old-parent' }, [existingContainer], 'old-parent', false, {});

            const formCacheMock = jasmine.createSpyObj('FormCache', ['getLayoutContainer', 'getFormComponent', 'addLayoutContainer', 'getComponent', 'removeComponent', 'removeFormComponent', 'getFormCacheByName', 'getPart']);
            formCacheMock.absolute = false;
            formCacheMock.mainStructure = new StructureCache(null, null);
            formCacheMock.layoutContainersCache = new Map();
            formCacheMock.partComponentsCache = [];

            formCacheMock.getLayoutContainer.and.callFake((id: string) => {
                if (id === 'container-1') return existingContainer;
                return null;
            });
            formCacheMock.getFormComponent.and.returnValue(null);
            formCacheMock.getComponent.and.returnValue(null);
            formCacheMock.getPart.and.returnValue(null);
            formServiceMock.getFormCacheByName.and.returnValue(formCacheMock);

            const updates = JSON.stringify({
                ng2containers: [{
                    tagname: 'div',
                    styleclass: ['col-md-8'],
                    attributes: { 'svy-id': 'container-1', 'svy-priority': '1' },
                    position: {},
                    cssPositionContainer: false
                }],
                childParentMap: {
                    'container-1': { uuid: 'non-existent-parent-uuid' }
                }
            });

            expect(() => service.updateFormData(updates)).not.toThrow();
            expect(existingContainer.classes).toEqual(['col-md-8']);
        });

        it('should not push null to reorderLayoutContainers when newParent is null', () => {
            const existingContainer = new StructureCache('div', ['col-md-6'], { 'svy-id': 'container-1', 'svy-priority': '1' }, [], 'container-1', false, {});
            const oldParent = new StructureCache('div', ['row'], { 'svy-id': 'old-parent', 'svy-priority': '0' }, [existingContainer], 'old-parent', false, {});
            existingContainer['parent'] = oldParent;

            const formCacheMock = jasmine.createSpyObj('FormCache', ['getLayoutContainer', 'getFormComponent', 'addLayoutContainer', 'getComponent', 'removeComponent', 'removeFormComponent', 'getPart']);
            formCacheMock.absolute = false;
            formCacheMock.mainStructure = new StructureCache(null, null);
            formCacheMock.layoutContainersCache = new Map();
            formCacheMock.partComponentsCache = [];

            formCacheMock.getLayoutContainer.and.callFake((id: string) => {
                if (id === 'container-1') return existingContainer;
                return null;
            });
            formCacheMock.getFormComponent.and.returnValue(null);
            formCacheMock.getComponent.and.returnValue(null);
            formCacheMock.getPart.and.returnValue(null);
            formServiceMock.getFormCacheByName.and.returnValue(formCacheMock);

            const updates = JSON.stringify({
                ng2containers: [{
                    tagname: 'div',
                    styleclass: ['col-md-8'],
                    attributes: { 'svy-id': 'container-1', 'svy-priority': '1' },
                    position: {},
                    cssPositionContainer: false
                }],
                childParentMap: {
                    'container-1': { uuid: 'missing-parent-uuid' }
                }
            });

            expect(() => service.updateFormData(updates)).not.toThrow();
            expect(existingContainer.classes).toEqual(['col-md-8']);
        });
    });

    describe('updateFormData - sortChildren null container guard (SVY-21255)', () => {

        it('should not throw TypeError when reorderLayoutContainers contains null entries', () => {
            const formCacheMock = jasmine.createSpyObj('FormCache', ['getLayoutContainer', 'getFormComponent', 'addLayoutContainer', 'getComponent', 'removeComponent', 'removeFormComponent', 'getPart']);
            formCacheMock.absolute = false;
            formCacheMock.mainStructure = new StructureCache(null, null);
            formCacheMock.layoutContainersCache = new Map();
            formCacheMock.partComponentsCache = [];
            formCacheMock.getLayoutContainer.and.returnValue(null);
            formCacheMock.getFormComponent.and.returnValue(null);
            formCacheMock.getComponent.and.returnValue(null);
            formCacheMock.getPart.and.returnValue(null);
            formServiceMock.getFormCacheByName.and.returnValue(formCacheMock);

            const updates = JSON.stringify({});

            expect(() => service.updateFormData(updates)).not.toThrow();
        });
    });
});
