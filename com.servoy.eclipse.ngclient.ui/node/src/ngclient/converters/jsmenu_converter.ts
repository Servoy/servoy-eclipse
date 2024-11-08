import { IType, IPropertyContext } from '../../sablo/types_registry';
import { ConverterService, ChangeAwareState, IChangeAwareValue } from '../../sablo/converter.service';
import { IJSMenu, IJSMenuItem } from '@servoy/public';

export class JSMenuType implements IType<any> {

    public static readonly TYPE_NAME = 'JSMenu';

    constructor(private readonly converterService: ConverterService<unknown>) { }

    fromServerToClient(serverJSONValue: any, currentClientValue: any, propertyContext: IPropertyContext): JSMenu {
        if (serverJSONValue.items) {
            for (const i in serverJSONValue.items) {
                if (serverJSONValue.items[i].extraProperties) {
                    for (const category in serverJSONValue.items[i].extraProperties) {
                        if (serverJSONValue.items[i].extraProperties[category]) {
                            for (const property in serverJSONValue.items[i].extraProperties[category]) {
                                if (serverJSONValue.items[i].extraProperties[category][property] instanceof Object && serverJSONValue.items[i].extraProperties[category][property][ConverterService.CONVERSION_CL_SIDE_TYPE_KEY] !== undefined) {
                                    serverJSONValue.items[i].extraProperties[category][property] = this.converterService.convertFromServerToClient(serverJSONValue.items[i].extraProperties[category][property], undefined,
                                        currentClientValue?.items[i]?.extraProperties[category][property], undefined, undefined, propertyContext);
                                }
                            }
                        }
                    }
                }
            }
        }
        return new JSMenu(serverJSONValue, new MenuState());
    }

    fromClientToServer(newClientData: JSMenu, oldClientData: JSMenu, propertyContext: IPropertyContext): [any, JSMenu] | null {
       if (newClientData) {
            const newDataInternalState = newClientData.getInternalState();
            if (newDataInternalState.pushValueRequest) {
                const tmp = newDataInternalState.pushValueRequest;
                delete newDataInternalState.pushValueRequest;
                return [tmp, newClientData];
            }
        }
        return null; // should never happen
    }

}

export class JSMenu implements IJSMenu, IChangeAwareValue {
    items : IJSMenuItem[];
    name: string;
    styleClass: string;
    
    constructor(private serverJSONValue: any, private internalState: MenuState) {
        this.items = serverJSONValue.items;
        this.name = serverJSONValue.name;
        this.styleClass = serverJSONValue.styleClass;
    }
    
    getInternalState(): MenuState {
        return this.internalState;
    }
    
     pushDataProviderValue(category: string, propertyName: string, itemIndex: number, dataproviderValue: any): void {
        this.internalState.pushValueRequest = {
            category,
            propertyName,
            itemIndex,
            dataproviderValue
        };
        this.internalState.notifyChangeListener();
    }

}
    
class MenuState extends ChangeAwareState {
    public pushValueRequest: { category: string; propertyName: string; itemIndex: number; dataproviderValue: any; };

    hasChanges(): boolean {
        return super.hasChanges() || this.pushValueRequest !== undefined;
    }
    
    clearChanges(): void {
        super.clearChanges();
        this.pushValueRequest = undefined;
    }

}