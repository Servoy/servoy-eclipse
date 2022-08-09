import { AfterViewInit, Component, ElementRef, OnInit, Renderer2, ViewChild, Input } from '@angular/core';
import { EditorSessionService, ISupportAutoscroll, PaletteComp } from './services/editorsession.service';
import { URLParserService } from 'src/designer/services/urlparser.service';
import { EditorContentService } from './services/editorcontent.service';

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
    autoscrollEnter: { [key: string]: (event: MouseEvent) => void; } = {};
    autoscrollStop: { [key: string]: ReturnType<typeof setInterval> } = {};
    autoscrollLeave: { [key: string]: (event: MouseEvent) => void; } = {};
    
    preview = false;
    
    @Input() component: PaletteComp;
    

    constructor(
		public readonly editorSession: EditorSessionService,
		private editorContentService: EditorContentService, 
		public urlParser: URLParserService, 
		protected readonly renderer: Renderer2
		) {
		
		this.editorSession.previewRequested.subscribe((value: {cmp: PaletteComp}) => {
			this.preview = !this.preview;
			this.component = value.cmp;
		})
    }

    ngOnInit() {
        this.editorSession.connect();
        this.editorSession.registerCallback.subscribe(value => {
            if (this.contentArea) this.renderer.listen(this.contentArea.nativeElement, value.event, value.function);
        })
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
        const scrollElement = this.contentArea.nativeElement;
        switch (direction) {
            case 'BOTTOM_AUTOSCROLL':
                //if ((scrollElement.scrollTop + scrollElement.offsetHeight) === scrollElement.scrollHeight) {
                    let height = this.glasspane.nativeElement.style.height;
                    if (!height) height = this.glasspane.nativeElement.offsetHeight + 'px';
                    this.glasspane.nativeElement.style.height = parseInt(height.replace('px', '')) + autoScrollPixelSpeed + 'px';
                //}
                scrollElement.scrollTop += autoScrollPixelSpeed;
                changeY = autoScrollPixelSpeed;
                break;
            case 'RIGHT_AUTOSCROLL':
                if ((scrollElement.scrollLeft + scrollElement.offsetWidth) === scrollElement.scrollWidth) {
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

        if (scrollComponent.getUpdateLocationCallback() != null) {
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
    
    onPreviewReady() {
		const columns = this.component.styleVariants.length > 8 ? 3 : this.component.styleVariants.length > 3 ? 2 : 1;
		this.editorContentService.sendMessageToPreview({ 
			id: 'createVariants', 
			variants: this.component.styleVariants, 
			model: this.component.model, 
			name: this.convertToJSName(this.component.name), 
			type: 'component',
			margin: 15,
			columns: columns
		});
	}
	
	convertToJSName(name: string): string {
        // this should do the same as websocket.ts #scriptifyServiceNameIfNeeded() and ClientService.java #convertToJSName()
        if (name) {
            const packageAndName = name.split('-');
            if (packageAndName.length > 1) {
                name = packageAndName[0];
                for (let i = 1; i < packageAndName.length; i++) {
                    if (packageAndName[1].length > 0) name += packageAndName[i].charAt(0).toUpperCase() + packageAndName[i].slice(1);
                }
            }
        }
        return name;
    }
}