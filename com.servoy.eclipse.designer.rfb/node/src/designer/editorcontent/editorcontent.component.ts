import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { URLParserService } from '../services/urlparser.service';

@Component({
  selector: 'designer-editorcontent',
  templateUrl: './editorcontent.component.html',
  styleUrls: ['./editorcontent.component.css']
})
export class EditorContentComponent implements OnInit{
    clientURL: SafeResourceUrl;
    
    constructor(private sanitizer: DomSanitizer, private urlParser: URLParserService) {
    }
    
     ngOnInit() {
        this.clientURL = this.sanitizer.bypassSecurityTrustResourceUrl('http://localhost:8080/designer/solution/'+this.urlParser.getSolutionName()+'/index.html'+'#'+this.urlParser.getFormName());
    }
}
