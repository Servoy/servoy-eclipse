import { AfterViewInit, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { EditorSessionService, ISupportAutoscroll } from './services/editorsession.service';
import { URLParserService } from 'src/designer/services/urlparser.service';

@Component({
    selector: 'app-designer',
    templateUrl: './designer.component.html',
    styleUrls: ['./designer.component.css']
})
export class DesignerComponent implements OnInit, AfterViewInit {

    @ViewChild('contentArea', { static: false }) contentArea: ElementRef<HTMLElement>;
    @ViewChild('glasspane', { static: false }) glasspane: ElementRef<HTMLElement>;

    @ViewChild('bottomAutoscroll', { static: false }) bottomAutoscroll: ElementRef<HTMLElement>;
    @ViewChild('rightAutoscroll', { static: false }) rightAutoscroll: ElementRef<HTMLElement>;
    @ViewChild('topAutoscroll', { static: false }) topAutoscroll: ElementRef<HTMLElement>;
    @ViewChild('leftAutoscroll', { static: false }) leftAutoscroll: ElementRef<HTMLElement>;

    autoscrollAreasEnabled: boolean;
    autoscrollElementClientBounds: Array<DOMRect>;
    autoscrollEnter:  { [key: string]: (event: MouseEvent) => void; } = {};
    autoscrollStop: { [key: string]: ReturnType<typeof setInterval>} = {};
    autoscrollLeave: { [key: string]: (event: MouseEvent) => void; } = {};

    constructor(public readonly editorSession: EditorSessionService, public urlParser: URLParserService) {
    }

    ngOnInit() {
        this.editorSession.connect();
    }

    ngAfterViewInit(): void {
        this.editorSession.autoscrollBehavior.subscribe((autoscroll: ISupportAutoscroll) => {
            if (autoscroll) {
                this.addAutoscrollListeners('BOTTOM_AUTOSCROLL', autoscroll);
				this.addAutoscrollListeners('RIGHT_AUTOSCROLL', autoscroll);
				this.addAutoscrollListeners('TOP_AUTOSCROLL', autoscroll);
				this.addAutoscrollListeners('LEFT_AUTOSCROLL', autoscroll);
            }
            else if (Object.keys(this.autoscrollStop).length > 0) {
                for (const direction in this.autoscrollEnter) {
                    if(this.autoscrollEnter[direction]) this.unregisterDOMEvent('mouseenter', direction, this.autoscrollEnter[direction]);
                }
                for (const direction in this.autoscrollLeave) {
                    if(this.autoscrollLeave[direction]) {
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
            this.autoscrollStop[direction] = this.startAutoScroll(direction, scrollComponent);
        }) as () => void;
    
        this.autoscrollLeave[direction] = (event: MouseEvent) => {
            if (this.autoscrollStop[direction]) {
                clearInterval(this.autoscrollStop[direction]);
                this.autoscrollStop[direction] = null;
            }
            if (event.type == 'mouseup')
                scrollComponent.onMouseUp(event);
        }
    
        this.registerDOMEvent('mouseleave', direction, this.autoscrollLeave[direction]);
        this.registerDOMEvent('mouseup', direction, this.autoscrollLeave[direction]);
       }
    
        startAutoScroll(direction: string, scrollComponent: ISupportAutoscroll): ReturnType<typeof setInterval> {
            let autoScrollPixelSpeed = 2;
            return setInterval(() => {
                autoScrollPixelSpeed = this.autoScroll(direction, autoScrollPixelSpeed, scrollComponent);
            }, 50);
        }
    
        private autoScroll(direction: string, autoScrollPixelSpeed: number, scrollComponent: ISupportAutoscroll) {
            let changeX = 0;
            let changeY = 0;
            const scrollElement = this.urlParser.isAbsoluteFormLayout() ? this.contentArea.nativeElement : this.contentArea.nativeElement.parentElement;
            switch (direction) {
                case 'BOTTOM_AUTOSCROLL':
                    if ((scrollElement.scrollTop + scrollElement.offsetHeight) === scrollElement.scrollHeight)
                        this.glasspane.nativeElement.style.height = parseInt(this.glasspane.nativeElement.style.height.replace('px', '')) + autoScrollPixelSpeed + 'px';
                    scrollElement.scrollTop += autoScrollPixelSpeed;
                    changeY = autoScrollPixelSpeed;
                    break;
                case 'RIGHT_AUTOSCROLL':
                    if ((scrollElement.scrollLeft + scrollElement.offsetWidth) === scrollElement.scrollWidth)
                        this.glasspane.nativeElement.style.width = parseInt(this.glasspane.nativeElement.style.width.replace('px', '')) + autoScrollPixelSpeed + 'px';
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
                    }
                    break;
                case 'TOP_AUTOSCROLL':
                    if (scrollElement.scrollTop >= autoScrollPixelSpeed) {
                        scrollElement.scrollTop -= autoScrollPixelSpeed;
                        changeY = -autoScrollPixelSpeed;
                    } else {
                        changeY = -scrollElement.scrollTop;
                        scrollElement.scrollTop -= 0;
                    }
                    break;
            }
    
            if (autoScrollPixelSpeed < 15)
                autoScrollPixelSpeed++;
    
            if (scrollComponent.getUpdateLocationCallback() != null)
            {
                scrollComponent.getUpdateLocationCallback()(changeX, changeY, 0, 0);
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
}