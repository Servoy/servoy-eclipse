import { Component, OnInit, Renderer2, ViewChild, ElementRef, AfterViewInit, HostListener, Input, Output, EventEmitter, OnDestroy } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { DesignSizeService } from '../services/designsize.service';
import { URLParserService } from '../services/urlparser.service';
import { WindowRefService } from '@servoy/public';
import { EditorSessionService } from '../services/editorsession.service';
import { EditorContentService, IContentMessageListener } from '../services/editorcontent.service';

@Component({
    selector: 'designer-editorcontent',
    templateUrl: './editorcontent.component.html',
    styleUrls: ['./editorcontent.component.css'],
    standalone: false
})
export class EditorContentComponent implements OnInit, AfterViewInit, IContentMessageListener, OnDestroy {

    initialWidth: string;
    contentStyle: CSSStyleDeclaration = {
        position: 'absolute',
        top: '20px',
        left: '20px'
    } as CSSStyleDeclaration;
    contentSizeFull = false;
    lastHeight: string;

    clientURL: SafeResourceUrl;
    @ViewChild('element', { static: true }) elementRef: ElementRef<HTMLElement>;
    
    @Input() styleVariantPreview: boolean
    @Output() previewReady = new EventEmitter<{previewReady: boolean}>();
    
    constructor(private sanitizer: DomSanitizer, private urlParser: URLParserService, protected readonly renderer: Renderer2,
        protected designSize: DesignSizeService, private editorContentService: EditorContentService, private windowRef: WindowRefService,
        private editorSession: EditorSessionService) {

        designSize.setEditor(this);
        this.editorContentService.addContentMessageListener(this);
    }

    ngOnInit() {
        this.clientURL = this.sanitizer.bypassSecurityTrustResourceUrl('http://' + this.windowRef.nativeWindow.location.host + '/designer/solution/' + this.urlParser.getSolutionName() + '/form/' + this.urlParser.getFormName() + '/clientnr/' + this.urlParser.getContentClientNr() + '/index.html');
        if (this.urlParser.isAbsoluteFormLayout()) {
            this.contentStyle['width'] = this.urlParser.getFormWidth() + 'px';
            this.contentStyle['height'] = this.urlParser.getFormHeight() + 'px';
        }
        else {
            this.contentStyle['bottom'] = '20px';
            this.contentStyle['right'] = '20px';
            this.contentStyle['minWidth'] = '992px';
        }
    }

    ngAfterViewInit() {
        if (this.urlParser.isAbsoluteFormLayout()) {
            const glassPane = this.editorContentService.getGlassPane();
            const formHeight = this.urlParser.getFormHeight() + 50;//should we calculate this number?
            if (glassPane.clientHeight < formHeight) {
                this.renderer.setStyle(glassPane, 'height', formHeight + 'px');
            }
        }
    }
    
    ngOnDestroy(): void {
        this.editorContentService.removeContentMessageListener(this);
    }
    
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    contentMessageReceived(id: string, data: { property: string, width? : number, height? : number, eventData?: any }) {
        if (id === 'updateFormSize' && this.urlParser.isAbsoluteFormLayout()) {
            this.contentStyle['width'] = data.width + 'px';
            this.contentStyle['height'] = data.height + 'px';
        }
        if (id === 'contentSizeChanged' && !this.urlParser.isAbsoluteFormLayout()) {
            this.adjustFromContentSize();
        }
        if (id === 'buildTitaniumClient') {
            this.editorSession.buildTiNG();
            window.parent.postMessage({ id: 'hideGhostContainer' }, '*');
		}
    }

    @HostListener('document:keydown', ['$event'])
    onKeyDown(event: KeyboardEvent) {
        if ((event.target as Element).className !== 'inlineEdit' && (event.ctrlKey || event.metaKey || event.altKey)) {
            this.editorSession.keyPressed(this.editorSession.getFixedKeyEvent(event));
            return false;
        }
        return true;
    }

    @HostListener('document:keyup', ['$event'])
    onKeyUp(event: KeyboardEvent) {
        // delete , f4 (open form hierarchy) and f5
        if (event.keyCode == 46 || event.keyCode == 115 || event.keyCode == 116 ) {
            this.editorSession.keyPressed(this.editorSession.getFixedKeyEvent(event));         
            return false;
        }
        return true;
    }
    
