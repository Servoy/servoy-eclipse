import { Component, ChangeDetectorRef, Renderer2, ContentChild, TemplateRef, Input, SimpleChanges, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBootstrapBaseComponent } from '../bts_basecomp';
import { FormService } from '../../ngclient/form.service';

@Component({
    selector: 'bootstrapcomponents-tablesspanel',
    templateUrl: './tablesspanel.html',
    styleUrls: ['./tablesspanel.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyBootstrapTablesspanel extends ServoyBootstrapBaseComponent<HTMLDivElement> {

    @Input() containedForm: any;
    @Input() relationName: any;
    @Input() waitForData: any;
    @Input() height: number;

    @ContentChild(TemplateRef, { static: true })
    templateRef: TemplateRef<any>;

    private realContainedForm: any;
    private formWillShowCalled: any;

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
                                        this.realContainedForm = this.containedForm;
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
                    this.realContainedForm = formName;
                    this.cdRef.detectChanges();
                });
            } else {
                this.servoyApi.formWillShow(formName, relationName).then(() => this.cdRef.detectChanges());
                this.realContainedForm = formName;
            }
        }
    }

    getForm() {
        return this.realContainedForm;
    }

    getContainerStyle() {
        let minHeight = 0;
        if (this.height) {
            minHeight = this.height
        }
        else if (this.containedForm) {
            // for absolute form default height is design height, for responsive form default height is 0
            const formCache = this.formService.getFormCacheByName(this.containedForm);
            if (formCache && formCache.absolute) {
                minHeight = formCache.size.height;
            }
        }
        return { position: 'relative', minHeight: minHeight + 'px' };
    }
}
