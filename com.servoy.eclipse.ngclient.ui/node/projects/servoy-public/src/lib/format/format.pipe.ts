import { inject, Pipe, PipeTransform} from '@angular/core';
import { Format, FormattingService } from './formatting.service';
import { LoggerFactory, LoggerService } from '../logger.service';

@Pipe( {
    name: 'formatFilter',
    standalone: false
} )
export class FormatFilterPipe implements PipeTransform {

    private readonly log: LoggerService;
    private formatService: FormattingService;
    
    public constructor(formatService?: FormattingService, logFactory?: LoggerFactory) {
        this.formatService = formatService ?? inject(FormattingService);
        this.log = (logFactory ?? inject(LoggerFactory)).getLogger('formatpipe'); 
    }

    transform( input: any, format: Format): any {
        if (!format) return input;
        
        try {
            return this.formatService.format(input, format, !format.display);
        } catch (e) {
            this.log.error(e);
        }
    }
}
