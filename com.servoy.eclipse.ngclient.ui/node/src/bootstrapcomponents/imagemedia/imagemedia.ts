import { DOCUMENT } from '@angular/common';
import { Component, OnInit, Renderer2, Input, OnChanges, SimpleChanges, ChangeDetectorRef, ChangeDetectionStrategy, Inject } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';

@Component({
  selector: 'bootstrapcomponents-imagemedia',
  templateUrl: './imagemedia.html',
  styleUrls: ['./imagemedia.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyBootstrapImageMedia extends ServoyBootstrapBasefield<HTMLImageElement> {

    @Input() media;
    @Input() alternate;

    imageURL = 'bootstrapcomponents/imagemedia/images/empty.gif';

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, @Inject(DOCUMENT) doc: Document) {
        super(renderer, cdRef, doc);
    }

    svyOnChanges(changes: SimpleChanges): void {
        if (changes) {
            for ( const property of Object.keys(changes) ) {
                const change = changes[property];
                switch ( property ) {
                case 'media':
                    this.updateImageURL();
                    break;
                case 'dataProviderID':
                    this.updateImageURL();
                    break;
                }
            }
            super.svyOnChanges(changes);
        }
    }

    private updateImageURL() {
        if (this.media) {
            this.imageURL = this.media;
            // do nothing if data provider changed but media is defined
        } else if(this.dataProviderID && this.dataProviderID.url) {
            this.imageURL = this.dataProviderID.url;
        } else if (!this.dataProviderID && this.servoyApi.isInDesigner()) {
            this.imageURL = 'bootstrapcomponents/imagemedia/media.png';
        } else if (!this.dataProviderID){
            this.imageURL = 'bootstrapcomponents/imagemedia/images/empty.gif';
        } else {
            this.imageURL = this.dataProviderID;
        }
    }
}
