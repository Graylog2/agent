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

import com.google.inject.assistedinject.Assisted;
import com.typesafe.config.Config;
import org.graylog.collector.config.ConfigurationUtils;
import org.graylog.collector.outputs.OutputConfiguration;
import org.graylog2.gelfclient.GelfTransports;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GelfOutputConfiguration extends OutputConfiguration {
    private final GelfOutput.Factory outputFactory;

    public interface Factory extends OutputConfiguration.Factory<GelfOutputConfiguration> {
        @Override
        GelfOutputConfiguration create(String id, Config config);
    }

    @NotNull
    private GelfTransports protocol = GelfTransports.TCP;

    @NotBlank
    private String host;

    @NotNull
    @Range(min = 1024, max = 65535)
    private int port;

    @NotNull
    private boolean clientTls = false;

    private File clientTlsCertChainFile = null;

    @NotNull
    private boolean clientTlsVerifyCert = true;

    @NotNull
    @Range(min = 1, max = Integer.MAX_VALUE)
    private int clientQueueSize = 512;

    @NotNull
    @Range(min = 1, max = Integer.MAX_VALUE)
    private int clientConnectTimeout = 5000;

    @NotNull
    @Range(min = 1, max = Integer.MAX_VALUE)
    private int clientReconnectDelay = 1000;

    @NotNull
    private boolean clientTcpNoDelay = true;

    @NotNull
    @Range(min = -1, max = Integer.MAX_VALUE)
    private int clientSendBufferSize = -1;

    @Inject
    public GelfOutputConfiguration(@Assisted String id,
                                   @Assisted Config config,
                                   GelfOutput.Factory outputFactory) {
        super(id, config);
        this.outputFactory = outputFactory;

        if (config.hasPath("protocol")) {
            switch (config.getString("protocol").toUpperCase(Locale.ENGLISH)) {
                case "UDP":
                    this.protocol = GelfTransports.UDP;
                    break;
                case "TCP":
                default:
                    this.protocol = GelfTransports.TCP;
                    break;
            }
        }
        if (config.hasPath("host")) {
            this.host = config.getString("host");
        }
        if (config.hasPath("port")) {
            this.port = config.getInt("port");
        }
        if (config.hasPath("client-tls")) {
            this.clientTls = config.getBoolean("client-tls");
        }
        if (config.hasPath("client-tls-cert-chain-file")) {
            this.clientTlsCertChainFile = new File(config.getString("client-tls-cert-chain-file"));
        }
        if (config.hasPath("client-tls-verify-cert")) {
            this.clientTlsVerifyCert = config.getBoolean("client-tls-verify-cert");
        }
        if (config.hasPath("client-queue-size")) {
            this.clientQueueSize = config.getInt("client-queue-size");
        }
        if (config.hasPath("client-connect-timeout")) {
            this.clientConnectTimeout = config.getInt("client-connect-timeout");
        }
        if (config.hasPath("client-reconnect-delay")) {
            this.clientReconnectDelay = config.getInt("client-reconnect-delay");
        }
        if (config.hasPath("client-tcp-no-delay")) {
            this.clientTcpNoDelay = config.getBoolean("client-tcp-no-delay");
        }
        if (config.hasPath("client-send-buffer-size")) {
            this.clientSendBufferSize = config.getInt("client-send-buffer-size");
        }
    }

    @Override
    public GelfOutput createOutput() {
        return outputFactory.create(this);
    }

    public GelfTransports getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isClientTls() {
        return clientTls;
    }

    public File getClientTlsCertChainFile() {
        return clientTlsCertChainFile;
    }

    public boolean isClientTlsVerifyCert() {
        return clientTlsVerifyCert;
    }

    public int getClientQueueSize() {
        return clientQueueSize;
    }

    public int getClientConnectTimeout() {
        return clientConnectTimeout;
    }

    public int getClientReconnectDelay() {
        return clientReconnectDelay;
    }

    public boolean isClientTcpNoDelay() {
        return clientTcpNoDelay;
    }

    public int getClientSendBufferSize() {
        return clientSendBufferSize;
    }

    @Override
    public Map<String, String> toStringValues() {
        return Collections.unmodifiableMap(new HashMap<String, String>(super.toStringValues()) {
            {
                put("host", getHost());
                put("port", String.valueOf(getPort()));
                put("client-queue-size", String.valueOf(getClientQueueSize()));
                put("client-connect-timeout", String.valueOf(getClientConnectTimeout()));
                put("client-reconnect-delay", String.valueOf(getClientReconnectDelay()));
                put("client-tcp-no-delay", String.valueOf(isClientTcpNoDelay()));
                put("client-send-buffer-size", String.valueOf(getClientSendBufferSize()));
            }
        });
    }

    @Override
    public String toString() {
        return ConfigurationUtils.toString(this);
    }
}
