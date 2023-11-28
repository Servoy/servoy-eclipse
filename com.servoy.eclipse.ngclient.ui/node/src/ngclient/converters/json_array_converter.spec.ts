import { TestBed } from '@angular/core/testing';
import { TypesRegistry, ICustomTypesFromServer, IPropertiesFromServer, IPropertyDescriptionFromServerWithMultipleEntries, ITypeFromServer,
            IFactoryTypeDetails, IPropertyContext, PushToServerEnum, PushToServerUtils } from '../../sablo/types_registry';

import { ConverterService, IChangeAwareValue } from '../../sablo/converter.service';
import { LoggerFactory, WindowRefService, ICustomArrayValue, SpecTypesService } from '@servoy/public';

import { CustomArrayTypeFactory, CustomArrayType, ICATFullValueFromServer, ICATGranularUpdatesToServer, ICATNoOpToServer,
            ICATFullArrayToServer, ICATGranularUpdatesFromServer, ICATOpTypeEnum } from './json_array_converter';
import { CustomObjectTypeFactory, CustomObjectType, ICOTFullValueFromServer, ICOTGranularUpdatesToServer,
            ICOTFullObjectToServer, ICOTNoOpToServer, ICOTGranularUpdatesFromServer } from './json_object_converter';
import { DateType } from '../../sablo/converters/date_converter';
import { ObjectType } from '../../sablo/converters/object_converter';

