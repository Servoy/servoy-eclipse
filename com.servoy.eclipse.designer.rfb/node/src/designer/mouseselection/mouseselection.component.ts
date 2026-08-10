import {
  Component, OnInit, AfterViewInit, ElementRef, Renderer2,
  OnDestroy, Directive, ChangeDetectionStrategy, inject, input, forwardRef,
  viewChild, viewChildren, effect, signal
} from '@angular/core';
import { EditorSessionService, ISelectionChangedListener } from '../services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';
import { DesignerUtilsService } from '../services/designerutils.service';
import { EditorContentService, IContentMessageListener } from '../services/editorcontent.service';
import { NgStyle } from '@angular/common';
import { ResizeKnobDirective } from '../directives/resizeknob.directive';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'selection-decorators',
    templateUrl: './mouseselection.component.html',
    styleUrls: ['./mouseselection.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [NgStyle, ResizeKnobDirective, forwardRef(() => PositionMenuDirective)]
})
// this should include lasso and all selection logic from mouseselection.js and dragselection.js
export class MouseSelectionComponent implements OnInit, AfterViewInit, ISelectionChangedListener, OnDestroy, IContentMessageListener {

    readonly lassoRef = viewChild.required<ElementRef<HTMLElement>>('lasso');
    readonly selectedRef = viewChildren<ElementRef<HTMLElement>>('selected');

    readonly nodes = signal<SelectionNode[]>([]);
    contentInit = false;
    topAdjust!: number;
    leftAdjust!: number;
    lassostarted = false;
    lastTimestamp!: number;
    moveFCorLFC = false;
    mouseDownEvent: MouseEvent | null = null;

    mousedownpoint!: Point;
    fieldLocation!: Point;
    removeSelectionChangedListener!: () => void;

    public readonly editorSession = inject(EditorSessionService);
    protected readonly renderer = inject(Renderer2);
    protected urlParser = inject(URLParserService);
    protected designerUtilsService = inject(DesignerUtilsService);
    private editorContentService = inject(EditorContentService);

    constructor() {
        this.editorContentService.addContentMessageListener(this);
        this.removeSelectionChangedListener = this.editorSession.addSelectionChangedListener(this);
        effect(() => {
            this.selectedRef();
            if (this.editorSession.showWireframe()) {
                this.applyWireframe();
            }
        });
    }

    ngOnInit(): void {
        void this.editorSession.requestSelection();
        this.editorContentService.getGlassPane().addEventListener('mousedown', (event) => this.onMouseDown(event));
        this.editorContentService.getGlassPane().addEventListener('mouseup', (event) => this.onMouseUp(event));
        this.editorContentService.getGlassPane().addEventListener('mousemove', (event) => this.onMouseMove(event));
    }

    ngOnDestroy(): void {
        this.removeSelectionChangedListener();
        this.editorContentService.removeContentMessageListener(this);
    }

    ngAfterViewInit(): void {
        this.editorContentService.executeOnlyAfterInit(() => {
            this.contentInit = true;
            this.calculateAdjustToMainRelativeLocation();
            setTimeout(()=>{
				this.createNodes(this.editorSession.getSelection());
			}, 50);
        });
    }
    redrawDecorators() {
        const currentNodes = this.nodes();
        if (currentNodes.length > 0) {
            this.nodes.set(currentNodes.map((selected: SelectionNode) => {
                const node = this.editorContentService.getContentElement(selected.svyid);
                if (!node) return selected;
                const position = this.designerUtilsService.adjustElementRect(node, node.getBoundingClientRect());
                return {
                    ...selected,
                    style: {
                        height: position.height + 'px',
                        width: position.width + 'px',
                        top: position.top + this.topAdjust + 'px',
                        left: position.left + this.leftAdjust + 'px',
                        display: 'block'
                    } as CSSStyleDeclaration
                };
            }));
        }
    }

    selectionChanged(selection: string[], redrawDecorators?: boolean): void {
        if (this.contentInit) {
            this.createNodes(selection);
        }
        if (redrawDecorators) {
            this.redrawDecorators();
        }
    }

    contentMessageReceived(id: string, _data: { property: string }) {
        if (id === 'redrawDecorators') {
            this.selectionChanged(this.editorSession.getSelection(), true);
        }
    }

