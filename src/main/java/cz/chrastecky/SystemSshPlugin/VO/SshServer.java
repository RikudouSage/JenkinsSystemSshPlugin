package cz.chrastecky.SystemSshPlugin.VO;

import org.kohsuke.stapler.DataBoundConstructor;

public class SshServer {

    private String serverDisplayName;
    private String hostname;
    private int port;
    private String sshKey;
    private String username;

    @DataBoundConstructor
    public SshServer(
            String serverDisplayName,
            String hostname,
            int port,
            String sshKey,
            String username
    ) {
        this.serverDisplayName = serverDisplayName;
        this.hostname = hostname;
        this.port = port;
        this.sshKey = sshKey;
        this.username = username;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSshKey() {
        return sshKey;
    }

    public void setSshKey(String sshKey) {
        this.sshKey = sshKey;
    }

    public String getServerDisplayName() {
        return serverDisplayName;
    }

    public void setServerDisplayName(String serverDisplayName) {
        this.serverDisplayName = serverDisplayName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
