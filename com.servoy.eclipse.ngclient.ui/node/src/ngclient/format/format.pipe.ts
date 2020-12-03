import { Injector, Pipe, PipeTransform} from '@angular/core';
import { Format, FormattingService } from './formatting.service';

@Pipe( { name: 'formatFilter'} )
export class FormatFilterPipe implements PipeTransform {

    public constructor(private injector: Injector, private formatService: FormattingService) {
    }

    transform( input: any, format: Format): any {
        if (!format) return input;
        return this.formatService.format(input, format, !format.display);
    }
}
