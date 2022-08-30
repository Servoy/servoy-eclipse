import { ServoyBaseComponent } from "./basecomponent";

export abstract class ServoyApi {
    public abstract formWillShow(formname: string, relationname?: string, formIndex?: number): Promise<boolean>;
    public abstract hideForm(formname: string, relationname?: string, formIndex?: number,
        formNameThatWillShow?: string, relationnameThatWillBeShown?: string, formIndexThatWillBeShown?: number): Promise<boolean>;

    public abstract startEdit(propertyName: string);

    public abstract apply(propertyName: string, value: any);

    public abstract callServerSideApi(methodName: string, args: Array<any>);

    public abstract getFormComponentElements(_propertyName: string, _formComponentValue: any);

    public abstract isInDesigner(): boolean;

    public abstract trustAsHtml(): boolean;

    public abstract isInAbsoluteLayout(): boolean;
    public abstract getMarkupId(): string;

    public abstract getFormName(): string;

    public abstract registerComponent(component: ServoyBaseComponent<any>);

    public abstract unRegisterComponent(component: ServoyBaseComponent<any>);

    public abstract getClientProperty(key: string): any;
}
