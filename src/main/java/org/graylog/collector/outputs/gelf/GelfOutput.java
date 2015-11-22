/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.collector.outputs.gelf;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.assistedinject.Assisted;
import org.graylog.collector.Message;
import org.graylog.collector.config.ConfigurationUtils;
import org.graylog.collector.outputs.OutputService;
import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfMessageBuilder;
import org.graylog2.gelfclient.GelfMessageLevel;
import org.graylog2.gelfclient.GelfTransports;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class GelfOutput extends OutputService {
    private static final Logger LOG = LoggerFactory.getLogger(GelfOutput.class);

    public interface Factory extends OutputService.Factory<GelfOutput, GelfOutputConfiguration> {
        GelfOutput create(GelfOutputConfiguration configuration);
    }

    private final GelfOutputConfiguration configuration;
    private GelfTransport transport;

    private final CountDownLatch transportInitialized = new CountDownLatch(1);

    @Inject
    public GelfOutput(@Assisted GelfOutputConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void doStart() {
        final GelfConfiguration clientConfig = new GelfConfiguration(configuration.getHost(), configuration.getPort());

        switch (configuration.getProtocol()) {
            case UDP:
                clientConfig
                        .transport(GelfTransports.UDP)
                        .queueSize(configuration.getClientQueueSize())
                        .sendBufferSize(configuration.getClientSendBufferSize());
            case TCP:
                clientConfig
                        .transport(GelfTransports.TCP)
                        .queueSize(configuration.getClientQueueSize())
                        .connectTimeout(configuration.getClientConnectTimeout())
                        .reconnectDelay(configuration.getClientReconnectDelay())
                        .tcpNoDelay(configuration.isClientTcpNoDelay())
                        .sendBufferSize(configuration.getClientSendBufferSize());

                if (configuration.isClientTls()) {
                    clientConfig.enableTls();
                    clientConfig.tlsTrustCertChainFile(configuration.getClientTlsCertChainFile());

                    if (configuration.isClientTlsVerifyCert()) {
                        clientConfig.enableTlsCertVerification();
                    } else {
                        clientConfig.disableTlsCertVerification();
                    }
                }
                break;
        }

        LOG.info("Starting GELF transport: {}", clientConfig);
        this.transport = GelfTransports.create(clientConfig);

        transportInitialized.countDown();

        notifyStarted();
    }

    @Override
    protected void doStop() {
        LOG.debug("Stopping transport {}", transport);
        transport.stop();
        notifyStopped();
    }

    @Override
    public void write(Message message) {
        Uninterruptibles.awaitUninterruptibly(transportInitialized);

        LOG.debug("Sending message: {}", message);

        try {
            final GelfMessageBuilder messageBuilder = new GelfMessageBuilder(message.getMessage(), message.getSource())
                    .timestamp(message.getTimestamp().getMillis() / 1000.0)
                    .additionalFields(message.getFields().asMap());

            if (message.getLevel() != null) {
                messageBuilder.level(GelfMessageLevel.valueOf(message.getLevel().toString()));
            } else {
                messageBuilder.level(null);
            }

            transport.send(messageBuilder.build());
        } catch (InterruptedException e) {
            LOG.error("Failed to send message", e);
        }
    }

    @Override
    public String toString() {
        return ConfigurationUtils.toString(configuration, this);
    }

    @Override
    public String getId() {
        return configuration.getId();
    }

    @Override
    public Set<String> getInputs() {
        return configuration.getInputs();
    }
}
