import { Injectable } from '@angular/core';
import { EditorContentService } from './editorcontent.service';
import { EditorSessionService, IShowDynamicGuidesChangedListener } from './editorsession.service';
import { BehaviorSubject } from 'rxjs/internal/BehaviorSubject';


@Injectable()
export class DynamicGuidesService implements IShowDynamicGuidesChangedListener {
    
    private leftPos: Map<string, number> = new Map();
    private rightPos: Map<string, number> = new Map();
    private topPos: Map<string, number> = new Map();
    private bottomPos : Map<string, number> = new Map();
    private middleV: Map<string, number> = new Map();
    private middleH : Map<string, number> = new Map();
    private rectangles : DOMRect[];
    private element: Element;
    private uuid: string;
    
    public snapDataListener: BehaviorSubject<SnapData>;
    private snapThreshold: number = 0;
    private equalDistanceThreshold: number = 0;
	private equalSizeThreshold: number = 0;
    guidesEnabled: boolean;
    topAdjust: any;
    leftAdjust: number;
    properties: SnapData;
	equalSize: boolean = false;

    constructor(private editorContentService: EditorContentService, protected readonly editorSession: EditorSessionService) {
        this.editorSession.addDynamicGuidesChangedListener(this);
        this.snapDataListener = new BehaviorSubject(null);
        this.editorContentService.executeOnlyAfterInit(() => {
            this.editorSession.getSnapThreshold().then((thresholds: { alignment: number, distance: number, size: number }) => {
                this.snapThreshold = thresholds.alignment;
                this.equalDistanceThreshold = thresholds.distance;
				this.equalSizeThreshold = thresholds.size;
                if (thresholds.alignment > 0 || thresholds.distance > 0 || thresholds.size > 0 ) {
                    const contentArea = this.editorContentService.getContentArea();
                    contentArea.addEventListener('mousemove', (event: MouseEvent) => this.onMouseMove(event));
                    contentArea.addEventListener('mouseup', () => this.onMouseUp());
                }
            });
        });
    }

    showDynamicGuidesChanged(showGuides: boolean): void {
        this.guidesEnabled = showGuides;
    }

    init(point: {x : number, y:number}): void {
        if (this.leftPos.size == 0) {
            this.rectangles = [];
            this.element = this.editorContentService.getContentElementsFromPoint(point).find(e => e.getAttribute('svy-id'));
            this.uuid = this.element?.getAttribute('svy-id');
            for (let comp of this.editorContentService.getAllContentElements()) {
                const id = comp.getAttribute('svy-id');
                const bounds = comp.getBoundingClientRect();
                this.leftPos.set(id, bounds.left);
                this.rightPos.set(id, bounds.right);
                this.topPos.set(id, bounds.top);
                this.bottomPos.set(id, bounds.bottom);
                this.middleV.set(id, (bounds.top + bounds.bottom) / 2);
                this.middleH.set(id, (bounds.left + bounds.right) / 2);
                if (id !== this.uuid) this.rectangles.push(bounds);
            }

            const sortfn = (a, b) => a[1] - b[1];
            this.leftPos = new Map([...this.leftPos].sort(sortfn));
            this.rightPos = new Map([...this.rightPos].sort(sortfn));
            this.topPos = new Map([...this.topPos].sort(sortfn));
            this.bottomPos = new Map([...this.bottomPos].sort(sortfn));
            this.middleH = new Map([...this.middleH].sort(sortfn));
            this.middleV = new Map([...this.middleV].sort(sortfn));
        }
    }

    onMouseUp(): void {
       this.clear();
    }

    private onMouseMove(event: MouseEvent): void {
      if (!this.guidesEnabled || !this.editorSession.getState().dragging && !this.editorSession.getState().resizing) return;

      let point = this.adjustPoint(event.pageX, event.pageY);
      if (this.leftPos.size == 0) this.init(point);
      this.computeGuides(point, this.editorSession.getState().resizing ? this.editorContentService.getGlassPane().style.cursor.split('-')[0] : null);
    }

