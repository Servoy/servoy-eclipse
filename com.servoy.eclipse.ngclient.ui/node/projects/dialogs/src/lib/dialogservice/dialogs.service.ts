import { Injectable } from '@angular/core';
import { ServoyPublicService } from '@servoy/public';

@Injectable()
export class DialogService {

    i18nOK = 'OK';
    i18nCancel = 'Cancel';

    constructor(private servoyService: ServoyPublicService) {
    }

    public init(): void {
        try {
            this.servoyService.listenForI18NMessages('servoy.button.cancel', 'servoy.button.ok').messages((val) => {
                this.i18nOK = val.get('servoy.button.ok');
                this.i18nCancel = val.get('servoy.button.cancel');
            });
        } catch (e) {
            console.error(e);
        }
    }

    public async showErrorDialog(dialogTitle: string, dialogMessage: string, buttonsText: string[]): Promise<any> {
        return await this.showDialog(dialogTitle, dialogMessage, 'type-error', null, buttonsText, null);
    }

    public async showInfoDialog(dialogTitle: string, dialogMessage: string, buttonsText: string[]): Promise<any> {
        return await this.showDialog(dialogTitle, dialogMessage, 'type-info', null, buttonsText, null);
    }

    public async showQuestionDialog(dialogTitle: string, dialogMessage: string, buttonsText: string[]): Promise<any> {
        return await this.showDialog(dialogTitle, dialogMessage, 'type-question', null, buttonsText, null);
    }

    public async showInputDialog(dialogTitle: string, dialogMessage: string, initValue: string, inputType: string): Promise<any> {
        if (!dialogTitle) {
            dialogTitle = 'Enter value';
        }
        return await this.showDialog(dialogTitle, dialogMessage, 'type-input', initValue !==null && initValue !== undefined ? [initValue] : null, [this.i18nCancel, this.i18nOK], inputType);
    }

    public async showSelectDialog(dialogTitle: string, dialogMessage: string, options: string[]): Promise<any> {
        return await this.showDialog(dialogTitle, dialogMessage, 'type-select', options, [this.i18nCancel, this.i18nOK], null);
    }

    public async showWarningDialog(dialogTitle: string, dialogMessage: string, buttonsText: string[]): Promise<any> {
        return await this.showDialog(dialogTitle, dialogMessage, 'type-warning', null, buttonsText, null);
    }

    private async showDialog(dialogTitle: string, dialogMessage: string, styleClass: string, values: string[], buttonsText: string[], inputType: string): Promise<any> {
        let dialogButtonsText = buttonsText;
        if (!dialogButtonsText || dialogButtonsText.length === 0) {
            dialogButtonsText = ['OK'];
        }
        return await this.servoyService.showMessageDialog(dialogTitle, dialogMessage, styleClass, values, dialogButtonsText, inputType);
    }
}
