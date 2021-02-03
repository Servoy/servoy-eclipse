import { Component, Output, ChangeDetectorRef, SimpleChanges, Renderer2, Input, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent, SvyUtilsService } from '../../ngclient/servoy_public';
import { IFoundset } from '../../sablo/spectypes.service';
import { LightboxModule, Lightbox } from 'ngx-lightbox';

@Component( {
    selector: 'servoyextra-lightboxgallery',
    templateUrl: './lightboxgallery.html',
    styleUrls: ['./lightboxgallery.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyExtraLightboxGallery extends ServoyBaseComponent<HTMLDivElement> {

    @Input() onHoverButtonClicked: ( e: Event, imageId: string ) => void;

    @Input() imagesFoundset: IFoundset;
    @Input() maxImageWidth: number;
    @Input() maxImageHeight: number;
    @Input() albumLabel: string;
    @Input() fadeDuration: number;
    @Input() fitImagesInViewport: boolean;
    @Input() imageFadeDuration: number;
    @Input() positionFromTop: number;
    @Input() resizeDuration: number;
    @Input() wrapAround: boolean;
    @Input() galleryVisible: boolean;
    @Input() showCaptionInGallery: boolean;
    @Input() showImageNumberLabel: boolean;
    @Input() hoverButtonIcon: string;
    @Input() buttonText: string;
    @Input() buttonStyleClass: string;
    @Input() enabled: boolean;

    public images: Array<any> = [];

    constructor( renderer: Renderer2, cdRef: ChangeDetectorRef, private _lightbox: Lightbox ) {
        super( renderer, cdRef );
    }


    svyOnInit() {
        super.svyOnInit();
        for ( let i = 0; i <= this.imagesFoundset.viewPort.rows.length; i++ ) {
            const row = this.imagesFoundset.viewPort.rows[i];
            const image = {
                src: row.image && row.image.url ? row.image.url : null,
                caption: row.caption ? row.caption : null,
                thumb: row.thumbnail && row.thumbnail.url ? row.thumbnail.url : null
            };

            //check if using url strings instead of media/blob
            image.src = typeof row.image == 'string' ? row.image : image.src;
            image.thumb = typeof row.thumbnail == 'string' ? row.thumbnail : image.thumb;

            if ( !image.src ) continue;
            this.images.push( image );
        }

    }

    svyOnChanges( changes: SimpleChanges ) {
        if ( changes ) {
            for ( const property of Object.keys( changes ) ) {
                const change = changes[property];
                switch ( property ) {
                    case 'styleClass':
                        if ( change.previousValue )
                            this.renderer.removeClass( this.getNativeElement(), change.previousValue );
                        if ( change.currentValue )
                            this.renderer.addClass( this.getNativeElement(), change.currentValue );
                        break;
                }
            }
        }
        super.svyOnChanges( changes );
    }

    open( index: number ): void {
        // open lightbox
        this._lightbox.open( this.images, index );
    }

    close(): void {
        // close lightbox programmatically
        this._lightbox.close();
    }
}

