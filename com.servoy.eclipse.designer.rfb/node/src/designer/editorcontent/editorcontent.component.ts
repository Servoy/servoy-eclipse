import { Component, OnInit, Renderer2, ViewChild, ElementRef } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { URLParserService } from '../services/urlparser.service';

@Component({
  selector: 'designer-editorcontent',
  templateUrl: './editorcontent.component.html',
  styleUrls: ['./editorcontent.component.css']
})
export class EditorContentComponent implements OnInit{
    clientURL: SafeResourceUrl;
    @ViewChild('element', { static: true }) elementRef: ElementRef;
    
    constructor(private sanitizer: DomSanitizer, private urlParser: URLParserService, protected readonly renderer: Renderer2) {
    }
    
     ngOnInit() {
        this.clientURL = this.sanitizer.bypassSecurityTrustResourceUrl('http://localhost:8080/designer/solution/'+this.urlParser.getSolutionName()+'/form/'+ this.urlParser.getFormName()+'/clientnr/'+ this.urlParser.getContentClientNr() +'/index.html');
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
}
