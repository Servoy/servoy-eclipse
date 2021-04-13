import { Injectable } from '@angular/core';

import { ConverterService } from './converter.service';
import { LoggerService, LoggerFactory } from '@servoy/public';

@Injectable()
export class ServicesService {
    private serviceProvider: ServiceProvider = new VoidServiceProvider();
    private log: LoggerService;
    private serviceScopesConversionInfo = {};

    constructor( private converterService: ConverterService, logFactory: LoggerFactory ) {
        this.log = logFactory.getLogger('ServicesService');
    }


    public setServiceProvider( serviceProvider: ServiceProvider ) {
        if ( serviceProvider == null ) this.serviceProvider = new VoidServiceProvider();
        else this.serviceProvider = serviceProvider;
    }
    public getServiceProvider(): ServiceProvider {
        return this.serviceProvider;
    }

    public callServiceApi( service: {name: string; call: string; args: []} ) {
        const serviceInstance = this.getServiceProvider().getService( service.name );
        if ( serviceInstance
            && serviceInstance[service.call] ) {
            // responseValue keeps last services call return value
            return serviceInstance[service.call].apply( serviceInstance, service.args );
        } else {
            this.log.warn('trying to call a service api '  + service.call + ' for service ' + service.name + ' but the sevice (' + serviceInstance + ') or the call was not found');
        }
    }

    public updateServiceScopes( services, conversionInfo ) {
        for ( const servicename of Object.keys(services) ) {
            // current model
            const service = this.serviceProvider.getService( servicename );
            if ( service ) {
                const serviceData = services[servicename];

                try {
                    for ( const key of Object.keys(serviceData) ) {
                        if ( conversionInfo && conversionInfo[servicename] && conversionInfo[servicename][key] ) {
                            // convert property, remember type for when a client-server conversion will be needed
                            if ( !this.serviceScopesConversionInfo[servicename] ) this.serviceScopesConversionInfo[servicename] = {};
                            serviceData[key] = this.converterService.convertFromServerToClient( serviceData[key], conversionInfo[servicename][key], service[key], undefined );
                            this.serviceScopesConversionInfo[servicename][key] = conversionInfo[servicename][key];
                        } else if ( this.serviceScopesConversionInfo[servicename] && this.serviceScopesConversionInfo[servicename][key] ) {
                            delete this.serviceScopesConversionInfo[servicename][key];
                        }

                        service[key] = serviceData[key];
                    }
                } catch ( ex ) {
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
