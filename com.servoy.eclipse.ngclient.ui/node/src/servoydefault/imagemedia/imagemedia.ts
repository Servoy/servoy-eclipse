import { Component, Renderer2, SimpleChanges, ChangeDetectorRef} from '@angular/core';

import {FormattingService} from '../../ngclient/servoy_public';

import {ServoyDefaultBaseField} from  '../basefield';

@Component( {
    selector: 'servoydefault-imagemedia',
    templateUrl: './imagemedia.html'
} )
export class ServoyDefaultImageMedia extends ServoyDefaultBaseField {

    imageURL = 'servoydefault/imagemedia/res/images/empty.gif';
    increment = 0;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef ,
                formattingService: FormattingService) {
        super(renderer, cdRef, formattingService);
    }

    deleteMedia(): void {
        this.dataProviderID = null;
        this.pushUpdate();
        this.imageURL = 'servoydefault/imagemedia/res/images/empty.gif';
    }

    downloadMedia(): void {
        if (this.dataProviderID) {
            const x = window.screenTop + 100;
            const y = window.screenLeft + 100;
            window.open(this.dataProviderID.url ? this.dataProviderID.url : this.dataProviderID, 'download', 'top=' + x + ',left=' + y + ',screenX=' + x
                    + ',screenY=' + y + ',location=no,toolbar=no,menubar=no,width=310,height=140,resizable=yes');
        }
    }

    svyOnInit() {
        super.svyOnInit();
        this.updateImageURL(this.dataProviderID);
    }

    svyOnChanges(changes: SimpleChanges): void {
        super.svyOnChanges(changes);
        this.updateImageURL(changes.dataProviderID.currentValue);
    }

    private updateImageURL(dp) {
        if (dp != null && dp != '') {
            const contentType = dp.contentType;
            if (contentType != null && contentType != undefined && contentType.indexOf('image') == 0) {
                this.imageURL = dp.url;
            } else {
                this.imageURL = 'servoydefault/imagemedia/res/images/notemptymedia.gif';
            }
        }
    }
}

