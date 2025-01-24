
import { Component, OnInit, Renderer2, Input } from '@angular/core';
import { WindowRefService } from '@servoy/public';
import { EditorSessionService, PaletteComp, Variant } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';

@Component({
    selector: 'designer-variantscontent',
    templateUrl: './variantscontent.component.html',
    styleUrls: ['./variantscontent.component.css'],
    standalone: false
})
export class VariantsContentComponent implements OnInit {

    @Input() component: PaletteComp;

	variantItemBeingDragged: Node;
	variantsIFrame: HTMLIFrameElement;
	activeVariant = false;
    firstQuery = true;


	private variantsQueryHandler: ReturnType<typeof setInterval>;
    
    constructor(protected readonly renderer: Renderer2, private windowRef: WindowRefService, 
                private editorSession: EditorSessionService, private editorContentService: EditorContentService) {
	
		this.editorSession.variantsTrigger.subscribe((value) => {
			if (this.component == value.component) {
                this.activeVariant = true;
				this.sendStylesToVariantsForm();
			}
			else {
                this.activeVariant = false;
			}
		});
		this.editorSession.variantsPopup.subscribe((value) => {
			if (value.status === 'visible') {
				if (this.variantsQueryHandler) {
					clearInterval(this.variantsQueryHandler);
				}
			}
			if (value.status === 'hidden') {
				if (this.variantsIFrame) {
					this.variantsIFrame.contentWindow.postMessage({ id: 'destroyVariants'});
				}
			}
		});
    }

    ngOnInit() {
		this.windowRef.nativeWindow.addEventListener('message', (event) => {
            //eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
            if ( this.activeVariant)
            {
                // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
                if (event.data.id === 'variantsReady') {
                    this.sendStylesToVariantsForm();
                }
            }
        });
    }

    getVariantContentMargin() {
        //TODO: find a better way to do right alignment of variantscontent component
        //maybe using divs. For now adding this line due to time contraints
        return this.component.name === 'bootstrapcomponents-button' ? '30px' : '37px';
    }

	sendStylesToVariantsForm() {
		void this.editorSession.getVariantsForCategory<{variants: Array<Variant>}>(this.component.styleVariantCategory).then((result: unknown) => {			
			//specifying type like 'then((result: {variants: Array<Variant>)}' is leading to undefined variants ???
			if (!this.variantsIFrame) {
				this.variantsIFrame = this.editorContentService.getDocument().getElementById('VariantsForm') as HTMLIFrameElement;
			}
            this.variantsIFrame.contentWindow.postMessage({ id: 'destroyVariants' }, '*');
			const message = { 
				id: 'createVariants', 
				variants: result as Array<Variant>,
				model: this.component.model, 
				name: this.convertToJSName(this.component.name), 
			};
			this.variantsIFrame.contentWindow.postMessage(message, '*');
			if (this.variantsQueryHandler) {
				clearInterval(this.variantsQueryHandler);
			}
            this.firstQuery = true;
			this.variantsQueryHandler = setInterval(() => {
                if (this.firstQuery) {
                    this.firstQuery = false;
                    return;
                }
                this.variantsIFrame.contentWindow.postMessage({ id: 'sendVariantsSize' }, '*')}, 50);
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