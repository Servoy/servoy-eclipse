import { Component, Renderer2, SimpleChanges, ChangeDetectorRef, ChangeDetectionStrategy} from '@angular/core';

import {FormattingService} from '../../ngclient/servoy_public';

import {ServoyDefaultBaseField} from  '../basefield';

@Component( {
    selector: 'servoydefault-imagemedia',
    templateUrl: './imagemedia.html',
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyDefaultImageMedia extends ServoyDefaultBaseField<HTMLDivElement> {

    public static readonly EMPTY = 'data:image/gif;base64,R0lGODlhEAAQALMAAAAAAP///1dzWYCcgoOehZq1nKfCqbnVu8biyPLy8s/Pz3p6emxsbA0NDcDAwAAAACH5BAEAAA4ALAAAAAAQABAAAAQR0MlJq7046827/2AojmRpThEAOw==';
    public static readonly NOT_EMPTY = 'data:image/gif;base64,R0lGODlhMgAQAKECAAAAAAAAhP///////yH5BAEKAAMALAAAAAAyABAAAAJOnI+py+0PFZg02muA2GLi72icBoBmQpLByp6gyAWcsLpYOskxbVvwJpvQWr0HDqCb8YohZZCiCTBDUKQSOL08KcssJDmTer+ssniMTjcKADs=';

    imageURL = ServoyDefaultImageMedia.EMPTY;
    increment = 0;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef ,
                formattingService: FormattingService) {
        super(renderer, cdRef, formattingService);
    }

    deleteMedia(): void {
        this.dataProviderID = null;
        this.pushUpdate();
        this.imageURL = ServoyDefaultImageMedia.EMPTY;
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
        if (changes.dataProviderID) this.updateImageURL(changes.dataProviderID.currentValue);
    }

    private updateImageURL(dp) {
        if (dp != null && dp !== '') {
            const contentType = dp.contentType;
            if (contentType != null && contentType !== undefined && contentType.indexOf('image') == 0) {
                this.imageURL = dp.url;
            } else {
                this.imageURL = ServoyDefaultImageMedia.NOT_EMPTY;
            }
        }
    }

    setCss(element) {
        const alignStyle = { top: '0px', left: '0px' };
        const imageHeight = element.clientHeight;
        const imageWidth = element.clientWidth;
        // vertical align cennter
        const height = element.parentNode['clientHeight'];
        if (height > imageHeight)
            alignStyle.top = (height - imageHeight) / 2 + 'px';
        // horizontal align (default left)
        const width = element.parentNode['clientWidth'];
        if (width > imageWidth) {
            if (this.horizontalAlignment === 0 /*SwingConstants.CENTER*/)
                alignStyle.left = (width - imageWidth) / 2 + 'px';
            else if (this.horizontalAlignment === 4 /*SwingConstants.RIGHT*/)
                alignStyle.left = (width - imageWidth) + 'px';
            else {
                if ((element.parentNode.childNodes.length > 1) && (imageHeight + 34 < height))
                    alignStyle.left = '51px';
            }
        }
        this.renderer.setStyle(element, 'top', alignStyle.top);
        this.renderer.setStyle(element, 'left', alignStyle.left);
    }
}

