import { Point } from './../mouseselection/mouseselection.component';
import { Component, Input, OnInit, Renderer2, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { EditorSessionService, ISupportAutoscroll } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';

@Component({
    selector: 'designer-autoscroll',
    templateUrl: './autoscroll.component.html',
    styleUrls: ['./autoscroll.component.css'],
    standalone: false
})

export class AutoscrollComponent implements OnInit, AfterViewInit {

    @ViewChild('autoscroll', {static: false}) autoscrollElement: ElementRef<HTMLElement>;
    @Input() placement: string;

    private scrollTarget: ISupportAutoscroll | null | undefined;
    private handler: ReturnType<typeof setInterval>;
    private direction: number;
    private mousePoint: Point;
    private isAutoscrollActive = false;
    private stopAutoscrollOffset = 3; 
    private step = 0;
    private speed = 0;

    constructor(protected readonly renderer: Renderer2, 
        protected readonly editorSession: EditorSessionService, 
        private editorContent: EditorContentService) {
   
    }

    ngOnInit() {
        this.editorSession.autoscrollBehavior.subscribe((scrollTarget: ISupportAutoscroll | null | undefined) => {
            this.scrollTarget = scrollTarget;
            this.editorSession.getState().pointerEvents = 'none';
            if (scrollTarget) {
                this.editorSession.getState().pointerEvents = 'all';
                this.setPosition();
            }
        });
        switch (this.placement) {
            case 'top':
            case 'left':
                this.direction = -1;
                break;
            case 'bottom':
            case 'right':
                this.direction = 1;
                break;
        }
    }

    ngAfterViewInit() {
        this.autoscrollElement.nativeElement.addEventListener('mouseenter', (event) => {this.onMouseEnter(event)})
        this.autoscrollElement.nativeElement.addEventListener('mouseleave', (event) => {this.onMouseLeave(event)})
        this.autoscrollElement.nativeElement.addEventListener('mouseup', (event) => {this.onMouseUp(event)})
        this.autoscrollElement.nativeElement.addEventListener('mousedown', (event) => {this.onMouseDown(event)})
        this.autoscrollElement.nativeElement.addEventListener('mousemove', (event) => {this.onMouseMove(event)})
    }

    public onMouseEnter(event: MouseEvent) {
        if (this.scrollTarget) {
            this.speed = 2;
            this.mousePoint = { x: event.pageX, y: event.pageY };
        }
    }

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    public onMouseLeave(_event: MouseEvent) {
        if (this.scrollTarget && this.isAutoscrollActive) {
            this.stopAutoscroll();
        }
    }

    public onMouseUp(event: MouseEvent) {
        if (this.scrollTarget) {
            this.stopAutoscroll();
            if (this.scrollTarget.onMouseUp) {
                this.scrollTarget.onMouseUp(event);
            }
        }
    }

    public onMouseDown(event: MouseEvent) {
        this.speed = 2;
        this.mousePoint = { x: event.pageX, y: event.pageY };
    }

    onMouseMove(event: MouseEvent) {
        if (this.scrollTarget) {
            switch (this.placement) {
                case 'top':
                case 'bottom':
                    this.step = (this.mousePoint.y - event.pageY);
                    break;
                case 'left':
                case 'right':
                    this.step = (this.mousePoint.x - event.pageX);
                    break;
            } 
            if (this.step * this.direction > 0) {//moving backward
                if (this.step * this.direction > this.stopAutoscrollOffset) {// stop autoscroll
                    this.stopAutoscroll();
                    this.mousePoint.x = event.pageX;
                    this.mousePoint.y = event.pageY;
                }
            } else if (this.step * this.direction < 0){//moving forward
                if (!this.isAutoscrollActive) { //moving forward; avoid multiple starts
                    this.startAutoScroll();
                }
                this.mousePoint.x = event.pageX;
                this.mousePoint.y = event.pageY;
            }
            this.scrollTarget.onMouseMove(event);  
        }
    }

    setPosition() {
        if (this.placement == 'left') {
            const left =  this.editorContent.getDesignerElementById('palette').offsetWidth + 
                this.editorContent.getDesignerElementById('palette').offsetLeft;
            this.renderer.setStyle(this.autoscrollElement.nativeElement, 'left', left + 'px');
        }
        if (this.placement != 'top' && this.placement != 'bottom') {
            const top = this.editorContent.getBodyElement().getElementsByClassName('top').item(0);
            const bottom = this.editorContent.getBodyElement().getElementsByClassName('bottom').item(0);
            const height = (bottom as HTMLElement).offsetTop - ((top as HTMLElement).offsetTop + (top as HTMLElement).offsetHeight) - 10;
            this.renderer.setStyle(this.autoscrollElement.nativeElement, 'height', height + 'px');
        }
    }

    autoscroll() {
        if (this.scrollTarget) {
            if (this.speed < 15) this.speed++;
            switch (this.placement) {
                case 'top':
                case 'bottom':
                    this.scrollTarget.updateLocationCallback(0, this.speed * this.direction);
                    break;
                case 'left':
                case 'right':
                    this.scrollTarget.updateLocationCallback(this.speed * this.direction, 0);
                    break;
            }
        }
    }

    startAutoScroll() {
        if (!this.isAutoscrollActive) {
            this.isAutoscrollActive = true;
            this.handler = setInterval(() => {this.autoscroll()}, 50);
        }
    }

    stopAutoscroll() {
        if (this.isAutoscrollActive) {
            this.isAutoscrollActive = false;
            clearInterval(this.handler);
        }
    }
}