    private adjustPoint(x: number, y:number) :{x: number, y:number} {
        let point = {x, y};
        if (!this.topAdjust) {
            const computedStyle = window.getComputedStyle(this.editorContentService.getContentArea(), null);
            this.topAdjust = parseInt(computedStyle.getPropertyValue('padding-left').replace('px', ''));
            this.leftAdjust = parseInt(computedStyle.getPropertyValue('padding-top').replace('px', ''));
        }
        const contentRect = this.editorContentService.getContentArea().getBoundingClientRect();
        point.x = point.x + this.editorContentService.getContentArea().scrollLeft - contentRect?.left - this.leftAdjust;
        point.y = point.y + this.editorContentService.getContentArea().scrollTop - contentRect?.top - this.topAdjust;
        return point;
    }

    public clear() {
        this.leftPos = new Map();
        this.rightPos = new Map();
        this.topPos = new Map();
        this.bottomPos = new Map();
        this.middleV = new Map();
        this.middleH = new Map();
        this.rectangles = [];
    }
    
    private isSnapInterval(uuid, coordinate, posMap) {
        for (let [key, value] of posMap) {
            if (key === uuid) continue;
            if ((coordinate > value - this.snapThreshold) && (coordinate < value + this.snapThreshold)) {
                //return the first component id that matches the coordinate
                return {uuid: key};
            }
        }
        return null;        
    }
    
    private getDraggedElementRect(point: {x: number, y:number}): DOMRect {
        let item;
         if (!this.element?.getAttribute('svy-id') && (item = this.editorContentService.getContentElementById('svy_draggedelement'))) {
            const r = item.getBoundingClientRect();
			return new DOMRect(point.x, point.y, r.width, r.height);
		}
        return this.element?.getBoundingClientRect();
    }
    