    private createNodes(selection: string[]) {
        this.createNodesImpl(selection);
    }

    private createNodesImpl(selection: string[]) {
        if (selection.length > 0) {
            this.editorContentService.executeOnlyAfterInit(() => {
                const newNodes = new Array<SelectionNode>();
                const elements = this.editorContentService.getAllContentElements();
                Array.from(elements).forEach(node => {
                    if (selection.indexOf(node.getAttribute('svy-id')!) >= 0) {
                        const position =  this.designerUtilsService.adjustElementRect(node, node.getBoundingClientRect());
                        const style = {
                            height: position.height + 'px',
                            width: position.width + 'px',
                            top: position.top + this.topAdjust + 'px',
                            left: position.left + this.leftAdjust + 'px',
                            display: 'block'
                        } as CSSStyleDeclaration;
                        const layoutName = node.getAttribute('svy-layoutname');
                        newNodes.push({
                            style: style,
                            isResizable: this.urlParser.isAbsoluteFormLayout() && !node.parentElement!.closest('.svy-responsivecontainer')
                            ? { t: true, l: true, b: true, r: true } : { t: false, l: false, b: false, r: false },
                            svyid: node.getAttribute('svy-id')!,
                            isContainer: layoutName != null && !node.closest('.svy-responsivecontainer'),
                            maxLevelDesign: node.classList.contains('maxLevelDesign'),
                            containerName: layoutName!,
                            autowizardProperties: this.editorSession.getWizardProperties(node.getAttribute('svy-formelement-type')!),
                            isFCorLFC: this.isSelectionFCorLFC()
                        })
                    }
                });
                this.nodes.set(newNodes);
            });
        } else {
            this.nodes.set([]);
        }
    }

    private calculateAdjustToMainRelativeLocation() {
        if (!this.topAdjust) {
            const computedStyle = window.getComputedStyle(this.editorContentService.getContentArea(), null)
            this.topAdjust = parseInt(computedStyle.getPropertyValue('padding-left').replace('px', ''));
            this.leftAdjust = parseInt(computedStyle.getPropertyValue('padding-top').replace('px', ''))
        }
    }

    private onMouseDown(event: MouseEvent) {
		this.mouseDownEvent = event;
        this.fieldLocation = { x: event.pageX, y: event.pageY };
        if (this.editorSession.dragging() || this.editorSession.ghosthandle()) return;
        let found;
        if (this.moveFCorLFC) {
			found = this.designerUtilsService.getNodeBasedOnSelectionFCorLFC();
			this.moveFCorLFC = false;
		} else {
			found = this.designerUtilsService.getNode(event);
		}
        if (found) {
            if (this.editorSession.getSelection().indexOf(found.getAttribute('svy-id')!) !== -1) {
                return;  //already selected
            } else if (!event.ctrlKey && !event.metaKey && !event.shiftKey) {
                let wrapper = found.parentElement;
                while (wrapper && !wrapper.classList.contains('svy-wrapper')) {
                    wrapper = wrapper.parentElement;
                }
                if (!wrapper || wrapper.style.visibility !== 'hidden') {
                    this.editorSession.setSelection([found.getAttribute('svy-id')!], this);
                }
            }
        } else {
            //lasso select
            this.nodes.set([]);
            this.editorSession.setSelection([], this);
            const contentRect = this.editorContentService.getContentArea().getBoundingClientRect();
            const lassoRef = this.lassoRef();
            this.renderer.setStyle(lassoRef.nativeElement, 'left', event.pageX + this.editorContentService.getContentArea().scrollLeft - contentRect?.left + 'px');
            this.renderer.setStyle(lassoRef.nativeElement, 'top', event.pageY + this.editorContentService.getContentArea().scrollTop - contentRect?.top + 'px');
            this.renderer.setStyle(lassoRef.nativeElement, 'width', '0px');
            this.renderer.setStyle(lassoRef.nativeElement, 'height', '0px');

            if (event.button !== 2) {
                this.lassostarted = true;
                this.mousedownpoint = { x: event.pageX, y: event.pageY };
            }
        }
    }

