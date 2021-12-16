import { DOCUMENT } from '@angular/common';
import { Component, OnInit, Renderer2, ViewChild, ElementRef, Inject, AfterViewInit, HostListener } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { DesignSizeService } from '../services/designsize.service';
import { URLParserService } from '../services/urlparser.service';
import { WindowRefService } from '@servoy/public';
import { EditorSessionService } from '../services/editorsession.service';

@Component({
    selector: 'designer-editorcontent',
    templateUrl: './editorcontent.component.html',
    styleUrls: ['./editorcontent.component.css']
})
export class EditorContentComponent implements OnInit, AfterViewInit {

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

    constructor(private sanitizer: DomSanitizer, private urlParser: URLParserService, protected readonly renderer: Renderer2,
        protected designSize: DesignSizeService, @Inject(DOCUMENT) private doc: Document, private windowRef: WindowRefService,
        private editorSession: EditorSessionService) {
        designSize.setEditor(this);
    }

    ngOnInit() {
        this.clientURL = this.sanitizer.bypassSecurityTrustResourceUrl('http://' + this.windowRef.nativeWindow.location.host + '/designer/solution/' + this.urlParser.getSolutionName() + '/form/' + this.urlParser.getFormName() + '/clientnr/' + this.urlParser.getContentClientNr() + '/index.html');
        if (this.urlParser.isAbsoluteFormLayout()) {
            this.contentStyle['width'] = this.urlParser.getFormWidth() + 'px';
            this.contentStyle['height'] = this.urlParser.getFormHeight() + 'px';
            this.windowRef.nativeWindow.addEventListener('message', (event) => {
                // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
                if (event.data.id === 'updateFormSize') {
                    this.contentStyle['width'] = event.data.width + 'px';
                    this.contentStyle['height'] = event.data.height + 'px';
                }
            });
        }
        else {
            this.contentStyle['bottom'] = '20px';
            this.contentStyle['right'] = '20px';
            this.contentStyle['minWidth'] = '992px';
            this.windowRef.nativeWindow.addEventListener('message', (event: MessageEvent) => {
                // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
                if (event.data.id === 'contentSizeChanged') {
                    this.adjustFromContentSize();
                }
            });
        }
    }

    ngAfterViewInit() {
        if (this.urlParser.isAbsoluteFormLayout()) {
            const glassPane = this.doc.querySelector('.contentframe-overlay');
            const formHeight = this.urlParser.getFormHeight() + 50;//should we calculate this number?
            if (glassPane.clientHeight < formHeight) {
                this.renderer.setStyle(glassPane, 'height', formHeight + 'px');
            }
        }
    }

    @HostListener('document:keydown', ['$event'])
    onKeyDown(event: KeyboardEvent) {
        this.editorSession.keyPressed(this.editorSession.getFixedKeyEvent(event));
    }

    adjustFromContentSize() {
        let paletteHeight = '100%';
        if (!this.lastHeight || this.lastHeight == 'auto' || this.contentSizeFull) {
            const iframe = this.doc.querySelector('iframe');
            const newHeight = iframe.contentWindow.document.body.clientHeight + 30;
            if (newHeight > this.elementRef.nativeElement.clientHeight) {
                this.renderer.setStyle(this.elementRef.nativeElement, 'height', newHeight + 'px');
                paletteHeight = newHeight + 'px';
            }
        }
        const palette = this.doc.querySelector('.palette');
        this.renderer.setStyle(palette, 'height', paletteHeight + 'px');
        this.renderer.setStyle(palette, 'max-height', paletteHeight + 'px');
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
        if (this.getContentDocument()) {
            const svyForm = this.getContentDocument().getElementsByClassName('svy-form')[0] as HTMLElement;
            svyForm.style['width'] = '';
        }
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
        const contentDoc: Document = this.getContentDocument();
        if (contentDoc) {
            const svyForm: HTMLElement = this.getContentDocument().querySelector('.svy-form');
            if (svyForm) {
                svyForm.style['width'] = width;
            } else {
                setTimeout(() => this.setFormWidth(width), 300);
            }
        }
        else {
            setTimeout(() => this.setFormWidth(width), 300);
        }
    }

    getContentDocument(): Document {
        const iframe = this.doc.querySelector('iframe');
        return iframe.contentWindow.document;
    }
}
