package cz.chrastecky.SystemSshPlugin;

import cz.chrastecky.SystemSshPlugin.System.ShellList;
import cz.chrastecky.SystemSshPlugin.VO.SshServer;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.CopyOnWriteList;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

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
        PrintStream logger = listener.getLogger();
        SshServer server = getDescriptor().getServerByName(sshSiteName, true);
        Result result = build.getResult();

        if (server == null) {
            logger.println("The site named '" + sshSiteName + "' does not exist");
            if (result == null || result.isBetterThan(Result.FAILURE)) {
                build.setResult(Result.FAILURE);
            }
            return false;
        }
        if (server.getHostname().isEmpty()) {
            logger.println("The hostname is not defined");
            if (result == null || result.isBetterThan(Result.FAILURE)) {
                build.setResult(Result.FAILURE);
            }
            return false;
        }
        if (server.getSshKey().isEmpty()) {
            logger.println("The path to ssh key is empty");
            if (result == null || result.isBetterThan(Result.FAILURE)) {
                build.setResult(Result.FAILURE);
            }
            return false;
        }
        File sshKeyFile = new File(server.getSshKey());
        if (!sshKeyFile.exists() || !sshKeyFile.canRead()) {
            logger.println("The SSH file '" + server.getSshKey() + "' does not exist or is not accessible");
            if (result == null || result.isBetterThan(Result.FAILURE)) {
                build.setResult(Result.FAILURE);
            }
            return false;
        }

        if (server.getPort() == 0) {
            logger.println("The SSH port was not set, using 22 as default");
            server.setPort(22);
        }

        SshClientConfig config = new SshClientConfig(
                getDescriptor().getLocalShell(),
                shouldHideOutput(),
                shouldHideCommand()
        );
        SshClient sshClient = new SshClient(server, build.getEnvironment(listener), config);

        int exitCode = 0;
        if (executeEachLine) {
            String[] commands = command.split("\n");
            for (String cmd : commands) {
                exitCode = sshClient.executeCommand(cmd, logger);
                if (exitCode != 0) {
                    break;
                }
            }
        } else {
            exitCode = sshClient.executeCommand(command, logger);
        }

        if (exitCode != 0) {
            logger.println("The command exited with non-zero status: " + exitCode);
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
                return null;
            }

            return new SshServer(
                    storedServer.getServerDisplayName(),
                    storedServer.getHostname(),
                    storedServer.getPort() == 0 ? defaultPort : storedServer.getPort(),
                    storedServer.getSshKey().isEmpty() ? defaultKey : storedServer.getSshKey(),
                    storedServer.getUsername().isEmpty() ? defaultUsername : storedServer.getUsername()
            );
        }

        public ListBoxModel doFillSshSiteNameItems() {

            ListBoxModel model = new ListBoxModel();
            for (SshServer server : servers) {
                model.add(server.getServerDisplayName());
            }

            return model;
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
    }
}