    private onMouseUp(event: MouseEvent) {
		let isNewSelection = false;
		if (this.mouseDownEvent && this.mouseDownEvent.x === event.x && this.mouseDownEvent.y === event.y) {
			isNewSelection = true;
		}
        if (this.fieldLocation && this.fieldLocation.x == event.pageX && this.fieldLocation.y == event.pageY) {
            const contentRect = this.editorContentService.getContentArea().getBoundingClientRect();
            this.editorSession.updateFieldPositioner({
                x: event.pageX + this.editorContentService.getContentArea().scrollLeft - contentRect?.left - this.leftAdjust,
                y: event.pageY + this.editorContentService.getContentArea().scrollTop - contentRect?.top - this.topAdjust
            });
        }
        this.fieldLocation = null!;
        if (this.editorSession.dragging() || this.editorSession.ghosthandle()) return;
        if (event.button == 2 && this.editorSession.getSelection().length > 1) {
            //if we right click on the selected element while multiple selection, just show context menu and do not modify selection
            const node = this.designerUtilsService.getNode(event);
            if (node && this.editorSession.getSelection().indexOf(node.getAttribute('svy-id')!) !== -1) {
                return;
            }
        }

        if (this.lassostarted && this.mousedownpoint.x != event.pageX && this.mousedownpoint.y != event.pageY) {
            const elements = this.editorContentService.getAllContentElements();
            const newNodes = new Array<SelectionNode>();
            const newSelection = new Array<string>();
            Array.from(elements).forEach((node) => {
                let wrapper = node.parentElement;
                while (wrapper && !wrapper.classList.contains('svy-wrapper')) wrapper = wrapper.parentElement;
                if (!(wrapper && wrapper.style.visibility === 'hidden')) {
                    const position =  this.designerUtilsService.adjustElementRect(node, node.getBoundingClientRect());
                    const iframeLeft = this.editorContentService.getLeftPositionIframe();
                    const iframeTop = this.editorContentService.getTopPositionIframe();
                    const rect1 = new DOMRect(
                        Math.min(event.pageX, this.mousedownpoint.x), Math.min(event.pageY, this.mousedownpoint.y),
                        Math.abs(event.pageX - this.mousedownpoint.x), Math.abs(event.pageY - this.mousedownpoint.y)
                    )
                    const rect2 = new DOMRect(position.x + iframeLeft, position.y + iframeTop, position.width, position.height);
					const compFullInside = this.urlParser.isMarqueeSelectOuter();
					if (this.rectanglesIntersect(rect1, rect2, compFullInside)) {
                        const layoutName = node.getAttribute('svy-layoutname');
                        const newNode: SelectionNode = {
                            style: {
                                height: position.height + 'px',
                                width: position.width + 'px',
                                top: position.top + this.topAdjust + 'px',
                                left: position.left + this.leftAdjust + 'px',
                                display: 'block'
                            } as CSSStyleDeclaration,
                            svyid: node.getAttribute('svy-id')!,
                            isResizable: this.urlParser.isAbsoluteFormLayout()
                                && !node.parentElement!.closest('.svy-responsivecontainer')
                                ? { t: true, l: true, b: true, r: true } : { t: false, l: false, b: false, r: false },
                            isContainer: layoutName != null && !node.closest('.svy-responsivecontainer'),
                            maxLevelDesign: node.classList.contains('maxLevelDesign'),
                            containerName: layoutName!,
                            autowizardProperties: this.editorSession.getWizardProperties(node.getAttribute('svy-formelement-type')!),
                            isFCorLFC: this.isSelectionFCorLFC()
                        };
                        newNodes.push(newNode);
                        newSelection.push(node.getAttribute('svy-id')!)
                    }
                }
            });
            this.nodes.set(newNodes);
            this.editorSession.setSelection(newSelection, this);
        } else {
            const point = { x: event.pageX, y: event.pageY };
            point.x = point.x - this.editorContentService.getLeftPositionIframe();
            point.y = point.y - this.editorContentService.getTopPositionIframe();
            this.calculateAdjustToMainRelativeLocation();

            const elements = this.editorContentService.getAllContentElements();
            const newNode = Array.from(elements).reverse().find((node) => {
                const position = this.designerUtilsService.adjustElementRect(node, node.getBoundingClientRect());
                let addToSelection = false;
                if (node['offsetParent'] !== null && position.x <= point.x && position.x + position.width >= point.x && position.y <= point.y && position.y + position.height >= point.y) {
                    let wrapper = node.parentElement;
                    while (wrapper && !wrapper.classList.contains('svy-wrapper')) wrapper = wrapper.parentElement;
                    addToSelection = wrapper && wrapper.style.visibility === 'hidden' ? false : true;

                } else if (node['offsetParent'] !== null && parseInt(window.getComputedStyle(node, ':before').height) > 0) {
                    const computedStyle = window.getComputedStyle(node, ':before');
                    //the top and left positions of the before pseudo element are computed as the sum of:
                    //top/left position of the element, padding Top/Left of the element and margin Top/Left of the pseudo element
                    const top = position.top + parseInt(window.getComputedStyle(node).paddingTop) + parseInt(computedStyle.marginTop);
                    const left = position.left + parseInt(window.getComputedStyle(node).paddingLeft) + parseInt(computedStyle.marginLeft);
                    const height = parseInt(computedStyle.height);
                    const width = parseInt(computedStyle.width);
                    if (point.y >= top && point.x >= left && point.y <= top + height && point.x <= left + width) {
                        addToSelection = true;
                    }
                }
                if (addToSelection) {
                    const id = node.getAttribute('svy-id')!;
                    let selection = this.editorSession.getSelection();
                    if (selection && selection.length > 0 && event.shiftKey) return node;
                    const layoutName = node.getAttribute('svy-layoutname');
                    const newNode = {
                        style: {
                            height: position.height + 'px',
                            width: position.width + 'px',
                            top: position.top + this.topAdjust + 'px',
                            left: position.left + this.leftAdjust + 'px',
                            display: 'block'
                        } as CSSStyleDeclaration,
                        isResizable: this.urlParser.isAbsoluteFormLayout()
                            && !node.parentElement!.closest('.svy-responsivecontainer')
                            ? { t: true, l: true, b: true, r: true } : { t: false, l: false, b: false, r: false },
                        svyid: node.getAttribute('svy-id')!,
                        isContainer: layoutName != null && !node.closest('.svy-responsivecontainer'),
                        maxLevelDesign: node.classList.contains('maxLevelDesign'),
                        containerName: layoutName!,
                        autowizardProperties: this.editorSession.getWizardProperties(node.getAttribute('svy-formelement-type')!),
                        isFCorLFC: this.isSelectionFCorLFC()
                    };
                    if (event.ctrlKey || event.metaKey) {
                        const index = selection.indexOf(id);
                        if (index >= 0) {
                            const current = this.nodes();
                            this.nodes.set([...current.slice(0, index), ...current.slice(index + 1)]);
                            selection.splice(index, 1);
                        } else {
                            this.nodes.set([...this.nodes(), newNode]);
                            selection.push(id);
                        }
                    } else if (isNewSelection) {
                        this.nodes.set([newNode]);
                        selection = [id];
                    }
                    this.editorSession.setSelection(selection, this);
                    return node;
                }
            });
            if (event.shiftKey && newNode) {
                const selection = this.editorSession.getSelection();
                if (selection && selection.length > 0) {
                    const position1 =  this.designerUtilsService.adjustElementRect(newNode, newNode.getBoundingClientRect());
                    const element = this.editorContentService.getContentElement(selection[0]);
                    if (element) {
                        const position2 =  this.designerUtilsService.adjustElementRect(element, element.getBoundingClientRect());
                        const rect1 = new DOMRect(
                            Math.min(position1.left, position2.left), Math.min(position1.top, position2.top),
                            Math.abs(position1.left - position2.left), Math.abs(position1.top - position2.top)
                         )
                        const shiftNodes: SelectionNode[] = [];
                        Array.from(elements).forEach((node) => {
                            const position = this.designerUtilsService.adjustElementRect(node, node.getBoundingClientRect());
                            if (this.rectanglesIntersect(rect1, position, false)) {
                                const id = node.getAttribute('svy-id')!;
                                const layoutName = node.getAttribute('svy-layoutname');
                                const newNode = {
                                    style: {
                                        height: position.height + 'px',
                                        width: position.width + 'px',
                                        top: position.top + this.topAdjust + 'px',
                                        left: position.left + this.leftAdjust + 'px',
                                        display: 'block'
                                    } as CSSStyleDeclaration,
                            isResizable: this.urlParser.isAbsoluteFormLayout()
                                && !node.parentElement!.closest('.svy-responsivecontainer')
                                ? { t: true, l: true, b: true, r: true } : { t: false, l: false, b: false, r: false },
                                    svyid: node.getAttribute('svy-id')!,
                                    isContainer: layoutName != null && !node.closest('.svy-responsivecontainer'),
                                    maxLevelDesign: node.classList.contains('maxLevelDesign'),
                                    containerName: layoutName!,
                                    autowizardProperties: this.editorSession.getWizardProperties(node.getAttribute('svy-formelement-type')!),
                                    isFCorLFC: this.isSelectionFCorLFC()
                                };
                                if (!selection.includes(id)) {
                                    shiftNodes.push(newNode);
                                    selection.push(id);
                                }
                            }
                        });
                        this.nodes.set([...this.nodes(), ...shiftNodes]);
                        this.editorSession.setSelection(selection, this);
                    }
                }
            }
        }
        this.lassostarted = false;
        this.renderer.setStyle(this.lassoRef().nativeElement, 'display', 'none');
        this.applyWireframe();

        if (event.button == 0 && event.timeStamp - this.lastTimestamp < 350) {
            // dblclick event; is not triggered by event
            const currentNodes = this.nodes();
            if (currentNodes.length > 0 && currentNodes[0].maxLevelDesign) {
                this.editorSession.executeAction('zoomIn');
            }
        }
        this.lastTimestamp = event.timeStamp;
    }