	computeGuides(point: { x: number, y: number }, resizing: string) {
        let elem = this.editorContentService.getContentElementsFromPoint(point).find(e => e.getAttribute('svy-id'));
        const draggedItem = this.editorContentService.getContentElementById('svy_draggedelement');
        let properties = new SnapData(point.y, point.x, {}, [] );
		if (!draggedItem && !resizing) {
            if (this.properties?.guides.length > 0 && !elem) {
                //get out of the snap mode?
                const r = this.element?.getBoundingClientRect();
                if (point.x < r.left - this.snapThreshold || point.x > r.left + r.width + this.snapThreshold ||
                    point.y < r.top - this.snapThreshold || point.y > r.top + r.height + this.snapThreshold)
                {
                    this.snapDataListener.next(properties);
                    return;
                }
            }
            else {
                this.element = elem;
            }
		}
		if (resizing) {
			if (this.equalSize) {
				 //get out of the snap mode?
				const r = this.element?.getBoundingClientRect();
				if (r) {
					const closerToTheLeft = this.pointCloserToTopOrLeftSide(point, r, 'x');
					const closerToTheTop = this.pointCloserToTopOrLeftSide(point, r, 'y');
					if ((resizing.indexOf('e') >= 0 || resizing.indexOf('w') >= 0) &&
						(closerToTheLeft && (point.x < r.left - this.equalSizeThreshold || point.x > r.left + this.equalSizeThreshold) ||
							!closerToTheLeft && (point.x < r.right - this.equalSizeThreshold || point.x > r.right + this.equalSizeThreshold)) ||
						(resizing.indexOf('s') >= 0 || resizing.indexOf('n') >= 0) &&
						(closerToTheTop && (point.y < r.top - this.equalSizeThreshold || point.y > r.top + this.equalSizeThreshold) ||
							!closerToTheTop && (point.y < r.bottom - this.equalSizeThreshold || point.y > r.bottom + this.equalSizeThreshold))) {

						this.equalSize = false;
						if (resizing.indexOf('e') >= 0 || resizing.indexOf('w') >= 0) {
							if (closerToTheLeft && point.x > r.left) {
								properties.left = point.x;
								properties.width = r.width - r.left + point.x;
							}
							if (!closerToTheLeft && point.x < r.right) {
								properties.left = r.left;
								properties.width = r.width - r.right + point.x;
							}
						}

						if (resizing.indexOf('s') >= 0 || resizing.indexOf('n') >= 0) {
							if (closerToTheTop && point.y > r.top) {
								properties.top = point.y;
								properties.height = r.height - r.top + point.y;
							}
							if (!closerToTheTop && point.y < r.bottom) {
								properties.top = r.top;
								properties.height = r.height - r.bottom + point.y;
							}
						}

						this.properties = properties;
						this.snapDataListener.next(properties);
						return;
					}
				}
			}
			else if (elem) {
				//in resize mode, we need the current element to determine the edge(resize knob) we are dragging
				//so we use the mouse position for the correct location
				//(the element rect might not always be up to date, especially when dragging faster) 
				this.element = elem;
			}
		}

		const uuid = this.element?.getAttribute('svy-id');
		if (!uuid && !draggedItem) {
            //TODO needed?
             this.snapDataListener.next(properties);
             return;
        }
		
        let rect = this.getDraggedElementRect(point);
		/*if (resizing && this.equalSizeThreshold > 0) { 
			let verticalDist: Guide[], horizontalDist: Guide[];
			if (resizing.indexOf('s') >= 0 || resizing.indexOf('n') >= 0) {
				verticalDist = this.addEqualHeightGuides(point, rect, properties);
			}

			if (resizing.indexOf('e') >= 0 || resizing.indexOf('w') >= 0) {
				horizontalDist = this.addEqualWidthGuides(point, rect, properties);
			}
			//same size have higher prio than edge
			if (verticalDist || horizontalDist) {
				this.equalSize = true;
				this.properties = properties;
				return this.snapDataListener.next(this.properties);
			}
		}*/
		
		const horizontalSnap = this.handleHorizontalSnap(resizing, point, uuid, rect, properties);
		const verticalSnap = this.handleVerticalSnap(resizing, point, uuid, rect, properties);
		if (!resizing && this.equalDistanceThreshold > 0) { 
			//equal distance guides
			const verticalDist = this.addEqualDistanceVerticalGuides(rect, properties);
			if (verticalDist && verticalSnap) {
				properties.guides.splice(properties.guides.indexOf(verticalSnap), 1);
			}
			const horizontalDist = this.addEqualDistanceHorizontalGuides(rect, properties);
			if (horizontalDist && horizontalSnap) {
				properties.guides.splice(properties.guides.indexOf(horizontalSnap), 1);
			}
		}

        this.properties = properties;
		return this.snapDataListener.next(properties.guides.length == 0 ? null : properties);
	}

	private addEqualWidthGuides(point: { x: number, y: number }, rect: DOMRect, properties: SnapData): Guide[] {
		const filteredRectangles = this.rectangles.filter(r =>
			Math.abs(r.width - rect.width) <= this.equalSizeThreshold
		).sort((rectA, rectB) => rectA.width - rectB.width);
		if (filteredRectangles.length > 0) {
			let size = filteredRectangles[0].width;
			if (this.pointCloserToTopOrLeftSide(point, rect, 'x')) {
				properties.left = rect.left + size - rect.width;
			}
			else {
				properties.left = rect.left;
			}
			properties.width = size;
			let guides = [];
			guides.push(new Guide(properties.left, rect.bottom + 5, 1, 15 , 'dist'));
			guides.push(new Guide(properties.left + size, rect.bottom + 5, 1, 15, 'dist'));
			guides.push(new Guide(properties.left, rect.bottom + 10, size, 1, 'dist'));

			filteredRectangles.filter(r => r.width == size).forEach(r => {
				guides.push(new Guide(r.left, r.bottom + 3, 1, 15 , 'dist'));
				guides.push(new Guide(r.right, r.bottom + 3, 1, 15, 'dist'));
				guides.push(new Guide(r.left, r.bottom + 10, size, 1, 'dist'));
			});
			properties.guides.push(...guides);
			return guides;
		}
		return null;
	}

