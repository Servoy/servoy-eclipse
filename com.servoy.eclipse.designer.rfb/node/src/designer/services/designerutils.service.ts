import { Injectable } from '@angular/core';
import { EditorSessionService } from './editorsession.service';

@Injectable()
export class DesignerUtilsService {

    constructor(protected readonly editorSession: EditorSessionService) {
    }

    public adjustElementRect(node: Element, position: DOMRect) {
        if (position.width == 0 || position.height == 0) {
            if (node.parentElement.classList.contains('svy-layoutcontainer')) {
                // if the parent element is a responsive container then height or width can be nust null
                return;
            }
            let correctWidth = position.width;
            let correctHeight = position.height;
            let currentNode = node.parentElement;
            while (correctWidth == 0 || correctHeight == 0) {
                const parentPosition = currentNode.getBoundingClientRect();
                correctWidth = parentPosition.width;
                correctHeight = parentPosition.height;
                currentNode = currentNode.parentElement;
            }
            if (position.width == 0) position.width = correctWidth;
            if (position.height == 0) position.height = correctHeight;
        } else if (node.getAttribute('svy-id') != null) {
            if (node.parentElement.classList.contains('svy-layoutcontainer')) {
                return;
            }
            let currentNode = null;
            if (node.parentElement.classList.contains('svy-wrapper')) {
                currentNode = node.parentElement;
            }
            if (!currentNode && node.parentElement.parentElement.classList.contains('svy-wrapper')) {
                currentNode = node.parentElement.parentElement;
            }
            if (!currentNode && node.parentElement.parentElement.classList.contains('svy-layoutcontainer')) {
                return;
            }
            if (!currentNode && node.parentElement.parentElement.parentElement.classList.contains('svy-wrapper')) {
                currentNode = node.parentElement.parentElement.parentElement;
            }
            if (currentNode) {
                // take position from wrapper div if available, is more accurate
                const parentPosition = currentNode.getBoundingClientRect();
                position.width = parentPosition.width;
                position.height = parentPosition.height;
            }
        }
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

    getDropNode(doc: Document, type: string, topContainer: boolean, layoutName: string, event: MouseEvent, componentName?: string, skipNodeId?: string): { dropAllowed: boolean, dropTarget?: Element, beforeChild?: Element, append?: boolean } {
        let dropTarget = null;
        if (type == "layout" || (type == "component" && !this.editorSession.isAbsoluteFormLayout())) {
            const realName = layoutName ? layoutName : "component";

            dropTarget = this.getNode(doc, event, true, skipNodeId);
            if (!dropTarget) {
                const content = doc.querySelector('.contentframe');
                const formRect = content.getBoundingClientRect();
                //it can be hard to drop on bottom, so just allow it to the end
                const isForm = event.clientX > formRect.left && event.clientX < formRect.right &&
                    event.clientY > formRect.top;
                // this is on the form, can this layout container be dropped on the form?
                if (!isForm || !topContainer) {
                    return {
                        dropAllowed: false
                    };
                }
                let beforeNode = null;
                dropTarget = doc.querySelector('iframe').contentWindow.document.body.querySelector('.svy-form');
                //droptarget is the form but has no svy-id
                for (let i = dropTarget.childNodes.length - 1; i >= 0; i--) {
                    const node = dropTarget.childNodes[i];
                    if (node && node.getAttribute && node.getAttribute('svy-id')) {
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
                let realDropTarget = this.getParent(dropTarget, realName);
                if (realDropTarget == null) {
                    return {
                        dropAllowed: false
                    };
                } else if (realDropTarget != dropTarget) {
                    const drop = dropTarget.clientWidth == 0 && dropTarget.clientHeight == 0 && dropTarget.firstElementChild ? dropTarget.firstElementChild : dropTarget;
                    const clientRec = drop.getBoundingClientRect();
                    const bottomPixels = (clientRec.bottom - clientRec.top) * 0.3;
                    const rightPixels = (clientRec.right - clientRec.left) * 0.3;
                    const absolutePoint = this.convertToAbsolutePoint(doc, {
                        x: clientRec.right,
                        y: clientRec.bottom
                    });
                    if (event.pageY > (absolutePoint.y - bottomPixels) || event.pageX > (absolutePoint.x - rightPixels)) {
                        // this is in the 30% corner (bottom or right) of the component
                        // the beforeChild should be a sibling of the dropTarget (or empty if it is the last)

                        dropTarget = dropTarget.nextElementSibling;
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
                        dropTarget = dropTarget.nextElementSibling;
                    }
                    return {
                        dropAllowed: true,
                        dropTarget: realDropTarget,
                        beforeChild: dropTarget
                    };
                }
                else {
                    // we drop directly on the node, try to determine its position between children
                    let beforeNode = null;
                    for (let i = dropTarget.childNodes.length - 1; i >= 0; i--) {
                        const node = dropTarget.childNodes[i];
                        if (node && node.getAttribute && node.getAttribute('svy-id')) {
                            const clientRec = node.getBoundingClientRect();
                            const absolutePoint = this.convertToAbsolutePoint(doc, {
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
        } else if (type != "component" && type != "template") {
            dropTarget = this.getNode(doc, event);
            if (dropTarget && dropTarget.getAttribute("svy-types")) {
                if (dropTarget.getAttribute("svy-types").indexOf(type) <= 0)
                    return {
                        dropAllowed: false
                    }; // the drop target doesn't support this type
            } else return {
                dropAllowed: false
            }; // ghost has no drop target or the drop target doesn't support any types
        } else {
            dropTarget = this.getNode(doc, event, true);
            if (componentName !== undefined && dropTarget && dropTarget.getAttribute("svy-forbidden-components")) {
                if (dropTarget.getAttribute("svy-forbidden-components").indexOf(componentName) > 0)
                    return {
                        dropAllowed: false
                    }; // the drop target doesn't support this component
            }
        }
        return {
            dropAllowed: true,
            dropTarget: dropTarget
        };
    }

    public getParent(dt: Element, realName?: string): Element {
        if (!dt) return null;
        let allowedChildren: any = this.editorSession.getAllowedChildrenForContainer(dt.getAttribute("svy-layoutname"));
        if (!allowedChildren || !(allowedChildren.indexOf(realName) >= 0)) {
            // maybe this is a component that has svy-types instead of svy-allowed-childrent
            allowedChildren = dt.getAttribute("svy-types");
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

    convertToAbsolutePoint(doc: Document, point: { x: number; y: number; }) {
        const frameRect = doc.querySelector('.content').getBoundingClientRect()
        if (isFinite(point.x) && isFinite(point.y)) {
            point.x = point.x + frameRect.left;
            point.y = point.y + frameRect.top;
        }
        return point;
    }

    getNode(doc: Document, event: any, topContainer?: boolean, skipNodeId?: string): Element {
        let point = { x: event.pageX, y: event.pageY };
        let frameElem = doc.querySelector('iframe');
        let frameRect = frameElem.getBoundingClientRect();
        point.x = point.x - frameRect.left;
        point.y = point.y - frameRect.top;
        let elements = Array.from(frameElem.contentWindow.document.querySelectorAll('[svy-id]'));
        if (skipNodeId) {
            elements = elements.filter(item => !(item.parentNode as Element).closest('[svy-id="' + skipNodeId + '"]'));
        }
        let found = elements.reverse().find((node) => {
            let position = node.getBoundingClientRect();
            this.adjustElementRect(node, position);
            if (node['offsetParent'] !== null && position.x <= point.x && position.x + position.width >= point.x && position.y <= point.y && position.y + position.height >= point.y) {
                return node;
            }
            else if (node['offsetParent'] !== null && parseInt(window.getComputedStyle(node, ":before").height) > 0) {
                const computedStyle = window.getComputedStyle(node, ":before");
                //the top and left positions of the before pseudo element are computed as the sum of:
                //top/left position of the element, padding Top/Left of the element and margin Top/Left of the pseudo element
                const top = position.top + parseInt(window.getComputedStyle(node).paddingTop) + parseInt(computedStyle.marginTop);
                const left = position.left + parseInt(window.getComputedStyle(node).paddingLeft) + parseInt(computedStyle.marginLeft);
                const height = parseInt(computedStyle.height);
                const width = parseInt(computedStyle.width);
                if (point.y >= top && point.x >= left && point.y <= top + height && point.x <= left + width) {
                    return node;
                }
            }
        });
        return found;
    }

    isTopContainer(layoutName: any) {
        const packages = this.editorSession.getState().packages;
        for (var i = 0; i < packages.length; i++) {
            if (packages[i].components[0] && packages[i].components[0].componentType === "layout") {
                for (var j = 0; j < packages[i].components.length; j++) {
                    if (packages[i].components[j].topContainer && packages[i].packageName + "." + packages[i].components[j].layoutName === layoutName) {
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

    getAutoscrollElementClientBounds(doc: Document): Array<DOMRect> {
        const bottomAutoscrollArea: HTMLElement = doc.querySelector('.bottomAutoscrollArea');

        let autoscrollElementClientBounds: Array<DOMRect>;
        if (bottomAutoscrollArea) {
            autoscrollElementClientBounds = [];
            autoscrollElementClientBounds[0] = bottomAutoscrollArea.getBoundingClientRect();
            autoscrollElementClientBounds[1] = doc.querySelector('.rightAutoscrollArea').getBoundingClientRect();
            autoscrollElementClientBounds[2] = doc.querySelector('.leftAutoscrollArea').getBoundingClientRect();
            autoscrollElementClientBounds[3] = doc.querySelector('.topAutoscrollArea').getBoundingClientRect();
        }
        return autoscrollElementClientBounds;
    }
}