describe( 'JSONArrayConverter', () => {
    let converterService: ConverterService<any>;
    let loggerFactory: LoggerFactory;
    let typesRegistry: TypesRegistry;
    let specTypesService: SpecTypesService;

    let stringArrayType: CustomArrayType<string>;
    let stringArrayPushToServer: PushToServerEnum;
    let tabArrayWithShallowOnElementsType: CustomArrayType<Tab>;
    let tabArrayWithShallowOnElementsPushToServer: PushToServerEnum;
    let stringArrayWithShallowOnElementsType: CustomArrayType<string>;
    let stringArrayWithShallowOnElementsPushToServer: PushToServerEnum;
    let tabHolderElementsType: CustomObjectType;
    let tabHolderElementsPushToServer: PushToServerEnum;
    let untypedObjectArrayWithREJECTOnElementsType: CustomArrayType<any>;
    let untypedObjectArrayWithREJECTOnElementsPushToServer: PushToServerEnum;
    let untypedObjectArrayWithALLOWOnElementsType: CustomArrayType<any>;
    let untypedObjectArrayWithALLOWOnElementsPushToServer: PushToServerEnum;
    let untypedObjectArrayWithSHALLOWOnElementsType: CustomArrayType<any>;
    let untypedObjectArrayWithSHALLOWOnElementsPushToServer: PushToServerEnum;
    let untypedObjectArrayWithDEEPOnElementsType: CustomArrayType<any>;
    let untypedObjectArrayWithDEEPOnElementsPushToServer: PushToServerEnum;
    let tabJustForTypeType: CustomObjectType;

    const getParentPropertyContext = (pushToServerCalculatedValueForProp: PushToServerEnum): IPropertyContext => ({
            getProperty: (_propertyName: string) => undefined,
            getPushToServerCalculatedValue: () => pushToServerCalculatedValueForProp,
            isInsideModel: true
        });

    beforeEach(() => {
        TestBed.configureTestingModule( {
            providers: [ConverterService, LoggerFactory, WindowRefService, SpecTypesService]
        } );
        converterService = TestBed.inject( ConverterService );
        loggerFactory = TestBed.inject(LoggerFactory);
        typesRegistry = TestBed.inject(TypesRegistry);
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
                    myvalue: { s: 2 } as IPropertyDescriptionFromServerWithMultipleEntries
                } as IPropertiesFromServer,
                TabHolder: {
                     tabs: { t: [CustomArrayTypeFactory.TYPE_FACTORY_NAME, { t: [CustomObjectTypeFactory.TYPE_FACTORY_NAME, 'Tab'], s: 2}], s: 2 } as IPropertyDescriptionFromServerWithMultipleEntries
                } as IPropertiesFromServer,
            } as ICustomTypesFromServer;

        typesRegistry.addComponentClientSideSpecs({
            someTabPanelSpec: {
                p: {
                    stringArray: { t: [CustomArrayTypeFactory.TYPE_FACTORY_NAME, null] as ITypeFromServer, s: 1 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    stringArrayWithShallowOnElements: { t: [CustomArrayTypeFactory.TYPE_FACTORY_NAME, { t: null, s: 2 }
                            ] as ITypeFromServer, s: 1 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    tabArray: { t: [CustomArrayTypeFactory.TYPE_FACTORY_NAME, [CustomObjectTypeFactory.TYPE_FACTORY_NAME, 'Tab']] as ITypeFromServer, s: 1 } as
                        IPropertyDescriptionFromServerWithMultipleEntries,
                    tabHolder: { t: [CustomObjectTypeFactory.TYPE_FACTORY_NAME, 'TabHolder'], s: 1 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    untypedObjectArrayREJECT: { t: [CustomArrayTypeFactory.TYPE_FACTORY_NAME, null] as ITypeFromServer, s: 0 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    untypedObjectArrayALLOW: { t: [CustomArrayTypeFactory.TYPE_FACTORY_NAME, null] as ITypeFromServer, s: 1 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    untypedObjectArraySHALLOW: { t: [CustomArrayTypeFactory.TYPE_FACTORY_NAME, { t: null, s: 2}] as ITypeFromServer, s: 1 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    untypedObjectArrayDEEP: { t: [CustomArrayTypeFactory.TYPE_FACTORY_NAME, { t: null, s: 3 }] as ITypeFromServer, s: 1 } as IPropertyDescriptionFromServerWithMultipleEntries,
                    tabJustForType: [CustomObjectTypeFactory.TYPE_FACTORY_NAME, 'Tab']
                },
                ftd: factoryTypeDetails
            }
        });

        const spec = typesRegistry.getComponentSpecification('someTabPanelSpec');
        stringArrayType = spec.getPropertyType('stringArray') as CustomArrayType<string>;
        stringArrayPushToServer = spec.getPropertyPushToServer('stringArray'); // so computed not declared (undefined -> REJECT)
        stringArrayWithShallowOnElementsType = spec.getPropertyType('stringArrayWithShallowOnElements') as CustomArrayType<string>;
        stringArrayWithShallowOnElementsPushToServer = spec.getPropertyPushToServer('stringArrayWithShallowOnElements'); // so computed not declared (undefined -> REJECT)
        tabArrayWithShallowOnElementsType = spec.getPropertyType('tabArray') as CustomArrayType<Tab>;
        tabArrayWithShallowOnElementsPushToServer = spec.getPropertyPushToServer('tabArray'); // so computed not declared (undefined -> REJECT)
        tabJustForTypeType = spec.getPropertyType('tabJustForType') as CustomObjectType;
        tabHolderElementsType = spec.getPropertyType('tabHolder') as CustomObjectType;
        tabHolderElementsPushToServer = spec.getPropertyPushToServer('tabHolder'); // so computed not declared (undefined -> REJECT)
        untypedObjectArrayWithREJECTOnElementsType = spec.getPropertyType('untypedObjectArrayREJECT') as CustomArrayType<any>;
        untypedObjectArrayWithREJECTOnElementsPushToServer = spec.getPropertyPushToServer('untypedObjectArrayREJECT'); // so computed not declared (undefined -> REJECT)
        untypedObjectArrayWithALLOWOnElementsType = spec.getPropertyType('untypedObjectArrayALLOW') as CustomArrayType<any>;
        untypedObjectArrayWithALLOWOnElementsPushToServer = spec.getPropertyPushToServer('untypedObjectArrayALLOW'); // so computed not declared (undefined -> REJECT)
        untypedObjectArrayWithSHALLOWOnElementsType = spec.getPropertyType('untypedObjectArraySHALLOW') as CustomArrayType<any>;
        untypedObjectArrayWithSHALLOWOnElementsPushToServer = spec.getPropertyPushToServer('untypedObjectArraySHALLOW'); // so computed not declared (undefined -> REJECT)
        untypedObjectArrayWithDEEPOnElementsType = spec.getPropertyType('untypedObjectArrayDEEP') as CustomArrayType<any>;
        untypedObjectArrayWithDEEPOnElementsPushToServer = spec.getPropertyPushToServer('untypedObjectArrayDEEP'); // so computed not declared (undefined -> REJECT)
    });

    const createDefaultStringArrayJSON = (): ICATFullValueFromServer => ({
            v: ['test1', 'test2', 'test3'],
            vEr: 1
        });

    const createSimpleUntypedObjectArray = (): ICATFullValueFromServer => ({
            v: [
                'test1',
                { test2: 1, a: true },
                5,
                [ 1, 2, 3 ],
                { _T: ObjectType.TYPE_NAME, _V: {
                     x: { _T: ObjectType.TYPE_NAME, _V:
                            ['bla', { _T: DateType.TYPE_NAME_SVY, _V: '2007-12-03T' } ]
                        },
                     y: 8 }
                }, // this is how ObjectPropertyType sends it JSON from server if it detects nested special types such as Date
                { _T: DateType.TYPE_NAME_SVY, _V: '2017-12-03T10:15:30' }
            ],
            vEr: 1
        });

    const createTabJSON = ( val: string ): ICOTFullValueFromServer => ({
            v: { name: val, myvalue: val },
            vEr: 1
        });

    const createTabHolderJSONWithFilledArray = ( val: string ): ICOTFullValueFromServer => ({
            v: { name: val, tabs: createArrayWithJSONObject() },
            vEr: 1
        });

    const createArrayWithJSONObject = (): ICATFullValueFromServer => ({
            v: [createTabJSON( 'test1' ), createTabJSON( 'test2' ), createTabJSON( 'test3' )],
            vEr: 1
        });

    it( 'type should be created an array from server to client', () => {
        const val: Array<string> = converterService.convertFromServerToClient(createDefaultStringArrayJSON(),
               stringArrayType , undefined, undefined, undefined, getParentPropertyContext(stringArrayPushToServer));

        expect( val ).toBeDefined();
        expect( val.length ).toBe( 3, 'array length should be 3' );
        expect( val[0] ).toBe( 'test1', 'array[0] should be test1' );
        expect( val[1] ).toBe( 'test2', 'array[1] should be  test2' );
        expect( val[2] ).toBe( 'test3', 'array[2] should be  test3' );
    } );

    it( 'updates from server to client', () => {
        let val: Array<string> = converterService.convertFromServerToClient(createDefaultStringArrayJSON(),
               stringArrayType , undefined, undefined, undefined, getParentPropertyContext(stringArrayPushToServer));

        val = converterService.convertFromServerToClient({
                g: [
                    { op: [ 2, 2, ICATOpTypeEnum.CHANGED ], d: [ 'testChanged' ] },
                    { op: [ 1, 1, ICATOpTypeEnum.DELETE ] },
                    { op: [ 0, 1, ICATOpTypeEnum.INSERT ], d: [ 'testNew1', 'testNew2' ] },
                    { op: [ 4, 4, ICATOpTypeEnum.INSERT ], d: [ 'testNew3' ] }
                ],
                vEr: 1
            } as ICATGranularUpdatesFromServer, stringArrayType, val, undefined, undefined, getParentPropertyContext(stringArrayPushToServer));

        expect( val ).toBeDefined();
        expect( val.length ).toBe( 5, 'array length should be 3' );
        expect( val[0] ).toBe( 'testNew1', 'array[0] should be test1' );
        expect( val[1] ).toBe( 'testNew2', 'array[1] should be  test2' );
        expect( val[2] ).toBe( 'test1', 'array[2] should be  test3' );
        expect( val[3] ).toBe( 'testChanged', 'array[3] should be  test3' );
        expect( val[4] ).toBe( 'testNew3', 'array[4] should be  test3' );
    } );

    it( 'simple change of 1 index', () => {
        const arr = converterService.convertFromServerToClient(createDefaultStringArrayJSON(),
               stringArrayWithShallowOnElementsType , undefined, undefined, undefined, getParentPropertyContext(stringArrayWithShallowOnElementsPushToServer));
        const valAsSeenInternally = arr as IChangeAwareValue;
        const val = arr as Array<String>;
        valAsSeenInternally.getInternalState().setChangeListener(() => {});

        val[0] = 'test4';

        const changes: ICATGranularUpdatesToServer = converterService.convertFromClientToServer(val, stringArrayWithShallowOnElementsType, val,
            getParentPropertyContext(stringArrayWithShallowOnElementsPushToServer))[0];
        expect( changes.vEr ).toBe( 1 );
        expect( changes.u ).toBeDefined( 'change object  shoulld have updates' );
        expect( changes.u.length ).toBe( 1, 'should have 1 update' );
        expect( changes.u[0].i ).toBe( 0 );
        expect( changes.u[0].v ).toBe( 'test4' );

        const changes2: ICATNoOpToServer = converterService.convertFromClientToServer(val, stringArrayWithShallowOnElementsType, val,
            getParentPropertyContext(stringArrayWithShallowOnElementsPushToServer))[0];
        expect( changes2.n ).toBe( true, 'should have no changes now' );
    } );

    it( 'remove of 1 index', () => {
        // TODO this could be improved by really only sending inserts/removes of indexes; so a javascript port of java class ArrayGranularChangeKeeper
        const arr = converterService.convertFromServerToClient(createDefaultStringArrayJSON(),
               stringArrayWithShallowOnElementsType , undefined, undefined, undefined, getParentPropertyContext(PushToServerEnum.ALLOW));
        const valAsSeenInternally = arr as IChangeAwareValue;
        const val = arr as Array<String>;
        valAsSeenInternally.getInternalState().setChangeListener(() => {});

        val.splice( 1, 1 );
        const changes: ICATFullArrayToServer = converterService.convertFromClientToServer(val, stringArrayWithShallowOnElementsType, val,
            getParentPropertyContext(stringArrayWithShallowOnElementsPushToServer))[0];
        expect( changes.vEr ).toBe( 0 );
        expect( changes.v ).toBeDefined( 'change object  shoulld have updates' );
        expect( changes.v.length ).toBe( 2, 'should have 2 updates' );
        expect( changes.v[0] ).toBe( 'test1' );
        expect( changes.v[1] ).toBe( 'test3' );

        const changes2: ICATNoOpToServer = converterService.convertFromClientToServer(val, stringArrayWithShallowOnElementsType, val,
            getParentPropertyContext(stringArrayWithShallowOnElementsPushToServer))[0];
        expect( changes2.n ).toBe( true, 'should have no changes now' );
    } );

    it( 'add  of 1 index', () => {
        // TODO this could be improved by really only sending inserts/removes of indexes; so a javascript port of java class ArrayGranularChangeKeeper
        const arr = converterService.convertFromServerToClient(createDefaultStringArrayJSON(),
               stringArrayWithShallowOnElementsType , undefined, undefined, undefined, getParentPropertyContext(stringArrayWithShallowOnElementsPushToServer));
        const valAsSeenInternally = arr as IChangeAwareValue;
        const val = arr as Array<String>;
        valAsSeenInternally.getInternalState().setChangeListener(() => {});

        val[3] = 'test4';
        const changes: ICATFullArrayToServer = converterService.convertFromClientToServer(val, stringArrayWithShallowOnElementsType, val,
            getParentPropertyContext(stringArrayWithShallowOnElementsPushToServer))[0];
        expect( changes.vEr ).toBe( 0 );
        expect( changes.v ).toBeDefined( 'change object  shoulld have updates' );
        expect( changes.v.length ).toBe( 4, 'should have 4 updates' );
        expect( changes.v[0] ).toBe( 'test1' );
        expect( changes.v[1] ).toBe( 'test2' );
        expect( changes.v[2] ).toBe( 'test3' );
        expect( changes.v[3] ).toBe( 'test4' );

        const changes2: ICATNoOpToServer = converterService.convertFromClientToServer(val, stringArrayWithShallowOnElementsType, val,
            getParentPropertyContext(stringArrayWithShallowOnElementsPushToServer))[0];
        expect( changes2.n ).toBe( true, 'should have no changes now' );
    } );

// THIS will currently no longer work - we need to be smarter if we want this; so a javascript port of java class ArrayGranularChangeKeeper
//    it( 'remove of 1 index and add of one', () => {
//        const val: Array<string> = converterService.convertFromServerToClient(createDefaultStringArrayJSON(),
//               stringArrayWithShallowOnElementsType , undefined, undefined, undefined, getParentPropertyContext(PushToServerEnum.ALLOW));
//        val.splice( 1, 1 );
//        val[2] = 'test4';
//        const changes: ICATGranularUpdatesToServer = converterService.convertFromClientToServer(val, stringArrayWithShallowOnElementsType, val, getParentPropertyContext(PushToServerEnum.ALLOW))[0];
//        expect( changes.vEr ).toBe( 1 );
//        expect( changes.u ).toBeDefined( 'change object  shoulld have updates' );
//        expect( changes.u.length ).toBe( 2, 'should have 2 updates' );
//        expect( changes.u[0].i ).toBe( 1 );
//        expect( changes.u[0].v ).toBe( 'test3' );
//        expect( changes.u[1].i ).toBe( 2 );
//        expect( changes.u[1].v ).toBe( 'test4' );
//
//        const changes2: ICATNoOpToServer = converterService.convertFromClientToServer(val, stringArrayWithShallowOnElementsType, val, getParentPropertyContext(PushToServerEnum.ALLOW))[0];
//        expect( changes2.n ).toBe( true, 'should have no changes now' );
//    } );

// THIS will currently no longer work - we need to be smarter if we want this; so a javascript port of java class ArrayGranularChangeKeeper
//    it( 'remove change and add of one', () => {
//        const val: Array<string> = converterService.convertFromServerToClient(createDefaultStringArrayJSON(),
//               stringArrayWithShallowOnElementsType , undefined, undefined, undefined, getParentPropertyContext(PushToServerEnum.ALLOW));
//        val.splice( 0, 1 );
//        val[1] = 'test4';
//        val[2] = 'test5';
//        const changes: ICATGranularUpdatesToServer = converterService.convertFromClientToServer(val, stringArrayWithShallowOnElementsType, val, getParentPropertyContext(PushToServerEnum.ALLOW))[0];
//        expect( changes.vEr ).toBe( 1 );
//        expect( changes.u ).toBeDefined( 'change object should have updates' );
//        expect( changes.u.length ).toBe( 3, 'should have 3 updates' );
//        expect( changes.u[0].i ).toBe( 0 );
//        expect( changes.u[0].v ).toBe( 'test2' );
//        expect( changes.u[1].i ).toBe( 1 );
//        expect( changes.u[1].v ).toBe( 'test4' );
//        expect( changes.u[2].i ).toBe( 2 );
//        expect( changes.u[2].v ).toBe( 'test5' );
//
//        const changes2: ICATNoOpToServer = converterService.convertFromClientToServer(val, stringArrayWithShallowOnElementsType, val, getParentPropertyContext(PushToServerEnum.ALLOW))[0];
//        expect( changes2.n ).toBe( true, 'should have no changes now' );
//    } );

    it( 'type should be created an array from server to client with custom json objects', () => {
        const arr = converterService.convertFromServerToClient(createArrayWithJSONObject(),
               tabArrayWithShallowOnElementsType , undefined, undefined, undefined, getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer));
        const valAsSeenInternally = arr as IChangeAwareValue;
        const val = arr as Array<Tab>;
        valAsSeenInternally.getInternalState().setChangeListener(() => {});

        expect( val ).toBeDefined();
        expect( val.length ).toBe( 3, 'array length should be 3' );
        expect( val[0].name ).toBe( 'test1', 'array[0] should be tab.name = test1' );
        expect( val[1].name ).toBe( 'test2', 'array[1] should be tab.name = . test2' );
        expect( val[2].name ).toBe( 'test3', 'array[2] should be  tab.name = .test3' );

        const changes2: ICATNoOpToServer = converterService.convertFromClientToServer(val, tabArrayWithShallowOnElementsType, val,
            getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer))[0];
        expect( changes2.n ).toBe( true, 'should have no changes now' );
    } );

    it( 'updates from server to client for custom object element', () => {
        let arr = converterService.convertFromServerToClient(createArrayWithJSONObject(),
               tabArrayWithShallowOnElementsType , undefined, undefined, undefined, getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer));

        arr = converterService.convertFromServerToClient({
                g: [
                    { op: [ 2, 2, ICATOpTypeEnum.CHANGED ], d: [ { u: [ { k: 'name', v: 'KM' } ], vEr: 1 } as ICOTGranularUpdatesFromServer ] },
                ],
                vEr: 1
            } as ICATGranularUpdatesFromServer, tabArrayWithShallowOnElementsType , arr, undefined, undefined, getParentPropertyContext(stringArrayPushToServer));
        const valAsSeenInternally = arr as IChangeAwareValue;
        const val = arr as Array<Tab>;
        valAsSeenInternally.getInternalState().setChangeListener(() => {});

        expect( val ).toBeDefined();
        expect( val.length ).toBe( 3, 'array length should be 3' );
        expect( val[2].name ).toBe( 'KM', 'array[2].name should be the updated one' );

        const changes2: ICATNoOpToServer = converterService.convertFromClientToServer(val, tabArrayWithShallowOnElementsType, val,
            getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer))[0];
        expect( changes2.n ).toBe( true, 'should have no changes now' );
    } );

    it( 'change of one tab value', () => {
        const val = converterService.convertFromServerToClient(createArrayWithJSONObject(),
               tabArrayWithShallowOnElementsType , undefined, undefined, undefined, getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer));

        let changeNotified = false;
        let triggeredSendToServer = false;
        const valSeenExternally = val as ICustomArrayValue<Tab>;
        const valSeenInternally = val as IChangeAwareValue;
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

        valSeenExternally[0].name = 'test4'; // name is ALLOW here; object/array do notice and remember that it has changed, but do not want to push it to server right away
        checkNotifiedAndTriggeredAndClear(true, false);

        let changes2: ICATNoOpToServer = converterService.convertFromClientToServer(valSeenExternally, tabArrayWithShallowOnElementsType, valSeenExternally,
            getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer))[0];
        expect( changes2.n ).not.toBeDefined(); // it will send name as we simulated that the root property send was called by someone and it knows it has changes for that 'name' ALLOW prop

        valSeenExternally[0].myvalue = 'test4';
        checkNotifiedAndTriggeredAndClear(true, true);
        const changes: ICATGranularUpdatesToServer = converterService.convertFromClientToServer(valSeenExternally, tabArrayWithShallowOnElementsType, valSeenExternally,
            getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer))[0];

        expect( changes.vEr ).toBe( 1 );
        expect( changes.u ).toBeDefined( 'change object  shoulld have updates' );
        expect( changes.u.length ).toBe( 1, 'should have 1 update' );
        expect( changes.u[0].i ).toBe( 0 );
        const objElemChange: ICOTGranularUpdatesToServer = changes.u[0].v;
        expect( objElemChange.vEr ).toBe( 1 );
        expect( objElemChange.u ).toBeDefined( 'change object  shoulld have updates' );
        expect( objElemChange.u.length ).toBe( 1, 'should have 1 update' );

        expect( objElemChange.u[0].k ).toBe( 'myvalue' );
        expect( objElemChange.u[0].v ).toBe( 'test4' );

        changes2 = converterService.convertFromClientToServer(valSeenExternally, tabArrayWithShallowOnElementsType,
            valSeenExternally, getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer))[0];
        expect( changes2.n ).toBe( true, 'should have no changes now' );
    } );

    it( 'delete 1 tab', () => {
        // TODO this could be improved by really only sending inserts/removes of indexes; so a javascript port of java class ArrayGranularChangeKeeper
        const arr = converterService.convertFromServerToClient(createArrayWithJSONObject(),
               tabArrayWithShallowOnElementsType , undefined, undefined, undefined, getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer));
        const valAsSeenInternally = arr as IChangeAwareValue;
        const val = arr as Array<Tab>;
        valAsSeenInternally.getInternalState().setChangeListener(() => {});

        val.splice( 1, 1 );
        const changes: ICATFullArrayToServer = converterService.convertFromClientToServer(val, tabArrayWithShallowOnElementsType, val,
            getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer))[0];
        // array is not smart enough yet to send granular delete to server; it sends full value; which means all children are seen as new
        expect( changes.vEr ).toBe( 0 );
        expect( changes.v ).toBeDefined( 'change object  shoulld have updates' );
        expect( changes.v.length ).toBe( 2, 'should have 2 values' );
        let fullTabChange: ICOTFullObjectToServer = changes.v[0];
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test1', 'item[0].name should be test1' );
        expect( fullTabChange.v.myvalue ).toBe( 'test1', 'item[0].myvalue should be test1' );
        fullTabChange = changes.v[1];
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test3', 'item[1].name should be test3' );
        expect( fullTabChange.v.myvalue ).toBe( 'test3', 'item[1].myvalue should be test3' );

        const changes2 = converterService.convertFromClientToServer(val, tabArrayWithShallowOnElementsType, val,
            getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer))[0];
        expect( changes2.n ).toBe( true, 'should have no changes now' );
    } );

    it( 'add 1 tab', () => {
        const arr = converterService.convertFromServerToClient(createArrayWithJSONObject(),
               tabArrayWithShallowOnElementsType , undefined, undefined, undefined, getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer));
        const valAsSeenInternally = arr as IChangeAwareValue;
        const val = arr as Array<Tab>;
        valAsSeenInternally.getInternalState().setChangeListener(() => {});

        const addedTab = new Tab();
        addedTab.myvalue = 'test5';
        addedTab.name = 'test5';
        val.push( addedTab );
        const changes: ICATFullArrayToServer = converterService.convertFromClientToServer(val, tabArrayWithShallowOnElementsType, val,
            getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer))[0];

        expect( changes.vEr ).toBe( 0 );
        expect( changes.v ).toBeDefined( 'change object  shoulld have updates' );
        expect( changes.v.length ).toBe( 4, 'should have 4 values' );
        let fullTabChange: ICOTFullObjectToServer = changes.v[0];
        expect( fullTabChange.vEr ).toBe( 0 );
        fullTabChange = changes.v[3];
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test5', 'item[3].name should be test5' );
        expect( fullTabChange.v.myvalue ).toBe( 'test5', 'item[3].myvalue should be test5' );

        const changes2 = converterService.convertFromClientToServer(val, tabArrayWithShallowOnElementsType, val,
            getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer))[0];
        expect( changes2.n ).toBe( true, 'should have no changes now' );
    } );

    it( 'change of one tab value delete 1 other', () => {
        const arr = converterService.convertFromServerToClient(createArrayWithJSONObject(),
               tabArrayWithShallowOnElementsType , undefined, undefined, undefined, getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer));
        const valAsSeenInternally = arr as IChangeAwareValue;
        const val = arr as Array<Tab>;
        valAsSeenInternally.getInternalState().setChangeListener(() => {});

        val[0].myvalue = 'test4';
        val.splice( 1, 1 );
        const changes: ICATFullArrayToServer = converterService.convertFromClientToServer(val, tabArrayWithShallowOnElementsType, val,
            getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer))[0];

        expect( changes.vEr ).toBe( 0 );
        expect( changes.v ).toBeDefined( 'change object should have updates' );
        expect( changes.v.length ).toBe( 2, 'should have 2 values' );
        let fullTabChange: ICOTFullObjectToServer = changes.v[0];
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test1', 'item[0].name should be test1' );
        expect( fullTabChange.v.myvalue ).toBe( 'test4', 'item[0].myvalue should be test1' );
        fullTabChange = changes.v[1];
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test3', 'item[1].name should be test3' );
        expect( fullTabChange.v.myvalue ).toBe( 'test3', 'item[1].myvalue should be test3' );

        const changes2 = converterService.convertFromClientToServer(val, tabArrayWithShallowOnElementsType, val,
            getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer))[0];
        expect( changes2.n ).toBe( true, 'should have no changes now' );
    } );

    it( 'send array as arg to handler, change a tab by ref', () => {
        const arr = converterService.convertFromServerToClient(createArrayWithJSONObject(),
               tabArrayWithShallowOnElementsType , undefined, undefined, undefined, getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer));
        const valAsSeenInternally = arr as IChangeAwareValue;
        let val = arr as Array<Tab>;
        let changeListenerWasCalled = false;
        valAsSeenInternally.getInternalState().setChangeListener(() => { changeListenerWasCalled = true; });

        // simulate a send to server as argument to a handler for this array (oldVal undefined) - to make sure it doesn't messup it's state if it's also a model prop. (it used getParentPropertyContext above which is for a model prop)
        const changesAndVal: [ICATFullArrayToServer, Array<Tab>] = converterService.convertFromClientToServer(val, tabArrayWithShallowOnElementsType, undefined,
            PushToServerUtils.PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES);
        const changes = changesAndVal[0];
        
        val = changesAndVal[1];
        
        expect( changes.vEr ).toBe( 0 );
        expect( changes.v ).toBeDefined( 'change object should have updates' );
        expect( changes.v.length ).toBe( 3, 'should have 3 values' );
        let fullTabChange: ICOTFullObjectToServer = changes.v[0];
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test1', 'item[0].name should be test1' );
        expect( fullTabChange.v.myvalue ).toBe( 'test1', 'item[0].myvalue should be test1' );
        fullTabChange = changes.v[1];
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test2', 'item[1].name should be test2' );
        expect( fullTabChange.v.myvalue ).toBe( 'test2', 'item[1].myvalue should be test2' );
        fullTabChange = changes.v[2];
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test3', 'item[2].name should be test3' );
        expect( fullTabChange.v.myvalue ).toBe( 'test3', 'item[2].myvalue should be test3' );

        val[0] = { name: 'test4', myvalue: 'test4' };

        expect(changeListenerWasCalled).toBe(true);

        const changes2: ICATGranularUpdatesToServer = converterService.convertFromClientToServer(val, tabArrayWithShallowOnElementsType, val,
            getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer))[0];
        expect( changes2.vEr ).toBe( 1, 'checking version for element update' );
        expect( changes2.u.length ).toBe( 1, 'checking that it is exactly 1 update' );
        expect( changes2.u[0].i ).toBe( 0, 'checking that it is first el' );
        fullTabChange = changes2.u[0].v;
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test4', 'item[0].name should be test4 now' );
        expect( fullTabChange.v.myvalue ).toBe( 'test4', 'item[0].myvalue should be test4 now' );
    } );

    it( 'send obj from model (with push to server reject) as arg to handler', () => {
        const val = converterService.convertFromServerToClient({ v: [ 'test1' ], vEr: 1 },
               untypedObjectArrayWithREJECTOnElementsType , undefined, undefined, undefined, getParentPropertyContext(untypedObjectArrayWithREJECTOnElementsPushToServer));

        const valAsSeenInternally = val as IChangeAwareValue;
        let valCaT = val as ICustomArrayValue<any>;

        let changeListenerWasCalled = false;
        valAsSeenInternally.getInternalState().setChangeListener(() => { changeListenerWasCalled = true; });

        // simulate a send to server as argument to a handler, which should work even though it is a model value with push to server reject
        const changesAndVal: [ICOTFullObjectToServer, ICustomArrayValue<any>] = converterService.convertFromClientToServer(val, untypedObjectArrayWithREJECTOnElementsType, undefined,
            PushToServerUtils.PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES);
        const changes = changesAndVal[0];
        
        valCaT = changesAndVal[1];
        
        expect(changes.vEr).toBe(0, 'full value being sent to server');
        expect(changes.v).toBeDefined('change object should have a value');
        expect(changes.v[0]).toBe('test1');

        valCaT[0] = 'test4';

        expect(changeListenerWasCalled).toBe(false);
        
        // inside the model it is push to server reject
        const changes2: ICATNoOpToServer = converterService.convertFromClientToServer(valCaT, untypedObjectArrayWithREJECTOnElementsType, valCaT,
            getParentPropertyContext(untypedObjectArrayWithREJECTOnElementsPushToServer))[0];
        expect( changes2.n ).toBeTrue();
    } );

    it( 'change array el. by ref but do not send to server (so it still has changes to send for the model property), then send array as arg to handler, change another tab by ref; both tabs changed by ref in the model should be then sent to server', () => {
        const arr = converterService.convertFromServerToClient(createArrayWithJSONObject(),
               tabArrayWithShallowOnElementsType , undefined, undefined, undefined, getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer));
        const valAsSeenInternally = arr as IChangeAwareValue;
        let val = arr as Array<Tab>;
        let changeListenerWasCalled = false;
        valAsSeenInternally.getInternalState().setChangeListener(() => { changeListenerWasCalled = true; }); // we ignore here the doNotPushNow arg of the listener so for this test 'shallow' and 'deep' have the same effect as 'allow', they do not to server send right away even if prop. requests that

        val[1] = { name: 'test2two', myvalue: 'test2two' };

        // simulate a send to server as argument to a handler for this array (oldVal undefined) - to make sure it doesn't messup it's state if it's also a model prop. (it used getParentPropertyContext above which is for a model prop)
        const changesAndVal: [ICATFullArrayToServer, Array<Tab>] = converterService.convertFromClientToServer(val, tabArrayWithShallowOnElementsType, undefined,
            PushToServerUtils.PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES);
        const changes = changesAndVal[0];
        
        val = changesAndVal[1];
        
        expect( changes.vEr ).toBe( 0 );
        expect( changes.v ).toBeDefined( 'change object should have updates' );
        expect( changes.v.length ).toBe( 3, 'should have 3 values' );
        let fullTabChange: ICOTFullObjectToServer = changes.v[0];
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test1', 'item[0].name should be test1' );
        expect( fullTabChange.v.myvalue ).toBe( 'test1', 'item[0].myvalue should be test1' );
        fullTabChange = changes.v[1];
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test2two', 'item[1].name should be test2two' );
        expect( fullTabChange.v.myvalue ).toBe( 'test2two', 'item[1].myvalue should be test2two' );
        fullTabChange = changes.v[2];
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test3', 'item[2].name should be test3' );
        expect( fullTabChange.v.myvalue ).toBe( 'test3', 'item[2].myvalue should be test3' );

        val[0] = { name: 'test4', myvalue: 'test4' };

        expect(changeListenerWasCalled).toBe(true);

        const changes2: ICATGranularUpdatesToServer = converterService.convertFromClientToServer(val, tabArrayWithShallowOnElementsType, val,
            getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer))[0];
        expect( changes2.vEr ).toBe( 1, 'checking version for element update' );
        expect( changes2.u.length ).toBe( 2, 'checking that it is exactly 2 updates' );
        expect( changes2.u[0].i ).toBe( 1, 'checking that first is the first el' );
        expect( changes2.u[1].i ).toBe( 0, 'checking that first is the first el' );
        fullTabChange = changes2.u[0].v;
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test2two', 'item[1].name should be test2two now' );
        expect( fullTabChange.v.myvalue ).toBe( 'test2two', 'item[1].myvalue should be test2two now' );
        fullTabChange = changes2.u[1].v;
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test4', 'item[0].name should be test4 now' );
        expect( fullTabChange.v.myvalue ).toBe( 'test4', 'item[0].myvalue should be test4 now' );
    } );

    it( 'send array as arg to handler, change a tab subprop', () => {
        const arr = converterService.convertFromServerToClient(createArrayWithJSONObject(),
               tabArrayWithShallowOnElementsType , undefined, undefined, undefined, getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer));
        const valAsSeenInternally = arr as IChangeAwareValue;
        let val = arr as Array<Tab>;
        let changeListenerWasCalled = false;
        valAsSeenInternally.getInternalState().setChangeListener(() => { changeListenerWasCalled = true; });

        // simulate a send to server as argument to a handler for this array (oldVal undefined) - to make sure it doesn't messup it's state if it's also a model prop. (it used getParentPropertyContext above which is for a model prop)
        const changesAndVal: [ICATFullArrayToServer, Array<Tab>] = converterService.convertFromClientToServer(val, tabArrayWithShallowOnElementsType, undefined,
            PushToServerUtils.PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES);
        const changes = changesAndVal[0];
        
        val = changesAndVal[1];
        
        expect( changes.vEr ).toBe( 0 );
        expect( changes.v ).toBeDefined( 'change object should have updates' );
        expect( changes.v.length ).toBe( 3, 'should have 3 values' );
        let fullTabChange: ICOTFullObjectToServer = changes.v[0];
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test1', 'item[0].name should be test1' );
        expect( fullTabChange.v.myvalue ).toBe( 'test1', 'item[0].myvalue should be test1' );
        fullTabChange = changes.v[1];
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test2', 'item[1].name should be test2' );
        expect( fullTabChange.v.myvalue ).toBe( 'test2', 'item[1].myvalue should be test2' );
        fullTabChange = changes.v[2];
        expect( fullTabChange.vEr ).toBe( 0 );
        expect( fullTabChange.v.name ).toBe( 'test3', 'item[2].name should be test3' );
        expect( fullTabChange.v.myvalue ).toBe( 'test3', 'item[2].myvalue should be test3' );

        val[0].myvalue = 'test4';

        expect(changeListenerWasCalled).toBe(true);

        const changes2: ICATGranularUpdatesToServer = converterService.convertFromClientToServer(val, tabArrayWithShallowOnElementsType, val,
            getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer))[0];
        expect( changes2.vEr ).toBe( 1, 'checking version for element update' );
        expect( changes2.u.length ).toBe( 1, 'checking that it is exactly 1 update' );
        expect( changes2.u[0].i ).toBe( 0, 'checking that it is first el' );
        const partialTabChange: ICOTGranularUpdatesToServer = changes2.u[0].v;
        expect( partialTabChange.vEr ).toBe( 1 );
        expect( partialTabChange.u.length ).toBe( 1, 'checking that it is exactly 1 update' );
        expect( partialTabChange.u[0].k ).toBe( 'myvalue', 'checking that it is first el' );
        expect( partialTabChange.u[0].v ).toBe( 'test4', 'item[0].myvalue should be test4 now' );
    } );

