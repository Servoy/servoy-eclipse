import { AfterViewInit, Component, ElementRef, OnInit, Renderer2, ViewChild } from '@angular/core';
import { EditorSessionService, ISupportAutoscroll } from './services/editorsession.service';
import { URLParserService } from 'src/designer/services/urlparser.service';
import { GhostsContainerComponent } from './ghostscontainer/ghostscontainer.component';
import { EditorContentService } from './services/editorcontent.service';

@Component({
    selector: 'app-designer',
    templateUrl: './designer.component.html',
    styleUrls: ['./designer.component.css']
})
export class DesignerComponent implements OnInit, AfterViewInit {

    @ViewChild('contentArea', { static: false }) contentArea: ElementRef<HTMLElement>;
    @ViewChild('glasspane', { static: false }) glasspane: ElementRef<HTMLElement>;
    @ViewChild('ghostContainer', { static: false }) ghostContainer: GhostsContainerComponent;


    @ViewChild('bottomAutoscroll', { static: false }) bottomAutoscroll: ElementRef<HTMLElement>;
    @ViewChild('rightAutoscroll', { static: false }) rightAutoscroll: ElementRef<HTMLElement>;
    @ViewChild('topAutoscroll', { static: false }) topAutoscroll: ElementRef<HTMLElement>;
    @ViewChild('leftAutoscroll', { static: false }) leftAutoscroll: ElementRef<HTMLElement>;

    autoscrollAreasEnabled: boolean;
    autoscrollElementClientBounds: Array<DOMRect>;
    autoscrollEnter: { [key: string]: (event: MouseEvent) => void; } = {};
    autoscrollTrigger: { [key: string]: ReturnType<typeof setInterval> } = {};
    autoscrollLeave: { [key: string]: (event: MouseEvent) => void; } = {};
    bottomAutoscrollLimit = 0; 
    rightAutoscrollLimit = 0;
    autoscrollMargin = 100;
    leftAutoscrollPosition = 0;
    isAutoscrollActive = false; //autoscroll safety

    constructor(public readonly editorSession: EditorSessionService, 
                public urlParser: URLParserService, 
                protected readonly renderer: Renderer2, 
                private editorContentService: EditorContentService) {
    }

    ngOnInit() {
        this.editorSession.connect();
        this.editorSession.registerCallback.subscribe(value => {
            if (this.contentArea) this.renderer.listen(this.contentArea.nativeElement, value.event, value.function);
        })
    }

    ngAfterViewInit(): void {
        this.editorSession.autoscrollBehavior.subscribe((scrollElement: ISupportAutoscroll) => {
            if (scrollElement) {
                this.addAutoscrollListeners('BOTTOM_AUTOSCROLL', scrollElement);
                this.addAutoscrollListeners('RIGHT_AUTOSCROLL', scrollElement);
                this.addAutoscrollListeners('TOP_AUTOSCROLL', scrollElement);
                this.addAutoscrollListeners('LEFT_AUTOSCROLL', scrollElement);

                this.bottomAutoscrollLimit = this.ghostContainer.formHeight + this.autoscrollMargin;
                this.rightAutoscrollLimit = this.ghostContainer.formWidth + this.autoscrollMargin;

                this.leftAutoscrollPosition =  this.editorContentService.getDesignerElementById('palette').offsetWidth;
                this.setAutoscrollPositions();
            }
            else if (Object.keys(this.autoscrollTrigger).length > 0) {
                for (const direction in this.autoscrollEnter) {
                    if (this.autoscrollEnter[direction]) this.unregisterDOMEvent('mouseenter', direction, this.autoscrollEnter[direction]);
                }
                for (const direction in this.autoscrollLeave) {
                    if (this.autoscrollLeave[direction]) {
                        this.unregisterDOMEvent('mouseleave', direction, this.autoscrollLeave[direction]);
                        this.unregisterDOMEvent('mouseup', direction, this.autoscrollLeave[direction]);
                    }
                }
            }
        });
    }

