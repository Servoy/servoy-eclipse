import { Component, Input, HostListener, ViewEncapsulation} from '@angular/core';
import { NgbActiveModal, NgbModal, NgbModalOptions } from '@ng-bootstrap/ng-bootstrap';
import { EditorSessionService, PaletteComp } from '../../services/editorsession.service';
import { EditorContentService } from '../../services/editorcontent.service';

@Component({
  selector: 'ngbd-modal-content',
  //<!--<div class="modal-body" [ngStyle]="{'height': '150px'}">-->
  template: `
    <div class="modal-body" (mousedown)="onMouseDown($event)">
		<designer-editorcontent
			[styleVariantPreview]="true"
			(previewReady)="onPreviewReady()"
		></designer-editorcontent>
	</div>
    <div class="modal-footer" >
      <button name="variants" type="button" 
	                            class="btn btn-outline-dark btn-sm" 
	                            >
	                            <i name="variants" class="fa fa-plus" aria-hidden="true"></i>
	                            Add
	                        </button>
	                        <button name="variants" type="button" 
	                            class="btn btn-outline-dark btn-sm" 
	                            >
	                            <i name="variants" class="fa fa-pen" aria-hidden="true"></i>
	                            Edit
	                        </button>
    </div>
  `,
  styleUrls: ['./preview.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class NgbdModalContent {
  @Input() component: PaletteComp;
  @Input() width: string;
  @Input() height: string;

  constructor(public activeModal: NgbActiveModal, private editorContentService: EditorContentService) {
	console.log('constructor called');	
  }
  
  	/*@HostListener('mousedown', ['$event.target'])
  	onMouseDown() {
		console.log(event);
	}*/
	
	onMouseDown(event: Event) {
		console.log('Mousedown - from the active modal');
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

@Component({
	selector: 'ngbd-modal-component', 
	templateUrl: './preview.component.html',
	styleUrls: ['./preview.component.css'],
	encapsulation: ViewEncapsulation.None
})
export class NgbdModalComponent {
	
	@Input() component: PaletteComp;
	
  constructor(private modalService: NgbModal, protected readonly editorSession: EditorSessionService){}

  open() {
	this.editorSession.getStyleVariantFor(this.component.styleVariantCategory)
	           .then((result) => {
		
		this.component.styleVariants = result;
		//todo: compute sizes and create options class
		
    	const modalRef = this.modalService.open(NgbdModalContent, windowClass : "myCustomModalClass"});
    	modalRef.componentInstance.component = this.component;
	}).catch((err) => {
		console.log(err);
	});
	
  }
  
  	getOptions(component) {
		const size = this.getPreviewFormSize(this.component);
		
		let options: NgbModalOptions = {
			size: 'lg',
			windowClass: 'myModal',
			scrollable: true
		};
		
		return options;
	}
	
	
  	getPreviewFormSize(component: PaletteComp): {width: number, height: number} {
		const columns = component.styleVariants.length > 8 ? 3 : component.styleVariants.length > 3 ? 2 : 1;
		const size = (JSON.parse(JSON.stringify(component.model))['size']) as {width: number, height: number}; 
		const rows = Math.round(component.styleVariants.length / columns ) + 1;
		return {width: columns * (size.width + 15) + size.width / 2,
				height: rows * (size.height + 15)};
	}
	
	onMouseDown(event: MouseEvent, 
				elementName: string, 
				packageName: string, 
				model: { [property: string]: any }, 
				ghost: PaletteComp, 
				propertyName?: string, 
				propertyValue?: {[property: string]: string }, 
				componentType?: string, 
				topContainer?: boolean, 
				layoutName?: string, 
				attributes?: { [property: string]: string }, 
				children?: [{ [property: string]: string }]) {
					
		console.log('Mousedown - from preview component');
	}
}