// THIS will currently not work - we need to be smarter if we want this; so a javascript port of java class ArrayGranularChangeKeeper
//    it( 'delete and add one', () => {
//        const val: Array<Tab> = converterService.convertFromServerToClient(createArrayWithJSONObject(),
//               tabArrayWithShallowOnElementsType , undefined, undefined, undefined, getParentPropertyContext(PushToServerEnum.ALLOW));
//
//        val.splice( 1, 1 );
//        const addedTab = new Tab();
//        addedTab.myvalue = 'test5';
//        addedTab.name = 'test5';
//        val.push( addedTab );
//
//        const changes: ICATGranularUpdatesToServer = converterService.convertFromClientToServer(val, tabArrayWithShallowOnElementsType, val, getParentPropertyContext(PushToServerEnum.ALLOW))[0];
//
//        expect( changes[CONTENT_VERSION] ).toBe( 1 );
//        expect( changes[UPDATES] ).toBeDefined( 'change object  shoulld have updates' );
//        expect( changes[UPDATES].length ).toBe( 2, 'should have 2 update' );
//
//        expect( changes[UPDATES][0][INDEX] ).toBe( 1 );
//        expect( changes[UPDATES][0][VALUE] ).toBeDefined( 'change object  shoulld have value' );
//        expect( changes[UPDATES][0][VALUE][VALUE].name ).toBe( 'test3' );
//        expect( changes[UPDATES][0][VALUE][VALUE].myvalue ).toBe( 'test3' );
//
//        expect( changes[UPDATES][1][INDEX] ).toBe( 2 );
//        expect( changes[UPDATES][1][VALUE] ).toBeDefined( 'change object  shoulld have value' );
//        expect( changes[UPDATES][1][VALUE][VALUE].name ).toBe( 'test5' );
//        expect( changes[UPDATES][1][VALUE][VALUE].myvalue ).toBe( 'test5' );
//
//        changes = converterService.convertFromClientToServer( val, 'JSON_arr', val )[0];
//        expect( changes[NO_OP] ).toBeDefined( 'should have no changes now' );
//    } );

