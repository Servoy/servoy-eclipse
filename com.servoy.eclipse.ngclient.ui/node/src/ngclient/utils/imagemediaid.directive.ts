import { Directive, Input, ViewContainerRef, SimpleChanges, OnChanges, ElementRef, Renderer2, HostListener } from '@angular/core';
import { LoggerService, LoggerFactory } from '../../sablo/logger.service';
import { ServoyBaseComponent, IComponentContributorListener, ComponentContributor } from '../servoy_public';

@Directive({
    selector: '[svyImageMediaId]'
})
export class ImageMediaIdDirective implements OnChanges, IComponentContributorListener {

    @Input('svyImageMediaId') media : any;
    private log: LoggerService;
    private field: ServoyBaseComponent;
    private imgStyle: Map<string, any>;
    private rollOverImgStyle: Map<string, any>;
    private clearStyle: Map<string, any>;

    public constructor(private componentContributor: ComponentContributor, private logFactory:LoggerFactory, private viewContainer: ViewContainerRef, private _elemRef: ElementRef, private _renderer: Renderer2) {
        this.log = logFactory.getLogger("ImageMediaIdDirective");
        if(this.viewContainer['_view'] != undefined && this.viewContainer['_view']['component'] != undefined) {
            this.field = this.viewContainer['_view']['component'];
            componentContributor.addComponentListener(this);
        }
        else {
            this.log.error("Can't find field for svyImageMediaId");
        }

        this.clearStyle = new Map();
        this.clearStyle.set('width', '0px');
        this.clearStyle.set('height', '0px');
        this.clearStyle.set('backgroundImage', '');
    }

    componentCreated(component: ServoyBaseComponent) {
        if(component == this.field) {
            let nativeElement = component.getNativeElement();
            let renderer = component.getRenderer();
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
    }

    ngOnChanges(changes: SimpleChanges): void {
        this.setImageStyle();
    }

    private setImageStyle(): void {
        if (this.media && this.media.visible) {
            var componentSize = this.media.componentSize;
            var mediaOptions = this.media.mediaOptions;
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
        var mediaKeepAspectRatio = mediaOptions == 0 || ((mediaOptions & 8) == 8);

        // default  img size values
        var imgWidth = 16;
        var imgHeight = 16;

        if (image.indexOf('imageWidth=') > 0 && image.indexOf('imageHeight=') > 0)
        {
            var vars = {};
            var parts = image.replace(/[?&]+([^=&]+)=([^&]*)/gi,    
                    function(m,key,value) {
                vars[key] = value;
            });
            imgWidth = vars['imageWidth'];
            imgHeight = vars['imageHeight'];
        }

        var widthChange = imgWidth / componentSize.width;
        var heightChange = imgHeight / componentSize.height;

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