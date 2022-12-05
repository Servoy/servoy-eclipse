
import { Component, OnInit, Renderer2, ViewChild, ElementRef, Inject, AfterViewInit, HostListener, Input, Output, EventEmitter } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { URLParserService } from '../services/urlparser.service';
import { WindowRefService } from '@servoy/public';
import { EditorSessionService, PaletteComp, Variant } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';

@Component({
    selector: 'designer-variantscontent',
    templateUrl: './variantscontent.component.html'
})
export class VariantsContentComponent implements OnInit {

    @Input() component: PaletteComp;

    margin = 16; //ng-popover margin
	variantItemBeingDragged: Node;
	variantsIFrame: HTMLIFrameElement;
	size: {width: number, height: number};
	activeVariant = false;

	rowInterspace = 10;
	columnInterspace = 20;
	maxFormSize = {width: 190, height: 240}; 

    
    constructor(private sanitizer: DomSanitizer, private urlParser: URLParserService, protected readonly renderer: Renderer2,
        private windowRef: WindowRefService, private editorSession: EditorSessionService, private editorContentService: EditorContentService) {
	
		this.editorSession.variantsTrigger.subscribe((value) => {
			if (this.component == value.component) {
			    this.activeVariant = true;
				this.sendStylesToVariantsForm();
			}
			else {
			    this.activeVariant = false;
			}
		});
    }

    ngOnInit() {
		this.windowRef.nativeWindow.addEventListener('message', (event) => {
            //eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
            if ( this.activeVariant)
            {
                if (event.data.id === 'variantsReady') {
                    this.sendStylesToVariantsForm();
                }
            }
        });
    }
	
	sendStylesToVariantsForm() {


		this.editorSession.getVariantsForCategory<{variants: Array<Variant>}>(this.component.styleVariantCategory).then((result: unknown) => {			
			//specifying type like 'then((result: {variants: Array<Variant>)}' is leading to undefined variants ???
			this.variantsIFrame = this.editorContentService.getDocument().getElementById('VariantsForm') as HTMLIFrameElement;
			const message = { 
				id: 'createVariants', 
				variants: result as Array<Variant>,
				model: this.component.model, 
				name: this.convertToJSName(this.component.name), 
				tag: this.component.name,
				rowInterspace: this.rowInterspace,
				columnInterspace: this.columnInterspace,
				maxFormSize: this.maxFormSize
			};
			this.variantsIFrame.contentWindow.postMessage(message, '*');
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