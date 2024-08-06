import { Pipe, PipeTransform} from '@angular/core';
import { Format, FormattingService } from './formatting.service';
import { LoggerFactory, LoggerService } from '../logger.service';

@Pipe( { name: 'formatFilter'} )
export class FormatFilterPipe implements PipeTransform {

    private readonly log: LoggerService;
    
    public constructor(private formatService: FormattingService, logFactory: LoggerFactory) {
        this.log = logFactory.getLogger('formatpipe'); 
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
