import { TestBed } from '@angular/core/testing';
import { ConverterService } from '../../sablo/converter.service';
import { SabloService } from '../../sablo/sablo.service';
import { LoggerFactory, SessionStorageService, WindowRefService } from '@servoy/public';
import { ServicesService } from '../../sablo/services.service';
import { FoundsetType, FoundsetValue } from './foundset_converter';
import { SabloDeferHelper } from '../../sablo/defer.service';
import { ViewportService } from '../services/viewport.service';
import { DateType } from '../../sablo/converters/date_converter';
import { LoadingIndicatorService } from '../../sablo/util/loading-indicator/loading-indicator.service';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { IPropertyContext, PushToServerEnum, TypesRegistry } from '../../sablo/types_registry';
import { ObjectType } from '../../sablo/converters/object_converter';
import { ChildComponentPropertyValue, ComponentType } from './component_converter';
import { UIBlockerService } from '../services/ui_blocker.service';
import { ServoyService } from '../servoy.service';

describe('ComponentConverter', () => {

    let converterService: ConverterService<any>;
    let typesRegistry: TypesRegistry;
    let loggerFactory: LoggerFactory;
    let sabloService: SabloService;
    let uiBlockerService: UIBlockerService;
    let viewportService: ViewportService;
    let sabloDeferHelper: SabloDeferHelper;
    let fsProp: FoundsetValue;
    let comp: ChildComponentPropertyValue;
    let changeNotified = false;
    let propertyContextWithReject: IPropertyContext;
    let propertyContextWithAllow: IPropertyContext;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports : [ServoyTestingModule],
            providers: [ConverterService, LoggerFactory,
                        WindowRefService, ServicesService, SessionStorageService, ViewportService, LoadingIndicatorService]
        });

        sabloService = TestBed.inject(SabloService);
        sabloService.connect({}, {}, '');
        sabloDeferHelper = TestBed.inject(SabloDeferHelper);
        viewportService = TestBed.inject(ViewportService);
        loggerFactory = TestBed.inject(LoggerFactory);
        converterService = TestBed.inject(ConverterService);
        uiBlockerService = new UIBlockerService(TestBed.inject(ServoyService));
        typesRegistry = TestBed.inject(TypesRegistry);
        typesRegistry.registerGlobalType(FoundsetType.TYPE_NAME, new FoundsetType(sabloService, sabloDeferHelper, viewportService, loggerFactory));
        typesRegistry.registerGlobalType(ComponentType.TYPE_NAME, new ComponentType(converterService, typesRegistry, loggerFactory, viewportService, sabloService, uiBlockerService));
        
        typesRegistry.registerGlobalType(DateType.TYPE_NAME_SABLO, new DateType());
        typesRegistry.registerGlobalType(ObjectType.TYPE_NAME, new ObjectType(typesRegistry, converterService, loggerFactory), true);

        // here we feed in the client side equivalent some (imaginary test) servoy .spec file
        // so we can play a bit with push to server settings in tests
        typesRegistry.addComponentClientSideSpecs({
            "test-datalabel": {
                p: {
                    nonRecDepProp: { s: 2 }, // shallow push to server
                    recDepProp: { s: 2 }, // shallow push to server
                }
            }
        });
        
        changeNotified = false;

        fsProp = converterService.convertFromServerToClient(createDefaultFoundset(), typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME),
                undefined, undefined, undefined, propertyContextWithReject);

        propertyContextWithReject = {
            getProperty: (_propertyName) => _propertyName === "myfoundset" ? fsProp : undefined,
            getPushToServerCalculatedValue: () => PushToServerEnum.REJECT,
            isInsideModel: true
        };
        propertyContextWithAllow = {
            getProperty: (_propertyName) => _propertyName === "myfoundset" ? fsProp : undefined,
            getPushToServerCalculatedValue: () => PushToServerEnum.ALLOW,
            isInsideModel: true
        };
    });
    
    // as property types reuse parts of JSON that comes from server, return a new instance each time to avoid meddling between separate tests due to this
    const initialChildComponentServerValue = () => {
        return {
        "forFoundset": "myfoundset",
        "componentDirectiveName": "test-datalabel",
        "name": "listformcomponentcontainer_1$containedForm$datalabel_1",
        "position": {
            "left": "20px",
            "top": "10px",
            "width": "80px",
            "height": "30px"
        },
        "model": {
            "nonRecDepProp": "Hello there!",
        },
        "handlers": {},
        "foundsetConfig": {
            "recordBasedProperties": [
                "recDepProp"
            ]
        },
        "model_vp": [
            {
                "recDepProp": 10248
            },
            {
                "recDepProp": 10255
            },
            {
                "recDepProp": 10256
            },
            {
                "recDepProp": 10257
            },
            {
                "recDepProp": 10259
            }
        ]
    }};

    // as property types reuse parts of JSON that comes from server, return a new instance each time to avoid meddling between separate tests due to this
    const createDefaultFoundset = () => {
        const json = {};
        json['foundsetId'] = 1;
        json['serverSize'] = 10;
        json['sortColumns'] = '';
        json['selectedRowIndexes'] = [0];
        json['multiSelect'] = false;
        json['hasMoreRows'] = true;
        const viewport = {};
        viewport['startIndex'] = 0;
        viewport['size'] = 5;
        viewport['rows'] = [{ _svyRowId: '5.ALFKI;_0' },
        { _svyRowId: '5.ANATR;_1' },
        { _svyRowId: '5.AROUT;_3' },
        { _svyRowId: '5.BERGS;_4' },
        { _svyRowId: '5.BLAUS;_5' }];
        json['viewPort'] = viewport;
        return json;
    };

    const getAndClearNotified = () => {
        const tm = changeNotified;
        changeNotified = false;
        return tm;
    };

    it('fs should be there', () => {
        expect(fsProp).toBeDefined();
        expect(fsProp.foundsetId).toBe(1, 'foundsetId should be \'1\' ');
        expect(fsProp.serverSize).toBe(10, 'foundset serverSize should be \'10');
        expect(fsProp.sortColumns).toBe('', 'foundset sort columns should be empty');
        expect(fsProp.multiSelect).toBe(false, 'foundset should not allow multiSelect');
        expect(fsProp.selectedRowIndexes.length).toBe(1, 'foundset should have \'1\' record selected ');
        expect(fsProp.selectedRowIndexes[0]).toBe(0, 'foundset should have first record selected');
        expect(fsProp.hasMoreRows).toBe(true, 'foundset should have more rows');
        expect(fsProp.viewPort).toBeDefined();
        expect(fsProp.viewPort.size).toBe(5, 'viewport size should be \'5\'');
        expect(fsProp.viewPort.startIndex).toBe(0, 'viewport startIndex should be \'0\'');
    });

    it('Should convert child component prop. from server to client', () => {
        comp = converterService.convertFromServerToClient(initialChildComponentServerValue(), typesRegistry.getAlreadyRegisteredType(ComponentType.TYPE_NAME), undefined, undefined, undefined, propertyContextWithAllow);
        expect(comp).toBeDefined();
        comp.getInternalState().setChangeListener((_doNotPush?: boolean) => {
            changeNotified = true;
        });
        expect(getAndClearNotified()).toEqual(false);

        expect(comp.model.nonRecDepProp).toBe("Hello there!", 'non-record-dependent-prop should be correct in "model"');
        expect(comp.modelViewport.length).toBe(5, 'model viewport size should be 5');
        expect(comp.modelViewport[0].recDepProp).toBe(10248, 'record dep. prop. value should be correct in "modelViewport"');
        expect(comp.modelViewport[0].nonRecDepProp).toBe("Hello there!", 'non-record-dependent-prop should be correct in "modelViewport"');
        expect(comp.modelViewport[1].recDepProp).toBe(10255, 'record dep. prop. value should be correct in "modelViewport"');
        expect(comp.modelViewport[1].nonRecDepProp).toBe("Hello there!", 'non-record-dependent-prop should be correct in "modelViewport"');
    });

    it('Should send property changes to server when asked to do so (api call on ChildComponentPropertyValue) for root properties by the component that uses the child component property', () => {
        comp = converterService.convertFromServerToClient(initialChildComponentServerValue(), typesRegistry.getAlreadyRegisteredType(ComponentType.TYPE_NAME), undefined, undefined, undefined, propertyContextWithAllow);
        comp.getInternalState().setChangeListener((_doNotPush?: boolean) => {
            changeNotified = true;
        });
        expect(getAndClearNotified()).toEqual(false);

        // the line below should send changes to server automatically
        // it's still a root prop of a component that generally needs an emit to be called; and an emit would be called by a component and then
        // for example list form component (or however uses the child 'component' property type) would call ChildComponentPropertyValue.sendChanges that handles
        // the property value assignment + the send to server; so no need to send it automatically 
        // comment in viewport.service.ts needsRowProxies(...)
        comp.modelViewport[1].recDepProp = 101011;
        expect(getAndClearNotified()).toEqual(false);

        comp.sendChanges("recDepProp", 101010, 10255, "5.ANATR;_1", false);
        expect(getAndClearNotified()).toEqual(true);
        expect(comp.getInternalState().hasChanges()).toEqual(true);
        expect(converterService.convertFromClientToServer(comp, typesRegistry.getAlreadyRegisteredType(ComponentType.TYPE_NAME), comp, propertyContextWithAllow)[0]).toEqual(
            [{ viewportDataChanged: { _svyRowId: '5.ANATR;_1', dp: 'recDepProp', value: 101010 } }]
        );
        expect(getAndClearNotified()).toEqual(false);
        expect(comp.getInternalState().hasChanges()).toEqual(false);

        // comp.modelViewport[1].nonRecDepProp = "new value 1"; // only child component emits would do this assignment but by the list from component's (or other parent comp) call to ChildComponentPropertyValue.sendChanges()

        comp.sendChanges("nonRecDepProp", "new value 1", "Hello there!", "5.ANATR;_1", false);
        expect(getAndClearNotified()).toEqual(true);
        expect(comp.getInternalState().hasChanges()).toEqual(true);
        expect(converterService.convertFromClientToServer(comp, typesRegistry.getAlreadyRegisteredType(ComponentType.TYPE_NAME), comp, propertyContextWithAllow)[0]).toEqual(
            [{ viewportDataChanged: { _svyRowId: '5.ANATR;_1', dp: 'nonRecDepProp', value: "new value 1" } }]
        );
        expect(getAndClearNotified()).toEqual(false);
        expect(comp.getInternalState().hasChanges()).toEqual(false);
    });

    it('Should work when inserting one record above (it should not restore "original unproxied" value of record that slided down on top of the newly added record)', () => {
        comp = converterService.convertFromServerToClient(initialChildComponentServerValue(),
                    typesRegistry.getAlreadyRegisteredType(ComponentType.TYPE_NAME), undefined, undefined, undefined, propertyContextWithAllow);
        comp.getInternalState().setChangeListener((_doNotPush?: boolean) => {
            changeNotified = true;
        });
        expect(getAndClearNotified()).toEqual(false);

        // insert one row at index 0 in foundset prop.
        fsProp = converterService.convertFromServerToClient({
            upd_serverSize: 5,
            upd_selectedRowIndexes: [1],
            upd_viewPort:
            {
                startIndex: 0,
                size: 6,
                upd_rows:
                    [
                        {
                            rows: [{ _svyRowId: '5.BLABL;_6' }],
                            startIndex: 0,
                            endIndex: 0,
                            type: 1
                        }
                    ]
            }
        }, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fsProp, undefined, undefined, propertyContextWithReject);

        // insert same row at index 0 in component prop.
        comp = converterService.convertFromServerToClient({
            propertyUpdates: {
                model_vp_ch:
                [
                    {
                        rows: [{ "recDepProp": 191919 }],
                        startIndex: 0,
                        endIndex: 0,
                        type: 1
                    }
                ]
            }
        }, typesRegistry.getAlreadyRegisteredType(ComponentType.TYPE_NAME), comp, undefined, undefined, propertyContextWithAllow);

        expect(comp.model.nonRecDepProp).toBe("Hello there!", 'non-record-dependent-prop should be correct in "model"');
        expect(comp.modelViewport.length).toBe(6, 'model viewport size should be 6');
        expect(comp.modelViewport[0].recDepProp).toBe(191919, 'record dep. prop. value should be correct in "modelViewport"');
        expect(comp.modelViewport[0].nonRecDepProp).toBe("Hello there!", 'non-record-dependent-prop should be correct in "modelViewport"');
        expect(comp.modelViewport[1].recDepProp).toBe(10248, 'record dep. prop. value should be correct in "modelViewport"');
        expect(comp.modelViewport[1].nonRecDepProp).toBe("Hello there!", 'non-record-dependent-prop should be correct in "modelViewport"');
        expect(comp.modelViewport[2].recDepProp).toBe(10255, 'record dep. prop. value should be correct in "modelViewport"');
        expect(comp.modelViewport[2].nonRecDepProp).toBe("Hello there!", 'non-record-dependent-prop should be correct in "modelViewport"');
    });

});
