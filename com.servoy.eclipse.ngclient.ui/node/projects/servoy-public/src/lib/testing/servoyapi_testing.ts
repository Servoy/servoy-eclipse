import { ServoyBaseComponent } from "../basecomponent";
import { ServoyApi } from "../servoy_api";

export class ServoyApiTesting implements ServoyApi {
    public formWillShow(formname: string, relationname?: string, formIndex?: number): Promise<boolean> {
        return Promise.resolve(true);
    }
    public hideForm(formname: string, relationname?: string, formIndex?: number, formNameThatWillShow?: string, relationnameThatWillBeShown?: string, formIndexThatWillBeShown?: number): Promise<boolean> {
        return Promise.resolve(true);
    }
    public startEdit(propertyName: string) {
        
    }
    public apply(propertyName: string, value: unknown) {
       
    }
    public callServerSideApi(methodName: string, args: Array<unknown>) {
        return Promise.resolve(null);
    }
    public isInDesigner(): boolean {
       return false;
    }
    public trustAsHtml(): boolean {
        return false;
    }
    public isInAbsoluteLayout(): boolean {
       return true;
    }
    public getMarkupId(): string {
        return 'testid';
    }
    public getFormName(): string {
        return 'testform';
    }
    public registerComponent(component: ServoyBaseComponent<HTMLElement>) {
        
    }
    public unRegisterComponent(component: ServoyBaseComponent<HTMLElement>) {
        
    }
    public getClientProperty(key: string) {
        return key;
    }

}