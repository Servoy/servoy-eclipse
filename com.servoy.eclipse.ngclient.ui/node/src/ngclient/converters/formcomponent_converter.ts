import { ChangeAwareState, ConverterService, IChangeAwareValue, instanceOfChangeAwareValue } from '../../sablo/converter.service';
import { ChildComponentPropertyValue, ComponentType } from './component_converter';
import { IType, IPropertyContext, TypesRegistry,  } from '../../sablo/types_registry';

export class FormcomponentType implements IType<FormComponentValue> {

    public static readonly TYPE_NAME = 'formcomponent';

    constructor(private converterService: ConverterService, private typesRegistry: TypesRegistry) {
    }

    fromServerToClient(serverSentData: any, currentClientData: FormComponentValue, propertyContext: IPropertyContext): FormComponentValue {
        let formComponentPropertyValue = currentClientData;
        if (!formComponentPropertyValue) formComponentPropertyValue = new FormComponentValue(serverSentData.absoluteLayout,
                                            serverSentData.childElements,
                                            serverSentData.formHeight,
                                            serverSentData.formWidth,
                                            serverSentData.startName,
                                            serverSentData.useCssPosition,
                                            serverSentData.uuid);

        if (serverSentData.childElements) {
            if (!formComponentPropertyValue.childElements) formComponentPropertyValue.childElements = [];
            else formComponentPropertyValue.childElements.length = formComponentPropertyValue.childElements.length;

            for (let idx = 0; idx < serverSentData.childElements.length; idx++) {
                let childCompElem: ChildComponentPropertyValue;

                formComponentPropertyValue.childElements[idx] = childCompElem = this.converterService.convertFromServerToClient(serverSentData.childElements[idx],
                        this.typesRegistry.getAlreadyRegisteredType(ComponentType.TYPE_NAME),
                        currentClientData && currentClientData.childElements ? currentClientData.childElements[idx] : undefined,
                        undefined, undefined, propertyContext);

                if (instanceOfChangeAwareValue(childCompElem)) {
                    childCompElem.getInternalState().setChangeListener((doNotPush?: boolean) => {
                        formComponentPropertyValue.notifyChangeListener(doNotPush);
                    });
                }
            }
        }

        return formComponentPropertyValue;
    }

    fromClientToServer(newClientData: FormComponentValue, oldClientData: FormComponentValue, propertyContext: IPropertyContext): [any, FormComponentValue] | null {
        if (!newClientData) return null;
        // only childElements are pushed.
        let changes = null;
        if (newClientData.childElements) {
            changes = [];
            for (let idx = 0; idx < newClientData.childElements.length; idx++) {
                changes[idx] = this.converterService.convertFromClientToServer(newClientData.childElements[idx],
                        this.typesRegistry.getAlreadyRegisteredType(ComponentType.TYPE_NAME),
                        oldClientData && oldClientData.childElements ? oldClientData.childElements[idx] : null, propertyContext);
            }
        }
        return [changes, newClientData];
    }

}

export class FormComponentValue extends ChangeAwareState implements IChangeAwareValue {

    conversionInfo: any = {};
    constructor(
        public absoluteLayout: boolean,
        public childElements: ChildComponentPropertyValue[],
        public formHeight: number,
        public formWidth: number,
        public startName: string,
        public useCssPosition: boolean,
        public uuid: string) {
        super();
    }

    getInternalState(): FormComponentValue {
        return this;
    }

}

