import { Directive, Input, SimpleChanges, OnChanges, ElementRef, Renderer2, OnDestroy } from '@angular/core';
import { ServoyBaseComponent } from '../servoy_public';
import { IViewStateListener } from '../basecomponent';

@Directive({
    selector: '[svyImageMediaId]'
})
export class ImageMediaIdDirective implements OnChanges, IViewStateListener, OnDestroy {

    @Input('svyImageMediaId') media : any;
    @Input('hostComponent') hostComponent: ServoyBaseComponent;

    private imgStyle: Map<string, any>;
    private rollOverImgStyle: Map<string, any>;
    private clearStyle: Map<string, any>;

    public constructor(private _elemRef: ElementRef, private _renderer: Renderer2) {
        this.clearStyle = new Map();
        this.clearStyle.set('width', '0px');
        this.clearStyle.set('height', '0px');
        this.clearStyle.set('backgroundImage', '');
    }

    ngOnChanges(changes: SimpleChanges): void {
        if(changes['hostComponent']) {
            this.hostComponent.addViewStateListener(this);
        }
        this.setImageStyle();
    }

    ngOnDestroy(): void {
        if(this.hostComponent) {
            this.hostComponent.removeViewStateListener(this);
        }
    }

    afterViewInit() {
        const nativeElement = this.hostComponent.getNativeElement();
        const renderer = this.hostComponent.getRenderer();
        renderer.listen( nativeElement, 'mouseenter', ( e ) => {
            if(this.rollOverImgStyle) {
                this.setCSSStyle(this.rollOverImgStyle);
            }
        } );

        renderer.listen( nativeElement, 'mouseleave', ( e ) => {
            if(this.rollOverImgStyle) {
                if(this.imgStyle) {
                    this.setCSSStyle(this.imgStyle);
                }
                else {
                    this.setCSSStyle(this.clearStyle);
                } 
            }
        } );
    }

    private setImageStyle(): void {
        if (this.media && this.media.visible) {
            const componentSize = this.media.componentSize;
            const mediaOptions = this.media.mediaOptions;
            if(this.media.rollOverImg) { 
                this.rollOverImgStyle = this.parseImageOptions(this.media.rollOverImg, mediaOptions, componentSize);
            } else {
                this.rollOverImgStyle = null
            }
            if(this.media.img) {
                this.imgStyle = this.parseImageOptions(this.media.img, mediaOptions, componentSize)
                this.setCSSStyle(this.imgStyle)
            } else {
                this.imgStyle = null;
                this.setCSSStyle(this.clearStyle);
            } 
        }
    }

    private parseImageOptions(image, mediaOptions, componentSize): Map<string, any> {
        let bgstyle = new Map();
        bgstyle.set('background-image', "url('" + image + "')"); 
        bgstyle.set('background-repeat', "no-repeat");
        bgstyle.set('background-position', "left");
        bgstyle.set('display', "inline-block");
        bgstyle.set('vertical-align', "middle"); 
        if(mediaOptions == undefined) mediaOptions = 14; // reduce-enlarge & keep aspect ration
        const mediaKeepAspectRatio = mediaOptions == 0 || ((mediaOptions & 8) == 8);

        // default  img size values
        let imgWidth = 16;
        let imgHeight = 16;

        if (image.indexOf('imageWidth=') > 0 && image.indexOf('imageHeight=') > 0)
        {
            let vars = {};
            image.replace(/[?&]+([^=&]+)=([^&]*)/gi,    
                    function(m,key,value) {
                vars[key] = value;
            });
            imgWidth = vars['imageWidth'];
            imgHeight = vars['imageHeight'];
        }

        const widthChange = imgWidth / componentSize.width;
        const heightChange = imgHeight / componentSize.height;

        if (widthChange > 1.01 || heightChange > 1.01 || widthChange < 0.99 || heightChange < 0.99) // resize needed
        {
            if ((mediaOptions & 6) == 6) // reduce-enlarge
            {
                if (mediaKeepAspectRatio)
                {
                    if (widthChange > heightChange)
                    {
                        imgWidth = imgWidth / widthChange;
                        imgHeight = imgHeight / widthChange;
                    }
                    else
                    {
                        imgWidth = imgWidth / heightChange;
                        imgHeight = imgHeight / heightChange;
                    }
                }
                else
                {
                    imgWidth = componentSize.width;
                    imgHeight = componentSize.height;
                }
            }        			  
            else if ((mediaOptions & 2) == 2) // reduce
            {
                if (widthChange > 1.01 && heightChange > 1.01)
                {
                    if (mediaKeepAspectRatio)
                    {
                        if (widthChange > heightChange)
                        {
                            imgWidth = imgWidth / widthChange;
                            imgHeight = imgHeight / widthChange;
                        }
                        else
                        {
                            imgWidth = imgWidth / heightChange;
                            imgHeight = imgHeight / heightChange;
                        }
                    }
                    else
                    {
                        imgWidth = componentSize.width;
                        imgHeight = componentSize.height;
                    }
                }
                else if (widthChange > 1.01)
                {
                    imgWidth = imgWidth / widthChange;
                    if (mediaKeepAspectRatio)
                    {
                        imgHeight = imgHeight / widthChange;
                    }
                    else
                    {
                        imgHeight = componentSize.height;
                    }
                }
                else if (heightChange > 1.01)
                {
                    imgHeight = imgHeight / heightChange;
                    if (mediaKeepAspectRatio)
                    {
                        imgWidth = imgWidth / heightChange;
                    }
                    else
                    {
                        imgWidth = componentSize.width;
                    }
                }
            }
            else if ((mediaOptions & 4) == 4) // enlarge
            {
                if (widthChange < 0.99 && heightChange < 0.99)
                {
                    if (mediaKeepAspectRatio)
                    {
                        if (widthChange > heightChange)
                        {
                            imgWidth = imgWidth / widthChange;
                            imgHeight = imgHeight / widthChange;
                        }
                        else
                        {
                            imgWidth = imgWidth / heightChange;
                            imgHeight = imgHeight / heightChange;
                        }
                    }
                    else
                    {
                        imgWidth = componentSize.width;
                        imgHeight = componentSize.height;
                    }
                }
                else if (widthChange < 0.99)
                {
                    imgWidth = imgWidth / widthChange;
                    if (mediaKeepAspectRatio)
                    {
                        imgHeight = imgHeight / widthChange;
                    }
                    else
                    {
                        imgHeight = componentSize.height;
                    }
                }
                else if (heightChange < 0.99)
                {
                    imgHeight = imgHeight / heightChange;
                    if (mediaKeepAspectRatio)
                    {
                        imgWidth = imgWidth / heightChange;
                    }
                    else
                    {
                        imgWidth = componentSize.width;
                    }
                }
            }
        }	  

        bgstyle.set('background-size', mediaKeepAspectRatio ? "contain" : "100% 100%");
        bgstyle.set('width', Math.round(imgWidth) + "px");
        bgstyle.set('height', Math.round(imgHeight) + "px");

        return bgstyle;
    }

    private setCSSStyle(cssStyle: Map<string, any>) {
        cssStyle.forEach((value: any, key: string) => {
            this._renderer.setStyle(this._elemRef.nativeElement, key, value);
        });
    }
}