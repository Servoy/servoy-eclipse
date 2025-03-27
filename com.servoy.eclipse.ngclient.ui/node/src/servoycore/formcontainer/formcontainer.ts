import { Component, ChangeDetectorRef, Renderer2, ContentChild, TemplateRef, Input, SimpleChanges, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent } from '@servoy/public';
import { FormService } from '../../ngclient/form.service';
import {
	trigger,
	style,
	animate,
	state,
	transition,
	AnimationEvent
	// ...
} from '@angular/animations';

@Component({
	selector: 'servoycore-formcontainer',
	templateUrl: './formcontainer.html',
	styleUrls: ['./formcontainer.scss'],
	animations: [
		trigger('slideAnimation', [
			state('hide', style({
				display: 'none'
			})),
			transition('void => slide-left', [
				style({ left: '100%', position: 'absolute', width: '100%', height: '100%' }),
				animate('750ms ease-in', style({ left: '0%', })),
			]),
			transition('slide-left => hide', [
				style({ right: '0%', position: 'absolute', width: '100%', height: '100%' }),
				animate('750ms ease-in', style({ right: '100%' }))
			]),
			transition('void => slide-right', [
				style({ right: '100%', position: 'absolute', width: '100%', height: '100%' }),
				animate('750ms ease-in', style({ right: '0%', })),
			]),
			transition('slide-right => hide', [
				style({ left: '0%', position: 'absolute', width: '100%', height: '100%' }),
				animate('750ms ease-in', style({ left: '100%' }))
			]),
			transition('void => slide-down', [
				style({ bottom: '100%', position: 'absolute', width: '100%', height: '100%' }),
				animate('750ms ease-in', style({ bottom: '0%', })),
			]),
			transition('slide-down => hide', [
				style({ top: '0%', position: 'absolute', width: '100%', height: '100%' }),
				animate('750ms ease-in', style({ top: '100%' }))
			]),
			transition('void => slide-up', [
				style({ top: '100%', position: 'absolute', width: '100%', height: '100%' }),
				animate('750ms ease-in', style({ top: '0%', })),
			]),
			transition('slide-up => hide', [
				style({ bottom: '0%', position: 'absolute', width: '100%', height: '100%' }),
				animate('750ms ease-in', style({ bottom: '100%' }))
			]),
			transition('void => rotate-y', [
				style({ transform: 'rotateY(90deg)', opacity: 0, position: 'absolute', width: '100%', height: '100%' }),
				animate('750ms ease-in', style({ transform: 'rotateY(0deg)', opacity: 1 }))
			]),
			transition('rotate-y => hide', [
				style({ transform: 'rotateY(0)', opacity: 1, position: 'absolute', width: '100%', height: '100%' }),
				animate('750ms ease-out', style({ transform: 'rotateY(90deg)', opacity: 0 }))
			]),
			transition('void => rotate-x', [
				style({ transform: 'rotateX(-90deg)', opacity: 0, position: 'absolute', width: '100%', height: '100%' }),
				animate('750ms ease-in', style({ transform: 'rotateX(0deg)', opacity: 1 }))
			]),
			transition('rotate-x => hide', [
				style({ transform: 'rotateX(0)', opacity: 1, position: 'absolute', width: '100%', height: '100%' }),
				animate('750ms ease-out', style({ transform: 'rotateX(90deg)', opacity: 0 }))
			])
		]),
	],
	changeDetection: ChangeDetectionStrategy.OnPush,
	standalone: false
})
export class ServoyCoreFormContainer extends ServoyBaseComponent<HTMLDivElement> {

	@Input() containedForm: any;
	@Input() relationName: any;
	// deprecated, not used anymore
	@Input() waitForData: any;
	@Input() height: string;
	@Input() tabSeq: number;
	@Input() toolTipText: string;
	@Input() animation: string;
	@Input() styleClass: string;

	@ContentChild(TemplateRef, { static: true })
	templateRef: TemplateRef<any>;

	form1_state: string;
	form2_state: string;

	form1_visible: boolean;
	form2_visible: boolean;

	private realContainedForm: any;

	private form1: string;
	private form2: string;

	constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, private formService: FormService) {
		super(renderer, cdRef);
	}

	svyOnChanges(changes: SimpleChanges) {
		if (changes) {
			for (const property of Object.keys(changes)) {
				const change = changes[property];
				switch (property) {
					case 'containedForm': {
						if (change.currentValue !== change.previousValue) {
							if (change.previousValue && this.realContainedForm) {
								this.form1_state = this.animation;
								this.form2_state = this.animation;
							}
							if (change.currentValue) {
								this.switchForm(change.currentValue);
							}
						}
						break;
					}
					case 'visible': {
						if (change.currentValue !== change.previousValue && !change.currentValue) {
							this.realContainedForm = undefined;
						}
						break;
					}
					case 'animation': {
						if (this.realContainedForm) {
							this.form1_state = this.animation;
							this.form2_state = this.animation;
						}
						break;
					}
					case 'styleClass':
						if (change.previousValue) {
							const array = change.previousValue.trim().split(' ');
							array.filter((element: string) => element !== '').forEach((element: string) => this.renderer.removeClass(this.getNativeElement(), element));
						}
						if (change.currentValue) {
							const array = change.currentValue.trim().split(' ');
							array.filter((element: string) => element !== '').forEach((element: string) => this.renderer.addClass(this.getNativeElement(), element));
						}
						break;
				}
			}
			super.svyOnChanges(changes);
		}
	}

	switchForm(name: string) {
		if (this.animation && this.animation !== 'none' && this.realContainedForm) {
			if (this.form1 === this.realContainedForm) {
				this.form2 = name;
				this.form1_state = 'hide';
				this.form2_state = this.animation;
				this.form2_visible = true;
			} else {
				this.form1 = name;
				this.form2_state = 'hide';
				this.form1_state = this.animation;
				this.form1_visible = true;
			}
		} else {
			if (this.form1 === this.realContainedForm) {
				this.form2 = name;
				this.form2_visible = true;
				this.form1_visible = false;
			} else {
				this.form1 = name;
				this.form1_visible = true;
				this.form2_visible = false;
			}
		}
		this.realContainedForm = name;
	}

	getForm1() {
		return this.form1;
	}

	getForm2() {
		return this.form2;
	}

	getContainerStyle(name: string) {
		const styl = {};
		let minHeight: string | number;
		if (this.height && this.height !== '0') {
			minHeight = this.height;
		} else if (this.containedForm || name) {
			// for absolute form default height is design height, for responsive form default height is 0
			const formCache = this.formService.getFormCacheByName(name);
			if (formCache && formCache.absolute) {
				minHeight = formCache.size.height;
			}
		}
		if (this.height && this.height.indexOf('%') >= 0) {
			styl['height'] = this.height;
			if (this.getNativeElement()) this.renderer.setStyle(this.getNativeElement(), 'height', this.height);
		} else if (minHeight) {
			styl['minHeight'] = minHeight + 'px';
		}
		return styl;
	}

	animationDone(event: AnimationEvent, whichForm: string) {
		if (event.toState === 'hide') {
			if (whichForm === 'form1') this.form1_visible = this.form1 === this.realContainedForm;
			else if (whichForm === 'form2') this.form2_visible = this.form2 === this.realContainedForm;
		}
	}
}
