import { Injectable } from '@angular/core';
import { EditorSessionService } from './editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';

@Injectable()
export class DesignerUtilsService {

    constructor(protected readonly editorSession: EditorSessionService, protected readonly editorContentService: EditorContentService) {
    }

    public adjustElementRect(node: Element, position: DOMRect): DOMRect {
        if (position.width == 0 || position.height == 0) {
            if (node.parentElement.classList.contains('svy-layoutcontainer')) {
                // if the parent element is a responsive container then height or width can be nust null
                return position;
            }
            let correctWidth = position.width;
            let correctHeight = position.height;
            let currentNode = node.parentElement;
            while ((correctWidth == 0 || correctHeight == 0) && currentNode) {
                const parentPosition = currentNode.getBoundingClientRect();
				correctWidth = parentPosition.width;
				correctHeight = parentPosition.height;
				currentNode = currentNode.parentElement;
            }
            if (position.width == 0) position.width = correctWidth;
            if (position.height == 0) position.height = correctHeight;
        } else if (node.getAttribute('svy-id') != null) {
            if (node.parentElement.classList.contains('svy-layoutcontainer')) {
                return position;
            }
            let currentNode: Element = null;
            if (node.parentElement.classList.contains('svy-wrapper')) {
                currentNode = node.parentElement;
            }
            if (!currentNode && node.parentElement.parentElement.classList.contains('svy-wrapper')) {
                currentNode = node.parentElement.parentElement;
            }
            if (!currentNode && node.parentElement.parentElement.classList.contains('svy-layoutcontainer')) {
                return position;
            }
            if (!currentNode && node.parentElement.parentElement.parentElement.classList.contains('svy-wrapper')) {
                currentNode = node.parentElement.parentElement.parentElement;
            }
            if (currentNode) {
                // take position from wrapper div if available, is more accurate
                return DOMRect.fromRect(currentNode.getBoundingClientRect());
            }
        }
        return position;
    }

    convertToContentPoint(glasspane: HTMLElement, point: { x?: number; y?: number; top?: number; left?: number }): { x?: number; y?: number; top?: number; left?: number } {
        const frameRect = glasspane.getBoundingClientRect();
        if (point.x && point.y) {
            point.x = point.x - frameRect.left;
            point.y = point.y - frameRect.top;
        } else if (point.top && point.left) {
            point.left = point.left - frameRect.left;
            point.top = point.top - frameRect.top;
        }
        return point;
    }