    private applyWireframe() {
        this.selectedRef().forEach((selectedNode) => {
            this.applyWireframeForNode(selectedNode);
        });
    }

    applyWireframeForNode(selectedNode: ElementRef<HTMLElement>) {
        const node = this.editorContentService.getContentElement(selectedNode.nativeElement.getAttribute('id')!);
        if (node === undefined) return;
        const position = node.getBoundingClientRect();
        if (node.classList.contains('svy-layoutcontainer') && !node.getAttribute('data-maincontainer')
            && !node.classList.contains('svy-responsivecontainer') && position.width > 0 && position.height > 0) {
            this.renderer.setAttribute(selectedNode.nativeElement, 'svytitle', node.getAttribute('svy-title')!);
            if (this.editorSession.showWireframe()) {
                this.renderer.addClass(selectedNode.nativeElement, 'showWireframe');
            }
            selectedNode.nativeElement.style.setProperty('--svyBackgroundColor', window.getComputedStyle(node).backgroundColor);
            if (node.classList.contains('maxLevelDesign')) {
                //fix for IE container background, the one above is still needed for the ::before pseudoelement
                selectedNode.nativeElement.style.setProperty('backgroundColor', window.getComputedStyle(node).backgroundColor);
                this.renderer.addClass(selectedNode.nativeElement, 'maxLevelDesign');
            }
        }
    }
    
