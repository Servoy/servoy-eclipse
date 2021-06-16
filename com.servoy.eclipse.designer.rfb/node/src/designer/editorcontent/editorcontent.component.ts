import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { WebsocketService } from '@servoy/sablo';

@Component({
  selector: 'designer-editorcontent',
  templateUrl: './editorcontent.component.html',
  styleUrls: ['./editorcontent.component.css']
})
export class EditorContentComponent implements OnInit{
    clientURL: SafeResourceUrl;
    
    constructor(private sanitizer: DomSanitizer, private websocketService: WebsocketService) {
    }
    
     ngOnInit() {
        //this.clientURL = this.sanitizer.bypassSecurityTrustResourceUrl('http://localhost:8080/solutions/aaa/index.html#orders_css2');
        //this.clientURL = this.sanitizer.bypassSecurityTrustResourceUrl('http://localhost:8080/solution/aaa/index.html#orders_css2');
        this.clientURL = this.sanitizer.bypassSecurityTrustResourceUrl('http://localhost:8080/designer/solution/'+this.websocketService.getURLParameter('s')+'/index.html'+'#'+this.websocketService.getURLParameter('f'));
    }
}
