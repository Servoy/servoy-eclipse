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

    public apply( _propertyName: string ) {
        // TODO is this ever needed now? this is now always done through EventEmitter ....
        //        $servoyInternal.pushDPChange( "${name}", this.item.name, propertyName );
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

    public getFormname(): string {
        return this.formname;
    }

    public registerComponent(_component: ServoyBaseComponent<any>) {
        // these are overwritten by components that needs this.
    }

    public unRegisterComponent(_component: ServoyBaseComponent<any>) {
        // these are overwritten by components that needs this.
    }
}