	private addEqualHeightGuides(point: { x: number, y: number }, rect: DOMRect, properties: SnapData): Guide[] {
		const filteredRectangles = this.rectangles.filter(r =>
			Math.abs(r.height - rect.height) <= this.equalSizeThreshold
		).sort((rectA, rectB) => rectA.height - rectB.height);
		if (filteredRectangles.length > 0) {
			let size = filteredRectangles[0].height;
			if (this.pointCloserToTopOrLeftSide(point, rect, 'y')) {
				properties.top = rect.top + size - rect.height;
			}
			else {
				properties.top = rect.top;
			}
			properties.height = size;
			let guides = [];
			guides.push(new Guide(rect.right + 5, rect.top, 15, 1 , 'dist'));
			guides.push(new Guide(rect.right + 5, rect.top + size, 15, 1, 'dist'));
			guides.push(new Guide(rect.right + 10, rect.top, 1, size, 'dist'));

			filteredRectangles.filter(r => r.height == size).forEach(r => {
				guides.push(new Guide(r.right + 3, r.top, 15, 1 , 'dist'));
				guides.push(new Guide(r.right + 3, r.bottom, 15, 1, 'dist'));
				guides.push(new Guide(r.right + 10, r.top, 1, size, 'dist'));
			});
			properties.guides.push(...guides);
			return guides;
		}
		return null;
	}

	private handleHorizontalSnap(resizing: string, point: { x: number, y: number }, uuid: string, rect: DOMRect, properties: any) : Guide {
		if (this.snapThreshold <= 0) return null;
		if (!resizing || resizing.indexOf('e') >= 0 || resizing.indexOf('w') >= 0) {
			let closerToTheLeft = this.pointCloserToTopOrLeftSide(point, rect, 'x');
			let snapX, guideX;
			if (!resizing || closerToTheLeft) {
				snapX = this.isSnapInterval(uuid, resizing ? point.x : rect.left, this.leftPos);
				if (snapX?.uuid) {
					properties.left = this.leftPos.get(snapX.uuid);
				}
				else {
					snapX = this.isSnapInterval(uuid, resizing ? point.x : rect.left, this.rightPos);
					if (snapX?.uuid) {
						properties.left = this.rightPos.get(snapX.uuid);
						snapX.prop = 'right';
					}
				}

				if (snapX) {
					properties.cssPosition['left'] = snapX;
					if (!properties.cssPosition['left']) properties.cssPosition['left'] = properties.left;
					guideX = properties.left;
					if (resizing) {
						properties['width'] = rect.width + rect.left - properties.left;
					}
				}
			}
			//if not found, check the right edge as well
			if(!snapX && (!resizing || !closerToTheLeft)) {
				snapX = this.isSnapInterval(uuid, resizing ? point.x : rect.right, this.rightPos);
				guideX = this.rightPos.get(snapX?.uuid);
				properties.left = snapX ? this.rightPos.get(snapX.uuid) : properties.left;
				if (!snapX) {
					snapX = this.isSnapInterval(uuid, resizing ? point.x : rect.right, this.leftPos);
					if (snapX?.uuid) {
						properties.left = this.leftPos.get(snapX.uuid);
						guideX = this.leftPos.get(snapX.uuid);
						snapX.prop = 'left';
					}
				}
				if (snapX) {
					properties.cssPosition['right'] = snapX;
					if (resizing) {
						properties.left = rect.left;
						properties['width'] = guideX - properties.left;
					}
					else {
						properties.left -= rect.width;
					}
					if (!properties.cssPosition['right']) properties.cssPosition['right'] = properties.left;
				}
			}

			if (!snapX && !resizing) {
				snapX = this.isSnapInterval(uuid, (rect.left + rect.right) / 2, this.middleH);
				if (snapX) {
					properties.cssPosition['middleH'] = snapX;
					properties.left = this.middleH.get(snapX.uuid) - rect.width / 2;
					guideX = this.middleH.get(snapX.uuid);
				}
			}

			if (snapX) {
				let guide : Guide;
				if (this.topPos.get(snapX.uuid) < rect.top) {
					guide = new Guide(guideX, this.topPos.get(snapX.uuid), 1, rect.bottom - this.topPos.get(snapX.uuid), 'snap');
				}
				else {
					guide = new Guide(guideX, rect.top, 1, this.topPos.get(snapX.uuid) - rect.top, 'snap');
				}
				properties.guides.push(guide);
				return guide;
			}
		}
		return null;
	}

