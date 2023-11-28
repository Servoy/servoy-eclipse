import { TestBed } from '@angular/core/testing';


import { ConverterService, IChangeAwareValue } from '../../sablo/converter.service';
import { LoggerFactory, SpecTypesService, WindowRefService, ICustomObjectValue, BaseCustomObject } from '@servoy/public';

import { TypesRegistry, ICustomTypesFromServer, IPropertiesFromServer, IPropertyDescriptionFromServerWithMultipleEntries, ITypeFromServer,
            IFactoryTypeDetails, IPropertyContext, PushToServerEnum, PushToServerUtils } from '../../sablo/types_registry';
import { CustomObjectTypeFactory, CustomObjectType, ICOTFullValueFromServer, ICOTGranularUpdatesToServer, BaseCustomObjectState,
            ICOTFullObjectToServer, ICOTNoOpToServer, ICOTGranularUpdatesFromServer, ICOTGranularOpToServer,  } from './json_object_converter';
import { DateType } from '../../sablo/converters/date_converter';
import { ObjectType } from '../../sablo/converters/object_converter';
import { CustomArrayType, CustomArrayTypeFactory, ICATFullArrayToServer, ICATGranularUpdatesToServer } from './json_array_converter';

describe('JSONObjectConverter', () => {

    let converterService: ConverterService<unknown>;
    let typesRegistry: TypesRegistry;
    let loggerFactory: LoggerFactory;
    let specTypesService: SpecTypesService;

    let oneTabType: CustomObjectType;
    let oneTabPushToServer: PushToServerEnum;
    let tabHolderType: CustomObjectType;
    let tabArrayType: CustomArrayType<Tab>;
    let tabHolderPushToServer: PushToServerEnum;

    let untypedObjectALLOWWithVariousSubpropPTSType: CustomObjectType;
    let untypedObjectALLOWWithVariousSubpropPTSPushToServer: PushToServerEnum;
    let untypedObjectWithREJECTOnSubpropType: CustomObjectType;
    let untypedObjectWithREJECTOnSubpropPushToServer: PushToServerEnum;
    let untypedObjectWithALLOWOnSubpropType: CustomObjectType;
    let untypedObjectWithALLOWOnSubpropPushToServer: PushToServerEnum;
    let untypedObjectWithSHALLOWOnSubpropType: CustomObjectType;
    let untypedObjectWithSHALLOWOnSubpropPushToServer: PushToServerEnum;
    let untypedObjectWithDEEPOnSubpropType: CustomObjectType;
    let untypedObjectWithDEEPOnSubpropPushToServer: PushToServerEnum;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [ConverterService, LoggerFactory, WindowRefService, SpecTypesService]
        });
        converterService = TestBed.inject(ConverterService);        typesRegistry = TestBed.inject(TypesRegistry);
        loggerFactory = TestBed.inject(LoggerFactory);
        specTypesService = TestBed.inject(SpecTypesService);

        typesRegistry.registerGlobalType(DateType.TYPE_NAME_SVY, new DateType(), true);
        typesRegistry.registerGlobalType(ObjectType.TYPE_NAME, new ObjectType(typesRegistry, converterService), true);
        typesRegistry.getTypeFactoryRegistry().contributeTypeFactory(CustomArrayTypeFactory.TYPE_FACTORY_NAME, new CustomArrayTypeFactory(typesRegistry, converterService, loggerFactory));
        typesRegistry.getTypeFactoryRegistry().contributeTypeFactory(CustomObjectTypeFactory.TYPE_FACTORY_NAME, new CustomObjectTypeFactory(typesRegistry, converterService, specTypesService, loggerFactory));

        // IWebObjectTypesFromServer { [specName: string]: IWebObjectSpecificationFromServer; }
        // IWebObjectSpecificationFromServer {
        //       p?: IPropertiesFromServer;
        //       ftd?: IFactoryTypeDetails; // this will be the custom type details from spec something like { "JSON_obj": ICustomTypesFromServer}}
        //       /** any handlers */
        //       h?: IWebObjectFunctionsFromServer;
        //       /** any api functions */
        //       a?: IWebObjectFunctionsFromServer;
        // }
        // IFactoryTypeDetails { [factoryTypeName: string]: any; } // generic, for any factory type; that any will be ICustomTypesFromServer in case of JSON_obj factory
        // ICustomTypesFromServer { [customTypeName: string]: IPropertiesFromServer; }
        // IPropertiesFromServer { [propertyName: string]: IPropertyDescriptionFromServer; }
        // IPropertyDescriptionFromServer = ITypeFromServer | IPropertyDescriptionFromServerWithMultipleEntries;
        // ITypeFromServer = string | [string, any]
        // IPropertyDescriptionFromServerWithMultipleEntries { t?: ITypeFromServer; s: PushToServerEnumServerValue }
        // PushToServerEnumServerValue = 0 | 1 | 2 | 3;
        const factoryTypeDetails = {} as IFactoryTypeDetails;
        factoryTypeDetails[CustomObjectTypeFactory.TYPE_FACTORY_NAME] = {
                Tab: {
                    myvalue: { s: 2 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    rejectString: { s: 0 } as IPropertyDescriptionFromServerWithMultipleEntries
                } as IPropertiesFromServer,
                TabHolder: {
                    clientSideOnlyTab: { t: [CustomObjectTypeFactory.TYPE_FACTORY_NAME, 'Tab'], s: 0 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    tab: [CustomObjectTypeFactory.TYPE_FACTORY_NAME, 'Tab'] as ITypeFromServer,
                    tab2: { t: [CustomObjectTypeFactory.TYPE_FACTORY_NAME, 'Tab'], s: 1 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    tab3: { t: [CustomObjectTypeFactory.TYPE_FACTORY_NAME, 'Tab'], s: 2 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    tabs: { t: [CustomArrayTypeFactory.TYPE_FACTORY_NAME, [CustomObjectTypeFactory.TYPE_FACTORY_NAME, 'Tab']] as ITypeFromServer, s: 1 } as
                        IPropertyDescriptionFromServerWithMultipleEntries
                } as IPropertiesFromServer,
                DumbSubpropsWithVariousPTS: {
                    rejectSubprop: { s: 0 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    allowSubprop: { s: 1 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    shallowSubprop: { s: 2 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    deepSubprop: { s: 3 } as IPropertyDescriptionFromServerWithMultipleEntries
                } as IPropertiesFromServer,
                DumbSubpropsThatInheritPTSFromParent: {
                    sa007: { s: 1 } as IPropertyDescriptionFromServerWithMultipleEntries
                } as IPropertiesFromServer
            } as ICustomTypesFromServer;

        typesRegistry.addComponentClientSideSpecs({
            someTabPanelSpec: {
                p: {
                    oneTab: { t: [CustomObjectTypeFactory.TYPE_FACTORY_NAME, 'Tab'] as ITypeFromServer, s: 1 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    tabHolder: { t: [CustomObjectTypeFactory.TYPE_FACTORY_NAME, 'TabHolder'] as ITypeFromServer, s: 1 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    untypedObjectALLOWWithVariousSubpropPTS: {
                         t: [CustomObjectTypeFactory.TYPE_FACTORY_NAME, 'DumbSubpropsWithVariousPTS'] as ITypeFromServer,
                         s: 1 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    untypedObjectREJECT: {
                        t: [CustomObjectTypeFactory.TYPE_FACTORY_NAME, 'DumbSubpropsThatInheritPTSFromParent'] as ITypeFromServer,
                        s: 0 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    untypedObjectALLOW: {
                        t: [CustomObjectTypeFactory.TYPE_FACTORY_NAME, 'DumbSubpropsThatInheritPTSFromParent'] as ITypeFromServer,
                        s: 1 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    untypedObjectSHALLOW: {
                        t: [CustomObjectTypeFactory.TYPE_FACTORY_NAME, 'DumbSubpropsThatInheritPTSFromParent'] as ITypeFromServer,
                        s: 2 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    untypedObjectDEEP: {
                        t: [CustomObjectTypeFactory.TYPE_FACTORY_NAME, 'DumbSubpropsThatInheritPTSFromParent'] as ITypeFromServer,
                        s: 3 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    tabArrayJustForType: { t: [CustomArrayTypeFactory.TYPE_FACTORY_NAME, [CustomObjectTypeFactory.TYPE_FACTORY_NAME, 'Tab']] as ITypeFromServer, s: 1 } as
                        IPropertyDescriptionFromServerWithMultipleEntries
                },
                ftd: factoryTypeDetails
            }
        });

        const spec = typesRegistry.getComponentSpecification('someTabPanelSpec');
        oneTabType = spec.getPropertyType('oneTab') as CustomObjectType;
        oneTabPushToServer = spec.getPropertyPushToServer('oneTab'); // so computed not declared (undefined -> REJECT)
        tabHolderType = spec.getPropertyType('tabHolder') as CustomObjectType;
        tabArrayType = spec.getPropertyType('tabArrayJustForType') as CustomArrayType<Tab>;
        tabHolderPushToServer = spec.getPropertyPushToServer('tabHolder'); // so computed not declared (undefined -> REJECT)

        untypedObjectALLOWWithVariousSubpropPTSType = spec.getPropertyType('untypedObjectALLOWWithVariousSubpropPTS') as CustomObjectType;
        untypedObjectALLOWWithVariousSubpropPTSPushToServer = spec.getPropertyPushToServer('untypedObjectALLOWWithVariousSubpropPTS'); // so computed not declared (undefined -> REJECT)
        untypedObjectWithREJECTOnSubpropType = spec.getPropertyType('untypedObjectREJECT') as CustomObjectType;
        untypedObjectWithREJECTOnSubpropPushToServer = spec.getPropertyPushToServer('untypedObjectREJECT'); // so computed not declared (undefined -> REJECT)
        untypedObjectWithALLOWOnSubpropType = spec.getPropertyType('untypedObjectALLOW') as CustomObjectType;
        untypedObjectWithALLOWOnSubpropPushToServer = spec.getPropertyPushToServer('untypedObjectALLOW'); // so computed not declared (undefined -> REJECT)
        untypedObjectWithSHALLOWOnSubpropType = spec.getPropertyType('untypedObjectSHALLOW') as CustomObjectType;
        untypedObjectWithSHALLOWOnSubpropPushToServer = spec.getPropertyPushToServer('untypedObjectSHALLOW'); // so computed not declared (undefined -> REJECT)
        untypedObjectWithDEEPOnSubpropType = spec.getPropertyType('untypedObjectDEEP') as CustomObjectType;
        untypedObjectWithDEEPOnSubpropPushToServer = spec.getPropertyPushToServer('untypedObjectDEEP'); // so computed not declared (undefined -> REJECT)
    });

    const createTabJSON = (): ICOTFullValueFromServer => ({ v: { name: 'test', myvalue: 'test' }, vEr: 1});
    const getParentPropertyContext = (pushToServerCalculatedValueForProp: PushToServerEnum): IPropertyContext => ({
            getProperty: (_propertyName: string) => undefined,
            getPushToServerCalculatedValue: () => pushToServerCalculatedValueForProp,
            isInsideModel: true
        });

    const createSimpleCustomObjectWithUntypedAndSpecificPTSOnSubprops = (): ICOTFullValueFromServer => {
        const subProp = () => ({ _T: ObjectType.TYPE_NAME, _V: {
                x: { _T: ObjectType.TYPE_NAME, _V:
                       ['bla', { _T: DateType.TYPE_NAME_SVY, _V: '2007-12-03T' } ]
                   },  // this is how ObjectPropertyType sends it JSON from server if it detects nested special types such as Date,
                y: 8
            }
        });
        return {
            v: {
                rejectSubprop: subProp(),
                allowSubprop: subProp(),
                shallowSubprop: subProp(),
                deepSubprop: subProp()
            },
            vEr: 1
        };
    };

    const createSimpleCustomObjectWithUntypedSubprops = (): ICOTFullValueFromServer => ({
            v: {
                a: 'test1',
                b: { test2: 1, a: true },
                c: 5,
                d: [ 1, 2, 3 ],
                e: { _T: ObjectType.TYPE_NAME, _V: {
                     x: { _T: ObjectType.TYPE_NAME, _V:
                            ['bla', { _T: DateType.TYPE_NAME_SVY, _V: '2018-01-01T00:00:00' } ]
                        },
                     y: 8 }
                }, // this is how ObjectPropertyType sends it JSON from server if it detects nested special types such as Date
                f: { _T: DateType.TYPE_NAME_SVY, _V: '2017-12-03T10:15:30' }    
            },
            vEr: 1
        });

    it('object should be created correctly from server side, and after an update from server it should still not be marked as changed (by client)', () => {
        const val = converterService.convertFromServerToClient(createTabJSON(),
               oneTabType , undefined, undefined, undefined, getParentPropertyContext(oneTabPushToServer));

        let tabAsSeenInternally = val as IChangeAwareValue;
        let tab = val as Tab;

        expect(tab).toBeDefined();
        expect(tab.name).toBe('test', 'name should be test');
        expect(tab.myvalue).toBe('test', 'myvalue should be test');

        expect(tabAsSeenInternally.getInternalState().hasChanges()).toBe(false, 'should not have changes');

        tab = converterService.convertFromServerToClient({ u: [{ k: 'myvalue', v: 'test2' }], vEr: 1 } as ICOTGranularUpdatesFromServer,
               oneTabType , tab, undefined, undefined, getParentPropertyContext(oneTabPushToServer)) as Tab;
        tabAsSeenInternally = val as IChangeAwareValue;

        expect(tab.myvalue).toBe('test2', 'myvalue should be test2');
        expect(tabAsSeenInternally.getInternalState().hasChanges()).toBe(false, 'should not have changes');
    });

    it('object created and changed from client side', () => {
        const val = converterService.convertFromServerToClient(createTabJSON(),
               oneTabType , undefined, undefined, undefined, getParentPropertyContext(oneTabPushToServer));

        const tabAsSeenInternally = val as IChangeAwareValue;
        const tab = val as Tab;

        let changeNotified = false;
        let triggeredSendToServer = false;
        tabAsSeenInternally.getInternalState().setChangeListener((doNotPushNow?: boolean) => {
            changeNotified = true;
            triggeredSendToServer = !doNotPushNow;
        });
        const checkNotifiedAndTriggeredAndClear = (changeNotifiedWanted: boolean, triggeredSendToServerWanted: boolean) => {
            expect(changeNotified).toBe(changeNotifiedWanted);
            expect(triggeredSendToServer).toBe(triggeredSendToServerWanted);
            changeNotified = false;
            triggeredSendToServer = false;
        };

        tab.rejectString = 'changedMyself';
        checkNotifiedAndTriggeredAndClear(false, false); // (REJECT pushToServer on subProp) not marked by obj proxy that it has change by ref, and not triggered to be sent automatically
        expect(tabAsSeenInternally.getInternalState().hasChanges()).toBe(false, 'should not have changes because of REJECT pushToServer');

        tab.name = 'test2';
        checkNotifiedAndTriggeredAndClear(true, false); // (ALLOW pushToServer on subProp) marked by obj proxy that it has change by ref, and not triggered to be sent automatically
        expect(tabAsSeenInternally.getInternalState().hasChanges()).toBe(true, 'should have changes even if they are not sent unless manually triggered, because of ALLOW pushToServer');

        tab.myvalue = 'test';
        checkNotifiedAndTriggeredAndClear(false, false); // value did not change!

        tab.myvalue = 'test2';
        checkNotifiedAndTriggeredAndClear(true, true); // (SHALLOW pushToServer on subProp) marked by obj proxy that it has change by ref, and triggered to be sent automatically

        const changes: ICOTGranularUpdatesToServer = converterService.convertFromClientToServer(tab, oneTabType, val,
            getParentPropertyContext(oneTabPushToServer))[0];
        expect(changes).toBeDefined('change object should be generated');
        expect(changes.vEr).toBe(1);
        expect(changes.u).toBeDefined('change object  shoulld have updates');
        expect(changes.u.length).toBe(2, 'should have 1 update');
        expect(changes.u[0].k).toBe('name');
        expect(changes.u[0].v).toBe('test2');
        expect(changes.u[1].k).toBe('myvalue');
        expect(changes.u[1].v).toBe('test2');

        expect(tabAsSeenInternally.getInternalState().hasChanges()).toBe(false, 'should not have changes');

        const changes2: ICOTNoOpToServer = converterService.convertFromClientToServer(tab, oneTabType, val, getParentPropertyContext(oneTabPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');
    });

    it( 'send obj as arg to handler, change a subprop by ref', () => {
        const val = converterService.convertFromServerToClient(createTabJSON(),
               oneTabType , undefined, undefined, undefined, getParentPropertyContext(oneTabPushToServer));

        const tabAsSeenInternally = val as IChangeAwareValue;
        let tab = val as Tab;

        let changeListenerWasCalled = false;
        tabAsSeenInternally.getInternalState().setChangeListener(() => { changeListenerWasCalled = true; });

        // simulate a send to server as argument to a handler for this array (oldVal undefined) - to make sure it doesn't messup it's state if it's also a model prop. (it used getParentPropertyContext above which is for a model prop)
        const changesAndVal: [ICOTFullObjectToServer, Tab] = converterService.convertFromClientToServer(tab, oneTabType, undefined,
            PushToServerUtils.PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES);
        const changes = changesAndVal[0];
        
        tab = changesAndVal[1];
        
        expect(changes.vEr).toBe(0, 'full value being sent to server');
        expect(changes.v).toBeDefined('change object should have a value');
        expect(changes.v.name).toBe('test');
        expect(changes.v.myvalue).toBe('test');

        tab.myvalue = 'test4';

        expect(changeListenerWasCalled).toBe(true);

        const changes2: ICOTGranularUpdatesToServer = converterService.convertFromClientToServer(tab, oneTabType, tab,
            getParentPropertyContext(oneTabPushToServer))[0];
        expect( changes2.vEr ).toBe( 1, 'checking version for element update' );
        expect( changes2.u.length ).toBe( 1, 'checking that it is exactly 1 update' );
        expect( changes2.u[0].k ).toBe( 'myvalue', 'checking that it is the correct key' );
        expect( changes2.u[0].v ).toBe( 'test4', 'checking that it is the correct value');
    } );

    it( 'send obj from model (with push to server reject) as arg to handler', () => {
        const val = converterService.convertFromServerToClient({ v: { sa007: 'test' }, vEr: 1},
               untypedObjectWithREJECTOnSubpropType , undefined, undefined, undefined, getParentPropertyContext(untypedObjectWithREJECTOnSubpropPushToServer));

        const valAsSeenInternally = val as IChangeAwareValue;
        let valCoT = val as ICustomObjectValue;

        let changeListenerWasCalled = false;
        valAsSeenInternally.getInternalState().setChangeListener(() => { changeListenerWasCalled = true; });

        // simulate a send to server as argument to a handler, which should work even though it is a model value with push to server reject
        const changesAndVal: [ICOTFullObjectToServer, ICustomObjectValue] = converterService.convertFromClientToServer(val, untypedObjectWithREJECTOnSubpropType, undefined,
            PushToServerUtils.PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES);
        const changes = changesAndVal[0];
        
        valCoT = changesAndVal[1];
        
        expect(changes.vEr).toBe(0, 'full value being sent to server');
        expect(changes.v).toBeDefined('change object should have a value');
        expect(changes.v.sa007).toBe('test');

        valCoT.sa007 = 'test4';

        expect(changeListenerWasCalled).toBe(false);
        
        // inside the model it is push to server reject
        const changes2: ICOTNoOpToServer = converterService.convertFromClientToServer(valCoT, untypedObjectWithREJECTOnSubpropType, valCoT,
            getParentPropertyContext(untypedObjectWithREJECTOnSubpropPushToServer))[0];
        expect( changes2.n ).toBeTrue();
    } );

    it( 'change subprop. by ref but do not send to server (so it still has changes to send for the model property), then send obj as arg to handler, change another tab by ref; both tabs changed by ref in the model should be then sent to server', () => {
        let tabHolder = new TabHolder();
        tabHolder.id = 'test';
        tabHolder.tab = new Tab();
        tabHolder.tab.name = 'test';
        tabHolder.tab.myvalue = 'test';
        
        let tabElement = new Tab();
        tabElement.name = 'test1';
        tabElement.myvalue = 'test1'; 
        tabHolder.tabs = [tabElement];

        // simulate that it is set into the model and sent to server
        const sendToServerResult = converterService.convertFromClientToServer(tabHolder, tabHolderType, undefined, getParentPropertyContext(tabHolderPushToServer));
        tabHolder = sendToServerResult[1]; // it has been converted into a Proxy of original object
        let tabHolderSeenInternally = sendToServerResult[1] as IChangeAwareValue;

        let changeListenerWasCalled = false;
        tabHolderSeenInternally.getInternalState().setChangeListener(() => { changeListenerWasCalled = true; });

        tabElement = new Tab();
        tabElement.name = 'test11';
        tabElement.myvalue = 'test11'; 

        tabHolder.tabs[0]= tabElement;
        expect(changeListenerWasCalled).toBe(true);

        // simulate a send to server as argument to a handler for this obj (oldVal undefined) - to make sure it doesn't messup it's state if it's also a model prop. (it used getParentPropertyContext above which is for a model prop)
        const changesAndVal: [ICOTFullObjectToServer, TabHolder] = converterService.convertFromClientToServer(tabHolder, tabHolderType, undefined,
            PushToServerUtils.PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES);
        const changes = changesAndVal[0];
        
        tabHolder = changesAndVal[1];
        
        expect( changes.vEr ).toBe( 0 );
        expect( changes.v ).toBeDefined( 'change object should have updates' );
        let fullTabChange: ICOTFullObjectToServer = changes.v.tab;
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test', 'tab should be test1' );
        expect( fullTabChange.v.myvalue ).toBe( 'test', 'tab should be test1' );
        let fullTabArrayChange: ICATFullArrayToServer = changes.v.tabs;
        expect( fullTabArrayChange.vEr ).toBe( 0 );
        expect( fullTabArrayChange.v.length ).toBe( 1, 'should have only one Tab' );
        fullTabChange = fullTabArrayChange.v[0];
        expect( fullTabChange.vEr ).toBe( 0, 'initial ver' );
        expect( fullTabChange.v.name ).toBe( 'test11', 'should be test1' );
        expect( fullTabChange.v.myvalue ).toBe( 'test11', 'should be test1' );

        // ok, see if it kept track of the changes for model prop value.
        const changes2: ICOTGranularUpdatesToServer = converterService.convertFromClientToServer(tabHolder, tabHolderType, tabHolder,
            getParentPropertyContext(tabHolderPushToServer))[0];
        expect( changes2.vEr ).toBe(1, 'checking initial version for full model update' );
        expect( changes2.u.length ).toBe( 1, 'checking that it is exactly 1 update' );
        expect( changes2.u[0].k).toBe( 'tabs', 'checking that it is tabs array' );
        const partialTabChange: ICATGranularUpdatesToServer = changes2.u[0].v;
        expect( partialTabChange.vEr ).toBe( 1 );
        expect( partialTabChange.u.length ).toBe( 1, 'checking that it is exactly 1 update' );
        expect( partialTabChange.u[0].i ).toBe( 0, 'checking that it is first el' );

        fullTabChange = partialTabChange.u[0].v;
        
        expect( fullTabChange.vEr ).toBe( 0, 'full send due to change by ref' );
        expect( fullTabChange.v.name ).toBe( 'test11', 'full send' );
        expect( fullTabChange.v.myvalue ).toBe( 'test11', 'full send' );
    } );

    it( 'send obj as arg to handler, change a array subprop\'s element', () => {
        let tabHolder = new TabHolder();
        tabHolder.id = 'test';
        tabHolder.tab = new Tab();
        tabHolder.tab.name = 'test';
        tabHolder.tab.myvalue = 'test';
        
        let tabElement = new Tab();
        tabElement.name = 'test1';
        tabElement.myvalue = 'test1'; 
        tabHolder.tabs = [tabElement];

        // simulate that it is set into the model and sent to server
        const sendToServerResult = converterService.convertFromClientToServer(tabHolder, tabHolderType, undefined, getParentPropertyContext(tabHolderPushToServer));
        tabHolder = sendToServerResult[1]; // it has been converted into a Proxy of original object
        let tabHolderSeenInternally = sendToServerResult[1] as IChangeAwareValue;

        let changeListenerWasCalled = false;
        tabHolderSeenInternally.getInternalState().setChangeListener(() => { changeListenerWasCalled = true; });

        // simulate a send to server as argument to a handler for this obj (oldVal undefined) - to make sure it doesn't messup it's state if it's also a model prop. (it used getParentPropertyContext above which is for a model prop)
        const changesAndVal: [ICOTFullObjectToServer, TabHolder] = converterService.convertFromClientToServer(tabHolder, tabHolderType, undefined,
            PushToServerUtils.PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES);
        const changes = changesAndVal[0];
        
        tabHolder = changesAndVal[1];
        
        expect( changes.vEr ).toBe( 0 );
        expect( changes.v ).toBeDefined( 'change object should have updates' );
        let fullTabChange: ICOTFullObjectToServer = changes.v.tab;
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test', 'tab should be test1' );
        expect( fullTabChange.v.myvalue ).toBe( 'test', 'tab should be test1' );
        let fullTabArrayChange: ICATFullArrayToServer = changes.v.tabs;
        expect( fullTabArrayChange.vEr ).toBe( 0 );
        expect( fullTabArrayChange.v.length ).toBe( 1, 'should have only one Tab' );
        fullTabChange = fullTabArrayChange.v[0];
        expect( fullTabChange.vEr ).toBe( 0, 'initial ver' );
        expect( fullTabChange.v.name ).toBe( 'test1', 'should be test1' );
        expect( fullTabChange.v.myvalue ).toBe( 'test1', 'should be test1' );

        tabElement = new Tab();
        tabElement.name = 'test11';
        tabElement.myvalue = 'test11'; 

        tabHolder.tabs[0]= tabElement;

        expect(changeListenerWasCalled).toBe(true);

        const changes2: ICOTGranularUpdatesToServer = converterService.convertFromClientToServer(tabHolder, tabHolderType, tabHolder,
            getParentPropertyContext(tabHolderPushToServer))[0];
        expect( changes2.vEr ).toBe( 1, 'checking version for element update' );
        expect( changes2.u.length ).toBe( 1, 'checking that it is exactly 1 update' );
        expect( changes2.u[0].k).toBe( 'tabs', 'checking that it is tabs array' );
        const partialTabChange: ICATGranularUpdatesToServer = changes2.u[0].v;
        expect( partialTabChange.vEr ).toBe( 1 );
        expect( partialTabChange.u.length ).toBe( 1, 'checking that it is exactly 1 update' );
        expect( partialTabChange.u[0].i ).toBe( 0, 'checking that it is first el' );

        fullTabChange = partialTabChange.u[0].v;
        
        expect( fullTabChange.vEr ).toBe( 0, 'full send due to change by ref' );
        expect( fullTabChange.v.name ).toBe( 'test11', 'full send' );
        expect( fullTabChange.v.myvalue ).toBe( 'test11', 'full send' );
    } );

    it('create object in scripting', () => {
        const tab = new Tab();
        tab.name = 'test';
        tab.myvalue = 'test2';
        tab.rejectString = 'thisWillNotBeSentToServer';

        const changes: [ICOTFullObjectToServer, any] = converterService.convertFromClientToServer(tab, oneTabType, undefined, getParentPropertyContext(oneTabPushToServer));
        expect(changes[0]).toBeDefined('change object should be generated');
        expect(changes[0].vEr).toBe(0, 'new full value being sent to server');
        expect(changes[0].v).toBeDefined('change object  shoulld have a value');
        expect(changes[0].v.name).toBe('test');
        expect(changes[0].v.myvalue).toBe('test2');
        
        const tabAsChangeAwareValue = changes[1] as IChangeAwareValue;

        tabAsChangeAwareValue.getInternalState().setChangeListener(() => {});
        const changes2: ICOTNoOpToServer = converterService.convertFromClientToServer(tab, oneTabType, tab, getParentPropertyContext(oneTabPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');
    });

    it('nested object created in scripting', () => {
        let tabHolder = new TabHolder();
        tabHolder.id = 'test';
        tabHolder.tab2 = new Tab();
        tabHolder.tab2.name = 'test';
        tabHolder.tab2.myvalue = 'test';
        tabHolder.clientSideOnlyTab = new Tab();
        tabHolder.clientSideOnlyTab.name = 'test';

        let sendToServerResult = converterService.convertFromClientToServer(tabHolder, tabHolderType, undefined, getParentPropertyContext(tabHolderPushToServer));
        tabHolder = sendToServerResult[1]; // it has been converted into a Proxy of original object
        let tabHolderSeenInternally = sendToServerResult[1] as IChangeAwareValue;

        let changeNotified = false;
        let triggeredSendToServer = false;
        tabHolderSeenInternally.getInternalState().setChangeListener((doNotPushNow?: boolean) => {
            changeNotified = true;
            triggeredSendToServer = !doNotPushNow;
        });
        const checkNotifiedAndTriggeredAndClear = (changeNotifiedWanted: boolean, triggeredSendToServerWanted: boolean) => {
            expect(changeNotified).toBe(changeNotifiedWanted);
            expect(triggeredSendToServer).toBe(triggeredSendToServerWanted);
            changeNotified = false;
            triggeredSendToServer = false;
        };

        let changes: ICOTFullObjectToServer = sendToServerResult[0];
        expect(changes.v).toBeDefined('change object should have a value');
        expect(changes.v.id).toBe('test');
        expect((tabHolderSeenInternally.getInternalState() as BaseCustomObjectState<string, any>).contentVersion)
            .toBe(1, 'it has been sent to server/reset, it should have version 1 to be in sync with server now'); // it used to be NaN at some point

        const tab2Changes: ICOTFullObjectToServer = changes.v.tab2;
        expect(tab2Changes.v).toBeDefined();
        expect(tab2Changes.v.name).toBe('test');
        expect(tab2Changes.v.myvalue).toBe('test');
        expect(tabHolderSeenInternally.getInternalState().hasChanges()).toBe(false, 'should not have changes');
        expect(((tabHolder.tab2 as any) as IChangeAwareValue).getInternalState().hasChanges()).toBe(false, 'should not have changes');

        tabHolder.clientSideOnlyTab.name = 'test2';
        checkNotifiedAndTriggeredAndClear(false, false); // (REJECT pushToServer on subProp) not marked by obj proxy that it has change by ref, and not triggered to be sent automatically
        tabHolder.tab2.name = 'test2';
        checkNotifiedAndTriggeredAndClear(true, false); // (ALLOW pushToServer on subProp) not marked by obj proxy that it has change by ref, and not triggered to be sent automatically
        tabHolder.tab2.myvalue = 'test2';
        checkNotifiedAndTriggeredAndClear(true, true); // (SHALLOW pushToServer on subProp) marked by obj proxy that it has change by ref, and triggered to be sent automatically

        let changesGranular: ICOTGranularUpdatesToServer = converterService.convertFromClientToServer(tabHolder, tabHolderType, tabHolder, getParentPropertyContext(tabHolderPushToServer))[0];

        expect(changesGranular.u).toBeDefined('change object should have an update');
        expect((tabHolderSeenInternally.getInternalState() as BaseCustomObjectState<string, any>).contentVersion)
            .toBe(1, 'update sent, version remains the same');
        expect(changesGranular.u.length).toBe(1, 'only myvalue subprop can be sent to server and is changed');
        expect(changesGranular.u[0].k).toBe('tab2');
        expect(changesGranular.u[0].v.vEr).toBe(1);
        expect(changesGranular.u[0].v.u.length).toBe(2);
        expect(changesGranular.u[0].v.u[0].k).toBe('name');
        expect(changesGranular.u[0].v.u[0].v).toBe('test2');
        expect(changesGranular.u[0].v.u[1].k).toBe('myvalue');
        expect(changesGranular.u[0].v.u[1].v).toBe('test2');

        let changesNoOp: ICOTNoOpToServer = converterService.convertFromClientToServer(tabHolder, tabHolderType, tabHolder, getParentPropertyContext(tabHolderPushToServer))[0] as ICOTNoOpToServer;
        expect(changesNoOp.n).toBe(true, 'should have no changes now');

        const oldTabHolder = tabHolder;
        const oldTab2 = tabHolder.tab2;
        tabHolder = converterService.convertFromServerToClient(
                {
                    u: [{
                        k: 'tab2',
                        v: {
                            u: [{
                                k: 'name',
                                v: 'ZelePuti'
                            }],
                            vEr: 1
                        } as ICOTGranularUpdatesFromServer
                    }],
                    vEr: 1
                } as ICOTGranularUpdatesFromServer,
               tabHolderType , tabHolder, undefined, undefined, getParentPropertyContext(tabHolderPushToServer)) as TabHolder;
        tabHolderSeenInternally = (tabHolder as any) as IChangeAwareValue;
        expect(tabHolder).toBe(oldTabHolder, 'Reference should not be changed; it is a granular update');
        expect(tabHolder.tab2).toBe(oldTab2, 'Reference should not be changed; it is a granular update');
        expect(tabHolder.tab2.name).toBe('ZelePuti', 'Granular update should have changed name in tab2');

        // now add another child and see that sending it and getting it back works ok (it will go through another branch if the new obj. does not have child objs)
        tabHolder.tab3 = new Tab();
        tabHolder.tab3.name = 'test';
        tabHolder.tab3.myvalue = 'test';

        const oldTab3 = tabHolder.tab3;
        expect((tabHolderSeenInternally.getInternalState() as BaseCustomObjectState<string, any>).hasFullyChanged()).toBe(false, 'should not be completely changed');
        sendToServerResult = converterService.convertFromClientToServer(tabHolder, tabHolderType, tabHolder, getParentPropertyContext(tabHolderPushToServer));
        changesGranular = sendToServerResult[0];
        tabHolder = sendToServerResult[1];
        expect(tabHolder).toBe(oldTabHolder, 'Reference should not be changed; it is not a new value and it was already proxied...');
        expect(tabHolder.tab3).not.toBe(oldTab3, 'Reference should be changed because the proxy was added; this is not a wanted thing, but it is needed');
        expect(tabHolder.tab3.name).toBe('test', 'proxy should not affect subprop. values');
        expect(tabHolder.tab3.myvalue).toBe('test', 'proxy should not affect subprop. values');

        expect(changesGranular.u).toBeDefined('should have updates');
        expect(changesGranular.u[0].k).toBe('tab3');
        changes = changesGranular.u[0].v as ICOTFullObjectToServer;
        expect(changes).toBeDefined();
        expect(changes.v).toBeDefined();
        expect(changes.v.name).toBe('test');
        expect(changes.v.myvalue).toBe('test');
        expect(tabHolderSeenInternally.getInternalState().hasChanges()).toBe(false, 'should not have changes');
        const tab3SeenInternally = (tabHolder.tab3 as any) as IChangeAwareValue;
        expect(tab3SeenInternally.getInternalState().hasChanges()).toBe(false, 'should not have changes');

        changesNoOp = converterService.convertFromClientToServer(tabHolder, tabHolderType, tabHolder, getParentPropertyContext(tabHolderPushToServer))[0];
        expect(changesNoOp.n).toBe(true, 'should have no changes now');
    });

    it('nested objects - if somehow marked as allchanged == true should send full values in-depth', () => {
        let tabHolder = new TabHolder();
        tabHolder.id = 'test';
        tabHolder.tab = new Tab();
        tabHolder.tab.name = 'test';
        tabHolder.tab.myvalue = 'test';

        const sendToServerResult = converterService.convertFromClientToServer(tabHolder, tabHolderType, undefined, getParentPropertyContext(tabHolderPushToServer));
        tabHolder = sendToServerResult[1]; // it has been converted into a Proxy of original object
        let tabHolderSeenInternally = sendToServerResult[1] as IChangeAwareValue;

        expect(tabHolderSeenInternally.getInternalState().hasChanges()).toBe(false, 'it should have no more outgoing changes');

        // now make only one prop. change in nested tab
        tabHolder.tab.myvalue = 'test1';
        expect(tabHolderSeenInternally.getInternalState().hasChanges()).toBe(true, 'we just changed myvalue');
        expect((tabHolderSeenInternally.getInternalState() as BaseCustomObjectState<string, any>).hasFullyChanged()).toBe(false, 'we just changed myvalue, not the whole thing');

        // simulate that it should be sent fully
        tabHolderSeenInternally.getInternalState().markAllChanged(false); // could be set at runtime for example by a parent custom object or custom array type when that one wants to be sent fully
        const sendToServerResult1 = converterService.convertFromClientToServer(tabHolder, tabHolderType, undefined,
            getParentPropertyContext(tabHolderPushToServer));
        tabHolder = sendToServerResult1[1];
        tabHolderSeenInternally = sendToServerResult1[1] as IChangeAwareValue;
        const fullChanges = sendToServerResult1[0] as ICOTFullObjectToServer;

        // check that both tabHolder and tab are fully sent (so for tab we don't send just the change to myvalue)
        expect(fullChanges.v).toBeDefined('it should send full value to server');
        expect(fullChanges.v.id).toBe('test');
        const fullTabChanges: ICOTFullObjectToServer = fullChanges.v.tab;
        expect(fullTabChanges.v).toBeDefined();
        expect(fullTabChanges.v.name).toBe('test');
        expect(fullTabChanges.v.myvalue).toBe('test1');
        const tabHholderTabAsSeenInternally = (tabHolder.tab as any) as IChangeAwareValue;
        expect(tabHolderSeenInternally.getInternalState().hasChanges()).toBe(false, 'should not have changes');
        expect(tabHholderTabAsSeenInternally.getInternalState().hasChanges()).toBe(false, 'should not have changes');
        expect((tabHolderSeenInternally.getInternalState() as BaseCustomObjectState<string, any>).hasFullyChanged()).toBe(false, 'should not have changes');
        expect((tabHholderTabAsSeenInternally.getInternalState() as BaseCustomObjectState<string, any>).hasFullyChanged()).toBe(false, 'should not have changes');
    });

    it('nested object from server', () => {
        const tabHolder: TabHolder = converterService.convertFromServerToClient({
            v: { id: 'test', tab2: createTabJSON() },
            vEr: 1
        } as ICOTFullValueFromServer, tabHolderType , undefined, undefined, undefined, getParentPropertyContext(tabHolderPushToServer)) as TabHolder;
        const tabHolderAsSeenInternally = (tabHolder as any) as IChangeAwareValue;
        tabHolderAsSeenInternally.getInternalState().setChangeListener(() => {});
        const tab2AsSeenInternally = (tabHolder.tab2 as any) as IChangeAwareValue;

        expect(tabHolderAsSeenInternally.getInternalState().hasChanges()).toBe(false, 'should not have changes');
        expect(tab2AsSeenInternally.getInternalState().hasChanges()).toBe(false, 'should not have changes');
        expect((tabHolderAsSeenInternally.getInternalState() as BaseCustomObjectState<string, any>).hasFullyChanged()).toBe(false, 'should not have changes');
        expect((tab2AsSeenInternally.getInternalState() as BaseCustomObjectState<string, any>).hasFullyChanged()).toBe(false, 'should not have changes');

        tabHolder.tab2.name = 'test2';
        expect(tabHolderAsSeenInternally.getInternalState().hasChanges()).toBe(true, 'should not have changes'); // knows it has changes but will not push directly to server
        tabHolder.tab2.myvalue = 'test2';
        expect(tabHolderAsSeenInternally.getInternalState().hasChanges()).toBe(true, 'should have changes');

        const sendToServerResult = converterService.convertFromClientToServer(tabHolder, tabHolderType, tabHolder, getParentPropertyContext(tabHolderPushToServer));
        const changesGranular: ICOTGranularUpdatesToServer = sendToServerResult[0];
        expect(sendToServerResult[1]).toBe(tabHolder, 'it was already proxied when comming from server, the ref should remain the same here');
        expect(changesGranular.u).toBeDefined('change object  should have an update value');
        expect(changesGranular.u.length).toBe(1, 'should have 1 update');
        const granularUpdateOp = changesGranular.u[0] as ICOTGranularOpToServer;
        expect(granularUpdateOp.k).toBe('tab2');
        const granularTabUpdate = granularUpdateOp.v as ICOTGranularUpdatesToServer;
        expect(granularTabUpdate.u).toBeDefined('change object  should have an update value');
        expect(granularTabUpdate.u.length).toBe(2, 'should have 1 update');

        expect(granularTabUpdate.u[0].k).toBe('name');
        expect(granularTabUpdate.u[0].v).toBe('test2');
        expect(granularTabUpdate.u[1].k).toBe('myvalue');
        expect(granularTabUpdate.u[1].v).toBe('test2');
    });

    it('nested object has 2 props of the same type, both with values, then the sub-values change the property to which they are assigned', () => {
        let tabHolder = new TabHolder();
        tabHolder.id = 'test';
        tabHolder.tab3 = new Tab();
        tabHolder.tab3.name = 'tab3Name';
        tabHolder.tab3.myvalue = 'tab3MyValue';
        tabHolder.tab2 = new Tab();
        tabHolder.tab2.name = 'tab2Name';
        tabHolder.tab2.myvalue = 'tab2MyValue';

        const sendToServerResult = converterService.convertFromClientToServer(tabHolder, tabHolderType, undefined, getParentPropertyContext(tabHolderPushToServer));
        tabHolder = sendToServerResult[1]; // it has been converted into a Proxy of original object
        const tabHolderSeenInternally = sendToServerResult[1] as IChangeAwareValue;
        tabHolderSeenInternally.getInternalState().setChangeListener(() => {});

        const tab2SeenInternally = (tabHolder.tab2 as any) as IChangeAwareValue;
        let tab3SeenInternally = (tabHolder.tab3 as any) as IChangeAwareValue;

        expect(tabHolderSeenInternally.getInternalState().hasChanges()).toBe(false, 'it should have no more outgoing changes');
        expect(tab2SeenInternally.getInternalState().hasChanges()).toBe(false, 'no change expected');
        expect(tab3SeenInternally.getInternalState().hasChanges()).toBe(false, 'no change expected');
        expect((tab2SeenInternally.getInternalState() as BaseCustomObjectState<string, any>).hasFullyChanged()).toBe(false, 'no change expected');
        expect((tab3SeenInternally.getInternalState() as BaseCustomObjectState<string, any>).hasFullyChanged()).toBe(false, 'no change expected');
        expect((tabHolderSeenInternally.getInternalState() as BaseCustomObjectState<string, any>).hasFullyChanged()).toBe(false, 'no change expected');

        // simulate that tab3 gets the value of tab2 (so tab3's value is no longer used)
        const obsoleteTab3 = tabHolder.tab3;
        tabHolder.tab3 = tabHolder.tab2;
        tabHolder.tab2 = null;
        const jsonToServer: ICOTGranularUpdatesToServer = converterService.convertFromClientToServer(tabHolder, tabHolderType, tabHolder, getParentPropertyContext(tabHolderPushToServer))[0];

        // check that both tabHolder and tab are fully sent (so for tab we don't send just the change to myvalue)
        expect(jsonToServer.u).toBeDefined('change object  should have an update value');
        expect(jsonToServer.u.length).toBe(2, 'should have 2 updates');
        expect(jsonToServer.u[0].k).toBe('tab3');
        const fullTabToServer = jsonToServer.u[0].v as ICOTFullObjectToServer;
        expect(fullTabToServer.v).toBeDefined('should send full value as it changed by ref');
        expect(fullTabToServer.v.name).toBe('tab2Name', 'should have old tab2\'s name');
        expect(fullTabToServer.v.myvalue).toBe('tab2MyValue', 'should have old tab2\'s myValue');
        expect(jsonToServer.u[1].k).toBe('tab2');
        expect(jsonToServer.u[1].v).toBeNull('tab2 was set to null');

        tab3SeenInternally = (tabHolder.tab3 as any) as IChangeAwareValue;

        expect(tab3SeenInternally.getInternalState().hasChanges()).toBe(false, 'no change expected');
        expect((tab3SeenInternally.getInternalState() as BaseCustomObjectState<string, any>).hasFullyChanged()).toBe(false, 'no change expected');
        expect(tabHolderSeenInternally.getInternalState().hasChanges()).toBe(false, 'no change expected');
        expect((tabHolderSeenInternally.getInternalState() as BaseCustomObjectState<string, any>).hasFullyChanged()).toBe(false, 'no change expected');

        // changing something in old tab3'v value should not trigger any changes as that value is no longer used
        obsoleteTab3.myvalue = 'aha';

        expect(tab3SeenInternally.getInternalState().hasChanges()).toBe(false, 'no change expected');
        expect((tab3SeenInternally.getInternalState() as BaseCustomObjectState<string, any>).hasFullyChanged()).toBe(false, 'no change expected');
        expect(tabHolderSeenInternally.getInternalState().hasChanges()).toBe(false, 'no change expected');
        expect((tabHolderSeenInternally.getInternalState() as BaseCustomObjectState<string, any>).hasFullyChanged()).toBe(false, 'no change expected');
    });

    it( 'test deep change in a custom object\'s "object" subprop. with various PTS pe subprop', () => {
        const val: DumbSubpropsWithVariousPTS = converterService.convertFromServerToClient(createSimpleCustomObjectWithUntypedAndSpecificPTSOnSubprops(),
               untypedObjectALLOWWithVariousSubpropPTSType , undefined, undefined, undefined, getParentPropertyContext(untypedObjectALLOWWithVariousSubpropPTSPushToServer)) as DumbSubpropsWithVariousPTS;

//        subProp = { x: ['bla', someDate], y: 8 }
//        { rejectSubprop: subProp(), allowSubprop: subProp(), shallowSubprop: subProp(), deepSubprop: subProp() }

        expect(val.rejectSubprop.x[1]).toBeInstanceOf(Date); // x should be a Date sent over via default/'object' conversions
        expect(val.rejectSubprop.y).toBe(8, 'simple number in nested \'object\' not received correctly');
        expect(val.allowSubprop.x[1]).toBeInstanceOf(Date); // x should be a Date sent over via default/'object' conversions
        expect(val.allowSubprop.y).toBe(8, 'simple number in nested \'object\' not received correctly');
        expect(val.shallowSubprop.x[1]).toBeInstanceOf(Date); // x should be a Date sent over via default/'object' conversions
        expect(val.shallowSubprop.y).toBe(8, 'simple number in nested \'object\' not received correctly');
        expect(val.deepSubprop.x[1]).toBeInstanceOf(Date); // x should be a Date sent over via default/'object' conversions
        expect(val.deepSubprop.y).toBe(8, 'simple number in nested \'object\' not received correctly');

        const valSeenInternally = (val as any) as IChangeAwareValue;

        let changeNotified = false;
        let triggeredSendToServer = false;
        valSeenInternally.getInternalState().setChangeListener((doNotPushNow?: boolean) => {
            changeNotified = true;
            triggeredSendToServer = !doNotPushNow;
        });
        const checkNotifiedAndTriggeredAndClear = (changeNotifiedWanted: boolean, triggeredSendToServerWanted: boolean) => {
            expect(changeNotified).toBe(changeNotifiedWanted);
            expect(triggeredSendToServer).toBe(triggeredSendToServerWanted);
            changeNotified = false;
            triggeredSendToServer = false;
        };


        val.rejectSubprop = 'changedByRefOnRejectPTS';
        checkNotifiedAndTriggeredAndClear(false, false); // marked by object proxy that it has no change by ref, and not triggered automatically as it is REJECT pushToServer on subprop

        let changes2: ICOTNoOpToServer = converterService.convertFromClientToServer(val, untypedObjectALLOWWithVariousSubpropPTSType, val,
            getParentPropertyContext(untypedObjectALLOWWithVariousSubpropPTSPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');

        val.allowSubprop = 'changedByRefOnALLOWPTS';
        checkNotifiedAndTriggeredAndClear(true, false); // marked by object proxy that it has change by ref, but not triggered automatically as it is ALLOW pushToServer on subprop

        let changes: ICOTGranularUpdatesToServer = converterService.convertFromClientToServer(val, untypedObjectALLOWWithVariousSubpropPTSType, val,
            getParentPropertyContext(untypedObjectALLOWWithVariousSubpropPTSPushToServer))[0];

        expect(changes.vEr).toBe(1);
        expect(changes.u).toBeDefined('change object should have updates');
        expect(changes.u.length).toBe(1, 'should have 1 update');
        expect(changes.u[0].k).toBe('allowSubprop');
        expect(changes.u[0].v).toBe('changedByRefOnALLOWPTS');

        val.shallowSubprop.y = 15;
        checkNotifiedAndTriggeredAndClear(false, false); // DEEP change in element - in untyped object; we do not automatically detect those
        changes2 = converterService.convertFromClientToServer(val, untypedObjectALLOWWithVariousSubpropPTSType, val,
            getParentPropertyContext(untypedObjectALLOWWithVariousSubpropPTSPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');

        val.markSubPropertyAsHavingDeepChanges('shallowSubprop');
        checkNotifiedAndTriggeredAndClear(true, false); // DEEP change in element that was marked manually; but as it is SHALLOW it does no send it right away to server
        changes = converterService.convertFromClientToServer(val, untypedObjectALLOWWithVariousSubpropPTSType, val,
            getParentPropertyContext(untypedObjectALLOWWithVariousSubpropPTSPushToServer))[0];

        expect(changes.vEr).toBe(1);
        expect(changes.u).toBeDefined('change object should have updates');
        expect(changes.u.length).toBe(1, 'should have 1 update');
        expect(changes.u[0].k).toBe('shallowSubprop');
        expect(changes.u[0].v.y).toBe(15, 'deep change happened here');

        val.deepSubprop.y = 55;
        checkNotifiedAndTriggeredAndClear(false, false); // DEEP change in element - in untyped object; we do not automatically detect those
        changes2 = converterService.convertFromClientToServer(val, untypedObjectALLOWWithVariousSubpropPTSType, val,
            getParentPropertyContext(untypedObjectALLOWWithVariousSubpropPTSPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');

        val.markSubPropertyAsHavingDeepChanges('deepSubprop');
        checkNotifiedAndTriggeredAndClear(true, true); // DEEP change in element that was marked manually; but as it is DEEP it does also send it right away to server
        changes = converterService.convertFromClientToServer(val, untypedObjectALLOWWithVariousSubpropPTSType, val,
            getParentPropertyContext(untypedObjectALLOWWithVariousSubpropPTSPushToServer))[0];

        expect(changes.vEr).toBe(1);
        expect(changes.u).toBeDefined('change object should have updates');
        expect(changes.u.length).toBe(1, 'should have 1 update');
        expect(changes.u[0].k).toBe('deepSubprop');
        expect(changes.u[0].v.y).toBe(55, 'deep change happened here');

        changes2 = converterService.convertFromClientToServer(val, untypedObjectALLOWWithVariousSubpropPTSType, val,
            getParentPropertyContext(untypedObjectALLOWWithVariousSubpropPTSPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');
    } );

    it( 'test deep change in a custom object with REJECT "object" subprops', () => {
        const val: DumbSubpropsWithInheritedPTS = converterService.convertFromServerToClient(createSimpleCustomObjectWithUntypedSubprops(),
               untypedObjectWithREJECTOnSubpropType , undefined, undefined, undefined, getParentPropertyContext(untypedObjectWithREJECTOnSubpropPushToServer)) as DumbSubpropsWithInheritedPTS;

        expect(val.a).toBe('test1');
        expect(val.b.test2).toBe(1);
        expect(val.b.a).toBe(true);
        expect(val.c).toBe(5);
        expect(val.d.length).toBe(3);
        expect(val.d[0]).toBe(1);
        expect(val.d[1]).toBe(2);
        expect(val.d[2]).toBe(3);
        expect(val.b.test2).toBe(1);
        expect(val.e.x[1] instanceof Date).toBeTruthy();
        expect(val.e.y).toBe(8);
        expect(val.f).toBeTruthy();

        const valSeenInternally = (val as any) as IChangeAwareValue;

        let changeNotified = false;
        let triggeredSendToServer = false;
        valSeenInternally.getInternalState().setChangeListener((doNotPushNow?: boolean) => {
            changeNotified = true;
            triggeredSendToServer = !doNotPushNow;
        });
        const checkNotifiedAndTriggeredAndClear = (changeNotifiedWanted: boolean, triggeredSendToServerWanted: boolean) => {
            expect(changeNotified).toBe(changeNotifiedWanted);
            expect(triggeredSendToServer).toBe(triggeredSendToServerWanted);
            changeNotified = false;
            triggeredSendToServer = false;
        };

        val.a = 'changedByRef';
        checkNotifiedAndTriggeredAndClear(false, false); // no array proxy; it is REJECT; so it doesn't care about changes

        let changes2: ICOTNoOpToServer = converterService.convertFromClientToServer(val, untypedObjectWithREJECTOnSubpropType, val,
            getParentPropertyContext(untypedObjectWithREJECTOnSubpropPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');

        val.b.a = false;
        checkNotifiedAndTriggeredAndClear(false, false); // no array proxy; it is REJECT; so it doesn't care about changes

        val.markSubPropertyAsHavingDeepChanges('b');
        checkNotifiedAndTriggeredAndClear(false, false); // it should be ignored; it is REJECT; so it doesn't care about changes
        changes2 = converterService.convertFromClientToServer(val, untypedObjectWithREJECTOnSubpropType, val,
            getParentPropertyContext(untypedObjectWithREJECTOnSubpropPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');
    } );

    it( 'test deep change in a custom object with ALLOW "object" subprops', () => {
        const val: DumbSubpropsWithInheritedPTS = converterService.convertFromServerToClient(createSimpleCustomObjectWithUntypedSubprops(),
               untypedObjectWithALLOWOnSubpropType , undefined, undefined, undefined, getParentPropertyContext(untypedObjectWithALLOWOnSubpropPushToServer)) as DumbSubpropsWithInheritedPTS;

        const valSeenInternally = (val as any) as IChangeAwareValue;

        let changeNotified = false;
        let triggeredSendToServer = false;
        valSeenInternally.getInternalState().setChangeListener((doNotPushNow?: boolean) => {
            changeNotified = true;
            triggeredSendToServer = !doNotPushNow;
        });
        const checkNotifiedAndTriggeredAndClear = (changeNotifiedWanted: boolean, triggeredSendToServerWanted: boolean) => {
            expect(changeNotified).toBe(changeNotifiedWanted);
            expect(triggeredSendToServer).toBe(triggeredSendToServerWanted);
            changeNotified = false;
            triggeredSendToServer = false;
        };

        val.a = 'changedByRef';
        checkNotifiedAndTriggeredAndClear(true, false); // marked by array proxy that it has change by ref, but not triggered automatically as it is only ALLOW pushToServer

        let changes: ICOTGranularUpdatesToServer = converterService.convertFromClientToServer(val, untypedObjectWithALLOWOnSubpropType, val,
            getParentPropertyContext(untypedObjectWithALLOWOnSubpropPushToServer))[0];

        expect(changes.vEr).toBe(1);
        expect(changes.u).toBeDefined('change object should have updates');
        expect(changes.u.length).toBe(1, 'should have 1 update');
        expect(changes.u[0].k).toBe('a');
        expect(changes.u[0].v).toBe('changedByRef');


        val.b.a = false;
        checkNotifiedAndTriggeredAndClear(false, false); // DEEP change in element - in untyped object; we do not automatically detect those
        let changes2: ICOTNoOpToServer = converterService.convertFromClientToServer(val, untypedObjectWithALLOWOnSubpropType, val,
            getParentPropertyContext(untypedObjectWithALLOWOnSubpropPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');

        val.markSubPropertyAsHavingDeepChanges('b');
        checkNotifiedAndTriggeredAndClear(true, false); // DEEP change in element that was marked manually; but as it is ALLOW it does no send it right away to server
        changes = converterService.convertFromClientToServer(val, untypedObjectWithALLOWOnSubpropType, val,
            getParentPropertyContext(untypedObjectWithALLOWOnSubpropPushToServer))[0];

        expect(changes.vEr).toBe(1);
        expect(changes.u).toBeDefined('change object should have updates');
        expect(changes.u.length).toBe(1, 'should have 1 update');
        expect(changes.u[0].k).toBe('b');
        expect(changes.u[0].v.a).toBe(false, 'should be changed');
        expect(changes.u[0].v.test2).toBe(1, 'should be unchanged');


        changes2 = converterService.convertFromClientToServer(val, untypedObjectWithALLOWOnSubpropType, val,
            getParentPropertyContext(untypedObjectWithALLOWOnSubpropPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');
    } );


    it( 'test deep change in a custom object with SHALLOW "object" subprops', () => {
        const val: DumbSubpropsWithInheritedPTS = converterService.convertFromServerToClient(createSimpleCustomObjectWithUntypedSubprops(),
               untypedObjectWithSHALLOWOnSubpropType , undefined, undefined, undefined, getParentPropertyContext(untypedObjectWithSHALLOWOnSubpropPushToServer)) as DumbSubpropsWithInheritedPTS;

        const valSeenInternally = (val as any) as IChangeAwareValue;

        let changeNotified = false;
        let triggeredSendToServer = false;
        valSeenInternally.getInternalState().setChangeListener((doNotPushNow?: boolean) => {
            changeNotified = true;
            triggeredSendToServer = !doNotPushNow;
        });
        const checkNotifiedAndTriggeredAndClear = (changeNotifiedWanted: boolean, triggeredSendToServerWanted: boolean) => {
            expect(changeNotified).toBe(changeNotifiedWanted);
            expect(triggeredSendToServer).toBe(triggeredSendToServerWanted);
            changeNotified = false;
            triggeredSendToServer = false;
        };

        val.a = 'changedByRef';
        checkNotifiedAndTriggeredAndClear(true, true); // marked by array proxy that it has change by ref, and triggered automatically as it has SHALLOW pushToServer

        let changes: ICOTGranularUpdatesToServer = converterService.convertFromClientToServer(val, untypedObjectWithSHALLOWOnSubpropType, val,
            getParentPropertyContext(untypedObjectWithSHALLOWOnSubpropPushToServer))[0];

        expect(changes.vEr).toBe(1);
        expect(changes.u).toBeDefined('change object should have updates');
        expect(changes.u.length).toBe(1, 'should have 1 update');
        expect(changes.u[0].k).toBe('a');
        expect(changes.u[0].v).toBe('changedByRef');


        val.b.a = false;
        checkNotifiedAndTriggeredAndClear(false, false); // DEEP change in element - in untyped object; we do not automatically detect those
        let changes2: ICOTNoOpToServer = converterService.convertFromClientToServer(val, untypedObjectWithSHALLOWOnSubpropType, val,
            getParentPropertyContext(untypedObjectWithSHALLOWOnSubpropPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');

        val.markSubPropertyAsHavingDeepChanges('b');
        checkNotifiedAndTriggeredAndClear(true, false); // DEEP change in element that was marked manually; but as it is SHALLOW it does no send it right away to server
        changes = converterService.convertFromClientToServer(val, untypedObjectWithSHALLOWOnSubpropType, val,
            getParentPropertyContext(untypedObjectWithSHALLOWOnSubpropPushToServer))[0];

        expect(changes.vEr).toBe(1);
        expect(changes.u).toBeDefined('change object should have updates');
        expect(changes.u.length).toBe(1, 'should have 1 update');
        expect(changes.u[0].k).toBe('b');
        expect(changes.u[0].v.a).toBe(false, 'should be changed');
        expect(changes.u[0].v.test2).toBe(1, 'should be unchanged');


        changes2 = converterService.convertFromClientToServer(val, untypedObjectWithSHALLOWOnSubpropType, val,
            getParentPropertyContext(untypedObjectWithSHALLOWOnSubpropPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');
    } );

    it( 'test deep change in a custom object with DEEP "object" subprops', () => {
        const val: DumbSubpropsWithInheritedPTS = converterService.convertFromServerToClient(createSimpleCustomObjectWithUntypedSubprops(),
               untypedObjectWithDEEPOnSubpropType , undefined, undefined, undefined, getParentPropertyContext(untypedObjectWithDEEPOnSubpropPushToServer)) as DumbSubpropsWithInheritedPTS;

        const valSeenInternally = (val as any) as IChangeAwareValue;

        let changeNotified = false;
        let triggeredSendToServer = false;
        valSeenInternally.getInternalState().setChangeListener((doNotPushNow?: boolean) => {
            changeNotified = true;
            triggeredSendToServer = !doNotPushNow;
        });
        const checkNotifiedAndTriggeredAndClear = (changeNotifiedWanted: boolean, triggeredSendToServerWanted: boolean) => {
            expect(changeNotified).toBe(changeNotifiedWanted);
            expect(triggeredSendToServer).toBe(triggeredSendToServerWanted);
            changeNotified = false;
            triggeredSendToServer = false;
        };

        val.a = 'changedByRef';
        checkNotifiedAndTriggeredAndClear(true, true); // marked by array proxy that it has change by ref, and triggered automatically as it has DEEP pushToServer

        let changes: ICOTGranularUpdatesToServer = converterService.convertFromClientToServer(val, untypedObjectWithDEEPOnSubpropType, val,
            getParentPropertyContext(untypedObjectWithDEEPOnSubpropPushToServer))[0];

        expect(changes.vEr).toBe(1);
        expect(changes.u).toBeDefined('change object should have updates');
        expect(changes.u.length).toBe(1, 'should have 1 update');
        expect(changes.u[0].k).toBe('a');
        expect(changes.u[0].v).toBe('changedByRef');


        val.b.a = false;
        checkNotifiedAndTriggeredAndClear(false, false); // DEEP change in element - in untyped object; we do not automatically detect those
        let changes2: ICOTNoOpToServer = converterService.convertFromClientToServer(val, untypedObjectWithDEEPOnSubpropType, val,
            getParentPropertyContext(untypedObjectWithDEEPOnSubpropPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');

        val.markSubPropertyAsHavingDeepChanges('b');
        checkNotifiedAndTriggeredAndClear(true, true); // DEEP change in element that was marked manually; but as it is DEEP it does send it right away to server
        changes = converterService.convertFromClientToServer(val, untypedObjectWithDEEPOnSubpropType, val,
            getParentPropertyContext(untypedObjectWithDEEPOnSubpropPushToServer))[0];

        expect(changes.vEr).toBe(1);
        expect(changes.u).toBeDefined('change object should have updates');
        expect(changes.u.length).toBe(1, 'should have 1 update');
        expect(changes.u[0].k).toBe('b');
        expect(changes.u[0].v.a).toBe(false, 'should be changed');
        expect(changes.u[0].v.test2).toBe(1, 'should be unchanged');


        changes2 = converterService.convertFromClientToServer(val, untypedObjectWithDEEPOnSubpropType, val,
            getParentPropertyContext(untypedObjectWithDEEPOnSubpropPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');
    } );

    it('legacy TabDeprecated created from server with correct instance, with old api available', () => {
        specTypesService.registerType("Tab", TabDeprecated);
        
        const val = converterService.convertFromServerToClient(createTabJSON(),
               oneTabType , undefined, undefined, undefined, getParentPropertyContext(oneTabPushToServer));

        const tabAsSeenInternally = val as IChangeAwareValue;
        const tab = val as TabDeprecated;
        
        for (const k in tab)
            if (k === "constructor" || k === "get2ConcattedProps") fail("'" + k + "' should not be an enumerable property!");
        
        expect(tab).toBeDefined();
        expect(tab.name).toBe('test', 'name should be test');
        expect(tab.myvalue).toBe('test', 'myvalue should be test');

        expect(tabAsSeenInternally.getInternalState().hasChanges()).toBe(false, 'should not have changes');

        expect(tab.getWatchedProperties).toBeTruthy(); // deprecated stuff that does nothing - it just has to not err. out
        expect(tab.getStateHolder().getChangedKeys().add('isCollapsed')).toBeTruthy(); // deprecated stuff that does nothing - it just has to not err. out
        expect(tab.getStateHolder().markAllChanged).toBeTruthy(); // deprecated stuff that does nothing - it just has to not err. out
        tab.getStateHolder().setPropertyAndHandleChanges(tab, "myvalue", "myvalue", "test101");
        expect(tab.myvalue).toBe('test101', 'myvalue should be test101');

        expect(tabAsSeenInternally.getInternalState().hasChanges()).toBe(true, 'should have changes');
        
        expect(tab instanceof TabDeprecated).toBeTrue();
        expect(tab instanceof BaseCustomObject).toBeTrue();
        
        const tabNewImpl = val as ICustomObjectValue;
        expect(tabNewImpl.markSubPropertyAsHavingDeepChanges).toBeTruthy();
        
        expect(tab.get2ConcattedProps()).toEqual("test - test101");
        specTypesService.registerType("Tab", undefined);
    });

    it('legacy TabDeprecated that was not registered with specTypesService, created from server should include deprecated BaseCustomObject, with old api available', () => {
        const val = converterService.convertFromServerToClient(createTabJSON(),
               oneTabType , undefined, undefined, undefined, getParentPropertyContext(oneTabPushToServer));

        const tabAsSeenInternally = val as IChangeAwareValue;
        const tab = val as TabDeprecated;
        
        expect(tab).toBeDefined();
        expect(tab.name).toBe('test', 'name should be test');
        expect(tab.myvalue).toBe('test', 'myvalue should be test');

        expect(tabAsSeenInternally.getInternalState().hasChanges()).toBe(false, 'should not have changes');

        expect(tab.getWatchedProperties).toBeTruthy(); // deprecated stuff that does nothing - it just has to not err. out
        expect(tab.getStateHolder().getChangedKeys().add('isCollapsed')).toBeTruthy(); // deprecated stuff that does nothing - it just has to not err. out
        expect(tab.getStateHolder().markAllChanged).toBeTruthy(); // deprecated stuff that does nothing - it just has to not err. out
        tab.getStateHolder().setPropertyAndHandleChanges(tab, "myvalue", "myvalue", "test101");
        expect(tab.myvalue).toBe('test101', 'myvalue should be test101');

        expect(tabAsSeenInternally.getInternalState().hasChanges()).toBe(true, 'should have changes');
        
        expect(tab instanceof TabDeprecated).toBeFalse();
        expect(tab instanceof BaseCustomObject).toBeTrue();
        
        const tabNewImpl = val as ICustomObjectValue;
        expect(tabNewImpl.markSubPropertyAsHavingDeepChanges).toBeTruthy();
        
        expect(tab.get2ConcattedProps).toBeUndefined();
    });

    it('legacy TabDeprecated created from client with correct instance, should get new impl. as well after send to server', () => {
        specTypesService.registerType("Tab", TabDeprecated);
        let tab = new TabDeprecated();
        
        tab.name = 'test';
        tab.myvalue = 'test';

        [, tab] = converterService.convertFromClientToServer(tab, oneTabType, undefined,
            getParentPropertyContext(oneTabPushToServer));
        
        expect(tab instanceof TabDeprecated).toBeTrue();
        expect(tab instanceof BaseCustomObject).toBeTrue();

        const tabNewImpl = tab as ICustomObjectValue;
        expect(tabNewImpl.markSubPropertyAsHavingDeepChanges).toBeTruthy();
        expect(tab.get2ConcattedProps()).toEqual("test - test");
        specTypesService.registerType("Tab", undefined);
    });

    it('new Tab created from server with correct instance, with new api available but not old api', () => {
        specTypesService.registerCustomObjectType("Tab", Tab);
        
        const val = converterService.convertFromServerToClient(createTabJSON(),
               oneTabType , undefined, undefined, undefined, getParentPropertyContext(oneTabPushToServer));

        const tabAsSeenInternally = val as IChangeAwareValue;
        const tab = val as Tab;
        
        expect(tab).toBeDefined();
        expect(tab.name).toBe('test', 'name should be test');
        expect(tab.myvalue).toBe('test', 'myvalue should be test');

        expect(tabAsSeenInternally.getInternalState().hasChanges()).toBe(false, 'should not have changes');

        expect(tab instanceof BaseCustomObject).toBeFalse(); // Tab is the new approch; doesn't need old/legacy API
        expect(tab['getWatchedProperties']).toBeUndefined();
        expect(tab['getStateHolder']).toBeUndefined();
        
        
        const tabNewImpl = tab as ICustomObjectValue;
        expect(tabNewImpl.markSubPropertyAsHavingDeepChanges).toBeTruthy();

        tabNewImpl.markSubPropertyAsHavingDeepChanges("myvalue"); // we are faking it - actually it doesn't have any changes
        expect(tabAsSeenInternally.getInternalState().hasChanges()).toBe(true, 'should have changes');

        expect(tab.get2ConcattedProps()).toEqual("test - test");
        specTypesService.registerCustomObjectType("Tab", undefined);
    });

    it('new Tab created from client with correct instance, should get new impl. as well after send to server', () => {
        specTypesService.registerCustomObjectType("Tab", Tab);
        let tab = new Tab();
        
        tab.name = 'test';
        tab.myvalue = 'test';

        [, tab] = converterService.convertFromClientToServer(tab, oneTabType, undefined,
            getParentPropertyContext(oneTabPushToServer));
        
        expect(tab instanceof Tab).toBeTrue();
        expect(tab instanceof BaseCustomObject).toBeFalse();

        const tabNewImpl = tab as ICustomObjectValue;
        expect(tabNewImpl.markSubPropertyAsHavingDeepChanges).toBeTruthy();
        expect(tab.get2ConcattedProps()).toEqual("test - test");
        specTypesService.registerCustomObjectType("Tab", undefined);
    });

    it( 'when an already smart value (received as return value from an server side api call for example) is assigned into the model into a new location and sent to server, it should still work - have a correct change listener etc.', () => {
        // in model
        const val = converterService.convertFromServerToClient({
            v: { id: 'test', tab2: createTabJSON() },
            vEr: 1
        } as ICOTFullValueFromServer, tabHolderType , undefined, undefined, undefined, getParentPropertyContext(tabHolderPushToServer));
        
        let tabHolder = val as TabHolder;
        let changeListenerWasCalled = false;
        (val as IChangeAwareValue).getInternalState().setChangeListener(() => { changeListenerWasCalled = true; });

        expect(changeListenerWasCalled).toBeFalse();

        // received as return value from a server side api call
        const childArray = converterService.convertFromServerToClient({ v: [ createTabJSON() ], vEr: 1},
               tabArrayType , undefined, undefined, undefined, PushToServerUtils.PROPERTY_CONTEXT_FOR_INCOMMING_ARGS_AND_RETURN_VALUES) as Tab[];
        
        expect(((childArray as any) as IChangeAwareValue).getInternalState().hasChangeListener()).toBeFalse();
        expect(((childArray[0] as any) as IChangeAwareValue).getInternalState().hasChangeListener()).toBeTrue();

        // assign it to model val's subproperty
        tabHolder.tabs = childArray;

        expect(changeListenerWasCalled).toBeTrue();
        changeListenerWasCalled = false;

        expect(((childArray as any) as IChangeAwareValue).getInternalState().hasChangeListener()).toBeTrue();
        expect(((childArray[0] as any) as IChangeAwareValue).getInternalState().hasChangeListener()).toBeTrue();

        // simulate a send to server as argument to a handler for this array (oldVal undefined) - to make sure it doesn't messup it's state if it's also a model prop. (it used getParentPropertyContext above which is for a model prop)
        const changesAndVal: [ICOTGranularUpdatesToServer, any] = converterService.convertFromClientToServer(tabHolder, tabHolderType, tabHolder,
             getParentPropertyContext(tabHolderPushToServer));
        let changes = changesAndVal[0];
        
        tabHolder = changesAndVal[1] as TabHolder;
        let tabHolderAsSeenInternally = changesAndVal[1] as IChangeAwareValue;
        
        expect(tabHolderAsSeenInternally.getInternalState().hasChangeListener()).toBeTrue();
        expect(((tabHolder.tabs as any) as IChangeAwareValue).getInternalState().hasChangeListener()).toBeTrue();
        expect(((tabHolder.tabs[0] as any) as IChangeAwareValue).getInternalState().hasChangeListener()).toBeTrue();
        
        expect(changes.vEr).toBe(1);
        expect(changes.u.length).toBe(1);
        expect(changes.u[0].k).toBe('tabs');
        const tabsChanges = changes.u[0].v as ICATFullArrayToServer;
        expect(tabsChanges.vEr).toBe(0);
        const changes1 = tabsChanges.v[0] as ICOTFullValueFromServer;
        expect(changes1.vEr).toBe(0);
        expect(changes1.v).toEqual({ name: 'test', myvalue: 'test' });

        tabHolder.tabs[0].myvalue = 'test42';

        expect(changeListenerWasCalled).toBe(true);

        const changes2: ICOTGranularUpdatesToServer = converterService.convertFromClientToServer(tabHolder, tabHolderType, tabHolder,
            getParentPropertyContext(tabHolderPushToServer))[0];
        
        expect(changes2.vEr).toBe(1);
        expect(changes2.u.length).toBe(1);
        expect(changes2.u[0].k).toBe('tabs'); 
        const tabsUpdate = changes2.u[0].v as ICATGranularUpdatesToServer;
        expect(tabsUpdate.vEr).toBe(1);
        expect(tabsUpdate.u.length).toBe(1);
        expect(tabsUpdate.u[0].i).toBe(0);
        const tabChanges = tabsUpdate.u[0].v as ICOTGranularUpdatesToServer;
        expect(tabChanges.vEr).toBe(1);
        expect(tabChanges.u.length).toBe(1);
        expect(tabChanges.u[0].k).toBe('myvalue');
        expect(tabChanges.u[0].v).toEqual('test42');
    } );
    
});

class Tab implements ICustomObjectValue {

    name: string;
    myvalue: string;
    rejectString: string;

    get2ConcattedProps() {
        return this.name + " - " + this.myvalue;
    }

}

class TabDeprecated extends BaseCustomObject { // test deprecated scenario as well

    name: string;
    myvalue: string;
    rejectString: string;

    get2ConcattedProps() {
        return this.name + " - " + this.myvalue;
    }

}

class TabHolder {

    id: string;
    tab: Tab;
    tab2: Tab;
    tab3: Tab;
    tabs: Tab[];
    clientSideOnlyTab: Tab;

}

interface DumbSubpropsWithVariousPTS extends ICustomObjectValue {

    rejectSubprop: any;
    allowSubprop: any;
    shallowSubprop: any;
    deepSubprop: any;

}

interface DumbSubpropsWithInheritedPTS extends ICustomObjectValue {

    a: any;
    b: any;
    c: any;
    d: any;
    e: any;
    f: any;

}