    addAutoscrollListeners(direction: string, scrollComponent: ISupportAutoscroll) {
        this.editorSession.getState().pointerEvents = 'all';
        this.autoscrollEnter[direction] = this.registerDOMEvent('mouseenter', direction, () => {
            if (this.isAutoscrollActive) {
                this.clearAutoscroll(null);
            } 
            this.autoscrollTrigger[direction] = this.startAutoScroll(direction, scrollComponent);
        }) as () => void;

        this.autoscrollLeave[direction] = (event: MouseEvent) => {
            this.clearAutoscroll(direction);
            if (event.type == 'mouseup')
                scrollComponent.onMouseUp(event);
        }
        this.registerDOMEvent('mouseleave', direction, this.autoscrollLeave[direction]);
        this.registerDOMEvent('mouseup', direction, this.autoscrollLeave[direction]);
    }

    clearAutoscroll(direction: string) {
        let tmpDirection = direction;
        if (!tmpDirection) {
            if (this.autoscrollTrigger['BOTTOM_AUTOSCROLL']) { tmpDirection = 'BOTTOM_AUTOSCROLL'; }
            else if (this.autoscrollTrigger['RIGHT_AUTOSCROLL']) { tmpDirection = 'RIGHT_AUTOSCROLL'; }
            else if (this.autoscrollTrigger['TOP_AUTOSCROLL']) { tmpDirection = 'TOP_AUTOSCROLL'; }
            else if (this.autoscrollTrigger['LEFT_AUTOSCROLL']) { tmpDirection = 'LEFT_AUTOSCROLL'; }
        } 
        if (this.autoscrollTrigger[tmpDirection]) {
            clearInterval(this.autoscrollTrigger[tmpDirection]);
            this.autoscrollTrigger[tmpDirection] = null;
        }
        this.isAutoscrollActive = false;
    }

    startAutoScroll(direction: string, scrollComponent: ISupportAutoscroll): ReturnType<typeof setInterval> {
        this.isAutoscrollActive = true;
        let autoScrollPixelSpeed = 2;
        return setInterval(() => {
            autoScrollPixelSpeed = this.autoScroll(direction, autoScrollPixelSpeed, scrollComponent);
        }, 50);
    }

    private autoScroll(direction: string, autoScrollPixelSpeed: number, scrollComponent: ISupportAutoscroll) {
        let changeX = 0;
        let changeY = 0;
        const scrollElement = this.contentArea.nativeElement;
        let stopAutoscroll = false;
        let executeOneStep = false;
        switch (direction) {


            case 'BOTTOM_AUTOSCROLL':
                if ((scrollElement.scrollTop + scrollElement.offsetHeight) >= scrollElement.scrollHeight) {
                    if (this.glasspane.nativeElement.offsetHeight >= this.bottomAutoscrollLimit) {
                        stopAutoscroll = true;
                        break;
                    }
                    let height = this.glasspane.nativeElement.style.height;
                    if (!height) height = this.glasspane.nativeElement.offsetHeight + 'px';
                    this.glasspane.nativeElement.style.height = parseInt(height.replace('px', '')) + autoScrollPixelSpeed + 'px';
                   
                }
                scrollElement.scrollTop += autoScrollPixelSpeed;
                changeY = autoScrollPixelSpeed;
                break;
            case 'RIGHT_AUTOSCROLL':
                
                if ((scrollElement.scrollLeft + scrollElement.offsetWidth) >= scrollElement.scrollWidth) {
                    if (this.glasspane.nativeElement.offsetWidth >= this.rightAutoscrollLimit) {
                        stopAutoscroll = true;
                        break;
                    }
                    let width = this.glasspane.nativeElement.style.width;
                    if (!width) width = this.glasspane.nativeElement.offsetWidth + 'px';
                    this.glasspane.nativeElement.style.width = parseInt(width.replace('px', '')) + autoScrollPixelSpeed + 'px';
                }
               
                scrollElement.scrollLeft += autoScrollPixelSpeed;
                changeX = autoScrollPixelSpeed;                
                break;
            case 'LEFT_AUTOSCROLL':
                if (scrollElement.scrollLeft >= autoScrollPixelSpeed) {
                    scrollElement.scrollLeft -= autoScrollPixelSpeed;
                    changeX = -autoScrollPixelSpeed;
                } else {
                    changeX = -scrollElement.scrollLeft;
                    scrollElement.scrollLeft = 0;
                    executeOneStep = true; //scrolling left make sure we can scroll till the left margin
                    
                }
                break;
            case 'TOP_AUTOSCROLL':
                if (scrollElement.scrollTop > autoScrollPixelSpeed) {
                    scrollElement.scrollTop -= autoScrollPixelSpeed;
                    changeY = -autoScrollPixelSpeed;
                } else {
                    changeY = -scrollElement.scrollTop;
                    scrollElement.scrollTop = 0;
                    executeOneStep = true; //scrolling top make sure we can scroll till the top margin
                }
                break;
        }

        if (executeOneStep) {
            stopAutoscroll = true;    
            if (scrollComponent.getUpdateLocationCallback() != null) {
                scrollComponent.getUpdateLocationCallback()(changeX, changeY, 0, 0);
            }
        }
        
        if (!stopAutoscroll) {
            if (autoScrollPixelSpeed < 15)
                autoScrollPixelSpeed++;

            stopAutoscroll = true;    
            if (scrollComponent.getUpdateLocationCallback() != null) {
                scrollComponent.getUpdateLocationCallback()(changeX, changeY, 0, 0);
            }
        }
        return autoScrollPixelSpeed;
    }

