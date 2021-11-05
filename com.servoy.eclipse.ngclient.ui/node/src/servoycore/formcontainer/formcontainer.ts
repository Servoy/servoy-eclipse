import { Component, ChangeDetectorRef, Renderer2, ContentChild, TemplateRef, Input, SimpleChanges, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent } from '@servoy/public';
import { FormService } from '../../ngclient/form.service';
import {
    trigger,
    state,
    style,
    animate,
    transition,
    // ...
} from '@angular/animations';

@Component({
    selector: 'servoycore-formcontainer',
    templateUrl: './formcontainer.html',
    styleUrls: ['./formcontainer.scss'],
    animations: [
        trigger('slideAnimation', [
            transition('void => slide-left', [
                style({ left: '100%'}),
                animate('750ms ease-in', style({ left: '0%', })),
            ]),
            transition('slide-left => void', [
                style({ right: '0%' }),
                animate('750ms ease-in', style({ right: '100%'}))
            ]),
             transition('void => slide-right', [
                style({ right: '100%'}),
                animate('750ms ease-in', style({ right: '0%', })),
            ]),
            transition('slide-right => void', [
                style({ left: '0%' }),
                animate('750ms ease-in', style({ left: '100%'}))
            ]),
               transition('void => slide-down', [
                style({ bottom: '100%'}),
                animate('750ms ease-in', style({ bottom: '0%', })),
            ]),
            transition('slide-down => void', [
                style({ top: '0%' }),
                animate('750ms ease-in', style({ top: '100%'}))
            ]),
             transition('void => slide-up', [
                style({ top: '100%'}),
                animate('750ms ease-in', style({ top: '0%', })),
            ]),
            transition('slide-up => void', [
                style({ bottom: '0%' }),
                animate('750ms ease-in', style({ bottom: '100%'}))
            ]),
             transition('void => rotate-y', [
              style({transform: 'rotateY(90deg)', opacity: 0}),
              animate('750ms ease-in', style({transform: 'rotateY(0deg)', opacity: 1}))
            ]),
            transition('rotate-y => void', [
               style({transform: 'rotateY(0)', opacity: 1}),
                animate('750ms ease-out', style({transform: 'rotateY(90deg)', opacity: 0}))
            ]),
             transition('void => rotate-x', [
              style({transform: 'rotateX(-90deg)', opacity: 0}),
              animate('750ms ease-in', style({transform: 'rotateX(0deg)', opacity: 1}))
            ]),
            transition('rotate-x => void', [
               style({transform: 'rotateX(0)', opacity: 1}),
                animate('750ms ease-out', style({transform: 'rotateX(90deg)', opacity: 0}))
            ])
        ]),
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyCoreFormContainer extends ServoyBaseComponent<HTMLDivElement> {

    @Input() containedForm: any;
    @Input() relationName: any;
    @Input() waitForData: any;
    @Input() height: number;
    @Input() tabSeq: number;
    @Input() toolTipText: string;
    @Input() animation: string;

    @ContentChild(TemplateRef, { static: true })
    templateRef: TemplateRef<any>;

    private realContainedForm: any;
    private formWillShowCalled: any;

    private formA: string;
    private formB: string;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, private formService: FormService) {
        super(renderer, cdRef);
    }

    svyOnChanges(changes: SimpleChanges) {
        if (changes) {
            for (const property of Object.keys(changes)) {
                const change = changes[property];
                switch (property) {
                    case 'containedForm': {
                        if (change.currentValue !== change.previousValue)
                            if (change.previousValue) {
                                this.formWillShowCalled = change.currentValue;
                                this.servoyApi.hideForm(change.previousValue, null, null, change.currentValue, this.relationName, null)
                                    .then(() => {
                                        this.switchForm(this.containedForm);
                                        this.cdRef.detectChanges();
                                    });
                            } else if (change.currentValue) {
                                this.setRealContainedForm(change.currentValue, this.relationName);
                            }
                        break;
                    }
                    case 'visible': {
                        if (this.containedForm && change.currentValue !== change.previousValue) {
                            this.formWillShowCalled = this.realContainedForm = undefined;
                            if (change.currentValue) {
                                this.setRealContainedForm(this.containedForm, this.relationName);
                            } else {
                                this.servoyApi.hideForm(this.containedForm);
                            }
                        }
                        break;
                    }
                }
            }
            super.svyOnChanges(changes);
        }
    }

    setRealContainedForm(formName: any, relationName: any) {
        if (this.formWillShowCalled !== formName && formName) {
            this.formWillShowCalled = formName;
            if (this.waitForData) {
                this.servoyApi.formWillShow(formName, relationName).then(() => {
                    this.switchForm(formName);
                    this.cdRef.detectChanges();
                });
            } else {
                this.servoyApi.formWillShow(formName, relationName).then(() => this.cdRef.detectChanges());
                this.switchForm(formName);
            }
        }
    }

    switchForm(name: string) {
        if (this.formA === this.realContainedForm) {
            this.formB = name;
        } else {
            this.formA = name;
        }
        this.realContainedForm = name;
    }

    getForm1() {
        return this.formA;
    }

    getForm2() {
        return this.formB;
    }

    getForm() {
        return this.realContainedForm;
    }

    getContainerStyle() {
        const style = { position: 'relative' };
        let minHeight = 0;
        if (this.height) {
            minHeight = this.height;
        } else if (this.containedForm) {
            // for absolute form default height is design height, for responsive form default height is 0
            const formCache = this.formService.getFormCacheByName(this.containedForm);
            if (formCache && formCache.absolute) {
                minHeight = formCache.size.height;
            }
        }
        if (minHeight > 0) {
            style['minHeight'] = minHeight + 'px';
        }
        return style;
    }
}
