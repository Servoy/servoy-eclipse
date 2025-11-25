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
            // Replace placeholders but keep scripts intact
            const processedHtml = res
                .replace('{{redirectUrl}}', sessionProblem.redirectUrl)
                .replace('ng-href', 'href');

            // Extract <script src>
            const externalScriptRegex = /<script\b[^>]*src=["']([^"']+)["'][^>]*><\/script>/gi;

            let safeHtml = processedHtml;

            // Remove external scripts from HTML string
            safeHtml = safeHtml.replace(externalScriptRegex, '');

            // Bind safe markup
            this.htmlString = this.sanitizer.bypassSecurityTrustHtml(safeHtml);
            this.cdRef.detectChanges();

            // Re-inject external scripts with nonce
            let match;
            while ((match = externalScriptRegex.exec(processedHtml)) !== null) {
                const src = match[1];
                const script = document.createElement('script');
                script.setAttribute('nonce', sessionProblem.nonce);
                script.src = src;
                if (/defer/i.test(match[0])) script.defer = true;
                document.body.appendChild(script);
            }
        });

        if (sessionProblem.redirectTimeout >= 0) {
            window.setTimeout(function() {
                window.location.href = sessionProblem.redirectUrl;
            }, sessionProblem.redirectTimeout * 1000);
        }

    }
}
