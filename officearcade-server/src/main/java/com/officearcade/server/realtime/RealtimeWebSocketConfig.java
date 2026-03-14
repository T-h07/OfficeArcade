package com.officearcade.server.realtime;

import com.officearcade.server.config.CorsProperties;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class RealtimeWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CorsProperties corsProperties;
    private final RealtimeJwtChannelInterceptor realtimeJwtChannelInterceptor;

    public RealtimeWebSocketConfig(
            CorsProperties corsProperties,
            RealtimeJwtChannelInterceptor realtimeJwtChannelInterceptor
    ) {
        this.corsProperties = corsProperties;
        this.realtimeJwtChannelInterceptor = realtimeJwtChannelInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        List<String> allowedOrigins = corsProperties.allowedOrigins();
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(realtimeJwtChannelInterceptor);
    }
}
