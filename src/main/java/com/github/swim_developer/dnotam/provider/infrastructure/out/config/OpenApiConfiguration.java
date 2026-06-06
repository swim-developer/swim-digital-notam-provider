package com.github.swim_developer.dnotam.provider.infrastructure.out.config;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@OpenAPIDefinition(
        info = @Info(
                title = "Digital NOTAM (DNOTAM) Subscription and Request Service",
                version = "1.0.0",
                description = """
                        ## Service Abstract
                        
                        
                        The Digital NOTAM Subscription and Request Service allows the service consumer to get aeronautical
                        information in accordance with the Digital NOTAM specification. The aeronautical information conforms
                        to the event scenarios that are supported by Digital NOTAM such as runway closures.
                        
                        
                        The service consumer may subscribe to the service, specifying the event scenarios of interest.
                        It is also possible to send a direct request to the service to get the aeronautical information.
                        The information returned is in the form of an **AIXM 5.1.1** message. This supports the integration
                        of the latest information into an existing aeronautical information store in the various ATM systems.
                        
                        
                        ## Operational Context
                        
                        
                        Air traffic management is defined by ICAO as the *"dynamic, integrated management of air traffic and
                        airspace including air traffic services, airspace management and air traffic flow management - safely,
                        economically and efficiently - through the provision of facilities and seamless services in collaboration
                        with all parties and involving airborne and ground-based functions"*.
                        
                        
                        Stakeholders involved in air traffic management need aeronautical data concerning the establishment,
                        condition or change in any aeronautical facility, service, procedure or hazard, the timely knowledge
                        of which is essential for flight operations.
                        
                        
                        ## Service Capabilities
                        
                        
                        - **Subscribe to event scenarios** - Receive notifications when aeronautical data changes
                        
                        - **Request Digital NOTAMs** - Query current state on-demand
                        
                        - **Distribution** - Automatic delivery when new data is available
                        
                        - **Subscription management** - Pause, resume, list subscriptions
                        
                        - **Topic discovery** - Browse available event scenarios
                        
                        
                        ## Use Cases
                        
                        
                        The aeronautical data can be used in:
                        
                        - **ePIB** (Electronic Pre-flight Information Bulletin)
                        
                        - **Automatic data verification** and graphical visualization
                        
                        - **Airspace reservation systems** (ARES)
                        
                        - **Flight planning** and execution
                        
                        - **ATM decision support** systems
                        
                        
                        ## Intended Consumers
                        
                        
                        - Airport Operators
                        
                        - Civil Air Navigation Service Providers (ANSPs)
                        
                        - Civil Airspace Users (Airlines)
                        
                        - Military Air Navigation Service Providers
                        
                        - Military Airspace Users
                        
                        - Network Manager
                        
                        - Providers of Data Services
                        
                        
                        ## Compliance and Standards
                        
                        
                        This service is compliant with:
                        
                        - **EUROCONTROL Specification for SWIM Technical Infrastructure (TI) Yellow Profile**
                        
                        - **Digital NOTAM Specification v1.0**
                        
                        - **EUROCONTROL SPEC-170** (SWIM-TI Yellow Profile protocol requirements)
                        
                        - **EU Implementing Regulation 2021/116** (Common Project One)
                        
                        - **Commission Implementing Regulation (EU) 2017/373** (Aeronautical data quality)
                        
                        - **ICAO Annex 15** - Aeronautical Information Services
                        
                        - **AIXM 5.1.1** coding guidelines (Common and Technical)
                        
                        
                        ## Three Service Interfaces
                        
                        
                        1. **Subscription Interface** (WS-Light/REST) - Manage subscriptions via REST API
                        
                        2. **Distribution Interface** (AMQP 1.0) - Receive real-time event notifications via message broker
                        
                        3. **Request Interface** (WFS 2.0) - Query current state using OGC Web Feature Service
                        """,
                contact = @Contact(
                        name = "EUROCONTROL",
                        url = "https://eur-registry.swim.aero",
                        email = "swim@eurocontrol.int"
                )
        ),
        tags = {
                @Tag(name = "Subscriptions", description = "Subscription lifecycle management"),
                @Tag(name = "Topics", description = "Event scenario catalog"),
                @Tag(name = "Request Interface (WFS)")
        }
)
@SecurityScheme(
        securitySchemeName = "mTLS",
        type = SecuritySchemeType.MUTUALTLS,
        description = "Mutual TLS authentication using EACP (European Aviation Common PKI) certificates. All API calls require a valid X.509 client certificate issued by the EACP."
)
public class OpenApiConfiguration extends Application {
}

