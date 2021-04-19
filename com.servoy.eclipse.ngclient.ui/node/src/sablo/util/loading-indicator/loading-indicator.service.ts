import { Subject } from 'rxjs';
import { Injectable, NgZone } from '@angular/core';
import { LoggerFactory, LoggerService } from '../../logger.service';

export interface ICustomLoadingIndicator {
    showLoading(): void;
    hideLoading(): void;
}

@Injectable()
export class LoadingIndicatorService {

    showLoadingIndicator: Subject<boolean> = new Subject<boolean>();
    customLoadingIndicator: ICustomLoadingIndicator;
    log: LoggerService;
    private showCounter = 0;
    private timeoutShow = null;
    private timeoutHide = null;

    constructor(private logFactory: LoggerFactory ) {
       this.customLoadingIndicator = null;
       this.log = logFactory.getLogger('LoadingIndicatorService');
    }

    public showLoading() {
        this.showCounter++;
        if (this.showCounter == 1) {
            if (this.timeoutHide) {
                clearTimeout(this.timeoutHide);
                this.timeoutHide = null;
            } else if (!this.timeoutShow) {
                this.timeoutShow = setTimeout(() => {
                    this.timeoutShow = null;
                    if (this.customLoadingIndicator) {
                        this.customLoadingIndicator.showLoading();
                    } else {
                        this.showLoadingIndicator.next(true);
                    }
                }, 400);
            }
        }
    }

    public hideLoading() {
        this.showCounter--;
        if (this.showCounter == 0) {
            this.timeoutHide = setTimeout(() => {
                this.timeoutHide = null;
                if (this.timeoutShow) {
                    clearTimeout(this.timeoutShow);
                    this.timeoutShow = null;
                } else if (this.customLoadingIndicator) {
                    this.customLoadingIndicator.hideLoading();
                } else {
                    this.showLoadingIndicator.next(false);
                }
            }, 50);
        }
    }

    isShowing() {
        return this.showCounter > 0;
    }

    public setCustomLoadingIndicator(customLoadingIndicator: ICustomLoadingIndicator) {
        if (customLoadingIndicator.hideLoading == undefined || customLoadingIndicator.showLoading == undefined) {
            this.log.warn(this.log.buildMessage(() => ('a custom loading indicator is defined but does not have the 2 functions: showLoading or hideLoading')));
            this.customLoadingIndicator = null;
        } else {
            this.customLoadingIndicator = customLoadingIndicator;
        }
    }
}
