import { FormService } from '../ngclient/form.service';
import { ServoyService } from '../ngclient/servoy.service';
import { ServoyBaseComponent } from './servoy_public';
import { ComponentCache } from './types';

export class ServoyApi {
    constructor( private item: ComponentCache,
                 private formname: string,
                 private absolute: boolean,
                 private formservice: FormService,
                 private servoyService: ServoyService) {
    }


    public formWillShow( formname: string, relationname?: string, formIndex?: number): Promise<boolean> {
        return this.formservice.formWillShow( formname, true, this.formname, this.item.name, relationname, formIndex );
    }

    public hideForm( formname: string, relationname?: string, formIndex?: number,
                        formNameThatWillShow?: string, relationnameThatWillBeShown?: string, formIndexThatWillBeShown?: number ): Promise<boolean> {
        return this.formservice.hideForm( formname, this.formname, this.item.name, relationname, formIndex, formNameThatWillShow, relationnameThatWillBeShown, formIndexThatWillBeShown );
    }

    public startEdit( propertyName: string ) {
        this.formservice.pushEditingStarted(this.formname, this.item.name, propertyName);
    }

    /**
     * This apply is only needed for nested dataproviders, so a dataprovider property of custom type, this will push and apply the data to the data model.
     */
    public apply( propertyName: string, value: any ) {
        this.formservice.sendChanges(this.formname, this.item.name, propertyName, value, null, true);
    }

    public callServerSideApi( methodName: string, args: Array<any> ) {
        return this.formservice.callComponentServerSideApi(this.formname, this.item.name, methodName, args);
    }

    public getFormComponentElements( _propertyName: string, _formComponentValue: any ) { }

    public isInDesigner() {
        return false;
    }

    public trustAsHtml() {
        if ( this.item.model && this.item.model.clientProperty && this.item.model.clientProperty.trustDataAsHtml ) {
            return this.item.model.clientProperty.trustDataAsHtml;
        }
        return this.servoyService.getUIProperties().getUIProperty( 'trustDataAsHtml' );
    }

    public isInAbsoluteLayout(): boolean {
        return this.absolute;
    }

    public getMarkupId(): string {
        return this.item.model.svyMarkupId;
    }

    public getFormName(): string {
        return this.formname;
    }

    public registerComponent(_component: ServoyBaseComponent<any>) {
        // these are overwritten by components that needs this.
    }

    public unRegisterComponent(_component: ServoyBaseComponent<any>) {
        // these are overwritten by components that needs this.
    }
    
    public getClientProperty(key){
        if ( this.item.model && this.item.model.clientProperty ) {
            return this.item.model.clientProperty[key];
        }
        return null;
    }
}