	private handleVerticalSnap(resizing: string, point: { x: number, y: number }, uuid: string, rect: DOMRect, properties: any) : Guide {
		if (this.snapThreshold <= 0) return null;
		if (!resizing || resizing.indexOf('s') >= 0 || resizing.indexOf('n') >= 0) {
			let closerToTheTop = this.pointCloserToTopOrLeftSide(point, rect, 'y');
			let snapY, guideY;
			if (!resizing || closerToTheTop) {
				snapY = this.isSnapInterval(uuid, resizing ? point.y : rect.top, this.topPos);
				if (snapY?.uuid) {
					properties.top = this.topPos.get(snapY.uuid);
				}
				else {
					snapY = this.isSnapInterval(uuid, resizing ? point.y : rect.top, this.bottomPos);
					if (snapY?.uuid) {
						properties.top = this.bottomPos.get(snapY.uuid);
						snapY.prop = 'bottom';
					}
				}

				if (snapY) {
					properties.cssPosition['top'] = snapY;
					guideY = properties.top;
					if (resizing) {
						properties['height'] = rect.height + rect.top - properties.top;
					}
				}
			}
			if (!snapY && (!resizing || !closerToTheTop)) {
				snapY = this.isSnapInterval(uuid, resizing ? point.y : rect.bottom, this.bottomPos);
				if (snapY?.uuid) {
					guideY = this.bottomPos.get(snapY.uuid);
					properties.top = snapY ? this.bottomPos.get(snapY.uuid) : properties.top;
				}
				if (!snapY) {
					snapY = this.isSnapInterval(uuid, resizing ? point.y : rect.bottom, this.topPos);
					if (snapY?.uuid) {
						properties.top = this.topPos.get(snapY.uuid);
						guideY = this.topPos.get(snapY.uuid);
						snapY.prop = 'top';
					}
				}
				if (snapY) {
					properties.cssPosition['bottom'] = snapY;
					if (resizing) {
						properties.top = rect.top;
						properties['height'] = guideY - properties.top;
					}
					else {
						properties.top -= rect.height;
					}
				}
			}
			if (!snapY && !resizing) {
				snapY = this.isSnapInterval(uuid, (rect.top + rect.bottom) / 2, this.middleV);
				if (snapY?.uuid) {
					properties.cssPosition['middleV'] = snapY;
					properties.top = this.middleV.get(snapY.uuid) - rect.height / 2;
					guideY = this.middleV.get(snapY.uuid);
				}
			}

			if (snapY) {
				let guide;
				if (this.leftPos.get(snapY.uuid) < rect.left) {
					guide = new Guide(this.leftPos.get(snapY.uuid), guideY, rect.right - this.leftPos.get(snapY.uuid), 1, 'snap');
				}
				else {
					guide = new Guide(rect.left, guideY, this.leftPos.get(snapY.uuid) - rect.left, 1, 'snap');
				}
				properties.guides.push(guide);
				return guide;
			}
		}
		return null;
	}
    