    @HostListener('document:click', ['$event'])
    onClick(event: MouseEvent) {
		if (event.offsetX > 50 && event.offsetY > 50) {
			window.parent.postMessage({ id: 'positionClick', x: event.offsetX, y: event.offsetY, isAbsoluteFormLayout: this.urlParser.isAbsoluteFormLayout() }, '*');
		}
    }

    adjustFromContentSize() {
        this.editorContentService.executeOnlyAfterInit(() => {
            const overlay = this.editorContentService.getGlassPane();
            this.renderer.setStyle(overlay, 'height', '100%');
            this.renderer.setStyle(overlay, 'width', '100%');

            let paletteHeight = '100%';
            if (!this.lastHeight || this.lastHeight == 'auto' || this.contentSizeFull) {
                const newHeight = this.editorContentService.getContentBodyElement().clientHeight + 30;
                if (newHeight > this.elementRef.nativeElement.clientHeight) {
                    this.renderer.setStyle(this.elementRef.nativeElement, 'height', newHeight + 'px');
                    paletteHeight = newHeight + 'px';
                }
            }
            const palette = this.editorContentService.getPallete();
            this.renderer.setStyle(palette, 'height', paletteHeight);
            this.renderer.setStyle(palette, 'max-height', paletteHeight);

            // make the overlay the same size as content area to cover it; unfortunately didn't find a way to do this through css'
            const contentArea = this.editorContentService.getContentArea();
            //add a small scroll space in order to make margins visible
            this.renderer.setStyle(overlay, 'height', contentArea.scrollHeight + 20 + 'px');
            this.renderer.setStyle(overlay, 'width', contentArea.scrollWidth + 20 + 'px');
        });
    }

    setContentSizeFull() {
        this.contentStyle = {
            position: 'absolute',
            top: '20px',
            left: '20px',
            right: '20px',
            bottom: '20px'
        } as CSSStyleDeclaration;
        this.contentSizeFull = true;
        delete this.contentStyle['width'];
        delete this.contentStyle['height'];
        
        this.editorContentService.executeOnlyAfterInit(()=>{
            const svyForm = this.editorContentService.getContentForm();
             svyForm.style['width'] = '';
        });
       
        // TODO
        // $scope.adjustGlassPaneSize();
        // if (redraw) {
        //     $scope.redrawDecorators()
        // }
    }

    getFormInitialWidth(): string {
        if (!this.initialWidth) {
            this.initialWidth = Math.round(this.elementRef.nativeElement.getBoundingClientRect().width) + 'px';
        }
        return this.initialWidth;
    }

    setContentSize(width: string, height: string) {
        this.contentSizeFull = false;
        this.lastHeight = height;
        this.contentStyle['width'] = width;
        // if size is auto, the listener from content will set the height
        if (height != 'auto') {
            this.contentStyle['height'] = height;
        }
        else {
            this.adjustFromContentSize();
        }
        delete this.contentStyle['top'];
        delete this.contentStyle['left'];
        delete this.contentStyle['position'];
        delete this.contentStyle['minWidth'];
        delete this.contentStyle['bottom'];
        delete this.contentStyle['right'];

        this.setFormWidth(width);
        //this.setMainContainerSize();
        //const contentFrame = this.elementRef.nativeElement.getElementsByClassName('contentframe')[0];
        //contentFrame.style['height'] = height;
        // TODO
        // $scope.adjustGlassPaneSize(width, height);
        // $scope.redrawDecorators();
    }

    /*setMainContainerSize(){
        if (this.getContentDocument()) {
            const maincontainer: HTMLElement = this.getContentDocument().querySelector('*[data-maincontainer="true"]');
            if (maincontainer) {
                const contentFrame = this.elementRef.nativeElement.getElementsByClassName('contentframe')[0] as HTMLElement;
                // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
                maincontainer.style['min-height'] = contentFrame.style['min-height'];
                // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
                maincontainer.style['min-width'] = contentFrame.style['min-width'];
            }
        }
    }*/
    private setFormWidth(width: string) {
        this.editorContentService.executeOnlyAfterInit(()=>{
            const svyForm = this.editorContentService.getContentForm();
             svyForm.style['width'] = width;
        });
    }
}
