import { ChangeDetectionStrategy, Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { ServoyService } from '../../ngclient/servoy.service';

@Component({
    selector: 'session-view',
    templateUrl: './session-view.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class SessionView implements OnInit {

    constructor(public http: HttpClient, public servoyService: ServoyService, private sanitizer: DomSanitizer, protected cdRef: ChangeDetectorRef) {
    }

    htmlString: SafeHtml;

    ngOnInit() {
        const sessionProblem = this.servoyService.getSolutionSettings().sessionProblem;
        const headers = new HttpHeaders({
            'Content-Type': 'text/plain',
        });
        const request = this.http.get(sessionProblem.viewUrl, {
            headers,
            responseType: 'text'
        }).subscribe(res => {
            this.htmlString = this.sanitizer.bypassSecurityTrustHtml(res.replace('{{redirectUrl}}', sessionProblem.redirectUrl).replace('ng-href', 'href'));
            this.cdRef.detectChanges();
        });

        if (sessionProblem.redirectTimeout >= 0) {
            window.setTimeout(function() {
                window.location.href = sessionProblem.redirectUrl;
            }, sessionProblem.redirectTimeout * 1000);
        }

    }
}
