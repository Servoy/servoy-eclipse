import { DOCUMENT } from '@angular/common';
import { Component, OnInit, Renderer2, ViewChild, ElementRef, Inject, AfterViewInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { DesignSizeService } from '../services/designsize.service';
import { URLParserService } from '../services/urlparser.service';
import { WindowRefService } from '@servoy/public';

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
    contentSizeFull = true;

    clientURL: SafeResourceUrl;
    @ViewChild('element', { static: true }) elementRef: ElementRef<HTMLElement>;

    constructor(private sanitizer: DomSanitizer, private urlParser: URLParserService, protected readonly renderer: Renderer2,
        protected designSize: DesignSizeService, @Inject(DOCUMENT) private doc: Document, private windowRef: WindowRefService) {
        designSize.setEditor(this);
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

    adjustFromContentSize() {
        const iframe = this.doc.querySelector('iframe');
        const contentPane = this.doc.querySelector('.content-area');
        const newHeight = iframe.contentWindow.document.body.clientHeight + 30;
        if (newHeight > contentPane.clientHeight) {
            this.renderer.setStyle(contentPane, 'height', newHeight + 'px');
            const palette = this.doc.querySelector('.palette');
            this.renderer.setStyle(palette, 'height', newHeight + 'px');
            this.renderer.setStyle(palette, 'max-height', newHeight + 'px');
        }

    }

    setContentSizeFull(redraw: boolean) {
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
        delete this.contentStyle['h'];
        delete this.contentStyle['w'];
        if (this.getContentDocument()) {
            const svyForm = this.getContentDocument().getElementsByClassName('svy-form')[0] as HTMLElement;
            svyForm.style['height'] = '';
            svyForm.style['width'] = '';
        }
        // TODO
        // $scope.adjustGlassPaneSize();
        // if (redraw) {
        //     $scope.redrawDecorators()
        // }
    }

    getFormInitialWidth() {
        if (!this.initialWidth) {
            this.initialWidth = Math.round(this.elementRef.nativeElement.getBoundingClientRect().width) + 'px';
        }
        return this.initialWidth;
    }

    setContentSize(width: string, height: string, fixedSize: boolean) {
        this.contentStyle['width'] = width;
        if (this.urlParser.isAbsoluteFormLayout()) {
            this.contentStyle['height'] = height;
        }
        if (fixedSize) this.contentSizeFull = false;
        delete this.contentStyle['top'];
        delete this.contentStyle['left'];
        delete this.contentStyle['position'];
        delete this.contentStyle['minWidth'];
        delete this.contentStyle['bottom'];
        delete this.contentStyle['right'];
        delete this.contentStyle['h'];
        delete this.contentStyle['w'];

        if (!this.urlParser.isAbsoluteFormLayout()) {
            this.setFormSize(width, height);
            //this.setMainContainerSize();
            //const contentFrame = this.elementRef.nativeElement.getElementsByClassName('contentframe')[0];
            //contentFrame.style['height'] = height;
            this.renderer.setStyle(this.elementRef.nativeElement, 'height', '100%');
        }
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
    private setFormSize(width: string, height: string) {
        const contentDoc: Document = this.getContentDocument();
        if (contentDoc) {
            const svyForm: HTMLElement = this.getContentDocument().querySelector('.svy-form');
            if (svyForm) {
                svyForm.style['width'] = width;
                svyForm.style['height'] = height;
            }else{
                setTimeout(() => this.setFormSize(width, height), 300);
            }
        }
        else {
            setTimeout(() => this.setFormSize(width, height), 300);
        }
    }

    getContentDocument(): Document {
        const iframe = this.doc.querySelector('iframe');
        return iframe.contentWindow.document;
    }
}
