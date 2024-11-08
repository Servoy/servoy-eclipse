import { TestBed } from '@angular/core/testing';
import { ConverterService } from '../../sablo/converter.service';
import { SabloService } from '../../sablo/sablo.service';
import { LoggerFactory, SessionStorageService } from '@servoy/public';
import { WindowRefService } from '@servoy/public';
import { ServicesService } from '../../sablo/services.service';
import { FoundsetType } from './foundset_converter';
import { FoundsetLinkedType, FoundsetLinkedValue } from './foundsetLinked_converter';
import { SabloDeferHelper } from '../../sablo/defer.service';
import { ViewportService } from '../services/viewport.service';
import { LoadingIndicatorService } from '../../sablo/util/loading-indicator/loading-indicator.service';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { TypesRegistry, PropertyContext, PushToServerEnum } from '../../sablo/types_registry';
import { DateType } from '../../sablo/converters/date_converter';
import { ObjectType } from '../../sablo/converters/object_converter';

describe('FoundsetLinked Converter', () => {
    let converterService: ConverterService<FoundsetLinkedValue>;
    let typesRegistry: TypesRegistry;
    let loggerFactory: LoggerFactory;
    let sabloService: SabloService;
    let sabloDeferHelper: SabloDeferHelper;

    let propertyContext: PropertyContext;
    let serverValue: any;
    let realClientValue: FoundsetLinkedValue;

    let changeNotified = false;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ServoyTestingModule],
            providers: [ConverterService, LoggerFactory,
                WindowRefService, ServicesService, SessionStorageService, ViewportService, LoadingIndicatorService]
        });

        sabloService = TestBed.inject(SabloService);
        sabloService.connect({}, {}, '');
        sabloDeferHelper = TestBed.inject(SabloDeferHelper);
        const viewportService = TestBed.inject(ViewportService);
        loggerFactory = TestBed.inject(LoggerFactory);
        converterService = TestBed.inject(ConverterService) as ConverterService<FoundsetLinkedValue>;
        typesRegistry = TestBed.inject(TypesRegistry);
        typesRegistry.registerGlobalType(DateType.TYPE_NAME_SVY, new DateType(), true);
        typesRegistry.registerGlobalType(ObjectType.TYPE_NAME, new ObjectType(typesRegistry, converterService, loggerFactory), true);
        typesRegistry.registerGlobalType(FoundsetType.TYPE_NAME, new FoundsetType(sabloService, sabloDeferHelper, viewportService, loggerFactory));
        typesRegistry.registerGlobalType(FoundsetLinkedType.TYPE_NAME, new FoundsetLinkedType(sabloService, viewportService, loggerFactory));
        changeNotified = false;

        const angularEquality = (first: any, second: any) => (JSON.stringify(first) === JSON.stringify(second)); // WAS angular.equals(first, second);

        jasmine.addCustomEqualityTester(angularEquality);
    });

    const getAndClearNotified = () => {
        const tm = changeNotified;
        changeNotified = false;
        return tm;
    };

    describe('foundsetLinked_property with dumb values and simple values suite; pushToServer not set (so reject)', () => {
        beforeEach(() => {
            const myfoundset = {
                serverSize: 0,
                selectedRowIndexes: [],
                multiSelect: false,
                viewPort:
                {
                    startIndex: 0,
                    size: 2,
                    rows: [{ _svyRowId: 'bla bla' }, { _svyRowId: 'har har' }]
                }
            };
            const fs = converterService.convertFromServerToClient(myfoundset, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), undefined, undefined, undefined, undefined);
            propertyContext = new PropertyContext((prop) => ({ myfoundset: fs }[prop]), PushToServerEnum.REJECT, true);

            serverValue = { forFoundset: 'myfoundset' };

            realClientValue = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetLinkedType.TYPE_NAME),
                                    undefined, undefined, undefined, propertyContext);
            realClientValue.getInternalState().setChangeListener((doNotPush?: boolean) => {
                changeNotified = true;
            });
            serverValue = { forFoundset: 'myfoundset', sv: ':) --- static string ***' };
            realClientValue = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetLinkedType.TYPE_NAME),
                                    realClientValue, undefined, undefined, propertyContext);

            expect(getAndClearNotified()).toEqual(false);
            expect(realClientValue.getInternalState().hasChanges()).toEqual(false);
            expect(realClientValue).toEqual([':) --- static string ***', ':) --- static string ***']);
        });


        it('Should not send value updates for when pushToServer is not specified', () => {
            realClientValue[1] = 'I am changed but shouldn\'t be sent';

            expect(getAndClearNotified()).toEqual(false);
            expect(realClientValue.getInternalState().hasChanges()).toEqual(false);
        });

    });

    describe('foundsetLinked_property with dumb values and foundset linked values suite; pushToServer not set (so reject)', () => {
        beforeEach(() => {
            const myfoundset = {
                serverSize: 0,
                selectedRowIndexes: [],
                multiSelect: false,
                viewPort:
                {
                    startIndex: 0,
                    size: 2,
                    rows: [{ _svyRowId: 'bla bla' }, { _svyRowId: 'har har' }, { _svyRowId: 'bl bl' }, { _svyRowId: 'ha ha' }, { _svyRowId: 'b b' }, { _svyRowId: 'h h' }]
                }
            };
            const fs = converterService.convertFromServerToClient(myfoundset, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), undefined, undefined, undefined, undefined);
            propertyContext = new PropertyContext((prop) => ({ myfoundset: fs }[prop]), PushToServerEnum.REJECT, true);

            serverValue = { forFoundset: 'myfoundset' };
            realClientValue = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetLinkedType.TYPE_NAME),
                                                undefined, undefined, undefined, propertyContext);
            realClientValue.getInternalState().setChangeListener((doNotPush?: boolean) => {
                changeNotified = true;
            });


            serverValue = {
                forFoundset: 'myfoundset',
                vp: [10643, 10702, 10835, 10952, 11011, 11081]
            };

            realClientValue = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetLinkedType.TYPE_NAME),
                                                realClientValue, undefined, undefined, propertyContext);

            expect(getAndClearNotified()).toEqual(false);
            expect(realClientValue.getInternalState().hasChanges()).toEqual(false);
            expect(realClientValue).toEqual([10643, 10702, 10835, 10952, 11011, 11081]);
        });


        it('Should not send value updates for when pushToServer is not specified', () => {
            realClientValue[2] = 100001010;
            realClientValue.dataChanged(2, 100001010);

            expect(getAndClearNotified()).toEqual(false);
            expect(realClientValue.getInternalState().hasChanges()).toEqual(false);

            // REJECT pushToServer in spec, so explicit changes should not be sent to server either
            realClientValue.dataChanged(2, 1111111);
            expect(getAndClearNotified()).toEqual(false);
            expect(realClientValue.getInternalState().hasChanges()).toEqual(false);
        });

    });

    describe('foundsetLinked_property with dumb values and foundset linked values suite; pushToServer set to ALLOW', () => {
        beforeEach(() => {
            const myfoundset = {
                serverSize: 0,
                selectedRowIndexes: [],
                multiSelect: false,
                viewPort:
                {
                    startIndex: 0,
                    size: 2,
                    rows: [{ _svyRowId: 'bla bla' }, { _svyRowId: 'har har' }, { _svyRowId: 'bl bl' }, { _svyRowId: 'ha ha' }, { _svyRowId: 'b b' }, { _svyRowId: 'h h' }]
                }
            };
            const fs = converterService.convertFromServerToClient(myfoundset, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), undefined, undefined, undefined, undefined);
            propertyContext = new PropertyContext((prop) => ({ myfoundset: fs }[prop]), PushToServerEnum.ALLOW, true);

            serverValue = { forFoundset: 'myfoundset', w: false };
            realClientValue = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetLinkedType.TYPE_NAME),
                                                undefined, undefined, undefined, propertyContext);
            realClientValue.getInternalState().setChangeListener((doNotPush?: boolean) => {
                changeNotified = true;
            });

            serverValue = {
                forFoundset: 'myfoundset',
                vp: [10643, 10702, 10835, 10952, 11011, 11081]
            };
            realClientValue = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetLinkedType.TYPE_NAME),
                                                realClientValue, undefined, undefined, propertyContext);

            expect(getAndClearNotified()).toEqual(false);
            expect(realClientValue.getInternalState().hasChanges()).toEqual(false);
            expect(realClientValue).toEqual([10643, 10702, 10835, 10952, 11011, 11081]);

        });

        it('Should not auto-send value updates for when pushToServer is ALLOW, but it should send explicit changes via api', () => {
            serverValue = { forFoundset: 'myfoundset', w: false, vp: [10643, 10702, 10835, 10952, 11011, 11081] };
            realClientValue = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetLinkedType.TYPE_NAME),
                                                undefined, undefined, undefined, propertyContext);
            realClientValue.getInternalState().setChangeListener((doNotPush?: boolean) => {
                changeNotified = true;
            });

            // ALLOW, so it should not auto-detect this change
            realClientValue[3] = 101;
            expect(getAndClearNotified()).toEqual(false);

            // ALLOW, so explicit changes should be sent
            realClientValue.dataChanged(3, 1010101010);
            expect(getAndClearNotified()).toEqual(true);
            expect(realClientValue.getInternalState().hasChanges()).toEqual(true);
            expect(converterService.convertFromClientToServer(realClientValue, typesRegistry.getAlreadyRegisteredType(FoundsetLinkedType.TYPE_NAME), realClientValue, propertyContext)[0]).toEqual(
                [{ viewportDataChanged: { _svyRowId: 'ha ha', value: 1010101010 } }]
            );

            expect(getAndClearNotified()).toEqual(false);
            expect(realClientValue.getInternalState().hasChanges()).toEqual(false);
        });

    });

    describe('foundsetLinked_property with dumb values and simple values suite; pushToServer set to SHALLOW', () => {
        beforeEach(() => {
            const myfoundset = {
                serverSize: 0,
                selectedRowIndexes: [],
                multiSelect: false,
                viewPort:
                {
                    startIndex: 0,
                    size: 2,
                    rows: [{ _svyRowId: 'bla bla' }, { _svyRowId: 'har har' }]
                }
            };
            const fs = converterService.convertFromServerToClient(myfoundset, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), undefined, undefined, undefined, undefined);
            propertyContext = new PropertyContext((prop) => ({ myfoundset: fs }[prop]), PushToServerEnum.SHALLOW, true);

            serverValue = { forFoundset: 'myfoundset', w: false };

            realClientValue = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetLinkedType.TYPE_NAME),
                                                undefined, undefined, undefined, propertyContext);
            realClientValue.getInternalState().setChangeListener((doNotPush?: boolean) => {
                changeNotified = true;
            });

            serverValue = { sv: ':) --- static string ***' };
            realClientValue = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetLinkedType.TYPE_NAME),
                                                realClientValue, undefined, undefined, propertyContext);

            expect(getAndClearNotified()).toEqual(false);
            expect(realClientValue.getInternalState().hasChanges()).toEqual(false);
            expect(realClientValue).toEqual([':) --- static string ***', ':) --- static string ***']);

        });

        it('Should send value updates for when pushToServer is SHALLOW', () => {
            serverValue = { forFoundset: 'myfoundset', w: false };
            realClientValue = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetLinkedType.TYPE_NAME),
                                                undefined, undefined, undefined, propertyContext);
            realClientValue.getInternalState().setChangeListener((doNotPush?: boolean) => {
                changeNotified = true;
            });

            realClientValue.dataChanged(0, 'I am really changed and I should be sent');
            expect(getAndClearNotified()).toEqual(true);
            expect(realClientValue.getInternalState().hasChanges()).toEqual(true);
            expect(converterService.convertFromClientToServer(realClientValue, typesRegistry.getAlreadyRegisteredType(FoundsetLinkedType.TYPE_NAME), realClientValue, propertyContext)[0]).toEqual(
                [{ propertyChange: 'I am really changed and I should be sent' }]
            );

            expect(getAndClearNotified()).toEqual(false);
            expect(realClientValue.getInternalState().hasChanges()).toEqual(false);
        });

        it('Should not send value updates for when pushToServer is not specified', () => {
            serverValue = { forFoundset: 'myfoundset', w: false, vp: [10643, 10702, 10835, 10952, 11011, 11081] };
            realClientValue = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetLinkedType.TYPE_NAME),
                                                undefined, undefined, undefined, propertyContext);
            realClientValue.getInternalState().setChangeListener((doNotPush?: boolean) => {
                changeNotified = true;
            });

            realClientValue[1] = 'I am changed';
            expect(getAndClearNotified()).toEqual(true);
            expect(realClientValue.getInternalState().hasChanges()).toEqual(true);
            expect(converterService.convertFromClientToServer(realClientValue, typesRegistry.getAlreadyRegisteredType(FoundsetLinkedType.TYPE_NAME), realClientValue, propertyContext)[0]).toEqual(
                [{ viewportDataChanged: { _svyRowId: 'har har', value: 'I am changed' } }]
            );

            expect(getAndClearNotified()).toEqual(false);
            expect(realClientValue.getInternalState().hasChanges()).toEqual(false);
        });
    });

});
