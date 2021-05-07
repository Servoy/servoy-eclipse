import { Component, ChangeDetectorRef, ViewChild, SimpleChanges, Renderer2, Input, ChangeDetectionStrategy } from '@angular/core';
import { BaseCustomObject, IFoundset, ServoyBaseComponent } from '@servoy/public';
import { NgbCarouselConfig, NgbCarousel } from '@ng-bootstrap/ng-bootstrap';

@Component( {
    selector: 'bootstrapextracomponents-carousel',
    templateUrl: './carousel.html',
    styleUrls: ['./bts-extra-carousel.css'],
    providers: [NgbCarouselConfig],
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyBootstrapExtraCarousel extends ServoyBaseComponent<HTMLDivElement> {

    @ViewChild( 'ngCarousel', { static: true } ) ngCarousel: NgbCarousel;

    @Input() onSlideClicked: ( e: Event, slide: Slide ) => void;
    @Input() cycleInterval: number;
    @Input() noPause: boolean;
    @Input() noTransition: boolean;
    @Input() slides: Array<Slide>;
    @Input() slidesFoundset: IFoundset;
    @Input() lazyLoading: boolean;
    @Input() imageOptions: string;
    @Input() visible: boolean;
    @Input() size: { width: number; height: number };
    @Input() location: any;
    @Input() styleClass: string;
    @Input() imageCssInternal: any;
    @Input() imageCss: Array<CssProperty>;
    @Input() responsiveHeight: number;

    innerSlides: Array<Slide>;

    loadingImage = 'bootstrapextracomponents/carousel/resources/loading.gif';
    missingImage = 'bootstrapextracomponents/carousel/resources/missing.png';

    constructor( renderer: Renderer2, cdRef: ChangeDetectorRef, _config: NgbCarouselConfig ) {
        super( renderer, cdRef );
    }

    svyOnInit() {
        super.svyOnInit();
    }

    svyOnChanges( changes: SimpleChanges ) {
        if ( changes ) {
            if ( changes.slidesFoundset ) {
                this.createSlides();
            }
            if ( changes.imageOptions ) {
                const currentValue = changes.imageOptions.currentValue;
                if ( currentValue === 'Reduce' && !this.imageCss ) {
                    this.imageCssInternal = {
                        'max-width': '100%',
                        'max-height': '100%',
                        width: this.size ? this.size.width + 'px' : null,
                        height: this.size ? this.size.height + 'px' : null,
                        'object-fit': 'contain'
                    };
                }
                if ( currentValue === 'Reduce/Enlarge' && !this.imageCss ) {
                    this.imageCssInternal = {
                        width: this.size ? this.size.width + 'px' : null,
                        height: this.size ? this.size.height + 'px' : null
                    };
                }
                if ( currentValue === 'Crop' && !this.imageCss ) {
                    this.imageCssInternal = {
                        width: 'auto',
                        height: 'auto'
                    };
                }
                if ( currentValue === 'Scale to fit' && !this.imageCss ) {
                    this.imageCssInternal = {
                        'max-width': '100%',
                        'max-height': '100%',
                        width: this.size ? this.size.width + 'px' : null || ( this.servoyApi.isInAbsoluteLayout() ? this.size.width + 'px' : '100%' ),
                        height: this.size ? this.size.height + 'px' : null || ( this.servoyApi.isInAbsoluteLayout() ? this.size.height + 'px' : this.responsiveHeight + 'px' ),
                        'object-fit': 'contain'
                    };
                }

            }
            if ( changes.imageCss ) {
                if ( changes.imageCss.currentValue !== undefined ) {
                    for ( const cssEntry of this.imageCss) {
                        this.imageCssInternal[cssEntry.propertyName] = cssEntry.propertyValue;
                    }
                }
            }
        }
        super.svyOnChanges( changes );
    }

    getSelectedIndex() {
        const activeId = this.ngCarousel.activeId;
        return parseInt( activeId.substr( activeId.length - 1 ), 10 );
    }

    setSelectedIndex( index: number ) {
        this.ngCarousel.activeId = 'ngb-slide-' + index;
        this.ngCarousel.select( this.ngCarousel.activeId );
    }

    getStyle(): any {
        const layoutStyle: any = {};
        if ( !this.servoyApi.isInAbsoluteLayout() ) {
            layoutStyle.height = this.responsiveHeight;
        }
        return layoutStyle;
    }

    private createSlides = () => {
        this.innerSlides = [];
        if ( this.slidesFoundset !== null && this.slidesFoundset !== undefined ) {
            for ( const row of this.slidesFoundset.viewPort.rows ) {
                const slide = new Slide();
                slide.imageUrl = row.image && row.image.url ? row.image.url : null;
                slide.caption = row.caption ? row.caption : null;
                this.innerSlides.push( slide );
            }
        } else {
            if ( this.slides !== null && this.slides !== undefined ) {
                this.innerSlides = this.slides;
            }
        }
    };
}

export class Slide extends BaseCustomObject {
    public imageUrl: string;
    public caption: string;
}

class CssProperty extends BaseCustomObject {
    public propertyName: string;
    public propertyValue: string;
}