    getDropNode(absoluteLayout: boolean, type: string, topContainer: boolean, layoutName: string, event: MouseEvent, componentName?: string, skipNodeId?: string): { dropAllowed: boolean, dropTarget?: Element, beforeChild?: Element, append?: boolean } {
        let dropTarget: Element = null;
        if (type == 'layout' || ((type == 'component' || type === 'jsmenu') && !absoluteLayout)) {
            const realName = layoutName ? layoutName : 'component';

            dropTarget = this.getNode(event, true, skipNodeId);
            if (!dropTarget) {
                const content = this.editorContentService.getContentArea();
                const formRect = content.getBoundingClientRect();
                //it can be hard to drop on bottom, so just allow it to the end
                const isForm = event.clientX > formRect.left && event.clientX < formRect.right &&
                    event.clientY > formRect.top;
                // this is on the form, can this layout container be dropped on the form?
                if ( (!isForm || !topContainer) && !(type === 'layout' && absoluteLayout)) {
                    return {
                        dropAllowed: false
                    };
                }
                let beforeNode: Element = null;
                dropTarget = this.editorContentService.getContentForm();
                //droptarget is the form but has no svy-id
                for (let i = dropTarget.childNodes.length - 1; i >= 0; i--) {
                    const node = dropTarget.childNodes[i] as HTMLElement;
                    if (node && node.nodeType === Node.ELEMENT_NODE && node.getAttribute('svy-id')) {
                        const clientRec = node.getBoundingClientRect();
                        const absolutePoint = //this.convertToAbsolutePoint(doc, 
                        {
                            x: clientRec.right,
                            y: clientRec.bottom
                        };//);
                        // if cursor is in rectangle between 0,0 and bottom right corner of component we consider it to be before that component
                        // can we enhance it ?
                        if (event.pageY < absolutePoint.y && event.pageX < absolutePoint.x) {
                            beforeNode = node;
                        }
                        else
                            break;

                    }
                }
                return {
                    dropAllowed: true,
                    dropTarget: null,
                    beforeChild: beforeNode
                };
            } else {
                const realDropTarget = this.getParent(dropTarget, realName);
                if (realDropTarget == null) {
                    return {
                        dropAllowed: false
                    };
                } else if (realDropTarget != dropTarget) {
                    const drop = dropTarget.clientWidth == 0 && dropTarget.clientHeight == 0 && dropTarget.firstElementChild ? dropTarget.firstElementChild : dropTarget;
                    const clientRec = drop.getBoundingClientRect();
                    const bottomPixels = (clientRec.bottom - clientRec.top) * 0.3;
                    const rightPixels = (clientRec.right - clientRec.left) * 0.3;
                    const absolutePoint = this.convertToAbsolutePoint({
                        x: clientRec.right,
                        y: clientRec.bottom
                    });
                    if (event.pageY > (absolutePoint.y - bottomPixels) || event.pageX > (absolutePoint.x - rightPixels)) {
                        // this is in the 30% corner (bottom or right) of the component
                        // the beforeChild should be a sibling of the dropTarget (or empty if it is the last)

                        dropTarget = this.getNextElementSibling(dropTarget);
                        //realDropTarget = this.getParent(dropTarget, realName);

                        // if there is no nextElementSibling then force it to append so that it is moved to the last position.
                        if (!dropTarget) {
                            return {
                                dropAllowed: true,
                                dropTarget: realDropTarget,
                                append: true
                            };
                        }
                    }
                    if (dropTarget && !dropTarget.getAttribute('svy-id')) {
                        dropTarget = this.getNextElementSibling(dropTarget);
                    }
                    return {
                        dropAllowed: true,
                        dropTarget: realDropTarget,
                        beforeChild: dropTarget
                    };
                }
                else {
                    // we drop directly on the node, try to determine its position between children
                    let beforeNode: Element = null;
                    for (let i = dropTarget.childNodes.length - 1; i >= 0; i--) {
                        let node = dropTarget.childNodes[i] as HTMLElement;
                        if (node && node.nodeType === Node.ELEMENT_NODE && !node.getAttribute('svy-id')){
                            node = node.querySelector('[svy-id]');
                        }
                        if (node && node.nodeType === Node.ELEMENT_NODE && node.getAttribute('svy-id')) {
                            const clientRec = node.getBoundingClientRect();
                            const absolutePoint = this.convertToAbsolutePoint({
                                x: clientRec.right,
                                y: clientRec.bottom
                            });
                            // if cursor is in rectangle between 0,0 and bottom right corner of component we consider it to be before that component
                            // can we enhance it ?
                            if (event.pageY < absolutePoint.y && event.pageX < absolutePoint.x) {
                                beforeNode = node;
                            }
                            else
                                break;

                        }
                    }
                    return {
                        dropAllowed: true,
                        dropTarget: realDropTarget,
                        beforeChild: beforeNode
                    };
                }
            }
        } else if (type != 'jsmenu' && type != 'component' && type != 'template') {
            dropTarget = this.getNode(event);
            if (dropTarget && dropTarget.getAttribute('svy-types')) {
                if (dropTarget.getAttribute('svy-types').indexOf(type) <= 0)
                    return {
                        dropAllowed: false
                    }; // the drop target doesn't support this type
            } else return {
                dropAllowed: false
            }; // ghost has no drop target or the drop target doesn't support any types
        } else {
            dropTarget = this.getNode(event, true);
            if (componentName !== undefined && dropTarget && dropTarget.getAttribute('svy-forbidden-components')) {
                if (dropTarget.getAttribute('svy-forbidden-components').indexOf(componentName) > 0)
                    return {
                        dropAllowed: false
                    }; // the drop target doesn't support this component
            }
            if (absoluteLayout && dropTarget && dropTarget.closest('.svy-responsivecontainer')){
                // we must find the drop target in responsive mode
                return this.getDropNode(false, type, topContainer, layoutName, event, componentName, skipNodeId);
            }
        }
        return {
            dropAllowed: true,
            dropTarget: dropTarget
        };
    }

    public getParent(dt: Element, realName?: string): Element {
        if (!dt) return null;
        let allowedChildren = this.editorSession.getAllowedChildrenForContainer(dt.getAttribute('svy-layoutname'));
        if (!allowedChildren || !(allowedChildren.indexOf(realName) >= 0)) {
            // maybe this is a component that has svy-types instead of svy-allowed-childrent
            const svytypes = dt.getAttribute('svy-types');
            if (svytypes) {
                allowedChildren = svytypes.split(',');
            }
            if (!allowedChildren || !(allowedChildren.indexOf(realName) >= 0)) {
                let parent = dt;
                while (parent !== null && !parent.hasAttribute('svy-id') && parent.parentElement && parent.classList.contains('svy-layoutcontainer')) {
                    parent = parent.parentElement;
                }
                return parent !== null ? this.getParent(parent.parentElement, realName) : null; // the drop target doesn't allow this layout container type
            }
        }
        return dt;
    }

    convertToAbsolutePoint(point: { x: number; y: number; }) {
        const frameRect = this.editorContentService.getContent().getBoundingClientRect()
        if (isFinite(point.x) && isFinite(point.y)) {
            point.x = point.x + frameRect.left;
            point.y = point.y + frameRect.top;
        }
        return point;
    }

