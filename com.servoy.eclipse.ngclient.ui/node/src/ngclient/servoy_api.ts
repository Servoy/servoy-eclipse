
import { FormService, FormCache, StructureCache, ComponentCache } from '../ngclient/form.service';
import { ServoyService } from '../ngclient/servoy.service'
import { Observable, of } from 'rxjs';

export class ServoyApi {
    constructor( private item: ComponentCache, private formname: string, private absolute: boolean, private formservice: FormService, private servoyService: ServoyService ) {
    }


    public formWillShow( formname, relationname?, formIndex ?) {
        return this.formservice.formWillShow( formname, true, this.formname, this.item.name, relationname, formIndex );
    }

    public hideForm( formname, relationname?, formIndex?, formNameThatWillShow?, relationnameThatWillBeShown?, formIndexThatWillBeShown? ):Promise<boolean> {
        return this.formservice.hideForm( formname, this.formname, this.item.name, relationname, formIndex, formNameThatWillShow, relationnameThatWillBeShown, formIndexThatWillBeShown );
    }

    public startEdit( propertyName ) {
        this.formservice.pushEditingStarted(this.formname, this.item.name, propertyName);
    }

    public apply( propertyName ) {
        // TODO is this ever needed now? this is now always done through EventEmitter ....
        //        $servoyInternal.pushDPChange( "${name}", this.item.name, propertyName );
    }

    public callServerSideApi( methodName, args ) {
        // TODO implement
        //        return $servoyInternal.callServerSideApi( "${name}", this.item.name, methodName, args );
    }

    public getFormComponentElements( propertyName, formComponentValue ) { }

    public isInDesigner() {
        return false;
    }

    public trustAsHtml() {
        if ( this.item.model && this.item.model.clientProperty && this.item.model.clientProperty.trustDataAsHtml ) {
            return this.item.model.clientProperty.trustDataAsHtml;
        }
        return this.servoyService.getUIProperties().getUIProperty( "trustDataAsHtml" );
    }

    public isInAbsoluteLayout() {
        return this.absolute;
    }

    public getMarkupId() {
        return this.item.model.svyMarkupId;
    }

    public getApiData() {
        return this.item.model.valuelistID;
    }

    public getDataProviderID(): Observable<number> {
        return of(this.item.model.dataProviderID);
    }
}