    public updateMoveFCorLFC() {
		this.moveFCorLFC = true;
	}
    
    private isSelectionFCorLFC() {
		return this.designerUtilsService.getNodeBasedOnSelectionFCorLFC() != null;
	}

    private onMouseMove(event: MouseEvent) {
        if (this.editorSession.dragging()) return;
        if (this.lassostarted) {
            const contentRect = this.editorContentService.getContentArea().getBoundingClientRect();
            const lassoRef = this.lassoRef();
            if (event.pageX < this.mousedownpoint.x) {
                this.renderer.setStyle(lassoRef.nativeElement, 'left', event.pageX + this.editorContentService.getContentArea().scrollLeft - contentRect.left + 'px');
            }
            if (event.pageY < this.mousedownpoint.y) {
                this.renderer.setStyle(lassoRef.nativeElement, 'top', event.pageY + this.editorContentService.getContentArea().scrollTop - contentRect.top + 'px');
            }
            if (lassoRef.nativeElement.style.display === 'none') {
                this.renderer.setStyle(lassoRef.nativeElement, 'display', 'block');
            }
            const currentWidth = event.pageX - this.mousedownpoint.x;
            const currentHeight = event.pageY - this.mousedownpoint.y;
            this.renderer.setStyle(lassoRef.nativeElement, 'width', Math.abs(currentWidth) + 'px');
            this.renderer.setStyle(lassoRef.nativeElement, 'height', Math.abs(currentHeight) + 'px');
        }
        if (this.editorSession.resizing()) {
			this.redrawDecorators();
		}
    }