    getNode(event: MouseEvent, topContainer?: boolean, skipNodeId?: string): HTMLElement {
        const point = { x: event.pageX, y: event.pageY };
        point.x = point.x - this.editorContentService.getLeftPositionIframe();
        point.y = point.y - this.editorContentService.getTopPositionIframe();
        let elements = this.editorContentService.getAllContentElements();
        if (skipNodeId) {
            elements = elements.filter(item => !(item.parentNode as Element).closest('[svy-id="' + skipNodeId + '"]'));
        }
        const found = elements.reverse().find((node) => {
            const position = this.adjustElementRect(node, node.getBoundingClientRect());
            
            if (node['offsetParent'] !== null && position.x <= point.x && position.x + position.width >= point.x && position.y <= point.y && position.y + position.height >= point.y) {
                return node;
            }
            else if (node['offsetParent'] !== null) {
                const computedStyle = window.getComputedStyle(node, ':before');
                if (parseInt(computedStyle.height) > 0) {
                    //the top and left positions of the before pseudo element are computed as the sum of:
                    //top/left position of the element, padding Top/Left of the element and margin Top/Left of the pseudo element
                    const nodeComputedStyle = window.getComputedStyle(node);
                    const top = position.top + parseInt(nodeComputedStyle.paddingTop) + parseInt(computedStyle.marginTop);
                    const left = position.left + parseInt(nodeComputedStyle.paddingLeft) + parseInt(computedStyle.marginLeft);
                    const height = parseInt(nodeComputedStyle.height);
                    const width = parseInt(nodeComputedStyle.width);
                    if (point.y >= top && point.x >= left && point.y <= top + height && point.x <= left + width) {
                        return node;
                    }
                }
            }
        });
        return found;
    }
    
    getNodeBasedOnSelectionFCorLFC() {
		const elements = this.editorContentService.getAllContentElements();
		const selectedNodeID = this.editorSession.getSelection().length === 1 && this.editorSession.getSelection()[0] || null;
		if (elements.length && selectedNodeID) {
			const node = elements.filter(item => item.getAttribute('svy-id') === selectedNodeID && (item.classList.contains('svy-formcomponent') || item.classList.contains('svy-listformcomponent')));
			return node.length === 1 ? node[0] : null;
		}
		return null;
	}

    isTopContainer(layoutName: string) {
        const packages = this.editorSession.getState().packages;
        for (let i = 0; i < packages.length; i++) {
            if (packages[i].components[0] && packages[i].components[0].componentType === 'layout') {
                for (let j = 0; j < packages[i].components.length; j++) {
                    if (packages[i].components[j].topContainer && packages[i].packageName + '.' + packages[i].components[j].layoutName === layoutName) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    isInsideAutoscrollElementClientBounds(autoscrollElementClientBounds: Array<DOMRect>, clientX: number, clientY: number): boolean {
        if (!autoscrollElementClientBounds) return false;

        let inside = false;
        let i = 0;
        while (!inside && i < 4) { // 4 == autoscrollElementClientBounds.length always
            const rect = autoscrollElementClientBounds[i++];
            inside = (clientX >= rect.left && clientX <= rect.right && clientY >= rect.top && clientY <= rect.bottom);
        }
        return inside;
    }

    getAutoscrollElementClientBounds(): Array<DOMRect> {
        const bottomAutoscrollArea = this.editorContentService.querySelector('.bottomAutoscrollArea');

        let autoscrollElementClientBounds: Array<DOMRect>;
        if (bottomAutoscrollArea) {
            autoscrollElementClientBounds = [];
            autoscrollElementClientBounds[0] = bottomAutoscrollArea.getBoundingClientRect();
            autoscrollElementClientBounds[1] = this.editorContentService.querySelector('.rightAutoscrollArea').getBoundingClientRect();
            autoscrollElementClientBounds[2] = this.editorContentService.querySelector('.leftAutoscrollArea').getBoundingClientRect();
            autoscrollElementClientBounds[3] = this.editorContentService.querySelector('.topAutoscrollArea').getBoundingClientRect();
        }
        return autoscrollElementClientBounds;
    }
    
     getNextElementSibling(element) : Element{
        // find the correct sibbling (the one which has the svy-id)
        const originalElement = element;
        while ( element.parentElement && !element.parentElement.getAttribute('svy-id')){
            element = element.parentElement;
        }
        let sibbling = element.nextElementSibling;
        if (sibbling && !sibbling.getAttribute('svy-id')){
            sibbling = sibbling.querySelector('[svy-id]');
        }
        if (sibbling === null) {
			sibbling = originalElement.nextElementSibling;
		}
        return sibbling;
    }
}