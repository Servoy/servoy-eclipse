import { Injectable } from '@angular/core';

@Injectable()
export class DesignerUtilsService {

    constructor() {
    }

    public adjustElementRect(node: Element, position: DOMRect) {
        if (position.width == 0 || position.height == 0) {
            let correctWidth = position.width;
            let correctHeight = position.height;
            let currentNode = node.parentElement;
            while (correctWidth == 0 || correctHeight == 0) {
                let parentPosition = currentNode.getBoundingClientRect();
                correctWidth = parentPosition.width;
                correctHeight = parentPosition.height;
                currentNode = currentNode.parentElement;
            }
            if (position.width == 0) position.width = correctWidth;
            if (position.height == 0) position.height = correctHeight;
        }
    }
}