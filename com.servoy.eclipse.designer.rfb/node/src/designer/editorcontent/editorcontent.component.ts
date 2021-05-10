import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'designer-editorcontent',
  templateUrl: './editorcontent.component.html',
  styleUrls: ['./editorcontent.component.css']
})
export class EditorContentComponent implements OnInit{
    clientURL: SafeResourceUrl;
    
    constructor(private sanitizer: DomSanitizer) {
    }
    
     ngOnInit() {
        //this.clientURL = this.sanitizer.bypassSecurityTrustResourceUrl('http://localhost:8080/solutions/aaa/index.html#orders_css2');
        this.clientURL = this.sanitizer.bypassSecurityTrustResourceUrl('http://localhost:8080/solution/aaa/index.html#orders_css2');
    }
}