    registerDOMEvent(eventType: string, target: string, callback: (event: MouseEvent) => void): (event: MouseEvent) => void {
        const element: HTMLElement = this.getAutoscrollElement(target);
        if (element) element.addEventListener(eventType, callback)
        return callback;
    }

    private getAutoscrollElement(target: string) {
        let element: ElementRef;
        if (target === 'BOTTOM_AUTOSCROLL') {
            element = this.bottomAutoscroll;
        } else if (target === 'RIGHT_AUTOSCROLL') {
            element = this.rightAutoscroll;
        } else if (target === 'LEFT_AUTOSCROLL') {
            element = this.leftAutoscroll;
        } else if (target === 'TOP_AUTOSCROLL') {
            element = this.topAutoscroll;
        }
        return element ? element.nativeElement : null;
    }

    unregisterDOMEvent(eventType: string, target: string, callback: (event: MouseEvent) => void) {
        const element: HTMLElement = this.getAutoscrollElement(target);
        if (element) element.removeEventListener(eventType, callback);
    }

    setAutoscrollPositions() {
        const leftAutoscrollElement = this.editorContentService.getBodyElement().getElementsByClassName('leftAutoscrollArea').item(0);
        this.renderer.setStyle(leftAutoscrollElement, 'left', this.leftAutoscrollPosition + 'px');
        const topAutoscrollElement = this.editorContentService.getBodyElement().getElementsByClassName('topAutoscrollArea').item(0);
        this.renderer.setStyle(topAutoscrollElement, 'left', (this.leftAutoscrollPosition) + 'px');
        const bottomAutoscrollElement = this.editorContentService.getBodyElement().getElementsByClassName('bottomAutoscrollArea').item(0);
        this.renderer.setStyle(bottomAutoscrollElement, 'left', (this.leftAutoscrollPosition) + 'px');

        //autoscroll area must not overlapp since in that case they can get mixed mouse events which lead to unexpected situations
        const sideAutoscrollHeight = (bottomAutoscrollElement as HTMLElement).offsetTop - ((topAutoscrollElement as HTMLElement).offsetTop + (topAutoscrollElement as HTMLElement).offsetHeight) - 10;

        this.renderer.setStyle(leftAutoscrollElement, 'height', sideAutoscrollHeight + 'px');
        const rightAutoscrollElement = this.editorContentService.getBodyElement().getElementsByClassName('rightAutoscrollArea').item(0);
        this.renderer.setStyle(rightAutoscrollElement, 'height', sideAutoscrollHeight + 'px');
    }
}