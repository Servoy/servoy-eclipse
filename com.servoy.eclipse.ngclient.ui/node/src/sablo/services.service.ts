import { Injectable } from '@angular/core';

import { ConverterService } from './converter.service'
import { LoggerService, LoggerFactory } from './logger.service'

@Injectable()
export class ServicesService {
    private serviceProvider: ServiceProvider = new VoidServiceProvider();
    private log: LoggerService;

    constructor( private converterService: ConverterService, private logFactory : LoggerFactory ) {
        this.log = logFactory.getLogger("ServicesService");
    }

    private serviceScopesConversionInfo = {};

    public setServiceProvider( serviceProvider: ServiceProvider ) {
        if ( serviceProvider == null ) this.serviceProvider = new VoidServiceProvider();
        else this.serviceProvider = serviceProvider;
    }
    public getServiceProvider(): ServiceProvider {
        return this.serviceProvider;
    }

    public callServiceApi( service ) {
        var serviceInstance = this.getServiceProvider().getService( service.name );
        if ( serviceInstance
            && serviceInstance[service.call] ) {
            // responseValue keeps last services call return value
            return serviceInstance[service.call].apply( serviceInstance, service.args );
        }
    }

    public updateServiceScopes( services, conversionInfo ) {
        for ( var servicename in services ) {
            // current model
            var service = this.serviceProvider.getService( servicename );
            if ( service ) {
                var serviceData = services[servicename];

                try {
                    for ( var key in serviceData ) {
                        if ( conversionInfo && conversionInfo[servicename] && conversionInfo[servicename][key] ) {
                            // convert property, remember type for when a client-server conversion will be needed
                            if ( !this.serviceScopesConversionInfo[servicename] ) this.serviceScopesConversionInfo[servicename] = {};
                            serviceData[key] = this.converterService.convertFromServerToClient( serviceData[key], conversionInfo[servicename][key], service[key])
                            this.serviceScopesConversionInfo[servicename][key] = conversionInfo[servicename][key];
                        } else if ( this.serviceScopesConversionInfo[servicename] && this.serviceScopesConversionInfo[servicename][key] ) {
                            delete this.serviceScopesConversionInfo[servicename][key];
                        }

                        service[key] = serviceData[key];
                    }
                }
                catch ( ex ) {
                    this.log.error(ex);
                }
            }
        }
    }
}


export interface ServiceProvider {
    getService( name: string );
}

class VoidServiceProvider implements ServiceProvider {
    getService( name ) {
        return null;
    }
}