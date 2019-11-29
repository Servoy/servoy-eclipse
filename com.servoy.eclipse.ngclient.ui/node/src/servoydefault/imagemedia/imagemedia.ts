import { Component, Renderer2, OnInit, OnChanges, SimpleChanges} from '@angular/core';

import {FormattingService} from '../../ngclient/servoy_public'

import {ServoyDefaultBaseField} from  '../basefield'
import { ApplicationService } from "../../ngclient/services/application.service";

@Component( {
    selector: 'servoydefault-imagemedia',
    templateUrl: './imagemedia.html'
} )
export class ServoyDefaultImageMedia extends ServoyDefaultBaseField implements OnInit, OnChanges {
  
    imageURL: string = "servoydefault/imagemedia/res/images/empty.gif";
    
    constructor(renderer: Renderer2, 
                formattingService : FormattingService) {
        super(renderer,formattingService);
    }
    
    deleteMedia(): void {
        this.update(null);
        this.imageURL = "servoydefault/imagemedia/res/images/empty.gif";
    }
    
    downloadMedia(): void {
        if (this.dataProviderID) {
            let x = window.screenTop + 100;
            let y = window.screenLeft + 100;
            window.open(this.dataProviderID.url ? this.dataProviderID.url : this.dataProviderID, 'download', 'top=' + x + ',left=' + y + ',screenX=' + x
                    + ',screenY=' + y + ',location=no,toolbar=no,menubar=no,width=310,height=140,resizable=yes');
        }
    }
    
    ngOnInit() {
        super.ngOnInit();
        this.updateImageURL(this.dataProviderID);
    }
    
    ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);
        this.updateImageURL(changes.dataProviderID.currentValue);
    }
    
    private updateImageURL(dp) {
        if(dp != null) {
            let contentType = dp.contentType;
            if (contentType != null && contentType != undefined && contentType.indexOf("image") == 0) {
                this.imageURL = dp.url;
            } else {
                this.imageURL = "servoydefault/imagemedia/res/images/notemptymedia.gif";
            }
        }
    }
}