	private pointCloserToTopOrLeftSide(point: {x: number, y: number}, rectangle: DOMRect, axis: 'x' | 'y'): boolean {
		const calculateDistance = (a: number, b: number) => Math.abs(a - b);

		const distanceToStart = axis === 'y' ? calculateDistance(point.y, rectangle.y) : calculateDistance(point.x, rectangle.x);
		const distanceToEnd = axis === 'y' ? calculateDistance(point.y, rectangle.y + rectangle.height) : calculateDistance(point.x, rectangle.x + rectangle.width);

		return distanceToStart < distanceToEnd;
	}
    
    private addEqualDistanceVerticalGuides(rect: DOMRect, properties: any ): Guide[] {
		const overlappingX = this.getOverlappingRectangles(rect, 'x');
        for (let pair of overlappingX){
			const e1 = pair[0];
            const e2 = pair[1];   
            if (e2.top > e1.bottom && rect.top > e2.bottom) {
				const dist = e2.top - e1.bottom;
				if (Math.abs(dist - rect.top + e2.bottom) < this.equalDistanceThreshold) {
					properties.top = e2.bottom + dist;
	    			const r = new DOMRect(properties.left ? properties.left : rect.x, properties.top, rect.width, rect.height);
    				return this.addVerticalGuides(e1, e2, r, dist, properties);
				}
			}
			if (e2.top > e1.bottom && e1.top > rect.bottom) {
				const dist = e2.top - e1.bottom;
				if (Math.abs(dist - e1.top + rect.bottom) < this.equalDistanceThreshold) {
					properties.top = e1.top - dist - rect.height;
	    			const r = new DOMRect(properties.left ? properties.left : rect.x, properties.top, rect.width, rect.height);
    				return this.addVerticalGuides(r, e1, e2, dist, properties);
				}
			}
			if (e2.top > rect.bottom && rect.top > e1.bottom) {
				const dist = (e2.top - e1.bottom) / 2;
				if (Math.abs(e1.bottom + dist - rect.top - rect.height / 2) < this.equalDistanceThreshold) {
					properties.top = e1.bottom + dist - rect.height/2;
	    			const r = new DOMRect(properties.left ? properties.left : rect.x, properties.top, rect.width, rect.height);
    				return this.addVerticalGuides(e1, r, e2, dist - rect.height/2, properties);
				}
			}
		}
		return null;
	}
	
	private addEqualDistanceHorizontalGuides(rect: DOMRect, properties: any ): Guide[] {
		const overlappingY = this.getOverlappingRectangles(rect, 'y');
        for (let pair of overlappingY){
			const e1 = pair[0];
            const e2 = pair[1];
            if (e2.left > e1.right && rect.left > e2.right) {
				const dist = e2.left - e1.right;
				if (Math.abs(dist - rect.left + e2.right) < this.equalDistanceThreshold) {                  
                	properties.left = e2.right + dist;
                 	const r = new DOMRect(properties.left, properties.top ? properties.top : rect.y, rect.width, rect.height);
    				return this.addHorizontalGuides(e1, e2, r, dist, properties);
                }
			}
			if (e1.left > rect.right && e2.left > e1.right) {
				const dist = e2.left - e1.right;
				if (Math.abs(dist - rect.right + e1.left) < this.equalDistanceThreshold) {
               		properties.left = e1.left - dist - rect.width;
                   	const r = new DOMRect(properties.left, properties.top ? properties.top : rect.y, rect.width, rect.height);
                   	return this.addHorizontalGuides(r, e1, e2, dist, properties);
               	}
			}
			if (e2.left > rect.right && rect.left > e1.right)  {
				const dist = (e2.left - e1.right) / 2;   
               	if (Math.abs(e1.right + dist - rect.left - rect.width / 2) < this.equalDistanceThreshold) {
					properties.left = e1.right + dist - rect.width/2;
	    			const r = new DOMRect(properties.left, properties.top ? properties.top : rect.y, rect.width, rect.height);
    				return this.addHorizontalGuides(e1, r, e2, dist - rect.width/2, properties);
				}
			}
		}
		return null;
	}
    
