
export class PopupForm {
    public component: string;
    public form: string;
    public x: number;
    public y: number;
    public width: number;
    public height: number;
    public showBackdrop: boolean;
    public doNotCloseOnClickOutside: boolean;
    public onClose: Callback;
}

export class Callback {
    public formname: string;
    public script: string;
}
