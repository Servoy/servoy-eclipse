import {Component, ChangeDetectorRef, Renderer2, SimpleChanges, ChangeDetectionStrategy, input} from '@angular/core';
import {ServoyDefaultBaseComponent} from '../basecomponent';
@Component({
    selector: 'servoydefault-rectangle',
    templateUrl: './rectangle.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ServoyDefaultRectangle extends ServoyDefaultBaseComponent<HTMLDivElement> {
    readonly lineSize = input<number>(undefined as any);
    readonly roundedRadius = input<number>(undefined as any);
    readonly shapeType = input<number>(undefined as any);
    readonly size = input<{width: number; height: number}>(undefined as any);

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    svyOnChanges( changes: SimpleChanges ) {

        super.svyOnChanges(changes);

        for ( const property of  Object.keys(changes) ) {
            const change = changes[property];
            switch ( property ) {
            case 'lineSize':
                this.renderer.setStyle(this.getNativeElement(), 'borderWidth', change.currentValue + 'px');
                if (!changes['borderType'] || !changes['borderType'].currentValue) {
                    this.renderer.setStyle(this.getNativeElement(), 'borderStyle', 'solid');
                }
                break;
            case 'foreground':
                if (change.currentValue) this.renderer.setStyle(this.getNativeElement(), 'borderColor', change.currentValue);
                break;
            case 'roundedRadius':
                this.renderer.setStyle(this.getNativeElement(), 'borderRadius', change.currentValue/2 + 'px');
                break;
            case 'shapeType':
                if (change.currentValue === 3 && this.size()) {
                    this.renderer.setStyle(this.getNativeElement(), 'borderRadius', this.size().width/2 + 'px');
                }
                break;
            }
        }
    }

}
