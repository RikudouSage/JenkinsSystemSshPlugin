package cz.chrastecky.SystemSshPlugin;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.PasswordCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import cz.chrastecky.SystemSshPlugin.System.ShellList;
import cz.chrastecky.SystemSshPlugin.VO.SshServer;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.CopyOnWriteList;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class SshBuildStep extends Builder {

    private String sshSiteName;
    private String command;
    private boolean hideCommand;
    private boolean hideOutput;
    private boolean executeEachLine;

    @DataBoundConstructor
    public SshBuildStep(
            String sshSiteName,
            String command,
            boolean hideCommand,
            boolean hideOutput,
            boolean executeEachLine
    ) {
        this.sshSiteName = sshSiteName;
        this.command = command;
        this.hideCommand = hideCommand;
        this.hideOutput = hideOutput;
        this.executeEachLine = executeEachLine;
    }

    public String getSshSiteName() {
        return sshSiteName;
    }

    public void setSshSiteName(String sshSiteName) {
        this.sshSiteName = sshSiteName;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, @NotNull BuildListener listener) throws InterruptedException, IOException {
        PrintStream outputStream = listener.getLogger();
        SshServer server = getDescriptor().getServerByName(sshSiteName, true);
        Result result = build.getResult();

        if (server == null) {
            outputStream.println("[SSH Plugin] The server named '" + sshSiteName + "' does not exist");
            if (result == null || result.isBetterThan(Result.FAILURE)) {
                build.setResult(Result.FAILURE);
            }
            return false;
        }
        if (server.getHostname().isEmpty()) {
            outputStream.println("[SSH Plugin] The hostname is not defined");
            if (result == null || result.isBetterThan(Result.FAILURE)) {
                build.setResult(Result.FAILURE);
            }
            return false;
        }
        if (server.getSshKey().isEmpty()) {
            outputStream.println("[SSH Plugin] The path to ssh key is empty");
            if (result == null || result.isBetterThan(Result.FAILURE)) {
                build.setResult(Result.FAILURE);
            }
            return false;
        }
        File sshKeyFile = new File(server.getSshKey());
        if (!sshKeyFile.exists() || !sshKeyFile.canRead()) {
            outputStream.println("[SSH Plugin] The SSH file '" + server.getSshKey() + "' does not exist or is not accessible");
            if (result == null || result.isBetterThan(Result.FAILURE)) {
                build.setResult(Result.FAILURE);
            }
            return false;
        }

        if (server.getPort() == 0) {
            outputStream.println("[SSH Plugin] The SSH port was not set, using 22 as default");
            server.setPort(22);
        }

        SshClientConfig config = new SshClientConfig(
                getDescriptor().getLocalShell(),
                shouldHideOutput(),
                shouldHideCommand()
        );
        SshClient sshClient = new SshClient(server, launcher, config);

        outputStream.println("[SSH Plugin] Executing script on remote server '"+server.getServerDisplayName()+"'");
        outputStream.println();

        int exitCode = 0;
        if (executeEachLine) {
            String[] commands = command.split("\n");
            for (String cmd : commands) {
                exitCode = sshClient.executeCommand(cmd, outputStream);
                if (exitCode != 0) {
                    break;
                }
            }
        } else {
            exitCode = sshClient.executeCommand(command, outputStream);
        }

        if (exitCode != 0) {
            outputStream.println("The command exited with non-zero status: " + exitCode);
            if (result == null || result.isBetterThan(Result.FAILURE)) {
                build.setResult(Result.FAILURE);
            }
            return false;
        }

        return true;
    }

    @Override
    public Descriptor getDescriptor() {
        return (Descriptor) super.getDescriptor();
    }

    public boolean shouldHideCommand() {
        return hideCommand;
    }

    public void setHideCommand(boolean hideCommand) {
        this.hideCommand = hideCommand;
    }

    public boolean shouldHideOutput() {
        return hideOutput;
    }

    public void setHideOutput(boolean hideOutput) {
        this.hideOutput = hideOutput;
    }

    public boolean isExecuteEachLine() {
        return executeEachLine;
    }

    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {

        private int defaultPort = 22;
        private String defaultKey = "";
        private String defaultUsername = "";
        private String localShell = "/bin/sh";
        private String defaultSshPasswordId = "";

        private CopyOnWriteList<SshServer> servers = new CopyOnWriteList<>();

        public Descriptor() {
            super();
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return FreeStyleProject.class.isAssignableFrom(jobType);
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Send command via system SSH";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            json = json.getJSONObject("cz_chrastecky_ssh");

            defaultPort = json.getInt("defaultPort");
            defaultKey = json.getString("defaultKey");
            defaultUsername = json.getString("defaultUsername");
            localShell = json.getString("localShell");
            defaultSshPasswordId = json.getString("defaultSshPasswordId");

            List<SshServer> newServers = req.bindJSONToList(SshServer.class, json.get("servers"));
            for (SshServer newServer : newServers) {
                if (newServer.getServerDisplayName().isEmpty()) {
                    newServer.setServerDisplayName(
                            (newServer.getUsername().isEmpty() ? defaultUsername : newServer.getUsername()) +
                                    "@" +
                                    newServer.getHostname() +
                                    ":" +
                                    (newServer.getPort() == 0 ? defaultPort : newServer.getPort())
                    );
                }
            }
            this.servers.replaceBy(newServers);

            save();
            return true;
        }

        public int getDefaultPort() {
            return defaultPort;
        }

        public String getDefaultKey() {
            return defaultKey;
        }

        public CopyOnWriteList<SshServer> getServers() {
            return servers;
        }

        @Nullable
        SshServer getServerByName(String serverName) {
            return getServerByName(serverName, false);
        }

        @Nullable
        SshServer getServerByName(String serverName, boolean fillDefaults) {
            SshServer storedServer = null;
            for (SshServer server : servers) {
                if (server.getServerDisplayName().equals(serverName)) {
                    storedServer = server;
                }
            }

            if (storedServer == null || !fillDefaults) {
                return storedServer;
            }

            String sshKey = storedServer.getSshKey().isEmpty() ? defaultKey : storedServer.getSshKey();
            StandardUsernameCredentials credentials = null;
            String password = null;

            if (sshKey.equals(defaultKey)) {
                credentials = getDefaultSshPassword();
            } else if(!sshKey.isEmpty()) {
                // todo
            }

            if (credentials != null) {
                if (credentials instanceof PasswordCredentials) {
                    password = Secret.toString(((PasswordCredentials) credentials).getPassword());
                } else if(credentials instanceof SSHUserPrivateKey) {
                    password = Secret.toString(((SSHUserPrivateKey) credentials).getPassphrase());
                } else {
                    Logger.getGlobal().warning("The System SSH Plugin does not support credentials of type '"+credentials.getClass().toString()+"'");
                }
            }

            return new SshServer(
                    storedServer.getServerDisplayName(),
                    storedServer.getHostname(),
                    storedServer.getPort() == 0 ? defaultPort : storedServer.getPort(),
                    storedServer.getSshKey().isEmpty() ? defaultKey : storedServer.getSshKey(),
                    storedServer.getUsername().isEmpty() ? defaultUsername : storedServer.getUsername(),
                    password
            );
        }

        public ListBoxModel doFillSshSiteNameItems() {

            ListBoxModel model = new ListBoxModel();
            for (SshServer server : servers) {
                model.add(server.getServerDisplayName());
            }

            return model;
        }

        public ListBoxModel doFillDefaultSshPasswordIdItems(final @AncestorInPath ItemGroup<?> context) {
            final List<StandardUsernameCredentials> credentials = CredentialsProvider.lookupCredentials(
                    StandardUsernameCredentials.class, context, ACL.SYSTEM, Collections.<DomainRequirement>emptyList()
            );

            return new StandardUsernameListBoxModel().includeEmptyValue().withAll(credentials);
        }

        public String getDefaultUsername() {
            return defaultUsername;
        }

        public String getLocalShell() {
            return localShell;
        }

        public List<String> getShells() {
            return new ShellList().getShells();
        }

        public String getDefaultSshPasswordId() {
            return defaultSshPasswordId;
        }

        @Nullable
        StandardUsernameCredentials getDefaultSshPassword() {
            if (defaultSshPasswordId == null || defaultSshPasswordId.isEmpty()) {
                return null;
            }

            return CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(
                            StandardUsernameCredentials.class,
                            (Item) null,
                            ACL.SYSTEM,
                            Collections.<DomainRequirement>emptyList()
                    ),
                    CredentialsMatchers.withId(defaultSshPasswordId)
            );
        }
    }
}
