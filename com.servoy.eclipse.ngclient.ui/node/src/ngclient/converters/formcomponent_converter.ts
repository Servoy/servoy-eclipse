import { IConverter, PropertyContext, ConverterService } from '../../sablo/converter.service';
import { IFormComponentType } from '../../sablo/spectypes.service';
import { ComponentType } from './component_converter';

export class FormcomponentConverter implements IConverter {

    constructor( private converterService: ConverterService) {       
    }

    fromServerToClient(serverSentData: any, currentClientData: FormComponentType, propertyContext: PropertyContext): IFormComponentType {
        let conversionInfo = null;
        let realValue = currentClientData;
        if ( realValue == null ) {
            realValue = new FormComponentType(serverSentData.absoluteLayout,
                serverSentData.childElements,
                serverSentData.formHeight,
                serverSentData.formWidth,
                serverSentData.startName,
                serverSentData.svy_types,
                serverSentData.useCssPosition,
                serverSentData.uuid);
            this.initializeNewValue(realValue);
        }
        if ( serverSentData[ConverterService.TYPES_KEY] ) {
            conversionInfo = serverSentData[ConverterService.TYPES_KEY];
        }
        if ( conversionInfo ) {
            for ( let key in conversionInfo ) {
                let elem = serverSentData[key];

                const internalState = realValue[ConverterService.INTERNAL_IMPL];
                internalState.conversionInfo[key] = conversionInfo[key];
                realValue[key] = elem = this.converterService.convertFromServerToClient( elem, conversionInfo[key], currentClientData ? currentClientData[key] : undefined, propertyContext );

                if ( elem && elem[ConverterService.INTERNAL_IMPL] && elem[ConverterService.INTERNAL_IMPL].setChangeNotifier ) {
                    // child is able to handle it's own change mechanism
                    elem[ConverterService.INTERNAL_IMPL].setChangeNotifier( this.getChangeNotifier( realValue ) );
                }
                if ( key == "childElements" && elem ) {
                    for ( let i = 0; i < elem.length; i++ ) {
                        var comp = elem[i];
                        if ( comp && comp[ConverterService.INTERNAL_IMPL] && comp[ConverterService.INTERNAL_IMPL].setChangeNotifier ) {
                            // child is able to handle it's own change mechanism
                            comp[ConverterService.INTERNAL_IMPL].setChangeNotifier( this.getChangeNotifier( realValue ) );
                        }
                    }
                }
            }
        }
        return realValue;
    }

    fromClientToServer(newClientData: Object, oldClientData: Object): Object {
        if ( !newClientData ) return null;
        // only childElements are pushed.
        const internalState = newClientData[ConverterService.INTERNAL_IMPL]
        let changes = this.converterService.convertFromClientToServer( newClientData["childElements"], internalState.conversionInfo["childElements"], oldClientData ? oldClientData["childElements"] : null );
        return changes;
    }

    /** Initializes internal state on a new array value */
    private initializeNewValue( newValue ) {
        var newInternalState = false; // TODO although unexpected (internal state to already be defined at this stage it can happen until SVY-8612 is implemented and property types change to use that
        if ( !newValue.hasOwnProperty( ConverterService.INTERNAL_IMPL ) ) {
            newInternalState = true;
            this.converterService.prepareInternalState(newValue, {});
        } // else: we don't try to redefine internal state if it's already defined

        var internalState = newValue[ConverterService.INTERNAL_IMPL];

        if ( newInternalState ) {
            // implement what $sabloConverters need to make this work
            internalState.setChangeNotifier = function( changeNotifier ) {
                internalState.changeNotifier = changeNotifier;
            }
            internalState.isChanged = function() {
                var hasChanges = internalState.allChanged;
                //				if (!hasChanges) for (var x in internalState.changedIndexes) { hasChanges = true; break; }
                return hasChanges;
            }

            // private impl
            internalState.conversionInfo = [];
            internalState.allChanged = false;
        } // else don't reinitilize it - it's already initialized
    }

    private getChangeNotifier( propertyValue ) {
        return function() {
            var internalState = propertyValue[ConverterService.INTERNAL_IMPL];
            internalState.changeNotifier();
        }
    }
}

export class FormComponentType implements IFormComponentType {

    constructor(
        public absoluteLayout: boolean,
        public childElements: ComponentType[],
        public formHeight: number,
        public formWidth: number,
        public startName: string,
        public svy_types: any[],
        public useCssPosition: boolean,
        public uuid: string) {
        }
}