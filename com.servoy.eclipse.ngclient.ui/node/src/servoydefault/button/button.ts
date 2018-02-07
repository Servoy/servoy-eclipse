import { Component, OnInit, Input ,Output,EventEmitter,Renderer,ElementRef} from '@angular/core';

@Component({
  selector: 'servoydefault-button',
  templateUrl: './button.html',
  styleUrls: ['./button.css']
})
export class ServoyDefaultButton implements OnInit {
 @Input()  name;
 @Input() text;
 @Input() onActionMethodID;
 
 private readonly renderer: Renderer; 
 private  readonly elementRef: ElementRef;
  
 constructor(renderer: Renderer, elementRef: ElementRef)
{
     this.renderer = renderer;
     this.elementRef =elementRef; 
}
 
 ngOnInit() {
     if (this.onActionMethodID) {
         this.renderer.listen(this.elementRef.nativeElement, 'click', (e) =>{
             this.onActionMethodID(e);
         });
     } 
 }
}
