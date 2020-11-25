import { IConverter, PropertyContext, ConverterService } from '../../sablo/converter.service';
import { ChangeAwareState, IChangeAwareValue, IFormComponentType, instanceOfChangeAwareValue,  } from '../../sablo/spectypes.service';
import { ComponentState, ComponentType } from './component_converter';

export class FormcomponentConverter implements IConverter {

    constructor( private converterService: ConverterService) {       
    } 
    fromServerToClient(serverSentData: FormComponentState, currentClientData: FormComponentType, propertyContext: PropertyContext): IFormComponentType {
        let conversionInfo = null;
        let realValue = currentClientData;
        let state: FormComponentState = new FormComponentState(serverSentData.absoluteLayout,
            serverSentData.childElements,
            serverSentData.formHeight,
            serverSentData.formWidth,
            serverSentData.startName,
            serverSentData.svy_types, 
            serverSentData.useCssPosition, 
            serverSentData.uuid);
        if ( realValue == null ) {
            realValue = new FormComponentType(state); 
        }
        if ( serverSentData[ConverterService.TYPES_KEY] ) {
            conversionInfo = serverSentData[ConverterService.TYPES_KEY];
        }
        if ( conversionInfo ) {
            for ( const key in conversionInfo ) {
                let elem = serverSentData[key]; 
                state.conversionInfo[key] = conversionInfo[key];
                realValue[key] = elem = this.converterService.convertFromServerToClient( elem, conversionInfo[key], currentClientData ? currentClientData[key] : undefined, propertyContext );
                
                if (instanceOfChangeAwareValue(elem)) {
                    // child is able to handle it's own change mechanism
                    elem.getStateHolder().setChangeListener(() => {
                      state.notifyChangeListener();
                    });
                }
                if ( key == 'childElements' && elem ) {
                    for ( let i = 0; i < elem.length; i++ ) {
                        const comp = elem[i];
                        if (instanceOfChangeAwareValue(comp)) {
                            comp.getStateHolder().setChangeListener(() => {
                              state.notifyChangeListener();
                            });
                        }
                    }
                }
            }
        }
        return realValue;
    }

    fromClientToServer(newClientData: FormComponentType, oldClientData: FormComponentType): Object {
        if ( !newClientData ) return null;
        // only childElements are pushed.
        const internalState = newClientData.getStateHolder();
        let changes = this.converterService.convertFromClientToServer( newClientData["childElements"], internalState.conversionInfo["childElements"], oldClientData ? oldClientData["childElements"] : null );
        return changes;
    }
}

class FormComponentState extends ChangeAwareState {

    conversionInfo: any = {};
    constructor(
        public absoluteLayout: boolean,
        public childElements: ComponentType[], 
        public formHeight: number,
        public formWidth: number,
        public startName: string,
        public svy_types: any[],
        public useCssPosition: boolean,
        public uuid: string) {
        super();
    }
}

export class FormComponentType implements IFormComponentType, IChangeAwareValue {

    constructor(private state: FormComponentState) {}

    getStateHolder(): FormComponentState {
        return this.state;
    }
}
