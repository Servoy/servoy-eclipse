import { Component, ChangeDetectorRef, Renderer2, ContentChild, TemplateRef, Input, SimpleChanges, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent } from '@servoy/public';
import { FormService } from '../../ngclient/form.service';
import {
    trigger,
    style,
    animate,
    state,
    transition,
    // ...
} from '@angular/animations';

@Component({
    selector: 'servoycore-formcontainer',
    templateUrl: './formcontainer.html',
    styleUrls: ['./formcontainer.scss'],
    animations: [
        trigger('slideAnimation', [
            state('hide', style({
                 display:'none'
            })),
            transition('void => slide-left', [
                style({ left: '100%',position: 'absolute', width: '100%', height: '100%' }),
                animate('750ms ease-in', style({ left: '0%', })),
            ]),
            transition('slide-left => hide', [
                style({ right: '0%', position: 'absolute', width: '100%', height: '100%' }),
                animate('750ms ease-in', style({ right: '100%'}))
            ]),
             transition('void => slide-right', [
                style({ right: '100%',position: 'absolute', width: '100%', height: '100%' }),
                animate('750ms ease-in', style({ right: '0%', })),
            ]),
            transition('slide-right => hide', [
                style({ left: '0%',position: 'absolute', width: '100%', height: '100%' }),
                animate('750ms ease-in', style({ left: '100%'}))
            ]),
               transition('void => slide-down', [
                style({ bottom: '100%',position: 'absolute', width: '100%', height: '100%'}),
                animate('750ms ease-in', style({ bottom: '0%', })),
            ]),
            transition('slide-down => hide', [
                style({ top: '0%',position: 'absolute', width: '100%', height: '100%' }),
                animate('750ms ease-in', style({ top: '100%'}))
            ]),
             transition('void => slide-up', [
                style({ top: '100%',position: 'absolute', width: '100%', height: '100%'}),
                animate('750ms ease-in', style({ top: '0%', })),
            ]),
            transition('slide-up => hide', [
                style({ bottom: '0%',position: 'absolute', width: '100%', height: '100%' }),
                animate('750ms ease-in', style({ bottom: '100%'}))
            ]),
             transition('void => rotate-y', [
              style({transform: 'rotateY(90deg)', opacity: 0,position: 'absolute', width: '100%', height: '100%'}),
              animate('750ms ease-in', style({transform: 'rotateY(0deg)', opacity: 1}))
            ]),
            transition('rotate-y => hide', [
               style({transform: 'rotateY(0)', opacity: 1,position: 'absolute', width: '100%', height: '100%'}),
                animate('750ms ease-out', style({transform: 'rotateY(90deg)', opacity: 0}))
            ]),
             transition('void => rotate-x', [
              style({transform: 'rotateX(-90deg)', opacity: 0,position: 'absolute', width: '100%', height: '100%'}),
              animate('750ms ease-in', style({transform: 'rotateX(0deg)', opacity: 1}))
            ]),
            transition('rotate-x => hide', [
               style({transform: 'rotateX(0)', opacity: 1,position: 'absolute', width: '100%', height: '100%'}),
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

    form1_state: string;
    form2_state: string;

    form1_visible: boolean;
    form2_visible: boolean;

    private realContainedForm: any;
    private formWillShowCalled: any;

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
                    case 'animation': {
                        this.form1_state = this.animation;
                        this.form2_state = this.animation;
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
        if (this.animation && this.animation !== 'none') {
            if (this.form1 === this.realContainedForm) {
                this.form2 = name;
                this.form1_state = 'hide';
                this.form2_state = this.animation;
                setTimeout(() =>  {
                    this.form1_visible =   this.form1 === this.realContainedForm;;
                    this.cdRef.detectChanges();
                }, 850) ;
                this.form2_visible = true;
            } else {
                this.form1 = name;
                this.form2_state = 'hide';
                this.form1_state = this.animation;
                setTimeout(() =>    {
                    this.form2_visible =  this.form2 === this.realContainedForm;
                    this.cdRef.detectChanges();
                }, 850) ;
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
                this.form2_visible =  false;
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
        const styl = {  };
        let minHeight = 0;
        if (this.height) {
            minHeight = this.height;
        } else if (this.containedForm || name) {
            // for absolute form default height is design height, for responsive form default height is 0
            const formCache = this.formService.getFormCacheByName(name);
            if (formCache && formCache.absolute) {
                minHeight = formCache.size.height;
            }
        }
        if (minHeight > 0) {
            styl['minHeight'] = minHeight + 'px';
        }
        return styl;
    }
}