    private rectanglesIntersect(r1: DOMRect, r2: DOMRect, compFullInside: boolean): boolean {
		if (compFullInside) {
			return (r2.left >= r1.left && 
				r2.right <= r1.right && 
				r2.top >= r1.top && 
				r2.bottom <= r1.bottom);
		}
        return !(r2.left > r1.right ||
            r2.right < r1.left ||
            r2.top > r1.bottom ||
            r2.bottom < r1.top);
    }

    deleteAction(event: MouseEvent) {
        event.stopPropagation();
        this.editorSession.keyPressed({ 'keyCode': 46 });
    }

    zoomInAction(event: MouseEvent) {
        event.stopPropagation();
        this.editorSession.executeAction('zoomIn');
    }

    copyAction(event: MouseEvent) {
        event.stopPropagation();
        this.editorSession.executeAction('copy');
    }

    openWizardAction(event: MouseEvent, property: string) {
        event.stopPropagation();
        this.editorSession.openConfigurator(property);
    }

    insertACopyAction(event: MouseEvent, node: SelectionNode, before: boolean) {
        event.stopPropagation();
        const component: Record<string, any> = {}
        const htmlNode = this.editorContentService.getContentElement(node.svyid);

        const layoutPackage = htmlNode.getAttribute('svy-layoutname')!.split('.');
        component['packageName'] = layoutPackage[0];
        component['name'] = layoutPackage[1];
        const droptarget = (htmlNode.parentNode as HTMLElement).getAttribute('svy-id');
        if (droptarget) component['dropTargetUUID'] = droptarget;
        if (before) {
            component['rightSibling'] = node.svyid;
        } else
            if (htmlNode.nextElementSibling) {
                component['rightSibling'] = htmlNode.nextElementSibling.getAttribute('svy-id');
            }

        component['keepOldSelection'] = true;
        this.editorSession.createComponent(component);
    }

    onEnter(event: MouseEvent) {
        ((event.srcElement as Node).nextSibling as HTMLElement).style.display = 'block'
    }

    onLeave(event: MouseEvent) {
        (event.srcElement as HTMLElement).style.display = 'none'
    }
    
    checkIfNodeIsVisible(node: SelectionNode) {
		const position = this.editorContentService.getContentElement(node.svyid)?.getBoundingClientRect();
		if (!position || (position.height === 0 && position.width === 0)) {
			return false;
		}
		return true;
	}
    
    notInsideFormComponent(node: SelectionNode) {
        const element = this.editorContentService.getContentElement(node.svyid);
        if (element.closest('.svy-listformcomponent') || element.closest('.svy-formcomponent')) {
            return false;
        }
        return true;
    }

}
// eslint-disable-next-line @angular-eslint/directive-selector
@Directive({ selector: '[positionMenu]' })
export class PositionMenuDirective implements OnInit {
    selectionNode = input<SelectionNode>(undefined!, { alias: 'positionMenu' });

    private editorContentService = inject(EditorContentService);
    private elementRef = inject(ElementRef<HTMLElement>);

    ngOnInit(): void {
        const htmlNode = this.editorContentService.getContentElement(this.selectionNode().svyid);
        if (parseInt(window.getComputedStyle(htmlNode, ':before').height) > 0) {
            const computedStyle = window.getComputedStyle(htmlNode, ':before');
            const left = parseInt(window.getComputedStyle(htmlNode, null).getPropertyValue('padding-left')) + parseInt(computedStyle.marginLeft);
            const right = htmlNode.getBoundingClientRect().width - left - parseInt(computedStyle.width);
            this.elementRef.nativeElement.style.marginRight = right + 'px';
        }
    }
}

export class SelectionNode {
    svyid!: string;
    style!: CSSStyleDeclaration;
    isResizable?: ResizeDefinition;
    isContainer!: boolean;
    maxLevelDesign!: boolean;
    containerName!: string;
    autowizardProperties?: string[];
    isFCorLFC!: boolean;
}
export class Point {
    x!: number;
    y!: number;
}
class ResizeDefinition {
    t!: boolean;
    l!: boolean;
    b!: boolean;
    r!: boolean;
}
