import { DOCUMENT } from '@angular/common';
import { Component, OnInit, Renderer2, ViewChild, ElementRef, Inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { DesignSizeService } from '../services/designsize.service';
import { URLParserService } from '../services/urlparser.service';
import {WindowRefService} from '@servoy/public';

@Component({
  selector: 'designer-editorcontent',
  templateUrl: './editorcontent.component.html',
  styleUrls: ['./editorcontent.component.css']
})
export class EditorContentComponent implements OnInit{

    initialWidth: string;
    contentStyle: any = {
        position: "absolute",
        top: "20px",
        left: "20px",
        right: "20px",
        minWidth: "992px",
        bottom: "20px"
    };
    contentSizeFull = true;

    clientURL: SafeResourceUrl;
    @ViewChild('element', { static: true }) elementRef: ElementRef;
    
    constructor(private sanitizer: DomSanitizer, private urlParser: URLParserService, protected readonly renderer: Renderer2,
        protected designSize: DesignSizeService, @Inject(DOCUMENT) private doc: Document, private windowRef: WindowRefService) {
        designSize.setEditor(this);
    }
    
    ngOnInit() {
        this.clientURL = this.sanitizer.bypassSecurityTrustResourceUrl('http://'+ this.windowRef.nativeWindow.location.host+'/designer/solution/'+this.urlParser.getSolutionName()+'/form/'+ this.urlParser.getFormName()+'/clientnr/'+ this.urlParser.getContentClientNr() +'/index.html');
        if (this.urlParser.isAbsoluteFormLayout())
        {
            this.renderer.setStyle(this.elementRef.nativeElement, 'width', this.urlParser.getFormWidth()+'px');
            this.renderer.setStyle(this.elementRef.nativeElement, 'height', this.urlParser.getFormHeight()+'px');
        }
        else
        {
             this.renderer.setStyle(this.elementRef.nativeElement, 'bottom', '20px');
             this.renderer.setStyle(this.elementRef.nativeElement, 'right', '20px');
        }
    }

    setContentSizeFull(redraw: boolean) {
        this.contentStyle = {
            position: "absolute",
            top: "20px",
            left: "20px",
            right: "20px",
            bottom: "20px"
        };
        this.contentSizeFull = true;
        delete this.contentStyle['width'];
        delete this.contentStyle['height'];
        delete this.contentStyle['h'];
        delete this.contentStyle['w'];
        if(this.getContentDocument()) {
            const svyForm: any = this.getContentDocument().getElementsByClassName('svy-form')[0];
            svyForm.style['height'] = "";
            svyForm.style['width'] = "";
        }
        // TODO
        // $scope.adjustGlassPaneSize();
        // if (redraw) {
        //     $scope.redrawDecorators()
        // }
    }

    getFormInitialWidth() {
        if (!this.initialWidth)
        {
            this.initialWidth = Math.round(this.elementRef.nativeElement.getBoundingClientRect().width) + "px";
        }
        return this.initialWidth;
    }

    setContentSize(width: string, height: string, fixedSize: boolean) {
        this.contentStyle['width'] = width;
        this.contentStyle['height'] = height;
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
            if(this.getContentDocument()) {
                const svyForm:any = this.getContentDocument().getElementsByClassName('svy-form')[0];
                svyForm.style['width'] = width;
                svyForm.style['height'] = height;
            }
            this.setMainContainerSize();
            const contentFrame = this.elementRef.nativeElement.getElementsByClassName('contentframe')[0];
            contentFrame.style['height'] = height;
        }
        // TODO
        // $scope.adjustGlassPaneSize(width, height);
        // $scope.redrawDecorators();
    }

    setMainContainerSize = function() {
        if(this.getContentDocument()) {
            const maincontainer = this.getContentDocument().querySelector('*[data-maincontainer="true"]');
            if(maincontainer) {
                const contentFrame = this.elementRef.nativeElement.getElementsByClassName('contentframe')[0];
                maincontainer.style['min-height'] = contentFrame.style['min-height'];
                maincontainer.style['min-width'] = contentFrame.style['min-width'];
            }
        }
    }

    getContentDocument(): Document {
        // TODO
        // this throws SecurityError: Blocked a frame with origin  from accessing a cross-origin frame
        // let iframe = this.doc.querySelector('iframe');
        // return iframe.contentWindow.document;
        return null;
    }
}
