package cz.chrastecky.SystemSshPlugin.VO;

import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nullable;

public class SshServer {

    private String serverDisplayName;
    private String hostname;
    private int port;
    private String sshKey;
    private String username;
    @Nullable
    private String password;
    @Nullable
    private String passwordCredentialsId;

    @DataBoundConstructor
    public SshServer(
            String serverDisplayName,
            String hostname,
            int port,
            String sshKey,
            String username,
            @Nullable String password,
            @Nullable String passwordCredentialsId
    ) {
        this.serverDisplayName = serverDisplayName;
        this.hostname = hostname;
        this.port = port;
        this.sshKey = sshKey;
        this.username = username;
        this.password = password;
        this.passwordCredentialsId = passwordCredentialsId;
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

    @Nullable
    public String getPassword() {
        return password;
    }

    public void setPassword(@Nullable String password) {
        this.password = password;
    }

    @Nullable
    public String getPasswordCredentialsId() {
        return passwordCredentialsId;
    }

    public void setPasswordCredentialsId(@Nullable String passwordCredentialsId) {
        this.passwordCredentialsId = passwordCredentialsId;
    }
}