    private getOverlappingRectangles(rect: DOMRect, axis: 'x' | 'y'): DOMRect[][] {
		let overlaps = this.rectangles.filter(r => this.isOverlap(rect, r, axis));
		const pairs: DOMRect[][] = [];
    	for (let i = 0; i < overlaps.length - 1; i++) {
        	for (let j = i + 1; j < overlaps.length; j++) {
            	if (overlaps[i] !== rect && overlaps[j] !== rect) {
                	pairs.push([overlaps[i], overlaps[j]]);
            	}
        	}
    	}
    	return pairs;
	}
    
    private isOverlap(rect: DOMRect, eRect: DOMRect, axis: 'x' | 'y'): boolean {
		if (axis === 'x') {
        	return (rect.left >= eRect.left && rect.left <= eRect.right) ||
               	(rect.right >= eRect.left && rect.right <= eRect.right);
    	} else if (axis === 'y') {
        	return (rect.top >= eRect.top && rect.top <= eRect.bottom) ||
               (rect.bottom >= eRect.top && rect.bottom <= eRect.bottom);
    	}
    	return false;
	}
	
	private getDOMRect(uuid: string) : DOMRect {
		return new DOMRect(this.leftPos.get(uuid), this.topPos.get(uuid), 
                	this.rightPos.get(uuid) - this.leftPos.get(uuid),
                	this.bottomPos.get(uuid) - this.topPos.get(uuid));
	}
	
	private addVerticalGuides(e1: DOMRect, e2: DOMRect, r: DOMRect, dist: number, properties: any): Guide[] {
	    const right = Math.max(r.right, e1.right, e2.right);
	    let guides = [];
	    guides.push(new Guide(e1.right, e1.bottom, this.getGuideLength(right, e1.right), 1, 'dist'));
	    guides.push(new Guide(right + 10, e1.bottom, 1, dist, 'dist'));
	    const len = this.getGuideLength(right, e2.right);
	    guides.push(new Guide(e2.right, e2.top, len, 1, 'dist'));
	    guides.push(new Guide(e2.right, e2.bottom, len, 1, 'dist'));
	    guides.push(new Guide(right + 10, e2.bottom, 1, dist, 'dist'));
	    guides.push(new Guide(r.right, r.top, this.getGuideLength(right, r.right), 1, 'dist'));
	    properties.guides.push(...guides);
	    return guides;
	}
	
	private addHorizontalGuides(e1: DOMRect, e2: DOMRect, r: DOMRect, dist: number, properties: any): Guide[] {
	    let bottom = Math.max(r.bottom, e1.bottom, e2.bottom);
		let guides = [];
	    guides.push(new Guide(e1.right, e1.bottom, 1, this.getGuideLength(bottom, e1.bottom), 'dist'));
	    guides.push(new Guide(e1.right, bottom + 10, dist, 1, 'dist'));
	    const len = this.getGuideLength(bottom, e2.bottom);
	    guides.push(new Guide(e2.left, e2.bottom, 1, len, 'dist'));
	    guides.push(new Guide(e2.right, e2.bottom, 1, len, 'dist'));
	    guides.push(new Guide(e2.right, bottom + 10, dist, 1, 'dist'));
	    guides.push(new Guide(r.left, r.bottom, 1, this.getGuideLength(bottom, r.bottom), 'dist'));
	    properties.guides.push(...guides);
	    return guides;
	}
	
	private getGuideLength(max: number, x: number): number {
		return max - x + 15;
	}
}

export class Guide {
	x: number;
	y: number;
	width: number;
	height: number;
	styleClass: string;
	constructor(x: number, y: number,
		width: number, height: number, styleClass: string) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.styleClass = styleClass;
	}
}

export class SnapData {
    top: number;
    left: number;
    width?: number;
    height?: number;
    guides?: Guide[];
    cssPosition: { property: string };
    constructor (top: number, left: number, cssPosition?, guides?: Guide[], width?: number, height?:number) {
        this.top = top;
        this.left = left;
        this.width = width;
        this.height = height;
        this.guides = guides;
        this.cssPosition = cssPosition;
    }
}