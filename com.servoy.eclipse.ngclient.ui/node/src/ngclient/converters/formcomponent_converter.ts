import { IConverter, PropertyContext, ConverterService } from '../../sablo/converter.service';
import { ChangeAwareState, IChangeAwareValue, instanceOfChangeAwareValue,  } from '../../sablo/spectypes.service';
import { ComponentModel } from './component_converter';

export class FormcomponentConverter implements IConverter {

    constructor( private converterService: ConverterService) {
    }
    fromServerToClient(serverSentData: any, currentClientData: FormComponentState, propertyContext: PropertyContext): FormComponentState {
        let conversionInfo = null;
        let state = currentClientData;
        if ( state == null ) {
            state = new FormComponentState(serverSentData.absoluteLayout,
            serverSentData.childElements,
            serverSentData.formHeight,
            serverSentData.formWidth,
            serverSentData.startName,
            serverSentData[ConverterService.TYPES_KEY],
            serverSentData.useCssPosition,
            serverSentData.uuid);
        }
        if ( serverSentData[ConverterService.TYPES_KEY] ) {
            conversionInfo = serverSentData[ConverterService.TYPES_KEY];
        }
        if ( conversionInfo ) {
            for ( const key of Object.keys(conversionInfo) ) {
                let elem = serverSentData[key];
                state.conversionInfo[key] = conversionInfo[key];
                state[key] = elem = this.converterService.convertFromServerToClient( elem, conversionInfo[key], currentClientData ? currentClientData[key] : undefined, propertyContext );

                if (instanceOfChangeAwareValue(elem)) {
                    // child is able to handle it's own change mechanism
                    elem.getStateHolder().setChangeListener(() => {
                      state.notifyChangeListener();
                    });
                }
                if ( key === 'childElements' && elem ) {
                    for ( const i of Object.keys(elem)) {
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
        return state;
    }

    fromClientToServer(newClientData: FormComponentState, oldClientData: FormComponentState): any {
        if ( !newClientData ) return null;
        // only childElements are pushed.
        const formState = newClientData.getStateHolder();
        const changes = this.converterService.convertFromClientToServer( newClientData['childElements'], formState.conversionInfo['childElements'],
                                                                            oldClientData ? oldClientData['childElements'] : null );
        return changes;
    }
}

export class FormComponentState extends ChangeAwareState implements IChangeAwareValue {

    conversionInfo: any = {};
    constructor(
        public absoluteLayout: boolean,
        public childElements: ComponentModel[],
        public formHeight: number,
        public formWidth: number,
        public startName: string,
        public svyTypes: any[],
        public useCssPosition: boolean,
        public uuid: string) {
        super();
    }

    getStateHolder(): FormComponentState {
        return this;
    }
}