// THIS will currently not work - we need to be smarter if we want this; so a javascript port of java class ArrayGranularChangeKeeper
//    it( ' change of one tab value delete 1 other and add 1', () => {
//        const val: Array<Tab> = converterService.convertFromServerToClient(createArrayWithJSONObject(),
//               tabArrayWithShallowOnElementsType , undefined, undefined, undefined, getParentPropertyContext(PushToServerEnum.ALLOW));
//
//        val[0].myvalue = 'test4';
//        val.splice( 1, 1 );
//        const addedTab = new Tab();
//        addedTab.myvalue = 'test5';
//        addedTab.name = 'test5';
//        val.push( addedTab );
//        const changes: ICATGranularUpdatesToServer = converterService.convertFromClientToServer(val, tabArrayWithShallowOnElementsType, val, getParentPropertyContext(PushToServerEnum.ALLOW))[0];
//
//        expect( changes[CONTENT_VERSION] ).toBe( 1 );
//        expect( changes[UPDATES] ).toBeDefined( 'change object  shoulld have updates' );
//        expect( changes[UPDATES].length ).toBe( 3, 'should have 1 update' );
//        expect( changes[UPDATES][0][INDEX] ).toBe( 0 );
//        expect( changes[UPDATES][0][VALUE][CONTENT_VERSION] ).toBe( 1 );
//        expect( changes[UPDATES][0][VALUE][UPDATES] ).toBeDefined( 'change object  shoulld have updates' );
//        expect( changes[UPDATES][0][VALUE][UPDATES].length ).toBe( 1, 'should have 1 update' );
//        expect( changes[UPDATES][0][VALUE][UPDATES][0][KEY] ).toBe( 'myvalue' );
//        expect( changes[UPDATES][0][VALUE][UPDATES][0][VALUE] ).toBe( 'test4' );
//
//        expect( changes[UPDATES][1][INDEX] ).toBe( 1 );
//        expect( changes[UPDATES][1][VALUE] ).toBeDefined( 'change object  shoulld have value' );
//        expect( changes[UPDATES][1][VALUE][VALUE].name ).toBe( 'test3' );
//        expect( changes[UPDATES][1][VALUE][VALUE].myvalue ).toBe( 'test3' );
//
//        expect( changes[UPDATES][2][INDEX] ).toBe( 2 );
//        expect( changes[UPDATES][2][VALUE] ).toBeDefined( 'change object  shoulld have value' );
//        expect( changes[UPDATES][2][VALUE][VALUE].name ).toBe( 'test5' );
//        expect( changes[UPDATES][2][VALUE][VALUE].myvalue ).toBe( 'test5' );
//        changes = converterService.convertFromClientToServer( val, 'JSON_arr', val )[0];
//        expect( changes[NO_OP] ).toBeDefined( 'should have no changes now' );
//    } );

    it( 'create a tabholder with 3 tabs in its array', () => {
        const arr = converterService.convertFromServerToClient(createTabHolderJSONWithFilledArray( 'test' ),
               tabHolderElementsType , undefined, undefined, undefined, getParentPropertyContext(tabHolderElementsPushToServer));
        const valAsSeenInternally = arr as IChangeAwareValue;
        const val = arr as TabHolder;
        valAsSeenInternally.getInternalState().setChangeListener(() => {});

        expect( val.tabs.length ).toBe( 3, 'should have 3 tabs' );
        const changes2: ICOTNoOpToServer = converterService.convertFromClientToServer(val, tabHolderElementsType, val,
            getParentPropertyContext(tabHolderElementsPushToServer))[0];
        expect( changes2.n ).toBe( true, 'should have no changes now' );
    } );

    it( 'update a tab in the tabs array of the TabHolder', () => {
        const arr = converterService.convertFromServerToClient(createTabHolderJSONWithFilledArray( 'test' ),
               tabHolderElementsType , undefined, undefined, undefined, getParentPropertyContext(tabHolderElementsPushToServer));
        const valAsSeenInternally = arr as IChangeAwareValue;
        const val = arr as TabHolder;
        valAsSeenInternally.getInternalState().setChangeListener(() => {});

        val.tabs[0].myvalue = 'test4';
        const changes: ICOTGranularUpdatesToServer = converterService.convertFromClientToServer(val, tabHolderElementsType, val,
            getParentPropertyContext(tabHolderElementsPushToServer))[0];

        expect( changes.vEr ).toBe( 1 );
        expect( changes.u ).toBeDefined( 'change object  shoulld have updates' );
        expect( changes.u.length ).toBe( 1, 'should have 1 update' );
        expect( changes.u[0].k ).toBe( 'tabs', 'should have tabs key' );
        const changedTabsUpdates: ICATGranularUpdatesToServer = changes.u[0].v;
        expect( changedTabsUpdates.u ).toBeDefined();
        expect( changedTabsUpdates.u.length ).toBe( 1 );
        expect( changedTabsUpdates.u[0].i ).toBe( 0 );
        const changedTabUpdates: ICOTGranularUpdatesToServer = changedTabsUpdates.u[0].v;
        expect( changedTabUpdates.u.length ).toBe( 1 );
        expect( changedTabUpdates.u[0].k ).toBe( 'myvalue' );
        expect( changedTabUpdates.u[0].v ).toBe( 'test4' );

        const changes2: ICOTNoOpToServer = converterService.convertFromClientToServer(val, tabHolderElementsType, val,
            getParentPropertyContext(tabHolderElementsPushToServer))[0];
        expect( changes2.n ).toBe( true, 'should have no changes now' );
    } );

    it( 'add script tabs array into a TabHolder', () => {
        const arr = converterService.convertFromServerToClient(createTabHolderJSONWithFilledArray( 'test' ),
               tabHolderElementsType , undefined, undefined, undefined, getParentPropertyContext(tabHolderElementsPushToServer));
        const valAsSeenInternally = arr as IChangeAwareValue;
        const val = arr as TabHolder;
        valAsSeenInternally.getInternalState().setChangeListener(() => {});

        const tabs: Array<Tab> = [];
        tabs[0] = new Tab();
        tabs[0].name = 'test1';
        tabs[0].myvalue = 'test1';
        val.tabs = tabs;
        const changes: ICOTGranularUpdatesToServer = converterService.convertFromClientToServer(val, tabHolderElementsType, val,
            getParentPropertyContext(tabHolderElementsPushToServer))[0];
        expect( changes.vEr ).toBe( 1 );
        expect( changes.u ).toBeDefined( 'change object  shoulld have updates' );
        expect( changes.u.length ).toBe( 1, 'should have 1 update' );
        expect( changes.u[0].k ).toBe( 'tabs', 'should have tabs key' );
        const changedTabsFull: ICATFullArrayToServer = changes.u[0].v;
        expect( changedTabsFull.v ).toBeDefined();
        expect( changedTabsFull.v.length ).toBe( 1 );
        const changedTabFull: ICOTFullObjectToServer = changedTabsFull.v[0];
        expect( changedTabFull.v.name ).toBe( 'test1' );
        expect( changedTabFull.v.myvalue ).toBe( 'test1' );

        const changes2: ICOTNoOpToServer = converterService.convertFromClientToServer(val, tabHolderElementsType, val,
            getParentPropertyContext(tabHolderElementsPushToServer))[0];
        expect( changes2.n ).toBe( true, 'should have no changes now' );
    } );

    it( 'test mark for change', () => {
        const arr = converterService.convertFromServerToClient(createTabHolderJSONWithFilledArray( 'test' ),
               tabHolderElementsType , undefined, undefined, undefined, getParentPropertyContext(tabHolderElementsPushToServer));
        const valAsSeenInternally = arr as IChangeAwareValue;
        const val = arr as TabHolder;
        valAsSeenInternally.getInternalState().setChangeListener(() => {});

        val.tabs[3] = new Tab();
        val.tabs[3].name = 'test4';
        val.tabs[3].myvalue = 'test4';

        const changes: ICOTGranularUpdatesToServer = converterService.convertFromClientToServer(val, tabHolderElementsType, val,
            getParentPropertyContext(tabHolderElementsPushToServer))[0];
        expect( changes.vEr ).toBe( 1 );
        expect( changes.u ).toBeDefined( 'change object should have updates' );
        expect( changes.u.length ).toBe( 1, 'should have 1 update' );
        expect( changes.u[0].k ).toBe( 'tabs', 'should have tabs key' );

        const changedTabsFull: ICATFullArrayToServer = changes.u[0].v;
        expect( changedTabsFull.v ).toBeDefined();
        expect( changedTabsFull.v.length ).toBe( 4 );

        let changedTabFull: ICOTFullObjectToServer = changedTabsFull.v[0];
        expect( changedTabFull.v.name ).toBe( 'test1' );
        expect( changedTabFull.v.myvalue ).toBe( 'test1' );
        expect( changedTabFull.vEr ).toBe( 0 );

        changedTabFull = changedTabsFull.v[3];
        expect( changedTabFull.v.name ).toBe( 'test4' );
        expect( changedTabFull.v.myvalue ).toBe( 'test4' );
        expect( changedTabFull.vEr ).toBe( 0 );

        const changes2: ICOTNoOpToServer = converterService.convertFromClientToServer(val, tabHolderElementsType, val,
            getParentPropertyContext(tabHolderElementsPushToServer))[0];
        expect( changes2.n ).toBe( true, 'should have no changes now' );
    } );

    it( 'test deep change in an "object[]" with ALLOW', () => {
        const val = converterService.convertFromServerToClient(createSimpleUntypedObjectArray(),
               untypedObjectArrayWithALLOWOnElementsType , undefined, undefined, undefined, getParentPropertyContext(untypedObjectArrayWithALLOWOnElementsPushToServer));

        expect(val.length).toBe(6, 'length not received correctly');
        expect(val[0]).toBe('test1', 'el. 0 with no conversion not received correctly');
        expect(val[1].test2).toBe(1, 'el. 1.test2 with no conversion not received correctly');
        expect(val[1].a).toBe(true, 'el. 1.a with no conversion not received correctly');
        expect(val[2]).toBe(5, 'el. 2 with no conversion not received correctly');

        expect(val[3].length).toBe(3, 'el. 3 with no conversion has wrong length');
        expect(val[3][0]).toBe(1, 'el. 3[0] with no conversion not received correctly');
        expect(val[3][1]).toBe(2, 'el. 3[1] with no conversion not received correctly');
        expect(val[3][2]).toBe(3, 'el. 3[2] with no conversion not received correctly');
        expect(val[4].x.length).toBe(2, 'el. 4.x with conversion has wrong length');
        expect(val[4].x[0]).toBe('bla', 'el. 4.x[0] with conversion not received correctly');
        expect(val[4].x[1] instanceof Date).toBeTruthy('el. 4.x[1] with conversion not received correctly or not instance of Date');
        expect(val[5] instanceof Date).toBeTruthy('el[5] with conversion not received correctly or not instance of Date');

        const valSeenExternally = val as ICustomArrayValue<any>;
        const valSeenInternally = val as IChangeAwareValue;

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

        val[0] = 'changedByRef';
        checkNotifiedAndTriggeredAndClear(true, false); // marked by array proxy that it has change by ref, but not triggered automatically as it is only ALLOW pushToServer

        let changes: ICATGranularUpdatesToServer = converterService.convertFromClientToServer(val, untypedObjectArrayWithALLOWOnElementsType, val,
            getParentPropertyContext(untypedObjectArrayWithALLOWOnElementsPushToServer))[0];

        expect(changes.vEr).toBe(1);
        expect(changes.u).toBeDefined('change object should have updates');
        expect(changes.u.length).toBe(1, 'should have 1 update');
        expect(changes.u[0].i).toBe(0);
        expect(changes.u[0].v).toBe('changedByRef');


        val[3][2] = 15;
        checkNotifiedAndTriggeredAndClear(false, false); // DEEP change in element - in untyped array object[]; we do not automatically detect those
        let changes2: ICATNoOpToServer = converterService.convertFromClientToServer(val, untypedObjectArrayWithALLOWOnElementsType, val,
            getParentPropertyContext(untypedObjectArrayWithALLOWOnElementsPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');

        valSeenExternally.markElementAsHavingDeepChanges(3);
        checkNotifiedAndTriggeredAndClear(true, false); // DEEP change in element that was marked manually; but as it is ALLOW it does no send it right away to server
        changes = converterService.convertFromClientToServer(val, untypedObjectArrayWithALLOWOnElementsType, val,
            getParentPropertyContext(untypedObjectArrayWithALLOWOnElementsPushToServer))[0];

        expect(changes.vEr).toBe(1);
        expect(changes.u).toBeDefined('change object should have updates');
        expect(changes.u.length).toBe(1, 'should have 1 update');
        expect(changes.u[0].i).toBe(3);
        expect(changes.u[0].v[0]).toBe(1, 'should be unchanged');
        expect(changes.u[0].v[1]).toBe(2, 'should be unchanged');
        expect(changes.u[0].v[2]).toBe(15, 'deep change happened here');


        changes2 = converterService.convertFromClientToServer(val, untypedObjectArrayWithALLOWOnElementsType, val,
            getParentPropertyContext(untypedObjectArrayWithALLOWOnElementsPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');
    } );

    it( 'test deep change in an "object[]" with SHALLOW', () => {
        const val = converterService.convertFromServerToClient(createSimpleUntypedObjectArray(),
               untypedObjectArrayWithSHALLOWOnElementsType , undefined, undefined, undefined, getParentPropertyContext(untypedObjectArrayWithSHALLOWOnElementsPushToServer));

        const valSeenExternally = val as ICustomArrayValue<any>;
        const valSeenInternally = val as IChangeAwareValue;

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


        val[0] = 'changedByRef';
        checkNotifiedAndTriggeredAndClear(true, true); // marked by array proxy that it has change by ref, and triggered to be sent automatically as it is SHALLOW pushToServer

        let changes: ICATGranularUpdatesToServer = converterService.convertFromClientToServer(val, untypedObjectArrayWithSHALLOWOnElementsType, val,
            getParentPropertyContext(untypedObjectArrayWithSHALLOWOnElementsPushToServer))[0];

        expect(changes.vEr).toBe(1);
        expect(changes.u).toBeDefined('change object should have updates');
        expect(changes.u.length).toBe(1, 'should have 1 update');
        expect(changes.u[0].i).toBe(0);
        expect(changes.u[0].v).toBe('changedByRef');


        val[3][2] = 15;
        checkNotifiedAndTriggeredAndClear(false, false); // DEEP change in element - in untyped array object[]; we do not automatically detect those
        let changes2: ICATNoOpToServer = converterService.convertFromClientToServer(val, untypedObjectArrayWithSHALLOWOnElementsType, val,
            getParentPropertyContext(untypedObjectArrayWithSHALLOWOnElementsPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');

        valSeenExternally.markElementAsHavingDeepChanges(3);
        checkNotifiedAndTriggeredAndClear(true, false); // DEEP change in element that was marked manually; but as it is SHALLOW it does no trigger a send right away to server
        changes = converterService.convertFromClientToServer(val, untypedObjectArrayWithSHALLOWOnElementsType, val,
            getParentPropertyContext(untypedObjectArrayWithSHALLOWOnElementsPushToServer))[0];

        expect(changes.vEr).toBe(1);
        expect(changes.u).toBeDefined('change object should have updates');
        expect(changes.u.length).toBe(1, 'should have 1 update');
        expect(changes.u[0].i).toBe(3);
        expect(changes.u[0].v[0]).toBe(1, 'should be unchanged');
        expect(changes.u[0].v[1]).toBe(2, 'should be unchanged');
        expect(changes.u[0].v[2]).toBe(15, 'deep change happened here');


        changes2 = converterService.convertFromClientToServer(val, untypedObjectArrayWithSHALLOWOnElementsType, val,
            getParentPropertyContext(untypedObjectArrayWithSHALLOWOnElementsPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');
    } );

    it( 'test deep change in an "object[]" with DEEP', () => {
        const val = converterService.convertFromServerToClient(createSimpleUntypedObjectArray(),
               untypedObjectArrayWithDEEPOnElementsType , undefined, undefined, undefined, getParentPropertyContext(untypedObjectArrayWithDEEPOnElementsPushToServer));

        const valSeenExternally = val as ICustomArrayValue<any>;
        const valSeenInternally = val as IChangeAwareValue;

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


        val[0] = 'changedByRef';
        checkNotifiedAndTriggeredAndClear(true, true); // marked by array proxy that it has change by ref, and triggered to be sent automatically as it is SHALLOW pushToServer

        let changes: ICATGranularUpdatesToServer = converterService.convertFromClientToServer(val, untypedObjectArrayWithDEEPOnElementsType, val,
            getParentPropertyContext(untypedObjectArrayWithDEEPOnElementsPushToServer))[0];

        expect(changes.vEr).toBe(1);
        expect(changes.u).toBeDefined('change object should have updates');
        expect(changes.u.length).toBe(1, 'should have 1 update');
        expect(changes.u[0].i).toBe(0);
        expect(changes.u[0].v).toBe('changedByRef');


        val[3][2] = 15;
        checkNotifiedAndTriggeredAndClear(false, false); // DEEP change in element - in untyped array object[]; we do not automatically detect those
        let changes2: ICATNoOpToServer = converterService.convertFromClientToServer(val, untypedObjectArrayWithDEEPOnElementsType, val,
            getParentPropertyContext(untypedObjectArrayWithALLOWOnElementsPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');

        valSeenExternally.markElementAsHavingDeepChanges(3);
        checkNotifiedAndTriggeredAndClear(true, true); // DEEP change in element that was marked manually; as it has DEEP pushToServer, it does trigger a send right away to server
        changes = converterService.convertFromClientToServer(val, untypedObjectArrayWithDEEPOnElementsType, val,
            getParentPropertyContext(untypedObjectArrayWithDEEPOnElementsPushToServer))[0];

        expect(changes.vEr).toBe(1);
        expect(changes.u).toBeDefined('change object should have updates');
        expect(changes.u.length).toBe(1, 'should have 1 update');
        expect(changes.u[0].i).toBe(3);
        expect(changes.u[0].v[0]).toBe(1, 'should be unchanged');
        expect(changes.u[0].v[1]).toBe(2, 'should be unchanged');
        expect(changes.u[0].v[2]).toBe(15, 'deep change happened here');


        changes2 = converterService.convertFromClientToServer(val, untypedObjectArrayWithDEEPOnElementsType, val,
            getParentPropertyContext(untypedObjectArrayWithDEEPOnElementsPushToServer))[0];
        expect(changes2.n).toBe(true, 'should have no changes now');
    } );

    it( 'when an already smart value (received as return value from an server side api call for example) is assigned into the model into a new location and sent to server, it should still work - have a correct change listener etc.', () => {
        // in model
        const val = converterService.convertFromServerToClient(createArrayWithJSONObject() as ICATFullValueFromServer, tabArrayWithShallowOnElementsType , undefined, undefined, undefined, getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer));
        
        let tabArray = val as Tab[];
        let changeListenerWasCalled = false;
        (val as IChangeAwareValue).getInternalState().setChangeListener(() => { changeListenerWasCalled = true; });

        expect(changeListenerWasCalled).toBeFalse();

        // received as return value from a server side api call
        const childTab = converterService.convertFromServerToClient(createTabJSON('iAmArg'),
               tabJustForTypeType , undefined, undefined, undefined, PushToServerUtils.PROPERTY_CONTEXT_FOR_INCOMMING_ARGS_AND_RETURN_VALUES);
        
        expect(((childTab as any) as IChangeAwareValue).getInternalState().hasChangeListener()).toBeFalse();

        // assign it to model val's subproperty
        tabArray[2] = childTab;

        expect(changeListenerWasCalled).toBeTrue();
        changeListenerWasCalled = false;

        expect(((childTab as any) as IChangeAwareValue).getInternalState().hasChangeListener()).toBeTrue();

        // simulate a send to server as argument to a handler for this array (oldVal undefined) - to make sure it doesn't messup it's state if it's also a model prop. (it used getParentPropertyContext above which is for a model prop)
        const changesAndVal: [ICATGranularUpdatesToServer, any] = converterService.convertFromClientToServer(tabArray, tabArrayWithShallowOnElementsType, tabArray,
             getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer));
        let changes = changesAndVal[0];
        
        tabArray = changesAndVal[1] as Tab[];
        let tabArrayAsSeenInternally = changesAndVal[1] as IChangeAwareValue;
        
        expect(tabArrayAsSeenInternally.getInternalState().hasChangeListener()).toBeTrue();
        expect(((tabArray[2] as any) as IChangeAwareValue).getInternalState().hasChangeListener()).toBeTrue();
        
        expect(changes.vEr).toBe(1);
        expect(changes.u.length).toBe(1);
        expect(changes.u[0].i).toBe(2);
        const tabChanges = changes.u[0].v as ICOTFullObjectToServer;
        expect(tabChanges.vEr).toBe(0);
        expect(tabChanges.v).toEqual({name: 'iAmArg', myvalue: 'iAmArg'});

        tabArray[2].myvalue = 'iAmModel';

        expect(changeListenerWasCalled).toBe(true);

        const changes2: ICATGranularUpdatesToServer = converterService.convertFromClientToServer(tabArray, tabArrayWithShallowOnElementsType, tabArray,
            getParentPropertyContext(tabArrayWithShallowOnElementsPushToServer))[0];
        
        expect(changes2.vEr).toBe(1);
        expect(changes2.u.length).toBe(1);
        expect(changes2.u[0].i).toBe(2); 
        const tabsUpdate = changes2.u[0].v as ICOTGranularUpdatesToServer;
        expect(tabsUpdate.vEr).toBe(1);
        expect(tabsUpdate.u.length).toBe(1);
        expect(tabsUpdate.u[0].k).toBe('myvalue');
        expect(tabsUpdate.u[0].v).toEqual('iAmModel');
    } );

} );

class Tab {
    name: string;
    myvalue: string;
}

class TabHolder {
    name: string;
    tabs: Array<Tab>;
}
